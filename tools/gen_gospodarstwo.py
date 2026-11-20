#!/usr/bin/env python3
"""Assets of Swojskie Gospodarstwo: animal models/animations/textures, item and block textures, models, blockstates,
loot tables, recipes, tags, worldgen, spawns and language files."""
import importlib.util, json, math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sprites as SP
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('ge', os.path.join(HERE, 'gen_elementals.py'))
ge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ge)

RES = os.path.join(HERE, '..', 'src', 'gospodarstwo', 'resources')
A = os.path.join(RES, 'assets', 'gospodarstwo')
D = os.path.join(RES, 'data')
NS = 'gospodarstwo'


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)


def wi(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


# =====================================================================================  animals
ANIMALS = {
    'duck': {
        'colors': {'body': rgb(0x8C7B66), 'belly': rgb(0xB8A88E), 'head': rgb(0x2F6B3A), 'neck': rgb(0x2F6B3A), 'beak': rgb(0xE0B020),
                   'leg': rgb(0xF08A24), 'wing': rgb(0x6E5E4C), 'tail': rgb(0x3C3430), 'eye': rgb(0x101010), 'accent': rgb(0x3050C0)},
        'bones': {'body': (None, (0, 4, 0), (0, 0, 0)), 'head': ('body', (0, 7, -3), (0, 0, 0)),
                  'wing_left': ('body', (3, 6, 0), (0, 0, 0)), 'wing_right': ('body', (-3, 6, 0), (0, 0, 0)),
                  'leg_left': ('body', (1.5, 2, 0), (0, 0, 0)), 'leg_right': ('body', (-1.5, 2, 0), (0, 0, 0)), 'tail': ('body', (0, 6, 4), (-15, 0, 0))},
        'cubes': [('body', (-3, 2, -4), (6, 5, 8), 'body'), ('tail', (-2, 5, 4), (4, 2, 2), 'tail'),
                  ('head', (-1.5, 6, -4.5), (3, 3, 3), 'neck'), ('head', (-2, 8, -6), (4, 4, 4), 'head'), ('head', (-1.5, 8, -8.5), (3, 1, 3), 'beak'),
                  ('wing_left', (3, 3, -3), (1, 4, 6), 'wing'), ('wing_right', (-4, 3, -3), (1, 4, 6), 'wing'),
                  ('leg_left', (1, 0, -1), (1, 2, 1), 'leg'), ('leg_left', (0.5, 0, -2), (2, 1, 1), 'leg'),
                  ('leg_right', (-2, 0, -1), (1, 2, 1), 'leg'), ('leg_right', (-2.5, 0, -2), (2, 1, 1), 'leg')],
    },
    'goose': {
        'colors': {'body': rgb(0xF2F0EA), 'belly': rgb(0xFFFFFF), 'head': rgb(0xF6F4EE), 'neck': rgb(0xF6F4EE), 'beak': rgb(0xF08A24),
                   'leg': rgb(0xF08A24), 'wing': rgb(0xDCD8CE), 'tail': rgb(0xE6E2D8), 'eye': rgb(0x202020), 'accent': rgb(0xC8C4BA)},
        'bones': {'body': (None, (0, 5, 0), (0, 0, 0)), 'head': ('body', (0, 8, -4), (0, 0, 0)),
                  'wing_left': ('body', (3.5, 8, 0), (0, 0, 0)), 'wing_right': ('body', (-3.5, 8, 0), (0, 0, 0)),
                  'leg_left': ('body', (1.5, 3, 0), (0, 0, 0)), 'leg_right': ('body', (-1.5, 3, 0), (0, 0, 0)), 'tail': ('body', (0, 7, 5), (-20, 0, 0))},
        'cubes': [('body', (-3.5, 3, -4.5), (7, 6, 10), 'body'), ('tail', (-2, 6, 5), (4, 2, 3), 'tail'),
                  ('head', (-1, 8, -5), (2, 7, 2), 'neck'), ('head', (-1.5, 14, -7), (3, 3, 4), 'head'), ('head', (-1, 14, -9), (2, 1, 2), 'beak'),
                  ('wing_left', (3.5, 4, -3.5), (1, 5, 8), 'wing'), ('wing_right', (-4.5, 4, -3.5), (1, 5, 8), 'wing'),
                  ('leg_left', (1, 0, -0.5), (1, 3, 1), 'leg'), ('leg_left', (0.5, 0, -2), (2, 1, 2), 'leg'),
                  ('leg_right', (-2, 0, -0.5), (1, 3, 1), 'leg'), ('leg_right', (-2.5, 0, -2), (2, 1, 2), 'leg')],
    },
    'turkey': {
        'colors': {'body': rgb(0x4A3A2C), 'belly': rgb(0x3A2C22), 'head': rgb(0xB8C8D8), 'neck': rgb(0xC85A5A), 'beak': rgb(0xC8B080),
                   'leg': rgb(0xC09A8A), 'wing': rgb(0x5A4836), 'tail': rgb(0x5C4632), 'eye': rgb(0x101010), 'accent': rgb(0xE8D8B0),
                   'wattle': rgb(0xC0303A)},
        'bones': {'body': (None, (0, 5, 0), (0, 0, 0)), 'head': ('body', (0, 10, -4), (0, 0, 0)), 'tail': ('body', (0, 9, 5), (-10, 0, 0)),
                  'wing_left': ('body', (4, 9, 0), (0, 0, 0)), 'wing_right': ('body', (-4, 9, 0), (0, 0, 0)),
                  'leg_left': ('body', (1.5, 4, 0), (0, 0, 0)), 'leg_right': ('body', (-1.5, 4, 0), (0, 0, 0))},
        'cubes': [('body', (-4, 4, -4), (8, 7, 9), 'body'), ('tail', (-7, 7, 5), (14, 11, 1), 'fan'),
                  ('head', (-1, 9, -6), (2, 6, 2), 'neck'), ('head', (-1.5, 14, -7), (3, 3, 3), 'head'), ('head', (-0.5, 14, -8), (1, 1, 1), 'beak'),
                  ('head', (-0.5, 11, -7.5), (1, 4, 1), 'wattle'),
                  ('wing_left', (4, 5, -3), (1, 5, 7), 'wing'), ('wing_right', (-5, 5, -3), (1, 5, 7), 'wing'),
                  ('leg_left', (1, 0, -0.5), (1, 4, 1), 'leg'), ('leg_right', (-2, 0, -0.5), (1, 4, 1), 'leg')],
    },
    'snail': {
        'colors': {'body': rgb(0xC8A878), 'belly': rgb(0xB0905E), 'head': rgb(0xC8A878), 'shell': rgb(0x9A6A36), 'band': rgb(0x5E3A1C),
                   'eye': rgb(0x202020), 'stalk': rgb(0xB89868)},
        'bones': {'body': (None, (0, 0, 0), (0, 0, 0)), 'head': ('body', (0, 2, -4), (0, 0, 0)), 'shell': ('body', (0, 2, 1), (0, 0, 0))},
        'cubes': [('body', (-2, 0, -5), (4, 2, 11), 'body'), ('head', (-1.5, 1, -7), (3, 3, 3), 'head'),
                  ('head', (-1.5, 4, -6.5), (1, 3, 1), 'stalk'), ('head', (0.5, 4, -6.5), (1, 3, 1), 'stalk'),
                  ('shell', (-3, 2, -3), (6, 7, 7), 'shell')],
    },
}


def paint_animal(name, a, uvs, th):
    rnd = random.Random(name)
    img = Image.new('RGBA', (64, th), (0, 0, 0, 0))
    px = img.load()
    col = a['colors']

    def put(x, y, c):
        if 0 <= x < 64 and 0 <= y < th:
            px[x, y] = tuple(c[:3]) + (255,)

    for ci, (bone, origin, size, role) in enumerate(a['cubes']):
        u, v = uvs[ci]
        for fname, (fu, fv, fw, fh) in ge.faces(u, v, size).items():
            for yy in range(fh):
                for xx in range(fw):
                    n = rnd.uniform(0.92, 1.06)
                    c = col.get(role, col['body'])
                    if role == 'body':
                        if fname == 'down' or (fname in ('north', 'south', 'east', 'west') and yy >= fh - max(1, fh // 3)):
                            c = col['belly']
                        if name == 'turkey' and fname in ('east', 'west', 'up') and (xx + yy) % 3 == 0:
                            c = mix(c, (90, 120, 80), 0.35)  # bronze-green sheen
                        if name == 'duck' and fname in ('east', 'west') and yy < 2:
                            c = col['wing']
                    elif role == 'wing':
                        c = col['wing'] if (xx + yy) % 4 else shade(col['wing'], 0.8)
                        if name == 'duck' and fname in ('east', 'west') and fh - 2 <= yy < fh - 1 and xx > 1:
                            c = col['accent']  # blue speculum
                        if name == 'turkey' and yy % 2 == 0:
                            c = col['accent'] if xx % 3 == 0 else c
                    elif role == 'fan':
                        # banded tail feathers with pale tips
                        band = (yy * 3) // max(1, fh)
                        c = col['accent'] if yy < 2 else (col['tail'] if band % 2 == 0 else shade(col['tail'], 0.7))
                        if xx % 2 == 0:
                            c = shade(c, 0.9)
                    elif role == 'neck':
                        c = col['neck']
                        if name == 'duck' and yy == fh - 1:
                            c = (240, 240, 235)  # white collar
                    elif role == 'shell':
                        ring = int(math.hypot(xx - fw / 2 + 0.5, yy - fh / 2 + 0.5))
                        c = col['band'] if ring % 2 == 1 else col['shell']
                        if fname == 'down':
                            c = col['belly']
                    elif role == 'stalk':
                        c = col['eye'] if yy == 0 else col['stalk']
                    put(fu + xx, fv + yy, shade(c, n))
        if role == 'head':
            fu, fv, fw, fh = ge.faces(u, v, size)['north']
            # eyes sit on the sides of a bird's head
            for side in ('east', 'west'):
                su, sv, sw, sh = ge.faces(u, v, size)[side]
                put(su + sw // 2, sv + max(0, sh // 3), col['eye'])
            if name == 'snail':
                put(fu + 1, fv + fh - 1, shade(col['body'], 0.7))
    return img


def animal_assets():
    for name, a in ANIMALS.items():
        uvs, th = ge.pack(a['cubes'])
        wj(f'{A}/geckolib/models/entity/{name}.geo.json', ge.geo(name, a, uvs, th))
        wj(f'{A}/geckolib/animations/entity/{name}.animation.json', ge.animations(name, a))
        wi(f'{A}/textures/entity/{name}.png', paint_animal(name, a, uvs, th))


# =====================================================================================  pixel-art helpers
def canvas():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def blob(img, cx, cy, rx, ry, c, outline=0.7, light=1.18, seed=0, speck=None):
    p = img.load()
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1:
                k = rnd.uniform(0.93, 1.05)
                cc = shade(c, k)
                if d > 0.7:
                    cc = shade(c, outline)
                elif dx < -0.2 and dy < -0.2 and d < 0.35:
                    cc = shade(c, light)
                if speck and rnd.random() < speck[1]:
                    cc = speck[0]
                p[x, y] = cc + (255,)


def line(img, pts, c, w=1):
    ImageDraw.Draw(img).line(pts, fill=tuple(c) + (255,), width=w)


def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.load()[x, y] = tuple(c[:3]) + (255,)


def seeds(c, seed):
    img = canvas()
    rnd = random.Random(seed)
    for (x, y) in [(4, 5), (9, 4), (7, 8), (11, 9), (5, 11), (9, 12), (12, 6)]:
        px(img, x, y, shade(c, 1.1))
        px(img, x + 1, y, c)
        px(img, x, y + 1, shade(c, 0.7))
    return img


def egg(c, big=False, spots=None):
    img = canvas()
    blob(img, 7.5, 8.5, 4.5 if big else 3.8, 6 if big else 5.2, c, seed=3, speck=spots)
    return img


def meat(raw):
    img = canvas()
    c = (230, 140, 130) if raw else (176, 108, 54)
    blob(img, 9, 7, 5, 4.5, c, seed=5)
    line(img, [(5, 10), (2, 13)], (240, 236, 220), 2)
    px(img, 1, 14, (250, 248, 240))
    px(img, 2, 14, (250, 248, 240))
    return img


def skein(c, seed):
    img = canvas()
    blob(img, 8, 8, 6, 5.5, c, seed=seed)
    for i in range(-6, 7, 3):
        line(img, [(3, 8 + i // 2), (13, 7 + i // 2)], shade(c, 0.8))
    return img


def cloth(c, stripes=None, seed=0):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 5), (13, 3), (14, 11), (3, 13)], fill=c + (255,), outline=shade(c, 0.65) + (255,))
    d.line([(3, 9), (13, 7)], fill=shade(c, 0.85) + (255,))
    if stripes:
        d.line([(3, 6), (13, 4)], fill=stripes + (255,))
        d.line([(3, 12), (14, 10)], fill=stripes + (255,))
    rnd = random.Random(seed)
    p = img.load()
    for y in range(16):
        for x in range(16):
            if p[x, y][3] and (x + y) % 2 == 0:
                p[x, y] = shade(p[x, y], rnd.uniform(0.92, 1.0)) + (255,)
    return img


def item_textures():
    T = f'{A}/textures/item'
    wi(f'{T}/flax_seeds.png', seeds((150, 110, 60), 1))
    wi(f'{T}/cabbage_seeds.png', seeds((70, 60, 50), 2))
    img = canvas()
    for i in range(5):
        line(img, [(4 + i, 14), (7 + i * 2 // 2, 2)], (200 - i * 6, 180 - i * 5, 120))
    line(img, [(4, 9), (12, 8)], (120, 90, 50), 2)
    wi(f'{T}/flax_fiber.png', img)
    img = canvas()
    blob(img, 8, 8.5, 6, 5.5, (110, 170, 70), seed=4)
    for i in range(3):
        line(img, [(8, 9), (3 + i * 5, 5)], (180, 220, 140))
    wi(f'{T}/cabbage.png', img)
    img = canvas()
    blob(img, 8, 10, 4.5, 4.5, (176, 110, 140), seed=6)
    line(img, [(8, 6), (7, 1)], (90, 160, 60))
    line(img, [(9, 6), (11, 2)], (90, 160, 60))
    wi(f'{T}/onion.png', img)
    img = canvas()
    blob(img, 8, 9, 4.5, 5, (214, 40, 50), seed=7, speck=((250, 220, 120), 0.12))
    line(img, [(5, 4), (11, 4)], (70, 150, 50), 2)
    px(img, 8, 2, (70, 150, 50))
    wi(f'{T}/strawberry.png', img)
    img = canvas()
    line(img, [(2, 13), (13, 10)], (110, 150, 70))
    for (x, y) in [(4, 9), (8, 8), (11, 6)]:
        blob_small = canvas()
        blob(img, x, y, 2.2, 1.6, (70, 150, 50), seed=x)
    wi(f'{T}/strawberry_runner.png', img)
    img = canvas()
    for (x, y) in [(5, 6), (10, 7), (7, 11)]:
        blob(img, x, y, 3, 3, (150, 190, 90), seed=x, light=1.3)
        line(img, [(x, y - 2), (x, y + 2)], (200, 220, 150))
    wi(f'{T}/gooseberries.png', img)
    img = canvas()
    blob(img, 8, 9, 4.8, 5.4, (110, 50, 120), seed=8)
    line(img, [(8, 4), (9, 1)], (90, 70, 40))
    px(img, 10, 2, (80, 150, 60))
    wi(f'{T}/plum.png', img)
    wi(f'{T}/duck_egg.png', egg((200, 225, 215)))
    wi(f'{T}/goose_egg.png', egg((246, 244, 236), big=True))
    wi(f'{T}/green_egg.png', egg((150, 200, 150), spots=((120, 170, 120), 0.08)))
    for n, raw in [('raw_duck', True), ('cooked_duck', False), ('raw_goose', True), ('cooked_goose', False), ('raw_turkey', True), ('cooked_turkey', False)]:
        wi(f'{T}/{n}.png', meat(raw))
    img = canvas()
    for (x, y, r) in [(6, 8, 3.6), (10, 7, 3.2), (8, 10, 3.4)]:
        blob(img, x, y, r, r * 0.9, (248, 248, 244), seed=int(x * y), outline=0.88)
    wi(f'{T}/down.png', img)
    img = canvas()
    line(img, [(3, 14), (12, 2)], (90, 70, 50))
    for i in range(10):
        c = (110, 80, 50) if i % 3 else (230, 220, 190)
        line(img, [(4 + i, 12 - i), (7 + i, 13 - i)], c)
        line(img, [(3 + i, 11 - i), (2 + i, 8 - i)], c)
    wi(f'{T}/turkey_feather.png', img)
    img = canvas()
    blob(img, 8, 8, 6, 6, (154, 106, 54), seed=9)
    for r in (4, 2):
        ImageDraw.Draw(img).ellipse([8 - r, 8 - r, 8 + r, 8 + r], outline=(94, 58, 28, 255))
    wi(f'{T}/snail_shell.png', img)
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([6, 2, 9, 4], fill=(150, 110, 70, 255))
    d.polygon([(5, 5), (10, 5), (12, 9), (12, 14), (3, 14), (3, 9)], fill=(200, 220, 230, 160), outline=(150, 170, 190, 255))
    d.rectangle([4, 9, 11, 13], fill=(170, 210, 120, 220))
    wi(f'{T}/snail_slime.png', img)
    wi(f'{T}/highland_hair.png', skein((192, 106, 52), 1))
    wi(f'{T}/merino_wool.png', skein((244, 236, 214), 2))
    wi(f'{T}/mangalica_wool.png', skein((214, 196, 156), 3))
    wi(f'{T}/angora_wool.png', skein((255, 255, 255), 4))
    wi(f'{T}/canvas.png', cloth((217, 210, 188), seed=1))
    wi(f'{T}/linen.png', cloth((230, 220, 192), stripes=(200, 186, 150), seed=2))
    wi(f'{T}/woolen_cloth.png', cloth((200, 194, 184), stripes=(160, 60, 50), seed=3))
    wi(f'{T}/merino_cloth.png', cloth((239, 227, 196), stripes=(120, 140, 190), seed=4))
    wi(f'{T}/angora_cloth.png', cloth((250, 246, 250), stripes=(230, 180, 200), seed=5))
    wi(f'{T}/felt.png', cloth((150, 125, 95), seed=6))
    for name, a, b in [('duck', (140, 123, 102), (47, 107, 58)), ('goose', (242, 240, 234), (240, 138, 36)),
                       ('turkey', (74, 58, 44), (192, 48, 58)), ('snail', (200, 168, 120), (154, 106, 54))]:
        img = canvas()
        blob(img, 7.5, 8.5, 5.5, 7, a, seed=len(name), speck=(b, 0.22))
        wi(f'{T}/{name}_spawn_egg.png', img)


def block_textures():
    T = f'{A}/textures/block'
    # crops: 4 stages
    def crop(stage, stem, top, seed):
        img = canvas()
        rnd = random.Random(seed * 10 + stage)
        h = [4, 8, 12, 14][stage]
        for x in (2, 5, 8, 11, 13):
            hh = h - rnd.randint(0, 2)
            line(img, [(x, 15), (x + rnd.randint(-1, 1), 15 - hh)], stem)
            if top:
                top(img, x, 15 - hh, stage, rnd)
        return img

    def flax_top(img, x, y, st, rnd):
        if st == 3:
            px(img, x, y, (110, 140, 230))
            px(img, x + 1, y, (140, 170, 240))
            px(img, x, y - 1, (90, 120, 220))

    def onion_top(img, x, y, st, rnd):
        if st >= 2 and x in (5, 11):
            blob(img, x, 14, 2 + st * 0.3, 1.8, (176, 110, 140), seed=x)

    def straw_top(img, x, y, st, rnd):
        px(img, x - 1, y + 1, (70, 150, 50))
        px(img, x + 1, y + 1, (70, 150, 50))
        if st == 2:
            px(img, x, y, (250, 250, 250))
        if st == 3 and x % 2:
            px(img, x, y + 2, (214, 40, 50))
            px(img, x, y + 3, (190, 30, 40))

    for st in range(4):
        wi(f'{T}/flax_stage{st}.png', crop(st, (110, 160, 80), flax_top, 1))
        wi(f'{T}/onions_stage{st}.png', crop(st, (90, 170, 60), onion_top, 2))
        wi(f'{T}/strawberries_stage{st}.png', crop(st, (80, 150, 60), straw_top if st else None, 3))
        img = canvas()
        r = [2.5, 4, 5.5, 6.5][st]
        blob(img, 8, 15 - r * 0.8, r, r * 0.8, (110, 170, 70), seed=st, outline=0.8)
        if st >= 2:
            blob(img, 8, 15 - r * 0.8, r * 0.55, r * 0.5, (170, 210, 120), seed=st + 9, outline=0.9)
        wi(f'{T}/cabbages_stage{st}.png', img)
        # gooseberry bush
        img = canvas()
        size = [4, 6, 7, 7.5][st]
        blob(img, 8, 15 - size * 0.9, size, size * 0.9, (60, 120, 50), seed=20 + st, outline=0.75)
        p = img.load()
        rnd = random.Random(st)
        for y in range(16):
            for x in range(16):
                if p[x, y][3] and rnd.random() < 0.25:
                    p[x, y] = (0, 0, 0, 0)
        if st >= 2:
            for (x, y) in [(5, 9), (10, 8), (8, 12), (11, 12), (4, 13)]:
                px(img, x, y, (150, 200, 90) if st == 2 else (180, 210, 110))
                px(img, x + 1, y, (120, 170, 70))
        wi(f'{T}/gooseberry_bush_stage{st}.png', img)

    def leaves(base, extra=None, seed=0):
        img = canvas()
        p = img.load()
        rnd = random.Random(seed)
        for y in range(16):
            for x in range(16):
                r = rnd.random()
                if r < 0.18:
                    continue
                c = shade(base, rnd.uniform(0.7, 1.15))
                p[x, y] = c + (255,)
        if extra:
            col, n, sz = extra
            for _ in range(n):
                x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
                for dx in range(sz):
                    for dy in range(sz):
                        px(img, x + dx, y + dy, shade(col, 1.15 if dx == 0 and dy == 0 else 1.0))
        return img

    for fruit, col, leaf in [('apple', (200, 40, 40), (80, 140, 50)), ('plum', (100, 40, 110), (70, 125, 55))]:
        wi(f'{T}/{fruit}_leaves.png', leaves(leaf, seed=1))
        wi(f'{T}/{fruit}_leaves_blossom.png', leaves(leaf, ((250, 240, 245) if fruit == 'apple' else (255, 255, 255), 10, 1), seed=1))
        wi(f'{T}/{fruit}_leaves_unripe.png', leaves(leaf, ((150, 190, 80), 6, 2), seed=1))
        wi(f'{T}/{fruit}_leaves_fruit.png', leaves(leaf, (col, 6, 2), seed=1))
        img = canvas()
        line(img, [(8, 15), (8, 6)], (110, 80, 50), 1)
        line(img, [(8, 9), (11, 6)], (110, 80, 50), 1)
        blob(img, 8, 5, 4, 3.5, leaf, seed=2)
        blob(img, 11.5, 5.5, 2.5, 2, leaf, seed=3)
        px(img, 6, 5, col)
        wi(f'{T}/{fruit}_sapling.png', img)
    # misc for renderers
    os.makedirs(f'{A}/textures/misc', exist_ok=True)
    wi(f'{A}/textures/misc/white.png', Image.new('RGBA', (16, 16), (255, 255, 255, 255)))
    fur = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    p = fur.load()
    rnd = random.Random(5)
    for y in range(64):
        for x in range(64):
            v = rnd.random()
            if v < 0.62:
                k = rnd.uniform(0.75, 1.0)
                p[x, y] = (int(255 * k), int(255 * k), int(255 * k), 255)
    fur.save(f'{A}/textures/misc/fur.png')


# =====================================================================================  models & blockstates
def models():
    M = f'{A}/models'
    BS = f'{A}/blockstates'
    IT = f'{A}/items'
    for crop in ('flax', 'cabbages', 'onions', 'strawberries'):
        variants = {}
        for st in range(4):
            wj(f'{M}/block/{crop}_stage{st}.json', {'parent': 'minecraft:block/crop', 'textures': {'crop': f'{NS}:block/{crop}_stage{st}'}})
        for age in range(8):
            variants[f'age={age}'] = {'model': f'{NS}:block/{crop}_stage{[0, 0, 1, 1, 2, 2, 2, 3][age]}'}
        wj(f'{BS}/{crop}.json', {'variants': variants})
    for st in range(4):
        wj(f'{M}/block/gooseberry_bush_stage{st}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'{NS}:block/gooseberry_bush_stage{st}'}})
    wj(f'{BS}/gooseberry_bush.json', {'variants': {f'age={a}': {'model': f'{NS}:block/gooseberry_bush_stage{a}'} for a in range(4)}})
    for fruit in ('apple', 'plum'):
        names = ['', '_blossom', '_unripe', '_fruit']
        for i, suf in enumerate(names):
            t = f'{NS}:block/{fruit}_leaves{suf}'
            wj(f'{M}/block/{fruit}_leaves{suf}.json', {'parent': 'minecraft:block/block', 'textures': {'particle': t, 'all': t}, 'elements': [
                {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#all', 'cullface': f}
                                                                for f in ('down', 'up', 'north', 'south', 'west', 'east')}}]})
        wj(f'{BS}/{fruit}_leaves.json', {'variants': {f'fruit={i}': {'model': f'{NS}:block/{fruit}_leaves{names[i]}'} for i in range(4)}})
        wj(f'{IT}/{fruit}_leaves.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{fruit}_leaves_fruit'}})
        wj(f'{M}/block/{fruit}_sapling.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'{NS}:block/{fruit}_sapling'}})
        wj(f'{BS}/{fruit}_sapling.json', {'variants': {f'stage={s}': {'model': f'{NS}:block/{fruit}_sapling'} for s in range(2)}})
        wj(f'{M}/item/{fruit}_sapling.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{fruit}_sapling'}})
        wj(f'{IT}/{fruit}_sapling.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{fruit}_sapling'}})

    def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), uvs=None):
        f = {}
        for face in faces:
            if face in ('up', 'down'):
                uv = [fr[0], fr[2], to[0], to[2]]
            elif face in ('north', 'south'):
                uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
            else:
                uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
            f[face] = {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv]}
        return {'from': fr, 'to': to, 'faces': f}

    # bonsai pot: a shallow rectangular tray with soil
    wj(f'{M}/block/bonsai_pot.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/terracotta', 'pot': 'minecraft:block/terracotta', 'rim': 'minecraft:block/brown_terracotta',
        'soil': 'minecraft:block/rooted_dirt', 'moss': 'minecraft:block/moss_block'},
        'elements': [el([2, 0, 4], [14, 1, 12], '#rim'),
                     el([1, 1, 3], [15, 4, 4], '#pot'), el([1, 1, 12], [15, 4, 13], '#pot'),
                     el([1, 1, 4], [2, 4, 12], '#pot'), el([14, 1, 4], [15, 4, 12], '#pot'),
                     el([2, 1, 4], [14, 3.5, 12], '#soil', faces=('up',)),
                     el([3, 3.5, 5], [7, 3.7, 8], '#moss', faces=('up',)), el([9, 3.5, 8], [13, 3.7, 11], '#moss', faces=('up',)),
                     el([0, 0, 2], [2, 1, 4], '#rim'), el([14, 0, 2], [16, 1, 4], '#rim'), el([0, 0, 12], [2, 1, 14], '#rim'),
                     el([14, 0, 12], [16, 1, 14], '#rim')]})
    wj(f'{BS}/bonsai_pot.json', {'variants': {'': {'model': f'{NS}:block/bonsai_pot'}}})
    wj(f'{IT}/bonsai_pot.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/bonsai_pot'}})

    # weaving loom frame (front = north, where the weaver sits)
    W, L = '#wood', '#log'
    loom = [
        # side frames: posts and rails
        el([0, 0, 1], [2, 15, 3], L), el([14, 0, 1], [16, 15, 3], L), el([0, 0, 13], [2, 15, 15], L), el([14, 0, 13], [16, 15, 15], L),
        el([0, 1, 3], [2, 2.5, 13], W), el([14, 1, 3], [16, 2.5, 13], W), el([0, 13.5, 3], [2, 15, 13], W), el([14, 13.5, 3], [16, 15, 13], W),
        # castle on top holding the heddles
        el([0, 15, 6.5], [16, 16, 9.5], W),
        # breast beam (front), warp beam (back), cloth beam below the front
        el([2, 9, 1], [14, 11, 3], L), el([2, 9, 13], [14, 11, 15], L), el([2, 4, 2], [14, 5.5, 3.5], L),
        # treadles
        el([5, 0.5, 2], [6.5, 1.5, 10], W), el([9.5, 0.5, 2], [11, 1.5, 10], W),
        # bench in front? no - the reed (beater) hanging from the castle
        el([2.5, 11.2, 4.4], [13.5, 13, 4.9], W), el([2, 11.2, 4.2], [2.5, 15, 5.1], W), el([13.5, 11.2, 4.2], [14, 15, 5.1], W),
    ]
    wj(f'{M}/block/weaving_loom.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/spruce_planks', 'wood': 'minecraft:block/spruce_planks', 'log': 'minecraft:block/stripped_spruce_log'},
        'elements': loom, 'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}})
    wj(f'{BS}/weaving_loom.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/weaving_loom', 'y': y} if y else {'model': f'{NS}:block/weaving_loom'})
                                                for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    wj(f'{IT}/weaving_loom.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/weaving_loom'}})

    simple = ['flax_seeds', 'flax_fiber', 'cabbage_seeds', 'cabbage', 'onion', 'strawberry_runner', 'strawberry', 'gooseberries', 'plum',
              'duck_egg', 'goose_egg', 'green_egg', 'raw_duck', 'cooked_duck', 'raw_goose', 'cooked_goose', 'raw_turkey', 'cooked_turkey',
              'down', 'turkey_feather', 'snail_shell', 'snail_slime', 'duck_spawn_egg', 'goose_spawn_egg', 'turkey_spawn_egg', 'snail_spawn_egg']
    for n in simple:
        wj(f'{M}/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})
        wj(f'{IT}/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{n}'}})


