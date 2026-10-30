"""Generates the item textures of the Venter Equinus products.

This script is the source of these files; edit the shapes here, not the PNGs.

    python tools/venter-textures/gen.py                    writes the textures
    python tools/venter-textures/gen.py --preview out.png  also writes an 8x preview next to Ash and Caput Mortuum

Written into alchemy/src/main/resources/assets/servus_alchemy/textures/item/:

- humus.png: a dark brown crumbly heap with a few lighter fibres
- grass_seed.png: three pale seeds, one with a green sprout
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'alchemy', 'src', 'main', 'resources', 'assets', 'servus_alchemy', 'textures', 'item')
SIZE = 16

HUMUS = {'o': (34, 22, 14), 'd': (58, 38, 24), 'b': (82, 56, 34), 'l': (110, 78, 48), 'f': (150, 122, 74)}
GRASS_SEED = {'o': (92, 74, 40), 'd': (170, 146, 92), 'b': (212, 192, 138), 'l': (238, 226, 184),
              'g': (52, 112, 34), 'h': (98, 170, 58), 'k': (30, 74, 22)}

HUMUS_SHAPE = [
    '................',
    '................',
    '................',
    '.......oo.......',
    '.....oobldo.....',
    '....odlbfbdo....',
    '...odbbdlbbdo...',
    '...obfbbdbldo...',
    '..odlbdbbfbddo..',
    '..obbdlbdbbldo..',
    '.odbfbbdlbdbbdo.',
    '.obdbbldbbfdbdo.',
    'odbbdbbbdlbbddo.',
    'oddbdddbddbdddo.',
    '.ooodddoddddoo..',
    '....oooooooo....',
]

GRASS_SEED_SHAPE = [
    '................',
    '.........h......',
    '........hg.h....',
    '........kghg....',
    '.........kg.....',
    '.........ok.....',
    '........olbo....',
    '...oo...olbdo...',
    '..olbo..obbdo...',
    '..olbdo..oddo...',
    '..obbdo...oo....',
    '...oddo..oo.....',
    '....oo..olbo....',
    '.......olbbdo...',
    '.......obbddo...',
    '........oooo....',
]


def png_bytes(img):
    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xffffffff)
    height = len(img)
    width = len(img[0])
    raw = b''.join(b'\x00' + b''.join(bytes(px) for px in row) for row in img)
    header = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


def blank():
    return [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]


def from_shape(shape, palette):
    img = blank()
    for y, row in enumerate(shape):
        for x, key in enumerate(row):
            if key in palette:
                img[y][x] = palette[key] + (255,)
    return img


def textures():
    return {'humus': from_shape(HUMUS_SHAPE, HUMUS), 'grass_seed': from_shape(GRASS_SEED_SHAPE, GRASS_SEED)}


def read_png(path):
    """Reads an 8 bit RGBA or RGB PNG."""
    with open(path, 'rb') as f:
        data = f.read()
    pos = 8
    width = height = 0
    colour = 6
    idat = b''
    while pos < len(data):
        length = struct.unpack('>I', data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if kind == b'IHDR':
            width, height, _, colour = struct.unpack('>IIBB', body[:10])
        elif kind == b'IDAT':
            idat += body
        pos += 12 + length
    channels = 4 if colour == 6 else 3
    raw = zlib.decompress(idat)
    stride = width * channels
    rows = []
    prev = bytearray(stride)
    for y in range(height):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            if f == 1:
                line[i] = (line[i] + a) & 0xff
            elif f == 2:
                line[i] = (line[i] + b) & 0xff
            elif f == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xff
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xff
        rows.append([tuple(line[x * channels:x * channels + channels]) + ((255,) if channels == 3 else ()) for x in range(width)])
        prev = line
    return rows


def preview(path, images, scale=8):
    gap = 2
    width = (SIZE + gap) * len(images) * scale
    out = [[(60, 60, 60, 255) for _ in range(width)] for _ in range(SIZE * scale)]
    for i, img in enumerate(images):
        for y in range(SIZE * scale):
            for x in range(SIZE * scale):
                px = img[y // scale][x // scale]
                if px[3] > 0:
                    out[y][i * (SIZE + gap) * scale + x] = px
    with open(path, 'wb') as f:
        f.write(png_bytes(out))


def main():
    made = textures()
    for name, img in made.items():
        with open(os.path.join(OUT, name + '.png'), 'wb') as f:
            f.write(png_bytes(img))
    if len(sys.argv) == 3 and sys.argv[1] == '--preview':
        others = [read_png(os.path.join(OUT, name + '.png')) for name in ('ash', 'caput_mortuum')]
        preview(sys.argv[2], list(made.values()) + others)


if __name__ == '__main__':
    main()
