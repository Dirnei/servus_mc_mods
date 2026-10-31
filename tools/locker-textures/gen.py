"""Generates the Parcel Locker textures.

This script is the source of the files; edit the patterns here, not the PNGs.

    python tools/locker-textures/gen.py

Writes, under shuttles/src/main/resources/assets/servus_shuttles/textures:

- block/parcel_locker_front_<part>.png for the eight box parts: the yellow
  doors of the part's boxes, as the north face of the part's model maps them
  (u = 16 - x, v = 16 - y), each stamped with its size and number (S1, M2,
  L1). Pixels outside the doors are never shown. The door rectangles must
  match LockerLayout and ParcelLockerModels.
- block/parcel_locker_yellow.png: the plain yellow of door edges and the
  screen bezel.
- block/parcel_locker_screen.png, 32 by 32: the screen's LCD with
  SCAN / TO / OPEN, drawn on the 24 pixels from 4 to 28 that the LCD plate
  (x and y 2 to 14) maps.
- block/parcel_locker_hatch.png: the dock's back wall, a dark hatch where
  shuttles hand over, framed by steel.
- block/parcel_locker_lamp_off.png, _reserved.png, _ready.png: the lamps.
- block/parcel_locker_handle.png: the dark door handle.
- item/parcel_locker.png: the locker from the front.
"""
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles', 'textures')

STEEL = (44, 43, 47, 255)
STEEL_LIGHT = (61, 60, 64, 255)
STEEL_DARK = (30, 29, 33, 255)
YELLOW = (240, 184, 28, 255)
YELLOW_LIGHT = (255, 216, 74, 255)
YELLOW_EDGE = (214, 156, 10, 255)
YELLOW_SHADE = (196, 140, 8, 255)
STAMP = (90, 67, 0, 255)
LCD = (15, 31, 18, 255)
LCD_EDGE = (8, 18, 10, 255)
LCD_TEXT = (141, 255, 154, 255)
HATCH = (20, 20, 20, 255)
HATCH_RIM = (85, 85, 85, 255)

GLYPHS = {
    'S': ['###', '#..', '###', '..#', '###'],
    'M': ['#.#', '###', '#.#', '#.#', '#.#'],
    'L': ['#..', '#..', '#..', '#..', '###'],
    'C': ['###', '#..', '#..', '#..', '###'],
    'A': ['###', '#.#', '###', '#.#', '#.#'],
    'N': ['##.', '#.#', '#.#', '#.#', '#.#'],
    'T': ['###', '.#.', '.#.', '.#.', '.#.'],
    'O': ['###', '#.#', '#.#', '#.#', '###'],
    'P': ['###', '#.#', '###', '#..', '#..'],
    'E': ['###', '#..', '###', '#..', '###'],
    '1': ['.#.', '##.', '.#.', '.#.', '###'],
    '2': ['###', '..#', '###', '#..', '###'],
    '3': ['###', '..#', '.##', '..#', '###'],
    '4': ['#.#', '#.#', '###', '..#', '..#'],
    '5': ['###', '#..', '###', '..#', '###'],
    '6': ['###', '#..', '###', '#.#', '###'],
}

# The boxes as LockerLayout lists them: label, size, column, bottom and top in pixels from the locker's bottom.
BOXES = [
    ('S1', 0, 24, 32), ('S2', 0, 16, 24), ('S3', 0, 8, 16), ('S4', 0, 0, 8),
    ('S5', 1, 24, 32), ('S6', 1, 16, 24), ('M1', 1, 0, 16),
    ('M2', 3, 16, 32), ('M3', 3, 0, 16),
    ('L1', 4, 0, 32),
]

PARTS = {
    's_column_low': (0, 0), 's_column_high': (0, 1),
    'mixed_low': (1, 0), 'mixed_high': (1, 1),
    'm_column_low': (3, 0), 'm_column_high': (3, 1),
    'l_low': (4, 0), 'l_high': (4, 1),
}


def text(img, x, y, word, colour):
    for glyph in word:
        rows = GLYPHS[glyph]
        for dy, row in enumerate(rows):
            for dx, cell in enumerate(row):
                if cell == '#':
                    img.putpixel((x + dx, y + dy), colour)
        x += len(rows[0]) + 1


def text_width(word):
    return sum(len(GLYPHS[glyph][0]) + 1 for glyph in word) - 1


def steel(size=16):
    img = Image.new('RGBA', (size, size), STEEL)
    for y in range(size):
        for x in range(size):
            if (x * 7 + y * 13) % 11 == 0:
                img.putpixel((x, y), STEEL_LIGHT)
            elif (x * 5 + y * 3) % 13 == 0:
                img.putpixel((x, y), STEEL_DARK)
    return img


