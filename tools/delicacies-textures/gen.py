"""Generates the Servus: Delicacies item and block textures.

This script is the source of the files; edit the sprites here, not the PNGs.

    python tools/delicacies-textures/gen.py                      writes the textures
    python tools/delicacies-textures/gen.py --preview <folder>   also writes 8x previews

Written into delicacies/src/main/resources/assets/servus_delicacies/textures/{item,block}/.
Grid sprites are drawn as character grids; a grid that lists only its left half is
mirrored into the right half. Painted sprites shade masks as soft domes lit from the
top left, with a fixed random seed per sprite so every run writes the same pixels.
"""
import json
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


# ---------- kitchen blocks (ported from the mockup) ----------
IRON = R('#15171a', '#24272c', '#33373e', '#454a52', '#5b616a')
BRASS = R('#5e4514', '#9a7428', '#c89c44', '#e8c46a')
TILE = R('#163020', '#24503a', '#326a4c', '#4a8a64', '#7cb890')
GLOW = [hx('#ffe08a'), hx('#ffb030'), hx('#e0601a')]


def tex_noise(ramp, seed, base=2, spread=1):
    rng = random.Random(seed)
    s = Spr()
    for y in range(S):
        for x in range(S):
            s.set(x, y, ramp[max(0, min(len(ramp) - 1, base + rng.randint(-spread, spread) * (1 if rng.random() < .35 else 0)))])
    return s


def kachel():
    """Four green glazed tiles with a light top left rim, a dark bottom right rim and a raised round centre."""
    s = Spr()
    for ty in (0, 8):
        for tx in (0, 8):
            for y in range(8):
                for x in range(8):
                    if x == 0 or y == 0:
                        c = TILE[4]
                    elif x == 7 or y == 7:
                        c = TILE[0]
                    elif x == 1 or y == 1:
                        c = TILE[3]
                    else:
                        r = math.hypot(x - 3.5, y - 3.5)
                        c = TILE[3] if r < 1.2 else TILE[1] if r < 2.0 else TILE[2]
                    s.set(tx + x, ty + y, c)
    return s


def kachel_door(lit, flicker=False):
    """The tiles with a brass firebox door; its two slits glow while the fire burns, the flicker frame shifts the glow."""
    s = kachel()
    door = rect(3, 6, 12, 13)
    for (x, y) in door:
        s.set(x, y, BRASS[2])
    for (x, y) in boundary(door):
        s.set(x, y, BRASS[0])
    for y in (8, 10):
        for x in range(5, 11):
            if not lit:
                s.set(x, y, BRASS[0])
            elif flicker:
                s.set(x, y, [GLOW[1], GLOW[2], GLOW[0]][(x * 2 + y) % 3])
            else:
                s.set(x, y, GLOW[(x + y) % 3])
    s.set(11, 9, BRASS[3])
    return s


def herd_top(lit):
    """The iron cooking plate with a brass rim and a hob ring; the ring glows dark red while lit."""
    s = tex_noise(IRON, 'ht', base=3)
    for (x, y) in boundary(rect(0, 0, 15, 15)):
        s.set(x, y, BRASS[2])
    for (x, y) in rect(1, 1, 14, 14):
        r = math.hypot(x + .5 - 8, y + .5 - 8)
        if abs(r - 5.6) < .55 or abs(r - 3.4) < .5 or abs(r - 1.3) < .5:
            s.set(x, y, IRON[1])
        elif r < 6.2 and lit and (x + y) % 2 == 0:
            s.set(x, y, hx('#5a2418'))
    return s


def strip(*frames):
    """Animation frames stacked top to bottom into one texture."""
    return [row for frame in frames for row in frame.image()]


# The door's flicker: frame indices of the strip and ticks per frame.
DOOR_FLICKER = {'animation': {'frames': [0, 1, 0, 0, 1, 1, 0, 1], 'frametime': 3}}


# ---------- kitchen items ----------
POTATO = R('#6b4a1c', '#b8903e', '#dcbc66', '#eed58e', '#f8eab8')
EGG_WHITE = R('#a8a294', '#e2ded2', '#f2f0ea', '#fcfbf8')
YOLK = R('#a0580a', '#e2921a', '#f6c03a', '#ffe27a')
FRIED = R('#4e260a', '#874615', '#b8722a', '#d89c48', '#efc56e')
ONION_PIECE = R('#8a7a5a', '#d8cba4', '#ece2c4', '#f8f2e0')
STEEL = R('#34383e', '#666d76', '#959da6', '#c4cad0', '#eef1f4')
WOOD = R('#3a220e', '#66401f', '#8c5a2e', '#ae7842')


def boiled_potatoes():
    """Three peeled potatoes with parsley flecks."""
    s = Spr()
    rng = rng_for('bp')
    for (cx, cy, rx, ry) in ((5.2, 10.6, 3.8, 3.0), (10.8, 10.9, 3.8, 3.0), (8.0, 6.6, 3.6, 2.9)):
        paint(s, ellipse(cx, cy, rx, ry), POTATO, rng, noise=.06, k=1.8)
    for (x, y) in ((4, 9), (9, 10), (7, 6), (12, 11), (8, 5), (5, 11)):
        s.set(x, y, PARS[2])
    return s


