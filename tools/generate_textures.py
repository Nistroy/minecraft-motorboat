#!/usr/bin/env python3
"""Génère les textures du mod (PNG RGBA, sans dépendance externe).

Les régions de la texture d'entité sont calculées à partir des boîtes du modèle : même dépliage UV
que Minecraft (haut, bas, droite, avant, gauche, arrière), donc les nombres ici doivent rester ceux
de MotorboatRenderer.createEngineLayer().

Usage : python3 tools/generate_textures.py
"""

import pathlib
import struct
import zlib

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/motorboat/textures"

CLEAR = (0, 0, 0, 0)
OUTLINE = (26, 26, 29, 255)
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


def engine_texture():
    image = Image(32, 32)
    # Bloc moteur : boîte 4 × 7 × 5 à texOffs(0, 0).
    body = box_faces(0, 0, 4, 7, 5)
    for name, (x, y, w, h) in body.items():
        image.rect(x, y, w, h, IRON_LIGHT if name == "top" else IRON)
        image.rect(x, y, w, 1, IRON_LIGHT)  # arête du haut
        image.rect(x, y + h - 1, w, 1, IRON_DARK)  # arête du bas
    for name in ("right", "front", "left", "back"):
        x, y, w, h = body[name]
        image.rect(x, y + 3, w, 1, COPPER)  # bandeau de cuivre
        image.rect(x, y + 4, w, 1, COPPER_DARK)
        image.rect(x, y + h - 2, 1, 1, IRON_DARK)  # boulons
        image.rect(x + w - 1, y + h - 2, 1, 1, IRON_DARK)
    x, y, w, h = body["top"]
    image.rect(x + 1, y + 1, w - 2, h - 2, IRON)  # plaque du dessus
    image.rect(x + 1, y + 2, 2, 2, COPPER)  # volant moteur

    # Échappement : boîte 2 × 4 × 2 à texOffs(0, 13).
    pipe = box_faces(0, 13, 2, 4, 2)
    for name, (x, y, w, h) in pipe.items():
        image.rect(x, y, w, h, PIPE)
    x, y, w, h = pipe["top"]
    image.rect(x, y, w, h, HOLE)
    image.save(ASSETS / "entity/motor.png")


# Boîtes de BigMotorboatModel.createBodyModel() : (texOffs u, v, dx, dy, dz).
BIG_HULL_BOXES = {
    "bottom": (0, 0, 36, 1, 28),
    "port": (0, 32, 36, 6, 1),
    "starboard": (0, 40, 36, 6, 1),
    "bow": (0, 48, 1, 6, 26),
    "stern": (64, 48, 1, 6, 26),
}

WOOD_LIGHT = (162, 128, 84, 255)


def planks(image, x, y, w, h):
    """Remplit une face de planches : veinures claires, joints sombres tous les 4 px."""
    image.rect(x, y, w, h, WOOD)
    for row in range(y, y + h):
        if (row - y) % 4 == 3:
            image.rect(x, row, w, 1, WOOD_DARK)
        elif (row - y) % 4 == 0:
            image.rect(x, row, w, 1, WOOD_LIGHT)
    image.rect(x, y, 1, h, WOOD_DARK)
    image.rect(x + w - 1, y, 1, h, WOOD_DARK)


def big_hull_texture():
    image = Image(256, 128)
    for u, v, dx, dy, dz in BIG_HULL_BOXES.values():
        for x, y, w, h in box_faces(u, v, dx, dy, dz).values():
            planks(image, x, y, w, h)
    # Plan d'eau : rendu en masque, jamais vu, mais on ne laisse pas de trou dans l'atlas.
    image.rect(128, 0, 34, 26, WOOD_DARK)
    image.save(ASSETS / "entity/big_motorboat.png")


def big_motorboat_item():
    image = Image(16, 16)
    # Coque longue vue de trois quarts, moteur à la poupe (à gauche).
    image.rect(0, 7, 16, 7, OUTLINE)
    image.rect(1, 8, 14, 5, WOOD)
    image.rect(1, 8, 14, 1, WOOD_LIGHT)
    image.rect(1, 11, 14, 2, WOOD_DARK)
    image.rect(2, 13, 12, 1, WOOD_DARK)
    # Trois bancs.
    for x in (4, 7, 10):
        image.rect(x, 9, 2, 2, WOOD_DARK)
    image.rect(0, 2, 5, 6, OUTLINE)
    image.rect(1, 3, 3, 4, IRON)
    image.rect(1, 3, 3, 1, IRON_LIGHT)
    image.rect(1, 5, 3, 1, COPPER)
    image.rect(2, 0, 3, 3, OUTLINE)
    image.rect(3, 0, 1, 2, PIPE)
    image.save(ASSETS / "item/big_motorboat.png")


def motor_item():
    image = Image(16, 16)
    image.rect(6, 1, 4, 4, OUTLINE)
    image.rect(7, 2, 2, 3, PIPE)
    image.rect(7, 2, 2, 1, HOLE)
    image.rect(2, 4, 12, 10, OUTLINE)
    image.rect(3, 5, 10, 8, IRON)
    image.rect(3, 5, 10, 1, IRON_LIGHT)
    image.rect(3, 12, 10, 1, IRON_DARK)
    image.rect(3, 8, 10, 2, COPPER)
    image.rect(3, 10, 10, 1, COPPER_DARK)
    image.rect(4, 6, 1, 1, IRON_LIGHT)
    image.rect(11, 6, 1, 1, IRON_LIGHT)
    image.rect(4, 11, 1, 1, IRON_DARK)
    image.rect(11, 11, 1, 1, IRON_DARK)
    image.save(ASSETS / "item/motor.png")


def motorboat_item():
    image = Image(16, 16)
    # Coque vue de trois quarts : deux rangées de planches entre deux bords sombres.
    image.rect(1, 8, 14, 6, OUTLINE)
    image.rect(2, 9, 12, 4, WOOD)
    image.rect(2, 11, 12, 2, WOOD_DARK)
    image.rect(2, 9, 12, 1, (162, 128, 84, 255))
    image.rect(3, 13, 10, 1, WOOD_DARK)
    # Moteur à la poupe (à gauche) et son échappement.
    image.rect(1, 3, 5, 6, OUTLINE)
    image.rect(2, 4, 3, 4, IRON)
    image.rect(2, 4, 3, 1, IRON_LIGHT)
    image.rect(2, 6, 3, 1, COPPER)
    image.rect(3, 1, 3, 3, OUTLINE)
    image.rect(4, 1, 1, 2, PIPE)
    image.save(ASSETS / "item/motorboat.png")


if __name__ == "__main__":
    gui_texture()
    engine_texture()
    big_hull_texture()
    motor_item()
    motorboat_item()
    big_motorboat_item()
    print("textures générées dans", ASSETS)
