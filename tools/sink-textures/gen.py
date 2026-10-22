"""Generates the Sink GUI texture and the Sink and Chimney block overlays.

This script is the source of the files; edit the patterns here, not the PNGs.

    python tools/sink-textures/gen.py

Writes, under foundation/src/main/resources/assets/servus_foundation/textures:

- block/sink_casing_frame.png: the iron frame of the unformed Sink casing, 16 by
  16, transparent where the casing shows its stone.
- block/chimney_flue.png: the dark open flue of the chimney cap, 16 by 16; the
  cap draws its inner ten by ten pixels (3 to 13), the rim around it is stone.
- gui/sink.png, a 256 by 256 sheet. The positions below must match SinkMenu and
  SinkScreen.

- Panel at 0,0, 176 by 200: soot and brick, the buffer slot wells (4 rows of 7
  at 7,17), a tank well beside each row (30 by 18 at 137,17), the batch bar well
  (162 by 4 at 7,92), five empty chimney pips (5 by 8 at 116 + 7i, 4) and the
  player inventory slots at 7,119 (hotbar at 7,177).
- Side tab at 176,0, 30 by 68: three upgrade slot wells at 7,7 + 18i, open on
  the left where it joins the panel.
- Sprites from y 200: the closed iron gate for a slot (18 by 18 at 0,200) and
  for a tank (30 by 18 at 18,200), the ember glow (16 by 16 at 48,200), a lit
  pip (66,200) and the lit top pip (71,200), two fan frames (14 by 14 at
  80,200 and 94,200) and the batch bar fill (160 by 2 at 0,220).
"""
import math
import os
import struct
import zlib

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEXTURES = os.path.join(ROOT, "foundation", "src", "main", "resources", "assets", "servus_foundation", "textures")
OUT = os.path.join(TEXTURES, "gui", "sink.png")

SIZE = 256
PANEL_W, PANEL_H = 176, 200
TAB_X, TAB_W, TAB_H = 176, 30, 68


def hexc(h, alpha=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha)


CLEAR = (0, 0, 0, 0)
OUTLINE = hexc("#0d0806")
PANEL = hexc("#5a4038")
PANEL_LIGHT = hexc("#8a6a5c")
PANEL_DARK = hexc("#241713")
MORTAR = hexc("#1b100c")
WELL = hexc("#2a1d19")
TANK_WELL = hexc("#1b1210")
WELL_DARK = hexc("#140c09")
WELL_LIGHT = hexc("#6e5247")
INV = hexc("#8b8b8b")
INV_DARK = hexc("#373737")
INV_LIGHT = hexc("#ffffff")
GATE_BACK = hexc("#050505")
GATE_BARS = [hexc("#070708"), hexc("#2e3236"), hexc("#1a1c1f"), hexc("#070708")]
GATE_RAIL = [hexc("#3a3e43"), hexc("#24272b"), hexc("#24272b"), hexc("#121315")]
GATE_RIVET = hexc("#8a9096")
GATE_EDGE_DARK = hexc("#000000")
GATE_EDGE_LIGHT = hexc("#3a3e43")
EMBER = (255, 140, 40)
BAR_FROM = hexc("#8a3a1a")
BAR_TO = hexc("#ff9a3a")
PIP_BORDER = hexc("#140c09")
PIP_ON = (hexc("#b0614a"), hexc("#7d3a27"), hexc("#3b1d14"))
PIP_CAP = (hexc("#ffcf6a"), hexc("#e2701e"), hexc("#7a3410"))
FAN_BACK = hexc("#1b1210")
FAN_RIM = hexc("#6e5247")
FAN_BLADE = hexc("#9aa1a6")
FAN_BLADE_EDGE = hexc("#6f777d")
FAN_HUB = hexc("#5d646a")


