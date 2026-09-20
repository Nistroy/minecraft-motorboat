#!/usr/bin/env python3
"""Génère les textures du mod (PNG RGBA, sans dépendance externe).

Les régions de la texture d'entité sont calculées à partir des boîtes du modèle : même dépliage UV
que Minecraft (haut, bas, droite, avant, gauche, arrière), donc les nombres du moteur ici doivent
rester ceux du menu.

Les modèles d'entité ne sont plus décrits ici : coque et moteurs sont lus dans tools/art/*.bbmodel,
donc les textures suivent les modèles même après retouche dans Blockbench. Peints à la main un jour,
supprimer la fonction correspondante — sinon elle les écrase.

Usage : python3 tools/generate_textures.py
"""

import json
import math
import pathlib
import struct
import zlib

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/motorboat/textures"

CLEAR = (0, 0, 0, 0)
IRON = (82, 85, 92, 255)
IRON_LIGHT = (107, 111, 120, 255)
IRON_DARK = (58, 60, 66, 255)
COPPER = (184, 115, 51, 255)
COPPER_DARK = (140, 84, 35, 255)
PIPE = (58, 58, 62, 255)
HOLE = (20, 20, 22, 255)
PANEL = (198, 198, 198, 255)
PANEL_LIGHT = (255, 255, 255, 255)
PANEL_DARK = (85, 85, 85, 255)
SLOT = (139, 139, 139, 255)
SLOT_SHADOW = (55, 55, 55, 255)
FLAME_OUT = (255, 154, 0, 255)
FLAME_IN = (255, 221, 85, 255)
FLAME_OFF = (110, 110, 110, 255)
WOOD = (138, 106, 67, 255)
WOOD_DARK = (105, 80, 50, 255)


class Image:
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.pixels = [[CLEAR] * width for _ in range(height)]

    def rect(self, x, y, w, h, color):
        for row in range(y, y + h):
            for col in range(x, x + w):
                self.pixels[row][col] = color

    def save(self, path):
        raw = b"".join(
            b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in self.pixels
        )
        chunks = [
            chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0)),
            chunk(b"IDAT", zlib.compress(raw, 9)),
            chunk(b"IEND", b""),
        ]
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b"\x89PNG\r\n\x1a\n" + b"".join(chunks))


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


def box_faces(u, v, dx, dy, dz):
    """Régions (x, y, w, h) des 6 faces d'une boîte, dépliage UV de Minecraft."""
    return {
        "top": (u + dz, v, dx, dz),
        "bottom": (u + dz + dx, v, dx, dz),
        "right": (u, v + dz, dz, dy),
        "front": (u + dz, v + dz, dx, dy),
        "left": (u + dz + dx, v + dz, dz, dy),
        "back": (u + 2 * dz + dx, v + dz, dx, dy),
    }


# Disposition du menu de la barque : doit rester celle de MotorboatMenu / MotorboatScreen.
GUI_WIDTH = 176
GUI_HEIGHT = 190
FUEL_SLOT = (8, 18)
MOTOR_SLOT = (52, 18)
FLAME = (30, 19)
STORAGE_TOP = 40
PLAYER_TOP = 107
HOTBAR_Y = 165

# Silhouette de la flamme de la jauge, 14 × 14.
FLAME_MASK = [
    "..............",
    "..............",
    "......##......",
    ".....####.....",
    ".....####.....",
    "....######....",
    "...########...",
    "...########...",
    "..##########..",
    "..##########..",
    "..##########..",
    "..##########..",
    "...########...",
    "....######....",
]


