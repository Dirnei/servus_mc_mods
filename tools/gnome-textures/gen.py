"""Generates the gnome textures after the elementals sheet.

This script is the source of these files; edit the colours and shapes here, not the PNGs.

    python tools/gnome-textures/gen.py                    writes the textures
    python tools/gnome-textures/gen.py --preview out.png  also writes an 8x preview of every texture

Written into alchemy/src/main/resources/assets/servus_alchemy/textures/:

- entity/gnome/<kind>.png: the 64x64 skin of each kind; the boxes and their texture offsets match GnomeModel
- item/<kind>_gnome_spawn_egg.png: an egg in the coat colour of the kind with red hat spots
"""
import math
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


# Shared colours of every gnome.
SKIN = '#f2c9a0'
NOSE = '#e0937a'
EYE = '#2a1e1a'
CHEEK = '#e79a86'
EYELID = '#c99a78'
BOOT = '#5a3a22'
TROUSERS = '#4a3a2e'
BELT = '#3b2616'
BRASS = '#e9b949'
BRIM = '#b02c25'
HAT_EVEN = '#c8332a'
HAT_ODD = '#d8392f'
HANDLE = '#8a5a2b'
STEEL = '#b4bcc6'
STEEL_DARK = '#8d959f'
IRON = '#3a3640'
FLAME = '#ffd36b'
CRYSTAL = '#d8f6ff'
CRYSTAL_DARK = '#9fdcec'


def noise(x, y, seed):
    value = math.sin(x * 12.9898 + y * 78.233 + seed * 37.719) * 43758.5453
    return value - math.floor(value)


def jitter(colour, amount, x, y, seed):
    return shade(colour, 1 + amount * (noise(x, y, seed) - 0.5))


def faces(u, v, w, h, d):
    """The texture rectangles (left, top, width, height) of a model box, as ModelPart.Cube maps them."""
    return {
        'down': (u + d, v, w, d),
        'up': (u + d + w, v, w, d),
        'west': (u, v + d, d, h),
        'north': (u + d, v + d, w, h),
        'east': (u + d + w, v + d, d, h),
        'south': (u + d + w + d, v + d, w, h),
    }


def paint_box(img, box, colour, amount=0.08, seed=1, details=None):
    """Fills every face of a box with its colour and a little noise; details(face, lx, ly) may return another colour."""
    u, v, w, h, d = box
    for face, (left, top, width, height) in faces(u, v, w, h, d).items():
        for y in range(math.floor(top), math.ceil(top + height)):
            for x in range(math.floor(left), math.ceil(left + width)):
                local = details(face, x - math.floor(left), y - math.floor(top)) if details else None
                img[y][x] = jitter(rgba(local or colour), amount, x, y, seed)


def face_details(face, x, y):
    if face != 'north':
        return None
    if y == 2 and x in (1, 4):
        return EYE
    if y == 3 and x in (0, 5):
        return CHEEK
    return None


def buckle(face, x, y):
    return BRASS if face == 'north' and x in (2, 3) else None


def beard_details(colours):
    def details(face, x, y):
        return colours['beard_dark'] if noise(x, y, 7) < 0.25 else None
    return details


def coat_details(colours):
    def details(face, x, y):
        return colours['coat_dark'] if noise(x, y, 11) < 0.2 else None
    return details


def flame_details(face, x, y):
    return '#ffe9a8' if noise(x, y, 5) < 0.3 else None


def crystal_details(face, x, y):
    return CRYSTAL_DARK if (x + y) % 3 == 0 else None


HAT_STEPS = [((24, 23), (6, 2.2, 5)), ((46, 23), (5, 2.2, 4)), ((0, 31), (4, 2, 3.2)), ((16, 31), (3, 2, 2.4)),
             ((56, 4), (2, 1.8, 1.8)), ((0, 40), (1.2, 1.6, 1.2))]


def skin(kind):
    """One gnome texture; every box is (texture u, texture v, width, height, depth) as in GnomeModel."""
    colours = KINDS[kind]
    img = blank(64, 64)
    paint_box(img, (0, 0, 7, 1, 6), BRIM, 0.1, 2)
    paint_box(img, (26, 0, 6, 5, 5), SKIN, 0.08, 3, face_details)
    paint_box(img, (48, 0, 2, 3, 2), TROUSERS, 0.1, 4)
    paint_box(img, (56, 0, 1.8, 1.5, 1.8), SKIN, 0.08, 5)
    paint_box(img, (48, 5, 1.6, 1.6, 1.2), NOSE, 0.06, 6)
    paint_box(img, (0, 10, 0.9, 0.9, 11), HANDLE, 0.12, 8)
    paint_box(img, (24, 10, 6, 5, 4), colours['coat'], 0.1, 9, coat_details(colours))
    paint_box(img, (44, 10, 6.4, 4.5, 1.4), colours['beard'], 0.12, 10, beard_details(colours))
    paint_box(img, (44, 17, 4, 1, 0.6), colours['beard'], 0.12, 12, beard_details(colours))
    paint_box(img, (54, 17, 1, 1, 0.15), EYELID, 0.0, 13)
    paint_box(img, (58, 17, 1.6, 1.6, 0.3), BRASS, 0.1, 14)
    paint_box(img, (24, 19, 0.4, 1.65, 0.4), IRON, 0.1, 15)
    paint_box(img, (28, 19, 0.45, 2.65, 0.45), IRON, 0.1, 16)
    paint_box(img, (0, 23, 6.3, 1, 4.3), BELT, 0.1, 17, buckle)
    for i, ((u, v), (w, h, d)) in enumerate(HAT_STEPS):
        paint_box(img, (u, v, w, h, d), HAT_EVEN if i % 2 == 0 else HAT_ODD, 0.1, 20 + i)
    paint_box(img, (28, 31, 2.4, 1.5, 3), BOOT, 0.12, 30)
    paint_box(img, (40, 31, 2, 4.2, 2), colours['coat'], 0.1, 31, coat_details(colours))
    paint_box(img, (48, 31, 1.1, 7, 1.1), STEEL, 0.08, 32)
    paint_box(img, (56, 31, 1.6, 2.4, 1.6), FLAME, 0.1, 33, flame_details)
    paint_box(img, (8, 40, 1, 1.2, 1.4), STEEL_DARK, 0.08, 34)
    paint_box(img, (14, 40, 2.6, 0.5, 2.6), IRON, 0.1, 35)
    paint_box(img, (26, 40, 1.8, 3, 1.8), CRYSTAL, 0.05, 36, crystal_details)
    return img


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
        img = skin(kind)
        write(os.path.join(TEXTURES, 'entity', 'gnome', kind + '.png'), img)
        images.append(img)
    for kind in KINDS:
        img = egg(kind)
        write(os.path.join(TEXTURES, 'item', kind + '_gnome_spawn_egg.png'), img)
        images.append(img)
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], images)


if __name__ == '__main__':
    main()