# =====================================================================================  data
def item(i, n=None):
    e = {'type': 'minecraft:item', 'name': i}
    if n:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': n, 'add': False}]
    return e


def age_cond(block, age, prop='age'):
    return {'condition': 'minecraft:block_state_property', 'block': block, 'properties': {prop: str(age)}}


def loot(path, pools, extra=None):
    o = {'type': 'minecraft:block', 'pools': pools, 'random_sequence': f'{NS}:blocks/{path}'}
    wj(f'{D}/{NS}/loot_table/blocks/{path}.json', o)


def data():
    G = lambda n: f'{NS}:{n}'
    # crops: seeds always, produce when ripe (+ bonus seeds)
    for crop, produce, seed in [('flax', 'flax_fiber', 'flax_seeds'), ('cabbages', 'cabbage', 'cabbage_seeds')]:
        loot(crop, [
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                dict(item(G(produce), {'type': 'minecraft:uniform', 'min': 1, 'max': 3 if crop == 'flax' else 1}), conditions=[age_cond(G(crop), 7)]),
                item(G(seed))]}]},
            {'rolls': 1, 'bonus_rolls': 0, 'conditions': [age_cond(G(crop), 7)], 'entries': [dict(item(G(seed)), functions=[
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:binomial_with_bonus_count',
                 'parameters': {'extra': 3, 'probability': 0.5714286}}])]}])
    for crop, produce, seed in [('onions', 'onion', 'onion'), ('strawberries', 'strawberry', 'strawberry_runner')]:
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [item(G(seed))]},
                 {'rolls': 1, 'bonus_rolls': 0, 'conditions': [age_cond(G(crop), 7)], 'entries': [dict(item(G(produce)), functions=[
                     {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:binomial_with_bonus_count',
                      'parameters': {'extra': 3, 'probability': 0.5714286}}])]}]
        loot(crop, pools)
    loot('gooseberry_bush', [{'rolls': 1, 'bonus_rolls': 0, 'entries': [item(G('gooseberries'))]}])
    for fruit in ('apple', 'plum'):
        loot(f'{fruit}_sapling', [{'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                   'entries': [item(G(f'{fruit}_sapling'))]}])
        shears = {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}}
        loot(f'{fruit}_leaves', [
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                dict(item(G(f'{fruit}_leaves')), conditions=[shears]),
                dict(item(G(f'{fruit}_sapling')), conditions=[{'condition': 'minecraft:survives_explosion'},
                                                              {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune',
                                                               'chances': [0.05, 0.0625, 0.083333336, 0.1]}])]}]},
            {'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:inverted', 'term': shears},
                                                          {'condition': 'minecraft:random_chance', 'chance': 0.04}],
             'entries': [item('minecraft:stick')]}])
    loot('bonsai_pot', [{'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                         'entries': [dict(item(G('bonsai_pot')), functions=[
                             {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [G('bonsai')]}])]}])
    loot('weaving_loom', [{'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                           'entries': [item(G('weaving_loom'))]}])

    # entity loot
    def eloot(name, pools):
        wj(f'{D}/{NS}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'{NS}:entities/{name}'})

    def meat_pool(raw, cooked, lo, hi):
        return {'rolls': 1, 'entries': [dict(item(G(raw), {'type': 'minecraft:uniform', 'min': lo, 'max': hi}), functions=[
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}},
            {'function': 'minecraft:furnace_smelt', 'conditions': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                                                                    'predicate': {'flags': {'is_on_fire': True}}}]}])]}

    def simple_pool(i, lo, hi):
        return {'rolls': 1, 'entries': [item(i, {'type': 'minecraft:uniform', 'min': lo, 'max': hi})]}

    eloot('duck', [simple_pool('minecraft:feather', 0, 2), meat_pool('raw_duck', 'cooked_duck', 1, 1)])
    eloot('goose', [simple_pool('minecraft:feather', 0, 2), simple_pool(G('down'), 0, 1), meat_pool('raw_goose', 'cooked_goose', 1, 2)])
    eloot('turkey', [simple_pool(G('turkey_feather'), 0, 2), meat_pool('raw_turkey', 'cooked_turkey', 1, 3)])
    eloot('snail', [{'rolls': 1, 'entries': [dict(item(G('snail_shell')), conditions=[{'condition': 'minecraft:random_chance', 'chance': 0.6}])]}])

    # recipes
    R = f'{D}/{NS}/recipe'
    for raw, cooked in [('raw_duck', 'cooked_duck'), ('raw_goose', 'cooked_goose'), ('raw_turkey', 'cooked_turkey')]:
        for kind, t, xp in [('smelting', 200, 0.35), ('smoking', 100, 0.35), ('campfire_cooking', 600, 0.35)]:
            wj(f'{R}/{cooked}_from_{kind}.json', {'type': f'minecraft:{kind}', 'category': 'food', 'ingredient': G(raw), 'result': {'id': G(cooked)},
                                                  'experience': xp, 'cookingtime': t})
    wj(f'{R}/weaving_loom.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['LSL', 'P P', 'L L'],
                                  'key': {'L': '#minecraft:logs', 'S': 'minecraft:string', 'P': '#minecraft:planks'},
                                  'result': {'id': G('weaving_loom'), 'count': 1}})
    wj(f'{R}/bonsai_pot.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['B B', 'TDT'],
                                'key': {'B': 'minecraft:brick', 'T': 'minecraft:terracotta', 'D': 'minecraft:dirt'},
                                'result': {'id': G('bonsai_pot'), 'count': 1}})
    wj(f'{R}/string_from_flax.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                      'ingredients': [G('flax_fiber'), G('flax_fiber')], 'result': {'id': 'minecraft:string', 'count': 1}})
    wj(f'{R}/white_wool_from_merino.json', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                                            'ingredients': [G('merino_wool')] * 2, 'result': {'id': 'minecraft:white_wool', 'count': 1}})
    wj(f'{R}/plum_cake_slice.json', {'type': 'minecraft:crafting_shapeless', 'category': 'food',
                                     'ingredients': [G('plum'), 'minecraft:sugar', 'minecraft:bread'], 'result': {'id': 'minecraft:pumpkin_pie', 'count': 1}})
    os.remove(f'{R}/plum_cake_slice.json')

    # tags
    def tag(kind, ns, name, values):
        wj(f'{D}/{ns}/tags/{kind}/{name}.json', {'replace': False, 'values': values})

    crops = [G('flax'), G('cabbages'), G('onions'), G('strawberries')]
    tag('block', 'minecraft', 'crops', crops)
    tag('block', 'minecraft', 'maintains_farmland', crops)
    tag('block', 'minecraft', 'bee_attractive', [G('strawberries'), G('apple_leaves'), G('plum_leaves')])
    tag('block', 'minecraft', 'leaves', [G('apple_leaves'), G('plum_leaves')])
    tag('block', 'minecraft', 'saplings', [G('apple_sapling'), G('plum_sapling')])
    tag('block', 'minecraft', 'mineable/hoe', [G('apple_leaves'), G('plum_leaves')])
    tag('block', 'minecraft', 'mineable/axe', [G('weaving_loom')])
    tag('block', 'minecraft', 'mineable/pickaxe', [G('bonsai_pot')])
    tag('item', 'minecraft', 'saplings', [G('apple_sapling'), G('plum_sapling')])
    tag('item', 'minecraft', 'leaves', [G('apple_leaves'), G('plum_leaves')])
    tag('item', 'minecraft', 'chicken_food', [G('flax_seeds'), G('cabbage_seeds')])
    tag('item', 'minecraft', 'villager_plantable_seeds', [G('flax_seeds'), G('cabbage_seeds'), G('onion')])
    tag('item', 'c', 'seeds', [G('flax_seeds'), G('cabbage_seeds')])
    tag('item', 'c', 'crops', [G('flax_fiber'), G('cabbage'), G('onion'), G('strawberry')])
    tag('item', 'c', 'foods/fruit', [G('strawberry'), G('gooseberries'), G('plum')])
    tag('item', 'c', 'foods/raw_meat', [G('raw_duck'), G('raw_goose'), G('raw_turkey')])
    tag('item', 'c', 'foods/cooked_meat', [G('cooked_duck'), G('cooked_goose'), G('cooked_turkey')])
    tag('item', 'c', 'eggs', [G('duck_egg'), G('goose_egg'), G('green_egg')])
    tag('item', 'minecraft', 'eggs', [G('duck_egg'), G('goose_egg'), G('green_egg')])

    # worldgen: fruit trees
    WG = f'{D}/{NS}/worldgen'
    for fruit, log in [('apple', 'minecraft:oak_log'), ('plum', 'minecraft:dark_oak_log')]:
        wj(f'{WG}/configured_feature/{fruit}_tree.json', {'type': 'minecraft:tree', 'config': TREE_TEMPLATE(log, G(f'{fruit}_leaves'))})
        wj(f'{WG}/placed_feature/{fruit}_tree_wild.json', {'feature': G(f'{fruit}_tree'), 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': 48},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
            {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
            {'type': 'minecraft:biome'},
            {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': {'Name': G(f'{fruit}_sapling'), 'Properties': {'stage': '0'}}}}]})
    BM = f'{D}/{NS}/neoforge/biome_modifier'
    wj(f'{BM}/apple_trees.json', {'type': 'neoforge:add_features', 'biomes': ['minecraft:plains', 'minecraft:meadow', 'minecraft:flower_forest', 'minecraft:forest'],
                                  'features': G('apple_tree_wild'), 'step': 'vegetal_decoration'})
    wj(f'{BM}/plum_trees.json', {'type': 'neoforge:add_features', 'biomes': ['minecraft:plains', 'minecraft:meadow', 'minecraft:birch_forest', 'minecraft:forest'],
                                 'features': G('plum_tree_wild'), 'step': 'vegetal_decoration'})

    def spawns(name, biomes, w, lo, hi):
        wj(f'{BM}/{name}_spawns.json', {'type': 'neoforge:add_spawns', 'biomes': biomes,
                                         'spawners': {'type': G(name), 'weight': w, 'minCount': lo, 'maxCount': hi}})

    spawns('duck', ['minecraft:river', 'minecraft:swamp', 'minecraft:mangrove_swamp', 'minecraft:plains'], 8, 2, 4)
    spawns('goose', ['minecraft:plains', 'minecraft:meadow', 'minecraft:river', 'minecraft:sunflower_plains'], 7, 2, 4)
    spawns('turkey', ['minecraft:forest', 'minecraft:birch_forest', 'minecraft:plains', 'minecraft:old_growth_birch_forest'], 6, 2, 3)
    spawns('snail', ['minecraft:forest', 'minecraft:swamp', 'minecraft:meadow', 'minecraft:flower_forest', 'minecraft:dark_forest'], 6, 1, 3)

    # seeds and saplings from grass / leaves
    LM = f'{D}/{NS}/loot_modifiers'
    wj(f'{D}/{NS}/loot_table/gameplay/grass_seeds.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
        {'type': 'minecraft:item', 'name': G('flax_seeds'), 'weight': 6}, {'type': 'minecraft:item', 'name': G('cabbage_seeds'), 'weight': 4},
        {'type': 'minecraft:item', 'name': G('onion'), 'weight': 2}, {'type': 'minecraft:item', 'name': G('strawberry_runner'), 'weight': 2},
        {'type': 'minecraft:item', 'name': G('gooseberries'), 'weight': 2}],
        'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.06}]}]})
    wj(f'{D}/{NS}/loot_table/gameplay/fruit_saplings.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
        {'type': 'minecraft:item', 'name': G('apple_sapling')}, {'type': 'minecraft:item', 'name': G('plum_sapling')}],
        'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.012}]}]})
    no_shears = {'condition': 'minecraft:inverted', 'term': {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}}}
    for grass in ('short_grass', 'tall_grass', 'fern'):
        wj(f'{LM}/seeds_from_{grass}.json', {'type': 'neoforge:add_table', 'conditions': [
            {'condition': 'neoforge:loot_table_id', 'loot_table_id': f'minecraft:blocks/{grass}'}, no_shears], 'table': G('gameplay/grass_seeds')})
    wj(f'{LM}/fruit_saplings_from_oak.json', {'type': 'neoforge:add_table', 'conditions': [
        {'condition': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:blocks/oak_leaves'}, no_shears], 'table': G('gameplay/fruit_saplings')})


def TREE_TEMPLATE(log, leaves):
    # filled in from the vanilla oak tree format (see .github data dump); kept in one place
    return {
        'below_trunk_provider': {'type': 'minecraft:rule_based_state_provider', 'rules': [{
            'if_true': {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:cannot_replace_below_tree_trunk'}},
            'then': {'type': 'minecraft:simple_state_provider', 'state': {'Name': 'minecraft:dirt'}}}]},
        'decorators': [],
        'foliage_placer': {'type': 'minecraft:blob_foliage_placer', 'height': 3, 'offset': 0, 'radius': 2},
        'foliage_provider': {'type': 'minecraft:simple_state_provider',
                             'state': {'Name': leaves, 'Properties': {'distance': '7', 'fruit': '0', 'persistent': 'false', 'waterlogged': 'false'}}},
        'ignore_vines': True,
        'minimum_size': {'type': 'minecraft:two_layers_feature_size'},
        'trunk_placer': {'type': 'minecraft:straight_trunk_placer', 'base_height': 4, 'height_rand_a': 1, 'height_rand_b': 0},
        'trunk_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': log, 'Properties': {'axis': 'y'}}},
    }


# =====================================================================================  language
def lang():
    pl = {
        'itemGroup.gospodarstwo': 'Swojskie Gospodarstwo',
        'entity.gospodarstwo.duck': 'Kaczka', 'entity.gospodarstwo.goose': 'Gęś', 'entity.gospodarstwo.turkey': 'Indyk',
        'entity.gospodarstwo.snail': 'Ślimak winniczek',
        'block.gospodarstwo.flax': 'Len', 'block.gospodarstwo.cabbages': 'Kapusta', 'block.gospodarstwo.onions': 'Cebula',
        'block.gospodarstwo.strawberries': 'Truskawki', 'block.gospodarstwo.gooseberry_bush': 'Krzew agrestu',
        'block.gospodarstwo.apple_leaves': 'Liście jabłoni', 'block.gospodarstwo.plum_leaves': 'Liście śliwy',
        'block.gospodarstwo.apple_sapling': 'Sadzonka jabłoni', 'block.gospodarstwo.plum_sapling': 'Sadzonka śliwy',
        'block.gospodarstwo.bonsai_pot': 'Doniczka bonsai', 'block.gospodarstwo.weaving_loom': 'Krosno tkackie',
        'item.gospodarstwo.flax_seeds': 'Nasiona lnu', 'item.gospodarstwo.flax_fiber': 'Włókno lniane',
        'item.gospodarstwo.cabbage_seeds': 'Nasiona kapusty', 'item.gospodarstwo.cabbage': 'Kapusta', 'item.gospodarstwo.onion': 'Cebula',
        'item.gospodarstwo.strawberry_runner': 'Sadzonka truskawki', 'item.gospodarstwo.strawberry': 'Truskawka',
        'item.gospodarstwo.gooseberries': 'Agrest', 'item.gospodarstwo.plum': 'Śliwka',
        'item.gospodarstwo.duck_egg': 'Kacze jajo', 'item.gospodarstwo.goose_egg': 'Gęsie jajo', 'item.gospodarstwo.green_egg': 'Zielone jajo',
        'item.gospodarstwo.raw_duck': 'Surowa kaczka', 'item.gospodarstwo.cooked_duck': 'Pieczona kaczka',
        'item.gospodarstwo.raw_goose': 'Surowa gęsina', 'item.gospodarstwo.cooked_goose': 'Pieczona gęś',
        'item.gospodarstwo.raw_turkey': 'Surowy indyk', 'item.gospodarstwo.cooked_turkey': 'Pieczony indyk',
        'item.gospodarstwo.down': 'Puch', 'item.gospodarstwo.turkey_feather': 'Pióro indyka', 'item.gospodarstwo.snail_shell': 'Muszla ślimaka',
        'item.gospodarstwo.snail_slime': 'Śluz ślimaka', 'item.gospodarstwo.highland_hair': 'Sierść krowy górskiej',
        'item.gospodarstwo.merino_wool': 'Wełna merynosowa', 'item.gospodarstwo.mangalica_wool': 'Wełna mangalicy',
        'item.gospodarstwo.angora_wool': 'Wełna angorska',
        'item.gospodarstwo.canvas': 'Płótno', 'item.gospodarstwo.linen': 'Płótno lniane', 'item.gospodarstwo.woolen_cloth': 'Sukno',
        'item.gospodarstwo.merino_cloth': 'Sukno merynosowe', 'item.gospodarstwo.angora_cloth': 'Tkanina angorska', 'item.gospodarstwo.felt': 'Filc',
        'item.gospodarstwo.duck_spawn_egg': 'Jajo spawnu kaczki', 'item.gospodarstwo.goose_spawn_egg': 'Jajo spawnu gęsi',
        'item.gospodarstwo.turkey_spawn_egg': 'Jajo spawnu indyka', 'item.gospodarstwo.snail_spawn_egg': 'Jajo spawnu ślimaka',
        'breed.gospodarstwo.highland': 'Krowa górska', 'breed.gospodarstwo.merino': 'Merynos', 'breed.gospodarstwo.silkie': 'Kura jedwabista',
        'breed.gospodarstwo.greenleg': 'Zielononóżka', 'breed.gospodarstwo.mangalica': 'Mangalica', 'breed.gospodarstwo.angora': 'Królik angorski',
        'breed.gospodarstwo.is': 'Rasa: %s', 'breed.gospodarstwo.label': 'Rasa: %s', 'breed.gospodarstwo.regrowing': 'Sierść jeszcze nie odrosła',
        'bonsai.gospodarstwo.empty': 'Posadź tu sadzonkę dowolnego drzewa',
        'bonsai.gospodarstwo.status': 'Bonsai: %s%%, %s, przycięte %s/%s razy',
        'bonsai.gospodarstwo.needs_trim': 'Drzewko zdziczało (%s%%, %s) – przytnij je nożycami (%s/%s), żeby rosło dalej jako bonsai',
        'bonsai.gospodarstwo.done': 'Gotowe bonsai! (%s%%, %s, przycięte %s/%s razy)',
        'bonsai.gospodarstwo.watered': 'podlane', 'bonsai.gospodarstwo.dry': 'sucha ziemia – podlej',
        'bonsai.gospodarstwo.no_trim': 'Jeszcze nie ma czego przycinać – niech podrośnie',
        'loom.gospodarstwo.empty': 'Krosno jest puste – włóż nici, włókno lniane albo wełnę',
        'loom.gospodarstwo.other_yarn': 'Na krośnie jest już inna przędza – zabierz ją (kucnij + PPM)',
        'gospodarstwo.configuration.title': 'Swojskie Gospodarstwo – ustawienia',
    }
    en = {
        'itemGroup.gospodarstwo': 'Homestead',
        'entity.gospodarstwo.duck': 'Duck', 'entity.gospodarstwo.goose': 'Goose', 'entity.gospodarstwo.turkey': 'Turkey',
        'entity.gospodarstwo.snail': 'Roman Snail',
        'block.gospodarstwo.flax': 'Flax', 'block.gospodarstwo.cabbages': 'Cabbages', 'block.gospodarstwo.onions': 'Onions',
        'block.gospodarstwo.strawberries': 'Strawberries', 'block.gospodarstwo.gooseberry_bush': 'Gooseberry Bush',
        'block.gospodarstwo.apple_leaves': 'Apple Leaves', 'block.gospodarstwo.plum_leaves': 'Plum Leaves',
        'block.gospodarstwo.apple_sapling': 'Apple Sapling', 'block.gospodarstwo.plum_sapling': 'Plum Sapling',
        'block.gospodarstwo.bonsai_pot': 'Bonsai Pot', 'block.gospodarstwo.weaving_loom': 'Weaving Loom',
        'item.gospodarstwo.flax_seeds': 'Flax Seeds', 'item.gospodarstwo.flax_fiber': 'Flax Fibre',
        'item.gospodarstwo.cabbage_seeds': 'Cabbage Seeds', 'item.gospodarstwo.cabbage': 'Cabbage', 'item.gospodarstwo.onion': 'Onion',
        'item.gospodarstwo.strawberry_runner': 'Strawberry Runner', 'item.gospodarstwo.strawberry': 'Strawberry',
        'item.gospodarstwo.gooseberries': 'Gooseberries', 'item.gospodarstwo.plum': 'Plum',
        'item.gospodarstwo.duck_egg': 'Duck Egg', 'item.gospodarstwo.goose_egg': 'Goose Egg', 'item.gospodarstwo.green_egg': 'Green Egg',
        'item.gospodarstwo.raw_duck': 'Raw Duck', 'item.gospodarstwo.cooked_duck': 'Roast Duck', 'item.gospodarstwo.raw_goose': 'Raw Goose',
        'item.gospodarstwo.cooked_goose': 'Roast Goose', 'item.gospodarstwo.raw_turkey': 'Raw Turkey', 'item.gospodarstwo.cooked_turkey': 'Roast Turkey',
        'item.gospodarstwo.down': 'Down', 'item.gospodarstwo.turkey_feather': 'Turkey Feather', 'item.gospodarstwo.snail_shell': 'Snail Shell',
        'item.gospodarstwo.snail_slime': 'Snail Slime', 'item.gospodarstwo.highland_hair': 'Highland Hair',
        'item.gospodarstwo.merino_wool': 'Merino Wool', 'item.gospodarstwo.mangalica_wool': 'Mangalica Wool', 'item.gospodarstwo.angora_wool': 'Angora Wool',
        'item.gospodarstwo.canvas': 'Canvas', 'item.gospodarstwo.linen': 'Linen', 'item.gospodarstwo.woolen_cloth': 'Woollen Cloth',
        'item.gospodarstwo.merino_cloth': 'Merino Cloth', 'item.gospodarstwo.angora_cloth': 'Angora Cloth', 'item.gospodarstwo.felt': 'Felt',
        'item.gospodarstwo.duck_spawn_egg': 'Duck Spawn Egg', 'item.gospodarstwo.goose_spawn_egg': 'Goose Spawn Egg',
        'item.gospodarstwo.turkey_spawn_egg': 'Turkey Spawn Egg', 'item.gospodarstwo.snail_spawn_egg': 'Snail Spawn Egg',
        'breed.gospodarstwo.highland': 'Highland Cow', 'breed.gospodarstwo.merino': 'Merino', 'breed.gospodarstwo.silkie': 'Silkie',
        'breed.gospodarstwo.greenleg': 'Green-legged Partridge', 'breed.gospodarstwo.mangalica': 'Mangalica', 'breed.gospodarstwo.angora': 'Angora Rabbit',
        'breed.gospodarstwo.is': 'Breed: %s', 'breed.gospodarstwo.label': 'Breed: %s', 'breed.gospodarstwo.regrowing': 'The coat has not grown back yet',
        'bonsai.gospodarstwo.empty': 'Plant a sapling of any tree here',
        'bonsai.gospodarstwo.status': 'Bonsai: %s%%, %s, trimmed %s/%s times',
        'bonsai.gospodarstwo.needs_trim': 'The tree has grown wild (%s%%, %s) - trim it with shears (%s/%s) to keep shaping it',
        'bonsai.gospodarstwo.done': 'A finished bonsai! (%s%%, %s, trimmed %s/%s times)',
        'bonsai.gospodarstwo.watered': 'watered', 'bonsai.gospodarstwo.dry': 'dry soil - water it',
        'bonsai.gospodarstwo.no_trim': 'Nothing to trim yet - let it grow',
        'loom.gospodarstwo.empty': 'The loom is empty - put in string, flax fibre or wool',
        'loom.gospodarstwo.other_yarn': 'Another yarn is on the loom - take it back first (sneak + right-click)',
        'gospodarstwo.configuration.title': 'Homestead settings',
    }
    wj(f'{A}/lang/pl_pl.json', pl)
    wj(f'{A}/lang/en_us.json', en)


# =====================================================================================  stage 1: textiles
YARN_NATURAL = {'yarn_wool': 0xEDEDED, 'yarn_linen': 0xD8CBA2, 'yarn_merino': 0xF4ECD6, 'yarn_angora': 0xFFFFFF,
                'yarn_cashmere': 0xE8DCCB, 'yarn_llama': 0xD9C2A0, 'yarn_coarse': 0xA88A64}
YARN_SHAPE = {'yarn_wool': 'yarn_ball', 'yarn_linen': 'yarn_hank', 'yarn_merino': 'yarn_ball', 'yarn_angora': 'yarn_fluffy',
              'yarn_cashmere': 'yarn_fluffy', 'yarn_llama': 'yarn_ball', 'yarn_coarse': 'yarn_hank'}
CLOTH_NATURAL = {'canvas': 0xD9D2BC, 'linen': 0xE6DCC0, 'woolen_cloth': 0xC8C2B8, 'merino_cloth': 0xEFE3C4, 'angora_cloth': 0xF8F4F8,
                 'cashmere_cloth': 0xDCCDB6, 'llama_cloth': 0xC8AE88, 'felt': 0x9E8C70}
FIBRES = {'cashmere': (0xE6D8C2, 'tuft'), 'llama_wool': (0xD4B88E, 'tuft'), 'camel_wool': (0xB98E5E, 'tuft'), 'dog_hair': (0x9A8670, 'tuft'),
          'highland_hair': (0xC06A34, 'tuft'), 'merino_wool': (0xF2EAD4, 'tuft'), 'mangalica_wool': (0xD9C39A, 'tuft'),
          'angora_wool': (0xF8F8F8, 'tuft'), 'horse_hair': (0x5A3A24, 'horse_hair')}


def argb_int(rgb):
    v = 0xFF000000 | rgb
    return v - (1 << 32)


def textiles():
    T = f'{A}/textures/item'
    bright = SP.grey(outline=70, dark=150, mid=200, light=230, high=252)
    for name, shape in YARN_SHAPE.items():
        wi(f'{T}/{name}.png', SP.render(SP.S[shape], bright))
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}',
                                                'tints': [{'type': 'minecraft:dye', 'default': argb_int(YARN_NATURAL[name])}]}})
    for name, col in CLOTH_NATURAL.items():
        wi(f'{T}/{name}.png', SP.render(SP.S['felt' if name == 'felt' else 'cloth'], bright))
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}',
                                                'tints': [{'type': 'minecraft:dye', 'default': argb_int(col)}]}})
    for name, (col, shape) in FIBRES.items():
        pal = SP.ramp(rgb(col))
        pal['s'] = (230, 220, 190)
        wi(f'{T}/{name}.png', SP.render(SP.S[shape], pal))
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
    wi(f'{T}/curry_comb.png', SP.render(SP.S['curry_comb'], SP.CURRY_PAL))
    wj(f'{A}/models/item/curry_comb.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{NS}:item/curry_comb'}})
    wj(f'{A}/items/curry_comb.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/curry_comb'}})

    # liquid of the dye vat (grey ripples, tinted by the renderer)
    img = Image.new('RGBA', (16, 16))
    px_ = img.load()
    for y in range(16):
        for x in range(16):
            v = 200 + int(30 * math.sin((x + y * 0.5) * 0.8) * math.cos(y * 0.6))
            px_[x, y] = (v, v, v, 225)
    wi(f'{A}/textures/block/dye_liquid.png', img)

    def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
        f = {}
        for face in faces:
            if face in ('up', 'down'):
                uv = [fr[0], fr[2], to[0], to[2]]
            elif face in ('north', 'south'):
                uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
            else:
                uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
            f[face] = {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv]}
        return {'from': fr, 'to': to, 'faces': f}

    disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}
    W, L = '#wood', '#log'
    wheel = [
        # bench with four splayed legs
        el([1, 4.5, 5], [15, 6, 11], W),
        el([2, 0, 5.5], [3, 4.5, 6.5], L), el([13, 0, 5.5], [14, 4.5, 6.5], L), el([2, 0, 9.5], [3, 4.5, 10.5], L), el([13, 0, 9.5], [14, 4.5, 10.5], L),
        # uprights holding the wheel axle
        el([10.2, 6, 7.4], [11.2, 12.2, 8.6], L), el([12.3, 6, 7.4], [13.3, 12.2, 8.6], L), el([10.2, 10.5, 7.5], [13.3, 11.5, 8.5], W),
        # mother-of-all and maidens holding the flyer and bobbin
        el([2.5, 6, 7.2], [5, 7.5, 8.8], W), el([2.7, 7.5, 5.6], [3.5, 12.8, 6.4], L), el([2.7, 7.5, 9.6], [3.5, 12.8, 10.4], L),
        # distaff (the post with fibre on top)
        el([5.6, 6, 7.6], [6.4, 15.5, 8.4], L),
        # treadle and footman
        el([6, 0.5, 2.5], [11, 1.5, 5], W), el([11.4, 1.5, 4.2], [11.9, 9, 4.7], W),
    ]
    wj(f'{A}/models/block/spinning_wheel.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/oak_planks', 'wood': 'minecraft:block/oak_planks', 'log': 'minecraft:block/stripped_oak_log'},
        'elements': wheel, 'display': disp})
    wj(f'{A}/blockstates/spinning_wheel.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/spinning_wheel', 'y': y} if y else
                                                                              {'model': f'{NS}:block/spinning_wheel'})
                                                             for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    wj(f'{A}/items/spinning_wheel.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/spinning_wheel'}})

    S_, I_ = '#staves', '#hoop'
    vat = [el([2, 0, 2], [14, 1, 14], S_),
           el([1, 0, 1], [15, 12, 2], S_), el([1, 0, 14], [15, 12, 15], S_), el([1, 0, 2], [2, 12, 14], S_), el([14, 0, 2], [15, 12, 14], S_),
           el([2, 1, 2], [14, 1.01, 14], '#inner', faces=('up',))]
    for y0 in (2.5, 8.5):
        vat += [el([0.6, y0, 0.6], [15.4, y0 + 1.2, 1.0], I_), el([0.6, y0, 15.0], [15.4, y0 + 1.2, 15.4], I_),
                el([0.6, y0, 1.0], [1.0, y0 + 1.2, 15.0], I_), el([15.0, y0, 1.0], [15.4, y0 + 1.2, 15.0], I_)]
    wj(f'{A}/models/block/dye_vat.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/spruce_planks', 'staves': 'minecraft:block/spruce_planks', 'hoop': 'minecraft:block/iron_block',
        'inner': 'minecraft:block/stripped_spruce_log'}, 'elements': vat, 'display': disp})
    wj(f'{A}/blockstates/dye_vat.json', {'variants': {'': {'model': f'{NS}:block/dye_vat'}}})
    wj(f'{A}/items/dye_vat.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/dye_vat'}})

    G = lambda n: f'{NS}:{n}'
    for b in ('spinning_wheel', 'dye_vat'):
        wj(f'{D}/{NS}/loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                                                            'entries': [{'type': 'minecraft:item', 'name': G(b)}]}],
                                                       'random_sequence': f'{NS}:blocks/{b}'})
    R = f'{D}/{NS}/recipe'
    wj(f'{R}/spinning_wheel.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['WS ', 'PPP', 'L L'],
                                    'key': {'W': '#minecraft:wooden_slabs', 'S': 'minecraft:string', 'P': '#minecraft:planks', 'L': 'minecraft:stick'},
                                    'result': {'id': G('spinning_wheel'), 'count': 1}})
    wj(f'{R}/dye_vat.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['P P', 'PIP', 'PPP'],
                             'key': {'P': '#minecraft:planks', 'I': 'minecraft:iron_ingot'}, 'result': {'id': G('dye_vat'), 'count': 1}})
    wj(f'{R}/curry_comb.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['NNN', 'SPS'],
                                'key': {'N': 'minecraft:iron_nugget', 'S': 'minecraft:stick', 'P': '#minecraft:planks'},
                                'result': {'id': G('curry_comb'), 'count': 1}})
    dyeable = [G(n) for n in list(YARN_NATURAL) + list(CLOTH_NATURAL)]
    wj(f'{D}/{NS}/tags/item/vat_dyeable.json', {'replace': False, 'values': dyeable})
    wj(f'{D}/minecraft/tags/item/dyeable.json', {'replace': False, 'values': dyeable})

    for lang_file, extra in [('pl_pl', {
        'block.gospodarstwo.spinning_wheel': 'Kołowrotek', 'block.gospodarstwo.dye_vat': 'Kadź farbiarska',
        'item.gospodarstwo.curry_comb': 'Zgrzebło', 'item.gospodarstwo.horse_hair': 'Włosie końskie', 'item.gospodarstwo.cashmere': 'Kaszmir',
        'item.gospodarstwo.llama_wool': 'Wełna lamy', 'item.gospodarstwo.camel_wool': 'Wełna wielbłądzia', 'item.gospodarstwo.dog_hair': 'Sierść psia',
        'item.gospodarstwo.yarn_wool': 'Przędza wełniana', 'item.gospodarstwo.yarn_linen': 'Przędza lniana',
        'item.gospodarstwo.yarn_merino': 'Przędza merynosowa', 'item.gospodarstwo.yarn_angora': 'Przędza angorska',
        'item.gospodarstwo.yarn_cashmere': 'Przędza kaszmirowa', 'item.gospodarstwo.yarn_llama': 'Przędza z wełny lamy',
        'item.gospodarstwo.yarn_coarse': 'Gruba przędza', 'item.gospodarstwo.cashmere_cloth': 'Tkanina kaszmirowa',
        'item.gospodarstwo.llama_cloth': 'Tkanina z wełny lamy',
        'station.gospodarstwo.empty': 'Pusto', 'station.gospodarstwo.other_input': 'Jest tu już inny materiał – zabierz go (kucnij + PPM)',
        'spinning.gospodarstwo.empty': 'Kołowrotek jest pusty – włóż wełnę, włókno lniane, kaszmir albo sierść',
        'loom.gospodarstwo.empty': 'Krosno jest puste – włóż przędzę albo sznurek',
        'vat.gospodarstwo.no_water': 'Najpierw nalej wody (wiadro wody)', 'vat.gospodarstwo.empty': 'Kadź jest pusta – nalej wody, potem dodaj barwnik',
        'vat.gospodarstwo.clean': 'Czysta woda – dodaj barwnik albo wypierz kolor z przędzy', 'vat.gospodarstwo.dye': 'Kąpiel barwiąca: starczy na %s sztuk',
        'groom.gospodarstwo.groomed': 'Zwierzę jest już wyczesane – zadowolone, ale sierść jeszcze nie odrosła'}),
        ('en_us', {
        'block.gospodarstwo.spinning_wheel': 'Spinning Wheel', 'block.gospodarstwo.dye_vat': 'Dye Vat',
        'item.gospodarstwo.curry_comb': 'Curry Comb', 'item.gospodarstwo.horse_hair': 'Horse Hair', 'item.gospodarstwo.cashmere': 'Cashmere',
        'item.gospodarstwo.llama_wool': 'Llama Wool', 'item.gospodarstwo.camel_wool': 'Camel Wool', 'item.gospodarstwo.dog_hair': 'Dog Hair',
        'item.gospodarstwo.yarn_wool': 'Wool Yarn', 'item.gospodarstwo.yarn_linen': 'Linen Yarn', 'item.gospodarstwo.yarn_merino': 'Merino Yarn',
        'item.gospodarstwo.yarn_angora': 'Angora Yarn', 'item.gospodarstwo.yarn_cashmere': 'Cashmere Yarn', 'item.gospodarstwo.yarn_llama': 'Llama Yarn',
        'item.gospodarstwo.yarn_coarse': 'Coarse Yarn', 'item.gospodarstwo.cashmere_cloth': 'Cashmere Cloth', 'item.gospodarstwo.llama_cloth': 'Llama Cloth',
        'station.gospodarstwo.empty': 'Empty', 'station.gospodarstwo.other_input': 'Another material is here - take it back first (sneak + right-click)',
        'spinning.gospodarstwo.empty': 'The spinning wheel is empty - put in wool, flax fibre, cashmere or hair',
        'loom.gospodarstwo.empty': 'The loom is empty - put in yarn or string',
        'vat.gospodarstwo.no_water': 'Fill it with water first (water bucket)', 'vat.gospodarstwo.empty': 'The vat is empty - add water, then a dye',
        'vat.gospodarstwo.clean': 'Clean water - add a dye, or wash the colour out of yarn', 'vat.gospodarstwo.dye': 'Dye bath: enough for %s pieces',
        'groom.gospodarstwo.groomed': 'Already groomed - happy, but the coat has not grown back yet'})]:
        path = f'{A}/lang/{lang_file}.json'
        d = json.load(open(path, encoding='utf-8'))
        d.update(extra)
        wj(path, d)


