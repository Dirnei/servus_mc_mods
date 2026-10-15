"""Generates the textures Elemental Sight draws over a Gnome Hollow.

This script is the source of these files; edit the patterns here, not the PNGs.

    python tools/hollow-textures/gen.py                    writes the textures
    python tools/hollow-textures/gen.py --preview out.png  also writes an 8x preview of every texture

All textures are 16 x 16 and the same on every face of the Hollow. Written into
alchemy/src/main/resources/assets/servus_alchemy/textures/sight/:

- hollow_rock.png: darkened stone with the dry, dark cracks
- hollow_cracks.png: a mask the game reads, not draws. Alpha 255 marks a crack pixel, red is how far along its
  crack the pixel lies (0 at the crack's start, 255 at its end), blue 255 marks a nodule that breaks out near ripe
- hollow_material.png: grey grain of the flowing material, tinted per vein by the game
- hollow_cave.png: the cave entry on the facing side, alpha 0 outside the mouth
- hollow_crystals.png: grey crystals around the cave mouth, tinted per vein by the game

No two crystals or nodules touch on a side, and none lies on a crack, inside the mouth or on another, so the
small boxes the game raises from them never share a plane.
"""
import os
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'alchemy', 'src', 'main', 'resources', 'assets', 'servus_alchemy', 'textures', 'sight')
SIZE = 16


def hexc(h):
    h = h.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255


def pal(*hs):
    return [hexc(h) for h in hs]


def noise(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def blank(w=SIZE, h=SIZE):
    return [[(0, 0, 0, 0)] * w for _ in range(h)]


def png_bytes(img):
    h = len(img)
    w = len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write(name, img):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), 'wb') as f:
        f.write(png_bytes(img))


# ------------------------------------------------------------------ shapes
# Each crack is a polyline from its start; one starting on an earlier crack is a branch of it, one ending on
# an earlier crack stops there.
CRACKS = [
    [(0, 3), (2, 4), (4, 3), (6, 5), (8, 4), (10, 5)],
    [(15, 1), (13, 3), (13, 6), (11, 8)],
    [(15, 9), (13, 10), (13, 12), (14, 14), (13, 15)],
    [(1, 15), (2, 12), (1, 10), (3, 8)],
    [(6, 0), (6, 2), (4, 3)],
]

# The cave mouth: half width per row from its top row down to the bottom edge, around x 7.5.
MOUTH_TOP = 5
MOUTH_HALF = [1.5, 2.5, 3.5, 3.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5]
MOUTH_CENTRE = 7.5


def line(a, b):
    """The 8-connected pixels from a to b, a included, b excluded."""
    (x0, y0), (x1, y1) = a, b
    steps = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * i / steps), round(y0 + (y1 - y0) * i / steps)) for i in range(steps)]


def crack_positions():
    """Every crack pixel with how far along its crack it lies, 0 to 1; a branch carries on from its parent."""
    positions = {}
    for crack in CRACKS:
        pixels = []
        for a, b in zip(crack, crack[1:]):
            pixels += line(a, b)
        pixels.append(crack[-1])
        if pixels[0] in positions:
            start = positions[pixels[0]]
            fresh = [p for p in pixels[1:] if p not in positions]
            for i, p in enumerate(fresh):
                positions[p] = start + (1 - start) * (i + 1) / len(fresh)
        else:
            fresh = [p for p in pixels if p not in positions]
            for i, p in enumerate(fresh):
                positions[p] = i / max(1, len(fresh) - 1)
    return positions


def in_mouth(x, y):
    row = y - MOUTH_TOP
    return 0 <= row < len(MOUTH_HALF) and abs(x + 0.5 - (MOUTH_CENTRE + 0.5)) <= MOUTH_HALF[row]


def mouth_depth(x, y):
    """Pixels to the nearest pixel outside the mouth, sides and top only: the bottom edge stays open."""
    d = 0
    while all(in_mouth(x + dx, y + dy) or y + dy >= SIZE
              for dx in range(-d - 1, d + 2) for dy in range(-d - 1, d + 2)):
        d += 1
    return d


def side_neighbours(p):
    x, y = p
    return [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]


def near_mouth(p):
    return any(in_mouth(*q) for q in side_neighbours(p) + [p])


def crystal_pixels(cracks):
    """Pixels just outside the mouth's rim, every other one, never on a crack."""
    rim = [(x, y) for y in range(SIZE) for x in range(SIZE)
           if not in_mouth(x, y) and any(in_mouth(*q) for q in side_neighbours((x, y)))]
    rim.sort(key=lambda p: (p[1], p[0]))
    chosen = []
    for p in rim:
        if p not in cracks and not any(q in chosen for q in side_neighbours(p)):
            chosen.append(p)
    return chosen


