"""Generates the Warehouse Handheld GUI textures.

This script is the source of the files; edit the patterns here, not the PNGs.

    python tools/handheld-textures/gen.py

Writes, under shuttles/src/main/resources/assets/servus_shuttles/textures/gui:

- handheld.png, a 256 by 256 sheet. The positions below must match
  HandheldScreen.
  - The device at 0,0, 216 by 236: a yellow rubber bumper with a dark rim
    around a dark steel body (rounded, from y 8), the red scan window on top
    (80 by 12 at 68,0), the brand line space with a green LED (195,20), the
    screen opening (188 by 160 at 14,30) and three softkey wells (56 by 20
    at 14 + 66i, 196).
  - Softkey sprites from y 236: a plain key (56 by 20 at 0,236) and a lit key
    (56 by 20 at 56,236).
- handheld_scanlines.png, 256 by 256: every third row dark at about 18 %
  alpha, the rest clear; the screen draws the part it covers.
- handheld_vignette.png, 188 by 160, the size of the screen: a dark two
  pixel rim and an inner shadow fading towards the middle.
"""
import math
import os
import struct
import zlib

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
GUI = os.path.join(ROOT, "shuttles", "src", "main", "resources", "assets", "servus_shuttles", "textures", "gui")

SIZE = 256
DEVICE_W, DEVICE_H = 216, 236
BODY_TOP = 8
WINDOW_X, WINDOW_W, WINDOW_H = 68, 80, 12
LED_X, LED_Y = 195, 20
SCREEN_X, SCREEN_Y, SCREEN_W, SCREEN_H = 14, 30, 188, 160
KEY_Y, KEY_W, KEY_H, KEY_STEP = 196, 56, 20, 66
KEY_SPRITE_Y = 236
RIM_WIDTH, BUMPER_WIDTH = 2, 5
RADIUS = 12


def hexc(h, alpha=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha)


CLEAR = (0, 0, 0, 0)
RIM = hexc("#6b4a0c")
BUMPER = hexc("#e0a325")
BUMPER_LIGHT = hexc("#f2c25a")
BUMPER_SHADE = hexc("#b8841a")
BODY_TOP_COLOUR = hexc("#3a4049")
BODY_BOTTOM_COLOUR = hexc("#23272e")
BODY_EDGE = hexc("#15181c")
WINDOW_FRAME = hexc("#1a1c20")
WINDOW_DARK = hexc("#5a1010")
WINDOW_LIGHT = hexc("#c0201f")
WINDOW_SHINE = hexc("#ff6060")
LED_OFF = hexc("#2d6b36")
LED_ON = hexc("#55ff6e")
LED_GLOW = hexc("#55ff6e", 70)
SCREEN_BEZEL = hexc("#050a06")
SCREEN = hexc("#0f1f12")
WELL = hexc("#15181c")
KEY_TOP = hexc("#4a515c")
KEY_BOTTOM = hexc("#2f343c")
KEY_LIT_TOP = hexc("#f2b134")
KEY_LIT_BOTTOM = hexc("#c98a12")
KEY_SHADOW = hexc("#15181c")


