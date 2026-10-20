"""Generates the Servus: Brewery item and block textures.

This script is the source of the files; edit the shapes here, not the PNGs.

    python tools/brewery-textures/gen.py                      writes the textures
    python tools/brewery-textures/gen.py --preview <folder>   also writes 8x previews

Written into brewery/src/main/resources/assets/servus_brewery/textures/item/ and .../block/.
Sprites are drawn from pixel masks on a 16x16 canvas. Block sprites use only fully
opaque or fully clear pixels, so the game draws them in the cutout layer.
"""
import json
import math
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEXTURES = os.path.join(ROOT, 'brewery', 'src', 'main', 'resources', 'assets', 'servus_brewery', 'textures')
SIZE = 16
CLEAR = (0, 0, 0, 0)


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def ramp(*hexes):
    return [hx(h) for h in hexes]


GLASS = (207, 230, 238, 150)
GLASS_EDGE = (111, 142, 154, 255)
GLASS_GLINT = (255, 255, 255, 170)

BARLEY = ramp('#6b5320', '#a88a3a', '#cdb25a', '#e6d38a')
HOPS = ramp('#2f4a14', '#5f8a2a', '#8fb84a', '#bcd97a', '#ddeeaa')
LEAF = ramp('#1f4012', '#3a6a1f', '#56902e', '#7db544')
ROOT_BROWN = ramp('#4f3a24', '#8f7350', '#b89a72', '#d6c09c', '#ebdcc0')
POLE = ramp('#3b2614', '#5e3d20', '#7a5230', '#966a40', '#b0844f')
STALK_GREEN = hx('#7a9a36')
STALK_DARK = hx('#5a7a26')
STALK_RIPE = hx('#b39a48')
STALK_RIPE_DARK = hx('#8f7a34')
EAR_GREEN = ramp('#4d6a1e', '#7d9c38', '#a3c25a', '#c4da84')
STEM = hx('#4a6b22')
MALT = ramp('#4a2a0e', '#7e4c1c', '#a86a2a', '#c98c44', '#e0ad66')
DARK_MALT = ramp('#1e0c04', '#3e1c0c', '#5e3016', '#7e4822', '#9a5e30')
WHEAT_MALT = ramp('#6b4a1a', '#a8762e', '#cf9a48', '#e6bb6a', '#f2d692')
GREEN_MALT = ramp('#5e5a26', '#8f8a42', '#b8b064', '#d6cf8c', '#ebe6b6')
GREEN_WHEAT = ramp('#6a5a24', '#a08a40', '#c8b062', '#e0cc8a', '#f0e3b4')
SOAKED_BARLEY = ramp('#5a4a22', '#8a7638', '#ad9852', '#c8b672', '#dccd98')
SOAKED_WHEAT = ramp('#5e4620', '#927034', '#b8924c', '#d2ae6a', '#e4c992')
ROOTLET = hx('#f4f0de')
WATER = hx('#7fb2d8')


# ---------- masks ----------
def rect(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


def ellipse(cx, cy, rx, ry):
    return {(x, y) for y in range(SIZE) for x in range(SIZE)
            if ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1}


def capsule(x1, y1, x2, y2, r, r2=None):
    """Pixels whose centre lies within r (growing to r2 at the end) of the segment from (x1, y1) to (x2, y2)."""
    r2 = r if r2 is None else r2
    out = set()
    dx, dy = x2 - x1, y2 - y1
    for y in range(SIZE):
        for x in range(SIZE):
            px, py = x + .5, y + .5
            t = max(0, min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)))
            if math.hypot(px - x1 - t * dx, py - y1 - t * dy) <= r + (r2 - r) * t:
                out.add((x, y))
    return out


def poly(points):
    out = set()
    for y in range(SIZE):
        for x in range(SIZE):
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


# ---------- painting ----------
LIGHT = (-0.55, -0.75, 0.65)


def canvas():
    return [[CLEAR] * SIZE for _ in range(SIZE)]


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = color


def fill(img, mask, color):
    for (x, y) in mask:
        put(img, x, y, color)