# =====================================================================================  stage 2: drops, rack, kettle
DROP_SPRITES = {
    'raw_hide': ('hide', 0xC9A27E), 'tallow': ('lump', 0xEDE3C8), 'horn': ('horn', 0xCDBB92), 'sinew': ('cord', 0xC89A80),
    'fatback': ('fatback', None), 'bristles': ('bristles', None), 'gut': ('cord', 0xD9A08A), 'rabbit_fur': ('hide', 0xB89A78),
    'fox_fur': ('hide', 0xD9782E), 'bear_fur': ('hide', 0xEDE8DA), 'bear_claw': ('claw', 0x4A4440), 'poultry_bone': ('small_bone', 0xEDE6D2),
    'swim_bladder': ('sac', 0xE6DCC4), 'tentacle': ('tentacle', 0x4A6A86), 'spinneret': ('sac', 0x5C4A44), 'venom_gland': ('sac', 0x58A03E),
    'rags': ('rag', 0x8A7E62), 'net_scrap': ('net', 0xA8966A), 'bone_marrow': ('jar', 0xE8C890), 'creeper_skin': ('hide', 0x5BAA48),
    'herb_pouch': ('pouch', 0x8A6440), 'strider_skin': ('hide', 0xB04A40), 'blaze_ash': ('pile', 0xE89A2A), 'ghast_membrane': ('hide', 0xE4E4EA),
    'thick_hide': ('hide', 0x7A5E48), 'hoglin_tusk': ('horn', 0xEDE2C6), 'ravager_horn': ('horn', 0x5E544A), 'guardian_spike': ('spike', 0xD8885A),
    'wind_fibre': ('wisp', 0xC8DAF2), 'poison_mycelium': ('pile', 0x5E8A3C), 'sculk_core': ('core', 0x1A8A9A), 'dragon_scale': ('scale', 0x4A3466),
    'thick_leather': ('hide', 0x5E3E24), 'beeswax': ('bar', 0xE8B23A), 'glue': ('jar', 0xDCCB8E), 'lard': ('jar', 0xF4EEE2), 'soap': ('bar', 0xD6E6DC),
}
DROP_NAMES = {
    'raw_hide': ('Surowa skóra', 'Raw Hide'), 'tallow': ('Łój', 'Tallow'), 'horn': ('Róg', 'Horn'), 'sinew': ('Ścięgno', 'Sinew'),
    'fatback': ('Słonina', 'Fatback'), 'bristles': ('Szczecina', 'Bristles'), 'gut': ('Jelito', 'Gut'), 'rabbit_fur': ('Futro królika', 'Rabbit Fur'),
    'fox_fur': ('Futro lisa', 'Fox Fur'), 'bear_fur': ('Futro niedźwiedzia', 'Bear Fur'), 'bear_claw': ('Pazur niedźwiedzia', 'Bear Claw'),
    'poultry_bone': ('Kostka drobiowa', 'Poultry Bone'), 'swim_bladder': ('Pęcherz pławny', 'Swim Bladder'), 'tentacle': ('Macka', 'Tentacle'),
    'spinneret': ('Kądziołek pajęczy', 'Spinneret'), 'venom_gland': ('Gruczoł jadowy', 'Venom Gland'), 'rags': ('Szmaty', 'Rags'),
    'net_scrap': ('Strzęp sieci', 'Net Scrap'), 'bone_marrow': ('Szpik kostny', 'Bone Marrow'), 'creeper_skin': ('Skóra creepera', 'Creeper Skin'),
    'herb_pouch': ('Sakiewka z ziołami', 'Herb Pouch'), 'strider_skin': ('Skóra lawochoda', 'Strider Skin'), 'blaze_ash': ('Popiół płomyka', 'Blaze Ash'),
    'ghast_membrane': ('Błona ghasta', 'Ghast Membrane'), 'thick_hide': ('Gruba skóra', 'Thick Hide'), 'hoglin_tusk': ('Kieł hoglina', 'Hoglin Tusk'),
    'ravager_horn': ('Róg niszczyciela', 'Ravager Horn'), 'guardian_spike': ('Kolec strażnika', 'Guardian Spike'), 'wind_fibre': ('Włókno wiatru', 'Wind Fibre'),
    'poison_mycelium': ('Trująca grzybnia', 'Poison Mycelium'), 'sculk_core': ('Rdzeń sculku', 'Sculk Core'), 'dragon_scale': ('Smocza łuska', 'Dragon Scale'),
    'thick_leather': ('Gruba skóra wyprawiona', 'Thick Leather'), 'beeswax': ('Wosk pszczeli', 'Beeswax'), 'glue': ('Klej kostny', 'Bone Glue'),
    'lard': ('Smalec', 'Lard'), 'soap': ('Mydło', 'Soap'),
}