def blank():
    return [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def rect(img, x, y, w, h, colour):
    for py in range(y, y + h):
        for px in range(x, x + w):
            img[py][px] = colour


def well(img, x, y, w, h, inside, dark=WELL_DARK, light=WELL_LIGHT):
    """A sunken box: dark on the top and left edge, light on the bottom and right."""
    rect(img, x, y, w, h, inside)
    rect(img, x, y, w - 1, 1, dark)
    rect(img, x, y, 1, h - 1, dark)
    rect(img, x + 1, y + h - 1, w - 1, 1, light)
    rect(img, x + w - 1, y + 1, 1, h - 1, light)


def soot_and_brick(img, x0, y0, w, h, open_left=False):
    """The panel body: brick courses 6 high, bricks 12 wide, darkening towards the bottom."""
    for y in range(h):
        shade = 0.35 * y / max(1, h - 1)
        base = mix(PANEL, (20, 12, 8), shade)
        for x in range(w):
            course = y // 6
            joint = (x + (6 if course % 2 else 0)) % 12 == 0
            colour = mix(base, MORTAR, 0.22) if y % 6 == 0 or joint else base
            img[y0 + y][x0 + x] = colour
    # Bevel and outline, with cut corners like vanilla panels.
    for x in range(w):
        img[y0][x0 + x] = OUTLINE
        img[y0 + h - 1][x0 + x] = OUTLINE
        img[y0 + 1][x0 + x] = PANEL_LIGHT
        img[y0 + h - 2][x0 + x] = PANEL_DARK
    for y in range(h):
        img[y0 + y][x0 + w - 1] = OUTLINE
        img[y0 + y][x0 + w - 2] = PANEL_DARK
        if not open_left:
            img[y0 + y][x0] = OUTLINE
            img[y0 + y][x0 + 1] = PANEL_LIGHT
    img[y0 + 1][x0 + w - 2] = PANEL_DARK
    for cx, cy in ((x0 + w - 1, y0), (x0 + w - 1, y0 + h - 1)) + (() if open_left else ((x0, y0), (x0, y0 + h - 1))):
        img[cy][cx] = CLEAR


def panel(img):
    soot_and_brick(img, 0, 0, PANEL_W, PANEL_H)
    for row in range(4):
        for column in range(7):
            well(img, 7 + column * 18, 17 + row * 18, 18, 18, WELL)
        well(img, 137, 17 + row * 18, 30, 18, TANK_WELL)
    well(img, 7, 92, 162, 4, TANK_WELL)
    for i in range(5):
        pip(img, 116 + i * 7, 4, None)
    for row in range(3):
        for column in range(9):
            well(img, 7 + column * 18, 119 + row * 18, 18, 18, INV, INV_DARK, INV_LIGHT)
    for column in range(9):
        well(img, 7 + column * 18, 177, 18, 18, INV, INV_DARK, INV_LIGHT)


def tab(img):
    soot_and_brick(img, TAB_X, 0, TAB_W, TAB_H, open_left=True)
    for i in range(3):
        well(img, TAB_X + 7, 7 + i * 18, 18, 18, WELL)


def gate(img, x0, y0, w, h):
    """A closed black iron gate: vertical bars, a riveted cross rail, dark gaps behind."""
    rect(img, x0, y0, w, h, GATE_BACK)
    for y in range(h):
        for x in range(w):
            img[y0 + y][x0 + x] = GATE_BARS[x % 4]
    rail_top = h // 2 - 2
    for i, colour in enumerate(GATE_RAIL):
        rect(img, x0, y0 + rail_top + i, w, 1, colour)
    for x in range(1, w - 1, 4):
        img[y0 + rail_top + 1][x0 + x] = GATE_RIVET
    rect(img, x0, y0, w - 1, 1, GATE_EDGE_DARK)
    rect(img, x0, y0, 1, h - 1, GATE_EDGE_DARK)
    rect(img, x0 + 1, y0 + h - 1, w - 1, 1, GATE_EDGE_LIGHT)
    rect(img, x0 + w - 1, y0 + 1, 1, h - 1, GATE_EDGE_LIGHT)


def ember(img, x0, y0):
    """An inset glow that is strongest at the edge of the slot and fades in over 5 pixels."""
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            strength = max(0.0, 1.0 - edge / 5.0) ** 1.5
            if strength > 0:
                img[y0 + y][x0 + x] = EMBER + (round(217 * strength),)


def pip(img, x0, y0, colours):
    """A chimney pip of 5 by 8: empty when colours is None, else a top to bottom gradient."""
    border = PIP_BORDER if colours is None else colours[2]
    rect(img, x0, y0, 5, 8, border)
    for y in range(6):
        colour = WELL if colours is None else mix(colours[0], colours[1], y / 5)
        rect(img, x0 + 1, y0 + 1 + y, 3, 1, colour)


def fan(img, x0, y0, turn):
    """A five blade fan in a round housing, turned by {turn} degrees."""
    for y in range(14):
        for x in range(14):
            dx = (x + 0.5 - 7) / 7 * 10
            dy = (y + 0.5 - 7) / 7 * 10
            r = math.hypot(dx, dy)
            if r > 9.9:
                continue
            colour = FAN_RIM if r > 8.6 else FAN_BACK
            if r < 8.6:
                angle = (math.degrees(math.atan2(dy, dx)) - turn - r * 4) % 72
                if r < 2.2:
                    colour = FAN_HUB
                elif angle < 26:
                    colour = FAN_BLADE_EDGE if angle < 5 or r > 7.6 else FAN_BLADE
            img[y0 + y][x0 + x] = colour


def bar(img, x0, y0):
    for x in range(160):
        colour = mix(BAR_FROM, BAR_TO, x / 159)
        img[y0][x0 + x] = mix(colour, (255, 230, 170), 0.25)
        img[y0 + 1][x0 + x] = colour


CASING_FRAME = [
    "abbbbbbbbbbbbbbb",
    "acccbcccccccccbd",
    "acefb......cefbd",
    "acfdb......cfdbd",
    "abbbb......bbbbd",
    "ac............bd",
    "ac............bd",
    "ac............bd",
    "ac............bd",
    "ac............bd",
    "ac............bd",
    "acccb......cccbd",
    "acefb......cefbd",
    "acfdb......cfdbd",
    "abbbbbbbbbbbbbbd",
    "addddddddddddddd",
]
CASING_FRAME_COLOURS = {"a": "#b2b2b4", "b": "#626268", "c": "#d8d8d8", "d": "#404046", "e": "#e8e8e4", "f": "#8c8c90"}

FLUE = [
    "aabacdecaf",
    "bgghijkjlm",
    "cjnopqrokf",
    "bhstuvvowx",
    "bgryzAunhx",
    "egBtAACDEe",
    "akFGvHvrkx",
    "eErrDnBFjI",
    "eJggjjlhka",
    "acIImdfdII",
]
FLUE_COLOURS = {"a": "#33302f", "b": "#363332", "c": "#312e2d", "d": "#2e2b2a", "e": "#322f2e", "f": "#302d2c", "g": "#282524",
                "h": "#2d2a29", "i": "#252221", "j": "#262322", "k": "#292625", "l": "#272423", "m": "#343130", "n": "#242120",
                "o": "#1e1b1a", "p": "#221f1e", "q": "#211e1d", "r": "#23201f", "s": "#1c1918", "t": "#161312", "u": "#141110",
                "v": "#13100f", "w": "#2a2726", "x": "#2f2c2b", "y": "#171413", "z": "#0a0706", "A": "#110e0d", "B": "#1f1c1b",
                "C": "#191615", "D": "#1d1a19", "E": "#2c2928", "F": "#201d1c", "G": "#151211", "H": "#1a1716", "I": "#353231",
                "J": "#2b2827"}


def pattern(img, x0, y0, rows, colours):
    """Paints a pattern of one character per pixel; '.' stays transparent."""
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                img[y0 + y][x0 + x] = hexc(colours[key])


def casing_frame():
    img = [[CLEAR for _ in range(16)] for _ in range(16)]
    pattern(img, 0, 0, CASING_FRAME, CASING_FRAME_COLOURS)
    return img


def flue():
    """The flue at 3,3 inside a border of its edge colour, which the cap never shows."""
    img = [[hexc(FLUE_COLOURS["a"]) for _ in range(16)] for _ in range(16)]
    pattern(img, 3, 3, FLUE, FLUE_COLOURS)
    return img


def png_bytes(img):
    def chunk(kind, data):
        c = kind + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in img)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", len(img[0]), len(img), 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main():
    img = blank()
    panel(img)
    tab(img)
    gate(img, 0, 200, 18, 18)
    gate(img, 18, 200, 30, 18)
    ember(img, 48, 200)
    pip(img, 66, 200, PIP_ON)
    pip(img, 71, 200, PIP_CAP)
    fan(img, 80, 200, 0)
    fan(img, 94, 200, 36)
    bar(img, 0, 220)
    write(OUT, img)
    write(os.path.join(TEXTURES, "block", "sink_casing_frame.png"), casing_frame())
    write(os.path.join(TEXTURES, "block", "chimney_flue.png"), flue())


def write(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png_bytes(img))


if __name__ == "__main__":
    main()