def heights(mask):
    outside = [(x, y) for y in range(-1, SIZE + 1) for x in range(-1, SIZE + 1) if (x, y) not in mask]
    d = {(x, y): min(math.hypot(x - ox, y - oy) for ox, oy in outside) for (x, y) in mask}
    m = max(d.values()) if d else 1
    return {k: math.sqrt(min(v / m, 1)) for k, v in d.items()}


def paint(img, mask, colors, rng, noise=0.12, outline=True, k=2.2, bias=0.0):
    """Shades a mask as a soft dome lit from the top left; colors run dark to light, colors[0] is the outline."""
    if not mask:
        return
    h = heights(mask)
    lx, ly, lz = LIGHT
    ln = math.sqrt(lx * lx + ly * ly + lz * lz)
    edge = boundary(mask) if outline else set()
    n = len(colors)
    lo = 1 if outline else 0
    for (x, y) in mask:
        if (x, y) in edge:
            put(img, x, y, colors[0])
            continue
        gx = h.get((x + 1, y), 0) - h.get((x - 1, y), 0)
        gy = h.get((x, y + 1), 0) - h.get((x, y - 1), 0)
        nx, ny, nz = -gx * k, -gy * k, 1
        nn = math.sqrt(nx * nx + ny * ny + nz * nz)
        dot = (nx * lx + ny * ly + nz * lz) / (nn * ln)
        v = (dot - 0.2) / 0.8 + bias + rng.uniform(-noise, noise)
        idx = lo + int(round(max(0, min(1, v)) * (n - 1 - lo)))
        put(img, x, y, colors[max(lo, min(n - 1, idx))])


# ---------- items ----------
def empty_glass(top=4, left=3, right=10):
    """The empty half litre glass: a straight body with glints and a handle ring on the right."""
    img = canvas()
    shell = rect(left, top, right, 14)
    fill(img, shell, GLASS)
    fill(img, boundary(shell), GLASS_EDGE)
    glints = {(x, y) for y in range(top + 1, 14) for x in range(left + 1, right)
              if (x - left) % 3 == 1 and (y - top) % 3 == 1}
    fill(img, glints, GLASS_GLINT)
    ring = capsule(right + 2.2, top + 3.5, right + 2.2, top + 8.5, 2.0) - capsule(right + 2.0, top + 3.5, right + 2.0, top + 8.5, .9)
    fill(img, {(x, y) for (x, y) in ring if x > right}, GLASS_EDGE)
    return img


def barley():
    """A bent barley stalk with an ear of paired grains and long awns (the mockup's gerste)."""
    img = canvas()
    fill(img, capsule(3, 15, 8.5, 8.5, .55), hx('#9aa04a'))
    for i in range(5):
        px, py = 8.6 + i * 1.0, 8.2 - i * 1.25
        for side in (-1, 1):
            gx, gy = px + side * 1.0, py + side * .6
            fill(img, ellipse(gx, gy, .9, .9), BARLEY[2] if side < 0 else BARLEY[1])
            for t in range(1, 5):
                put(img, int(gx + side * .3 + t * .4), int(gy - t * 1.1 - .6), BARLEY[3])
    put(img, 13, 2, BARLEY[2])
    return img