def stage2():
    T = f'{A}/textures/item'
    for name, (shape, col) in DROP_SPRITES.items():
        if name == 'fatback':
            pal = SP.ramp((244, 236, 222))
            pal.update({'p': (222, 140, 130), 'P': (188, 98, 92)})
        elif name == 'bristles':
            pal = SP.ramp((70, 60, 54))
            pal['s'] = (168, 120, 70)
        else:
            pal = SP.ramp(rgb(col))
        if shape == 'pouch':
            pal.update({'g': (98, 160, 64), 'G': (60, 112, 42)})
        if shape == 'jar':
            pal.update({'b': (150, 104, 60), 'c': (214, 214, 220)})
        if name == 'ghast_membrane':
            pal = {k: v + (200,) for k, v in pal.items()}
        img = SP.render(SP.S[shape], pal)
        if name == 'rabbit_fur':
            pal['t'] = (240, 236, 228)
            img = SP.render(SP.S['pelt'], pal)
        elif name == 'fox_fur':
            pal['t'] = (248, 244, 236)
            img = SP.render(SP.S['pelt'], pal)
        elif name == 'bear_fur':
            pal['t'] = pal['d']
            img = SP.render(SP.S['pelt'], pal)
        elif name == 'thick_leather':
            pal['c'] = (60, 40, 26)
            img = SP.render(SP.S['roll'], pal)
        elif name == 'creeper_skin':
            SP.spots(img, (40, 88, 34), [(5, 5), (6, 6), (10, 5), (9, 6), (7, 8), (8, 8), (7, 9), (8, 9), (6, 10), (9, 10), (11, 9), (4, 8)])
        elif name == 'strider_skin':
            SP.spots(img, (230, 120, 100), [(5, 4), (9, 5), (6, 8), (11, 7), (8, 10), (5, 11), (10, 11)])
        elif name == 'ghast_membrane':
            SP.spots(img, (190, 190, 200), [(6, 6), (7, 7), (9, 6), (8, 9), (6, 10), (10, 10)])
        wi(f'{T}/{name}.png', img)
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
    bright = SP.grey(outline=70, dark=150, mid=200, light=230, high=252)
    wi(f'{T}/yarn_silk.png', SP.render(SP.S['yarn_hank'], bright))
    wi(f'{T}/silk.png', SP.render(SP.S['cloth'], bright))
    for name, col in (('yarn_silk', 0xF4F4F8), ('silk', 0xF0F0F6)):
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}',
                                                'tints': [{'type': 'minecraft:dye', 'default': argb_int(col)}]}})

    # stretched hide on the rack: grey (tinted in game) with grain, stretched corners and lace holes
    rnd = random.Random(7)
    img = Image.new('RGBA', (16, 16))
    p_ = img.load()
    for y in range(16):
        for x in range(16):
            # pinched edges between the lace points
            ex = min(x, 15 - x)
            ey = min(y, 15 - y)
            wav_x = 1 if (y % 4 in (1, 2)) else 0
            wav_y = 1 if (x % 4 in (1, 2)) else 0
            if ex < wav_x or ey < wav_y:
                continue
            if (ex == 0 and ey == 0):
                continue
            v = 232 + rnd.randint(-12, 12) + int(8 * math.sin(x * 0.9 + y * 0.3))
            if ex <= wav_x or ey <= wav_y:
                v -= 36
            v = min(255, v)
            p_[x, y] = (v, v, v, 255)
    for (x, y) in [(1, 3), (1, 7), (1, 11), (14, 3), (14, 7), (14, 11), (4, 1), (8, 1), (12, 1), (4, 14), (8, 14), (12, 14)]:
        p_[x, y] = (70, 70, 70, 255)
    wi(f'{A}/textures/block/stretched_hide.png', img)

    def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
        f = {}
        for face in faces:
            if face in ('up', 'down'):
                uv = [fr[0], fr[2], to[0], to[2]]
            elif face in ('north', 'south'):
                uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
            else:
                uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
            f[face] = {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv]}
        return {'from': fr, 'to': to, 'faces': f}

    disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}
    L, W = '#log', '#wood'
    rack = [
        # two posts with splayed feet
        el([0.5, 0, 7], [2, 16, 9], L), el([14, 0, 7], [15.5, 16, 9], L),
        el([0, 0, 4], [2.5, 1.5, 12], W), el([13.5, 0, 4], [16, 1.5, 12], W),
        # braces from the feet to the posts
        el([0.8, 1.5, 5], [1.7, 4, 6], W), el([0.8, 1.5, 10], [1.7, 4, 11], W),
        el([14.3, 1.5, 5], [15.2, 4, 6], W), el([14.3, 1.5, 10], [15.2, 4, 11], W),
        # top and bottom rails, with pegs sticking out
        el([2, 14, 7.25], [14, 15.5, 8.75], L), el([2, 1.5, 7.25], [14, 3, 8.75], L),
        el([-0.5, 14.3, 7.6], [0.5, 15.2, 8.4], W), el([15.5, 14.3, 7.6], [16.5, 15.2, 8.4], W),
    ]
    wj(f'{A}/models/block/tanning_rack.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/spruce_planks', 'log': 'minecraft:block/stripped_spruce_log', 'wood': 'minecraft:block/spruce_planks'},
        'elements': rack, 'display': disp})
    # pot made of walls, a rim, three feet and a bail handle
    P, I, H = '#side', '#inner', '#iron'
    pot = [el([4, 1, 4], [12, 2, 12], P),
           el([3, 1, 3], [13, 9, 4], P), el([3, 1, 12], [13, 9, 13], P), el([3, 1, 4], [4, 9, 12], P), el([12, 1, 4], [13, 9, 12], P),
           el([4, 2, 4], [12, 2.01, 12], I, faces=('up',)),
           el([2.5, 8.5, 2.5], [13.5, 9.8, 3.5], H), el([2.5, 8.5, 12.5], [13.5, 9.8, 13.5], H),
           el([2.5, 8.5, 3.5], [3.5, 9.8, 12.5], H), el([12.5, 8.5, 3.5], [13.5, 9.8, 12.5], H),
           el([4, 0, 4], [5.5, 1, 5.5], H), el([10.5, 0, 4], [12, 1, 5.5], H), el([7.25, 0, 10.5], [8.75, 1, 12], H),
           # ears and the handle
           el([2, 8, 7.4], [2.5, 11, 8.6], H), el([13.5, 8, 7.4], [14, 11, 8.6], H),
           el([2.1, 11, 7.7], [2.6, 13.5, 8.3], H), el([13.4, 11, 7.7], [13.9, 13.5, 8.3], H),
           el([2.1, 13.5, 7.7], [13.9, 14, 8.3], H)]
    wj(f'{A}/models/block/kettle.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'minecraft:block/cauldron_side', 'side': 'minecraft:block/cauldron_side', 'inner': 'minecraft:block/cauldron_inner',
        'iron': 'minecraft:block/anvil'}, 'elements': pot, 'display': disp})
    wj(f'{A}/blockstates/tanning_rack.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/tanning_rack', 'y': y} if y else
                                                                            {'model': f'{NS}:block/tanning_rack'})
                                                           for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    wj(f'{A}/blockstates/kettle.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/kettle', 'y': y} if y else {'model': f'{NS}:block/kettle'})
                                                     for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    for b in ('tanning_rack', 'kettle'):
        wj(f'{A}/items/{b}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{b}'}})
        wj(f'{D}/{NS}/loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                                                            'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{b}'}]}],
                                                       'random_sequence': f'{NS}:blocks/{b}'})
    G = lambda n: f'{NS}:{n}'
    R = f'{D}/{NS}/recipe'
    wj(f'{R}/tanning_rack.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SLS', 'S S', 'PLP'],
                                  'key': {'S': 'minecraft:stick', 'L': '#minecraft:logs', 'P': '#minecraft:planks'},
                                  'result': {'id': G('tanning_rack'), 'count': 1}})
    wj(f'{R}/kettle.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', 'I I', 'III'],
                            'key': {'S': 'minecraft:stick', 'I': 'minecraft:iron_ingot'}, 'result': {'id': G('kettle'), 'count': 1}})
    wj(f'{R}/soap.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                          'ingredients': [G('tallow'), G('tallow'), 'minecraft:charcoal'], 'result': {'id': G('soap'), 'count': 2}})
    wj(f'{R}/soap_from_lard.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                    'ingredients': [G('lard'), 'minecraft:charcoal'], 'result': {'id': G('soap'), 'count': 1}})
    wj(f'{R}/candle_from_tallow.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                        'ingredients': [G('tallow'), 'minecraft:string'], 'result': {'id': 'minecraft:candle', 'count': 1}})
    wj(f'{R}/candle_from_beeswax.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                         'ingredients': [G('beeswax'), 'minecraft:string'], 'result': {'id': 'minecraft:candle', 'count': 2}})
    wj(f'{R}/string_from_sinew.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                       'ingredients': [G('sinew')], 'result': {'id': 'minecraft:string', 'count': 2}})
    wj(f'{R}/string_from_gut.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                     'ingredients': [G('gut')], 'result': {'id': 'minecraft:string', 'count': 1}})
    wj(f'{R}/leather_from_furs.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                       'ingredients': [[G('rabbit_fur'), G('fox_fur')], [G('rabbit_fur'), G('fox_fur')]],
                                       'result': {'id': 'minecraft:leather', 'count': 1}})
    wj(f'{R}/bone_meal_from_poultry_bone.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                 'ingredients': [G('poultry_bone')], 'result': {'id': 'minecraft:bone_meal', 'count': 2}})
    wj(f'{R}/paper_from_rags.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                     'ingredients': [G('rags'), G('rags'), G('rags'), 'minecraft:water_bucket'],
                                     'result': {'id': 'minecraft:paper', 'count': 3}})
    wj(f'{R}/string_from_net_scrap.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                           'ingredients': [G('net_scrap')], 'result': {'id': 'minecraft:string', 'count': 3}})
    wj(f'{R}/cooked_tentacle.json', {'type': 'minecraft:smelting', 'category': 'food', 'ingredient': G('tentacle'),
                                     'result': {'id': G('cooked_tentacle')}, 'experience': 0.2, 'cookingtime': 200})
    T2 = f'{A}/textures/item'
    wi(f'{T2}/cooked_tentacle.png', SP.render(SP.S['tentacle'], SP.ramp((196, 120, 84))))
    wj(f'{A}/models/item/cooked_tentacle.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/cooked_tentacle'}})
    wj(f'{A}/items/cooked_tentacle.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/cooked_tentacle'}})

    pl = {f'item.{NS}.{k}': v[0] for k, v in DROP_NAMES.items()}
    en = {f'item.{NS}.{k}': v[1] for k, v in DROP_NAMES.items()}
    pl.update({'item.gospodarstwo.yarn_silk': 'Przędza jedwabna', 'item.gospodarstwo.silk': 'Jedwab', 'item.gospodarstwo.cooked_tentacle': 'Pieczona macka',
               'block.gospodarstwo.tanning_rack': 'Rama garbarska', 'block.gospodarstwo.kettle': 'Kocioł rzemieślniczy',
               'rack.gospodarstwo.empty': 'Rama jest pusta – rozepnij na niej surową albo grubą skórę',
               'kettle.gospodarstwo.empty': 'Kocioł jest pusty – wrzuć plaster miodu, kości, szpik, pęcherz albo słoninę',
               'kettle.gospodarstwo.cold': 'Kocioł jest zimny – postaw go na rozpalonym ognisku, ogniu albo magmie'})
    en.update({'item.gospodarstwo.yarn_silk': 'Silk Yarn', 'item.gospodarstwo.silk': 'Silk', 'item.gospodarstwo.cooked_tentacle': 'Grilled Tentacle',
               'block.gospodarstwo.tanning_rack': 'Tanning Rack', 'block.gospodarstwo.kettle': 'Craft Kettle',
               'rack.gospodarstwo.empty': 'The rack is empty - stretch a raw or thick hide on it',
               'kettle.gospodarstwo.empty': 'The kettle is empty - add honeycomb, bones, marrow, a swim bladder or fatback',
               'kettle.gospodarstwo.cold': 'The kettle is cold - stand it on a lit campfire, fire or magma'})
    for lang_file, extra in (('pl_pl', pl), ('en_us', en)):
        path = f'{A}/lang/{lang_file}.json'
        d = json.load(open(path, encoding='utf-8'))
        d.update(extra)
        wj(path, d)
    dyeable = [G(n) for n in list(YARN_NATURAL) + list(CLOTH_NATURAL) + ['yarn_silk', 'silk']]
    wj(f'{D}/{NS}/tags/item/vat_dyeable.json', {'replace': False, 'values': dyeable})
    wj(f'{D}/minecraft/tags/item/dyeable.json', {'replace': False, 'values': dyeable})


# =====================================================================================  stage 3: clothes, bedding, home
# humanoid armour texture (64x32) face rectangles: name -> (x, y, w, h)
HEAD = {'top': (8, 0, 8, 8), 'bottom': (16, 0, 8, 8), 'right': (0, 8, 8, 8), 'front': (8, 8, 8, 8), 'left': (16, 8, 8, 8), 'back': (24, 8, 8, 8)}
BODY = {'top': (20, 16, 8, 4), 'bottom': (28, 16, 8, 4), 'right': (16, 20, 4, 12), 'front': (20, 20, 8, 12), 'left': (28, 20, 4, 12),
        'back': (32, 20, 8, 12)}
ARM = {'top': (44, 16, 4, 4), 'bottom': (48, 16, 4, 4), 'right': (40, 20, 4, 12), 'front': (44, 20, 4, 12), 'left': (48, 20, 4, 12),
       'back': (52, 20, 4, 12)}
LEG = {'top': (4, 16, 4, 4), 'bottom': (8, 16, 4, 4), 'right': (0, 20, 4, 12), 'front': (4, 20, 4, 12), 'left': (8, 20, 4, 12), 'back': (12, 20, 4, 12)}


def paint(img, part, fn):
    """fn(face, x, y, w, h) -> RGBA or None, for every pixel of every face of a body part."""
    p = img.load()
    for face, (fx, fy, w, h) in part.items():
        for y in range(h):
            for x in range(w):
                c = fn(face, x, y, w, h)
                if c is not None:
                    p[fx + x, fy + y] = c if len(c) == 4 else tuple(c) + (255,)


def g(v):
    v = max(0, min(255, int(v)))
    return (v, v, v)


def knit(x, y, base=214):
    # V-shaped stitches: neighbouring columns lean opposite ways
    k = (x + (y // 1)) % 2
    v = base + (10 if (x % 2 == 0) == (y % 2 == 0) else -8)
    return v


def tint(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def noisy(rnd, c, amt=0.08):
    return tint(c, 1 + rnd.uniform(-amt, amt))


def armour_textures():
    E = f'{A}/textures/entity/equipment'
    rnd = random.Random(31)
    # ---- wool: knitted cap, Nordic sweater, knitted trousers (grey, tinted by the dye colour) ----
    hum = Image.new('RGBA', (64, 32))

    def cap(face, x, y, w, h):
        rib = g(170 + (40 if x % 2 == 0 else 0))
        if face == 'top':
            d = abs(x - 3.5) + abs(y - 3.5)
            return g(knit(x, y) + (14 if d < 1.5 else 0))
        if face == 'bottom':
            return None
        last = 3 if face == 'front' else 6 if face == 'back' else 5
        if y > last + 1:
            return None
        if y >= last:
            return rib
        return g(knit(x, y))
    paint(hum, HEAD, cap)

    def nordic(y, x, top):
        # band of small diamonds between two dotted rows
        if y == top or y == top + 4:
            return 244 if x % 2 == 0 else 196
        if top < y < top + 4:
            dy = abs(y - (top + 2))
            return 248 if (x % 4) in ((2 - dy) % 4, (2 + dy) % 4) else 186
        return None

    def sweater(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return g(knit(x, y) - 6)
        if y >= h - 2:
            return g(168 + (44 if x % 2 == 0 else 0))
        n = nordic(y, x + (2 if face in ('right', 'left') else 0), 2)
        if n is not None:
            return g(n)
        if face == 'front' and y == 0 and 2 <= x <= 5:
            return g(170 + (40 if x % 2 == 0 else 0))
        return g(knit(x, y))
    paint(hum, BODY, sweater)

    def sleeve(face, x, y, w, h):
        if face == 'top':
            return g(knit(x, y))
        if face == 'bottom':
            return g(170)
        if y >= h - 2:
            return g(168 + (44 if x % 2 == 0 else 0))
        n = nordic(y, x, 2)
        if n is not None:
            return g(n)
        return g(knit(x, y))
    paint(hum, ARM, sleeve)
    wi(f'{E}/humanoid/wool.png', hum)

    legs = Image.new('RGBA', (64, 32))

    def trousers(face, x, y, w, h):
        if face == 'top':
            return None
        if face == 'bottom':
            return g(168)
        if y >= h - 2:
            return g(166 + (44 if x % 2 == 0 else 0))
        # a cable down the front of each leg
        if face == 'front' and x in (1, 2):
            return g(236 if (y + x) % 3 == 0 else 200)
        return g(knit(x, y, 206))
    paint(legs, LEG, trousers)

    def waist(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return g(180) if face == 'bottom' else None
        if y < 7:
            return None
        if y < 9:
            return g(166 + (44 if x % 2 == 0 else 0))
        return g(knit(x, y, 206))
    paint(legs, BODY, waist)
    wi(f'{E}/humanoid_leggings/wool.png', legs)

    # ---- felt boots (walonki): thick felt shafts up to the knee, darker sole ----
    felt = Image.new('RGBA', (64, 32))
    FELT = rgb(0x8E7A5E)

    def boots(face, x, y, w, h):
        if face == 'top':
            return None
        if face == 'bottom':
            return tint(FELT, 0.45)
        if y < 3:
            return None
        if y in (3, 4):
            return noisy(rnd, tint(FELT, 1.25 if y == 3 else 1.12), 0.04)  # turned-down cuff
        if y == 5:
            return tint(FELT, 0.8)
        if y == h - 1:
            return tint(FELT, 0.45)  # sole
        if y == h - 2:
            return tint(FELT, 0.85)
        return noisy(rnd, FELT, 0.07)
    paint(felt, LEG, boots)
    wi(f'{E}/humanoid/felt.png', felt)

    # ---- fur hat (ushanka): brown crown, thick fluffy fur band and ear flaps ----
    fur = Image.new('RGBA', (64, 32))
    CROWN, FUR = rgb(0x5E3E26), rgb(0xB08458)

    def fluff(x, y):
        return noisy(rnd, tint(FUR, 1.15 if (x + y * 3) % 5 == 0 else 0.92 if (x * 7 + y) % 4 == 0 else 1.0), 0.06)

    def hat(face, x, y, w, h):
        if face == 'top':
            return noisy(rnd, CROWN, 0.08)
        if face == 'bottom':
            return None
        if face == 'front':
            if y <= 1:
                return noisy(rnd, CROWN, 0.06)
            if y <= 3:
                return fluff(x, y)
            return None
        if face == 'back':
            return fluff(x, y) if y <= 6 else None
        # side faces: band all along, ear flap hanging down in the middle
        if y <= 1:
            return noisy(rnd, CROWN, 0.06)
        if y <= 3:
            return fluff(x, y)
        if 2 <= x <= 5 and y <= 7:
            return fluff(x, y) if y < 7 or x in (3, 4) else None
        return None
    paint(fur, HEAD, hat)
    wi(f'{E}/humanoid/fur.png', fur)

    # ---- sheepskin coat (kożuch): suede with a fleece collar, cuffs and front edge, toggles ----
    coat = Image.new('RGBA', (64, 32))
    SUEDE, FLEECE = rgb(0x9A6A3E), rgb(0xEDE4D2)

    def fleece(x, y):
        return noisy(rnd, tint(FLEECE, 0.9 if (x + y) % 3 == 0 else 1.0), 0.04)

    def coat_body(face, x, y, w, h):
        if face == 'top':
            return fleece(x, y)
        if face == 'bottom':
            return fleece(x, y)
        if y <= 1 or y == h - 1:
            return fleece(x, y)
        if face == 'front' and x in (3, 4):
            if x == 4 and y in (4, 7, 10):
                return (226, 200, 150)  # wooden toggles
            return fleece(x, y)
        c = noisy(rnd, SUEDE, 0.06)
        if face == 'front' and y == 8 and x in (0, 1, 6, 7):
            return tint(SUEDE, 0.7)  # pocket edges
        return c
    paint(coat, BODY, coat_body)

    def coat_arm(face, x, y, w, h):
        if face == 'bottom':
            return fleece(x, y)
        if face == 'top':
            return noisy(rnd, SUEDE, 0.06)
        if y >= h - 2:
            return fleece(x, y)
        return noisy(rnd, SUEDE, 0.06)
    paint(coat, ARM, coat_arm)
    wi(f'{E}/humanoid/sheepskin.png', coat)

    # ---- linen: shirt with a V neck, buttons and rolled-up short sleeves; trousers with a rope belt ----
    lin = Image.new('RGBA', (64, 32))

    def weave(x, y, base=228):
        return g(base + (6 if (x + y) % 2 == 0 else -5) + (-6 if y % 4 == 3 else 0))

    def shirt(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return weave(x, y, 220)
        if face == 'front':
            if y <= 1 and 3 <= x <= 4:
                return None  # V neck: the skin shows through
            if y <= 1 and x in (2, 5):
                return g(196)
            if x == 4 and y in (3, 6, 9):
                return g(150)  # buttons
            if x == 3 and 2 <= y <= 10:
                return g(206)  # placket
        if y == h - 1:
            return g(206)
        return weave(x, y)
    paint(lin, BODY, shirt)

    def short_sleeve(face, x, y, w, h):
        if face == 'top':
            return weave(x, y)
        if face == 'bottom':
            return None
        if y > 6:
            return None
        if y >= 5:
            return g(204 if y == 5 else 186)
        return weave(x, y)
    paint(lin, ARM, short_sleeve)
    wi(f'{E}/humanoid/linen.png', lin)

    lin_legs = Image.new('RGBA', (64, 32))

    def lin_trousers(face, x, y, w, h):
        if face == 'top':
            return None
        if face == 'bottom':
            return None
        if y >= h - 1:
            return None
        if y >= h - 3:
            return g(204 if y == h - 3 else 184)
        return weave(x, y, 222)
    paint(lin_legs, LEG, lin_trousers)

    def rope(face, x, y, w, h):
        if face == 'bottom':
            return g(200)
        if face == 'top' or y < 7:
            return None
        if y == 8:
            return (160 + (30 if x % 2 == 0 else 0), 126 + (24 if x % 2 == 0 else 0), 80)  # rope belt (not tinted much)
        return weave(x, y, 222)
    paint(lin_legs, BODY, rope)
    wi(f'{E}/humanoid_leggings/linen.png', lin_legs)

    def eq(name, dye=None, legs=False, body=True):
        layer = {'texture': f'{NS}:{name}'}
        if dye is not None:
            layer['dyeable'] = {'color_when_undyed': argb_int(dye)}
        layers = {}
        if body:
            layers['humanoid'] = [layer]
        if legs:
            layers['humanoid_leggings'] = [layer]
        wj(f'{A}/equipment/{name}.json', {'layers': layers})
    eq('wool', 0xEDE6DA, legs=True)
    eq('felt')
    eq('fur')
    eq('sheepskin')
    eq('linen', 0xE6DCC0, legs=True)


CLOTHES = {  # item: (sprite, natural dye colour or None, palette)
    'wool_cap': ('cap', 0xEDE6DA), 'wool_sweater': ('sweater', 0xEDE6DA), 'wool_trousers': ('trousers', 0xEDE6DA),
    'linen_shirt': ('shirt', 0xE6DCC0), 'linen_trousers': ('trousers_belt', 0xE6DCC0),
    'felt_boots': ('boot', None), 'fur_hat': ('ushanka', None), 'sheepskin_coat': ('coat', None),
}


def stage3():
    armour_textures()
    T = f'{A}/textures/item'
    bright = SP.grey(outline=64, dark=150, mid=204, light=232, high=252)
    bright.update({'r': (168, 168, 168), 'p': (248, 248, 248), 'b': (120, 120, 120), 'c': (150, 112, 70)})
    for name, (shape, dye) in CLOTHES.items():
        if dye is not None:
            img = SP.render(SP.S[shape], bright)
            if shape == 'trousers_belt':
                # the rope belt is drawn on a second, untinted layer
                belt = Image.new('RGBA', (16, 16))
                pp, bp = img.load(), belt.load()
                for x in range(16):
                    if pp[x, 2][3] and SP.S[shape][2][x] == 'c':
                        bp[x, 2] = (176, 134, 82, 255)
                wi(f'{T}/{name}_belt.png', belt)
            wi(f'{T}/{name}.png', img)
            layers = {'layer0': f'{NS}:item/{name}'}
            if shape == 'trousers_belt':
                layers['layer1'] = f'{NS}:item/{name}_belt'
            wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': layers})
            wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}',
                                                    'tints': [{'type': 'minecraft:dye', 'default': argb_int(dye)}]}})
        else:
            if name == 'felt_boots':
                pal = SP.ramp(rgb(0x8E7A5E))
                pal['c'] = tint(rgb(0x8E7A5E), 0.75)
            elif name == 'fur_hat':
                pal = SP.ramp(rgb(0x5E3E26))
                pal.update({'f': rgb(0xC89A68), 'F': rgb(0xA27850)})
            else:
                pal = SP.ramp(rgb(0x9A6A3E))
                pal.update({'f': rgb(0xEFE6D4), 'F': rgb(0xD6CAB4)})
            wi(f'{T}/{name}.png', SP.render(SP.S[shape], pal))
            wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
            wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
    # pillow (white linen with a tuft) and featherbed (red-white striped ticking, a Polish "pasiak")
    pal = SP.ramp((236, 230, 216))
    wi(f'{T}/pillow.png', SP.render(SP.S['pillow'], pal))
    pal = SP.ramp((236, 230, 216))
    pal.update({'R': (200, 62, 56), 'P': (158, 46, 42)})
    wi(f'{T}/featherbed.png', SP.render(SP.S['featherbed'], pal))
    for n in ('pillow', 'featherbed'):
        wj(f'{A}/models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})
        wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{n}'}})
    # curtain item: tinted cloth + untinted rod; rug item: tinted
    wi(f'{T}/curtain.png', SP.render(SP.S['curtain_cloth'], bright))
    rod = SP.ramp((112, 78, 48))
    rod['c'] = (124, 86, 52)
    wi(f'{T}/curtain_rod.png', SP.render(SP.S['curtain_rod'], rod))
    wj(f'{A}/models/item/curtain.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/curtain', 'layer1': f'{NS}:item/curtain_rod'}})
    wj(f'{A}/items/curtain.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/curtain',
                                             'tints': [{'type': 'minecraft:dye', 'default': argb_int(0xE6DCC0)}]}})
    wi(f'{T}/wool_rug.png', SP.render(SP.S['rug'], bright))
    wj(f'{A}/models/item/wool_rug.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/wool_rug'}})
    wj(f'{A}/items/wool_rug.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/wool_rug',
                                              'tints': [{'type': 'minecraft:dye', 'default': argb_int(0xC8C2B8)}]}})
    lpal = {'o': (36, 34, 38), 'i': (78, 76, 84), 'I': (112, 110, 118), 'a': (196, 126, 48), 'h': (238, 178, 84), 'H': (255, 230, 150)}
    wi(f'{T}/horn_lantern.png', SP.render(SP.S['lantern'], lpal))
    wj(f'{A}/models/item/horn_lantern.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/horn_lantern'}})
    wj(f'{A}/items/horn_lantern.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/horn_lantern'}})

    # ---- block textures ----
    B = f'{A}/textures/block'
    img = Image.new('RGBA', (16, 16))
    px_ = img.load()
    for y in range(16):
        for x in range(16):
            v = 226 + (5 if (x + y) % 2 == 0 else -4) + (-5 if y % 4 == 3 else 0)
            if y in (0, 15):
                v = 204
            if y == 1 or y == 14:
                v = 214
            px_[x, y] = (v, v, v, 255)
    wi(f'{B}/curtain_cloth.png', img)
    # rugs: 16 variants by which sides continue into another rug (1 n, 2 s, 4 w, 8 e)
    for mask in range(16):
        img = Image.new('RGBA', (16, 16))
        px_ = img.load()
        for y in range(16):
            for x in range(16):
                dn = 99 if mask & 1 else y
                ds = 99 if mask & 2 else 15 - y
                dw = 99 if mask & 4 else x
                de = 99 if mask & 8 else 15 - x
                d = min(dn, ds, dw, de)
                if d == 0:
                    v = 140
                elif d == 1:
                    v = 166 + (30 if (x + y) % 2 == 0 else 0)  # braided edge
                elif d == 2:
                    v = 240
                else:
                    lx, ly = x % 8, y % 8
                    dia = abs(lx - 3.5) + abs(ly - 3.5)
                    v = 236 if 2.6 < dia < 3.6 else 206 if dia > 3.6 else 220
                    if dia < 1.1:
                        v = 244
                    v += (3 if (x + y) % 2 == 0 else -3)
                px_[x, y] = (v, v, v, 255)
        wi(f'{B}/rug/rug_{mask}.png', img)
    # rag rug: rows of coloured rag strips woven over a warp
    rag_cols = [(176, 72, 60), (84, 112, 150), (196, 170, 96), (110, 140, 84), (150, 96, 130), (200, 190, 170), (120, 90, 70)]
    rr = random.Random(5)
    img = Image.new('RGBA', (16, 16))
    px_ = img.load()
    row_col = [rag_cols[rr.randrange(len(rag_cols))] for _ in range(8)]
    for y in range(16):
        c = row_col[y // 2]
        for x in range(16):
            k = 1.08 if (x // 2 + y) % 2 == 0 else 0.86
            if y % 2 == 1:
                k *= 0.9
            px_[x, y] = tint(noisy(rr, c, 0.05), k) + (255,)
    wi(f'{B}/rag_rug.png', img)
    # horn lantern block texture: (0,0)-(6,7) side pane, (0,9)-(6,15) top/bottom, (8,0)-(12,4) iron, (8,6)-(9,16) chain
    img = Image.new('RGBA', (16, 16))
    px_ = img.load()
    IRON, IRON_L = (64, 62, 70), (104, 102, 112)
    for y in range(7):
        for x in range(6):
            if x in (0, 5) or y in (0, 6):
                c = IRON_L if y == 0 else IRON
            else:
                t = 1 - abs(y - 3) / 4
                c = (int(214 + 40 * t), int(140 + 70 * t), int(52 + 60 * t))
                if (x * 3 + y) % 5 == 0:
                    c = tint(c, 0.9)  # streaks in the horn
            px_[x, y] = c + (255,)
    for y in range(9, 15):
        for x in range(6):
            px_[x, y] = (IRON_L if (x + y) % 3 else IRON) + (255,)
    for y in range(0, 4):
        for x in range(8, 12):
            px_[x, y] = (IRON_L if y == 0 else IRON) + (255,)
    for y in range(6, 16):
        px_[8, y] = (IRON if y % 2 else IRON_L) + (255,)
    wi(f'{B}/horn_lantern.png', img)

    def el(fr, to, uv_side, uv_top, tex='#t', light=None):
        f = {}
        for face in ('north', 'south', 'east', 'west'):
            f[face] = {'texture': tex, 'uv': uv_side}
        for face in ('up', 'down'):
            f[face] = {'texture': tex, 'uv': uv_top}
        e = {'from': fr, 'to': to, 'faces': f}
        if light:
            e['light_emission'] = light
        return e

    def lantern(dy):
        els = [el([5, 0 + dy, 5], [11, 7 + dy, 11], [0, 0, 6, 7], [0, 9, 6, 15], light=12),
               el([6, 7 + dy, 6], [10, 8 + dy, 10], [8, 0, 12, 1], [8, 0, 12, 4]),
               el([6.5, 8 + dy, 7.75], [7, 10 + dy, 8.25], [8, 6, 9, 8], [8, 6, 9, 7]),
               el([9, 8 + dy, 7.75], [9.5, 10 + dy, 8.25], [8, 6, 9, 8], [8, 6, 9, 7]),
               el([6.5, 10 + dy, 7.75], [9.5, 10.5 + dy, 8.25], [8, 1, 11, 2], [8, 1, 11, 2])]
        if dy:
            els.append(el([7.75, 10.5 + dy, 7.75], [8.25, 16, 8.25], [8, 6, 9, 16], [8, 6, 9, 7]))
        return els
    for name, dy in (('horn_lantern', 0), ('horn_lantern_hanging', 1)):
        wj(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/block', 'ambientocclusion': False,
                                             'textures': {'t': f'{NS}:block/horn_lantern', 'particle': f'{NS}:block/horn_lantern'},
                                             'elements': lantern(dy)})
    wj(f'{A}/blockstates/horn_lantern.json', {'variants': {'hanging=false': {'model': f'{NS}:block/horn_lantern'},
                                                           'hanging=true': {'model': f'{NS}:block/horn_lantern_hanging'}}})
    # curtain and woven rug are drawn by their renderers: models only give the breaking particles
    wj(f'{A}/models/block/curtain.json', {'textures': {'particle': f'{NS}:block/curtain_cloth'}})
    wj(f'{A}/models/block/wool_rug.json', {'textures': {'particle': f'{NS}:block/rug/rug_0'}})
    wj(f'{A}/blockstates/curtain.json', {'variants': {'': {'model': f'{NS}:block/curtain'}}})
    wj(f'{A}/blockstates/wool_rug.json', {'variants': {'': {'model': f'{NS}:block/wool_rug'}}})
    wj(f'{A}/models/block/rag_rug.json', {'parent': 'minecraft:block/carpet', 'textures': {'wool': f'{NS}:block/rag_rug'}})
    wj(f'{A}/blockstates/rag_rug.json', {'variants': {'': {'model': f'{NS}:block/rag_rug'}}})
    wj(f'{A}/items/rag_rug.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/rag_rug'}})

    # rested effect icon: a crescent moon with a little "z"
    img = Image.new('RGBA', (18, 18))
    px_ = img.load()
    for y in range(18):
        for x in range(18):
            d1 = ((x - 8) ** 2 + (y - 9) ** 2) ** 0.5
            d2 = ((x - 11) ** 2 + (y - 7) ** 2) ** 0.5
            if d1 <= 6.5 and d2 > 5.2:
                edge = d1 > 5.6 or d2 < 6.0
                px_[x, y] = ((168, 128, 40) if edge else (250, 222, 120)) + (255,)
    for x, y in [(12, 11), (13, 11), (14, 11), (13, 12), (12, 13), (13, 13), (14, 13)]:
        px_[x, y] = (230, 236, 255, 255)
    wi(f'{A}/textures/mob_effect/rested.png', img)

    # ---- data: loot, recipes, tags ----
    G = lambda n: f'{NS}:{n}'
    for b, copy in (('curtain', True), ('wool_rug', True), ('rag_rug', False), ('horn_lantern', False)):
        entry = {'type': 'minecraft:item', 'name': G(b)}
        if copy:
            entry['functions'] = [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['minecraft:dyed_color']}]
        wj(f'{D}/{NS}/loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                                                            'entries': [entry]}],
                                                       'random_sequence': f'{NS}:blocks/{b}'})
    R = f'{D}/{NS}/recipe'

    def shaped(name, pattern, key, count=1, cat='equipment'):
        wj(f'{R}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': cat, 'pattern': pattern, 'key': key,
                                'result': {'id': G(name), 'count': count}})
    shaped('wool_cap', ['YYY', 'Y Y'], {'Y': G('yarn_wool')})
    shaped('wool_sweater', ['Y Y', 'YYY', 'YYY'], {'Y': G('yarn_wool')})
    shaped('wool_trousers', ['YYY', 'Y Y', 'Y Y'], {'Y': G('yarn_wool')})
    shaped('felt_boots', ['F F', 'F F'], {'F': G('felt')})
    shaped('fur_hat', ['FLF', 'F F'], {'F': '#' + G('furs'), 'L': 'minecraft:leather'})
    shaped('sheepskin_coat', ['L L', 'LWL', 'LLL'], {'L': 'minecraft:leather', 'W': '#minecraft:wool'})
    shaped('linen_shirt', ['C C', 'CCC', 'CCC'], {'C': G('linen')})
    shaped('linen_trousers', ['CCC', 'C C', 'C C'], {'C': G('linen')})
    shaped('pillow', ['DDD', 'CCC'], {'D': '#' + G('stuffing'), 'C': G('linen')}, cat='misc')
    shaped('featherbed', ['DDD', 'DDD', 'CCC'], {'D': '#' + G('stuffing'), 'C': G('linen')}, cat='misc')
    shaped('rag_rug', ['RRR'], {'R': G('rags')}, count=2, cat='building')
    shaped('horn_lantern', ['NNN', 'HTH', 'NNN'], {'N': 'minecraft:iron_nugget', 'H': G('horn'), 'T': 'minecraft:torch'}, cat='building')
    # curtains and rugs keep the colour of the cloth they are made of
    wj(f'{R}/curtain.json', {'type': 'minecraft:crafting_transmute', 'category': 'building', 'input': '#' + G('cloths'),
                             'material': 'minecraft:stick', 'result': {'id': G('curtain')}})
    wj(f'{R}/wool_rug.json', {'type': 'minecraft:crafting_transmute', 'category': 'building', 'input': G('woolen_cloth'),
                              'material': G('yarn_wool'), 'result': {'id': G('wool_rug')}})

    def tag(path, values):
        wj(f'{D}/{path}.json', {'replace': False, 'values': values})
    warm = [G(n) for n in ('wool_cap', 'wool_sweater', 'wool_trousers', 'felt_boots', 'fur_hat', 'sheepskin_coat')]
    airy = [G('linen_shirt'), G('linen_trousers')]
    tag(f'{NS}/tags/item/warm_clothing', warm)
    tag(f'{NS}/tags/item/airy_clothing', airy)
    tag('minecraft/tags/item/freeze_immune_wearables', warm)
    tag(f'{NS}/tags/item/furs', [G('rabbit_fur'), G('fox_fur'), G('bear_fur')])
    tag(f'{NS}/tags/item/stuffing', [G('down'), 'minecraft:feather', G('turkey_feather')])
    tag(f'{NS}/tags/item/cloths', [G(n) for n in list(CLOTH_NATURAL) + ['silk']])
    tag(f'{NS}/tags/item/repairs_wool', [G('yarn_wool')])
    tag(f'{NS}/tags/item/repairs_felt', [G('felt')])
    tag(f'{NS}/tags/item/repairs_fur', ['#' + G('furs')])
    tag(f'{NS}/tags/item/repairs_sheepskin', ['minecraft:leather'])
    tag(f'{NS}/tags/item/repairs_linen', [G('linen')])
    tag('minecraft/tags/item/head_armor', [G('wool_cap'), G('fur_hat')])
    tag('minecraft/tags/item/chest_armor', [G('wool_sweater'), G('sheepskin_coat'), G('linen_shirt')])
    tag('minecraft/tags/item/leg_armor', [G('wool_trousers'), G('linen_trousers')])
    tag('minecraft/tags/item/foot_armor', [G('felt_boots')])
    dyeable = [G(n) for n in list(YARN_NATURAL) + list(CLOTH_NATURAL) + ['yarn_silk', 'silk', 'wool_cap', 'wool_sweater', 'wool_trousers',
                                                                           'linen_shirt', 'linen_trousers', 'curtain', 'wool_rug']]
    tag(f'{NS}/tags/item/vat_dyeable', dyeable)
    tag('minecraft/tags/item/dyeable', dyeable)

    pl = {'item.gospodarstwo.wool_cap': 'Wełniana czapka', 'item.gospodarstwo.wool_sweater': 'Sweter', 'item.gospodarstwo.wool_trousers': 'Wełniane spodnie',
          'item.gospodarstwo.felt_boots': 'Walonki', 'item.gospodarstwo.fur_hat': 'Futrzana uszanka', 'item.gospodarstwo.sheepskin_coat': 'Kożuch',
          'item.gospodarstwo.linen_shirt': 'Lniana koszula', 'item.gospodarstwo.linen_trousers': 'Lniane spodnie',
          'item.gospodarstwo.pillow': 'Poduszka', 'item.gospodarstwo.featherbed': 'Pierzyna',
          'block.gospodarstwo.curtain': 'Zasłona', 'block.gospodarstwo.wool_rug': 'Dywan tkany', 'block.gospodarstwo.rag_rug': 'Chodnik szmaciany',
          'block.gospodarstwo.horn_lantern': 'Latarnia z rogu', 'effect.gospodarstwo.rested': 'Wypoczęty',
          'bed.gospodarstwo.added': 'Łóżko jest wygodniejsze', 'bed.gospodarstwo.cosy': 'Poduszka i pierzyna – wyśpisz się jak u babci',
          'bed.gospodarstwo.has_pillow': 'Na tym łóżku jest już poduszka', 'bed.gospodarstwo.has_featherbed': 'Na tym łóżku jest już pierzyna',
          'item.gospodarstwo.pillow.desc': 'Kucnij i kliknij łóżko'}
    en = {'item.gospodarstwo.wool_cap': 'Wool Cap', 'item.gospodarstwo.wool_sweater': 'Sweater', 'item.gospodarstwo.wool_trousers': 'Wool Trousers',
          'item.gospodarstwo.felt_boots': 'Felt Boots', 'item.gospodarstwo.fur_hat': 'Fur Hat', 'item.gospodarstwo.sheepskin_coat': 'Sheepskin Coat',
          'item.gospodarstwo.linen_shirt': 'Linen Shirt', 'item.gospodarstwo.linen_trousers': 'Linen Trousers',
          'item.gospodarstwo.pillow': 'Pillow', 'item.gospodarstwo.featherbed': 'Featherbed',
          'block.gospodarstwo.curtain': 'Curtain', 'block.gospodarstwo.wool_rug': 'Woven Rug', 'block.gospodarstwo.rag_rug': 'Rag Rug',
          'block.gospodarstwo.horn_lantern': 'Horn Lantern', 'effect.gospodarstwo.rested': 'Rested',
          'bed.gospodarstwo.added': 'The bed is comfier now', 'bed.gospodarstwo.cosy': 'Pillow and featherbed - you will sleep like a baby',
          'bed.gospodarstwo.has_pillow': 'This bed already has a pillow', 'bed.gospodarstwo.has_featherbed': 'This bed already has a featherbed',
          'item.gospodarstwo.pillow.desc': 'Sneak and click a bed'}
    for lang_file, extra in (('pl_pl', pl), ('en_us', en)):
        path = f'{A}/lang/{lang_file}.json'
        d = json.load(open(path, encoding='utf-8'))
        d.update(extra)
        wj(path, d)


