#!/usr/bin/env python3
"""Generates the assets of Żywiołaki (elementals): GeckoLib models + animations, element textures with glow masks,
spawn egg items, overlays. Models are defined once here; the mod's fallback renderer (no GeckoLib) reads the same geo.json."""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'elementals', 'resources', 'assets', 'elementals')

ELEMENTS = {
    'none': (0xbd, 0xb7, 0xa6),
    'fire': (0xe8, 0x57, 0x2a),
    'ice': (0x7c, 0xc6, 0xe4),
    'dark': (0x8b, 0x5c, 0xf6),
    'electric': (0xe9, 0xc3, 0x2a),
    'dragon': (0x2f, 0x9e, 0x78),
}


def wjson(rel, obj):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)


def wimg(rel, img):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# ---------------------------------------------------------------- creatures
# cube: (bone, origin xyz, size xyz, role)  role picks the paint: body/belly/head/leg/wing/tail/moss/crest/ear/beak/horn
# bones: name -> (parent, pivot, rotation)
CREATURES = {
    'mossling': {
        'colors': {'main': (0x6b, 0x7d, 0x4a), 'dark': (0x4a, 0x56, 0x34), 'belly': (0xa8, 0x9a, 0x76), 'accent': (0x8f, 0xb8, 0x4c),
                   'eye': (0x20, 0x1a, 0x14)},
        'bones': {
            'body': (None, (0, 6, 0), (0, 0, 0)),
            'moss': ('body', (0, 10, 0), (0, 0, 0)),
            'head': ('body', (0, 7, -7), (0, 0, 0)),
            'leg_fl': ('body', (3.5, 4, -4.5), (0, 0, 0)),
            'leg_fr': ('body', (-3.5, 4, -4.5), (0, 0, 0)),
            'leg_bl': ('body', (3.5, 4, 4.5), (0, 0, 0)),
            'leg_br': ('body', (-3.5, 4, 4.5), (0, 0, 0)),
            'tail': ('body', (0, 6, 7), (-20, 0, 0)),
        },
        'cubes': [
            ('body', (-5, 3, -7), (10, 7, 14), 'body'),
            ('moss', (-4, 10, -6), (8, 2, 12), 'moss'),
            ('moss', (-2, 12, -3), (4, 1, 6), 'moss'),
            ('head', (-3, 4, -12), (6, 5, 5), 'head'),
            ('head', (-4, 8, -10), (2, 2, 1), 'ear'),
            ('head', (2, 8, -10), (2, 2, 1), 'ear'),
            ('leg_fl', (2, 0, -6), (3, 4, 3), 'leg'),
            ('leg_fr', (-5, 0, -6), (3, 4, 3), 'leg'),
            ('leg_bl', (2, 0, 3), (3, 4, 3), 'leg'),
            ('leg_br', (-5, 0, 3), (3, 4, 3), 'leg'),
            ('tail', (-1, 4, 7), (2, 2, 4), 'tail'),
        ],
        'eyes': 'head',
    },
    'puffowl': {
        'colors': {'main': (0xc9, 0xa8, 0x7a), 'dark': (0x8a, 0x6a, 0x48), 'belly': (0xf1, 0xe3, 0xc8), 'accent': (0xe0, 0x9a, 0x3a),
                   'eye': (0x18, 0x14, 0x10)},
        'bones': {
            'body': (None, (0, 0, 0), (0, 0, 0)),
            'head': ('body', (0, 9, 0), (0, 0, 0)),
            'wing_left': ('body', (4, 8, 0), (0, 0, 0)),
            'wing_right': ('body', (-4, 8, 0), (0, 0, 0)),
            'leg_left': ('body', (2, 1, 0), (0, 0, 0)),
            'leg_right': ('body', (-2, 1, 0), (0, 0, 0)),
        },
        'cubes': [
            ('body', (-4, 1, -4), (8, 8, 8), 'body'),
            ('head', (-3.5, 9, -3.5), (7, 5, 7), 'head'),
            ('head', (-3.5, 14, -1), (2, 2, 1), 'ear'),
            ('head', (1.5, 14, -1), (2, 2, 1), 'ear'),
            ('head', (-1, 10, -4.5), (2, 2, 1), 'beak'),
            ('wing_left', (4, 3, -3), (1, 6, 6), 'wing'),
            ('wing_right', (-5, 3, -3), (1, 6, 6), 'wing'),
            ('leg_left', (1, 0, -2), (2, 1, 3), 'beak'),
            ('leg_right', (-3, 0, -2), (2, 1, 3), 'beak'),
        ],
        'eyes': 'head',
    },
    'emberlizard': {
        'colors': {'main': (0x9c, 0x3a, 0x22), 'dark': (0x5a, 0x22, 0x18), 'belly': (0xe8, 0x9a, 0x4a), 'accent': (0xff, 0xc2, 0x3a),
                   'eye': (0xff, 0xe0, 0x60)},
        'bones': {
            'body': (None, (0, 5, 0), (0, 0, 0)),
            'head': ('body', (0, 4.5, -7), (0, 0, 0)),
            'tail': ('body', (0, 4, 6), (0, 0, 0)),
            'tail_tip': ('tail', (0, 4, 13), (0, 0, 0)),
            'leg_fl': ('body', (3, 3, -4), (0, 0, 0)),
            'leg_fr': ('body', (-3, 3, -4), (0, 0, 0)),
            'leg_bl': ('body', (3, 3, 4), (0, 0, 0)),
            'leg_br': ('body', (-3, 3, 4), (0, 0, 0)),
        },
        'cubes': [
            ('body', (-3, 2, -8), (6, 4, 14), 'body'),
            ('body', (-1, 6, -7), (2, 2, 12), 'crest'),
            ('head', (-2.5, 2, -13), (5, 4, 6), 'head'),
            ('head', (-2, 6, -10), (1, 2, 1), 'horn'),
            ('head', (1, 6, -10), (1, 2, 1), 'horn'),
            ('tail', (-1.5, 2.5, 6), (3, 3, 7), 'tail'),
            ('tail_tip', (-1, 3, 13), (2, 2, 6), 'tail'),
            ('leg_fl', (3, 0, -5), (3, 3, 2), 'leg'),
            ('leg_fr', (-6, 0, -5), (3, 3, 2), 'leg'),
            ('leg_bl', (3, 0, 3), (3, 3, 2), 'leg'),
            ('leg_br', (-6, 0, 3), (3, 3, 2), 'leg'),
        ],
        'eyes': 'head',
    },
    'frostling': {
        'colors': {'main': (0xc8, 0xe4, 0xf2), 'dark': (0x7a, 0xa8, 0xc8), 'belly': (0xf4, 0xfb, 0xff), 'accent': (0x9f, 0xe6, 0xff),
                   'eye': (0x1c, 0x3a, 0x6a)},
        'bones': {
            'body': (None, (0, 10, 0), (0, 0, 0)),
            'head': ('body', (0, 18, 0), (0, 0, 0)),
            'arm_left': ('body', (4, 17, 0), (0, 0, -10)),
            'arm_right': ('body', (-4, 17, 0), (0, 0, 10)),
            'wisp': ('body', (0, 8, 0), (0, 0, 0)),
        },
        'cubes': [
            ('body', (-3, 8, -2), (6, 10, 4), 'body'),
            ('head', (-3.5, 18, -3.5), (7, 7, 7), 'head'),
            ('head', (-3, 25, -1), (2, 3, 2), 'horn'),
            ('head', (1, 25, -1), (2, 3, 2), 'horn'),
            ('head', (-1, 25, 0), (2, 4, 2), 'horn'),
            ('arm_left', (4, 10, -1), (2, 7, 2), 'leg'),
            ('arm_right', (-6, 10, -1), (2, 7, 2), 'leg'),
            ('wisp', (-2, 3, -1.5), (4, 5, 3), 'tail'),
            ('wisp', (-1, 0, -1), (2, 3, 2), 'tail'),
        ],
        'eyes': 'head',
    },
}


