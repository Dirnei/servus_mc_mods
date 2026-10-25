"""Draws the Lumber module item texture: a toothed saw disc between two grip claws on a steel arm."""
import math
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles',
                   'textures', 'item', 'lumber.png')

OUTLINE = (40, 40, 44, 255)
STEEL = (170, 174, 182, 255)
STEEL_DARK = (110, 114, 122, 255)
BLADE = (200, 204, 210, 255)
TOOTH = (236, 238, 242, 255)
HUB = (90, 90, 96, 255)
WOOD = (150, 108, 62, 255)
WOOD_DARK = (104, 74, 42, 255)


def main():
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    put = image.putpixel
    cx, cy = 7.5, 6.5
    for y in range(16):
        for x in range(16):
            distance = math.hypot(x - cx, y - cy)
            angle = math.atan2(y - cy, x - cx)
            if distance <= 4.2:
                put((x, y), BLADE if distance > 1.6 else HUB)
            elif distance <= 5.6 and int((angle + math.pi) / (2 * math.pi) * 12) % 2 == 0:
                put((x, y), TOOTH)
    for x in (2, 13):
        for y in range(4, 10):
            put((x, y), OUTLINE)
        for y in range(5, 9):
            put((x + (1 if x == 2 else -1), y), STEEL_DARK)
    for y in range(11, 16):
        for x in range(6, 10):
            put((x, y), STEEL if x in (7, 8) else OUTLINE)
    for x in range(3, 13):
        put((x, 11), OUTLINE)
        put((x, 12), STEEL_DARK if x % 3 else STEEL)
    for y in range(13, 16):
        for x in (1, 2, 13, 14):
            put((x, y), WOOD if (x + y) % 2 else WOOD_DARK)
    image.save(OUT)


if __name__ == '__main__':
    main()