def gui_texture():
    """Menu de la barque : cadre, slots, jauge. Dessin maison, aucun asset Mojang copié."""
    image = Image(256, 256)
    image.rect(0, 0, GUI_WIDTH, GUI_HEIGHT, PANEL)
    # Biseau du cadre : clair en haut à gauche, sombre en bas à droite.
    image.rect(0, 0, GUI_WIDTH, 1, PANEL_LIGHT)
    image.rect(0, 0, 1, GUI_HEIGHT, PANEL_LIGHT)
    image.rect(0, GUI_HEIGHT - 1, GUI_WIDTH, 1, PANEL_DARK)
    image.rect(GUI_WIDTH - 1, 0, 1, GUI_HEIGHT, PANEL_DARK)
    image.rect(1, GUI_HEIGHT - 2, GUI_WIDTH - 2, 1, (170, 170, 170, 255))
    image.rect(GUI_WIDTH - 2, 1, 1, GUI_HEIGHT - 2, (170, 170, 170, 255))

    for x, y in slot_positions():
        slot(image, x, y)
    for row, y in enumerate(FLAME_MASK):
        for col, cell in enumerate(y):
            if cell == "#":
                image.pixels[FLAME[1] + row][FLAME[0] + col] = FLAME_OFF
                # Sprite allumé, rangé à droite du panneau (u = 176, v = 0).
                image.pixels[row][GUI_WIDTH + col] = FLAME_IN if 3 < col < 10 and row > 6 else FLAME_OUT
    image.save(ASSETS / "gui/container/motorboat.png")


def slot_positions():
    """Coins haut-gauche des 18 × 18 de chaque slot (le slot logique est 1 px plus bas à droite)."""
    yield FUEL_SLOT
    yield MOTOR_SLOT
    for row in range(3):
        for col in range(9):
            yield (8 + col * 18, STORAGE_TOP + row * 18)
    for row in range(3):
        for col in range(9):
            yield (8 + col * 18, PLAYER_TOP + row * 18)
    for col in range(9):
        yield (8 + col * 18, HOTBAR_Y)


def slot(image, x, y):
    """Creux 18 × 18 : le contenu tient dans les 16 × 16 du milieu."""
    image.rect(x - 1, y - 1, 18, 18, SLOT)
    image.rect(x - 1, y - 1, 18, 1, SLOT_SHADOW)
    image.rect(x - 1, y - 1, 1, 18, SLOT_SHADOW)
    image.rect(x - 1, y + 16, 18, 1, PANEL_LIGHT)
    image.rect(x + 16, y - 1, 1, 18, PANEL_LIGHT)
    image.pixels[y - 1][x + 16] = SLOT
    image.pixels[y + 16][x - 1] = SLOT


ART = pathlib.Path(__file__).resolve().parent / "art"

HULL_MODEL = ART / "big_hull.bbmodel"

WOOD_LIGHT = (162, 128, 84, 255)

# Teintes (base, clair, sombre) par famille de pièce de coque, d'après le préfixe du nom.
HULL_TONES = {
    "fond": ((120, 92, 58, 255), (139, 107, 69, 255), (92, 70, 44, 255)),
    "pont": ((132, 102, 65, 255), (154, 120, 78, 255), (101, 77, 48, 255)),
    "borde": ((146, 113, 72, 255), (170, 134, 88, 255), (110, 84, 53, 255)),
    "tableau": ((146, 113, 72, 255), (170, 134, 88, 255), (110, 84, 53, 255)),
    "etrave": ((138, 106, 67, 255), (162, 128, 84, 255), (105, 80, 50, 255)),
    "liston": ((172, 137, 91, 255), (196, 160, 110, 255), (128, 100, 64, 255)),
    "banquette": ((172, 137, 91, 255), (196, 160, 110, 255), (128, 100, 64, 255)),
    "banc": ((158, 124, 81, 255), (182, 147, 99, 255), (118, 91, 58, 255)),
}

# Teintes (base, clair, sombre) par famille de pièce de moteur, d'après le préfixe du nom.
ENGINE_TONES = {
    "capot": (IRON, IRON_LIGHT, IRON_DARK),
    "chape": (IRON_DARK, IRON, (40, 42, 46, 255)),
    "echappement": (PIPE, (78, 78, 84, 255), HOLE),
    "arbre": (IRON_DARK, IRON, (40, 42, 46, 255)),
    "embase": (IRON, IRON_LIGHT, IRON_DARK),
    "helice": (IRON_LIGHT, (140, 144, 154, 255), IRON),
    "barre": (IRON_DARK, IRON, (40, 42, 46, 255)),
}


