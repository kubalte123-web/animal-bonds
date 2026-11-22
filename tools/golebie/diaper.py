"""Pigeon diapers: fabric patterns painted only onto the 'diaper' cubes of a bird model (an overlay texture with the
same layout as the bird's texture), plus 16x16 item icons.  One overlay per pattern, so a diaper can be swapped
without touching the bird texture."""
import random
from PIL import Image

from pigeon import faces, shade

PATTERNS = {
    # name: (kind, colour A, colour B, trim)
    'gingham_black': ('gingham', (246, 246, 242), (28, 28, 30), (40, 40, 44)),
    'stripes_grey': ('stripes', (150, 154, 162), (30, 30, 34), (36, 36, 40)),
    'gingham_red': ('gingham', (248, 244, 238), (196, 40, 44), (150, 30, 34)),
    'gingham_blue': ('gingham', (246, 246, 244), (52, 88, 170), (40, 64, 130)),
    'dots_pink': ('dots', (236, 150, 176), (255, 246, 250), (200, 104, 136)),
    'denim': ('plain', (70, 96, 140), (92, 120, 166), (226, 190, 90)),
}
CLIP = (238, 196, 36)


def fabric(kind, a, b, x, y):
    if kind == 'gingham':
        v = (x // 2) % 2 + (y // 2) % 2
        return a if v == 0 else (b if v == 2 else tuple((p + q) // 2 for p, q in zip(a, b)))
    if kind == 'stripes':
        return b if (x + y) % 5 in (0, 1) else a
    if kind == 'dots':
        return b if (x % 3 == 1 and (y + (x // 3)) % 3 == 1) else a
    return b if (x + y) % 7 == 0 else a


def paint_overlay(bones, size, pattern, seed=3):
    kind, a, b, trim = PATTERNS[pattern]
    rnd = random.Random(seed)
    img = Image.new('RGBA', size, (0, 0, 0, 0))
    px = img.load()
    for bone in bones:
        for c in bone.cubes:
            if c.mat != 'diaper':
                continue
            F = faces(c)
            for n, r in F.items():
                for y in range(r[3]):
                    for x in range(r[2]):
                        if c.deco == 'd_clip':
                            col = shade(CLIP, 1.1 if n == 'up' else 0.9)
                        elif c.deco in ('d_strap', 'd_edge'):
                            col = shade(trim, 1.0 + 0.15 * ((x + y) % 2))
                        else:
                            col = fabric(kind, a, b, r[0] + x, r[1] + y)
                            if c.deco == 'd_pouch' and (y == 0 or x == 0 or x == r[2] - 1):
                                col = trim            # stitched hem round the pouch
                        k = {'up': 1.05, 'down': 0.8}.get(n, 0.95) * rnd.uniform(0.97, 1.03)
                        px[r[0] + x, r[1] + y] = shade(col, k) + (255,)
    return img


def icon(pattern):
    """16x16 item icon: a little pair of bird pants with shoulder loops and the yellow snap."""
    kind, a, b, trim = PATTERNS[pattern]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    shape = [
        '................',
        '..TT........TT..',
        '.T..T......T..T.',
        '.T..T......T..T.',
        '..TTFFFFFFFFTT..',
        '...FFFFFFFFFF...',
        '...FFFFFFFFFF...',
        '...FFFFFFFCCF...',
        '....FFFFFFCCF...',
        '....FFFFFFFF....',
        '.....FFFFFF.....',
        '.....FFFFFF.....',
        '......FFFF......',
        '......TTTT......',
        '................',
        '................',
    ]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == 'F':
                col = fabric(kind, a, b, x, y)
                edge = any(shape[y + dy][x + dx] == '.' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16)
                px[x, y] = shade(col, 0.78 if edge else 1.0) + (255,)
            elif ch == 'T':
                px[x, y] = shade(trim, 0.9) + (255,)
            elif ch == 'C':
                px[x, y] = CLIP + (255,)
    # outline
    out = img.copy()
    o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] and px[x + dx, y + dy][:3] != CLIP
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = (30, 26, 26, 255)
    return out
