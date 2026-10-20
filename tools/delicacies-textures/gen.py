"""Generates the Servus: Delicacies item and block textures.

This script is the source of the files; edit the sprites here, not the PNGs.

    python tools/delicacies-textures/gen.py                      writes the textures
    python tools/delicacies-textures/gen.py --preview <folder>   also writes 8x previews

Written into delicacies/src/main/resources/assets/servus_delicacies/textures/{item,block}/.
Grid sprites are drawn as character grids; a grid that lists only its left half is
mirrored into the right half. Painted sprites shade masks as soft domes lit from the
top left, with a fixed random seed per sprite so every run writes the same pixels.
"""
import math
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'delicacies', 'src', 'main', 'resources', 'assets', 'servus_delicacies', 'textures')
S = 16
CLEAR = (0, 0, 0, 0)


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def R(*hexes):
    return [hx(h) for h in hexes]


# ---------- grid sprites ----------

# Baked dough from the dark outline to the light crust, plus coarse salt.
DOUGH = {
    'A': (58, 26, 7),
    'G': (94, 44, 14),
    'F': (127, 61, 20),
    'E': (157, 83, 31),
    'C': (187, 107, 46),
    'B': (207, 134, 66),
    'D': (247, 244, 236),
}

# The left half of the pretzel: two loops over the crossed arms, with salt on the top bends.
PRETZEL_LEFT = [
    "........",
    "...AAAA.",
    "..ABCCDA",
    ".ABCAACE",
    ".ACA..AE",
    ".ACA..AE",
    ".ADA..AE",
    ".AECAAEA",
    ".ABECA..",
    ".ACEGA..",
    ".AEDGA..",
    ".ABEEEAA",
    "..AGEEEE",
    "...AAGGG",
    ".....AAA",
    "........",
]


def mirrored(left):
    return [row + row[::-1] for row in left]


def grid_sprite(rows, palette):
    return [[palette[c] + (255,) if c != '.' else CLEAR for c in row] for row in rows]


# ---------- painted sprites ----------

class Spr:
    def __init__(self):
        self.p = [[CLEAR] * S for _ in range(S)]

    def set(self, x, y, c):
        if 0 <= x < S and 0 <= y < S:
            self.p[y][x] = c

    def image(self):
        return self.p


def ellipse(cx, cy, rx, ry):
    return {(x, y) for y in range(S) for x in range(S)
            if ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1}