def pack(cubes, tw=64):
    """Shelf packer for box UVs. Returns list of (u, v) and texture height."""
    order = sorted(range(len(cubes)), key=lambda i: -(math.ceil(cubes[i][2][2]) + math.ceil(cubes[i][2][1])))
    uvs = [None] * len(cubes)
    x = y = shelf = 0
    for i in order:
        sx, sy, sz = (math.ceil(s) for s in cubes[i][2])
        w, h = 2 * (sx + sz), sz + sy
        if x + w > tw:
            x, y, shelf = 0, y + shelf, 0
        uvs[i] = (x, y)
        x += w
        shelf = max(shelf, h)
    th = 32
    while th < y + shelf:
        th *= 2
    return uvs, th


def faces(u, v, s):
    sx, sy, sz = (math.ceil(a) for a in s)
    return {
        'up': (u + sz, v, sx, sz), 'down': (u + sz + sx, v, sx, sz),
        'north': (u + sz, v + sz, sx, sy), 'south': (u + 2 * sz + sx, v + sz, sx, sy),
        'east': (u, v + sz, sz, sy), 'west': (u + sz + sx, v + sz, sz, sy),
    }


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(x * k))) for x in c)


def palette(base, element):
    if element == 'none':
        return dict(base)
    e = ELEMENTS[element]
    p = {k: mix(v, e, 0.55 if k in ('main', 'dark') else 0.35) for k, v in base.items() if k != 'eye'}
    p['dark'] = shade(mix(base['dark'], e, 0.5), 0.75)
    p['eye'] = {'fire': (0xff, 0xd0, 0x40), 'ice': (0xd8, 0xf6, 0xff), 'dark': (0xd0, 0xb0, 0xff),
                'electric': (0xff, 0xf4, 0x9a), 'dragon': (0x9a, 0xff, 0xc8)}[element]
    if element == 'dark':
        p['main'] = shade(p['main'], 0.6)
        p['belly'] = shade(p['belly'], 0.7)
    return p


