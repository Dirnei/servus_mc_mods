"""Generates the Servus: Notes notebook GUI sprites.

This script is the source of the files; edit the sprites here, not the PNGs.

    python tools/notes-textures/gen.py                      writes the sprites
    python tools/notes-textures/gen.py --preview <folder>   also writes 4x previews

Written into notes/src/main/resources/assets/servus_notes/textures/gui/sprites/notebook/.
The cover, the pages and the tabs are nine-slice sprites (a .png.mcmeta next to them), so the
notebook can grow with the screen; the ruled lines are drawn by the notebook, not the pages. Noise uses a fixed seed per sprite so every run writes the same pixels.
"""
import json
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'notes', 'src', 'main', 'resources', 'assets', 'servus_notes',
                   'textures', 'gui', 'sprites', 'notebook')
CLEAR = (0, 0, 0, 0)

# Sizes the NotebookScreen draws; keep them in step with its constants.
COVER = 64
COVER_BORDER = 12
PAGE = 64
PAGE_BORDER = 4
# The spine side keeps the fold shadow, so its border is as wide as the fold.
PAGE_SPINE = 24
TAB = 16
TAB_BORDER = 5

LEATHER = '#5a321b'
LEATHER_RIM = '#3e2111'
LEATHER_BAND = '#7a4a28'
STITCH = '#b08550'
PAPER = '#f1e4c3'
PAPER_EDGE = '#d9c69c'
CHECK_GREEN = '#2e6b1f'
INK = '#3b2a1a'
WARN_RED = '#a8322a'
TAB_COLOURS = {
    'red': '#a8322a',
    'green': '#3f7f3a',
    'blue': '#2f5aa0',
    'gold': '#b5842f',
}


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), c[3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(4))


def blank(w, h):
    return [[CLEAR for _ in range(w)] for _ in range(h)]


def rounded_out(x, y, w, h, r, top_only=False):
    """True when the pixel lies outside a rectangle with corners rounded by r."""
    if x < r:
        cx = r - 0.5
    elif x >= w - r:
        cx = w - r - 0.5
    else:
        return False
    if y < r:
        cy = r - 0.5
    elif y >= h - r and not top_only:
        cy = h - r - 0.5
    else:
        return False
    return (x - cx) ** 2 + (y - cy) ** 2 > r * r


# ---------- cover ----------

def cover():
    rnd = random.Random(1)
    img = blank(COVER, COVER)
    rim, band, body, stitch = hx(LEATHER_RIM), hx(LEATHER_BAND), hx(LEATHER), hx(STITCH)
    for y in range(COVER):
        for x in range(COVER):
            if rounded_out(x, y, COVER, COVER, 5):
                continue
            edge = min(x, y, COVER - 1 - x, COVER - 1 - y)
            if edge < 3:
                img[y][x] = rim
            elif edge < 5:
                img[y][x] = band
            else:
                img[y][x] = shade(body, 1 + rnd.uniform(-0.07, 0.07))
    # A stitched seam at a fixed inset; dashes repeat every 4 pixels so the tiled edges stay seamless.
    inset = 8
    for i in range(inset, COVER - inset):
        on = (i % 4) < 2
        if on:
            for x, y in ((i, inset), (i, COVER - 1 - inset), (inset, i), (COVER - 1 - inset, i)):
                img[y][x] = stitch
    return img


# ---------- pages ----------

def page(spine_right):
    rnd = random.Random(PAGE + (1 if spine_right else 0))
    img = blank(PAGE, PAGE)
    paper, edge = hx(PAPER), hx(PAPER_EDGE)
    dark = (0, 0, 0, 255)
    fold = PAGE_SPINE
    fold_alpha = 0x2a / 255 if spine_right else 0x30 / 255
    for y in range(PAGE):
        for x in range(PAGE):
            if spine_right and x < PAGE // 2 and rounded_out(x, y, PAGE, PAGE, 3):
                continue
            if not spine_right and x > PAGE // 2 and rounded_out(x, y, PAGE, PAGE, 3):
                continue
            c = shade(paper, 1 + rnd.uniform(-0.015, 0.015))
            outer = x if spine_right else PAGE - 1 - x
            if outer == 0 or y == 0 or y == PAGE - 1:
                c = edge
            spine = PAGE - 1 - x if spine_right else x
            if spine < fold:
                c = mix(c, dark, fold_alpha * (1 - spine / fold) ** 1.5)
            img[y][x] = c
    return img


