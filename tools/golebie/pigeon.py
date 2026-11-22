"""Pigeon for GeckoLib / Blockbench: geometry, box-UV texture and animations, plus a software preview renderer.
Built at double resolution (1 unit = half a vanilla pixel); render it at about 0.35-0.4 scale.
Front is -Z, up is +Y, the bird's right side is -X."""
import json, math, random, sys
from PIL import Image, ImageDraw

sys.path.insert(0, __import__('os').path.join(__import__('os').path.dirname(__import__('os').path.abspath(__file__)), '..'))
from goblin_gecko import anim, keys, TAU  # noqa: E402

S = math.sin
TEX_W, TEX_H = 128, 128

# ------------------------------------------------------------------------------------------- palette (from the photo)
P = dict(
    head=(118, 126, 142), head2=(104, 112, 128), crown=(126, 134, 150),
    neck=(50, 54, 62), neck_g=(58, 112, 92), neck_p=(104, 78, 120), neck_w=(96, 104, 112),
    breast=(56, 58, 66), breast2=(66, 68, 78), belly=(150, 154, 166),
    wing=(168, 172, 182), wing2=(150, 154, 166), bar=(40, 42, 48), scap=(150, 154, 166),
    prim=(66, 68, 78), prim2=(48, 50, 58),
    tail=(110, 114, 126), band=(36, 36, 42), rump=(160, 164, 174),
    beak=(52, 50, 54), beak2=(40, 38, 42), cere=(236, 218, 210), cere2=(214, 192, 186),
    ring=(176, 176, 172), iris=(222, 118, 44), pupil=(22, 14, 10),
    leg=(206, 96, 92), leg2=(170, 70, 72), claw=(38, 36, 40), thigh=(130, 134, 146),
    diaper=(0, 0, 0),
    ring_band=(240, 196, 40), ring_band2=(200, 156, 24),
)


class Cube:
    def __init__(self, o, s, mat, deco=None, inflate=0.0, rot=None, pivot=None):
        self.o, self.s, self.mat, self.deco, self.inflate, self.rot, self.pivot = o, s, mat, deco, inflate, rot, pivot
        self.uv = None


class Bone:
    def __init__(self, name, pivot, parent=None, rot=None):
        self.name, self.pivot, self.parent, self.rot, self.cubes = name, pivot, parent, rot or [0, 0, 0], []

    def add(self, *c):
        self.cubes += c
        return self


def mirror(c):
    o, s = c.o, c.s
    return Cube([-(o[0] + s[0]), o[1], o[2]], list(s), c.mat, c.deco, c.inflate,
                None if c.rot is None else [c.rot[0], -c.rot[1], -c.rot[2]],
                None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]])


