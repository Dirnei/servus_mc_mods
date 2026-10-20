"""Generates the Servus: Brewery item textures.

This script is the source of the files; edit the shapes here, not the PNGs.

    python tools/brewery-textures/gen.py                      writes the textures
    python tools/brewery-textures/gen.py --preview <folder>   also writes 8x previews

Written into brewery/src/main/resources/assets/servus_brewery/textures/item/.
Sprites are drawn from pixel masks on a 16x16 canvas.
"""
import math
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'brewery', 'src', 'main', 'resources', 'assets', 'servus_brewery', 'textures', 'item')
SIZE = 16
CLEAR = (0, 0, 0, 0)

GLASS = (207, 230, 238, 150)
GLASS_EDGE = (111, 142, 154, 255)
GLASS_GLINT = (255, 255, 255, 170)


def rect(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


def capsule(x1, y1, x2, y2, r):
    """Pixels whose centre lies within r of the segment from (x1, y1) to (x2, y2)."""
    out = set()
    dx, dy = x2 - x1, y2 - y1
    for y in range(SIZE):
        for x in range(SIZE):
            px, py = x + .5, y + .5
            t = max(0, min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)))
            if math.hypot(px - x1 - t * dx, py - y1 - t * dy) <= r:
                out.add((x, y))
    return out


def boundary(mask):
    return {(x, y) for (x, y) in mask
            if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def canvas():
    return [[CLEAR] * SIZE for _ in range(SIZE)]


def fill(img, mask, color):
    for (x, y) in mask:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            img[y][x] = color


def empty_glass(top=4, left=3, right=10):
    """The empty half litre glass: a straight body with glints and a handle ring on the right."""
    img = canvas()
    shell = rect(left, top, right, 14)
    fill(img, shell, GLASS)
    fill(img, boundary(shell), GLASS_EDGE)
    glints = {(x, y) for y in range(top + 1, 14) for x in range(left + 1, right)
              if (x - left) % 3 == 1 and (y - top) % 3 == 1}
    fill(img, glints, GLASS_GLINT)
    ring = capsule(right + 2.2, top + 3.5, right + 2.2, top + 8.5, 2.0) - capsule(right + 2.0, top + 3.5, right + 2.0, top + 8.5, .9)
    fill(img, {(x, y) for (x, y) in ring if x > right}, GLASS_EDGE)
    return img


SPRITES = {
    'beer_glass': empty_glass(),
}


def png_bytes(img):
    h = len(img)
    w = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def scaled(img, factor):
    return [[px for px in row for _ in range(factor)] for row in img for _ in range(factor)]


def write(path, img):
    with open(path, 'wb') as f:
        f.write(png_bytes(img))


def main():
    os.makedirs(OUT, exist_ok=True)
    preview = sys.argv[2] if len(sys.argv) == 3 and sys.argv[1] == '--preview' else None
    for name, img in SPRITES.items():
        write(os.path.join(OUT, name + '.png'), img)
        if preview:
            os.makedirs(preview, exist_ok=True)
            write(os.path.join(preview, name + '.png'), scaled(img, 8))


if __name__ == '__main__':
    main()