def fried_egg():
    """A fried egg seen from above: white with crisp brown edges and a round yolk."""
    s = Spr()
    rng = rng_for('fe')
    white = ellipse(8, 8.6, 6.4, 5.4) | ellipse(4.8, 10.4, 3.4, 3.4) | ellipse(11.4, 6.4, 3.4, 3.0)
    paint(s, white, EGG_WHITE, rng, noise=.04, k=.6)
    for (x, y) in boundary(white):
        if rng.random() < .4:
            s.set(x, y, FRIED[2])
    paint(s, ellipse(8.4, 8.2, 2.7, 2.5), YOLK, rng, noise=.03, k=2.0)
    s.set(7, 7, YOLK[3])
    return s


def rings(s, rng, centres, ramp):
    """Onion rings seen slightly from above: light upper arcs, darker lower arcs with a dark rim."""
    for (cx, cy, r) in centres:
        for y in range(S):
            for x in range(S):
                d = math.hypot(x + .5 - cx, (y + .5 - cy) * 1.25)
                if abs(d - r) < .75:
                    shade = 3 if y + .5 < cy else 2
                    if rng.random() < .15:
                        shade -= 1
                    s.set(x, y, ramp[shade])
                elif abs(d - r) < 1.25 and y + .5 > cy:
                    s.set(x, y, ramp[0])


def fried_onions():
    """A heap of golden fried onion rings."""
    s = Spr()
    rng = rng_for('fo')
    rings(s, rng, ((5.5, 10.5, 3.2), (10.5, 10.8, 3.0), (8.0, 6.8, 3.0)), FRIED)
    return s


def fried_potatoes():
    """Golden fried potato slices with onion bits."""
    s = Spr()
    rng = rng_for('fp')
    for (cx, cy) in ((4.6, 11.0), (8.4, 12.0), (12.0, 10.6), (6.4, 7.6), (10.4, 7.0), (8.2, 4.4)):
        paint(s, ellipse(cx, cy, 2.9, 2.2), FRIED, rng, noise=.1, k=1.0, bias=.15)
    for (x, y) in ((6, 10), (10, 11), (9, 7), (5, 7), (12, 9), (8, 4)):
        s.set(x, y, ONION_PIECE[2])
    return s


def chopped_onion():
    """A small heap of pale onion cubes."""
    s = Spr()
    rng = rng_for('co')
    cubes = [(3, 11), (6, 12), (9, 12), (12, 11), (5, 9), (8, 9), (11, 9), (7, 6), (10, 6)]
    for (x, y) in cubes:
        jx = rng.choice((0, 0, 1))
        for (dx, dy, c) in ((0, 0, 3), (1, 0, 2), (0, 1, 2), (1, 1, 1)):
            s.set(x + jx + dx, y + dy, ONION_PIECE[c])
        s.set(x + jx + 2, y + 1, ONION_PIECE[0])
        s.set(x + jx, y + 2, ONION_PIECE[0])
        s.set(x + jx + 1, y + 2, ONION_PIECE[0])
    return s


def shredded_cabbage():
    """A loose heap of thin pale green cabbage strands."""
    return shredded(CAB, 'sc')


def shredded(ramp, seed):
    """A loose heap of thin cabbage strands in the cabbage's ramp."""
    s = Spr()
    rng = rng_for(seed)
    for i in range(9):
        y0 = 6 + i * .9
        x0 = 2.5 + rng.random() * 2
        x1 = 11 + rng.random() * 2.5
        c = ramp[1 + (i % 3)]
        for x in range(int(x0), int(x1) + 1):
            y = int(round(y0 + math.sin(x * .9 + i) * 1.1))
            s.set(x, y, c)
            if rng.random() < .25:
                s.set(x, y + 1, ramp[1])
    return s


def kitchen_knife():
    """A kitchen knife held like a vanilla tool: wooden handle bottom left, steel blade to the top right."""
    s = Spr()
    for (x, y) in capsule(2.2, 13.8, 5.4, 10.6, 1.15):
        s.set(x, y, WOOD[2] if x + y < 16 else WOOD[1])
    for (x, y) in ((3, 12), (5, 10)):
        s.set(x, y, BRASS[3])
    blade = poly([(4.6, 8.6), (7.8, 11.2), (15.2, 2.8), (14.9, 0.6)])
    for (x, y) in blade:
        s.set(x, y, STEEL[2])
    for (x, y) in boundary(blade):
        s.set(x, y, STEEL[1] if x + y > 16 else STEEL[3])
    for (x, y) in ((7, 9), (9, 7), (11, 5), (13, 3)):
        s.set(x, y, STEEL[4])
    s.set(5, 10, STEEL[1])
    return s


# ---------- bakery ----------
SEMMEL = R('#5a3412', '#9a5f24', '#c4863d', '#dba55a', '#ecc47e')
SACK = R('#5e4e34', '#a8946e', '#ccbb96', '#e6dbbf', '#f4eedd')
SACK_BAND = hx('#5b86c8')
SALT_WHITE = hx('#f7f4ec')
DOUGH_PALE = R('#8a7650', '#d2c29a', '#e4d7b4', '#f0e7cf', '#faf5e8')
SALT_HEAP = R('#8e8a80', '#cfcbc0', '#e6e3da', '#f4f2ec', '#ffffff')
ASH_GREY = R('#3a3836', '#666361', '#8a8784', '#aeaba7', '#cecbc6')
CHAR = hx('#24201e')
STONEWARE = R('#45454c', '#77777f', '#9d9da5', '#c3c3c9', '#e0e0e4')
COBALT = hx('#2f5fa8')
CORK = R('#5a3c1c', '#8e6a3c', '#b48e5a')
CRUMB = R('#6b3d12', '#a8661e', '#c98a2e', '#e0aa45', '#f0c868')
OVEN_GLOW = [hx('#ffe08a'), hx('#ffb030'), hx('#e0601a')]


