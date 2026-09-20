-- motorboat_sprites.lua
-- Ajoute la palette ordonnée et un calque d'exemple ombré sans toucher à ton dessin

local palette_colors = {
    {  30,  28,  34, 255 }, -- 1: Contour sombre (#1E1C22)
    { 240, 244, 250, 255 }, -- 2: Fer reflet blanc (#F0F4FA)
    { 195, 200, 210, 255 }, -- 3: Fer clair (#C3C8D2)
    { 145, 150, 160, 255 }, -- 4: Fer moyen (#9196A0)
    {  65,  68,  76, 255 }, -- 5: Fer sombre / ombre (#41444C)
    { 240, 170, 100, 255 }, -- 6: Cuivre reflet clair (#F0AA64)
    { 195, 120,  60, 255 }, -- 7: Cuivre moyen (#C3783C)
    { 145,  80,  35, 255 }, -- 8: Cuivre sombre (#915023)
    { 204, 166, 118, 255 }, -- 9: Bois reflet (#CCA676)
    { 166, 126,  80, 255 }, -- 10: Bois clair (#A67E50)
    { 132,  94,  54, 255 }, -- 11: Bois moyen (#845E36)
    {  96,  64,  34, 255 }, -- 12: Bois sombre (#604022)
    {  58,  36,  18, 255 }, -- 13: Bois contour (#3A2412)
    { 255, 170,  30, 255 }, -- 14: Braise orange (#FFAA1E)
    { 255, 230, 110, 255 }, -- 15: Braise jaune (#FFE66E)
}

local char_to_rgba = {
    ["."] = {0, 0, 0, 0},
    ["0"] = palette_colors[1],
    ["W"] = palette_colors[2],
    ["H"] = palette_colors[3],
    ["I"] = palette_colors[4],
    ["D"] = palette_colors[5],
    ["L"] = palette_colors[6],
    ["C"] = palette_colors[7],
    ["c"] = palette_colors[8],
}

local motor_shaded = {
    "..........00....",
    ".........0HI0...",
    "....000000II0...",
    "...0WHHHHHI00...",
    "...0HIIIIiID0...",
    ".0000iiiiiiD0...",
    ".0I00LCCCCCC0...",
    ".0I0.0LCCCCc0...",
    ".0D0.0LCCCCc0...",
    ".00..00kCCCc0...",
    "......00HiD0....",
    ".......0HiD0....",
    "......00HiD00...",
    ".....0H00iD0I0..",
    ".....0000D000...",
    "........00......",
}

local function apply_palette_and_reference_layer()
    local spr = app.activeSprite
    if not spr then
        app.alert("Ouvre d'abord ton fichier de sprites dans Aseprite !")
        return
    end

    app.transaction(function()
        -- 1. Mettre à jour la palette dans la barre de gauche
        local pal = spr.palettes[1]
        if pal then
            pal:resize(#palette_colors + 1)
            pal:setColor(0, Color{ r = 0, g = 0, b = 0, a = 0 })
            for i, c in ipairs(palette_colors) do
                pal:setColor(i, Color{ r = c[1], g = c[2], b = c[3], a = c[4] })
            end
        end

        -- 2. Trouver ou créer le calque 'Modele_Ombrage'
        local ref_layer = nil
        for _, l in ipairs(spr.layers) do
            if l.name == "Modele_Ombrage" then
                ref_layer = l
                break
            end
        end
        if not ref_layer then
            ref_layer = spr:newLayer()
            ref_layer.name = "Modele_Ombrage"
        end

        -- 3. Dessiner le modèle ombré sur ce calque
        local old_cel = ref_layer:cel(1)
        if old_cel then
            spr:deleteCel(old_cel)
        end

        local img = Image(spr.width, spr.height, spr.colorMode)
        for r = 1, 16 do
            local line = motor_shaded[r]
            for c = 1, 16 do
                local ch = line:sub(c, c)
                local rgba = char_to_rgba[ch]
                if rgba and rgba[4] > 0 then
                    local col = Color{ r = rgba[1], g = rgba[2], b = rgba[3], a = rgba[4] }
                    img:drawPixel(c - 1, r - 1, col)
                end
            end
        end
        spr:newCel(ref_layer, 1, img, Point(0, 0))
    end)

    app.refresh()
    app.alert("Palette mise a jour dans la barre de gauche !\nNouveau calque 'Modele_Ombrage' cree (clique sur l'oeil du calque pour comparer).")
end

local function export_slices()
    local spr = app.activeSprite
    if not spr then return end
    local dlg = Dialog("Exporter les sprites")
    dlg:entry{ id = "export_dir", label = "Dossier cible :", text = "" }
    dlg:button{ id = "ok", text = "Exporter" }
    dlg:button{ id = "cancel", text = "Annuler" }
    dlg:show()
    local data = dlg.data
    if not data.ok or data.export_dir == "" then return end
    local dir = data.export_dir:gsub("\\", "/")
    if dir:sub(-1) ~= "/" then dir = dir .. "/" end

    local count = 0
    for _, slice in ipairs(spr.slices) do
        local bounds = slice.bounds
        local img = Image(bounds.width, bounds.height, spr.colorMode)
        img:drawSprite(spr, 1, Point(-bounds.x, -bounds.y))
        img:saveAs(dir .. slice.name .. ".png")
        count = count + 1
    end
    app.alert(count .. " sprites exportes dans :\n" .. dir)
end

local dlg = Dialog("Motorboat Helper")
dlg:button{ text = "1. Charger la palette & calque exemple (sans toucher a mon dessin)", onclick = apply_palette_and_reference_layer }
dlg:separator()
dlg:button{ text = "2. Exporter les 5 PNG", onclick = export_slices }
dlg:show{ wait = false }
