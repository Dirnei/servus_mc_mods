"""Generates the Data Potato item texture.

This script is the source of the file; edit the shapes here, not the PNG.

    python tools/potato-texture/gen.py                    writes the texture
    python tools/potato-texture/gen.py --preview out.png  also writes an 8x preview

Written into foundation/src/main/resources/assets/servus_foundation/textures/item/data_potato.png:
a potato lying on the diagonal in vanilla potato browns, a copper probe stuck
into its upper end and a small dark readout with a green trace on its side.
"""
import math
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'foundation', 'src', 'main', 'resources', 'assets', 'servus_foundation', 'textures', 'item')
SIZE = 16

CENTRE = (6.8, 9.2)
HALF_LONG = 6.6
HALF_SHORT = 4.1

OUTLINE = (0x5c, 0x3b, 0x17)
SKIN = [(0x8e, 0x60, 0x27), (0xb0, 0x7d, 0x36), (0xc9, 0x96, 0x47), (0xdc, 0xb0, 0x60), (0xea, 0xc9, 0x82)]
EYE = (0x7a, 0x52, 0x22)
EYES = [(3, 11), (7, 6), (9, 11)]

COPPER = [(0x8a, 0x3e, 0x28), (0xc0, 0x5a, 0x3a), (0xe7, 0x7c, 0x56)]
# The probe from inside the potato's upper end out to its tip; each pixel names its copper shade.
PROBE = [(10, 5, 0), (11, 5, 0), (11, 4, 1), (12, 4, 0), (12, 3, 1), (13, 3, 0), (13, 2, 2), (14, 2, 1), (14, 1, 2)]

SCREEN = (0x1f, 0x26, 0x22)
TRACE = [(0x3f, 0xa8, 0x48), (0x7d, 0xf0, 0x7a)]
# The readout's dark frame, then the green trace drawn over it.
READOUT = (4, 8, 4, 3)
TRACE_PIXELS = [(4, 9, 0), (5, 9, 1), (6, 8, 1), (7, 9, 0)]


def png_bytes(img):
    h = len(img)
    w = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def axes(x, y):
    """Distance of the pixel centre along the long axis (towards upper right) and across it (towards lower right)."""
    dx = x + 0.5 - CENTRE[0]
    dy = y + 0.5 - CENTRE[1]
    return (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)


def inside(x, y):
    if not (0 <= x < SIZE and 0 <= y < SIZE):
        return False
    along, across = axes(x, y)
    return (along / HALF_LONG) ** 2 + (across / HALF_SHORT) ** 2 <= 1


def edge(x, y):
    return inside(x, y) and not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def skin(x, y):
    """Light falls from the upper left: the shade steps up across the axis towards it."""
    _, across = axes(x, y)
    t = 1 - (across / HALF_SHORT + 1) / 2
    return SKIN[min(len(SKIN) - 1, max(0, int(t * len(SKIN))))]


def potato():
    img = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if edge(x, y):
                img[y][x] = OUTLINE + (255,)
            elif inside(x, y):
                img[y][x] = skin(x, y) + (255,)
    for x, y in EYES:
        img[y][x] = EYE + (255,)
    rx, ry, rw, rh = READOUT
    for y in range(ry, ry + rh):
        for x in range(rx, rx + rw):
            img[y][x] = SCREEN + (255,)
    for x, y, shade in TRACE_PIXELS:
        img[y][x] = TRACE[shade] + (255,)
    for x, y, shade in PROBE:
        img[y][x] = COPPER[shade] + (255,)
    return img


def scaled(img, factor):
    return [[px for px in row for _ in range(factor)] for row in img for _ in range(factor)]


def main():
    img = potato()
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, 'data_potato.png'), 'wb') as f:
        f.write(png_bytes(img))
    if len(sys.argv) == 3 and sys.argv[1] == '--preview':
        with open(sys.argv[2], 'wb') as f:
            f.write(png_bytes(scaled(img, 8)))


if __name__ == '__main__':
    main()
