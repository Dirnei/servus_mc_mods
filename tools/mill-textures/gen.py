"""Generates the textures of the Mill, the Mixer, the Hand Crank and the Electric Motor.

This script is the source of these files; edit the patterns here, not the PNGs.

    python tools/mill-textures/gen.py                    writes the textures
    python tools/mill-textures/gen.py --preview out.png  also writes an 8x preview of the block textures

Written into foundation/src/main/resources/assets/servus_foundation/textures/:

- block/heavy_iron.png: the family's heavy iron, copied from Servus: Shuttles' heavy_rack_iron.png,
  which Foundation cannot reference
- block/brass.png: brass for hubs and rims
- block/mill_hopper_top.png: the hopper's mouth seen from above, a brass rim round a dark funnel;
  the rim box shows pixels 4 to 12 of it
- block/mill_chute.png: steel with the chute's dark mouth where the chute's front face maps (x 6..10, y 11.5..14.5)
- block/electric_motor_grille.png: the motor's round back grille with see-through gaps in front of the fan;
  the grille box shows pixels 4 to 12 of it
- block/mixer_mix.png: the dry mix surface in the Mixer's tub
- block/mixer_port.png: brass with the fluid port's dark mouth where the port's back face maps (x 6.75..9.25, y 6.5..9.5)
- gui/mill.png: the burner machine screen with the input slot moved down beside the arrow, fuel and flame gone
- gui/mixer.png: the Mill screen with a 2x2 grid of input slots and a fluid gauge frame right of the output
- gui/electric_motor.png: the coal generator screen with its slot moved to the middle as the upgrade slot, flame gone
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'foundation', 'src', 'main', 'resources', 'assets', 'servus_foundation', 'textures')
HEAVY_RACK_IRON = os.path.join(ROOT, 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles', 'textures', 'block',
                               'heavy_rack_iron.png')
SIZE = 16


def hexc(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


IRON = [hexc(h) for h in ('#15171a', '#24272c', '#33373e', '#454a52', '#5b616a')]
BRASS = [hexc(h) for h in ('#5e4514', '#9a7428', '#c89c44', '#e8c46a')]
CLEAR = (0, 0, 0, 0)


# ---------- PNG in and out ----------
def read_png(path):
    """Reads an 8 bit, non interlaced PNG into rows of RGBA tuples."""
    with open(path, 'rb') as f:
        data = f.read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(path + ' is not a PNG')
    pos = 8
    idat = b''
    palette = []
    alpha = b''
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, color, _, _, interlace = struct.unpack('>IIBBBBB', body)
            if depth != 8 or interlace:
                raise ValueError(path + ': only 8 bit, non interlaced PNGs')
        elif kind == b'PLTE':
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b'tRNS':
            alpha = body
        elif kind == b'IDAT':
            idat += body
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
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
            if color == 6:
                row.append(tuple(px))
            elif color == 2:
                row.append((px[0], px[1], px[2], 255))
            elif color == 3:
                index = px[0]
                r, g, b = palette[index]
                row.append((r, g, b, alpha[index] if index < len(alpha) else 255))
            elif color == 0:
                row.append((px[0], px[0], px[0], 255))
            else:
                row.append((px[0], px[0], px[0], px[1]))
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


def write(relative, img):
    path = os.path.join(ASSETS, relative)
    with open(path, 'wb') as f:
        f.write(png_bytes(img))
    print('wrote', os.path.relpath(path, ROOT))


def copy(img):
    return [list(row) for row in img]


# ---------- pattern helpers ----------
def noise(x, y, seed):
    """A fixed pseudo random value in 0..1 per pixel."""
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF


def speckled(ramp, base, seed, dark=0.12, light=0.06):
    """The ramp's base colour with a sprinkling of its neighbours: dark and light are their shares."""
    img = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            n = noise(x, y, seed)
            shift = -1 if n < dark else 1 if n > 1 - light else 0
            row.append(ramp[max(0, min(len(ramp) - 1, base + shift))])
        img.append(row)
    return img


def frame(img, x0, y0, x1, y1, color):
    for x in range(x0, x1 + 1):
        img[y0][x] = color
        img[y1][x] = color
    for y in range(y0, y1 + 1):
        img[y][x0] = color
        img[y][x1] = color


# ---------- block textures ----------
def brass():
    return speckled(BRASS, 2, 7)


def hopper_top():
    """Shown on the rim box's top, pixels 4..11: brass lip, darker inner edge, then the funnel going dark to the middle."""
    img = brass()
    frame(img, 4, 4, 11, 11, BRASS[3])
    frame(img, 5, 5, 10, 10, BRASS[1])
    for y in range(6, 10):
        for x in range(6, 10):
            depth = min(x - 6, 9 - x, y - 6, 9 - y)
            img[y][x] = IRON[2] if depth == 0 else IRON[0]
    return img


def chute(steel):
    """Steel with the chute's mouth: the chute's front face shows x 6..10, y 11.5..14.5 of it."""
    img = copy(steel)
    for x in range(6, 10):
        img[11][x] = IRON[4]
        img[15][x] = IRON[4]
    for y in range(12, 15):
        img[y][6] = IRON[4]
        img[y][9] = IRON[4]
        for x in range(7, 9):
            img[y][x] = IRON[0] if y > 12 else IRON[1]
    return img