def hops():
    """A hop cone with scale rows and a leaf on its stem (the mockup's hopfen)."""
    img = canvas()
    rng = random.Random('hop')
    m = ellipse(8, 9.6, 4.4, 5.8)
    paint(img, m, HOPS, rng, noise=.05, k=1.6)
    for (x, y) in m - boundary(m):
        if (y + (1 if (x // 2) % 2 else 0)) % 3 == 0:
            put(img, x, y, HOPS[1])
    for (x, y) in ((8, 3), (8, 2), (9, 1)):
        put(img, x, y, HOPS[0])
    paint(img, poly([(9, 2.5), (14.5, 1.0), (13.5, 5.5)]), LEAF, rng, noise=.1, k=1.0)
    return img


def barley_seeds():
    """A small heap of pale husked barley grains."""
    img = canvas()
    rng = random.Random('barley_seeds')
    for (x1, y1, x2, y2) in ((4.5, 5.5, 6.5, 8.5), (9.5, 3.5, 11, 6.5), (10, 9, 12.5, 11), (3.5, 11, 6.5, 12.5), (7.5, 10.5, 8.5, 13.5)):
        paint(img, capsule(x1, y1, x2, y2, 1.4), BARLEY, rng, noise=.05, k=1.2, bias=.2)
    return img


def hop_rhizome():
    """A knobby brown root cutting with a green shoot."""
    img = canvas()
    rng = random.Random('hop_rhizome')
    root = capsule(2.5, 12.5, 12.5, 8.5, 1.5, 1.9) | ellipse(5.5, 12.6, 1.6, 1.4) | ellipse(10.5, 10.8, 1.5, 1.5)
    paint(img, root, ROOT_BROWN, rng, noise=.1, k=1.8)
    for (x, y) in ((4, 14), (3, 15), (8, 13), (8, 14), (12, 11), (13, 12)):
        put(img, x, y, ROOT_BROWN[0])
    fill(img, capsule(10.5, 8, 11.5, 3.5, .5), STEM)
    paint(img, poly([(11.5, 4.5), (15, 2.5), (13.5, 6.5)]), LEAF, rng, noise=.1, k=1.0)
    paint(img, poly([(11, 5.5), (7.5, 3.5), (9, 7)]), LEAF, rng, noise=.1, k=1.0)
    return img


def hop_pole_item():
    """A slim wooden pole lying diagonally, pointed at the bottom end."""
    img = canvas()
    rng = random.Random('hop_pole')
    pole = capsule(2.5, 14.5, 13, 2, .6, 1.25)
    paint(img, pole, POLE, rng, noise=.08, k=1.4)
    return img


def heap(colors, seed, specks=26):
    """A heap of grain with paired grain specks (the mockup's malz)."""
    img = canvas()
    rng = random.Random(seed)
    m = ellipse(8, 11.4, 7.2, 4.4)
    paint(img, m, colors, rng, noise=.1, k=1.2)
    for _ in range(specks):
        x = rng.randint(2, 13)
        y = rng.randint(8, 14)
        if (x, y) in m and (x + 1, y) in m:
            put(img, x, y, colors[rng.randint(2, 4)])
            put(img, x + 1, y, colors[1])
    return img


def sprouted(colors, seed):
    """A pale grain heap with white rootlets curling out of the top."""
    img = heap(colors, seed, specks=18)
    rng = random.Random(seed + '_roots')
    m = ellipse(8, 11.4, 7.2, 4.4)
    edge = sorted(p for p in boundary(m) if p[1] <= 10)
    for (x, y) in rng.sample(edge, 6):
        put(img, x, y - 1, ROOTLET)
        put(img, x + (1 if x < 8 else -1), y - 2, ROOTLET)
    for _ in range(5):
        x, y = rng.randint(4, 11), rng.randint(9, 13)
        if (x, y) in m:
            put(img, x, y, ROOTLET)
    return img


def soaked(colors, seed):
    """A damp grain heap with blue water glints."""
    img = heap(colors, seed, specks=22)
    rng = random.Random(seed + '_water')
    m = ellipse(8, 11.4, 7.2, 4.4)
    inside = m - boundary(m)
    for (x, y) in rng.sample(sorted(inside), 7):
        put(img, x, y, WATER)
    return img


def grist(colors, seed):
    """A low heap of coarse ground malt: per pixel noise instead of whole grains."""
    img = canvas()
    rng = random.Random(seed)
    m = ellipse(8, 12, 6.8, 3.4) | ellipse(8, 10.6, 4.2, 2.6)
    paint(img, m, colors, rng, noise=.25, k=1.0)
    for (x, y) in m - boundary(m):
        if rng.random() < .22:
            put(img, x, y, colors[rng.randint(1, 4)])
    return img


# ---------- blocks ----------
STALKS = (2, 6, 10, 14)
STALK_HEIGHT = (3, 5, 7, 9, 9, 9, 10, 10)


def barley_stage(age):
    """Barley stalks for crop stage age (0 to 7): taller with age, ears from stage 4, golden at 7."""
    img = canvas()
    ripe = age >= 6
    stalk = STALK_RIPE if ripe else STALK_GREEN
    dark = STALK_RIPE_DARK if ripe else STALK_DARK
    ear = BARLEY if age == 7 else (ramp('#7d8a34', '#a8ac4a', '#c6c06a', '#dcd896') if age == 6 else EAR_GREEN)
    for i, x in enumerate(STALKS):
        height = STALK_HEIGHT[age] - (i % 2)
        top = 16 - height
        lean = 1 if i % 2 else -1
        for y in range(top, 16):
            put(img, x, y, dark if y > 12 else stalk)
        if age >= 1:
            put(img, x + lean, 13 - (i % 3), stalk)
        if age >= 4:
            ear_len = 2 if age == 4 else 4
            for y in range(top - ear_len, top + 1):
                side = 1 if (y + i) % 2 else -1
                put(img, x, y, ear[1])
                put(img, x + side, y, ear[2])
            if age >= 5:
                for t in range(1, 3):
                    put(img, x - 1 - t // 2, top - ear_len - t, ear[3])
                    put(img, x + 1 + t // 2, top - ear_len - t, ear[3])
    return img


def hop_pole_block():
    """Vertical bark grain; the pole model shows columns 7 and 8 on its sides."""
    img = canvas()
    stripes = (2, 3, 1, 3, 2, 4, 2, 3, 3, 1, 2, 3, 4, 2, 1, 3)
    for y in range(SIZE):
        for x in range(SIZE):
            img[y][x] = POLE[stripes[x]]
    for (x, y) in ((7, 3), (8, 4), (7, 10), (8, 12), (8, 13)):
        put(img, x, y, POLE[0])
    for (x, y) in ((7, 6), (8, 8), (7, 14)):
        put(img, x, y, POLE[4])
    return img


def leaf(img, cx, cy, size, rng):
    """A small three lobed hop leaf centred on (cx, cy)."""
    lobes = ellipse(cx, cy, size, size * .8) | ellipse(cx - size * .7, cy - size * .5, size * .6, size * .6) \
        | ellipse(cx + size * .7, cy - size * .5, size * .6, size * .6)
    paint(img, lobes, LEAF, rng, noise=.1, k=1.0, bias=.15)


def bine(img, top):
    """The twining stem, winding across columns 5 to 10 from the bottom up to row top."""
    for y in range(top, 16):
        put(img, 5 + round(2.5 + 2.5 * math.sin(y * .9)), y, STEM)


def hop_vine_young():
    """Young shoots in the lower half (rows 8 to 15, columns 5 to 10)."""
    img = canvas()
    rng = random.Random('hop_vine_young')
    bine(img, 9)
    leaf(img, 6.5, 13.5, 1.3, rng)
    leaf(img, 9.5, 10.8, 1.2, rng)
    return img


def hop_vine():
    """Leaves along the twining stem over the whole height (columns 5 to 10)."""
    img = canvas()
    rng = random.Random('hop_vine')
    bine(img, 0)
    for (cx, cy) in ((6.4, 2.0), (9.6, 5.6), (6.6, 9.0), (9.4, 12.6), (6.8, 15.2)):
        leaf(img, cx, cy, 1.6, rng)
    return img


def hop_cones():
    """Hanging Hop cones with short stems over the outer planes (columns 3 to 12)."""
    img = canvas()
    rng = random.Random('hop_cones')
    for (cx, cy) in ((5.0, 4.0), (10.5, 2.5), (8.0, 8.5), (4.5, 12.0), (11.5, 11.0)):
        put(img, int(cx), int(cy - 2.6), STEM)
        cone = ellipse(cx, cy, 1.6, 2.4)
        paint(img, cone, HOPS, rng, noise=.05, k=1.4, outline=False, bias=.1)
        for (x, y) in cone:
            if (y + x // 2) % 3 == 0:
                put(img, x, y, HOPS[1])
        put(img, int(cx), int(cy + 2.2), HOPS[0])
    return img


def tex_noise(colors, seed, base=2, spread=1):
    """A full tile of the ramp's base colour with some pixels one step darker or lighter."""
    rng = random.Random(seed)
    img = canvas()
    for y in range(SIZE):
        for x in range(SIZE):
            step = rng.randint(-spread, spread) * (1 if rng.random() < .35 else 0)
            img[y][x] = colors[max(0, min(len(colors) - 1, base + step))]
    return img


def grain_floor(colors, seed, specks_seed):
    """Grain spread on the floor: noise with 40 paired grain specks (the mockup's malt_floor)."""
    img = tex_noise(colors, seed, base=2)
    rng = random.Random(specks_seed)
    for _ in range(40):
        x, y = rng.randint(0, 14), rng.randint(0, 15)
        put(img, x, y, colors[4])
        put(img, x + 1, y, colors[1])
    return img


# ---------- brewhouse (ported from the mockup's sudhaus) ----------
IRON = ramp('#15171a', '#24272c', '#33373e', '#454a52', '#5b616a')
BRASS = ramp('#5e4514', '#9a7428', '#c89c44', '#e8c46a')
COPPER = ramp('#5a2a14', '#8e4424', '#b8622e', '#d8844a', '#f0a870')
GLOW = [hx('#ffe08a'), hx('#ffb030'), hx('#e0601a')]


def kettle_copper():
    """Copper sheet with two riveted bands and seam dots (the mockup's copper over noise instead of vanilla copper)."""
    img = tex_noise(COPPER, 'copper', base=2)
    for y in (2, 13):
        for x in range(SIZE):
            put(img, x, y, COPPER[4] if x % 4 else COPPER[3])
            put(img, x, y + 1, COPPER[1])
    for y in range(3, 13):
        if y % 3 == 0:
            put(img, 3, y, COPPER[0])
            put(img, 12, y, COPPER[0])
    return img


def fire_door(lit, flicker=False):
    """The iron fire door with a brass frame and two slits that glow while the fire burns (the mockup's herd_front)."""
    img = tex_noise(IRON, 'hf', base=2)
    for x in range(SIZE):
        put(img, x, 0, BRASS[3])
        put(img, x, 1, BRASS[1])
        put(img, x, 15, IRON[0])
    for y in range(2, 15):
        put(img, 0, y, IRON[1])
        put(img, 15, y, IRON[0])
    door = rect(3, 4, 12, 11)
    fill(img, door, IRON[3])
    for (x, y) in boundary(door):
        put(img, x, y, BRASS[2] if y == 4 or x == 3 else BRASS[1])
    for y in (6, 8):
        for x in range(5, 11):
            if not lit:
                put(img, x, y, IRON[0])
            elif flicker:
                put(img, x, y, [GLOW[1], GLOW[2], GLOW[0]][(x * 2 + y) % 3])
            else:
                put(img, x, y, GLOW[(x + y) % 3])
    put(img, 11, 7, BRASS[3])
    put(img, 11, 8, BRASS[2])
    for x in range(5, 11):
        put(img, x, 13, IRON[1])
    put(img, 7, 13, BRASS[2])
    put(img, 8, 13, BRASS[2])
    for (x, y) in ((1, 2), (14, 2), (1, 14), (14, 14)):
        put(img, x, y, IRON[4])
    return img


def kiln_grate():
    """An iron lattice, clear between the bars (the mockup's grate)."""
    img = canvas()
    for y in range(SIZE):
        for x in range(SIZE):
            if x % 3 == 0 or y % 3 == 0:
                put(img, x, y, IRON[3])
    return img


def strip(*frames):
    """Animation frames stacked top to bottom into one texture."""
    return [row for frame in frames for row in frame]


# The door's flicker, as on the Tiled Stove: frame indices of the strip and ticks per frame.
DOOR_FLICKER = {'animation': {'frames': [0, 1, 0, 0, 1, 1, 0, 1], 'frametime': 3}}


# ---------- beer (ported from the mockup's mug, dunkles and weissbier) ----------
HELL = ramp('#8a4e06', '#c77d0e', '#e6a01c', '#f5c14a')
WEISS = ramp('#a5560c', '#d98622', '#eba845', '#f5c873')
FOAM = ramp('#a8a294', '#d8d4c8', '#f0eee6', '#fdfcf8')
WORT = ramp('#6e4410', '#8e5a18', '#a8701f', '#c8902a', '#dcae52')
YEAST = ramp('#7a6236', '#b89c62', '#d6bd84', '#e8d6a4', '#f6ebc6')
PAPER = ramp('#8c8a84', '#c8c6c0', '#e4e2dc', '#f6f5f2')
SPRUCE = ramp('#3d2a16', '#553b20', '#6b4a29', '#7d5a34', '#8f6a3f')
GLASS_HI = (255, 255, 255, 220)
BUBBLE = (255, 246, 216, 200)
# Helles colours the Dunkles icon replaces: the beer's middle and dark tone.
DUNKLES_RECOLOUR = {(230, 160, 28, 255): (112, 58, 22, 255), (199, 125, 14, 255): (78, 38, 12, 255)}


def full_glass(beer, seed, top=4, left=3, right=10):
    """A full half litre glass: beer darkening to the bottom, bubbles, a glint, a foam head and the handle ring."""
    img = canvas()
    rng = random.Random(seed)
    shell = rect(left, top, right, 14)
    fill(img, shell, GLASS)
    inside = rect(left + 1, top + 2, right - 1, 13)
    for (x, y) in inside:
        t = (y - top) / (14 - top)
        v = 1 - t * .8 + rng.uniform(-.12, .12)
        put(img, x, y, beer[max(0, min(len(beer) - 1, int(v * (len(beer) - 1) + .5)))])
    fill(img, boundary(shell), GLASS_EDGE)
    fill(img, {(x, y) for (x, y) in inside if (x - left) % 3 == 1 and (y - top) % 3 == 1}, BUBBLE)
    for y in range(top + 2, 13):
        put(img, left + 1, y, GLASS_HI)
    foam = ellipse((left + right + 1) / 2, top + .8, (right - left + 1) / 2 + .2, 2.0) | rect(left, top, right, top + 1)
    paint(img, foam, FOAM, rng, noise=.15, k=1.0, bias=.25)
    put(img, left + 2, top + 2, FOAM[2])
    ring = capsule(right + 2.2, top + 3.5, right + 2.2, top + 8.5, 2.0) - capsule(right + 2.0, top + 3.5, right + 2.0, top + 8.5, .9)
    fill(img, {(x, y) for (x, y) in ring if x > right}, GLASS_EDGE)
    return img


def recoloured(img, colours):
    return [[colours.get(px, px) for px in row] for row in img]


def weissbier(full):
    """The tall wheat beer glass, narrow at the foot and wide at the top; full with a foam crown."""
    img = canvas()
    rng = random.Random('weiss')

    def half_width(y):
        if y >= 14:
            return 3.0
        t = (13 - y) / 12
        return 2.2 + 2.2 * t * t

    shell = {(x, y) for y in range(1, 16) for x in range(SIZE) if abs(x + .5 - 8) <= half_width(y)}
    fill(img, shell, GLASS)
    for (x, y) in shell:
        if full and 4 <= y <= 13 and abs(x + .5 - 8) <= half_width(y) - 1:
            v = .9 - (y - 5) / 12 + rng.uniform(-.2, .2)
            put(img, x, y, WEISS[max(0, min(3, int(v * 3 + .5)))])
    fill(img, boundary(shell), GLASS_EDGE)
    if not full:
        return img
    foam = {(x, y) for (x, y) in shell if y <= 4} | ellipse(8, 1.6, 3.6, 1.6)
    paint(img, foam, FOAM, rng, noise=.15, k=1.0, bias=.3)
    for y in range(6, 13):
        put(img, int(8 - half_width(y) + 1.2), y, GLASS_HI)
    return img


def yeast():
    """A cube of fresh yeast, half wrapped in paper, with a few crumbs on top."""
    img = canvas()
    rng = random.Random('yeast')
    cake = rect(3, 4, 12, 12)
    paint(img, cake, YEAST, rng, noise=.1, k=1.4)
    for (x, y) in ((5, 6), (8, 5), (10, 7), (6, 8), (9, 9)):
        put(img, x, y, YEAST[1])
    wrapper = rect(2, 9, 13, 13)
    paint(img, wrapper, PAPER, rng, noise=.06, k=0.8)
    for x in range(3, 13):
        put(img, x, 10, PAPER[1] if x % 3 else PAPER[3])
    return img


def vat_side():
    """Spruce staves with two iron bands and rivets (the mockup's vat_side, staves drawn instead of vanilla planks)."""
    rng = random.Random('vat')
    img = canvas()
    for x in range(SIZE):
        stave = x // 4
        for y in range(SIZE):
            base = 2 + (1 if stave % 2 else 0) - (1 if x % 4 == 0 else 0)
            step = rng.choice((-1, 0, 0, 0, 1))
            img[y][x] = SPRUCE[max(0, min(len(SPRUCE) - 1, base + step))]
    for y in (2, 12):
        for x in range(SIZE):
            put(img, x, y, IRON[2])
            put(img, x, y + 1, IRON[1])
        for x in (2, 10):
            put(img, x, y, IRON[4])
    return img


def liquid(colors, seed, foam=False):
    """A still surface: the ramp's noise with light flecks, more and wider for foam (the mockup's liquid)."""
    img = tex_noise(colors, seed, base=2)
    rng = random.Random(seed)
    for _ in range(14 if foam else 6):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        put(img, x, y, colors[-1])
        if foam:
            put(img, x + 1, y, colors[-1])
    return img


ITEM_SPRITES = {
    'beer_glass': empty_glass(),
    'barley': barley(),
    'barley_seeds': barley_seeds(),
    'hops': hops(),
    'hop_rhizome': hop_rhizome(),
    'hop_pole': hop_pole_item(),
    'soaked_barley': soaked(SOAKED_BARLEY, 'soaked_barley'),
    'soaked_wheat': soaked(SOAKED_WHEAT, 'soaked_wheat'),
    'green_malt': sprouted(GREEN_MALT, 'green_malt'),
    'green_wheat_malt': sprouted(GREEN_WHEAT, 'green_wheat_malt'),
    'pale_malt': heap(MALT, 'malz'),
    'dark_malt': heap(DARK_MALT, 'dark_malt'),
    'wheat_malt': heap(WHEAT_MALT, 'wheat_malt'),
    'pale_malt_grist': grist(MALT, 'pale_malt_grist'),
    'dark_malt_grist': grist(DARK_MALT, 'dark_malt_grist'),
    'wheat_malt_grist': grist(WHEAT_MALT, 'wheat_malt_grist'),
    'yeast': yeast(),
    'weissbier_glass': weissbier(False),
    'helles': full_glass(HELL, 'helles'),
    'dunkles': recoloured(full_glass(HELL, 'helles'), DUNKLES_RECOLOUR),
    'weissbier': weissbier(True),
}

BLOCK_SPRITES = {
    **{'barley_stage%d' % age: barley_stage(age) for age in range(8)},
    'hop_pole': hop_pole_block(),
    'hop_vine_young': hop_vine_young(),
    'hop_vine': hop_vine(),
    'hop_cones': hop_cones(),
    'malt_floor': grain_floor(MALT, 'mf', 4),
    'soaked_grain_floor': grain_floor(SOAKED_BARLEY, 'sgf', 5),
    'kiln_green_malt': grain_floor(GREEN_MALT, 'kgm', 6),
    'kiln_dark_malt': grain_floor(DARK_MALT, 'kdm', 7),
    'brew_kettle_copper': kettle_copper(),
    'brewhouse_door': fire_door(False),
    'brewhouse_door_lit': strip(fire_door(True), fire_door(True, flicker=True)),
    'kiln_grate': kiln_grate(),
    'fermenting_vat_side': vat_side(),
    'vat_wort': liquid(WORT, 'wort'),
    'vat_foam': liquid(FOAM, 'foam', True),
    'vat_beer': liquid(HELL, 'beer'),
    'cask_tap': tex_noise(BRASS, 'tap', base=2),
}

BLOCK_ANIMATIONS = {
    'brewhouse_door_lit': DOOR_FLICKER,
}


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
    with open(path, 'wb') as f:
        f.write(png_bytes(img))


def main():
    preview = sys.argv[2] if len(sys.argv) == 3 and sys.argv[1] == '--preview' else None
    for folder, sprites in (('item', ITEM_SPRITES), ('block', BLOCK_SPRITES)):
        out = os.path.join(TEXTURES, folder)
        os.makedirs(out, exist_ok=True)
        for name, img in sprites.items():
            write(os.path.join(out, name + '.png'), img)
            if preview:
                os.makedirs(os.path.join(preview, folder), exist_ok=True)
                write(os.path.join(preview, folder, name + '.png'), scaled(img, 8))
    for name, meta in BLOCK_ANIMATIONS.items():
        with open(os.path.join(TEXTURES, 'block', name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write(json.dumps(meta, indent=2) + '\n')


if __name__ == '__main__':
    main()
