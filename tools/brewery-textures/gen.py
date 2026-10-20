"""Generates the Servus: Brewery item and block textures.

This script is the source of the files; edit the shapes here, not the PNGs.

    python tools/brewery-textures/gen.py                      writes the textures
    python tools/brewery-textures/gen.py --preview <folder>   also writes 8x previews

Written into brewery/src/main/resources/assets/servus_brewery/textures/item/ and .../block/.
Sprites are drawn from pixel masks on a 16x16 canvas. Block sprites use only fully
opaque or fully clear pixels, so the game draws them in the cutout layer.
"""
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


ITEM_SPRITES = {
    'beer_glass': empty_glass(),
    'barley': barley(),
    'barley_seeds': barley_seeds(),
    'hops': hops(),
    'hop_rhizome': hop_rhizome(),
    'hop_pole': hop_pole_item(),
}

BLOCK_SPRITES = {
    **{'barley_stage%d' % age: barley_stage(age) for age in range(8)},
    'hop_pole': hop_pole_block(),
    'hop_vine_young': hop_vine_young(),
    'hop_vine': hop_vine(),
    'hop_cones': hop_cones(),
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


if __name__ == '__main__':
    main()