def build():
    B = []

    def bone(*a, **k):
        b = Bone(*a, **k)
        B.append(b)
        return b

    root = bone('root', [0, 0, 0])
    # ---- body: tilted, front up
    body = bone('body', [0, 8, 1], 'root', rot=[-20, 0, 0])
    body.add(Cube([-3.5, 6.5, -5], [7, 7, 12], 'breast', 'body'))
    body.add(Cube([-3, 6.8, -6.5], [6, 6, 2], 'breast', 'chest', inflate=0.3))            # puffed breast
    body.add(Cube([-3, 6, -3], [6, 1, 8], 'belly', 'belly', inflate=0.2))                 # belly
    body.add(Cube([-3, 9, 6], [6, 4, 3], 'rump', 'rump'))                                 # rump
    body.add(Cube([-2.5, 7.5, 7], [5, 2, 4], 'belly', 'vent'))                             # under-tail coverts
    # ---- neck and head
    neck = bone('neck', [0, 12.5, -3.5], 'body', rot=[20, 0, 0])
    neck.add(Cube([-2.5, 11.5, -6.5], [5, 6, 4], 'neck', 'neck', inflate=0.2))
    neck.add(Cube([-2, 13, -7], [4, 4, 1], 'neck', 'throat'))                               # throat
    head = bone('head', [0, 17.5, -4.5], 'neck')
    head.add(Cube([-2.25, 17, -7.5], [4.5, 4.5, 5], 'head', 'head'))
    head.add(Cube([-1.75, 21.5, -7], [3.5, 0.6, 4], 'crown', None))                        # rounded crown
    head.add(Cube([-1.5, 17.6, -8.2], [3, 2.6, 1], 'head', 'face'))                         # forehead / lores
    head.add(Cube([-0.9, 19.7, -8.9], [1.8, 0.8, 1.4], 'cere', 'cere'))                     # white cere
    head.add(Cube([-0.6, 18.9, -10.3], [1.2, 1, 2.6], 'beak', 'beak'))                      # upper beak
    head.add(Cube([-0.45, 18.4, -10.6], [0.9, 0.9, 0.7], 'beak2', None, rot=[-25, 0, 0], pivot=[0, 19, -10.3]))  # hooked tip
    for side in (-1, 1):
        x = 2.25 - 0.6 if side > 0 else -2.25 - 0.4
        head.add(Cube([x, 18.8, -6.3], [1, 3, 3], 'ring', 'eye'))                             # eye with pale ring
    jaw = bone('jaw', [0, 18.9, -8], 'head')
    jaw.add(Cube([-0.5, 18.4, -10], [1, 0.6, 2.2], 'beak2', None))
    # ---- wings (folded along the sides; flight rotates them out around the body axis)
    wl = bone('wing_l', [3.4, 13, -3], 'body')
    wl.add(Cube([3.2, 7.5, -3], [1, 5.5, 10], 'wing', 'wing'))
    wl.add(Cube([2.8, 11, -3.6], [1, 2.5, 5], 'scap', 'shoulder', inflate=0.1))
    wlt = bone('wing_l_tip', [3.8, 8.5, 5], 'wing_l')
    wlt.add(Cube([3.3, 7.6, 3], [1, 3, 10], 'prim', 'primaries'))
    wr = bone('wing_r', [-3.4, 13, -3], 'body')
    wrt = bone('wing_r_tip', [-3.8, 8.5, 5], 'wing_r')
    for c in wl.cubes:
        wr.cubes.append(mirror(c))
    for c in wlt.cubes:
        wrt.cubes.append(mirror(c))
    # ---- tail
    tail = bone('tail', [0, 10.5, 8], 'body')
    tail.add(Cube([-2.5, 9.8, 8], [5, 1, 9], 'tail', 'tail'))
    tail.add(Cube([-2, 9.2, 8.5], [4, 0.6, 6], 'belly', None))
    # ---- pigeon diaper (hidden unless worn): back panel, shoulder loops in front of the wings, pouch under the tail
    dp = bone('diaper', [0, 8, 1], 'body')
    dp.add(Cube([-3, 13.4, -2], [6, 1, 10], 'diaper', 'd_panel', inflate=0.1),
           Cube([-3.9, 7.8, -4.4], [1, 6, 1], 'diaper', 'd_strap'), Cube([2.9, 7.8, -4.4], [1, 6, 1], 'diaper', 'd_strap'),
           Cube([-3.6, 6, -4.4], [7.2, 1, 1], 'diaper', 'd_strap'),
           Cube([-2.8, 7, 6.2], [5.6, 2.6, 5], 'diaper', 'd_pouch', inflate=0.3),
           Cube([-3.2, 13.6, 5], [6.4, 1, 1.5], 'diaper', 'd_edge'),
           Cube([2.3, 14.2, 2.5], [1, 1, 1], 'diaper', 'd_clip'))
    # ---- legs (the yellow ring sits on the right leg, as in the photo)
    for side, name in ((-1, 'leg_r'), (1, 'leg_l')):
        lx = 1.8 * side
        leg = bone(name, [lx, 7, 0.5], 'root')
        leg.add(Cube([lx - 1.25, 4.6, -0.75], [2.5, 2.6, 2.5], 'thigh', 'thigh', inflate=0.2),
                Cube([lx - 0.5, 0.8, -0.1], [1, 4, 1], 'leg', 'tarsus'))
        if side < 0:
            leg.add(Cube([lx - 0.8, 2.2, -0.4], [1.6, 1.3, 1.6], 'ring_band', 'band'))
        foot = bone('foot' + name[3:], [lx, 0.8, 0.4], name)        # toes on their own bone, so they curl in flight
        foot.add(Cube([lx - 0.45, 0, -3.6], [0.9, 0.8, 4], 'leg', 'toe'),                                   # middle toe
                 Cube([lx - 0.4, 0, -2.9], [0.8, 0.8, 3.3], 'leg', 'toe', rot=[0, 32, 0], pivot=[lx, 0.4, 0.3]),
                 Cube([lx - 0.4, 0, -2.9], [0.8, 0.8, 3.3], 'leg', 'toe', rot=[0, -32, 0], pivot=[lx, 0.4, 0.3]),
                 Cube([lx - 0.4, 0, 0.6], [0.8, 0.7, 1.8], 'leg', 'toe'),                                     # back toe
                 Cube([lx - 0.3, -0.05, -4.2], [0.6, 0.6, 0.7], 'claw', None))
    return B


# ------------------------------------------------------------------------------------------- box UV and painting
def dims(c):
    return [max(1, int(math.ceil(v - 1e-6))) for v in c.s]


def pack(bones):
    cubes = sorted([c for b in bones for c in b.cubes], key=lambda c: -(dims(c)[2] + dims(c)[1]))
    x = y = row = 0
    for c in cubes:
        w, h, d = dims(c)
        uw, uh = 2 * (d + w), d + h
        if x + uw > TEX_W:
            x, y, row = 0, y + row, 0
        c.uv = [x, y]
        x += uw
        row = max(row, uh)
    assert y + row <= TEX_H, 'texture too small'


def faces(c):
    u, v = c.uv
    w, h, d = dims(c)
    return dict(up=(u + d, v, w, d), down=(u + d + w, v, w, d), east=(u, v + d, d, h), north=(u + d, v + d, w, h),
                west=(u + d + w, v + d, d, h), south=(u + 2 * d + w, v + d, w, h))


def shade(c, k):
    return tuple(max(0, min(255, int(round(x * k)))) for x in c)