# =====================================================================================  stage 4: kitchen
GLASS = {'g': (196, 222, 232), 'h': (244, 252, 255), 'c': (150, 108, 66)}
KITCHEN_ITEMS = {
    # name: (sprite, base colour, extra palette)
    'flour': ('sack', 0xC8AE7E, {'s': (176, 64, 52), 'c': (110, 76, 44)}),
    'butter': ('bar', 0xF2D46A, {}),
    'apple_juice': ('bottle', 0xE8B848, GLASS), 'plum_juice': ('bottle', 0x7A2E6E, GLASS), 'berry_juice': ('bottle', 0xC83A48, GLASS),
    'linseed_oil': ('bottle', 0xD8B640, GLASS), 'vinegar': ('bottle', 0xD8B888, GLASS),
    'sauerkraut': ('jar', 0xD8D49A, {'b': (150, 104, 60), 'c': (214, 214, 220)}),
    'pickled_beets': ('jar', 0x8E1E3A, {'b': (150, 104, 60), 'c': (214, 214, 220)}),
    'pickled_onions': ('jar', 0xE8DCB8, {'b': (150, 104, 60), 'c': (214, 214, 220)}),
    'plum_jam': ('jar', 0x5A1E50, {'b': (196, 60, 52), 'c': (214, 214, 220)}),
    'strawberry_jam': ('jar', 0xC02838, {'b': (236, 236, 236), 'c': (214, 214, 220)}),
    'gooseberry_jam': ('jar', 0x8AA040, {'b': (196, 60, 52), 'c': (214, 214, 220)}),
    'smoked_ham': ('ham', 0xA8503A, {'b': (238, 230, 214)}),
    'smoked_fatback': ('fatback', None, {}),
    'smoked_fish': ('fish', 0xB07A44, {'e': (30, 30, 30)}),
    'smoked_poultry': ('drumstick', 0xA86A36, {'b': (238, 230, 214)}),
    'raw_sausage': ('sausage', 0xD88E84, {'s': (246, 226, 220)}),
    'kielbasa': ('sausage', 0x8E3E2A, {'s': (230, 200, 180)}),
    'raw_pierogi': ('pierogi', 0xEEE4CA, {'c': (214, 202, 172)}),
    'pierogi': ('pierogi', 0xE6C88C, {'c': (196, 150, 84)}),
    'pancakes': ('pancakes', 0xE8C27A, {'y': (250, 226, 120)}),
    'apple_pie': ('pie', None, {}),
    'bigos': ('bowl', 0x7A5030, {'k': (150, 60, 44), 's': (214, 208, 150), 'B': (176, 128, 80), 'b': (134, 92, 52)}),
    'bread_with_butter': ('slice', 0xEFD9A8, {'c': (176, 116, 54), 'y': (250, 222, 110)}),
}
KITCHEN_NAMES = {
    'flour': ('Mąka', 'Flour'), 'butter': ('Masło', 'Butter'), 'apple_juice': ('Sok jabłkowy', 'Apple Juice'),
    'plum_juice': ('Sok śliwkowy', 'Plum Juice'), 'berry_juice': ('Sok owocowy', 'Berry Juice'), 'linseed_oil': ('Olej lniany', 'Linseed Oil'),
    'vinegar': ('Ocet jabłkowy', 'Cider Vinegar'), 'sauerkraut': ('Kiszona kapusta', 'Sauerkraut'), 'pickled_beets': ('Kiszone buraki', 'Pickled Beets'),
    'pickled_onions': ('Marynowana cebulka', 'Pickled Onions'), 'plum_jam': ('Powidła śliwkowe', 'Plum Butter'),
    'strawberry_jam': ('Dżem truskawkowy', 'Strawberry Jam'), 'gooseberry_jam': ('Dżem agrestowy', 'Gooseberry Jam'),
    'smoked_ham': ('Szynka wędzona', 'Smoked Ham'), 'smoked_fatback': ('Słonina wędzona', 'Smoked Fatback'),
    'smoked_fish': ('Wędzona ryba', 'Smoked Fish'), 'smoked_poultry': ('Wędzony drób', 'Smoked Poultry'),
    'raw_sausage': ('Surowa kiełbasa', 'Raw Sausage'), 'kielbasa': ('Kiełbasa wędzona', 'Kielbasa'),
    'raw_pierogi': ('Surowe pierogi', 'Raw Pierogi'), 'pierogi': ('Pierogi', 'Pierogi'), 'pancakes': ('Naleśniki', 'Pancakes'),
    'apple_pie': ('Szarlotka', 'Apple Pie'), 'bigos': ('Bigos', 'Bigos'), 'bread_with_butter': ('Chleb z masłem', 'Bread with Butter'),
}