def seg_dist(px, py, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    t = max(0, min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - x1 - t * dx, py - y1 - t * dy), t


def capsule(x1, y1, x2, y2, r, r2=None):
    r2 = r if r2 is None else r2
    out = set()
    for y in range(S):
        for x in range(S):
            d, t = seg_dist(x + .5, y + .5, x1, y1, x2, y2)
            if d <= r + (r2 - r) * t:
                out.add((x, y))
    return out


def rect(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


def poly(points):
    out = set()
    for y in range(S):
        for x in range(S):
            px, py = x + .5, y + .5
            inside = False
            j = len(points) - 1
            for i in range(len(points)):
                xi, yi = points[i]
                xj, yj = points[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                    inside = not inside
                j = i
            if inside:
                out.add((x, y))
    return out


def boundary(mask):
    return {(x, y) for (x, y) in mask
            if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


LIGHT = (-0.55, -0.75, 0.65)


def heights(mask):
    outside = [(x, y) for y in range(-1, S + 1) for x in range(-1, S + 1) if (x, y) not in mask]
    d = {(x, y): min(math.hypot(x - ox, y - oy) for ox, oy in outside) for (x, y) in mask}
    m = max(d.values()) if d else 1
    return {k: math.sqrt(min(v / m, 1)) for k, v in d.items()}


def paint(spr, mask, ramp, rng, noise=0.12, outline=True, k=2.2, bias=0.0, specks=(), pattern=None):
    """Shade a mask as a soft dome lit from the top left; ramp runs dark to light, ramp[0] is the outline."""
    if not mask:
        return
    h = heights(mask)
    lx, ly, lz = LIGHT
    ln = math.sqrt(lx * lx + ly * ly + lz * lz)
    edge = boundary(mask) if outline else set()
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
        v = (dot - 0.2) / 0.8 + bias + rng.uniform(-noise, noise)
        if pattern:
            v += pattern(x, y)
        lo = 1 if outline else 0
        idx = lo + int(round(max(0, min(1, v)) * (n - 1 - lo)))
        spr.set(x, y, ramp[max(lo, min(n - 1, idx))])
    for color, prob in specks:
        for (x, y) in mask - edge:
            if rng.random() < prob:
                spr.set(x, y, color)


def line(spr, x1, y1, x2, y2, color, r=.5):
    for (x, y) in capsule(x1, y1, x2, y2, r):
        spr.set(x, y, color)


def rng_for(name):
    return random.Random(name)


# ---------- crop palettes ----------
REDK = R('#2e0d2c', '#5c1f56', '#7e2f75', '#a14d96', '#c27ab8')
PARS = R('#1f4012', '#3a6a1f', '#56902e', '#7db544')
CAB = R('#2f4d1a', '#5a8a32', '#86b356', '#b0d27e', '#d6e9b0')
ONION = R('#5a2e0c', '#9a5a1c', '#c4842f', '#dfac58', '#efcf8e')
KREN = R('#4f3a24', '#8f7350', '#b89a72', '#d6c09c', '#ebdcc0')
SENF = R('#3e2406', '#6e420e', '#97621a', '#bd8428', '#d6a240')
SEED = R('#3b2a14', '#6e5230', '#9a7a4c', '#c4a774')
LEAF = R('#1f3d12', '#356a1f', '#4f8f2c', '#73b043', '#9fcf68')
BLUE_LEAF = R('#22382c', '#3c5e4a', '#58806a', '#7aa08a')
STALK = hx('#4f8f2c')
STALK_DARK = hx('#356a1f')
POD = R('#3f4a14', '#66751f', '#8c9a34', '#b4bf5a')
FLOWER = R('#8a6a06', '#d8b018', '#f4d83a', '#fff08a')
PAPER = R('#7a6a4a', '#c8b48a', '#e6d6b0', '#f6ecd2')


# ---------- crop items ----------

def kraut_head(ramp, seed):
    s = Spr()
    rng = rng_for(seed)
    m = ellipse(8, 8.8, 6.6, 6.0)
    paint(s, m, ramp, rng, noise=.08, k=2.0)
    for (cx, cy, r) in ((5.5, 13, 7.5), (10.8, 13.4, 7.2), (8, 15.5, 8.6)):
        for (x, y) in m - boundary(m):
            if abs(math.hypot(x + .5 - cx, y + .5 - cy) - r) < .45:
                s.set(x, y, ramp[1])
    for (x, y) in ((8, 6), (8, 7), (8, 8), (7, 9), (9, 9)):
        s.set(x, y, ramp[4])
    for lf in (ellipse(3.3, 10.5, 2.4, 3.8), ellipse(12.7, 10.5, 2.4, 3.8)):
        paint(s, lf, ramp, rng, noise=.08, k=1.2, bias=-.3)
    return s


def onion():
    s = Spr()
    rng = rng_for('zw')
    m = ellipse(8, 10.0, 5.4, 4.8) | poly([(6.6, 6.5), (8, 2.5), (9.4, 6.5)])
    paint(s, m, ONION, rng, noise=.08, k=1.8, pattern=lambda x, y: .15 * math.sin((x - 8) * 1.6))
    for x in (6, 8, 10):
        s.set(x, 15, hx('#e8dcc0'))
    s.set(7, 15, hx('#c9b994'))
    s.set(9, 15, hx('#c9b994'))
    return s


def parsley():
    s = Spr()
    rng = rng_for('pet')
    for (x2, y2) in ((5, 5), (9, 4), (11, 7), (6, 8)):
        for (x, y) in capsule(8, 14.5, x2, y2, .5):
            s.set(x, y, PARS[2])
    for (cx, cy) in ((5, 4.6), (9.2, 3.8), (11.4, 6.8), (6.0, 8.0), (8.4, 6.6)):
        paint(s, ellipse(cx, cy, 2.4, 2.2), PARS, rng, noise=.3, k=1.4)
    return s


def horseradish():
    s = Spr()
    rng = rng_for('kren')
    m = capsule(3.0, 13.2, 11.0, 5.0, .9, 2.3)
    paint(s, m, KREN, rng, noise=.1, k=1.8)
    for (x, y) in m - boundary(m):
        if (x + y) % 3 == 0 and rng.random() < .7:
            s.set(x, y, KREN[1])
    for (x2, y2) in ((14, 1), (15, 4), (12, 1)):
        for (x, y) in capsule(11.5, 4.5, x2, y2, .7):
            s.set(x, y, PARS[2] if (x + y) % 2 else PARS[3])
    s.set(2, 14, KREN[1])
    s.set(1, 15, KREN[0])
    return s


def mustard_seeds():
    """A small heap of round yellow brown mustard seeds."""
    s = Spr()
    rng = rng_for('senfkorn')
    heap = ellipse(8, 12.2, 6.4, 3.4) | ellipse(8, 10.4, 4.2, 3.0)
    for (x, y) in boundary(heap):
        s.set(x, y, SENF[0])
    inner = sorted(heap - boundary(heap), key=lambda p: (p[1], p[0]))
    for (x, y) in inner:
        s.set(x, y, SENF[1])
    for (x, y) in inner:
        if (x + 2 * y) % 3 == 0 or rng.random() < .25:
            light = 3 if x + y < 19 else 2
            s.set(x, y, SENF[light + (1 if rng.random() < .3 else 0)])
    for (x, y) in ((4, 15), (12, 15), (13, 13)):
        s.set(x, y, SENF[3])
    return s


def seed_packet(ramp, seed):
    """A heap of seeds in the crop's own colours, the vanilla seed item style."""
    s = Spr()
    rng = rng_for(seed)
    spots = [(5, 9), (8, 7), (11, 9), (6, 12), (9, 11), (12, 12), (4, 14), (8, 14), (11, 14), (10, 5)]
    for (cx, cy) in spots:
        jx = rng.choice((-1, 0, 0, 1))
        grain = {(cx + jx, cy), (cx + jx + 1, cy), (cx + jx, cy + 1), (cx + jx + 1, cy + 1)}
        for (x, y) in grain:
            s.set(x, y, ramp[1])
        s.set(cx + jx, cy, ramp[3])
        s.set(cx + jx + 1, cy + 1, ramp[0])
    return s


# ---------- crop stages (cross model textures, 16 x 16, rooted on the bottom row) ----------

def leaf(base, tip, width):
    """A pointed leaf from its base to its tip, widest a little below the middle."""
    bx, by = base
    tx, ty = tip
    dx, dy = tx - bx, ty - by
    n = math.hypot(dx, dy)
    nx, ny = -dy / n * width / 2, dx / n * width / 2
    mx, my = bx + dx * .4, by + dy * .4
    return poly([(bx, by), (mx + nx, my + ny), (tx, ty), (mx - nx, my - ny)])


def shade_flat(spr, mask, ramp):
    """Fills a small mask without an outline: the top rows light, the rest mid."""
    top = min(y for (_, y) in mask)
    for (x, y) in mask:
        spr.set(x, y, ramp[3] if y == top else ramp[2])


def sprout(seed, ramp=LEAF):
    """Stage 0 of every crop: two seedlings, each a pair of small leaves on a short stem."""
    s = Spr()
    for (x, h) in ((5, 3), (11, 2)):
        line(s, x + .5, 15.5, x + .5, 15.5 - h, ramp[1], .4)
        shade_flat(s, leaf((x + .5, 15.5 - h), (x - 2.0, 13.4 - h), 2.2), ramp)
        shade_flat(s, leaf((x + .5, 15.5 - h), (x + 3.0, 13.6 - h), 2.2), ramp)
    return s


def leaf_fan(s, rng, ramp, leaves, k=1.1, bias=0.0):
    for (cx, cy, rx, ry) in leaves:
        paint(s, ellipse(cx, cy, rx, ry), ramp, rng, noise=.1, k=k, bias=bias)


def cabbage_stage(ramp, outer, stage, seed):
    s = Spr()
    rng = rng_for(seed + str(stage))
    if stage == 0:
        return sprout(seed, outer)
    if stage == 1:
        leaf_fan(s, rng, outer, ((5.0, 12.6, 3.0, 2.4), (11.0, 12.6, 3.0, 2.4), (8.0, 11.4, 2.6, 3.0)))
        return s
    if stage == 2:
        leaf_fan(s, rng, outer, ((3.8, 11.6, 3.6, 3.4), (12.2, 11.6, 3.6, 3.4)), bias=-.15)
        paint(s, ellipse(8, 12.0, 4.0, 3.6), ramp, rng, noise=.08, k=1.8)
        return s
    leaf_fan(s, rng, outer, ((3.0, 11.8, 3.2, 4.0), (13.0, 11.8, 3.2, 4.0)), bias=-.2)
    head = ellipse(8, 10.6, 5.6, 5.0)
    paint(s, head, ramp, rng, noise=.08, k=2.0)
    for (cx, cy, r) in ((5.5, 15.0, 6.8), (10.5, 15.2, 6.6)):
        for (x, y) in head - boundary(head):
            if abs(math.hypot(x + .5 - cx, y + .5 - cy) - r) < .45:
                s.set(x, y, ramp[1])
    for (x, y) in ((8, 7), (8, 8), (7, 9), (9, 9)):
        s.set(x, y, ramp[4])
    leaf_fan(s, rng, outer, ((4.2, 14.4, 3.0, 1.6), (11.8, 14.4, 3.0, 1.6)), bias=-.3)
    return s


def onion_stage(stage):
    if stage == 0:
        return sprout('zw0')
    s = Spr()
    rng = rng_for('zw' + str(stage))
    tops = {1: ((6, 9), (9, 8), (11, 10)),
            2: ((4, 5), (7, 3), (10, 4), (12, 7)),
            3: ((3, 5), (6, 2), (9, 1), (12, 3), (13, 7))}[stage]
    base_y = 14.5 if stage < 3 else 11.5
    for i, (x2, y2) in enumerate(tops):
        line(s, 8, base_y, x2, y2, LEAF[2] if i % 2 else LEAF[3], .55)
        s.set(x2, y2, LEAF[1])
    if stage == 2:
        paint(s, ellipse(8, 14.8, 2.2, 1.6), ONION, rng, noise=.06, k=1.2)
    if stage == 3:
        bulb = ellipse(8, 13.2, 4.0, 3.0)
        paint(s, bulb, ONION, rng, noise=.06, k=1.8, pattern=lambda x, y: .15 * math.sin((x - 8) * 1.6))
    return s


def parsley_stage(stage):
    if stage == 0:
        return sprout('pet0', PARS + [hx('#9fcf68')])
    s = Spr()
    rng = rng_for('pet' + str(stage))
    sprigs = {1: ((5, 11), (11, 10), (8, 9)),
              2: ((4, 9), (8, 7), (12, 9), (6, 12), (10, 11)),
              3: ((3, 7), (7, 4), (10, 3), (13, 6), (5, 10), (11, 9), (8, 8), (3, 12), (13, 12))}[stage]
    r = {1: 1.9, 2: 2.0, 3: 2.2}[stage]
    for (x2, y2) in sprigs:
        line(s, 8, 15.5, x2, y2, PARS[1], .45)
    for (cx, cy) in sprigs:
        paint(s, ellipse(cx, cy, r, r * .9), PARS, rng, noise=.3, k=1.4)
    return s


def horseradish_stage(stage):
    if stage == 0:
        return sprout('kren0')
    s = Spr()
    rng = rng_for('kren' + str(stage))
    tips = {1: ((4.0, 9.0), (8.0, 7.0), (12.0, 8.5)),
            2: ((2.5, 6.0), (7.0, 3.5), (11.0, 4.0), (14.0, 7.5)),
            3: ((1.5, 4.5), (5.0, 1.5), (9.0, 0.5), (12.5, 2.0), (15.0, 6.0))}[stage]
    width = {1: 3.2, 2: 3.6, 3: 4.0}[stage]
    for (tx, ty) in tips:
        paint(s, leaf((8, 15.6), (tx, ty), width), LEAF, rng, noise=.12, k=.9, bias=.15,
              pattern=lambda x, y: .18 * math.sin((x + y) * 1.7))
        line(s, 8, 15, tx + (8 - tx) * .15, ty + (15 - ty) * .15, LEAF[2], .3)
    if stage == 3:
        paint(s, ellipse(8, 15.5, 3.0, 1.6), KREN, rng, noise=.08, k=1.4)
    return s


def mustard_stage(stage):
    if stage == 0:
        return sprout('senf0')
    s = Spr()
    rng = rng_for('senf' + str(stage))
    if stage == 1:
        leaf_fan(s, rng, LEAF, ((5.4, 12.4, 2.6, 2.0), (10.6, 12.0, 2.6, 2.0), (8.0, 10.4, 1.8, 2.4)))
        return s
    stems = ((3, 4), (8, 1), (13, 4), (5, 7), (11, 6))
    for i, (x2, y2) in enumerate(stems):
        line(s, 8, 15.5, x2, y2, STALK if i % 2 else STALK_DARK, .45)
    leaf_fan(s, rng, LEAF, ((5.0, 13.0, 2.4, 1.6), (11.0, 13.2, 2.4, 1.6)))
    for (cx, cy) in stems:
        if stage == 2:
            for (dx, dy, c) in ((0, -1, 3), (-1, 0, 2), (1, 0, 2), (0, 0, 1), (-1, -1, 2), (1, -1, 3)):
                s.set(cx + dx, cy + dy, FLOWER[c])
        else:
            for (dx, dy) in ((-1, -1), (1, -1), (-2, 1), (2, 1)):
                line(s, cx, cy, cx + dx, cy + dy - 1.0, SENF[3] if dx < 0 else SENF[2], .35)
            s.set(cx, cy - 1, SENF[4])
    return s


def stages(name, draw):
    return {'block/%s_stage%d' % (name, i): draw(i).image() for i in range(4)}


SPRITES = {
    'item/pretzel': grid_sprite(mirrored(PRETZEL_LEFT), DOUGH),
    'item/cabbage': kraut_head(CAB, 'wk').image(),
    'item/red_cabbage': kraut_head(REDK, 'bkk').image(),
    'item/onion': onion().image(),
    'item/parsley': parsley().image(),
    'item/horseradish': horseradish().image(),
    'item/mustard_seeds': mustard_seeds().image(),
    'item/cabbage_seeds': seed_packet(R('#2c3a12', '#5f7a26', '#8aa83e', '#b8d06a'), 'cabseed').image(),
    'item/red_cabbage_seeds': seed_packet(R('#2a1428', '#5a2c50', '#86507a', '#b07aa4'), 'redseed').image(),
    'item/parsley_seeds': seed_packet(SEED, 'parseed').image(),
}
SPRITES.update(stages('cabbage', lambda i: cabbage_stage(CAB, LEAF, i, 'cab')))
SPRITES.update(stages('red_cabbage', lambda i: cabbage_stage(REDK, BLUE_LEAF, i, 'redcab')))
SPRITES.update(stages('onion', onion_stage))
SPRITES.update(stages('parsley', parsley_stage))
SPRITES.update(stages('horseradish', horseradish_stage))
SPRITES.update(stages('mustard', mustard_stage))


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
        write(os.path.join(TEXTURES, name + '.png'), img)
        if preview:
            write(os.path.join(preview, name.replace('/', '_') + '.png'), scaled(img, 8))


if __name__ == '__main__':
    main()