def paint(name, spec, uvs, th, element):
    rnd = random.Random(name + ':' + element)
    img = Image.new('RGBA', (64, th), (0, 0, 0, 0))
    glow = Image.new('RGBA', (64, th), (0, 0, 0, 0))
    px, gx = img.load(), glow.load()
    pal = palette(spec['colors'], element)
    ecol = ELEMENTS[element]

    def put(x, y, c, glowing=False):
        if 0 <= x < 64 and 0 <= y < th:
            px[x, y] = c + (255,)
            if glowing:
                gx[x, y] = c + (255,)

    for ci, (bone, origin, size, role) in enumerate(spec['cubes']):
        u, v = uvs[ci]
        for fname, (fu, fv, fw, fh) in faces(u, v, size).items():
            for yy in range(fh):
                for xx in range(fw):
                    n = rnd.uniform(0.9, 1.08)
                    if role in ('body', 'head', 'tail', 'leg', 'wing'):
                        c = pal['main']
                        if fname == 'down' or (fname in ('north', 'south', 'east', 'west') and yy >= fh - max(1, fh // 3) and role == 'body'):
                            c = pal['belly']
                        if role == 'leg' and yy >= fh - 1:
                            c = pal['dark']
                        if role == 'wing':
                            c = pal['dark'] if (xx + yy) % 3 == 0 else pal['main']
                    elif role == 'moss':
                        c = pal['accent'] if rnd.random() < 0.55 else shade(pal['accent'], 0.75)
                    elif role in ('crest', 'horn'):
                        c = pal['accent'] if yy < fh / 2 else pal['dark']
                    elif role == 'ear':
                        c = pal['dark']
                    elif role == 'beak':
                        c = pal['accent']
                    else:
                        c = pal['main']
                    c = shade(c, n)
                    glowing = False
                    # element patterns
                    if element == 'fire' and role in ('body', 'tail', 'crest') and fname in ('up', 'east', 'west') and rnd.random() < 0.12:
                        c, glowing = mix(ecol, (255, 220, 90), rnd.random() * 0.6), True
                    if element == 'ice' and fname == 'up' and rnd.random() < 0.35:
                        c, glowing = mix((230, 250, 255), ecol, rnd.random() * 0.4), rnd.random() < 0.4
                    if element == 'dark' and role in ('body', 'head', 'wing', 'tail') and rnd.random() < 0.04:
                        c, glowing = (0xd6, 0xc2, 0xff), True
                    if element == 'electric' and role in ('body', 'tail') and fname in ('east', 'west', 'up') and (xx + 2 * (yy % 2)) % 6 == 0:
                        c, glowing = (0xff, 0xf2, 0x7a), True
                    if element == 'dragon' and role in ('body', 'tail', 'head') and (xx % 3 == 0 and yy % 2 == 0):
                        c = shade(c, 0.78)
                    if element == 'dragon' and role in ('crest', 'horn'):
                        c, glowing = mix(ecol, (180, 255, 220), 0.5), True
                    if element != 'none' and role in ('crest', 'horn') and element in ('fire', 'ice', 'electric'):
                        glowing = True
                    put(fu + xx, fv + yy, c, glowing)
        # eyes on the front of the head
        if bone == spec['eyes'] and role == 'head':
            fu, fv, fw, fh = faces(u, v, size)['north']
            ey = fv + max(1, fh // 3)
            for ex in (fu + 1, fu + fw - 2):
                put(ex, ey, pal['eye'], element != 'none')
                put(ex, ey + 1, shade(pal['eye'], 0.8), element != 'none')
    return img, glow


def geo(name, spec, uvs, th):
    bones = []
    for bname, (parent, pivot, rot) in spec['bones'].items():
        b = {'name': bname, 'pivot': list(pivot)}
        if parent:
            b['parent'] = parent
        if any(rot):
            b['rotation'] = list(rot)
        cubes = [{'origin': list(o), 'size': list(s), 'uv': list(uvs[i])}
                 for i, (bone, o, s, _) in enumerate(spec['cubes']) if bone == bname]
        if cubes:
            b['cubes'] = cubes
        bones.append(b)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.' + name, 'texture_width': 64, 'texture_height': th,
                        'visible_bounds_width': 3, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones}]}