def paint(bones, seed=5):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (TEX_W, TEX_H), (0, 0, 0, 0))
    px = img.load()

    def put(r, x, y, col):
        if 0 <= x < r[2] and 0 <= y < r[3]:
            px[r[0] + x, r[1] + y] = tuple(col) + (255,)

    def fill(r, col, noise=0.04, vgrad=0.0):
        for y in range(r[3]):
            for x in range(r[2]):
                k = 1 + rnd.uniform(-noise, noise) + vgrad * (0.5 - y / max(1, r[3] - 1))
                put(r, x, y, shade(col, k))

    for b in bones:
        for c in b.cubes:
            if c.mat == 'diaper':
                continue              # the diaper lives in its own overlay texture
            F = faces(c)
            for n, r in F.items():
                fill(r, shade(P[c.mat], {'up': 1.06, 'down': 0.82}.get(n, 1.0)), vgrad=0.1 if n not in ('up', 'down') else 0)
            d = c.deco
            if d in ('neck', 'throat'):
                # iridescent neck: dark slate with green and purple sheen, finely speckled with pale feather tips
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            q = rnd.random()
                            if q < 0.22:
                                put(r, x, y, shade(P['neck_g'], rnd.uniform(0.8, 1.05)))
                            elif q < 0.38:
                                put(r, x, y, shade(P['neck_p'], rnd.uniform(0.75, 0.95)))
                            elif q < 0.50:
                                put(r, x, y, shade(P['neck_w'], rnd.uniform(0.8, 1.0)))
                            else:
                                put(r, x, y, shade(P['neck'], rnd.uniform(0.9, 1.1)))
            elif d == 'eye':
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(3):
                        for x in range(3):
                            put(r, x, y, P['ring'])
                    for x, y in ((1, 0), (0, 1), (2, 1), (1, 2)):
                        put(r, x, y, P['iris'])
                    put(r, 1, 1, P['pupil'])
                    put(r, 2, 0, (250, 250, 250))
                for n in ('north', 'south', 'up', 'down'):
                    fill(F[n], P['ring'], 0.02)
            elif d == 'cere':
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            if rnd.random() < 0.35:
                                put(r, x, y, P['cere2'])
            elif d == 'beak':
                for n, r in F.items():
                    for x in range(r[2]):
                        put(r, x, 0, shade(P['beak'], 1.15))
                    put(r, 0, r[3] - 1, P['beak2'])
            elif d == 'head':
                for n in ('up', 'east', 'west', 'south'):
                    r = F[n]
                    for _ in range(r[2] * r[3] // 4):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), shade(P['head2'], rnd.uniform(0.9, 1.1)))
                r = F['north']
                for x in range(r[2]):
                    put(r, x, r[3] - 1, P['neck'])
            elif d in ('wing',):
                # smooth light grey coverts with the two dark bars of a blue-bar pigeon near the back edge
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(r[3]):
                        for x in range(r[2]):
                            put(r, x, y, shade(P['wing'], rnd.uniform(0.96, 1.04) - 0.04 * y / r[3]))
                    for k, back in ((0, 3), (1, 6)):
                        for y in range(1, r[3]):
                            xx = back + y // 2
                            xx = r[2] - 1 - xx if n == 'west' else xx
                            put(r, r[2] - 1 - xx if n == 'east' else xx, y, P['bar'])
                    for x in range(r[2]):
                        put(r, x, 0, shade(P['scap'], 1.05))
            elif d == 'primaries':
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            put(r, x, y, shade(P['prim'] if (x + y) % 3 else P['prim2'], rnd.uniform(0.92, 1.05)))
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(r[3]):
                        put(r, r[2] - 1 if n == 'east' else 0, y, P['prim2'])
            elif d == 'tail':
                for n, r in F.items():
                    if n in ('up', 'down'):
                        for y in range(r[3] - 2, r[3]):
                            for x in range(r[2]):
                                put(r, x, y, P['band'])
                        for x in range(0, r[2], 2):
                            for y in range(r[3] - 2):
                                put(r, x, y, shade(P['tail'], 0.92))
                    elif n == 'south':
                        fill(r, P['band'], 0.02)
                    else:
                        for x in range(r[2]):
                            put(r, x, 0, P['tail'])
            elif d in ('body', 'chest'):
                for n, r in F.items():
                    if n in ('east', 'west'):
                        for y in range(r[3]):
                            for x in range(r[2]):
                                t = x / max(1, r[2] - 1)
                                if (n == 'east' and t < 0.35) or (n == 'west' and t > 0.65):
                                    continue
                                if y > r[3] // 2:
                                    put(r, x, y, shade(P['belly'], rnd.uniform(0.92, 1.05)))
                    if n == 'up':
                        for y in range(r[3]):
                            for x in range(r[2]):
                                if y > r[3] // 3:
                                    put(r, x, y, shade(P['wing'], rnd.uniform(0.92, 1.05)))
                    if n == 'north' and d == 'chest':
                        for y in range(r[3] // 2):
                            for x in range(r[2]):
                                if rnd.random() < 0.3:
                                    put(r, x, y, shade(P['neck_p'], 0.8))
            elif d == 'tarsus':
                for n, r in F.items():
                    for y in range(r[3]):
                        if y % 2:
                            for x in range(r[2]):
                                put(r, x, y, P['leg2'])
            elif d == 'toe':
                for n, r in F.items():
                    for x in range(r[2]):
                        if x % 2:
                            for y in range(r[3]):
                                put(r, x, y, P['leg2'])
            elif d == 'band':
                for n, r in F.items():
                    for x in range(r[2]):
                        put(r, x, 0, P['ring_band2'])
            elif d == 'thigh':
                for n, r in F.items():
                    for _ in range(3):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), shade(P['thigh'], 1.15))
    return img


def geo_json(bones):
    out = []
    for b in bones:
        jb = {'name': b.name, 'pivot': [round(v, 3) for v in b.pivot]}
        if b.parent:
            jb['parent'] = b.parent
        if any(b.rot):
            jb['rotation'] = b.rot
        cubes = []
        for c in b.cubes:
            jc = {'origin': [round(v, 3) for v in c.o], 'size': [round(v, 3) for v in c.s], 'uv': c.uv}
            if c.inflate:
                jc['inflate'] = c.inflate
            if c.rot:
                jc['rotation'] = c.rot
                jc['pivot'] = [round(v, 3) for v in c.pivot]
            cubes.append(jc)
        if cubes:
            jb['cubes'] = cubes
        out.append(jb)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.pigeon', 'texture_width': TEX_W, 'texture_height': TEX_H,
                        'visible_bounds_width': 3, 'visible_bounds_height': 2.5, 'visible_bounds_offset': [0, 0.75, 0]},
        'bones': out}]}