def mixer_mix():
    """The dry mix in the Mixer's tub: grainy grey brown, a few lighter grains and darker clods."""
    ramp = [hexc(h) for h in ('#4a4038', '#5e544a', '#72675b', '#877c6e', '#9c9283')]
    img = speckled(ramp, 2, 11, dark=0.25, light=0.2)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 23)
            if n < 0.06:
                img[y][x] = ramp[0]
            elif n > 0.95:
                img[y][x] = ramp[4]
    return img


def mixer_port():
    """The port stub's back face shows pixels 6.75 to 9.25 across and 6.5 to 9.5 down: a brass lip round a dark mouth."""
    img = brass()
    frame(img, 6, 6, 9, 9, BRASS[1])
    for x in range(6, 10):
        img[6][x] = BRASS[3]
    for y in range(7, 9):
        for x in range(7, 9):
            img[y][x] = IRON[0] if y == 8 else IRON[1]
    return img


def motor_grille(steel):
    """Shown on the grille box's back, pixels 4..11: a round grille of slats with open gaps and a brass bearing."""
    img = copy(steel)
    for y in range(4, 12):
        for x in range(4, 12):
            r = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5
            if r > 3.9:
                img[y][x] = IRON[2]
            elif r > 3.1:
                img[y][x] = IRON[4]
            elif y % 2 == 0:
                img[y][x] = IRON[3]
            else:
                img[y][x] = CLEAR
    for y in range(7, 9):
        for x in range(7, 9):
            img[y][x] = BRASS[2]
    return img


# ---------- GUI textures ----------
def gui_from(name):
    return read_png(os.path.join(ASSETS, 'gui', name + '.png'))


def clear(img, x0, y0, x1, y1, background):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img[y][x] = background


def move_slot(img, sx, sy, dx, dy, background):
    """Moves the 18 x 18 slot frame at (sx, sy) to (dx, dy)."""
    slot = [img[sy + y][sx:sx + 18] for y in range(18)]
    clear(img, sx, sy, sx + 17, sy + 17, background)
    for y in range(18):
        img[dy + y][dx:dx + 18] = slot[y]


def mill_gui():
    img = gui_from('burner_machine')
    background = img[40][40]
    clear(img, 55, 35, 72, 51, background)
    clear(img, 55, 52, 72, 69, background)
    move_slot(img, 55, 16, 55, 34, background)
    return img


def stretched_slot(slot, width, height):
    """The 18 x 18 slot frame stretched to width x height: edges and corners kept, the middle row and column repeated."""
    def source(at, size):
        return 0 if at == 0 else 17 if at == size - 1 else 8
    return [[slot[source(y, height)][source(x, width)] for x in range(width)] for y in range(height)]


def paste(img, part, x0, y0):
    for y, row in enumerate(part):
        img[y0 + y][x0:x0 + len(row)] = row


def mixer_gui():
    """The Mill screen with its input slot replaced by a 2x2 grid of inputs and a gauge frame right of the output."""
    img = gui_from('mill')
    background = img[40][40]
    slot = [img[34 + y][55:55 + 18] for y in range(18)]
    clear(img, 55, 34, 72, 51, background)
    for dx, dy in ((33, 25), (51, 25), (33, 43), (51, 43)):
        paste(img, slot, dx, dy)
    paste(img, stretched_slot(slot, 18, 54), 147, 16)
    return img


def motor_gui():
    img = gui_from('coal_generator')
    background = img[40][40]
    clear(img, 55, 34, 72, 51, background)
    move_slot(img, 55, 52, 79, 34, background)
    return img


def preview(path, images):
    scale = 8
    gap = 4
    width = len(images) * (SIZE * scale + gap) + gap
    height = SIZE * scale + 2 * gap
    canvas = [[(90, 100, 110, 255)] * width for _ in range(height)]
    for i, img in enumerate(images):
        for y in range(SIZE * scale):
            for x in range(SIZE * scale):
                px = img[y // scale][x // scale]
                if px[3]:
                    canvas[gap + y][gap + i * (SIZE * scale + gap) + x] = px
    with open(path, 'wb') as f:
        f.write(png_bytes(canvas))
    print('wrote', path)


def main():
    steel = read_png(os.path.join(ASSETS, 'block', 'electric_pump_steel.png'))
    blocks = {
        'block/heavy_iron.png': read_png(HEAVY_RACK_IRON),
        'block/brass.png': brass(),
        'block/mill_hopper_top.png': hopper_top(),
        'block/mill_chute.png': chute(steel),
        'block/electric_motor_grille.png': motor_grille(steel),
        'block/mixer_mix.png': mixer_mix(),
        'block/mixer_port.png': mixer_port(),
    }
    for name, img in blocks.items():
        write(name, img)
    write('gui/mill.png', mill_gui())
    write('gui/mixer.png', mixer_gui())
    write('gui/electric_motor.png', motor_gui())
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], list(blocks.values()))


if __name__ == '__main__':
    main()
