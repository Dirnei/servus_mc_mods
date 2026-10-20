"""Generates the textures of the Heating Element and of the fireboxes that take it.

This script is the source of these files; edit the patterns here, not the PNGs. It reads the cold fire doors the
Delicacies and Brewery generators wrote and draws the heating coils over their slits, so run those first.

    python tools/heating-textures/gen.py                    writes the textures
    python tools/heating-textures/gen.py --preview out.png  also writes an 8x preview of every texture

Written:

- foundation .../textures/item/heating_element.png: the item, a ceramic plate with three coils and brass contacts
  (the mockup sprite `heating_element`)
- delicacies .../textures/block/stove_tile_door_heater.png and stove_tile_door_heater_cold.png: the cold stove door
  with the coils warm orange (the firebox is hot) and dull (it is not)
- brewery .../textures/block/brewhouse_door_heater.png and brewhouse_door_heater_cold.png: the same on the Brewhouse
  fire door
- delicacies .../textures/block/gauge_dial.png: the round brass thermometer without its needle; corners clear, the
  outer ring (column 1 and 14, row 1 and 14) is what the dial's edges show
- delicacies .../textures/block/gauge_needle.png: the needle, near black
- delicacies .../textures/block/firebox_port.png: the flat iron power port, brass rim and two socket slots
"""
import math
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))


def textures(mod):
    return os.path.join(ROOT, mod, 'src', 'main', 'resources', 'assets', 'servus_' + mod, 'textures')


S = 16
CLEAR = (0, 0, 0, 0)


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def R(*hexes):
    return [hx(h) for h in hexes]


IRON = R('#15171a', '#24272c', '#33373e', '#454a52', '#5b616a')
BRASS = R('#5e4514', '#9a7428', '#c89c44', '#e8c46a')
CER = R('#56606c', '#a9b2bd', '#d3dae2', '#eef2f6', '#fafcfe')
COIL_HOT = R('#ffd08a', '#ff8a2a', '#c8401a')
COIL_COLD = R('#7a4a36', '#5a3426', '#3e241c')
DIAL_FACE = hx('#f2ead6')
TICK = hx('#3a2a20')
TICK_HOT = hx('#c8301a')
NEEDLE = hx('#1a1210')


# ---------- PNG in and out ----------
def read_png(path):
    """Reads an 8 bit RGBA or RGB, non interlaced PNG into rows of RGBA tuples."""
    with open(path, 'rb') as f:
        data = f.read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(path + ' is not a PNG')
    pos = 8
    idat = b''
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, color, _, _, interlace = struct.unpack('>IIBBBBB', body)
            if depth != 8 or interlace or color not in (2, 6):
                raise ValueError(path + ': only 8 bit RGB or RGBA, non interlaced PNGs')
        elif kind == b'IDAT':
            idat += body
    channels = 4 if color == 6 else 3
    raw = zlib.decompress(idat)
    stride = width * channels
    rows = []
    previous = bytearray(stride)
    at = 0
    for _ in range(height):
        kind = raw[at]
        line = bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride
        for i in range(stride):
            left = line[i - channels] if i >= channels else 0
            up = previous[i]
            corner = previous[i - channels] if i >= channels else 0
            if kind == 1:
                line[i] = (line[i] + left) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + up) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (left + up) // 2) & 0xFF
            elif kind == 4:
                p = left + up - corner
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - corner)
                predictor = left if pa <= pb and pa <= pc else up if pb <= pc else corner
                line[i] = (line[i] + predictor) & 0xFF
        previous = line
        row = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            row.append(tuple(px) if channels == 4 else (px[0], px[1], px[2], 255))
        rows.append(row)
    return rows


def png_bytes(img):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)

    height = len(img)
    width = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write(mod, relative, img):
    path = os.path.join(textures(mod), relative)
    with open(path, 'wb') as f:
        f.write(png_bytes(img))
    print('wrote', os.path.relpath(path, ROOT))


# ---------- sprites ----------
class Spr:
    def __init__(self, rows=None):
        self.p = [list(row) for row in rows] if rows else [[CLEAR] * S for _ in range(S)]

    def set(self, x, y, c):
        if 0 <= x < S and 0 <= y < S:
            self.p[y][x] = c

    def rows(self):
        return self.p


def ellipse(cx, cy, rx, ry):
    return {(x, y) for y in range(S) for x in range(S) if ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1}