# ------------------------------------------------------------------------------------------- animations
def animations():
    A = {}
    w = lambda t, p, a, ph=0.0: a * S(TAU * t / p + ph)

    def saw(t, period, thrust=0.25):
        """Pigeon head-bob: the head shoots forward fast, then stays still in the world while the body catches up."""
        f = (t % period) / period
        if f < thrust:
            return keys(f, [(0, 1.2), (thrust, -1.6)])
        return -1.6 + 2.8 * (f - thrust) / (1 - thrust)

    A['idle'] = anim(4.0, True, {
        'body': {'scale': lambda t: [1 + w(t, 2, 0.012), 1 + w(t, 2, 0.02), 1]},
        'neck': {'rotation': lambda t: [w(t, 4, 2), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (1.6, 0), (1.7, -8), (2.6, -8), (2.7, 0), (4, 0)]),
                                        keys(t, [(0, 0), (0.6, 0), (0.7, 35), (1.4, 35), (1.5, 0), (2.8, 0), (2.9, -30), (3.6, -30), (3.7, 0), (4, 0)]),
                                        keys(t, [(0, 0), (0.7, 0), (0.8, 12), (1.4, 12), (1.5, 0), (4, 0)])]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (3.0, 0), (3.1, -6), (3.25, 2), (3.4, 0), (4, 0)]), 0, 0]},
    })
    A['walk'] = anim(0.6, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.6, 28), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.6, -28), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.6, 0.35)), 0]},
        'body': {'rotation': lambda t: [0, w(t, 0.6, 4), w(t, 0.6, 3)]},
        'neck': {'position': lambda t: [0, 0, saw(t, 0.3)]},
        'head': {'rotation': lambda t: [w(t, 0.3, 2), 0, 0]},
        'tail': {'rotation': lambda t: [0, w(t, 0.6, -5), 0]},
    })
    A['run'] = anim(0.36, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.36, 40), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.36, -40), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.36, 0.6)), 0]},
        'body': {'rotation': lambda t: [8, w(t, 0.36, 5), 0]},
        'neck': {'position': lambda t: [0, 0, saw(t, 0.18, 0.3) * 0.7], 'rotation': lambda t: [10, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 12 + w(t, 0.18, 4)]},
        'wing_r': {'rotation': lambda t: [0, 0, -12 - w(t, 0.18, 4)]},
    })
    A['peck'] = anim(1.3, False, {
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, 34), (1.1, 34), (1.3, 0)]), 0, 0]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, 42), (1.1, 42), (1.3, 0)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, 25), (0.3, 50), (0.38, 22), (0.55, 52), (0.62, 22), (0.8, 50), (0.88, 25), (1.1, 25), (1.3, 0)]), 0, 0]},
        'jaw': {'rotation': lambda t: [keys(t, [(0, 0), (0.28, 0), (0.3, 18), (0.36, 0), (0.53, 0), (0.55, 18), (0.6, 0), (0.78, 0), (0.8, 18), (0.86, 0), (1.3, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, -25), (1.1, -25), (1.3, 0)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, -18), (1.1, -18), (1.3, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, -18), (1.1, -18), (1.3, 0)]), 0, 0]},
    })
    A['drink'] = anim(2.2, False, {
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 30), (1.9, 30), (2.2, 0)]), 0, 0]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 55), (1.9, 55), (2.2, 0)]), 0, 0],
                 'scale': lambda t: [1, 1 + (w(t, 0.4, 0.04) if 0.4 < t < 1.8 else 0), 1]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 30), (1.9, 30), (2.2, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -22), (1.9, -22), (2.2, 0)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -16), (1.9, -16), (2.2, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -16), (1.9, -16), (2.2, 0)]), 0, 0]},
    })
    # courtship: puffed neck, bows, fanned dragging tail
    def bow(t, p):
        f = (t % p) / p
        return keys(f, [(0, 0), (0.25, 1), (0.45, 1), (0.7, 0), (1, 0)])
    A['coo'] = anim(2.0, True, {
        'neck': {'scale': lambda t: [1.35, 1.12, 1.3], 'rotation': lambda t: [-6 + 46 * bow(t, 1.0), 0, 0]},
        'head': {'rotation': lambda t: [-10 + 30 * bow(t, 1.0), 0, 0]},
        'body': {'rotation': lambda t: [24 * bow(t, 1.0), 0, 0], 'scale': lambda t: [1.06, 1.06, 1]},
        'tail': {'rotation': lambda t: [-16, 0, 0], 'scale': lambda t: [1.7, 1, 1.05]},
        'wing_l': {'rotation': lambda t: [0, 0, 6]},
        'wing_r': {'rotation': lambda t: [0, 0, -6]},
        'jaw': {'rotation': lambda t: [0, 0, 0]},
    })
    A['strut'] = anim(3.2, True, {          # turning circles in front of a hen while cooing
        'root': {'rotation': lambda t: [0, -360 * t / 3.2, 0]},
        'neck': {'scale': lambda t: [1.35, 1.12, 1.3], 'rotation': lambda t: [-6 + 40 * bow(t, 0.8), 0, 0]},
        'head': {'rotation': lambda t: [-10 + 26 * bow(t, 0.8), 0, 0]},
        'body': {'rotation': lambda t: [20 * bow(t, 0.8), 0, 0]},
        'tail': {'rotation': lambda t: [-16, 0, 0], 'scale': lambda t: [1.7, 1, 1.05]},
        'leg_r': {'rotation': lambda t: [w(t, 0.4, 18), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.4, -18), 0, 0]},
    })
    # tame pigeons circling in front of you: turning on the spot in quick little steps, bowing with a puffed neck,
    # the spread tail sweeping the floor
    def circle(direction, L=2.4):
        return anim(L, True, {
            'root': {'rotation': lambda t: [0, -direction * 360 * t / L, 0]},
            'leg_r': {'rotation': lambda t: [w(t, 0.3, 22), 0, -direction * 6]},
            'leg_l': {'rotation': lambda t: [w(t, 0.3, -22), 0, -direction * 6]},
            'body': {'rotation': lambda t: [22 * bow(t, 0.8), 0, direction * 6], 'scale': lambda t: [1.05, 1.04, 1]},
            'neck': {'scale': lambda t: [1.35, 1.12, 1.3], 'rotation': lambda t: [-6 + 40 * bow(t, 0.8), 0, 0],
                     'position': lambda t: [0, 0, w(t, 0.3, 0.6)]},
            'head': {'rotation': lambda t: [-8 + 22 * bow(t, 0.8), direction * 14, 0]},
            'tail': {'rotation': lambda t: [-16, -direction * 10, 0], 'scale': lambda t: [1.7, 1, 1.05]},
            'wing_l': {'rotation': lambda t: [0, 0, 7 + w(t, 0.4, 2)]},
            'wing_r': {'rotation': lambda t: [0, 0, -7 - w(t, 0.4, 2)]},
        })
    A['circle'] = circle(1)
    A['circle_ccw'] = circle(-1)

    # running up to you: head held low and forward, tail lowered, quick little steps
    A['runup'] = anim(0.36, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.36, 40), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.36, -40), 0, 0]},
        'foot_r': {'rotation': lambda t: [max(0, w(t, 0.36, -20)), 0, 0]},
        'foot_l': {'rotation': lambda t: [max(0, w(t, 0.36, 20)), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.36, 0.7)), 0]},
        'body': {'rotation': lambda t: [24, w(t, 0.36, 4), 0]},
        'neck': {'rotation': lambda t: [30, 0, 0], 'position': lambda t: [0, -0.6, -0.8 + w(t, 0.18, 0.4)]},
        'head': {'rotation': lambda t: [-8, 0, 0]},
        'tail': {'rotation': lambda t: [-20, 0, 0], 'scale': lambda t: [0.9, 1, 1]},
        'wing_l': {'rotation': lambda t: [0, 0, 8 + w(t, 0.18, 4)]},
        'wing_r': {'rotation': lambda t: [0, 0, -8 - w(t, 0.18, 4)]},
    })
    # being stroked: leans into the hand with the neck stretched forward and low, feathers a little fluffed
    A['petted'] = anim(2.4, True, {
        'root': {'position': lambda t: [0, -0.6, -0.3]},
        'body': {'rotation': lambda t: [20 + w(t, 2.4, 3), 0, w(t, 2.4, 2)], 'scale': lambda t: [1.05, 1.04, 1.02]},
        'neck': {'rotation': lambda t: [44 + w(t, 1.2, 4), 0, w(t, 2.4, 5)], 'scale': lambda t: [1.08, 1.1, 1.08]},
        'head': {'rotation': lambda t: [18, 0, w(t, 2.4, 8, 1.0)]},
        'tail': {'rotation': lambda t: [-10, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 5]},
        'wing_r': {'rotation': lambda t: [0, 0, -5]},
        'leg_r': {'rotation': lambda t: [-12, 0, 0]},
        'leg_l': {'rotation': lambda t: [-12, 0, 0]},
    })
    # on your shoulder, nibbling at your face: turns to the face, a burst of quick little pecks, a look round, again
    def nibble(side, L=2.6):
        s_ = -1 if side == 'l' else 1          # on the left shoulder the face is to its right
        pecks = [0.35, 0.5, 0.65, 0.8, 1.35, 1.48, 1.61]
        def thrust(t):
            return min([abs(t - p) for p in pecks]) < 0.05
        return anim(L, True, {
            'root': {'rotation': lambda t: [0, s_ * 28, 0]},
            'neck': {'rotation': lambda t: [-14 + (10 if thrust(t) else 0), s_ * keys(t, [(0, 40), (1.0, 40), (1.1, 10), (1.2, 40), (2.0, 40), (2.1, -10), (2.35, -10), (2.5, 40), (2.6, 40)]), 0],
                     'position': lambda t: [0, 0, -1.4 if thrust(t) else 0]},
            'head': {'rotation': lambda t: [(16 if thrust(t) else 0), 0, s_ * keys(t, [(0, 0), (2.05, 0), (2.15, 18), (2.4, 18), (2.5, 0), (2.6, 0)])]},
            'jaw': {'rotation': lambda t: [14 if thrust(t) else 0, 0, 0]},
            'tail': {'rotation': lambda t: [0, -s_ * 8, 0]},
        }, fps=40)
    A['nibble_l'] = nibble('l')
    A['nibble_r'] = nibble('r')
    # the answer to your nod: one deep bow with a puffed neck
    A['bow'] = anim(1.3, False, {
        'neck': {'scale': lambda t: [keys(t, [(0, 1), (0.25, 1.35), (1.0, 1.35), (1.3, 1)]), keys(t, [(0, 1), (0.25, 1.12), (1.0, 1.12), (1.3, 1)]),
                                     keys(t, [(0, 1), (0.25, 1.3), (1.0, 1.3), (1.3, 1)])],
                 'rotation': lambda t: [keys(t, [(0, 0), (0.2, -10), (0.55, 48), (0.8, 48), (1.15, 0)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, -8), (0.55, 24), (0.8, 24), (1.15, 0)]), 0, 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.55, 26), (0.8, 26), (1.15, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (0.55, -18), (0.8, -18), (1.15, 0)]), 0, 0],
                 'scale': lambda t: [keys(t, [(0, 1), (0.4, 1.6), (0.9, 1.6), (1.3, 1)]), 1, 1]},
    })
    # waiting in front of you to be stroked: head low, looking up, tiny wing quiver
    A['beg'] = anim(2.0, True, {
        'body': {'rotation': lambda t: [14, 0, 0]},
        'neck': {'rotation': lambda t: [22, 0, 0]},
        'head': {'rotation': lambda t: [-30, w(t, 2, 10), w(t, 2, 6)]},
        'wing_l': {'rotation': lambda t: [0, 0, 6 + w(t, 0.12, 2)]},
        'wing_r': {'rotation': lambda t: [0, 0, -6 - w(t, 0.12, 2)]},
        'tail': {'rotation': lambda t: [-12, 0, 0]},
    })
    # flight
    def flap(t, p, base=88, amp=55, lag=0.0):
        return base + amp * S(TAU * t / p + lag)
    A['takeoff'] = anim(0.55, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.12, -1.2), (0.3, 1.5), (0.55, 3)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, 10), (0.55, 14)]), 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.12, 60), (0.25, 150), (0.4, 40), (0.55, 140)])]},
        'wing_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.12, 60), (0.25, 150), (0.4, 40), (0.55, 140)])]},
        'wing_l_tip': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -55), (0.55, -55)]), 0, 0]},
        'wing_r_tip': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -55), (0.55, -55)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -20), (0.3, 50), (0.55, 70)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -20), (0.3, 50), (0.55, 70)]), 0, 0]},
        'tail': {'scale': lambda t: [keys(t, [(0, 1), (0.3, 1.6)]), 1, 1]},
        'foot_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 0), (0.55, -70)]), 0, 0]},
        'foot_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 0), (0.55, -70)]), 0, 0]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.55, 18)]), 0, 0]},
    })
    A['fly'] = anim(0.36, True, {
        'body': {'rotation': lambda t: [18, 0, 0]},
        'root': {'position': lambda t: [0, w(t, 0.36, 0.5, 1.6), 0]},
        'neck': {'rotation': lambda t: [24, 0, 0]},
        'head': {'rotation': lambda t: [-22, 0, 0]},
        'wing_l': {'rotation': lambda t: [w(t, 0.36, 8, 1.2), 0, flap(t, 0.36)]},
        'wing_r': {'rotation': lambda t: [w(t, 0.36, 8, 1.2), 0, -flap(t, 0.36)]},
        'wing_l_tip': {'rotation': lambda t: [-55 + w(t, 0.36, 15, -0.9), 0, w(t, 0.36, 20, -0.9)]},
        'wing_r_tip': {'rotation': lambda t: [-55 + w(t, 0.36, 15, -0.9), 0, -w(t, 0.36, 20, -0.9)]},
        'tail': {'scale': lambda t: [1.4, 1, 1], 'rotation': lambda t: [w(t, 0.36, 4), 0, 0]},
        'leg_r': {'rotation': lambda t: [75, 0, 0]},
        'leg_l': {'rotation': lambda t: [75, 0, 0]},
        'foot_r': {'rotation': lambda t: [-70, 0, 0]},
        'foot_l': {'rotation': lambda t: [-70, 0, 0]},
    })
    A['glide'] = anim(2.0, True, {
        'body': {'rotation': lambda t: [18, 0, w(t, 2, 4)]},
        'neck': {'rotation': lambda t: [24, 0, 0]},
        'head': {'rotation': lambda t: [-22, 0, w(t, 2, -4)]},
        'wing_l': {'rotation': lambda t: [0, 0, 100 + w(t, 1, 3)]},
        'wing_r': {'rotation': lambda t: [0, 0, -100 - w(t, 1, 3)]},
        'wing_l_tip': {'rotation': lambda t: [-62, 0, -6]},
        'wing_r_tip': {'rotation': lambda t: [-62, 0, 6]},
        'tail': {'scale': lambda t: [1.5, 1, 1]},
        'leg_r': {'rotation': lambda t: [75, 0, 0]},
        'leg_l': {'rotation': lambda t: [75, 0, 0]},
        'foot_r': {'rotation': lambda t: [-70, 0, 0]},
        'foot_l': {'rotation': lambda t: [-70, 0, 0]},
    })
    A['land'] = anim(0.7, False, {
        'body': {'rotation': lambda t: [keys(t, [(0, 18), (0.3, -18), (0.55, 6), (0.7, 0)]), 0, 0]},
        'wing_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -25), (0.7, 0)]), 0, keys(t, [(0, 100), (0.15, 140), (0.3, 70), (0.45, 120), (0.7, 0)])]},
        'wing_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -25), (0.7, 0)]), 0, -keys(t, [(0, 100), (0.15, 140), (0.3, 70), (0.45, 120), (0.7, 0)])]},
        'wing_l_tip': {'rotation': lambda t: [keys(t, [(0, -60), (0.45, -50), (0.7, 0)]), 0, 0]},
        'wing_r_tip': {'rotation': lambda t: [keys(t, [(0, -60), (0.45, -50), (0.7, 0)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 70), (0.3, -35), (0.55, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 70), (0.3, -35), (0.55, 0)]), 0, 0]},
        'foot_r': {'rotation': lambda t: [keys(t, [(0, -70), (0.25, 20), (0.55, 0)]), 0, 0]},
        'foot_l': {'rotation': lambda t: [keys(t, [(0, -70), (0.25, 20), (0.55, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, 25), (0.7, 0)]), 0, 0], 'scale': lambda t: [keys(t, [(0, 1.5), (0.3, 1.8), (0.7, 1)]), 1, 1]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 20), (0.3, -10), (0.7, 0)]), 0, 0]},
    })
    # grooming and rest
    A['preen'] = anim(2.6, False, {
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.35, 25), (2.2, 25), (2.6, 0)]), keys(t, [(0, 0), (0.35, 125), (2.2, 125), (2.6, 0)]), 0]},
        'head': {'rotation': lambda t: [(keys(t, [(0, 0), (0.35, 30), (2.2, 30), (2.6, 0)]) + (w(t, 0.22, 10) if 0.5 < t < 2.1 else 0)), 0,
                                        keys(t, [(0, 0), (0.35, 25), (2.2, 25), (2.6, 0)])]},
        'jaw': {'rotation': lambda t: [(max(0, w(t, 0.22, 15)) if 0.5 < t < 2.1 else 0), 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.4, 18), (2.1, 18), (2.6, 0)])]},
        'body': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.4, -6), (2.1, -6), (2.6, 0)])]},
    })
    A['shake'] = anim(1.1, False, {
        'body': {'scale': lambda t: [keys(t, [(0, 1), (0.2, 1.18), (0.9, 1.15), (1.1, 1)])] * 3,
                 'rotation': lambda t: [0, (w(t, 0.12, 12) if 0.2 < t < 0.85 else 0), 0]},
        'neck': {'scale': lambda t: [keys(t, [(0, 1), (0.2, 1.15), (0.9, 1.1), (1.1, 1)])] * 3,
                 'rotation': lambda t: [0, (w(t, 0.12, -18) if 0.2 < t < 0.85 else 0), 0]},
        'wing_l': {'rotation': lambda t: [0, 0, (12 + w(t, 0.12, 6)) if 0.2 < t < 0.85 else 0]},
        'wing_r': {'rotation': lambda t: [0, 0, -(12 + w(t, 0.12, 6)) if 0.2 < t < 0.85 else 0]},
        'tail': {'rotation': lambda t: [0, (w(t, 0.1, 20) if 0.75 < t < 1.05 else 0), 0]},
    })
    A['sit'] = anim(4.0, True, {
        'root': {'position': lambda t: [0, -3.6, 0]},
        'body': {'rotation': lambda t: [8, 0, 0], 'scale': lambda t: [1.08, 1.05 + w(t, 2, 0.015), 1.03]},
        'neck': {'rotation': lambda t: [-6, 0, 0], 'position': lambda t: [0, -1, 1]},
        'head': {'rotation': lambda t: [0, keys(t, [(0, 0), (1.2, 0), (1.3, 30), (2.2, 30), (2.3, 0), (4, 0)]), 0]},
        'leg_r': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'leg_l': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'tail': {'rotation': lambda t: [-6, 0, 0]},
    })
    A['sleep'] = anim(4.0, True, {          # sitting, fluffed up, head tucked back into the shoulder feathers
        'root': {'position': lambda t: [0, -3.6, 0]},
        'body': {'rotation': lambda t: [8, 0, 0], 'scale': lambda t: [1.15, 1.12 + w(t, 4, 0.03), 1.05]},
        'neck': {'rotation': lambda t: [-35, 150, 0], 'position': lambda t: [0, -2, 2], 'scale': lambda t: [1.15, 0.7, 1.15]},
        'head': {'rotation': lambda t: [10, 0, 20]},
        'leg_r': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'leg_l': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'tail': {'rotation': lambda t: [-6, 0, 0]},
    })
    A['wing_stretch'] = anim(2.0, False, {
        'wing_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.4, 20), (1.6, 20), (2, 0)]), 0, keys(t, [(0, 0), (0.4, 35), (1.6, 35), (2, 0)])]},
        'wing_l_tip': {'rotation': lambda t: [keys(t, [(0, 0), (0.4, -45), (1.6, -45), (2, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.4, 55), (1.6, 55), (2, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [0, 0, 0], 'scale': lambda t: [keys(t, [(0, 1), (0.4, 1.5), (1.6, 1.5), (2, 1)]), 1, 1]},
        'body': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.4, -5), (1.6, -5), (2, 0)])]},
    })
    A['scratch'] = anim(1.4, False, {
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -95), (1.15, -95), (1.4, 0)]) + (w(t, 0.12, 12) if 0.3 < t < 1.1 else 0), 0, -20]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 20), (1.15, 20), (1.4, 0)]), 0, keys(t, [(0, 0), (0.25, -25), (1.15, -25), (1.4, 0)])]},
        'head': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.25, -25), (1.15, -25), (1.4, 0)])]},
        'body': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.25, 8), (1.15, 8), (1.4, 0)])]},
    })
    A['look_up'] = anim(1.8, False, {      # cocks its head to watch the sky with one eye
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.15, -10), (1.5, -10), (1.8, 0)]), 0, keys(t, [(0, 0), (0.15, 75), (1.5, 75), (1.8, 0)])]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.2, -10), (1.5, -10), (1.8, 0)]), 0, 0]},
    })
    A['hop'] = anim(0.5, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.08, -0.8), (0.25, 3), (0.45, 0), (0.5, 0)]), 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.15, 45), (0.3, 10), (0.5, 0)])]},
        'wing_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.15, 45), (0.3, 10), (0.5, 0)])]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 25), (0.45, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 25), (0.45, 0)]), 0, 0]},
    })
    A['startled'] = anim(0.8, False, {     # neck stretched up, feathers tight, ready to fly
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.1, -18), (0.6, -18), (0.8, 0)]), 0, 0],
                 'scale': lambda t: [0.9, keys(t, [(0, 1), (0.1, 1.2), (0.6, 1.2), (0.8, 1)]), 0.9]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.1, -12), (0.6, -12), (0.8, 0)]), 0, 0], 'scale': lambda t: [0.94, 1, 0.97]},
        'head': {'rotation': lambda t: [0, keys(t, [(0, 0), (0.2, 25), (0.4, -25), (0.6, 0)]), 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.1, 8), (0.8, 0)])]},
        'wing_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.1, 8), (0.8, 0)])]},
    })
    return {'format_version': '1.8.0', 'animations': {f'animation.pigeon.{k}': v for k, v in A.items()}}


