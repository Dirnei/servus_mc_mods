"""Generates the two layers of the Seed item.

This script is the source of these files; edit the shapes here, not the PNGs.

    python tools/seed-textures/gen.py                    writes the textures
    python tools/seed-textures/gen.py --preview out.png  also writes an 8x preview, tinted gold

Written into alchemy/src/main/resources/assets/servus_alchemy/textures/item/:

- seed.png: layer 0, a greyscale kernel lying on the diagonal, tinted per vein by the game
- seed_husk.png: layer 1, the dark husk cupping the kernel's lower end and its little stem, drawn untinted
"""
import math
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'alchemy', 'src', 'main', 'resources', 'assets', 'servus_alchemy', 'textures', 'item')
SIZE = 16

# The kernel: an ellipse along the diagonal from lower left to upper right.
CENTRE = (8.5, 7.5)
HALF_LONG = 6.2
HALF_SHORT = 3.6
# The husk covers the kernel below this distance along the long axis, measured from the centre.
HUSK_FROM = -1.6
HUSK = [(0x3b, 0x2a, 0x1c), (0x55, 0x3c, 0x26), (0x70, 0x50, 0x32)]
STEM = [(2, 14), (3, 13), (1, 15)]


def png_bytes(img):
    h = len(img)
    w = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write(name, img):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), 'wb') as f:
        f.write(png_bytes(img))


def blank():
    return [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]


def axes(x, y):
    """Distance of the pixel centre along the long axis (towards upper right) and across it."""
    dx = x + 0.5 - CENTRE[0]
    dy = y + 0.5 - CENTRE[1]
    along = (dx - dy) / math.sqrt(2)
    across = (dx + dy) / math.sqrt(2)
    return along, across


def inside(x, y):
    along, across = axes(x, y)
    return (along / HALF_LONG) ** 2 + (across / HALF_SHORT) ** 2 <= 1


def edge(x, y):
    return inside(x, y) and not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def kernel():
    img = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if not inside(x, y):
                continue
            along, across = axes(x, y)
            # Light falls from the upper left: brighter across the axis towards it, a highlight streak near the tip.
            shade = 0.72 - 0.07 * across + 0.03 * along
            if -1.2 < across < -0.2 and 0.5 < along < 4.0:
                shade += 0.22
            if edge(x, y):
                shade -= 0.3
            v = max(40, min(255, round(255 * shade)))
            img[y][x] = (v, v, v, 255)
    return img


def husk():
    img = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if not inside(x, y):
                continue
            along, across = axes(x, y)
            if along >= HUSK_FROM + 0.25 * abs(across):
                continue
            tone = 0 if edge(x, y) else (2 if across < -0.8 else 1)
            img[y][x] = HUSK[tone] + (255,)
    for x, y in STEM:
        img[y][x] = HUSK[0] + (255,)
    return img


def preview(path, layers, tint=(0xfa, 0xd6, 0x3e)):
    scale = 8
    out = [[(0, 0, 0, 255)] * (SIZE * scale) for _ in range(SIZE * scale)]
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            back = 90 if ((x // 8 + y // 8) % 2) else 60
            px = (back, back, back)
            for i, layer in enumerate(layers):
                r, g, b, a = layer[y // scale][x // scale]
                if i == 0:
                    r, g, b = r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255
                mix = a / 255
                px = tuple(round(c * mix + p * (1 - mix)) for c, p in zip((r, g, b), px))
            out[y][x] = px + (255,)
    with open(path, 'wb') as f:
        f.write(png_bytes(out))


def main():
    layers = [kernel(), husk()]
    write('seed.png', layers[0])
    write('seed_husk.png', layers[1])
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], layers)


if __name__ == '__main__':
    main()
