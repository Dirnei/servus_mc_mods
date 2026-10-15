"""Generates the gnome textures after the elementals sheet.

This script is the source of these files; edit the colours and shapes here, not the PNGs.

    python tools/gnome-textures/gen.py                    writes the textures
    python tools/gnome-textures/gen.py --preview out.png  also writes an 8x preview of every texture

Written into alchemy/src/main/resources/assets/servus_alchemy/textures/:

- item/<kind>_gnome_spawn_egg.png: an egg in the coat colour of the kind with red hat spots
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'alchemy', 'src', 'main', 'resources', 'assets', 'servus_alchemy', 'textures')

HAT = '#d8392f'
HAT_DARK = '#9e2621'

# Per kind: coat and its dark shade, beard and its dark shade, as on the sheet.
KINDS = {
    'miner': {'coat': '#3c64b4', 'coat_dark': '#2a4785', 'beard': '#f4f1ea', 'beard_dark': '#cfc8bb'},
    'lantern': {'coat': '#5e7a3a', 'coat_dark': '#435a28', 'beard': '#8a5a33', 'beard_dark': '#6b4425'},
    'house': {'coat': '#2f7a5f', 'coat_dark': '#1f5843', 'beard': '#dcdad3', 'beard_dark': '#b9b6ad'},
}


def rgba(hex_colour, alpha=255):
    value = hex_colour.lstrip('#')
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def shade(colour, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in colour[:3]) + (colour[3],)


def blank(width, height):
    return [[(0, 0, 0, 0)] * width for _ in range(height)]


def png_bytes(img):
    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xffffffff)

    height = len(img)
    width = len(img[0])
    raw = b''.join(b'\x00' + b''.join(bytes(p) for p in row) for row in img)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as out:
        out.write(png_bytes(img))


# The egg outline, one row per pixel row: first and last filled column.
EGG_ROWS = {2: (7, 8), 3: (6, 9), 4: (5, 10), 5: (5, 10), 6: (4, 11), 7: (4, 11), 8: (3, 12), 9: (3, 12),
            10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (5, 10), 14: (6, 9)}
EGG_SPOTS = [(6, 5), (9, 7), (5, 10), (10, 11), (7, 12), (8, 9)]


def egg(kind):
    colours = KINDS[kind]
    coat = rgba(colours['coat'])
    coat_dark = rgba(colours['coat_dark'])
    outline = shade(coat_dark, 0.6)
    img = blank(16, 16)
    for y, (first, last) in EGG_ROWS.items():
        for x in range(first, last + 1):
            edge = x in (first, last) or y in (2, 14) or y - 1 not in EGG_ROWS or y + 1 not in EGG_ROWS
            img[y][x] = outline if edge else (coat_dark if x > (first + last) / 2 + 1 or y > 12 else coat)
    for x, y in EGG_SPOTS:
        img[y][x] = rgba(HAT)
        if x + 1 <= EGG_ROWS[y][1] - 1:
            img[y][x + 1] = rgba(HAT_DARK)
    img[4][7] = shade(coat, 1.3)
    img[5][6] = shade(coat, 1.3)
    return img


def preview(path, images):
    scale = 8
    gap = 4
    width = sum(len(img[0]) * scale + gap for img in images)
    height = max(len(img) * scale for img in images)
    out = [[(40, 36, 44, 255)] * width for _ in range(height)]
    left = 0
    for img in images:
        for y, row in enumerate(img):
            for x, pixel in enumerate(row):
                if pixel[3]:
                    for dy in range(scale):
                        for dx in range(scale):
                            out[y * scale + dy][left + x * scale + dx] = pixel
        left += len(img[0]) * scale + gap
    write(path, out)


def main():
    images = []
    for kind in KINDS:
        img = egg(kind)
        write(os.path.join(TEXTURES, 'item', kind + '_gnome_spawn_egg.png'), img)
        images.append(img)
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], images)


if __name__ == '__main__':
    main()