# ------------------------------------------------------------------------------------------- software preview
def rot_matrix(rx, ry, rz):
    """GeckoLib convention: X and Y are flipped, applied X first, then Y, then Z."""
    ax, ay, az = math.radians(-rx), math.radians(-ry), math.radians(rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    X = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mm(Z, mm(Y, X))


def mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mv(a, v):
    return [sum(a[i][k] * v[k] for k in range(3)) for i in range(3)]


def affine(R, t):
    return (R, t)


def compose(A1, A2):     # A1 after A2
    R1, t1 = A1
    R2, t2 = A2
    return (mm(R1, R2), [a + b for a, b in zip(mv(R1, t2), t1)])


def apply(A, v):
    R, t = A
    return [a + b for a, b in zip(mv(R, v), t)]


def about(pivot, R, scale=(1, 1, 1), pos=(0, 0, 0)):
    S_ = [[scale[0], 0, 0], [0, scale[1], 0], [0, 0, scale[2]]]
    M = mm(R, S_)
    p = pivot
    # x' = M (x - p) + p + pos
    t = [p[i] - mv(M, p)[i] + pos[i] for i in range(3)]
    return (M, t)


def sample(animdef, bone, kind, t):
    ch = animdef['bones'].get(bone, {}).get(kind)
    if not ch:
        return None
    ts = sorted((float(k), v['vector'] if isinstance(v, dict) else v) for k, v in ch.items())
    t = t % animdef['animation_length'] if animdef.get('loop') is True else min(t, animdef['animation_length'])
    for (t0, v0), (t1, v1) in zip(ts, ts[1:]):
        if t0 <= t <= t1:
            f = (t - t0) / max(1e-6, t1 - t0)
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return ts[-1][1]


def render(geo, tex, poses, view_yaw=35, view_pitch=18, size=(380, 340), zoom=11):
    """poses: list of (title, [(animdef, time), ...]); draws each pose into a tile."""
    bones = {b['name']: b for b in geo['minecraft:geometry'][0]['bones']}
    texpx = tex.load()
    tiles = []
    for title, layers in poses:
        world = {}

        def bone_tf(name):
            if name in world:
                return world[name]
            b = bones[name]
            rot = list(b.get('rotation', [0, 0, 0]))
            pos, scl = [0, 0, 0], [1, 1, 1]
            for a, t in layers:
                r = sample(a, name, 'rotation', t)
                if r:
                    rot = [x + y for x, y in zip(rot, r)]
                p = sample(a, name, 'position', t)
                if p:
                    pos = [x + y for x, y in zip(pos, p)]
                s = sample(a, name, 'scale', t)
                if s:
                    scl = [x * y for x, y in zip(scl, s)]
            local = about(b['pivot'], rot_matrix(*rot), scl, pos)
            tf = compose(bone_tf(b['parent']), local) if b.get('parent') else local
            world[name] = tf
            return tf

        quads = []
        vy, vp = math.radians(view_yaw), math.radians(view_pitch)
        for name, b in bones.items():
            tf = bone_tf(name)
            for c in b.get('cubes', []):
                inf = c.get('inflate', 0)
                o = [x - inf for x in c['origin']]
                s = [x + 2 * inf for x in c['size']]
                ctf = tf
                if 'rotation' in c:
                    ctf = compose(tf, about(c['pivot'], rot_matrix(*c['rotation'])))
                cube = Cube(c['origin'], c['size'], None)
                cube.uv = c['uv']
                F = faces(cube)
                W, H, D = dims(cube)
                x0, y0, z0 = o
                x1, y1, z1 = o[0] + s[0], o[1] + s[1], o[2] + s[2]
                defs = {
                    'up': (lambda i, j, a, b_: [x0 + a * s[0], y1, z0 + b_ * s[2]], W, D),
                    'down': (lambda i, j, a, b_: [x0 + a * s[0], y0, z0 + b_ * s[2]], W, D),
                    'north': (lambda i, j, a, b_: [x1 - a * s[0], y1 - b_ * s[1], z0], W, H),
                    'south': (lambda i, j, a, b_: [x0 + a * s[0], y1 - b_ * s[1], z1], W, H),
                    'east': (lambda i, j, a, b_: [x0, y1 - b_ * s[1], z1 - a * s[2]], D, H),
                    'west': (lambda i, j, a, b_: [x1, y1 - b_ * s[1], z0 + a * s[2]], D, H),
                }
                for fname, (fn, nu, nv) in defs.items():
                    r = F[fname]
                    for j in range(nv):
                        for i in range(nu):
                            col = texpx[r[0] + i, r[1] + j]
                            if col[3] == 0:
                                continue
                            corners = [fn(i, j, (i + du) / nu, (j + dv) / nv) for du, dv in ((0, 0), (1, 0), (1, 1), (0, 1))]
                            pts = []
                            depth = 0
                            for p in corners:
                                p = apply(ctf, p)
                                # view: yaw around Y then pitch around X
                                x = p[0] * math.cos(vy) - p[2] * math.sin(vy)
                                z = p[0] * math.sin(vy) + p[2] * math.cos(vy)
                                y = p[1] * math.cos(vp) + z * math.sin(vp)
                                z2 = -p[1] * math.sin(vp) + z * math.cos(vp)
                                pts.append((size[0] / 2 + x * zoom, size[1] - 40 - y * zoom))
                                depth += z2
                            light = {'up': 1.0, 'down': 0.55, 'north': 0.85, 'south': 0.75, 'east': 0.7, 'west': 0.9}[fname]
                            quads.append((depth, pts, shade(col[:3], light)))
        img = Image.new('RGB', size, (232, 226, 214))
        d = ImageDraw.Draw(img)
        d.ellipse((size[0] / 2 - 70, size[1] - 52, size[0] / 2 + 70, size[1] - 28), fill=(210, 204, 192))
        for depth, pts, col in sorted(quads, key=lambda q: -q[0]):
            d.polygon(pts, fill=col)
        d.text((8, 6), title, fill=(40, 40, 40))
        tiles.append(img)
    return tiles


def main(out='/home/claude/pigeon/out'):
    import os
    os.makedirs(out, exist_ok=True)
    bones = build()
    pack(bones)
    tex = paint(bones)
    geo = geo_json(bones)
    anims = animations()
    json.dump(geo, open(f'{out}/pigeon.geo.json', 'w'), indent=1)
    json.dump(anims, open(f'{out}/pigeon.animation.json', 'w'))
    tex.save(f'{out}/pigeon.png')
    import diaper
    for name in diaper.PATTERNS:
        diaper.paint_overlay(bones, (TEX_W, TEX_H), name).save(f'{out}/pigeon_diaper_{name}.png')
    return geo, tex, anims


if __name__ == '__main__':
    main()
