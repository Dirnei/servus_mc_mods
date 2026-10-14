"""Generates the Utility Duct textures and the line item icons.

This script is the source of these files; edit the patterns here, not the PNGs.

    python tools/duct-textures/gen.py           writes the textures
    python tools/duct-textures/gen.py --check   checks the band split only

Block textures are 16 x 16. The game maps them so u runs along the duct
(one block per tile, so every pattern tiles on u) and a face of thickness t
shows rows 8 - t/2 to 8 + t/2. Written into
foundation/src/main/resources/assets/servus_foundation/textures/:

- block/duct_<line>.png, _6, _8: one line's strip cut for faces 4, 6 and 8 px thick
- block/duct_<lines>.png: the bands of a set of two to four lines, side by side
- _duplex and _fast copies of every texture with the data line, for the cable tiers
- block/duct_fitting_<r>.png: the moulded rubber fitting for a centre of radius r
- block/duct_ring.png: the endpoint ring, grey so the game can tint it
- item/<line item>.png: the cross-section icons
"""
import math
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'foundation', 'src', 'main', 'resources', 'assets', 'servus_foundation', 'textures')


# ------------------------------------------------------------------ pixels
def hexc(h):
    h = h.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255


def pal(*hs):
    return [hexc(h) for h in hs]


def ramp(p, i):
    return p[max(0, min(len(p) - 1, int(i)))]