def door_piece(column, row, bottom, top):
    """The door rectangle of a box inside the block at column, row, in block pixels, or None."""
    low, high = row * 16, row * 16 + 16
    y0 = max(bottom + 1, low) - low
    y1 = min(top - 1, high) - low
    return (1, y0, 15, y1) if y1 > y0 else None


def door(img, rect, label, shows_label):
    x0, y0, x1, y1 = rect
    u0, u1 = 16 - x1, 16 - x0
    v0, v1 = 16 - y1, 16 - y0
    for v in range(v0, v1):
        for u in range(u0, u1):
            colour = YELLOW
            if u == u0 or v == v0:
                colour = YELLOW_LIGHT
            elif u == u1 - 1 or v == v1 - 1:
                colour = YELLOW_EDGE
            elif (u + v) % 7 == 0:
                colour = YELLOW_SHADE
            img.putpixel((u, v), colour)
    if shows_label:
        text(img, u0 + 1, v0 + max(0, (min(v1 - v0, 7) - 5) // 2), label, STAMP)


def front(name, column, row):
    img = steel()
    for label, box_column, bottom, top in BOXES:
        if box_column != column:
            continue
        rect = door_piece(column, row, bottom, top)
        if rect is None:
            continue
        label_here = top > row * 16 and top <= row * 16 + 16
        door(img, rect, label, label_here)
    save(img, 'block', 'parcel_locker_front_' + name)


def yellow():
    img = Image.new('RGBA', (16, 16), YELLOW)
    for y in range(16):
        for x in range(16):
            if (x + y) % 7 == 0:
                img.putpixel((x, y), YELLOW_SHADE)
            elif (x * 3 + y) % 11 == 0:
                img.putpixel((x, y), YELLOW_LIGHT)
    save(img, 'block', 'parcel_locker_yellow')


def screen():
    img = Image.new('RGBA', (32, 32), LCD_EDGE)
    for y in range(5, 27):
        for x in range(5, 27):
            img.putpixel((x, y), LCD)
    for line, word in enumerate(('SCAN', 'TO', 'OPEN')):
        text(img, 16 - (text_width(word) + 1) // 2, 7 + line * 7, word, LCD_TEXT)
    save(img, 'block', 'parcel_locker_screen')


def hatch():
    img = steel()
    for v in range(4, 13):
        for u in range(3, 13):
            rim = u in (3, 12) or v in (4, 12)
            img.putpixel((u, v), HATCH_RIM if rim else HATCH)
    for u in range(5, 11):
        img.putpixel((u, 8), STEEL_DARK)
    save(img, 'block', 'parcel_locker_hatch')


def solid(name, colour, rim):
    img = Image.new('RGBA', (16, 16), colour)
    for i in range(16):
        for edge in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(edge, rim)
    save(img, 'block', name)


def item():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(3, 15):
        for x in range(0, 16):
            img.putpixel((x, y), STEEL_DARK)
    columns = [(0, ['S', 'S', 'S', 'S']), (3, ['S', 'S', 'M', 'M']), (9, ['M', 'M', 'M', 'M']), (12, ['L', 'L', 'L', 'L'])]
    for x0, cells in columns:
        for i, size in enumerate(cells):
            for y in range(4 + i * 3, 6 + i * 3):
                for x in range(x0 + 1, x0 + 3 + (1 if x0 == 12 else 0)):
                    img.putpixel((x, y), YELLOW if (y + x) % 5 else YELLOW_LIGHT)
    for y in range(4, 9):
        for x in range(6, 9):
            img.putpixel((x, y), LCD_TEXT if (x + y) % 2 else LCD)
    for y in range(10, 14):
        for x in range(6, 9):
            img.putpixel((x, y), HATCH)
    for x in range(0, 16):
        img.putpixel((x, 2), YELLOW_EDGE)
        img.putpixel((x, 15), STEEL)
    save(img, 'item', 'parcel_locker')


def save(img, folder, name):
    path = os.path.join(TEXTURES, folder, name + '.png')
    img.save(path)


def main():
    for name, (column, row) in PARTS.items():
        front(name, column, row)
    yellow()
    screen()
    hatch()
    solid('parcel_locker_lamp_off', (58, 42, 0, 255), (36, 26, 0, 255))
    solid('parcel_locker_lamp_reserved', (255, 176, 0, 255), (214, 120, 0, 255))
    solid('parcel_locker_lamp_ready', (60, 255, 90, 255), (30, 190, 60, 255))
    solid('parcel_locker_handle', (40, 40, 44, 255), (22, 22, 24, 255))
    item()


if __name__ == '__main__':
    main()