def blank(width, height):
    return [[CLEAR for _ in range(width)] for _ in range(height)]


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def inside_rounded(x, y, x0, y0, x1, y1, radius):
    """Whether pixel x, y lies in the rectangle x0..x1, y0..y1 (exclusive) with rounded corners."""
    if x < x0 or x >= x1 or y < y0 or y >= y1:
        return False
    cx = min(max(x + 0.5, x0 + radius), x1 - radius)
    cy = min(max(y + 0.5, y0 + radius), y1 - radius)
    return (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= radius * radius


def device(img):
    x0, y0, x1, y1 = 0, BODY_TOP, DEVICE_W, DEVICE_H
    inset = RIM_WIDTH + BUMPER_WIDTH
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not inside_rounded(x, y, x0, y0, x1, y1, RADIUS):
                continue
            if not inside_rounded(x, y, x0 + RIM_WIDTH, y0 + RIM_WIDTH, x1 - RIM_WIDTH, y1 - RIM_WIDTH, RADIUS - RIM_WIDTH):
                img[y][x] = RIM
            elif not inside_rounded(x, y, x0 + inset, y0 + inset, x1 - inset, y1 - inset, RADIUS - inset):
                light = x < x0 + inset + 2 or y < y0 + inset + 2
                shade = x >= x1 - inset - 2 or y >= y1 - inset - 2
                img[y][x] = BUMPER_LIGHT if light and not shade else BUMPER_SHADE if shade and not light else BUMPER
            elif not inside_rounded(x, y, x0 + inset + 1, y0 + inset + 1, x1 - inset - 1, y1 - inset - 1, RADIUS - inset - 1):
                img[y][x] = BODY_EDGE
            else:
                # The body's light comes from the top left, darkest from 60 % down.
                t = ((x - x0) / (x1 - x0) * 0.35 + (y - y0) / (y1 - y0)) / (1.35 * 0.6)
                img[y][x] = mix(BODY_TOP_COLOUR, BODY_BOTTOM_COLOUR, t)


def window(img):
    x0, x1 = WINDOW_X, WINDOW_X + WINDOW_W
    for y in range(0, WINDOW_H + 2):
        for x in range(x0 - 2, x1 + 2):
            if not inside_rounded(x, y, x0 - 2, 0, x1 + 2, WINDOW_H + 6, 6):
                continue
            if inside_rounded(x, y, x0, 2, x1, WINDOW_H + 6, 4) and y < WINDOW_H + 2:
                colour = mix(WINDOW_DARK, WINDOW_LIGHT, (y - 2) / (WINDOW_H - 1))
                if y == 3 and x0 + 6 <= x < x0 + 24:
                    colour = WINDOW_SHINE
                img[y][x] = colour
            else:
                img[y][x] = WINDOW_FRAME


def led(img):
    for dy in range(-2, 7):
        for dx in range(-2, 7):
            d = math.hypot(dx - 2, dy - 2)
            x, y = LED_X + dx, LED_Y + dy
            if d <= 2.2:
                img[y][x] = LED_ON if d <= 1.2 else mix(LED_ON, LED_OFF, 0.5)
            elif d <= 3.6:
                img[y][x] = mix(img[y][x], (LED_GLOW[0], LED_GLOW[1], LED_GLOW[2], 255), 0.3)


def screen(img):
    for y in range(SCREEN_Y - 1, SCREEN_Y + SCREEN_H + 1):
        for x in range(SCREEN_X - 1, SCREEN_X + SCREEN_W + 1):
            edge = x < SCREEN_X or y < SCREEN_Y or x >= SCREEN_X + SCREEN_W or y >= SCREEN_Y + SCREEN_H
            img[y][x] = WELL if edge else SCREEN


def key_wells(img):
    for i in range(3):
        kx = SCREEN_X + i * KEY_STEP
        for y in range(KEY_Y - 1, KEY_Y + KEY_H + 1):
            for x in range(kx - 1, kx + KEY_W + 1):
                img[y][x] = WELL


def key(img, x0, y0, top, bottom):
    for y in range(KEY_H):
        for x in range(KEY_W):
            if not inside_rounded(x, y, 0, 0, KEY_W, KEY_H, 3):
                continue
            if y >= KEY_H - 3:
                colour = KEY_SHADOW
            else:
                colour = mix(top, bottom, y / (KEY_H - 4))
                if y == 0:
                    colour = mix(colour, (255, 255, 255, 255), 0.18)
            img[y0 + y][x0 + x] = colour


def scanlines():
    img = blank(SIZE, SIZE)
    for y in range(0, SIZE, 3):
        for x in range(SIZE):
            img[y][x] = (0, 0, 0, 46)
    return img


def vignette():
    img = blank(SCREEN_W, SCREEN_H)
    fade = 11.0
    for y in range(SCREEN_H):
        for x in range(SCREEN_W):
            d = min(x, y, SCREEN_W - 1 - x, SCREEN_H - 1 - y)
            if d < 2:
                img[y][x] = SCREEN_BEZEL
                continue
            t = max(0.0, 1.0 - (d - 2) / fade)
            alpha = round(0.8 * 255 * t * t)
            if alpha > 0:
                img[y][x] = (0, 0, 0, alpha)
    return img


def write_png(path, img):
    height = len(img)
    width = len(img[0])
    raw = bytearray()
    for row in img:
        raw.append(0)
        for pixel in row:
            raw.extend(bytes(pixel))

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as file:
        file.write(png)


def main():
    sheet = blank(SIZE, SIZE)
    device(sheet)
    window(sheet)
    led(sheet)
    screen(sheet)
    key_wells(sheet)
    key(sheet, 0, KEY_SPRITE_Y, KEY_TOP, KEY_BOTTOM)
    key(sheet, KEY_W, KEY_SPRITE_Y, KEY_LIT_TOP, KEY_LIT_BOTTOM)
    write_png(os.path.join(GUI, "handheld.png"), sheet)
    write_png(os.path.join(GUI, "handheld_scanlines.png"), scanlines())
    write_png(os.path.join(GUI, "handheld_vignette.png"), vignette())


if __name__ == "__main__":
    main()