def noise(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def jitter(x, y, seed=0, p_up=0.18, p_dn=0.18):
    r = noise(x, y, seed)
    return 1 if r < p_up else (-1 if r > 1 - p_dn else 0)


def blank():
    return [[(0, 0, 0, 0)] * 16 for _ in range(16)]


def png_bytes(img):
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


STEEL = pal('#24262b', '#383a40', '#4b4e54', '#5d6066', '#717479', '#8a8d93', '#a6a9ae')
COPPER = pal('#4a2410', '#7a3b1c', '#a8582e', '#d07a45', '#eea06a', '#ffc896')
BRASS = pal('#4a3410', '#7a5a1c', '#a8862e', '#d0aa45', '#eecc6a', '#fff09a')
ORANGE = pal('#5e3009', '#8f4c10', '#bf6614', '#e8841f', '#f6a142', '#fcc274')
RED = pal('#3e0b0a', '#701614', '#a0221e', '#c8302a', '#e0544a', '#f2867c')
BLUE = pal('#0f2050', '#1a3780', '#2a54b4', '#3c74e0', '#64a0f0', '#a2c8fa')
WHITE = pal('#6e7076', '#9a9ca2', '#bec0c5', '#dcdee2', '#eff0f2', '#ffffff')
GRAY = pal('#26282c', '#3a3c41', '#51545a', '#6a6d74', '#878a91', '#a6a9af')
BLACK = pal('#060607', '#0f1012', '#18191c', '#222327', '#2e3034', '#42444a')
RUBBER = pal('#0c0c0e', '#151518', '#1e1f23', '#2a2b30', '#383a3f', '#4c4e54')
GLASS = pal('#0b1014', '#121a20', '#1a252d', '#263540', '#3a5060', '#7fa6b8')
CYAN = pal('#0a3a40', '#127078', '#1aa6b0', '#3cd2dc', '#8af0f4', '#d8ffff')
WATER = pal('#0e2460', '#163a8c', '#2152b4', '#3270d8', '#5a98ee', '#a8d0fa')

# Line ids in the game's order; a data line is drawn as one of the tiers.
LINES = ['item', 'energy', 'fluid', 'data']
TIERS = {'data': None, 'duplex': 'duplex', 'fast': 'fast'}
ICONS = {'item': 'item_pipe', 'fluid': 'fluid_pipe', 'energy': 'power_cable',
         'data': 'data_cable', 'duplex': 'duplex_data_cable', 'fast': 'fast_data_cable'}
CORES = {'data': 1, 'duplex': 2, 'fast': 3}


# ------------------------------------------------------------------ strips
def edge_shift(k, n):
    return 1 if k == 0 else (-1 if k == n - 1 else 0)


def strip(look):
    """The pattern of a line at texel u along the duct, row k of a band n rows thick, texture row y."""
    def fn(u, k, n, y):
        e = edge_shift(k, n)
        if u in (0, 1):
            sleeve = BRASS if look == 'fluid' else STEEL
            return ramp(sleeve, (5 if u == 0 else 3) + e)
        if look == 'item':
            return ramp(ORANGE, (4 if u % 2 == 0 else 2) + e)
        if look == 'energy':
            return ramp(RED, (2 if u % 4 == 0 else 4 if u % 4 == 1 else 3) + e + jitter(u, y, 5, .1, .1))
        if look == 'fluid':
            return ramp(BLUE, 3 + e + (2 if (u + k) % 5 == 0 else 0) + jitter(u, y, 6, .1, .1))
        light = ((u + k) % 4 < 2) if (u // 4) % 2 == 0 else ((u - k) % 4 < 2)
        if look == 'data':
            return ramp(WHITE, (4 if light else 2) + e)
        if look == 'duplex':
            return ramp(WHITE if light else GRAY, (3 if light else 2) + e)
        if (u + k) % 7 == 0:
            return ramp(CYAN, 3 + e)
        return ramp(BLACK, (4 if light else 2) + e)
    return fn


def line_texture(look, thickness):
    """One line's strip, repeated every {@code thickness} rows around row 8."""
    fn = strip(look)
    top = 8 - thickness // 2
    img = blank()
    for y in range(16):
        for u in range(16):
            img[y][u] = fn(u, (y - top) % thickness, thickness, y)
    return img


def thickness(count):
    return 4 if count == 1 else 6 if count == 2 else 8


def band_widths(count):
    """Rows per band for a part of that many lines, first line on the negative side."""
    t = thickness(count)
    base = t // count
    widths = [base] * count
    for i in range(t - base * count):
        widths[i if i % 2 == 0 else count - 1 - i // 2] += 1
    return widths


def band_texture(looks):
    widths = band_widths(len(looks))
    top = 8 - thickness(len(looks)) // 2
    rows = {}
    y = top
    for look, width in zip(looks, widths):
        for k in range(width):
            rows[y] = (strip(look), k, width)
            y += 1
    img = blank()
    for row in range(16):
        for u in range(16):
            if row in rows:
                fn, k, width = rows[row]
                img[row][u] = fn(u, k, width, row)
            else:
                img[row][u] = ramp(RUBBER, 3)
    return img


# ------------------------------------------------------------------ fitting and ring
def fitting_texture(r):
    lo, hi = 8 - r, 8 + r - 1
    img = blank()
    for y in range(16):
        for x in range(16):
            if x in (lo, hi) or y in (lo, hi):
                c = ramp(RUBBER, 1)
            elif x == lo + 1 or y == lo + 1:
                c = ramp(RUBBER, 5)
            elif x == hi - 1 or y == hi - 1:
                c = ramp(RUBBER, 2)
            elif r >= 4 and abs(x + 0.5 - 8) < 1.6 and abs(y + 0.5 - 8) < 1.6:
                c = ramp(STEEL, 5 if x + y < 15 else 3)
            else:
                c = ramp(RUBBER, 3 + jitter(x, y, 31, .12, .12))
            img[y][x] = c
    return img


def ring_texture():
    img = blank()
    for y in range(16):
        for x in range(16):
            v = 240 if x % 2 == 0 else 190
            img[y][x] = (v, v, v, 255)
    return img


# ------------------------------------------------------------------ icons
ICON_SPEC = {
    'item': dict(body=STEEL, rim=STEEL, layer=ORANGE),
    'fluid': dict(body=STEEL, rim=STEEL, layer=BLUE),
    'energy': dict(body=RED, rim=RED, layer=RUBBER),
    'data': dict(body=WHITE, rim=WHITE, layer=STEEL),
    'duplex': dict(body=GRAY, rim=GRAY, layer=STEEL),
    'fast': dict(body=BLACK, rim=BLACK, layer=STEEL),
}


def core_pixel(look, x, y, cx, cy):
    dx, dy = x + 0.5 - cx, y + 0.5 - cy
    if look == 'item':
        if abs(dx) < 1.1 and abs(dy) < 1.1:
            return ramp(ORANGE, 4 if dx + dy < 0 else 2)
        return GLASS[1]
    if look == 'fluid':
        if dx + dy < -1.6:
            return WATER[5]
        return ramp(WATER, 3 if dx + dy < 0.5 else 2)
    if look == 'energy':
        return ramp(COPPER, 5 if dx + dy < -1.2 else 4 if dx + dy < 0.5 else 2)
    pins = {1: [(0, 0)], 2: [(-1, 0), (1, 0)], 3: [(-1, -1), (1, -1), (0, 1)]}[CORES[look]]
    for (px, py) in pins:
        if abs(dx - px) < 0.6 and abs(dy - py) < 0.6:
            return CYAN[4] if look == 'fast' else COPPER[4]
    return BLACK[1]


def icon(look):
    """A short stub of the line seen from its end, the far end down right."""
    img = blank()
    spec = ICON_SPEC[look]
    front = (6.2, 6.2)
    back = (10.6, 10.6)
    radius = 5.2
    ax = (back[0] - front[0], back[1] - front[1])
    length = math.hypot(*ax)
    ad = (ax[0] / length, ax[1] / length)
    perp = (ad[1], -ad[0])
    body = {}
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - front[0], y + 0.5 - front[1]
            u = px * ad[0] + py * ad[1]
            v = px * perp[0] + py * perp[1]
            uc = max(0, min(length, u))
            if math.hypot(px - uc * ad[0], py - uc * ad[1]) > radius:
                continue
            s = v / radius
            i = 4 if s > 0.4 else 3 if s > -0.2 else 2 if s > -0.7 else 1
            if look in ('item', 'fluid') and abs(u - length * 0.55) < 0.6:
                i -= 1
            body[(x, y)] = (spec['body'], i)
    for (x, y), (p, i) in body.items():
        img[y][x] = ramp(p, i)
    for (x, y), (p, i) in body.items():
        edges = [(dx, dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (x + dx, y + dy) not in body]
        if edges:
            lower = any(e in ((1, 0), (0, 1)) for e in edges)
            img[y][x] = ramp(p, (i - 2) if lower else (i - 1))
    face = {}
    rr = radius - 0.15
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - front[0], y + 0.5 - front[1]
            dd = math.hypot(dx, dy)
            if dd > rr:
                continue
            lit = -(dx + dy) / (rr * 1.414)
            if dd > rr - 1.25:
                face[(x, y)] = ('rim', spec['rim'], 5 if lit > 0.2 else 4 if lit > -0.35 else 3)
            elif dd > rr - 2.35:
                face[(x, y)] = ('layer', spec['layer'], 2 if lit > 0.2 else 3 if lit > -0.35 else 4)
            else:
                face[(x, y)] = ('core', None, 0)
    for (x, y), (kind, p, i) in face.items():
        img[y][x] = core_pixel(look, x, y, front[0], front[1]) if kind == 'core' else ramp(p, i)
    for (x, y), (kind, p, i) in face.items():
        if kind == 'rim' and ((x - 1, y) not in face and (x - 1, y) not in body
                              or (x, y - 1) not in face and (x, y - 1) not in body):
            img[y][x] = ramp(p, 2)
    return img


# ------------------------------------------------------------------ output
def line_sets():
    for mask in range(1, 16):
        yield [LINES[i] for i in range(4) if mask & 1 << i]


def textures():
    """Every file this script owns, by path below textures/."""
    out = {}
    for line in LINES:
        for tier, suffix in TIERS.items() if line == 'data' else [(line, None)]:
            for t in (4, 6, 8):
                name = f'duct_{line}' + ('' if t == 4 else f'_{t}') + (f'_{suffix}' if suffix else '')
                out[f'block/{name}.png'] = line_texture(tier, t)
    for lines in line_sets():
        if len(lines) < 2:
            continue
        name = 'duct_' + '_'.join(lines)
        for tier, suffix in TIERS.items() if 'data' in lines else [(None, None)]:
            looks = [tier if line == 'data' else line for line in lines]
            out[f'block/{name}' + (f'_{suffix}' if suffix else '') + '.png'] = band_texture(looks)
    for r in (2, 4, 5):
        out[f'block/duct_fitting_{r}.png'] = fitting_texture(r)
    out['block/duct_ring.png'] = ring_texture()
    for look, item in ICONS.items():
        out[f'item/{item}.png'] = icon(look)
    return out


def check():
    for count in (2, 3, 4):
        widths = band_widths(count)
        assert sum(widths) == thickness(count), (count, widths)
        assert min(widths) >= 2, (count, widths)
    print('band split ok:', {count: band_widths(count) for count in (2, 3, 4)})


def main():
    check()
    if '--check' in sys.argv:
        return
    for path, img in textures().items():
        target = os.path.join(TEXTURES, *path.split('/'))
        data = png_bytes(img)
        if not os.path.exists(target) or open(target, 'rb').read() != data:
            with open(target, 'wb') as f:
                f.write(data)
    print('wrote', len(textures()), 'textures into', TEXTURES)


if __name__ == '__main__':
    main()