def semmel():
    """A Bread Roll from the mockup: a round golden roll with the five cut star and a few salt grains."""
    s = Spr()
    rng = rng_for('semmel')
    m = ellipse(8, 9.6, 6.6, 5.0) - {(x, y) for x in range(S) for y in range(13, S)}
    m |= rect(3, 12, 12, 12)
    paint(s, m, SEMMEL, rng, noise=.1, k=2.6)
    edge = boundary(m)
    c = (8, 7.6)
    for a in (-90, -18, 54, 126, 198):
        r = math.radians(a)
        for t in (1.0, 2.0, 3.0, 3.8):
            x, y = int(c[0] + math.cos(r) * t + math.sin(r) * t * .35), int(c[1] + math.sin(r) * t * .62 - math.cos(r) * t * .2)
            if (x, y) in m and (x, y) not in edge:
                s.set(x, y, SEMMEL[1])
                if (x, y - 1) in m:
                    s.set(x, y - 1, SEMMEL[4])
    for (x, y) in m:
        if rng.random() < .04 and (x, y) not in edge:
            s.set(x, y, SALT_WHITE)
    return s


def mehl():
    """Flour from the mockup: a linen sack tied at the neck with a blue band and a puff of flour above."""
    s = Spr()
    rng = rng_for('mehl')
    body = ellipse(8, 10.4, 5.6, 4.8) | rect(3, 9, 12, 14)
    neck = poly([(6, 6.5), (5.2, 2.5), (10.8, 2.5), (10, 6.5)])
    paint(s, neck, SACK, rng, noise=.08, k=1.0)
    paint(s, body, SACK, rng, noise=.06, k=1.6)
    for x in range(6, 11):
        s.set(x, 6, hx('#6e4a24'))
    for x in range(5, 11):
        s.set(x, 11, SACK_BAND)
    for (x, y) in ((5, 1), (7, 0), (9, 1), (11, 0), (8, 1)):
        s.set(x, y, hx('#fbfaf5', 200))
    return s


def dough():
    """A pale kneaded ball of dough with a fold across its top and a dusting of flour."""
    s = Spr()
    rng = rng_for('dough')
    m = ellipse(8, 9.8, 6.2, 4.6) | rect(3, 12, 12, 13)
    paint(s, m, DOUGH_PALE, rng, noise=.05, k=1.4, specks=((SALT_WHITE, .05),))
    for x in range(5, 11):
        y = int(round(8.4 + math.sin((x - 5) * .55) * 1.1))
        s.set(x, y, DOUGH_PALE[1])
        s.set(x, y - 1, DOUGH_PALE[4])
    return s


def salt():
    """A wooden bowl heaped with white salt crystals."""
    s = Spr()
    rng = rng_for('salt')
    heap = ellipse(8, 9.2, 5.4, 3.6)
    paint(s, heap, SALT_HEAP, rng, noise=.18, k=1.2, specks=((SALT_HEAP[4], .12), (SALT_HEAP[1], .06)))
    bowl = ellipse(8, 10.2, 6.6, 4.4) - {(x, y) for x in range(S) for y in range(0, 10)}
    paint(s, bowl, WOOD, rng, noise=.06, k=1.4)
    for x in range(2, 14):
        if (x, 10) in bowl:
            s.set(x, 10, WOOD[3])
    return s


def wood_ash():
    """A soft grey heap of wood ash with dark bits of charcoal."""
    s = Spr()
    rng = rng_for('ash')
    m = ellipse(8, 11.0, 6.4, 3.8) | ellipse(8, 9.0, 4.2, 3.4)
    paint(s, m, ASH_GREY, rng, noise=.16, k=1.1, specks=((CHAR, .08), (ASH_GREY[4], .06)))
    return s


def lye():
    """A small grey stoneware jar with a cobalt band and a cork, holding the lye."""
    s = Spr()
    rng = rng_for('lye')
    body = ellipse(8, 10.0, 5.0, 5.0) | rect(4, 9, 11, 14)
    neck = rect(6, 3, 9, 5)
    paint(s, body, STONEWARE, rng, noise=.05, k=1.6)
    paint(s, neck, STONEWARE, rng, noise=.05, k=.8)
    for x in range(4, 12):
        for y in (9, 10):
            if (x, y) in body and (x, y) not in boundary(body):
                s.set(x, y, COBALT)
    for (x, y) in rect(6, 1, 9, 2):
        s.set(x, y, CORK[2] if y == 1 else CORK[1])
    for (x, y) in ((6, 1), (9, 2)):
        s.set(x, y, CORK[0])
    return s


