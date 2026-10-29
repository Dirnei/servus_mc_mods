"""Generates the Muffler item texture.

This script is the source of the file; edit the shapes here, not the PNG.

    python tools/shuttle-sounds/muffler_texture.py                    writes the texture
    python tools/shuttle-sounds/muffler_texture.py --preview out.png  also writes an 8x preview

Written into shuttles/src/main/resources/assets/servus_shuttles/textures/item/muffler.png:
the copper upgrade card of the Shuttles upgrades (frame, dark face, gold
rivets) with a wool white speaker and a red cross beside it.
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles', 'textures', 'item')
SIZE = 16

# The card, row by row: a outline, b copper frame, c rivet, d inner rim, e face; the same as the Handling Upgrade.
CARD = [
    '..aaaaaaaaaaaa..',
    '.abbbbbbbbbbbba.',
    'abcddddddddddcba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abdeeeeeeeeeedba',
    'abcddddddddddcba',
    '.abbbbbbbbbbbba.',
    '..aaaaaaaaaaaa..',
]
# The glyph over the face, from (4, 4): s speaker, t its shaded side, x cross, y its shaded side.
GLYPH = [
    '...s....',
    '..ss....',
    'tsss.x.x',
    'tsss..y.',
    'tsss.x.x',
    'tsss....',
    '..ss....',
    '...s....',
]
GLYPH_AT = (4, 4)
COLOURS = {
    'a': (0x7a, 0x3e, 0x28),
    'b': (0xc4, 0x6e, 0x48),
    'c': (0xe8, 0xc8, 0x78),
    'd': (0x26, 0x28, 0x30),
    'e': (0x36, 0x3a, 0x44),
    's': (0xf2, 0xee, 0xe6),
    't': (0xb8, 0xb2, 0xa8),
    'x': (0xf0, 0x6a, 0x58),
    'y': (0xb8, 0x40, 0x34),
}


def png_bytes(img):
    h = len(img)
    w = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def pixel(key):
    return (0, 0, 0, 0) if key == '.' else COLOURS[key] + (255,)


def texture():
    img = [[pixel(key) for key in row] for row in CARD]
    gx, gy = GLYPH_AT
    for dy, row in enumerate(GLYPH):
        for dx, key in enumerate(row):
            if key != '.':
                img[gy + dy][gx + dx] = pixel(key)
    return img


def scaled(img, factor):
    return [[px for px in row for _ in range(factor)] for row in img for _ in range(factor)]


def main():
    img = texture()
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, 'muffler.png'), 'wb') as f:
        f.write(png_bytes(img))
    if len(sys.argv) == 3 and sys.argv[1] == '--preview':
        with open(sys.argv[2], 'wb') as f:
            f.write(png_bytes(scaled(img, 8)))


if __name__ == '__main__':
    main()