def rect(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


def boundary(mask):
    return {(x, y) for (x, y) in mask
            if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


LIGHT = (-0.55, -0.75, 0.65)


def heights(mask):
    outside = [(x, y) for y in range(-1, S + 1) for x in range(-1, S + 1) if (x, y) not in mask]
    d = {(x, y): min(math.hypot(x - ox, y - oy) for ox, oy in outside) for (x, y) in mask}
    m = max(d.values()) if d else 1
    return {k: math.sqrt(min(v / m, 1)) for k, v in d.items()}


def paint(spr, mask, ramp, rng, noise=0.12, k=2.2):
    """Shades a mask as a soft dome lit from the top left; ramp runs dark to light, ramp[0] is the outline."""
    h = heights(mask)
    lx, ly, lz = LIGHT
    ln = math.sqrt(lx * lx + ly * ly + lz * lz)
    edge = boundary(mask)
    n = len(ramp)
    for (x, y) in mask:
        if (x, y) in edge:
            spr.set(x, y, ramp[0])
            continue
        gx = h.get((x + 1, y), 0) - h.get((x - 1, y), 0)
        gy = h.get((x, y + 1), 0) - h.get((x, y - 1), 0)
        nx, ny, nz = -gx * k, -gy * k, 1
        nn = math.sqrt(nx * nx + ny * ny + nz * nz)
        dot = (nx * lx + ny * ly + nz * lz) / (nn * ln)
        v = (dot - 0.2) / 0.8 + rng.uniform(-noise, noise)
        idx = 1 + int(round(max(0, min(1, v)) * (n - 2)))
        spr.set(x, y, ramp[max(1, min(n - 1, idx))])


def tex_noise(ramp, seed, base=2, spread=1):
    rng = random.Random(seed)
    s = Spr()
    for y in range(S):
        for x in range(S):
            s.set(x, y, ramp[max(0, min(len(ramp) - 1, base + rng.randint(-spread, spread)))])
    return s


def coil(s, x0, x1, rows, colours):
    """A zigzag heating coil along each of {@code rows}, from x0 to x1, with a highlight at its start."""
    for y in rows:
        for x in range(x0, x1 + 1):
            up = (x - x0) % 4 < 2
            s.set(x, y - (1 if up else 0), colours[1] if up else colours[2])
        s.set(x0 + 1, y - 1, colours[0])


def heating_element():
    s = Spr()
    rng = random.Random('he')
    paint(s, rect(2, 3, 13, 12), CER, rng, noise=.03, k=.5)
    for y in (5, 8, 11):
        for x in range(3, 13):
            up = (x - 3) % 4 < 2
            s.set(x, y - (1 if up else 0), COIL_HOT[1] if up else COIL_HOT[2])
    for (x, y) in ((1, 13), (1, 14), (14, 13), (14, 14)):
        s.set(x, y, BRASS[2])
    for x in range(4, 12):
        s.set(x, 13, IRON[2])
        s.set(x, 14, IRON[3])
    return s


def door_with_coils(path, rows, colours):
    s = Spr(read_png(path)[:S])
    coil(s, 4, 11, rows, colours)
    return s


def gauge_dial():
    """The mockup's thermometer dial with its needle left out; the needle is a model of its own."""
    s = Spr()
    face = ellipse(8, 8, 7.5, 7.5)
    for (x, y) in face:
        s.set(x, y, DIAL_FACE)
    for (x, y) in boundary(face):
        s.set(x, y, BRASS[1])
    for (x, y) in boundary(face - boundary(face)):
        s.set(x, y, BRASS[3])
    for i in range(7):
        a = math.radians(210 - i * 40)
        x, y = 8 + math.cos(a) * 5.0, 8 - math.sin(a) * 5.0
        s.set(int(x), int(y), TICK_HOT if i >= 5 else TICK)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        s.set(x, y, BRASS[0])
    return s


def gauge_needle():
    s = Spr()
    for y in range(S):
        for x in range(S):
            s.set(x, y, NEEDLE)
    return s


def firebox_port():
    s = tex_noise(IRON, 'port', base=3)
    for (x, y) in boundary(rect(0, 0, 15, 15)):
        s.set(x, y, BRASS[1])
    for (x, y) in boundary(rect(1, 1, 14, 14)):
        s.set(x, y, IRON[1])
    for x0 in (4, 10):
        for (x, y) in rect(x0, 5, x0 + 1, 10):
            s.set(x, y, IRON[0])
        s.set(x0, 4, IRON[4])
        s.set(x0 + 1, 4, IRON[4])
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        s.set(x, y, BRASS[3])
    return s


# ---------- preview ----------
def preview(path, images):
    scale = 8
    gap = 8
    width = len(images) * (S * scale + gap) + gap
    height = S * scale + 2 * gap
    canvas = [[(48, 48, 48, 255)] * width for _ in range(height)]
    for i, img in enumerate(images):
        for y in range(S * scale):
            for x in range(S * scale):
                px = img[y // scale][x // scale]
                if px[3]:
                    canvas[gap + y][gap + i * (S * scale + gap) + x] = px
    with open(path, 'wb') as f:
        f.write(png_bytes(canvas))
    print('wrote preview', path)


def main():
    stove_door = os.path.join(textures('delicacies'), 'block', 'stove_tile_door.png')
    brew_door = os.path.join(textures('brewery'), 'block', 'brewhouse_door.png')
    out = [
        ('foundation', 'item/heating_element.png', heating_element()),
        ('delicacies', 'block/stove_tile_door_heater.png', door_with_coils(stove_door, (9, 12), COIL_HOT)),
        ('delicacies', 'block/stove_tile_door_heater_cold.png', door_with_coils(stove_door, (9, 12), COIL_COLD)),
        ('brewery', 'block/brewhouse_door_heater.png', door_with_coils(brew_door, (7, 10), COIL_HOT)),
        ('brewery', 'block/brewhouse_door_heater_cold.png', door_with_coils(brew_door, (7, 10), COIL_COLD)),
        ('delicacies', 'block/gauge_dial.png', gauge_dial()),
        ('delicacies', 'block/gauge_needle.png', gauge_needle()),
        ('delicacies', 'block/firebox_port.png', firebox_port()),
    ]
    for mod, relative, spr in out:
        write(mod, relative, spr.rows())
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], [spr.rows() for _, _, spr in out])


if __name__ == '__main__':
    main()