def bread_crumbs():
    """A small golden heap of bread crumbs with loose crumbs around it."""
    s = Spr()
    rng = rng_for('crumbs')
    m = ellipse(8, 11.0, 5.8, 3.4) | ellipse(8, 9.4, 3.6, 2.8)
    paint(s, m, CRUMB, rng, noise=.35, k=1.0, specks=((CRUMB[1], .14), (CRUMB[4], .1)))
    for (x, y) in ((2, 13), (13, 12), (3, 10), (12, 14), (14, 10)):
        s.set(x, y, CRUMB[rng.randint(2, 4)])
    return s


def oven_door(lit):
    """The iron oven door: a light framed window low on the plate, dark when cold, a warm glow with a loaf when baking.

    The door's front maps to u 3 to 13 and v 8 to 15 of this texture; the handle covers row 9.
    """
    s = tex_noise(IRON, 'oven', base=3)
    door = rect(3, 8, 12, 14)
    for (x, y) in boundary(door):
        s.set(x, y, IRON[1])
    frame = rect(4, 10, 11, 14)
    for (x, y) in boundary(frame):
        s.set(x, y, IRON[4])
    window = rect(5, 11, 10, 13)
    for (x, y) in window:
        if lit:
            s.set(x, y, OVEN_GLOW[(x + 2 * y) % 2] if y < 13 else OVEN_GLOW[2])
        else:
            s.set(x, y, IRON[0] if (x + y) % 3 else IRON[1])
    if lit:
        for (x, y) in ((6, 12), (7, 12), (8, 12), (9, 12), (7, 11), (8, 11)):
            s.set(x, y, SEMMEL[1] if y == 12 else SEMMEL[2])
    for (x, y) in ((4, 9), (11, 9), (3, 14), (12, 14)):
        s.set(x, y, BRASS[2])
    return s


# ---------- meat dishes ----------
WURST = R('#7d7465', '#bdb3a0', '#d9d1c0', '#ece6d8', '#f8f5ee')
PINK = R('#6e3326', '#b86a58', '#d68b78', '#e8a593', '#f2c0b0')
CRUST = R('#3f1d0c', '#6e3517', '#93501f', '#b06a2c')
BREAD = R('#6b3d12', '#a8661e', '#c98a2e', '#e0aa45', '#f0c868')
HAM = R('#8a3a3a', '#d77f80', '#eda3a3')
CHEESE = R('#a07d12', '#e8c236', '#f7df6e')
CRACK = R('#2e1408', '#5a2a10', '#8a4a1c', '#b56c32', '#d89048')
GRAVY = R('#24110a', '#4a2414', '#6b3a20', '#8a5030')
CER = R('#56606c', '#a9b2bd', '#d3dae2', '#eef2f6', '#fafcfe')
BLUE = hx('#2f5fa8')
BLUE2 = hx('#5b86c8')
GLASS_EDGE = hx('#6f8e9a')
GLASS = hx('#cfe6ee', 150)
GLASS_HI = hx('#ffffff', 220)
GREEN_FLECK = hx('#3f7a22')
RAW_PORK = R('#7a3a34', '#d48a86', '#e8a8a2', '#f4c6c0')
RAW_BEEF = R('#4a1414', '#8a2a26', '#a83a32', '#c45a4c', '#d87a6a')
FAT = hx('#f6e6dc')
BREADING = R('#7a6a3a', '#c8b070', '#dcc88a', '#ece0aa')
MUSTARD_POWDER = R('#8a6a1a', '#d8b040', '#e8c860', '#f4dc8a')


def blob(cx, cy, rx, ry, wobble, seed, n=48):
    rng = random.Random(seed)
    ph = [rng.uniform(0, 6.28) for _ in range(3)]
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n
        k = 1 + wobble * (0.6 * math.sin(3 * a + ph[0]) + 0.4 * math.sin(5 * a + ph[1]) + 0.25 * math.sin(7 * a + ph[2]))
        pts.append((cx + math.cos(a) * rx * k, cy + math.sin(a) * ry * k))
    return poly(pts)


def flat(spr, mask, color):
    for (x, y) in mask:
        spr.set(x, y, color)


def _cutlet(seed):
    return blob(7.6, 8.6, 6.6, 5.0, .09, seed)


def _seam(s, m, ham, cheese, edge):
    """The ham and cheese layer showing along a diagonal cut of a filled cutlet."""
    for (x, y) in m - boundary(m):
        d = (x + .5 - 9.0) * .8 + (y + .5 - 8.5) * .6
        if -2.4 < d < 2.4:
            ad = abs(d)
            s.set(x, y, cheese[2] if ad < .7 else cheese[1] if ad < 1.0 else ham[2] if ad < 1.6 else ham[1] if ad < 2.0 else edge)


def pork_cutlet():
    """A thin raw pork cutlet, pale pink with a few streaks of fat."""
    s = Spr()
    rng = rng_for('cutlet')
    m = _cutlet(5)
    paint(s, m, RAW_PORK, rng, noise=.08, k=.8, specks=((FAT, .05),))
    for x in range(4, 11):
        s.set(x, int(round(7.0 + math.sin(x * .8) * .8)), FAT)
    return s


def mince(ramp, seed):
    """A heap of minced meat, coarse with flecks of fat."""
    s = Spr()
    rng = rng_for(seed)
    m = ellipse(8, 11.0, 6.2, 3.6) | ellipse(8, 9.2, 4.0, 3.0)
    paint(s, m, ramp, rng, noise=.4, k=1.2, specks=((FAT, .07), (ramp[1], .1)))
    return s


