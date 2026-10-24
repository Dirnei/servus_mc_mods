"""Generates the item textures of the Purifier's seed residues.

This script is the source of these files; edit the shapes here, not the PNGs.

    python tools/purifier-textures/gen.py                    writes the textures
    python tools/purifier-textures/gen.py --preview out.png  also writes an 8x preview next to Sulfur and Salt Crystal

Written into alchemy/src/main/resources/assets/servus_alchemy/textures/item/:

- kandis.png: a cluster of clear rock sugar crystals, pale amber
- brimstone.png: a rough, faceted lump of yellow rock
- cinnabar.png: vermilion crystals growing out of grey rock
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'alchemy', 'src', 'main', 'resources', 'assets', 'servus_alchemy', 'textures', 'item')
SIZE = 16

KANDIS = {'o': (176, 122, 52), 'd': (224, 178, 106), 'b': (243, 218, 164), 'l': (252, 239, 210), 'h': (255, 255, 255)}
BRIMSTONE = {'o': (110, 84, 18), 'd': (168, 132, 30), 'b': (214, 178, 46), 'l': (240, 214, 92)}
CINNABAR = {'o': (104, 16, 18), 'b': (188, 40, 34), 'l': (232, 94, 72),
            'r': (66, 64, 68), 's': (108, 104, 108), 't': (142, 138, 142)}

KANDIS_SHAPE = [
    '................',
    '......oooo......',
    '.....ohhlbo.....',
    '....ohllbbdo....',
    '....olllbbdo....',
    '..oooollbddooo..',
    '.ohhlolbbddohlo.',
    '.ohllbodddolbbdo',
    '.ollbbdoooolbbdo',
    '.olbbbdohhlobbdo',
    '.olbbddohllbdddo',
    '..obbdolllbbddo.',
    '..oddoolbbbddo..',
    '...ooo.obbddo...',
    '.......oddoo....',
    '........oo......',
]

BRIMSTONE_SHAPE = [
    '................',
    '................',
    '.......oo.......',
    '.....oollo......',
    '....ollllloo....',
    '...ollllbbbbo...',
    '..ollllbbbbbdo..',
    '..olllbbbbbddo..',
    '.ollbbbbbbdddo..',
    '.olbbbbdbbddddo.',
    '.obbbbbdbbdddo..',
    '..obbbdbbbddddo.',
    '..obbdbbbdddo...',
    '...oddddddoo....',
    '....ooooooo.....',
    '................',
]

CINNABAR_SHAPE = [
    '................',
    '........o.......',
    '.......olo......',
    '...o...olbo.....',
    '..olo..olbo..o..',
    '..olbo.olbo.olo.',
    '..olbo.olbbooblo',
    '..olbboolbboblo.',
    '...olbbolbbbblo.',
    '...olbbolbbbbo..',
    '..rsolbbbbbbor..',
    '.rtssolbbbborsr.',
    '.rttsssooooossr.',
    '.rstsssstssssr..',
    '..rrssssssssrr..',
    '....rrrrrrrr....',
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
    return {'kandis': from_shape(KANDIS_SHAPE, KANDIS), 'brimstone': from_shape(BRIMSTONE_SHAPE, BRIMSTONE),
            'cinnabar': from_shape(CINNABAR_SHAPE, CINNABAR)}


def read_png(path):
    """Reads an 8 bit RGBA or palette free RGB PNG without filters other than the ones zlib output uses."""
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
        others = [read_png(os.path.join(OUT, name + '.png')) for name in ('salt_crystal', 'sulfur', 'quicksilver')]
        preview(sys.argv[2], list(made.values()) + others)


if __name__ == '__main__':
    main()
