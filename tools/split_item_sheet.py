#!/usr/bin/env python3
"""Découpe la planche de sprites faite main en un PNG par item (stdlib seule).

Source : tools/art/motorboat_items.png, 16 px de haut, un sprite 16 × 16 par item, dans l'ordre de
SPRITES. La planche reste hors de assets/ : tout PNG posé dans textures/item/ est cousu dans l'atlas
des items par Minecraft, une planche entière y prendrait de la place pour rien.

Usage : python3 tools/split_item_sheet.py
"""

import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
SHEET = ROOT / "tools/art/motorboat_items.png"
ITEMS = ROOT / "src/main/resources/assets/motorboat/textures/item"

SIZE = 16
# Ordre de la planche, gauche → droite (confirmé par nistroy 2026-09-20).
SPRITES = ["motor", "big_motor", "double_motor", "motorboat", "big_motorboat"]


def read_rgba(path):
    """Décode un PNG RGBA 8 bits non entrelacé en lignes de pixels."""
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path} : pas un PNG")
    pos, idat, width, height = 8, b"", 0, 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        kind = data[pos + 4 : pos + 8]
        body = data[pos + 8 : pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if (depth, color, interlace) != (8, 6, 0):
                raise ValueError(f"{path} : attendu RGBA 8 bits non entrelacé")
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    return width, height, unfilter(zlib.decompress(idat), width, height)


def unfilter(raw, width, height):
    """Annule les filtres par ligne du PNG (types 0 à 4)."""
    stride = width * 4
    previous = bytearray(stride)
    rows = []
    pos = 0
    for _ in range(height):
        kind = raw[pos]
        line = bytearray(raw[pos + 1 : pos + 1 + stride])
        pos += 1 + stride
        for i in range(stride):
            left = line[i - 4] if i >= 4 else 0
            up = previous[i]
            corner = previous[i - 4] if i >= 4 else 0
            if kind == 0:
                value = line[i]
            elif kind == 1:
                value = line[i] + left
            elif kind == 2:
                value = line[i] + up
            elif kind == 3:
                value = line[i] + ((left + up) >> 1)
            elif kind == 4:
                value = line[i] + paeth(left, up, corner)
            else:
                raise ValueError(f"filtre PNG inconnu : {kind}")
            line[i] = value & 0xFF
        rows.append([tuple(line[x * 4 : x * 4 + 4]) for x in range(width)])
        previous = line
    return rows


def paeth(left, up, corner):
    guess = left + up - corner
    da, db, dc = abs(guess - left), abs(guess - up), abs(guess - corner)
    if da <= db and da <= dc:
        return left
    return up if db <= dc else corner


def write_rgba(path, rows):
    raw = b"".join(
        b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in rows
    )
    chunks = [
        chunk(b"IHDR", struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0)),
        chunk(b"IDAT", zlib.compress(raw, 9)),
        chunk(b"IEND", b""),
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + b"".join(chunks))


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


def main():
    width, height, rows = read_rgba(SHEET)
    expected = SIZE * len(SPRITES)
    if (width, height) != (expected, SIZE):
        raise SystemExit(f"{SHEET} : attendu {expected} × {SIZE}, trouvé {width} × {height}")
    for index, name in enumerate(SPRITES):
        left = index * SIZE
        sprite = [row[left : left + SIZE] for row in rows]
        write_rgba(ITEMS / f"{name}.png", sprite)
    print(f"{len(SPRITES)} sprites écrits dans {ITEMS}")


if __name__ == "__main__":
    main()
