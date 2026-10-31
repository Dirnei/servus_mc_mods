"""Generates the Parcel Locker textures.

This script is the source of the files; edit the patterns here, not the PNGs.

    python tools/locker-textures/gen.py

Writes, under shuttles/src/main/resources/assets/servus_shuttles/textures:

- block/parcel_locker_door_<box>.png, one per box (s01 to s06, m01 to m03,
  l01): the yellow door at 32 pixels per block, 30 wide and two pixels less
  than twice the box's height, in the top left of a 32 by 16, 32 or 64
  canvas, with the box id (S-01), the handle and a sheen. The renderer maps
  that rectangle onto the door's front; the lamp is drawn on top of it.
- block/parcel_locker_door_inside.png: the ochre inside of a door, 16 pixels
  per block, mapped from its top left corner.
- block/parcel_locker_yellow.png: the door edges.
- block/parcel_locker_carcass.png: the dark inside of a box.
- block/parcel_locker_ochre.png: the box frames, seen in the one pixel gaps
  right of and below every door.
- block/parcel_locker_side.png: the outer ends of the locker.
- block/parcel_locker_roof.png: the roof strip's front, back and ends in
  its top three rows; block/parcel_locker_roof_top.png its top and bottom.
- block/parcel_locker_dock.png: the dock front, the Inbound Station's side
  with a dark hatch where shuttles hand over.
- block/parcel_locker_screen_<state>.png at 32 pixels per block: steel bezel
  with a yellow edge, red scan window and green LCD. idle is animated (QR
  code with a blinking SCAN), hello leaves its middle line for the count the
  renderer writes, none, load and deny.
- block/parcel_locker_dock_lamp_off.png and the animated
  parcel_locker_dock_lamp_loading.png.
- block/parcel_locker_lamp.png: the white door lamp the renderer tints.
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles', 'textures')


def rgb(h, a=255):
    h = h.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a


OCHRE = rgb('b98c0e')
OCHRE_DARK = rgb('8a6a08')
OCHRE_MID = rgb('9c760a')
OCHRE_LIGHT = rgb('cf9e14')
D_LIGHT = rgb('ffd84a')
D_MID = rgb('f7c832')
D_BASE = rgb('f0b81c')
D_EDGE = rgb('e5a508')
D_SHADE = rgb('cf930a')
STAMP = rgb('5a4300')
HANDLE = rgb('5a4a1a')
HANDLE_HI = rgb('7a6630')
ROOF = rgb('d9a514')
ROOF_LIGHT = rgb('f0c040')
BEZ_A = rgb('3a4049')
BEZ_B = rgb('23272e')
BEZ_EDGE = rgb('e0a325')
BEZ_EDGE_D = rgb('b07c10')
LCD = rgb('0f1f12')
LCD_DIM = rgb('1a3a20')
LCD_TEXT = rgb('8dff9a')
LCD_MID = rgb('3f8a4a')
RED = rgb('a01818')
RED_HI = rgb('d83030')
CARCASS = rgb('1d1606')
HATCH_RIM = rgb('555555')
HATCH_TOP = rgb('0c0c0c')
HATCH = rgb('141414')
HATCH_LINE = rgb('080808')
DOCK_LAMP_OFF = rgb('5a3a00')
DOCK_LAMP_ON = rgb('ffb000')
DOCK_LAMP_DIM = rgb('8a5a00')
STEEL_DARK = rgb('1e1d21')
DOCK_PANEL = rgb('2c2b2f')

FONT = {
    'A': ['###', '#.#', '###', '#.#', '#.#'], 'B': ['##.', '#.#', '##.', '#.#', '##.'], 'C': ['###', '#..', '#..', '#..', '###'],
    'D': ['##.', '#.#', '#.#', '#.#', '##.'], 'E': ['###', '#..', '###', '#..', '###'], 'H': ['#.#', '#.#', '###', '#.#', '#.#'],
    'L': ['#..', '#..', '#..', '#..', '###'], 'M': ['#.#', '###', '#.#', '#.#', '#.#'], 'N': ['##.', '#.#', '#.#', '#.#', '#.#'],
    'O': ['###', '#.#', '#.#', '#.#', '###'], 'P': ['###', '#.#', '###', '#..', '#..'], 'R': ['##.', '#.#', '##.', '#.#', '#.#'],
    'S': ['###', '#..', '###', '..#', '###'], 'T': ['###', '.#.', '.#.', '.#.', '.#.'], 'U': ['#.#', '#.#', '#.#', '#.#', '###'],
    'Y': ['#.#', '#.#', '###', '.#.', '.#.'], '0': ['###', '#.#', '#.#', '#.#', '###'], '1': ['.#.', '##.', '.#.', '.#.', '###'],
    '2': ['###', '..#', '###', '#..', '###'], '3': ['###', '..#', '.##', '..#', '###'], '4': ['#.#', '#.#', '###', '..#', '..#'],
    '5': ['###', '#..', '###', '..#', '###'], '6': ['###', '#..', '###', '#.#', '###'], '-': ['...', '...', '###', '...', '...'],
    ' ': ['...', '...', '...', '...', '...'],
}

# The boxes as LockerLayout lists them: id and height in pixels at 16 per block.
BOXES = [('S-01', 8), ('S-02', 8), ('S-03', 8), ('S-04', 8), ('S-05', 8), ('S-06', 8),
         ('M-01', 16), ('M-02', 16), ('M-03', 16), ('L-01', 32)]


def text(img, word, x, y, colour):
    for glyph in word:
        for row, line in enumerate(FONT[glyph]):
            for column, cell in enumerate(line):
                if cell == '#':
                    img.putpixel((x + column, y + row), colour)
        x += 4


def text_width(word):
    return len(word) * 4 - 1


def noise(img, x0, y0, x1, y1, base, spread, seed):
    rnd = random.Random(seed)
    for y in range(y0, y1):
        for x in range(x0, x1):
            d = rnd.randint(-spread, spread)
            img.putpixel((x, y), tuple(max(0, min(255, base[i] + d)) for i in range(3)) + (255,))


def door(w, h, label):
    """The front of a door, w by h at 32 pixels per block."""
    img = Image.new('RGBA', (w, h))
    for y in range(h):
        t = y / max(1, h - 1)
        base = D_LIGHT if y < 2 else (D_MID if t < .3 else D_BASE)
        for x in range(w):
            img.putpixel((x, y), base)
    for y in range(h):
        img.putpixel((0, y), D_LIGHT)
        img.putpixel((w - 1, y), D_EDGE)
        img.putpixel((w - 2, y), D_EDGE)
    for x in range(w):
        img.putpixel((x, h - 1), D_SHADE)
        img.putpixel((x, h - 2), D_EDGE)
    for i in range(6):
        for k in (0, 1):
            x, y = w - 8 - i - k, h - 5 - i
            if 0 < x < w and 0 < y < h:
                img.putpixel((x, y), D_MID if h > 16 else D_BASE)
    text(img, label, 2, 2, STAMP)
    length = 6
    top = h // 2 - length // 2 + (1 if h <= 16 else 0)
    for y in range(top, top + length):
        img.putpixel((w - 5, y), HANDLE)
        img.putpixel((w - 4, y), HANDLE_HI if y == top else HANDLE)
    return img


def doors():
    for label, height in BOXES:
        w, h = 30, height * 2 - 2
        canvas = Image.new('RGBA', (32, {8: 16, 16: 32, 32: 64}[height]), D_EDGE)
        canvas.paste(door(w, h, label), (0, 0))
        save(canvas, 'block', 'parcel_locker_door_' + label.replace('-', '').lower())


def door_inside():
    img = Image.new('RGBA', (16, 32), OCHRE)
    noise(img, 0, 0, 16, 32, OCHRE, 6, 465)
    for x in range(16):
        img.putpixel((x, 0), OCHRE_LIGHT)
    save(img, 'block', 'parcel_locker_door_inside')


def yellow():
    img = Image.new('RGBA', (16, 16))
    noise(img, 0, 0, 16, 16, D_EDGE, 5, 21)
    save(img, 'block', 'parcel_locker_yellow')


def plain(name, base, spread, seed):
    img = Image.new('RGBA', (16, 16))
    noise(img, 0, 0, 16, 16, base, spread, seed)
    save(img, 'block', name)


def side():
    img = Image.new('RGBA', (16, 16))
    noise(img, 0, 0, 16, 16, OCHRE, 7, 5)
    for i in range(16):
        img.putpixel((i, 0), OCHRE_LIGHT)
        img.putpixel((0, i), OCHRE_DARK)
        img.putpixel((15, i), OCHRE_DARK)
    for i in range(1, 15):
        img.putpixel((i, 8), OCHRE_MID)
    save(img, 'block', 'parcel_locker_side')


def roof():
    img = Image.new('RGBA', (16, 16))
    noise(img, 0, 0, 16, 16, ROOF, 5, 9)
    for x in range(16):
        img.putpixel((x, 0), ROOF_LIGHT)
        img.putpixel((x, 2), OCHRE_DARK)
    save(img, 'block', 'parcel_locker_roof')
    top = Image.new('RGBA', (16, 16))
    noise(top, 0, 0, 16, 16, ROOF, 6, 4)
    save(top, 'block', 'parcel_locker_roof_top')


def dock():
    img = Image.open(os.path.join(TEXTURES, 'block', 'inbound_station_side.png')).convert('RGBA').copy()
    panel = Image.new('RGBA', (16, 16))
    noise(panel, 0, 0, 16, 16, DOCK_PANEL, 3, 11)
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] < 255:
                img.putpixel((x, y), panel.getpixel((x, y)))
    for y in range(5, 14):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (5, 13)
            img.putpixel((x, y), HATCH_RIM if edge else (HATCH_TOP if y < 8 else HATCH))
    for x in range(4, 12):
        img.putpixel((x, 6), HATCH_LINE)
    save(img, 'block', 'parcel_locker_dock')


QR_BITS = []
_seed = 7
for _y in range(11):
    _row = []
    for _x in range(11):
        _seed = (_seed * 1103515245 + 12345) & 0xffffffff
        finder = (_x < 3 and _y < 3) or (_x > 7 and _y < 3) or (_x < 3 and _y > 7)
        _row.append((not (_x % 10 == 1 and _y % 10 == 1)) if finder else (_seed >> 16) % 2 == 1)
    QR_BITS.append(_row)


def screen(lines, qr=False, blink_off=False):
    """One screen state, 32 by 32: the LCD spans x 3 to 29 and y 6 to 29."""
    img = Image.new('RGBA', (32, 32))
    for y in range(32):
        colour = tuple(int(BEZ_A[i] + (BEZ_B[i] - BEZ_A[i]) * y / 31) for i in range(3)) + (255,)
        for x in range(32):
            img.putpixel((x, y), colour)
    for i in range(32):
        for e in (0, 31):
            img.putpixel((i, e), BEZ_EDGE)
            img.putpixel((e, i), BEZ_EDGE)
        for e in (1, 30):
            img.putpixel((i, e), BEZ_EDGE_D if e == 30 else BEZ_EDGE)
            img.putpixel((e, i), BEZ_EDGE_D if e == 30 else BEZ_EDGE)
    for x in range(24, 28):
        for y in (3, 4):
            img.putpixel((x, y), RED_HI if y == 3 and x == 24 else RED)
    for y in range(6, 29):
        for x in range(3, 29):
            img.putpixel((x, y), LCD)
    for x in range(3, 29):
        img.putpixel((x, 6), LCD_DIM)
    if qr:
        for y in range(11):
            for x in range(11):
                if QR_BITS[y][x]:
                    img.putpixel((11 + x, 8 + y), LCD_TEXT)
        if not blink_off:
            text(img, 'SCAN', 16 - text_width('SCAN') // 2, 21, LCD_TEXT)
    else:
        y = 17 - (len(lines) * 6 - 1) // 2
        for word, colour in lines:
            text(img, word, 16 - text_width(word) // 2, y, colour)
            y += 6
    return img


def animated(frames, name, frametime):
    img = Image.new('RGBA', (frames[0].width, frames[0].height * len(frames)))
    for i, frame in enumerate(frames):
        img.paste(frame, (0, i * frames[0].height))
    save(img, 'block', name)
    path = os.path.join(TEXTURES, 'block', name + '.png.mcmeta')
    with open(path, 'w', encoding='utf-8', newline='\n') as out:
        out.write(json.dumps({'animation': {'frametime': frametime}}) + '\n')


def screens():
    animated([screen([], qr=True), screen([], qr=True, blink_off=True)], 'parcel_locker_screen_idle', 12)
    save(screen([('HELLO', LCD_TEXT), (' ', LCD_TEXT), ('OPEN', LCD_MID)]), 'block', 'parcel_locker_screen_hello')
    save(screen([('NO', LCD_TEXT), ('PARCEL', LCD_TEXT)]), 'block', 'parcel_locker_screen_none')
    save(screen([('LOAD', LCD_TEXT)]), 'block', 'parcel_locker_screen_load')
    save(screen([('NOT', RED_HI), ('YOURS', RED_HI)]), 'block', 'parcel_locker_screen_deny')


def lamp(colour, rim):
    img = Image.new('RGBA', (16, 16), colour)
    for i in range(16):
        for edge in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(edge, rim)
    return img


def lamps():
    save(lamp(DOCK_LAMP_OFF, rgb('3a2600')), 'block', 'parcel_locker_dock_lamp_off')
    animated([lamp(DOCK_LAMP_ON, rgb('d67800')), lamp(DOCK_LAMP_DIM, rgb('6a4200'))], 'parcel_locker_dock_lamp_loading', 5)
    save(lamp(rgb('ffffff'), rgb('d8d8d8')), 'block', 'parcel_locker_lamp')


def save(img, folder, name):
    img.save(os.path.join(TEXTURES, folder, name + '.png'))


def main():
    doors()
    door_inside()
    yellow()
    plain('parcel_locker_carcass', CARCASS, 4, 3)
    plain('parcel_locker_ochre', OCHRE_MID, 5, 11)
    side()
    roof()
    dock()
    screens()
    lamps()


if __name__ == '__main__':
    main()