def kf(values, length):
    """values: list of (t, [x,y,z]) -> GeckoLib keyframes."""
    return {f'{t:.4g}': {'vector': list(vec), 'easing': 'easeInOutSine'} for t, vec in values}


def swing(amp, length, phase=0.0, axis=0, n=4):
    out = []
    for i in range(n + 1):
        t = length * i / n
        a = amp * math.sin(2 * math.pi * (i / n) + phase)
        vec = [0, 0, 0]
        vec[axis] = round(a, 2)
        out.append((t, vec))
    return out


def animations(name, spec):
    bones = spec['bones']
    idle, walk = {}, {}
    L = 2.0
    W = 0.8
    if 'head' in bones:
        idle['head'] = {'rotation': kf(swing(4, L, 0, 0), L)}
        walk['head'] = {'rotation': kf(swing(3, W, 0.5, 0), W)}
    if 'body' in bones:
        if name == 'frostling':
            idle['body'] = {'position': kf([(t, [0, v[0], 0]) for t, v in swing(1.2, L, 0, 0)], L)}
            walk['body'] = {'position': kf([(t, [0, v[0], 0]) for t, v in swing(1.0, W, 0, 0)], W),
                            'rotation': kf([(0, [8, 0, 0]), (W, [8, 0, 0])], W)}
        else:
            idle['body'] = {'scale': kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.03, 1.02]), (L, [1, 1, 1])], L)}
            walk['body'] = {'position': kf([(t, [0, abs(v[0]) * 0.5, 0]) for t, v in swing(1, W, 0, 0)], W)}
    for leg in ('leg_fl', 'leg_br'):
        if leg in bones:
            walk[leg] = {'rotation': kf(swing(30, W, 0, 0), W)}
    for leg in ('leg_fr', 'leg_bl'):
        if leg in bones:
            walk[leg] = {'rotation': kf(swing(30, W, math.pi, 0), W)}
    if 'leg_left' in bones:
        walk['leg_left'] = {'rotation': kf(swing(25, W, 0, 0), W)}
        walk['leg_right'] = {'rotation': kf(swing(25, W, math.pi, 0), W)}
    if 'wing_left' in bones:
        idle['wing_left'] = {'rotation': kf(swing(4, L, 0, 2), L)}
        idle['wing_right'] = {'rotation': kf(swing(-4, L, 0, 2), L)}
        walk['wing_left'] = {'rotation': kf(swing(-25, W / 2, 0, 2), W / 2)}
        walk['wing_right'] = {'rotation': kf(swing(25, W / 2, 0, 2), W / 2)}
    if 'arm_left' in bones:
        idle['arm_left'] = {'rotation': kf(swing(6, L, 0, 2), L)}
        idle['arm_right'] = {'rotation': kf(swing(-6, L, 0, 2), L)}
        walk['arm_left'] = {'rotation': kf(swing(20, W, 0, 0), W)}
        walk['arm_right'] = {'rotation': kf(swing(20, W, math.pi, 0), W)}
    for t in ('tail', 'wisp'):
        if t in bones:
            idle[t] = {'rotation': kf(swing(8, L, 0, 1), L)}
            walk[t] = {'rotation': kf(swing(14, W, 0, 1), W)}
    if 'tail_tip' in bones:
        idle['tail_tip'] = {'rotation': kf(swing(10, L, 1, 1), L)}
        walk['tail_tip'] = {'rotation': kf(swing(18, W, 1, 1), W)}
    return {'format_version': '1.8.0', 'animations': {
        'idle': {'loop': True, 'animation_length': L, 'bones': idle},
        'walk': {'loop': True, 'animation_length': W, 'bones': walk}}}