def metal(image, x, y, w, h, tone, band=False):
    """Remplit une face de métal : arête claire en haut, sombre en bas, boulons aux coins bas.

    {@code band} ajoute le bandeau de cuivre du capot, la seule touche de couleur du moteur."""
    base, light, dark = tone
    image.rect(x, y, w, h, base)
    image.rect(x, y, w, 1, light)
    image.rect(x, y + h - 1, w, 1, dark)
    if band and h >= 5 and w >= 3:
        image.rect(x, y + h // 2, w, 1, COPPER)
        image.rect(x, y + h // 2 + 1, w, 1, COPPER_DARK)
    if w >= 3 and h >= 3:
        image.rect(x, y + h - 2, 1, 1, dark)
        image.rect(x + w - 1, y + h - 2, 1, 1, dark)


def engine_textures():
    """Peint les deux hors-bord d'après tools/art/{motor,big_motor}.bbmodel.

    Même montage que la coque : les boîtes et les UV viennent du modèle, jamais d'une table
    recopiée ici. Peints à la main un jour → supprimer cette fonction, sinon elle écrase."""
    for model in ("motor", "big_motor"):
        source = json.loads((ART / f"{model}.bbmodel").read_text(encoding="utf-8"))
        image = Image(source["resolution"]["width"], source["resolution"]["height"])
        for element in source["elements"]:
            u, v = element["uv_offset"]
            dx, dy, dz = (math.ceil(element["to"][i] - element["from"][i]) for i in range(3))
            family = element["name"].split("_")[0]
            for name, (x, y, w, h) in box_faces(u, v, dx, dy, dz).items():
                metal(image, x, y, w, h, ENGINE_TONES[family],
                      band=family == "capot" and name in ("right", "front", "left", "back"))
        image.save(ASSETS / f"entity/{model}.png")


def planks(image, x, y, w, h, tone=None):
    """Remplit une face de planches : joints sombres tous les 4 px, veinure claire au-dessus.

    La phase des joints dépend de la position de la face dans l'atlas : deux faces voisines ne
    s'alignent pas, ce qui évite l'effet « papier peint » sur une coque de 23 pièces."""
    base, light, dark = tone or (WOOD, WOOD_LIGHT, WOOD_DARK)
    phase = (x * 7 + y * 3) % 4
    image.rect(x, y, w, h, base)
    for row in range(y, y + h):
        if (row - y + phase) % 4 == 3:
            image.rect(x, row, w, 1, dark)
        elif (row - y + phase) % 4 == 0:
            image.rect(x, row, w, 1, light)
        # veinure : un pixel sombre isolé, toujours au même endroit pour une face donnée
        elif w > 5 and (row * 5 + x * 11) % 7 == 0:
            image.rect(x + ((row * 13 + x * 5) % (w - 2)) + 1, row, 1, 1, dark)
    # arêtes : liseré clair en haut, sombre en bas et sur les côtés
    image.rect(x, y, w, 1, light)
    image.rect(x, y + h - 1, w, 1, dark)
    image.rect(x, y, 1, h, dark)
    image.rect(x + w - 1, y, 1, h, dark)


def big_hull_texture():
    """Peint la grande coque d'après tools/art/big_hull.bbmodel : une teinte par famille de pièce,
    les UV viennent du modèle (uv_offset), jamais d'une table recopiée ici."""
    model = json.loads(HULL_MODEL.read_text(encoding="utf-8"))
    image = Image(model["resolution"]["width"], model["resolution"]["height"])
    for element in model["elements"]:
        u, v = element["uv_offset"]
        dx, dy, dz = (math.ceil(element["to"][i] - element["from"][i]) for i in range(3))
        tone = HULL_TONES[element["name"].split("_")[0]]
        for x, y, w, h in box_faces(u, v, dx, dy, dz).values():
            planks(image, x, y, w, h, tone)
    image.save(ASSETS / "entity/big_motorboat.png")


if __name__ == "__main__":
    gui_texture()
    engine_textures()
    big_hull_texture()
    print("textures générées dans", ASSETS)
