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
    engine_texture()
    motor_item()
    motorboat_item()
    print("textures générées dans", ASSETS)