def egg(name, spec):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    p = img.load()
    a, b = spec['colors']['main'], spec['colors']['accent']
    rnd = random.Random(name)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.5, (y - 8.5) / 7.0
            if dx * dx + dy * dy <= 1:
                c = b if rnd.random() < 0.22 else a
                if dx * dx + dy * dy > 0.75:
                    c = shade(c, 0.7)
                p[x, y] = c + (255,)
    return img


def main():
    for name, spec in CREATURES.items():
        uvs, th = pack(spec['cubes'])
        wjson(f'geckolib/models/entity/{name}.geo.json', geo(name, spec, uvs, th))
        wjson(f'geckolib/animations/entity/{name}.animation.json', animations(name, spec))
        for el in ELEMENTS:
            img, glow = paint(name, spec, uvs, th, el)
            wimg(f'textures/entity/{name}_{el}.png', img)
            wimg(f'textures/entity/{name}_{el}_glowmask.png', glow)
        wimg(f'textures/item/{name}_spawn_egg.png', egg(name, spec))
        wjson(f'items/{name}_spawn_egg.json', {'model': {'type': 'minecraft:model', 'model': f'elementals:item/{name}_spawn_egg'}})
        wjson(f'models/item/{name}_spawn_egg.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'elementals:item/{name}_spawn_egg'}})
    # overlays for elements on every other creature
    wimg('textures/misc/white.png', Image.new('RGBA', (16, 16), (255, 255, 255, 255)))
    sp = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    pp = sp.load()
    rnd = random.Random(7)
    for _ in range(70):
        x, y = rnd.randrange(64), rnd.randrange(64)
        pp[x, y] = (255, 255, 255, 255)
    wimg('textures/misc/speckles.png', sp)


if __name__ == '__main__':
    main()
