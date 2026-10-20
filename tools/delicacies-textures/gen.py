"""Generates the Servus: Delicacies item textures.

This script is the source of the files; edit the grids here, not the PNGs.

    python tools/delicacies-textures/gen.py                      writes the textures
    python tools/delicacies-textures/gen.py --preview <folder>   also writes 8x previews

Written into delicacies/src/main/resources/assets/servus_delicacies/textures/item/.
Sprites are drawn as character grids; a grid that lists only its left half is
mirrored into the right half.
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'delicacies', 'src', 'main', 'resources', 'assets', 'servus_delicacies', 'textures', 'item')

# Baked dough from the dark outline to the light crust, plus coarse salt.
DOUGH = {
    'A': (58, 26, 7),
    'G': (94, 44, 14),
    'F': (127, 61, 20),
    'E': (157, 83, 31),
    'C': (187, 107, 46),
    'B': (207, 134, 66),
    'D': (247, 244, 236),
}

# The left half of the pretzel: two loops over the crossed arms, with salt on the top bends.
PRETZEL_LEFT = [
    "........",
    "...AAAA.",
    "..ABCCDA",
    ".ABCAACE",
    ".ACA..AE",
    ".ACA..AE",
    ".ADA..AE",
    ".AECAAEA",
    ".ABECA..",
    ".ACEGA..",
    ".AEDGA..",
    ".ABEEEAA",
    "..AGEEEE",
    "...AAGGG",
    ".....AAA",
    "........",
]


def mirrored(left):
    return [row + row[::-1] for row in left]


def sprite(rows, palette):
    return [[palette[c] + (255,) if c != '.' else (0, 0, 0, 0) for c in row] for row in rows]


SPRITES = {
    'pretzel': sprite(mirrored(PRETZEL_LEFT), DOUGH),
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
