"""Generates the Fluid Gantry's sign and lock plate textures.

This script is the source of these files; edit the patterns here, not the PNGs.

    python tools/gantry-textures/gen.py

Written into shuttles/src/main/resources/assets/servus_shuttles/textures/block/:

- fluid_gantry_sign_<mode>_<lamp>.png: the beam's sign, mode mark on the left,
  connection lamp and activity lamp on the right, like the stations. The model
  maps rows 0 to 5 onto the sign face (16 by 6 texels on a 9 by 3.2 px plate).
  Lamp is off (offline: both dark), idle, working or blocked.
- fluid_gantry_plate.png: the lock plate, striped for Any; the renderer draws
  the locked fluid over the window (texels 3 to 13 across, 3 to 10 down).
"""
import os
import struct
import zlib

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "shuttles", "src", "main", "resources", "assets", "servus_shuttles", "textures", "block")


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


RIM = hexc("#3a3f45")
EDGE = hexc("#62686f")
FIELD = hexc("#2a2e33")
MARK = hexc("#e0e0e0")
DARK_LAMP = hexc("#1a1c1f")
LAMPS = {"idle": hexc("#43c463"), "working": hexc("#f0a030"), "blocked": hexc("#e04040")}
SHINE = hexc("#f4f4f4")

MARKS = {
    "storage": ["#####",
                "#...#",
                "#...#",
                "#####"],
    "import": ["..#..",
               "#.#.#",
               ".###.",
               "..#.."],
    "export": ["..#..",
               ".###.",
               "#.#.#",
               "..#.."],
}

STRIPES = [hexc("#3f76e4"), hexc("#e8641c"), hexc("#d8c83a"), hexc("#a8aeb4")]


def blank(fill):
    return [[fill for _ in range(16)] for _ in range(16)]


def png_bytes(img):
    def chunk(kind, data):
        c = kind + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in img)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def write(name, img):
    with open(os.path.join(OUT, name + ".png"), "wb") as f:
        f.write(png_bytes(img))


def lamp(img, x, colour):
    for dy in (2, 3):
        for dx in (0, 1):
            img[dy][x + dx] = colour
    if colour != DARK_LAMP:
        img[2][x] = SHINE


def sign(mode, lamp_state):
    img = blank(EDGE)
    for x in range(16):
        img[0][x] = RIM
        img[5][x] = RIM
    for y in range(1, 5):
        img[y][0] = RIM
        img[y][15] = RIM
        for x in range(1, 15):
            img[y][x] = FIELD
    for row, line in enumerate(MARKS[mode]):
        for col, ch in enumerate(line):
            if ch == "#":
                img[1 + row][2 + col] = MARK
    online = lamp_state != "off"
    lamp(img, 9, LAMPS["idle"] if online else DARK_LAMP)
    lamp(img, 12, LAMPS[lamp_state] if online else DARK_LAMP)
    return img


def plate():
    img = blank(EDGE)
    for i in range(16):
        img[0][i] = RIM
        img[15][i] = RIM
        img[i][0] = RIM
        img[i][15] = RIM
    for y in range(2, 14):
        for x in range(2, 14):
            img[y][x] = FIELD
    for y in range(3, 10):
        for x in range(3, 13):
            img[y][x] = STRIPES[((x - 3) * 4) // 10]
    for x in range(5, 11):
        img[11][x] = MARK
        img[12][x] = MARK
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for mode in MARKS:
        for lamp_state in ("off", "idle", "working", "blocked"):
            write(f"fluid_gantry_sign_{mode}_{lamp_state}", sign(mode, lamp_state))
    write("fluid_gantry_plate", plate())


if __name__ == "__main__":
    main()