def breaded_cutlet():
    """A raw cutlet in a pale coat of flour, egg and crumbs, before frying."""
    s = Spr()
    rng = rng_for('breaded')
    m = _cutlet(3)
    paint(s, m, BREADING, rng, noise=.3, k=1.4, specks=((BREADING[1], .08), (BREAD[3], .05)))
    return s


def raw_cordon_bleu():
    """The breaded cutlet with its ham and cheese seam, still pale."""
    s = Spr()
    rng = rng_for('rawcb')
    m = _cutlet(9)
    paint(s, m, BREADING, rng, noise=.3, k=1.4, specks=((BREADING[1], .08), (BREAD[3], .05)))
    _seam(s, m, HAM, CHEESE, BREADING[0])
    return s


def schnitzel():
    """The mockup's Schnitzel: a golden breaded cutlet with parsley, without the lemon (the mod has none)."""
    s = Spr()
    rng = rng_for('schn')
    m = _cutlet(3)
    paint(s, m, BREAD, rng, noise=.35, k=1.6, specks=((BREAD[1], .06), (BREAD[4], .05)))
    for (x, y) in ((4, 5), (5, 4), (5, 5), (3, 6)):
        s.set(x, y, PARS[2] if (x + y) % 2 else PARS[1])
    return s


def cordon_bleu():
    """The mockup's Cordon Bleu: golden, cut open, ham and melting cheese."""
    s = Spr()
    rng = rng_for('cb')
    m = _cutlet(9)
    paint(s, m, BREAD, rng, noise=.35, k=1.6, specks=((BREAD[1], .06), (BREAD[4], .05)))
    _seam(s, m, HAM, CHEESE, BREAD[0])
    for (x, y) in ((8, 14), (9, 14), (9, 15), (10, 14)):
        s.set(x, y, CHEESE[1] if y == 14 else CHEESE[0])
    return s


def ham():
    """Two folded slices of boiled ham with a pale rim of fat."""
    s = Spr()
    rng = rng_for('ham')
    for (cx, cy) in ((6.4, 7.4), (9.6, 10.2)):
        m = ellipse(cx, cy, 5.0, 4.2)
        paint(s, m, HAM, rng, noise=.08, k=.8)
        inner = m - boundary(m)
        for (x, y) in boundary(inner):
            s.set(x, y, FAT)
    return s


def sausage_meat():
    """A pale heap of fine sausage meat with green flecks of parsley."""
    s = Spr()
    rng = rng_for('braet')
    m = ellipse(8, 11.0, 6.2, 3.6) | ellipse(8, 9.0, 4.2, 3.2)
    paint(s, m, WURST, rng, noise=.12, k=1.6, specks=((GREEN_FLECK, .08), (PINK[3], .05)))
    return s


def weisswurst():
    """The mockup's pair of white sausages with parsley flecks."""
    s = Spr()
    rng = rng_for('ww')
    for (a, b) in (((3.0, 9.6), (10.6, 2.8)), ((5.4, 13.4), (13.0, 6.6))):
        m = capsule(a[0], a[1], b[0], b[1], 2.5)
        paint(s, m, WURST, rng, noise=.05, k=2.8, specks=((GREEN_FLECK, .05),))
        for (px, py) in (a, b):
            s.set(int(px), int(py), WURST[1])
    return s


def mustard_flour():
    """A soft heap of yellow ground mustard seed."""
    s = Spr()
    rng = rng_for('senfmehl')
    m = ellipse(8, 11.4, 6.4, 3.4) | ellipse(8, 9.6, 4.0, 2.8)
    paint(s, m, MUSTARD_POWDER, rng, noise=.14, k=1.0, specks=((SENF[2], .06), (MUSTARD_POWDER[3], .08)))
    return s