def nodule_pixels(cracks, crystals):
    """A few crack pixels late along their crack, away from the mouth, the crystals and each other."""
    late = sorted((p for p, pos in cracks.items() if pos >= 0.45 and not near_mouth(p)), key=lambda p: noise(*p, seed=3))
    chosen = []
    for p in late:
        blocked = chosen + crystals
        if any(q in blocked for q in side_neighbours(p) + [p]):
            continue
        chosen.append(p)
        if len(chosen) == 5:
            break
    return chosen


# ------------------------------------------------------------------ textures
ROCK = pal('#343336', '#3c3b3f', '#454448', '#4e4d51', '#58575b')
CRACK = hexc('#120f10')
CRACK_EDGE = hexc('#68645f')


def rock(cracks):
    img = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 1) * 0.7 + noise(x // 2, y // 2, 2) * 0.3
            img[y][x] = ROCK[min(len(ROCK) - 1, int(n * len(ROCK)))]
    for (x, y) in cracks:
        img[y][x] = CRACK
    for (x, y) in cracks:
        below = (x, y + 1)
        if y + 1 < SIZE and below not in cracks and noise(x, y, 4) < 0.6:
            img[y + 1][x] = CRACK_EDGE
    return img


def mask(cracks, nodules):
    img = blank()
    for (x, y), pos in cracks.items():
        img[y][x] = (round(pos * 255), 0, 255 if (x, y) in nodules else 0, 255)
    return img


def material():
    img = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            v = 190 + int(noise(x, y, 5) * 45)
            if noise(x, y, 6) > 0.9:
                v = 255
            img[y][x] = (v, v, v, 255)
    return img


CAVE_RIM = hexc('#2b2623')
CAVE_RUBBLE = hexc('#5c5753')
CAVE_DEEP = pal('#2b2623', '#1a1614', '#0f0c0b', '#090706')
CAVE_GLOW = hexc('#3a2a1c')


def cave():
    img = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if not in_mouth(x, y):
                continue
            d = mouth_depth(x, y)
            colour = CAVE_DEEP[min(d, len(CAVE_DEEP) - 1)]
            if d == 0 and noise(x, y, 8) < 0.35:
                colour = CAVE_RUBBLE
            if y >= SIZE - 2 and abs(x - MOUTH_CENTRE) <= 1.5:
                colour = CAVE_GLOW
            img[y][x] = colour
    return img


def crystals(pixels):
    img = blank()
    for (x, y) in pixels:
        v = 200 + int(noise(x, y, 9) * 55)
        img[y][x] = (v, v, v, 255)
    return img


def check(cracks, nodules, crystal_list):
    raised = nodules + crystal_list
    for p in raised:
        assert not any(q in raised for q in side_neighbours(p)), f'{p} touches another raised pixel'
        assert not in_mouth(*p), f'{p} lies inside the mouth'
    for p in crystal_list:
        assert p not in cracks, f'crystal {p} lies on a crack'
    assert len(cracks) <= 64, f'{len(cracks)} crack pixels'


def preview(path, images):
    scale = 8
    gap = 4
    w = len(images) * (SIZE * scale + gap)
    out = [[(30, 30, 30, 255)] * w for _ in range(SIZE * scale)]
    for i, img in enumerate(images):
        for y in range(SIZE * scale):
            for x in range(SIZE * scale):
                r, g, b, a = img[y // scale][x // scale]
                back = 90 if ((x // 8 + y // 8) % 2) else 60
                mix = a / 255
                out[y][i * (SIZE * scale + gap) + x] = (round(r * mix + back * (1 - mix)), round(g * mix + back * (1 - mix)),
                                                        round(b * mix + back * (1 - mix)), 255)
    with open(path, 'wb') as f:
        f.write(png_bytes(out))


def main():
    cracks = crack_positions()
    crystal_list = crystal_pixels(cracks)
    nodules = nodule_pixels(cracks, crystal_list)
    check(cracks, nodules, crystal_list)
    images = {
        'hollow_rock.png': rock(cracks),
        'hollow_cracks.png': mask(cracks, nodules),
        'hollow_material.png': material(),
        'hollow_cave.png': cave(),
        'hollow_crystals.png': crystals(crystal_list),
    }
    for name, img in images.items():
        write(name, img)
    print(f'{len(cracks)} crack pixels, {len(nodules)} nodules, {len(crystal_list)} crystals')
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], list(images.values()))


if __name__ == '__main__':
    main()