def page_meta(spine_right):
    border = {'top': PAGE_BORDER, 'bottom': PAGE_BORDER,
              'left': PAGE_BORDER if spine_right else PAGE_SPINE,
              'right': PAGE_SPINE if spine_right else PAGE_BORDER}
    return {'gui': {'scaling': {'type': 'nine_slice', 'width': PAGE, 'height': PAGE, 'border': border}}}


# ---------- tabs ----------

def tab(colour, active):
    base = hx(colour)
    if not active:
        grey = (base[0] + base[1] + base[2]) / 3
        base = tuple(clamp((grey + (base[i] - grey) * 0.6) * 0.85) for i in range(3)) + (255,)
    img = blank(TAB, TAB)
    side = shade(base, 0.72)
    light = mix(base, (255, 255, 255, 255), 0.19)
    for y in range(TAB):
        for x in range(TAB):
            if rounded_out(x, y, TAB, TAB, 4, top_only=True):
                continue
            c = base
            if y == 0 or (y == 1 and (x < 2 or x > TAB - 3)):
                c = side
            elif y == 1:
                c = light
            elif x == 0 or x == TAB - 1:
                c = side
            if y >= TAB - 6:
                c = mix(c, (0, 0, 0, 255), 0.06 * (y - (TAB - 7)))
            img[y][x] = c
    return img


# ---------- task marks ----------

def from_rows(rows, colours):
    """An image from text rows; each character picks a colour, '.' is clear."""
    return [[colours[ch] if ch != '.' else CLEAR for ch in row] for row in rows]


def check():
    green = hx(CHECK_GREEN)
    dark = shade(green, 0.7)
    return from_rows([
        '........#',
        '.......##',
        '......##.',
        '#....##..',
        '##..##...',
        '.####....',
        '..##.....',
        '..d......',
        '.........',
    ], {'#': green, 'd': dark})


def spark():
    green = hx(CHECK_GREEN)
    light = mix(green, (255, 255, 255, 255), 0.35)
    return from_rows([
        '.#.',
        '#o#',
        '.#.',
    ], {'#': green, 'o': light})


def bin_icon(colour):
    ink = hx(colour)
    light = mix(ink, (255, 255, 255, 255), 0.35)
    return from_rows([
        '...###...',
        '#########',
        '.........',
        '.#######.',
        '.#o#o#o#.',
        '.#o#o#o#.',
        '.#o#o#o#.',
        '.#o#o#o#.',
        '..#####..',
    ], {'#': ink, 'o': light})


def nine_slice(width, height, border):
    return {'gui': {'scaling': {'type': 'nine_slice', 'width': width, 'height': height, 'border': border}}}


SPRITES = {
    'cover': cover(),
    'page_left': page(True),
    'page_right': page(False),
}
SPRITES['check'] = check()
SPRITES['spark'] = spark()
SPRITES['bin'] = bin_icon(INK)
SPRITES['bin_armed'] = bin_icon(WARN_RED)
META = {
    'cover': nine_slice(COVER, COVER, COVER_BORDER),
    'page_left': page_meta(True),
    'page_right': page_meta(False),
}
for _name, _colour in TAB_COLOURS.items():
    SPRITES['tab_' + _name] = tab(_colour, False)
    SPRITES['tab_' + _name + '_active'] = tab(_colour, True)
    META['tab_' + _name] = nine_slice(TAB, TAB, TAB_BORDER)
    META['tab_' + _name + '_active'] = nine_slice(TAB, TAB, TAB_BORDER)


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
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png_bytes(img))


def main():
    preview = sys.argv[2] if len(sys.argv) == 3 and sys.argv[1] == '--preview' else None
    for name, img in SPRITES.items():
        write(os.path.join(OUT, name + '.png'), img)
        if preview:
            write(os.path.join(preview, name + '.png'), scaled(img, 4))
    for name, meta in META.items():
        with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write(json.dumps(meta, indent=2) + '\n')


if __name__ == '__main__':
    main()