def stage4():
    T = f'{A}/textures/item'
    for name, (shape, col, extra) in KITCHEN_ITEMS.items():
        if name == 'smoked_fatback':
            pal = SP.ramp((236, 214, 180))
            pal.update({'p': (156, 92, 52), 'P': (120, 64, 34)})
        elif name == 'apple_pie':
            pal = {'o': (70, 44, 24), 'c': (214, 162, 92), 'd': (176, 122, 60), 'f': (236, 206, 120), 'P': (150, 150, 160)}
        else:
            pal = SP.ramp(rgb(col))
        pal.update(extra)
        wi(f'{T}/{name}.png', SP.render(SP.S[shape], pal))
        wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})

    def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
        f = {}
        for face in faces:
            if face in ('up', 'down'):
                uv = [fr[0], fr[2], to[0], to[2]]
            elif face in ('north', 'south'):
                uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
            else:
                uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
            f[face] = {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv]}
        return {'from': fr, 'to': to, 'faces': f}

    disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}

    def ring(x0, y0, z0, x1, y1, z1, t, tex):
        return [el([x0, y0, z0], [x1, y1, z0 + t], tex), el([x0, y0, z1 - t], [x1, y1, z1], tex),
                el([x0, y0, z0 + t], [x0 + t, y1, z1 - t], tex), el([x1 - t, y0, z0 + t], [x1, y1, z1 - t], tex)]

    models = {}
    # quern: wooden platform, lower millstone, spout (the upper stone turns in the renderer)
    models['quern'] = ({'p': 'minecraft:block/spruce_planks', 's': 'minecraft:block/smooth_stone', 'particle': 'minecraft:block/smooth_stone'},
                       [el([1, 0, 1], [15, 3, 15], '#p'), el([0.5, 0, 0.5], [2.5, 3.5, 2.5], '#p'), el([13.5, 0, 0.5], [15.5, 3.5, 2.5], '#p'),
                        el([0.5, 0, 13.5], [2.5, 3.5, 15.5], '#p'), el([13.5, 0, 13.5], [15.5, 3.5, 15.5], '#p'),
                        el([2, 3, 2], [14, 5, 14], '#s'), el([7, 3, 14], [9, 4.5, 15.5], '#s')], 'quern')
    # fruit press: tub with a spout, slatted basket with iron hoops, frame with a beam
    press = [el([1, 0, 1], [15, 1, 15], '#p'), el([7, 1, 15], [9, 2, 16.5], '#p')] + ring(1, 1, 1, 15, 3, 15, 1, '#p')
    for x in (3.5, 6, 8.5, 11):
        press += [el([x, 3, 3.3], [x + 1.4, 10, 4], '#s'), el([x, 3, 12], [x + 1.4, 10, 12.7], '#s')]
        press += [el([3.3, 3, x], [4, 10, x + 1.4], '#s'), el([12, 3, x], [12.7, 10, x + 1.4], '#s')]
    for y in (4.5, 8.2):
        press += ring(3.1, y, 3.1, 12.9, y + 0.8, 12.9, 0.4, '#i')
    press += [el([0, 0, 6.5], [1.5, 16, 9.5], '#l'), el([14.5, 0, 6.5], [16, 16, 9.5], '#l'), el([0, 14, 6.5], [16, 16, 9.5], '#l')]
    models['fruit_press'] = ({'p': 'minecraft:block/spruce_planks', 's': 'minecraft:block/stripped_spruce_log', 'i': 'minecraft:block/anvil',
                              'l': 'minecraft:block/dark_oak_log', 'particle': 'minecraft:block/spruce_planks'}, press, 'fruit_press')
    # crock: stoneware body, brown glazed top and rim, ears, wooden lid
    crock = [el([3, 0, 3], [13, 8, 13], '#w'), el([3, 8, 3], [13, 10.5, 13], '#b'), el([2.95, 0, 2.95], [13.05, 0.8, 13.05], '#b')]
    crock += ring(2.9, 4, 2.9, 13.1, 4.8, 13.1, 0.15, '#u') + ring(2.9, 5.6, 2.9, 13.1, 5.9, 13.1, 0.15, '#u') + ring(2.5, 10.5, 2.5, 13.5, 11.5, 13.5, 0.8, '#b')
    crock += [el([2, 7.5, 7], [3, 9.5, 9], '#b'), el([13, 7.5, 7], [14, 9.5, 9], '#b'), el([3.4, 10.5, 3.4], [12.6, 11, 12.6], '#b'),
              el([4, 11.2, 4], [12, 12.6, 12], '#p'), el([7.5, 12.6, 7.5], [8.5, 13, 8.5], '#p')]
    models['crock'] = ({'w': 'minecraft:block/light_gray_terracotta', 'b': 'minecraft:block/brown_terracotta', 'u': 'minecraft:block/blue_terracotta', 'p': 'minecraft:block/spruce_planks',
                        'particle': 'minecraft:block/light_gray_terracotta'}, crock, 'crock')
    # smokehouse: corner posts, closed back and sides, slatted front, grate floor, roof with a chimney, a bar for hooks
    sm = [el([0, 0, 0], [2, 14, 2], '#l'), el([14, 0, 0], [16, 14, 2], '#l'), el([0, 0, 14], [2, 14, 16], '#l'), el([14, 0, 14], [16, 14, 16], '#l'),
          el([2, 0, 15], [14, 14, 16], '#p'), el([0, 0, 2], [1, 14, 14], '#p'), el([15, 0, 2], [16, 14, 14], '#p')]
    for y in (0.5, 4.5, 8.5):
        sm.append(el([2, y, 0.5], [14, y + 2.5, 1.5], '#p'))
    for x in (2, 5, 8, 11):
        sm.append(el([x, 0, 2], [x + 2, 0.6, 14], '#p'))
    sm += [el([-0.5, 14, -0.5], [16.5, 15, 16.5], '#r'), el([1, 15, 1], [15, 16, 15], '#r'), el([6, 16, 6], [10, 19, 10], '#c'),
           el([2, 12, 7.5], [14, 12.6, 8.5], '#i')]
    models['smokehouse'] = ({'l': 'minecraft:block/dark_oak_log', 'p': 'minecraft:block/spruce_planks', 'r': 'minecraft:block/dark_oak_planks',
                             'c': 'minecraft:block/bricks', 'i': 'minecraft:block/anvil', 'particle': 'minecraft:block/spruce_planks'}, sm, 'smokehouse')
    # butter churn: staves on a wider foot, two iron hoops, a lid
    churn = [el([4, 0, 4], [12, 2, 12], '#p'), el([4.5, 2, 4.5], [11.5, 13, 11.5], '#p'),
             el([4.3, 2.5, 4.3], [11.7, 3.5, 11.7], '#i'), el([4.3, 10, 4.3], [11.7, 11, 11.7], '#i'), el([4.8, 13, 4.8], [11.2, 14, 11.2], '#o')]
    models['butter_churn'] = ({'p': 'minecraft:block/stripped_spruce_log', 'i': 'minecraft:block/anvil', 'o': 'minecraft:block/oak_planks',
                               'particle': 'minecraft:block/stripped_spruce_log'}, churn, 'butter_churn')
    for name, (tex, els, _) in models.items():
        wj(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els, 'display': disp})
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        if name == 'butter_churn':
            wj(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
        else:
            wj(f'{A}/blockstates/{name}.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/{name}', 'y': y} if y else {'model': f'{NS}:block/{name}'})
                                                             for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
        wj(f'{D}/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                                                               'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}]}],
                                                          'random_sequence': f'{NS}:blocks/{name}'})
    wj(f'{D}/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{NS}:quern', f'{NS}:crock', f'{NS}:bonsai_pot', f'{NS}:kettle', f'{NS}:horn_lantern']})
    wj(f'{D}/minecraft/tags/block/mineable/axe.json', {'replace': False, 'values': [f'{NS}:fruit_press', f'{NS}:smokehouse', f'{NS}:butter_churn',
                                                                                    f'{NS}:tanning_rack', f'{NS}:spinning_wheel', f'{NS}:weaving_loom', f'{NS}:dye_vat']})

    G = lambda n: f'{NS}:{n}'
    R = f'{D}/{NS}/recipe'

    def shaped(name, pattern, key, count=1, cat='misc'):
        wj(f'{R}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': cat, 'pattern': pattern, 'key': key,
                                'result': {'id': G(name), 'count': count}})

    def shapeless(name, ingredients, count=1, file=None):
        wj(f'{R}/{file or name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
                                        'result': {'id': G(name), 'count': count}})
    shaped('quern', [' T ', 'SSS', 'PPP'], {'T': 'minecraft:stick', 'S': 'minecraft:smooth_stone', 'P': '#minecraft:wooden_slabs'})
    shaped('fruit_press', ['LIL', 'P P', 'PSP'], {'L': '#minecraft:logs', 'I': 'minecraft:iron_ingot', 'P': '#minecraft:planks',
                                                 'S': '#minecraft:wooden_slabs'})
    shaped('crock', ['T T', 'TTT'], {'T': 'minecraft:terracotta'})
    shaped('smokehouse', ['LPL', 'PBP', 'LPL'], {'L': '#minecraft:logs', 'P': '#minecraft:planks', 'B': 'minecraft:iron_bars'})
    shaped('butter_churn', [' S ', 'P P', 'PIP'], {'S': 'minecraft:stick', 'P': '#minecraft:planks', 'I': 'minecraft:iron_ingot'})
    eggs = ['minecraft:egg', G('duck_egg'), G('goose_egg'), G('green_egg')]
    meats = ['minecraft:porkchop', 'minecraft:beef', 'minecraft:mutton']
    shapeless('raw_sausage', [meats, G('gut'), G('onion')], 3)
    shapeless('raw_pierogi', [G('flour'), G('flour'), [G('sauerkraut'), G('cabbage'), 'minecraft:potato', G('strawberry')], G('onion')], 4)
    shapeless('pancakes', [G('flour'), eggs, 'minecraft:milk_bucket', G('butter')], 4)
    shapeless('apple_pie', ['minecraft:apple', 'minecraft:apple', 'minecraft:sugar', eggs, G('flour'), G('butter')])
    shapeless('bigos', [G('sauerkraut'), G('sauerkraut'), [G('kielbasa'), G('smoked_ham'), 'minecraft:cooked_porkchop', 'minecraft:cooked_beef'],
                        G('onion'), 'minecraft:bowl'])
    shapeless('bread_with_butter', ['minecraft:bread', G('butter')], 2)
    shapeless('plum_jam', [G('plum'), G('plum'), G('plum'), 'minecraft:sugar', 'minecraft:glass_bottle'])
    shapeless('strawberry_jam', [G('strawberry'), G('strawberry'), G('strawberry'), 'minecraft:sugar', 'minecraft:glass_bottle'])
    shapeless('gooseberry_jam', [G('gooseberries'), G('gooseberries'), G('gooseberries'), 'minecraft:sugar', 'minecraft:glass_bottle'])
    shapeless('flour', ['minecraft:wheat', 'minecraft:wheat'], 1, file='flour_by_hand')
    for kind in ('smelting', 'smoking', 'campfire_cooking'):
        wj(f'{R}/pierogi_{kind}.json', {'type': f'minecraft:{kind}', 'category': 'food', 'ingredient': G('raw_pierogi'), 'result': {'id': G('pierogi')},
                                        'experience': 0.3, 'cookingtime': {'smelting': 200, 'smoking': 100, 'campfire_cooking': 600}[kind]})
    wj(f'{R}/bread_from_flour.json', {'type': 'minecraft:smelting', 'category': 'food', 'ingredient': G('flour'), 'result': {'id': 'minecraft:bread'},
                                      'experience': 0.2, 'cookingtime': 200})

    pl = {f'item.{NS}.{k}': v[0] for k, v in KITCHEN_NAMES.items()}
    en = {f'item.{NS}.{k}': v[1] for k, v in KITCHEN_NAMES.items()}
    pl.update({'block.gospodarstwo.quern': 'Żarna', 'block.gospodarstwo.fruit_press': 'Prasa do owoców', 'block.gospodarstwo.crock': 'Kamionkowy garnek',
               'block.gospodarstwo.smokehouse': 'Wędzarnia', 'block.gospodarstwo.butter_churn': 'Maselnica',
               'quern.gospodarstwo.empty': 'Żarna są puste – wsyp zboże, trzcinę cukrową albo kości',
               'press.gospodarstwo.empty': 'Prasa jest pusta – włóż jabłka, śliwki, owoce albo siemię lniane',
               'press.gospodarstwo.need_bottle': 'Sok nalejesz do szklanej butelki',
               'press.gospodarstwo.no_juice': 'W prasie nie ma jeszcze soku',
               'crock.gospodarstwo.empty': 'Garnek jest pusty – włóż kapustę, buraki, cebulę albo sok jabłkowy',
               'smokehouse.gospodarstwo.empty': 'Wędzarnia jest pusta – zawieś mięso, słoninę, ryby albo kiełbasę',
               'smokehouse.gospodarstwo.cold': 'Pod wędzarnią musi się palić ognisko',
               'churn.gospodarstwo.empty': 'Wlej wiadro mleka, potem ubijaj (PPM) aż powstanie masło'})
    en.update({'block.gospodarstwo.quern': 'Quern', 'block.gospodarstwo.fruit_press': 'Fruit Press', 'block.gospodarstwo.crock': 'Fermenting Crock',
               'block.gospodarstwo.smokehouse': 'Smokehouse', 'block.gospodarstwo.butter_churn': 'Butter Churn',
               'quern.gospodarstwo.empty': 'The quern is empty - put in grain, sugar cane or bones',
               'press.gospodarstwo.empty': 'The press is empty - put in apples, plums, berries or flax seeds',
               'press.gospodarstwo.need_bottle': 'Fill a glass bottle with the juice', 'press.gospodarstwo.no_juice': 'No juice yet',
               'crock.gospodarstwo.empty': 'The crock is empty - put in cabbage, beetroots, onions or apple juice',
               'smokehouse.gospodarstwo.empty': 'The smokehouse is empty - hang meat, fatback, fish or sausages',
               'smokehouse.gospodarstwo.cold': 'A campfire must burn under the smokehouse',
               'churn.gospodarstwo.empty': 'Pour in a bucket of milk, then churn (right-click) until it turns into butter'})
    for lang_file, extra in (('pl_pl', pl), ('en_us', en)):
        path = f'{A}/lang/{lang_file}.json'
        d = json.load(open(path, encoding='utf-8'))
        d.update(extra)
        wj(path, d)


