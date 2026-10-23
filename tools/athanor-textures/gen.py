"""Generates the stage fire textures of the formed Athanor and the crystal of its rituals.

This script is the source of these files; edit the patterns here, not the PNGs.

    python tools/athanor-textures/gen.py                    writes the textures
    python tools/athanor-textures/gen.py --preview out.png  also writes an 8x preview of every frame

Written, one per stage (0 Nigredo, 1 Albedo, 2 Citrinitas, 3 Rubedo):

- alchemy .../textures/block/athanor_fire_<tier>.png and its .mcmeta: an animated strip of 16 by 16 frames. Rows 0 to 11
  are the flame behind the door window, rows 12 to 15 the glow of the dome vents. The flame flickers from frame to frame.

And once:

- alchemy .../textures/block/ritual_crystal.png: one face of a crystal prism, pale so the renderer tints it in the
  crystal's colour. u runs across the face, v from the tip (row 0) to the root; a bright sheen runs down the middle of the
  face, its edges are darker, and a few glints cross it.
"""
import math
import os
import random
import struct
import sys
import zlib

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "alchemy", "src", "main", "resources", "assets", "servus_alchemy", "textures", "block")

STAGES = [0x3A2A4A, 0xF4F4FF, 0xFFC030, 0xFF2A2A]
FRAMES = 8
FRAME_TIME = 2
FLAME_ROWS = 12


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def scale(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def frame(base, n, rng):
    dark = scale(base, 0.22)
    edge = scale(base, 0.65)
    core = mix(base, (255, 255, 255), 0.4)
    pixels = [[dark] * 16 for _ in range(16)]
    phase = n / FRAMES * 2 * math.pi
    for x in range(16):
        height = 5 + 3.5 * math.sin(x * 0.9 + phase) * math.sin(x * 0.37 - phase * 0.5) + rng.uniform(-1.2, 1.2)
        height = max(2, min(FLAME_ROWS - 1, height + 3))
        for y in range(FLAME_ROWS):
            up = FLAME_ROWS - 1 - y
            if up < height - 2:
                pixels[y][x] = core if up < height * 0.3 else base
            elif up < height:
                pixels[y][x] = edge
    pulse = 0.78 + 0.22 * math.sin(phase)
    for y in range(FLAME_ROWS, 16):
        for x in range(16):
            ripple = 0.9 + 0.1 * math.sin(x * 0.8 + phase * 2)
            pixels[y][x] = scale(mix(base, core, 0.25), pulse * ripple)
    return pixels


def png(rows):
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in rows)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 2, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def crystal():
    rng = random.Random(77)
    glints = {(rng.randrange(3, 13), rng.randrange(1, 15)) for _ in range(5)}
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            across = abs(x - 7.5) / 7.5
            sheen = 0.78 + 0.22 * math.cos(across * math.pi / 2) ** 2
            edge = 0.82 if x in (0, 15) else 1.0
            depth = 1.0 - 0.12 * (y / 15)
            value = sheen * edge * depth
            if (x, y) in glints or (x - 1, y) in glints and x % 2 == 0:
                value = 1.0
            level = round(255 * min(1.0, value))
            row.append((level, level, round(level * 0.97)))
        rows.append(row)
    return rows


def main():
    with open(os.path.join(OUT, "ritual_crystal.png"), "wb") as out:
        out.write(png(crystal()))
    preview = sys.argv[2] if len(sys.argv) > 2 and sys.argv[1] == "--preview" else None
    strips = []
    for tier, color in enumerate(STAGES):
        rng = random.Random(1000 + tier)
        rows = []
        for n in range(FRAMES):
            rows.extend(frame(rgb(color), n, rng))
        strips.append(rows)
        with open(os.path.join(OUT, f"athanor_fire_{tier}.png"), "wb") as out:
            out.write(png(rows))
        with open(os.path.join(OUT, f"athanor_fire_{tier}.png.mcmeta"), "w", newline="\n") as out:
            out.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % FRAME_TIME)
    if preview:
        big = []
        for y in range(16 * FRAMES):
            line = []
            for rows in strips:
                line.extend(px for px in rows[y] for _ in range(8))
            for _ in range(8):
                big.append(line)
        with open(preview, "wb") as out:
            out.write(png(big))


if __name__ == "__main__":
    main()