def senf():
    """The mockup's Sweet Mustard: a glass jar of brown mustard with a red and white checked lid."""
    s = Spr()
    rng = rng_for('senf')
    jar = rect(4, 6, 11, 14) - {(4, 14), (11, 14)}
    flat(s, jar, GLASS)
    paint(s, rect(5, 8, 10, 13), SENF, rng, noise=.15, outline=False, k=.6)
    for (x, y) in boundary(jar):
        s.set(x, y, GLASS_EDGE)
    for y in range(7, 13):
        s.set(5, y, GLASS_HI)
    cap = ellipse(8, 4.2, 5.2, 2.6) | rect(4, 4, 11, 6)
    for (x, y) in cap:
        s.set(x, y, hx('#f4f1ea') if (x // 2 + y // 2) % 2 else hx('#c4302b'))
    for (x, y) in boundary(cap):
        s.set(x, y, hx('#6e1712'))
    for x in range(4, 12):
        s.set(x, 6, hx('#8a6a3a'))
    return s


def leberkas():
    """The mockup's Leberkaese loaf: a brown crusted block with a pink cut face."""
    s = Spr()
    rng = rng_for('lk')
    top = poly([(2, 5), (6, 2.5), (15, 2.5), (11, 5)])
    front = rect(2, 5, 10, 13)
    side = poly([(11, 5), (15, 2.5), (15, 10.5), (11, 13.99)])
    paint(s, top, CRUST, rng, noise=.2, k=.4, bias=.3)
    paint(s, side, CRUST, rng, noise=.2, k=.4, bias=-.25)
    paint(s, front, PINK, rng, noise=.12, k=.8, specks=((PINK[4], .08),))
    for x in range(2, 11):
        s.set(x, 5, CRUST[1])
    return s


def leberkaese_slice():
    """A thick pink slice of Leberkaese with its brown crust along the top."""
    s = Spr()
    rng = rng_for('lks_slice')
    top = poly([(2, 6), (4, 4), (14, 4), (12, 6)])
    front = rect(2, 6, 12, 11)
    side = poly([(13, 6), (14, 4.5), (14, 9.5), (13, 11.99)])
    paint(s, top, CRUST, rng, noise=.2, k=.4, bias=.3)
    paint(s, side, PINK, rng, noise=.1, k=.4, bias=-.3)
    paint(s, front, PINK, rng, noise=.12, k=.8, specks=((PINK[4], .08),))
    for x in range(2, 13):
        s.set(x, 6, CRUST[1])
    return s


def leberkassemmel():
    """The mockup's Leberkaese Roll: a thick slice in a Bread Roll."""
    s = Spr()
    rng = rng_for('lks')
    bottom = ellipse(8, 10.5, 6.4, 3.6) & {(x, y) for x in range(S) for y in range(11, 15)}
    paint(s, bottom, SEMMEL, rng, noise=.08, bias=-.2)
    paint(s, rect(1, 8, 14, 10), PINK, rng, noise=.1, k=1.0)
    for x in range(2, 14):
        s.set(x, 8, CRUST[2] if x % 3 else CRUST[1])
    for x in (4, 5, 9, 10, 11):
        s.set(x, 11, SENF[4])
    s.set(10, 12, SENF[3])
    top = ellipse(8, 8.2, 6.5, 5.4) & {(x, y) for x in range(S) for y in range(0, 8)}
    paint(s, top, SEMMEL, rng, noise=.1, k=2.8)
    for (x, y) in top:
        if rng.random() < .05 and (x, y) not in boundary(top):
            s.set(x, y, SALT_WHITE)
    return s


def schweinebraten():
    """The mockup's Roast Pork: a slice with a crackling top and white fat line in a pool of gravy."""
    s = Spr()
    rng = rng_for('sb')
    paint(s, ellipse(8, 11.8, 7.4, 3.4), GRAVY, rng, noise=.1, k=.3, specks=((GRAVY[3], .06),))
    m = ellipse(8, 8.4, 6.0, 5.2)
    meat = R('#5c3020', '#b07858', '#c99272', '#ddae8e')
    paint(s, m, meat, rng, noise=.1, k=1.0, pattern=lambda x, y: .1 * math.sin(x * 1.7 + y * .5))

    def crackling_line(x):
        return 6.4 + abs(x + .5 - 8) * .22

    edge = boundary(m)
    for (x, y) in m:
        yy = y + .5
        if yy < crackling_line(x):
            s.set(x, y, CRACK[0] if (x, y) in edge else CRACK[4] if (x + (y % 2)) % 3 == 0 else CRACK[3] if (x + y) % 2 else CRACK[2])
        elif yy < crackling_line(x) + 1.1 and (x, y) not in edge:
            s.set(x, y, hx('#f2e2c6'))
    for (x, y) in ((4, 10), (5, 11), (11, 11), (12, 10)):
        s.set(x, y, GRAVY[2])
    return s


def gravy_bowl():
    """A blue banded stoneware bowl of dark brown gravy."""
    s = Spr()
    rng = rng_for('gravy')
    surface = ellipse(7.5, 8.2, 6.0, 2.4) & {(x, y) for x in range(S) for y in range(0, 9)}
    paint(s, surface, GRAVY, rng, noise=.12, k=.4, bias=.2, specks=((GRAVY[3], .1),))
    body = ellipse(7.5, 7.6, 7.2, 6.8) & {(x, y) for x in range(S) for y in range(8, 15)}
    paint(s, body, CER, rng, noise=.04, k=1.8)
    for x in range(1, 15):
        if (x, 8) in body:
            s.set(x, 8, CER[4])
    for (x, y) in body - boundary(body):
        if y == 10:
            s.set(x, y, BLUE if x % 2 else BLUE2)
    return s


def leberkaese_crust():
    """The loaf's top: dark brown baked crust with lighter blisters."""
    s = tex_noise(CRUST, 'lk_crust', base=2)
    rng = rng_for('lk_crust_blisters')
    for _ in range(14):
        x, y = rng.randrange(S), rng.randrange(S)
        s.set(x, y, CRUST[3])
        s.set((x + 1) % S, y, CRUST[1])
    return s


def leberkaese_side():
    """The loaf's sides: the crust a little darker, with a dark seam where it met the tin."""
    s = tex_noise(CRUST, 'lk_side', base=1)
    for x in range(S):
        s.set(x, 15, CRUST[0])
    return s


def leberkaese_inside():
    """The cut face: fine pink meat with light flecks."""
    s = tex_noise(PINK, 'lk_inside', base=2)
    rng = rng_for('lk_inside_flecks')
    for y in range(S):
        for x in range(S):
            if rng.random() < .08:
                s.set(x, y, PINK[4])
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
SPRITES.update({
    'block/stove_tile': kachel().image(),
    'block/stove_tile_door': kachel_door(False).image(),
    'block/stove_tile_door_lit': strip(kachel_door(True), kachel_door(True, flicker=True)),
    'block/stove_plate': herd_top(False).image(),
    'block/stove_plate_lit': herd_top(True).image(),
    'item/boiled_potatoes': boiled_potatoes().image(),
    'item/fried_egg': fried_egg().image(),
    'item/fried_onions': fried_onions().image(),
    'item/fried_potatoes': fried_potatoes().image(),
    'item/chopped_onion': chopped_onion().image(),
    'item/shredded_cabbage': shredded_cabbage().image(),
    'item/kitchen_knife': kitchen_knife().image(),
    'item/bread_roll': semmel().image(),
    'item/flour': mehl().image(),
    'item/dough': dough().image(),
    'item/salt': salt().image(),
    'item/wood_ash': wood_ash().image(),
    'item/lye': lye().image(),
    'item/bread_crumbs': bread_crumbs().image(),
    'block/baking_oven_door': oven_door(False).image(),
    'block/baking_oven_door_lit': oven_door(True).image(),
})


# ---------- sides: Fermenting Crock, sauerkraut, red cabbage, cheese, dumplings ----------
KRAUT = R('#6f6526', '#a89a48', '#cfc27a', '#e6dca2', '#f3edc8')
SKN = R('#6b5530', '#a88c58', '#cdb37c', '#e3cf9e', '#f1e4c0')
KKN = R('#7a6e4e', '#bfb185', '#d9cea6', '#ebe3c6', '#f8f3e2')
CHEESE_RAMP = R('#a07d12', '#d9ae2a', '#e8c236', '#f7df6e')
CHEESE_RIND = hx('#c98f1c')
CROCK = R('#2e1c10', '#4e3019', '#6a4324', '#835632', '#9c6a40', '#b58254')
SEAL_WATER = R('#24364e', '#35506e', '#4a6e92')


def bowl(fill_ramp, seed, specks=(), pattern=None, mound=3.4):
    """A white bowl with a blue band, heaped with food."""
    s = Spr()
    rng = rng_for(seed)
    mnd = ellipse(7.5, 8.2, 6.0, mound) & {(x, y) for x in range(S) for y in range(0, 9)}
    paint(s, mnd, fill_ramp, rng, noise=.25, k=1.4, specks=specks, pattern=pattern)
    body = ellipse(7.5, 7.6, 7.2, 6.8) & {(x, y) for x in range(S) for y in range(8, 15)}
    paint(s, body, CER, rng, noise=.04, k=1.8)
    for x in range(1, 15):
        if (x, 8) in body:
            s.set(x, 8, CER[4])
    for (x, y) in body - boundary(body):
        if y == 10:
            s.set(x, y, BLUE if x % 2 else BLUE2)
    return s


def sauerkraut_bowl():
    return bowl(KRAUT, 'sk', pattern=lambda x, y: .35 * math.sin(x * 1.9 + y * 2.7))


def blaukraut_bowl():
    return bowl(REDK, 'bk', pattern=lambda x, y: .3 * math.sin(x * 2.3 - y * 2.1), specks=((hx('#b33b3b'), .03),))


def potato_salad():
    """The bowl heaped with potato slices, onion and parsley."""
    return bowl(KKN, 'sa', specks=((PARS[2], .08), (ONION[3], .06)), pattern=lambda x, y: .25 * math.sin(x * 1.4 + y * .6))


def knoedel(ramp, seed, specks):
    s = Spr()
    rng = rng_for(seed)
    paint(s, ellipse(8, 13.0, 6.6, 2.0), GRAVY, rng, noise=.1, k=.3)
    paint(s, ellipse(8, 8.3, 5.7, 5.5), ramp, rng, noise=.12, k=2.4, specks=specks)
    return s


def semmelknoedel():
    return knoedel(SKN, 'skn', ((SKN[1], .12), (SKN[4], .06), (GREEN_FLECK, .06), (SEMMEL[2], .05)))


def kartoffelknoedel():
    s = knoedel(KKN, 'kkn', ((KKN[1], .03),))
    s.set(6, 5, KKN[4])
    s.set(5, 6, hx('#ffffff'))
    return s


def heap(ramp, seed, pattern, specks=()):
    """A loose heap of cooked or fermented cabbage, without a bowl."""
    s = Spr()
    rng = rng_for(seed)
    m = ellipse(8, 11.2, 6.6, 3.6) | ellipse(8, 8.8, 4.6, 3.6)
    paint(s, m, ramp, rng, noise=.25, k=1.2, specks=specks, pattern=pattern)
    return s


def grated_potato():
    """A pale heap of short potato gratings."""
    s = Spr()
    rng = rng_for('gp')
    m = ellipse(8, 11.0, 6.0, 3.4) | ellipse(8, 9.0, 4.0, 3.0)
    paint(s, m, POTATO, rng, noise=.1, k=1.0, bias=.15)
    for _ in range(16):
        x, y = rng.choice(sorted(m - boundary(m)))
        s.set(x, y, POTATO[1])
        s.set(x + 1, y, POTATO[4])
    return s


def cheese():
    """A wedge of yellow cheese seen from the front: a light top, a darker cut face with holes and a rind on the back."""
    s = Spr()
    rng = rng_for('cheese')
    top = poly([(1, 10), (12, 3), (15.5, 6.5), (4.5, 11.5)])
    face = poly([(1, 10), (4.5, 11.5), (15.5, 6.5), (15.5, 11.5), (4.5, 15.5), (1, 14)])
    paint(s, face, CHEESE_RAMP, rng, noise=.05, k=.3, bias=-.2)
    paint(s, top, CHEESE_RAMP, rng, noise=.04, k=.2, bias=.35)
    for (x, y) in face:
        if x >= 14 and (x, y) not in boundary(face):
            s.set(x, y, CHEESE_RIND)
    for (cx, cy) in ((4, 13), (8, 12), (11, 11), (7, 14)):
        s.set(cx, cy, CHEESE_RAMP[0])
        s.set(cx + 1, cy, CHEESE_RAMP[1])
    for (x, y) in ((8, 7), (11, 5)):
        s.set(x, y, CHEESE_RAMP[2])
    return s


def crock_side():
    """Brown salt glazed stoneware: a lighter glazed rim at the top, a dark band round the belly, darker drips."""
    rng = rng_for('crock_side')
    s = tex_noise(CROCK, 'crock_side', base=3)
    for x in range(S):
        for y in range(4, 8):
            s.set(x, y, CROCK[4] if y == 6 else CROCK[5] if y == 5 and rng.random() < .3 else CROCK[4 - (y == 7)])
        s.set(x, 11, CROCK[1])
        s.set(x, 12, CROCK[2] if x % 3 else CROCK[1])
        s.set(x, 15, CROCK[2])
        if rng.random() < .3:
            for y in range(8, 8 + rng.randint(1, 3)):
                s.set(x, y, CROCK[2])
    return s


def crock_top():
    """The top of the belly: dark inside the neck, the water of the seal in the ring between lip and neck."""
    s = tex_noise(CROCK, 'crock_top', base=3)
    for y in range(S):
        for x in range(S):
            ring = max(abs(x - 7.5), abs(y - 7.5))
            if ring < 3:
                s.set(x, y, CROCK[0] if (x + y) % 3 else CROCK[1])
            elif 4 < ring < 5:
                s.set(x, y, SEAL_WATER[2] if (x * 3 + y) % 5 == 0 else SEAL_WATER[1])
    return s


def crock_lid():
    """The lid's glaze, a shade lighter than the crock, with a light ring."""
    s = tex_noise(CROCK, 'crock_lid', base=4)
    for y in range(S):
        for x in range(S):
            if abs(max(abs(x - 7.5), abs(y - 7.5)) - 3.5) < .6:
                s.set(x, y, CROCK[5])
    return s


SPRITES.update({
    'item/bowl_of_sauerkraut': sauerkraut_bowl().image(),
    'item/bowl_of_red_cabbage': blaukraut_bowl().image(),
    'item/potato_salad': potato_salad().image(),
    'item/bread_dumpling': semmelknoedel().image(),
    'item/potato_dumpling': kartoffelknoedel().image(),
    'item/sauerkraut': heap(KRAUT, 'skh', lambda x, y: .35 * math.sin(x * 1.9 + y * 2.7)).image(),
    'item/braised_red_cabbage': heap(REDK, 'bkh', lambda x, y: .3 * math.sin(x * 2.3 - y * 2.1), ((hx('#b33b3b'), .03),)).image(),
    'item/shredded_red_cabbage': shredded(REDK, 'src').image(),
    'item/grated_potato': grated_potato().image(),
    'item/cheese': cheese().image(),
    'block/crock_side': crock_side().image(),
    'block/crock_top': crock_top().image(),
    'block/crock_lid': crock_lid().image(),
})


SPRITES.update({
    'item/pork_cutlet': pork_cutlet().image(),
    'item/minced_pork': mince(RAW_PORK + [hx('#f8dad4')], 'mince_pork').image(),
    'item/minced_beef': mince(RAW_BEEF, 'mince_beef').image(),
    'item/breaded_cutlet': breaded_cutlet().image(),
    'item/raw_cordon_bleu': raw_cordon_bleu().image(),
    'item/schnitzel': schnitzel().image(),
    'item/cordon_bleu': cordon_bleu().image(),
    'item/ham': ham().image(),
    'item/sausage_meat': sausage_meat().image(),
    'item/weisswurst': weisswurst().image(),
    'item/mustard_flour': mustard_flour().image(),
    'item/sweet_mustard': senf().image(),
    'item/leberkaese': leberkas().image(),
    'item/leberkaese_slice': leberkaese_slice().image(),
    'item/leberkaese_roll': leberkassemmel().image(),
    'item/roast_pork': schweinebraten().image(),
    'item/gravy': gravy_bowl().image(),
    'block/leberkaese_crust': leberkaese_crust().image(),
    'block/leberkaese_side': leberkaese_side().image(),
    'block/leberkaese_inside': leberkaese_inside().image(),
})
ANIMATIONS = {'block/stove_tile_door_lit': DOOR_FLICKER}


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
    for name, meta in ANIMATIONS.items():
        with open(os.path.join(TEXTURES, name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write(json.dumps(meta, indent=2) + '\n')


if __name__ == '__main__':
    main()