# =====================================================================================  stage 5: tools and farm equipment
def stage5():
    T = f'{A}/textures/item'
    METAL = {'b': (196, 200, 206), 'h': (240, 244, 248), 'w': (128, 86, 50), 'o': (40, 34, 30)}
    wi(f'{T}/watering_can.png', SP.render(SP.S['watering_can'], SP.ramp((200, 116, 74))))
    wi(f'{T}/scythe.png', SP.render(SP.S['scythe'], METAL))
    wi(f'{T}/sickle.png', SP.render(SP.S['sickle'], METAL))
    horn = SP.render(SP.S['horn'], SP.ramp((222, 206, 166)))
    SP.spots(horn, (120, 78, 44), [(10, 5), (11, 5), (9, 6), (10, 6), (5, 9), (6, 9), (4, 10), (5, 10)])
    wi(f'{T}/hunting_horn.png', horn)
    war = SP.render(SP.S['horn'], SP.ramp((98, 86, 74)))
    SP.spots(war, (232, 190, 70), [(10, 5), (11, 5), (9, 6), (10, 6), (5, 9), (6, 9), (4, 10), (5, 10), (3, 11), (4, 11)])
    wi(f'{T}/war_horn.png', war)
    for n, hand in (('watering_can', False), ('scythe', True), ('sickle', True), ('hunting_horn', False), ('war_horn', False)):
        wj(f'{A}/models/item/{n}.json', {'parent': 'minecraft:item/handheld' if hand else 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})
        wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{n}'}})

    # fishing net texture: a diamond mesh of tarred rope
    img = Image.new('RGBA', (16, 16))
    px_ = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                knot = (x + y) % 4 == 0 and (x - y) % 4 == 0
                px_[x, y] = (92, 70, 46, 255) if knot else (128, 102, 70, 255)
    wi(f'{A}/textures/block/fishing_net.png', img)

    def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), light=None):
        f = {}
        for face in faces:
            if face in ('up', 'down'):
                uv = [fr[0], fr[2], to[0], to[2]]
            elif face in ('north', 'south'):
                uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
            else:
                uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
            f[face] = {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv]}
        e = {'from': fr, 'to': to, 'faces': f}
        if light:
            e['light_emission'] = light
        return e

    def ring(x0, y0, z0, x1, y1, z1, t, tex):
        return [el([x0, y0, z0], [x1, y1, z0 + t], tex), el([x0, y0, z1 - t], [x1, y1, z1], tex),
                el([x0, y0, z0 + t], [x0 + t, y1, z1 - t], tex), el([x1 - t, y0, z0 + t], [x1, y1, z1 - t], tex)]
    disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}
    inc = [el([1, 0, 1], [15, 1, 15], '#p')] + ring(1, 1, 1, 15, 5, 15, 1, '#p') + [
        el([2, 1, 2], [14, 4.2, 14], '#h'),
        el([12.5, 5, 12.5], [13.5, 12, 13.5], '#i'), el([7.5, 11, 12.5], [13.5, 12, 13.5], '#i'), el([7.5, 11, 7.5], [8.5, 12, 12.5], '#i'),
        el([6, 9.8, 6], [10, 11, 10], '#c'), el([7, 9, 7], [9, 9.8, 9], '#g', light=15)]
    net = [el([0, 0, 7], [1.5, 16, 9], '#l'), el([14.5, 0, 7], [16, 16, 9], '#l'), el([1.5, 14.5, 7.75], [14.5, 15, 8.25], '#r'),
           el([1.5, 0, 8], [14.5, 14.5, 8], '#n', faces=('north', 'south'))]
    for x in (3, 7.25, 11.5):
        net.append(el([x, 14, 7.3], [x + 1.5, 15.5, 8.7], '#k'))
    bed = [el([2, 0, 2], [14, 15, 14], '#d', faces=('up',))] + ring(0, 0, 0, 16, 16, 16, 2, '#p') + [el([2, 0, 2], [14, 0.01, 14], '#p', faces=('down',))]
    bed[0]['faces']['up']['texture'] = '#f'
    models = {
        'incubator': ({'p': 'minecraft:block/spruce_planks', 'h': 'minecraft:block/hay_block_top', 'i': 'minecraft:block/anvil',
                       'c': 'minecraft:block/copper_block', 'g': 'minecraft:block/glowstone', 'particle': 'minecraft:block/spruce_planks'}, inc),
        'fishing_net': ({'l': 'minecraft:block/stripped_spruce_log', 'r': 'minecraft:block/brown_wool', 'n': f'{NS}:block/fishing_net',
                         'k': 'minecraft:block/orange_terracotta', 'particle': 'minecraft:block/stripped_spruce_log'}, net),
        'raised_bed': ({'p': 'minecraft:block/oak_planks', 'f': 'minecraft:block/farmland_moist', 'd': 'minecraft:block/dirt',
                        'particle': 'minecraft:block/oak_planks'}, bed),
    }
    for name, (tex, els) in models.items():
        m = {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els, 'display': disp}
        if name == 'fishing_net':
            m['render_type'] = 'minecraft:cutout'
            m['ambientocclusion'] = False
        wj(f'{A}/models/block/{name}.json', m)
        wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        wj(f'{D}/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                                                                                               'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}]}],
                                                          'random_sequence': f'{NS}:blocks/{name}'})
    wj(f'{A}/blockstates/incubator.json', {'variants': {'': {'model': f'{NS}:block/incubator'}}})
    wj(f'{A}/blockstates/raised_bed.json', {'variants': {'': {'model': f'{NS}:block/raised_bed'}}})
    wj(f'{A}/blockstates/fishing_net.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/fishing_net', 'y': y} if y else {'model': f'{NS}:block/fishing_net'})
                                                          for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
    G = lambda n: f'{NS}:{n}'
    R = f'{D}/{NS}/recipe'

    def shaped(name, pattern, key, count=1, cat='equipment'):
        wj(f'{R}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': cat, 'pattern': pattern, 'key': key,
                                'result': {'id': G(name), 'count': count}})
    shaped('watering_can', ['C  ', 'CBC', ' C '], {'C': 'minecraft:copper_ingot', 'B': 'minecraft:bucket'})
    shaped('scythe', ['III', '  S', '  S'], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
    shaped('sickle', ['II', ' I', 'S '], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
    shaped('hunting_horn', ['H', 'L'], {'H': G('horn'), 'L': 'minecraft:leather'})
    shaped('war_horn', ['GHG', ' L '], {'H': G('ravager_horn'), 'G': 'minecraft:gold_ingot', 'L': 'minecraft:leather'})
    shaped('incubator', ['ILI', 'DHD', 'PPP'], {'I': 'minecraft:iron_nugget', 'L': 'minecraft:lantern', 'D': G('down'), 'H': 'minecraft:hay_block',
                                               'P': '#minecraft:planks'}, cat='misc')
    shaped('fishing_net', ['SNS', 'NNN', 'SNS'], {'S': 'minecraft:stick', 'N': [G('net_scrap'), G('horse_hair'), 'minecraft:string']}, cat='misc')
    shaped('raised_bed', ['P P', 'PDP'], {'P': '#minecraft:planks', 'D': 'minecraft:dirt'}, count=2, cat='building')
    wj(f'{D}/minecraft/tags/block/mineable/axe.json', {'replace': False, 'values': [G(n) for n in (
        'fruit_press', 'smokehouse', 'butter_churn', 'tanning_rack', 'spinning_wheel', 'weaving_loom', 'dye_vat', 'incubator', 'fishing_net', 'raised_bed')]})
    wj(f'{D}/minecraft/tags/item/enchantable/durability.json', {'replace': False, 'values': [G('scythe'), G('sickle'), G('hunting_horn'), G('war_horn')]})

    pl = {'item.gospodarstwo.watering_can': 'Konewka', 'item.gospodarstwo.scythe': 'Kosa', 'item.gospodarstwo.sickle': 'Sierp',
          'item.gospodarstwo.hunting_horn': 'Róg myśliwski', 'item.gospodarstwo.war_horn': 'Róg bojowy',
          'block.gospodarstwo.incubator': 'Wylęgarnia', 'block.gospodarstwo.fishing_net': 'Sieć rybacka', 'block.gospodarstwo.raised_bed': 'Grządka podniesiona',
          'can.gospodarstwo.empty': 'Konewka jest pusta – nabierz wody (PPM na wodę)',
          'incubator.gospodarstwo.empty': 'Wylęgarnia jest pusta – włóż do 4 jaj (kurze, kacze, gęsie, zielone)',
          'incubator.gospodarstwo.other': 'W wylęgarni są już inne jaja',
          'incubator.gospodarstwo.status': 'Jaja wysiadują się: %s%%',
          'net.gospodarstwo.empty': 'Na razie nic się nie złapało', 'net.gospodarstwo.no_water': 'Sieć potrzebuje otwartej wody dookoła'}
    en = {'item.gospodarstwo.watering_can': 'Watering Can', 'item.gospodarstwo.scythe': 'Scythe', 'item.gospodarstwo.sickle': 'Sickle',
          'item.gospodarstwo.hunting_horn': 'Hunting Horn', 'item.gospodarstwo.war_horn': 'War Horn',
          'block.gospodarstwo.incubator': 'Incubator', 'block.gospodarstwo.fishing_net': 'Fishing Net', 'block.gospodarstwo.raised_bed': 'Raised Bed',
          'can.gospodarstwo.empty': 'The watering can is empty - fill it from water',
          'incubator.gospodarstwo.empty': 'The incubator is empty - put in up to 4 eggs (chicken, duck, goose, green)',
          'incubator.gospodarstwo.other': 'There are other eggs in the incubator', 'incubator.gospodarstwo.status': 'Incubating: %s%%',
          'net.gospodarstwo.empty': 'Nothing caught yet', 'net.gospodarstwo.no_water': 'The net needs open water around it'}
    for lang_file, extra in (('pl_pl', pl), ('en_us', en)):
        path = f'{A}/lang/{lang_file}.json'
        d = json.load(open(path, encoding='utf-8'))
        d.update(extra)
        wj(path, d)


if __name__ == '__main__':
    animal_assets()
    item_textures()
    block_textures()
    models()
    data()
    lang()
    textiles()
    stage2()
    stage3()
    stage4()
    stage5()
