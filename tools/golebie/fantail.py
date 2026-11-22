"""Brown fantail pigeon (pawik) for GeckoLib / Blockbench, built on the pigeon tools.
Upright body with a puffed chest, short neck held back, small head, a vertical fan of tail feathers rising above the
head, and low-carried wings.  Double resolution like the pigeon (render at ~0.35-0.4).  Front -Z, bird's right -X."""
import json, math, random
from PIL import Image

import pigeon as PG
from pigeon import Cube, Bone, mirror, dims, faces, shade, anim, keys, TAU

S = math.sin
PG.TEX_W, PG.TEX_H = 128, 128

C = dict(                       # colours from the video: chestnut body, mauve-grey head, grey-taupe fan
    body=(150, 90, 64), body2=(126, 74, 52), body3=(168, 108, 80), chest=(146, 92, 70),
    head=(130, 106, 100), head2=(114, 92, 88),
    wing=(156, 96, 68), scale_hi=(180, 122, 92), scale_lo=(118, 70, 50), white=(240, 236, 228), prim=(112, 70, 52),
    tail=(152, 140, 130), tail_band=(176, 166, 156), tail_tip=(116, 104, 98), shaft=(124, 112, 104),
    beak=(224, 176, 168), beak2=(198, 146, 140), cere=(242, 240, 236), cere2=(222, 218, 212),
    ring=(150, 116, 104), iris=(226, 128, 50), pupil=(24, 16, 12),
    leg=(214, 112, 104), leg2=(182, 84, 80), claw=(70, 60, 58), thigh=(140, 88, 66),
    irid_g=(88, 128, 92), irid_p=(128, 92, 126), diaper=(0, 0, 0),
)

FAN_N = 21          # long tail feathers in the fan
FAN_SPREAD = 68     # degrees either side of vertical


def build():
    B = []

    def bone(*a, **k):
        b = Bone(*a, **k)
        B.append(b)
        return b

    bone('root', [0, 0, 0])
    body = bone('body', [0, 7.5, 1], 'root')
    body.add(Cube([-3.5, 6, -3], [7, 10, 6], 'body', 'body'))                                   # upright torso
    body.add(Cube([-3.75, 8.5, -5.6], [7.5, 8, 3], 'chest', 'chest', inflate=0.7))              # puffed breast
    body.add(Cube([-3, 10, -7], [6, 6, 2], 'chest', 'chest', inflate=0.5))                       # the proud, pushed-out front
    body.add(Cube([-3, 14.5, -6], [6, 3.5, 3.5], 'chest', 'crop', inflate=0.5))                  # crop pushed forward
    body.add(Cube([-3, 5.4, -2.5], [6, 2, 5.5], 'body2', 'belly'))
    body.add(Cube([-3, 7, 2], [6, 5, 1.2], 'body', 'rump'))
    neck = bone('neck', [0, 16.5, -1], 'body', rot=[-22, 0, 0])                                 # short neck held back
    neck.add(Cube([-2.4, 16, -3.4], [4.8, 4.5, 4.4], 'head', 'neck', inflate=0.2))
    head = bone('head', [0, 20, -1.2], 'neck', rot=[12, 0, 0])
    head.add(Cube([-2, 19.6, -4], [4, 4, 4.5], 'head', 'head'))
    head.add(Cube([-1.5, 23.5, -3.5], [3, 0.6, 3.5], 'head', None))                             # rounded crown
    head.add(Cube([-0.9, 21.4, -5.3], [1.8, 0.9, 1.4], 'cere', 'cere'))
    head.add(Cube([-0.55, 20.6, -6.6], [1.1, 0.9, 2.4], 'beak', 'beak'))
    head.add(Cube([-0.4, 20.2, -6.9], [0.8, 0.8, 0.6], 'beak2', None, rot=[-25, 0, 0], pivot=[0, 20.6, -6.6]))
    for side in (-1, 1):
        x = 2 - 0.6 if side > 0 else -2 - 0.4
        head.add(Cube([x, 20.6, -3.4], [1, 3, 3], 'ring', 'eye'))
    jaw = bone('jaw', [0, 20.6, -4.8], 'head')
    jaw.add(Cube([-0.45, 20.1, -6.4], [0.9, 0.5, 1.9], 'beak2', None))
    # ---- wings: carried low, primaries reaching almost to the ground
    wl = bone('wing_l', [3.5, 15.5, -0.5], 'body')
    wl.add(Cube([3.3, 7, -3], [1.2, 8.5, 6.5], 'wing', 'wing'))
    wl.add(Cube([2.9, 12.5, -3.4], [1, 3.4, 4.5], 'body3', 'shoulder', inflate=0.15))
    wlt = bone('wing_l_tip', [3.9, 7.5, 0.5], 'wing_l')
    wlt.add(Cube([3.5, 2.2, -1.5], [1, 6, 5], 'prim', 'primaries'))
    wr = bone('wing_r', [-3.5, 15.5, -0.5], 'body')
    wrt = bone('wing_r_tip', [-3.9, 7.5, 0.5], 'wing_r')
    wr.cubes = [mirror(c) for c in wl.cubes]
    wrt.cubes = [mirror(c) for c in wlt.cubes]
    # ---- the fan: a vertical half-circle of feathers behind the body, cupped forward at the edges
    pv = [0, 10, 3.4]
    tail = bone('tail', pv, 'body', rot=[-10, 0, 0])
    tl = bone('tail_l', pv, 'tail')
    tr = bone('tail_r', pv, 'tail')
    for i in range(FAN_N):
        a = -FAN_SPREAD + 2 * FAN_SPREAD * i / (FAN_N - 1)
        if abs(a) < 1:
            continue                                   # the fan parts in the middle into two lobes
        a += 5 * (1 if a > 0 else -1)
        z = pv[2] + 1.2 * math.cos(math.radians(a * 1.2)) - 0.6 + 0.03 * (i % 2)   # centre further back, edges cupped forward
        length = 17 - 5 * (abs(a) / FAN_SPREAD) ** 1.5
        c = Cube([-1.35, pv[1], z], [2.7, length, 0.5], 'tail', 'feather', rot=[0, 0, a], pivot=[0, pv[1], z])
        (tail if abs(a) < 12 else (tr if a > 0 else tl)).cubes.append(c)
    for i in range(9):                                                         # inner row of shorter coverts
        a = -60 + 120 * i / 8
        z = pv[2] - 0.6
        c = Cube([-1.4, pv[1], z], [2.8, 7, 0.5], 'body3', 'covert', rot=[0, 0, a], pivot=[0, pv[1], z])
        (tail if abs(a) < 12 else (tr if a > 0 else tl)).cubes.append(c)
    # ---- diaper: panel over the shoulders and down the back to the fan, loops in front of the wings, pouch under the vent
    dp = bone('diaper', [0, 7.5, 1], 'body')
    dp.add(Cube([-4.2, 15.6, 0], [8.4, 1, 4], 'diaper', 'd_panel', inflate=0.1),
           Cube([-4.2, 9.5, 3], [8.4, 7, 1], 'diaper', 'd_panel', inflate=0.1),
           Cube([-4.6, 9.5, -1], [1, 7, 4], 'diaper', 'd_panel'), Cube([3.6, 9.5, -1], [1, 7, 4], 'diaper', 'd_panel'),
           Cube([-3.9, 8, -3.9], [1, 8.5, 1], 'diaper', 'd_strap'), Cube([2.9, 8, -3.9], [1, 8.5, 1], 'diaper', 'd_strap'),
           Cube([-2.6, 5, -0.5], [5.2, 2.5, 4.4], 'diaper', 'd_pouch', inflate=0.3),
           Cube([-4.4, 9, 3.2], [8.8, 1, 1], 'diaper', 'd_edge'),
           Cube([3.0, 16.3, 1.5], [1, 1, 1], 'diaper', 'd_clip'))
    # ---- legs (standing tall, on its toes)
    for side, name in ((-1, 'leg_r'), (1, 'leg_l')):
        lx = 1.9 * side
        leg = bone(name, [lx, 6.6, -0.3], 'root')
        leg.add(Cube([lx - 1.25, 4.4, -1.5], [2.5, 2.6, 2.5], 'thigh', 'thigh', inflate=0.2),
                Cube([lx - 0.5, 0.8, -0.8], [1, 4, 1], 'leg', 'tarsus'))
        foot = bone('foot' + name[3:], [lx, 0.8, -0.3], name)
        foot.add(Cube([lx - 0.45, 0, -4.2], [0.9, 0.8, 4], 'leg', 'toe'),
                 Cube([lx - 0.4, 0, -3.5], [0.8, 0.8, 3.3], 'leg', 'toe', rot=[0, 34, 0], pivot=[lx, 0.4, -0.3]),
                 Cube([lx - 0.4, 0, -3.5], [0.8, 0.8, 3.3], 'leg', 'toe', rot=[0, -34, 0], pivot=[lx, 0.4, -0.3]),
                 Cube([lx - 0.4, 0, -0.1], [0.8, 0.7, 1.8], 'leg', 'toe'),
                 Cube([lx - 0.3, -0.05, -4.8], [0.6, 0.6, 0.7], 'claw', None))
    return B


def paint(bones, seed=11):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (PG.TEX_W, PG.TEX_H), (0, 0, 0, 0))
    px = img.load()

    def put(r, x, y, col):
        if 0 <= x < r[2] and 0 <= y < r[3]:
            px[r[0] + x, r[1] + y] = tuple(col) + (255,)

    def fill(r, col, noise=0.035, vgrad=0.0):
        for y in range(r[3]):
            for x in range(r[2]):
                put(r, x, y, shade(col, 1 + rnd.uniform(-noise, noise) + vgrad * (0.5 - y / max(1, r[3] - 1))))

    for b in bones:
        for c in b.cubes:
            if c.mat == 'diaper':
                continue
            F = faces(c)
            for n, r in F.items():
                fill(r, shade(C[c.mat], {'up': 1.06, 'down': 0.84}.get(n, 1.0)), vgrad=0.08 if n not in ('up', 'down') else 0)
            d = c.deco
            if d == 'eye':
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(3):
                        for x in range(3):
                            put(r, x, y, C['ring'])
                    for x, y in ((1, 0), (0, 1), (2, 1), (1, 2)):
                        put(r, x, y, C['iris'])
                    put(r, 1, 1, C['pupil'])
                    put(r, 2, 0, (250, 246, 240))
                for n in ('north', 'south', 'up', 'down'):
                    fill(F[n], C['ring'], 0.02)
            elif d == 'cere':
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            if rnd.random() < 0.3:
                                put(r, x, y, C['cere2'])
            elif d == 'beak':
                for n, r in F.items():
                    for x in range(r[2]):
                        put(r, x, 0, shade(C['beak'], 1.1))
                    put(r, 0, r[3] - 1, C['beak2'])
            if d in ('crop', 'neck'):
                for n in ('east', 'west', 'north'):
                    r = F[n]
                    for y in range(r[3]):
                        for x in range(r[2]):
                            q = rnd.random()
                            if q < 0.22:
                                put(r, x, y, shade(C['irid_g'], rnd.uniform(0.85, 1.05)))
                            elif q < 0.36:
                                put(r, x, y, shade(C['irid_p'], rnd.uniform(0.85, 1.05)))
            if d in ('chest', 'crop', 'neck'):
                # soft, slightly glossy breast feathers with faint darker edges
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            if (x + 2 * y) % 4 == 0 and rnd.random() < 0.6:
                                put(r, x, y, shade(C['body2'], rnd.uniform(0.95, 1.05)))
                            elif rnd.random() < 0.08:
                                put(r, x, y, shade(C['body3'], 1.03))
            elif d == 'wing':
                # scalloped coverts: lighter feathers with darker rims, and the white line along the front edge
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(r[3]):
                        for x in range(r[2]):
                            fx, fy = x % 3, (y + (x // 3) % 2) % 3
                            col = C['scale_hi'] if (fx == 1 and fy == 1) else (C['scale_lo'] if fy == 2 else C['wing'])
                            put(r, x, y, shade(col, rnd.uniform(0.96, 1.04)))
                    for y in range(1, r[3] // 2):
                        xx = min(r[2] - 1, 1 + y // 3)              # a thin white line curving back over the shoulder
                        put(r, xx if n == 'east' else r[2] - 1 - xx, y, C['white'])
                    for x in range(r[2]):
                        put(r, x, 0, shade(C['body3'], 1.02))
            elif d == 'shoulder':
                for n, r in F.items():
                    for y in range(r[3]):
                        for x in range(r[2]):
                            if (x + y) % 3 == 0:
                                put(r, x, y, C['scale_hi'])
            elif d == 'primaries':
                for n in ('east', 'west'):
                    r = F[n]
                    for y in range(r[3]):
                        for x in range(r[2]):
                            put(r, x, y, shade(C['prim'] if x % 2 else C['wing'], rnd.uniform(0.94, 1.04) - 0.15 * y / r[3]))
            elif d in ('feather', 'covert'):
                # each fan feather: brown vane, darker shaft, a pale band near the end and a darker rounded tip
                for n in ('north', 'south'):
                    r = F[n]
                    L = r[3]
                    for y in range(L):
                        f = 1 - y / max(1, L - 1)          # 1 at the tip (top row), 0 at the base
                        for x in range(r[2]):
                            col = C['tail']
                            if d == 'feather' and 0.62 < f < 0.84:
                                col = C['tail_band']
                            elif d == 'feather' and f >= 0.90:
                                col = C['tail_tip']
                            elif d == 'covert':
                                col = C['body3']
                            put(r, x, y, shade(col, rnd.uniform(0.95, 1.05)))
                        put(r, r[2] // 2 if r[2] > 2 else 1, y, C['shaft'] if f < 0.92 else C['tail_tip'])
                    if d == 'feather':
                        for y in range(int(L * 0.65)):
                            for x in (0, r[2] - 1):
                                if rnd.random() < 0.22:
                                    px[r[0] + x, r[1] + y] = (0, 0, 0, 0)       # ragged, uneven vane
                        px[r[0], r[1]] = (0, 0, 0, 0)
                        px[r[0] + r[2] - 1, r[1]] = (0, 0, 0, 0)
                for n in ('east', 'west', 'up', 'down'):
                    fill(F[n], C['shaft'], 0.03)
            elif d == 'tarsus':
                for n, r in F.items():
                    for y in range(1, r[3], 2):
                        for x in range(r[2]):
                            put(r, x, y, C['leg2'])
            elif d == 'toe':
                for n, r in F.items():
                    for x in range(1, r[2], 2):
                        for y in range(r[3]):
                            put(r, x, y, C['leg2'])
            elif d == 'head':
                r = F['north']
                for _ in range(3):
                    put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), C['head2'])
    return img


# ------------------------------------------------------------------------------------------- animations
def animations():
    A = {}
    w = lambda t, p, a, ph=0.0: a * S(TAU * t / p + ph)

    def shiver(t, on=True, amp=2.5):
        """The fantail's famous neck tremble."""
        return amp * S(TAU * t * 11) if on else 0

    fan_open = lambda k: {'tail_l': {'rotation': lambda t: [0, 0, -k]}, 'tail_r': {'rotation': lambda t: [0, 0, k]}}

    A['idle'] = anim(4.0, True, {
        'body': {'scale': lambda t: [1 + w(t, 2, 0.015), 1 + w(t, 2, 0.015), 1 + w(t, 2, 0.02)]},
        'neck': {'rotation': lambda t: [shiver(t, 0.6 < t < 1.6, 2.2), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (2.0, 0), (2.1, -8), (2.8, -8), (2.9, 0), (4, 0)]),
                                        keys(t, [(0, 0), (2.0, 0), (2.1, 30), (2.8, 30), (2.9, 0), (3.2, 0), (3.3, -25), (3.8, -25), (3.9, 0), (4, 0)]),
                                        keys(t, [(0, 0), (2.1, 0), (2.2, 10), (2.8, 10), (2.9, 0), (4, 0)])]},
        'tail': {'rotation': lambda t: [w(t, 4, 1.5), 0, w(t, 4, 2)]},
    })
    A['walk'] = anim(0.5, True, {             # short mincing steps on tiptoe, the fan swaying
        'leg_r': {'rotation': lambda t: [w(t, 0.5, 22), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.5, -22), 0, 0]},
        'foot_r': {'rotation': lambda t: [max(0, w(t, 0.5, -15)), 0, 0]},
        'foot_l': {'rotation': lambda t: [max(0, w(t, 0.5, 15)), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.5, 0.45)), 0]},
        'body': {'rotation': lambda t: [0, w(t, 0.5, 3), w(t, 0.5, 4)]},
        'neck': {'rotation': lambda t: [w(t, 0.25, 3), 0, 0]},
        'tail': {'rotation': lambda t: [0, w(t, 0.5, -4), w(t, 0.5, -5)]},
        'wing_l': {'rotation': lambda t: [0, 0, w(t, 0.25, 2)]},
        'wing_r': {'rotation': lambda t: [0, 0, -w(t, 0.25, 2)]},
    })
    A['run'] = anim(0.34, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.34, 34), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.34, -34), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.34, 0.8)), 0]},
        'body': {'rotation': lambda t: [18, w(t, 0.34, 4), w(t, 0.34, 4)]},
        'neck': {'rotation': lambda t: [8, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 14 + w(t, 0.17, 5)]},
        'wing_r': {'rotation': lambda t: [0, 0, -14 - w(t, 0.17, 5)]},
        'tail': {'rotation': lambda t: [-10, 0, w(t, 0.34, -6)]},
    })
    A['hop'] = anim(0.5, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.08, -0.8), (0.25, 3), (0.45, 0), (0.5, 0)]), 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.15, 45), (0.3, 10), (0.5, 0)])]},
        'wing_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.15, 45), (0.3, 10), (0.5, 0)])]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 25), (0.45, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 25), (0.45, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -8), (0.5, 0)]), 0, 0]},
    })
    bend = lambda t, a, t0, t1, L: keys(t, [(0, 0), (t0, a), (t1, a), (L, 0)])
    A['peck'] = anim(1.4, False, {
        'body': {'rotation': lambda t: [bend(t, 38, 0.25, 1.15, 1.4), 0, 0]},
        'neck': {'rotation': lambda t: [bend(t, 50, 0.25, 1.15, 1.4), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 20), (0.35, 45), (0.43, 18), (0.6, 46), (0.68, 18), (0.85, 45), (0.93, 20), (1.15, 20), (1.4, 0)]), 0, 0]},
        'jaw': {'rotation': lambda t: [keys(t, [(0, 0), (0.33, 0), (0.35, 18), (0.41, 0), (0.58, 0), (0.6, 18), (0.66, 0), (0.83, 0), (0.85, 18), (0.91, 0), (1.4, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [bend(t, -12, 0.25, 1.15, 1.4), 0, 0]},
        'leg_r': {'rotation': lambda t: [bend(t, -20, 0.25, 1.15, 1.4), 0, 0]},
        'leg_l': {'rotation': lambda t: [bend(t, -20, 0.25, 1.15, 1.4), 0, 0]},
    })
    A['drink'] = anim(2.2, False, {
        'body': {'rotation': lambda t: [bend(t, 40, 0.35, 1.9, 2.2), 0, 0]},
        'neck': {'rotation': lambda t: [bend(t, 62, 0.35, 1.9, 2.2), 0, 0],
                 'scale': lambda t: [1, 1 + (w(t, 0.4, 0.05) if 0.45 < t < 1.8 else 0), 1]},
        'head': {'rotation': lambda t: [bend(t, 28, 0.35, 1.9, 2.2), 0, 0]},
        'leg_r': {'rotation': lambda t: [bend(t, -20, 0.35, 1.9, 2.2), 0, 0]},
        'leg_l': {'rotation': lambda t: [bend(t, -20, 0.35, 1.9, 2.2), 0, 0]},
    })
    # ---- display: chest out, head thrown back against the fan, trembling, fan fully open
    def bow(t, p):
        f = (t % p) / p
        return keys(f, [(0, 0), (0.3, 1), (0.5, 1), (0.75, 0), (1, 0)])
    disp = lambda extra: dict({
        'body': {'scale': lambda t: [1.08, 1.06, 1.18], 'rotation': lambda t: [-6 + 14 * bow(t, 1.0), 0, 0]},
        'neck': {'rotation': lambda t: [-18 + shiver(t, True, 3), 0, 0], 'scale': lambda t: [1.2, 1.05, 1.15]},
        'head': {'rotation': lambda t: [-22 + 18 * bow(t, 1.0), 0, 0]},
        'tail': {'rotation': lambda t: [8, 0, 0], 'scale': lambda t: [1.05, 1.08, 1]},
        'tail_l': {'rotation': lambda t: [0, 0, -9]},
        'tail_r': {'rotation': lambda t: [0, 0, 9]},
        'wing_l': {'rotation': lambda t: [8, 0, 10]},
        'wing_r': {'rotation': lambda t: [8, 0, -10]},
        'wing_l_tip': {'rotation': lambda t: [-8, 0, 0]},
        'wing_r_tip': {'rotation': lambda t: [-8, 0, 0]},
    }, **extra)
    A['display'] = anim(2.0, True, disp({}))
    A['coo'] = A['display']
    A['strut'] = anim(3.2, True, disp({
        'root': {'rotation': lambda t: [0, -360 * t / 3.2, 0]},
        'leg_r': {'rotation': lambda t: [w(t, 0.4, 16), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.4, -16), 0, 0]},
    }))
    def circle(direction, L=2.6):
        return anim(L, True, {
            'root': {'rotation': lambda t: [0, -direction * 360 * t / L, 0]},
            'leg_r': {'rotation': lambda t: [w(t, 0.32, 20), 0, -direction * 6]},
            'leg_l': {'rotation': lambda t: [w(t, 0.32, -20), 0, -direction * 6]},
            'body': {'rotation': lambda t: [16 * bow(t, 0.9), 0, direction * 7], 'scale': lambda t: [1.06, 1.04, 1.12]},
            'neck': {'rotation': lambda t: [-10 + 34 * bow(t, 0.9) + shiver(t, True, 2), 0, 0], 'scale': lambda t: [1.15, 1.05, 1.12]},
            'head': {'rotation': lambda t: [-12 + 26 * bow(t, 0.9), direction * 12, 0]},
            'tail': {'rotation': lambda t: [-6, -direction * 14, -direction * 8]},       # the fan leans out of the turn
            'tail_l': {'rotation': lambda t: [0, 0, -6]},
            'tail_r': {'rotation': lambda t: [0, 0, 6]},
            'wing_l': {'rotation': lambda t: [6, 0, 9 + w(t, 0.45, 2)]},
            'wing_r': {'rotation': lambda t: [6, 0, -9 - w(t, 0.45, 2)]},
        })
    A['circle'] = circle(1)
    A['circle_ccw'] = circle(-1)

    # running up: leaning forward, head low, the fan laid back and lowered behind it
    A['runup'] = anim(0.34, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.34, 36), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.34, -36), 0, 0]},
        'foot_r': {'rotation': lambda t: [max(0, w(t, 0.34, -20)), 0, 0]},
        'foot_l': {'rotation': lambda t: [max(0, w(t, 0.34, 20)), 0, 0]},
        'root': {'position': lambda t: [0, abs(w(t, 0.34, 0.8)), 0]},
        'body': {'rotation': lambda t: [34, w(t, 0.34, 4), 0]},
        'neck': {'rotation': lambda t: [34, 0, 0]},
        'head': {'rotation': lambda t: [-6, 0, 0]},
        'tail': {'rotation': lambda t: [-58, 0, w(t, 0.34, -4)], 'scale': lambda t: [0.9, 0.95, 1]},
        'tail_l': {'rotation': lambda t: [0, 0, 12]},
        'tail_r': {'rotation': lambda t: [0, 0, -12]},
        'wing_l': {'rotation': lambda t: [0, 0, 10 + w(t, 0.17, 4)]},
        'wing_r': {'rotation': lambda t: [0, 0, -10 - w(t, 0.17, 4)]},
    })
    # being stroked: bows deep into the hand, neck stretched forward, the fan swung back to balance
    A['petted'] = anim(2.4, True, {
        'root': {'position': lambda t: [0, -0.8, 0]},
        'body': {'rotation': lambda t: [38 + w(t, 2.4, 3), 0, w(t, 2.4, 2)], 'scale': lambda t: [1.06, 1.04, 1.1]},
        'neck': {'rotation': lambda t: [44 + w(t, 1.2, 4), 0, w(t, 2.4, 5)], 'scale': lambda t: [1.1, 1.1, 1.1]},
        'head': {'rotation': lambda t: [20, 0, w(t, 2.4, 8, 1.0)]},
        'tail': {'rotation': lambda t: [-30, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 6]},
        'wing_r': {'rotation': lambda t: [0, 0, -6]},
        'leg_r': {'rotation': lambda t: [-16, 0, 0]},
        'leg_l': {'rotation': lambda t: [-16, 0, 0]},
    })
    def nibble(side, L=2.6):
        s_ = -1 if side == 'l' else 1
        pecks = [0.35, 0.5, 0.65, 0.8, 1.35, 1.48, 1.61]
        def thrust(t):
            return min([abs(t - p) for p in pecks]) < 0.05
        return anim(L, True, {
            'root': {'rotation': lambda t: [0, s_ * 28, 0]},
            'body': {'rotation': lambda t: [8, 0, 0]},
            'neck': {'rotation': lambda t: [-8 + (12 if thrust(t) else 0), s_ * keys(t, [(0, 40), (1.0, 40), (1.1, 10), (1.2, 40), (2.0, 40), (2.1, -10), (2.35, -10), (2.5, 40), (2.6, 40)]), 0],
                     'position': lambda t: [0, 0, -1.2 if thrust(t) else 0]},
            'head': {'rotation': lambda t: [(16 if thrust(t) else 0), 0, s_ * keys(t, [(0, 0), (2.05, 0), (2.15, 18), (2.4, 18), (2.5, 0), (2.6, 0)])]},
            'jaw': {'rotation': lambda t: [14 if thrust(t) else 0, 0, 0]},
            'tail': {'rotation': lambda t: [-6, -s_ * 8, 0]},
        }, fps=40)
    A['nibble_l'] = nibble('l')
    A['nibble_r'] = nibble('r')
    A['bow'] = anim(1.4, False, {
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.55, 30), (0.85, 30), (1.25, 0)]), 0, 0], 'scale': lambda t: [1.06, 1.05, 1.14]},
        'neck': {'rotation': lambda t: [keys(t, [(0, -10), (0.55, 40), (0.85, 40), (1.25, -10), (1.4, -10)]) + shiver(t, True, 2.5), 0, 0],
                 'scale': lambda t: [1.2, 1.05, 1.15]},
        'head': {'rotation': lambda t: [keys(t, [(0, -15), (0.55, 20), (0.85, 20), (1.25, -15), (1.4, 0)]), 0, 0]},
        'tail': {'rotation': lambda t: [keys(t, [(0, 4), (0.55, -14), (0.85, -14), (1.25, 4), (1.4, 0)]), 0, 0]},
        'tail_l': {'rotation': lambda t: [0, 0, -8]},
        'tail_r': {'rotation': lambda t: [0, 0, 8]},
    })
    A['beg'] = anim(2.0, True, {
        'body': {'rotation': lambda t: [18, 0, 0]},
        'neck': {'rotation': lambda t: [18, 0, 0]},
        'head': {'rotation': lambda t: [-30, w(t, 2, 10), w(t, 2, 6)]},
        'wing_l': {'rotation': lambda t: [0, 0, 7 + w(t, 0.12, 2)]},
        'wing_r': {'rotation': lambda t: [0, 0, -7 - w(t, 0.12, 2)]},
        'tail': {'rotation': lambda t: [-14, 0, w(t, 0.1, 1.5)]},
    })
    A['tail_shiver'] = anim(1.2, False, {      # fan quivers, as fantails do when excited
        'tail': {'rotation': lambda t: [w(t, 0.08, 1.5) * keys(t, [(0, 0), (0.15, 1), (1.0, 1), (1.2, 0)]), 0,
                                        w(t, 0.09, 3) * keys(t, [(0, 0), (0.15, 1), (1.0, 1), (1.2, 0)])]},
        'tail_l': {'rotation': lambda t: [0, 0, -6 + w(t, 0.07, 3)]},
        'tail_r': {'rotation': lambda t: [0, 0, 6 - w(t, 0.07, 3)]},
        'neck': {'rotation': lambda t: [-8 + shiver(t, 0.1 < t < 1.1, 3), 0, 0]},
        'body': {'scale': lambda t: [1.04, 1.03, 1.08]},
    })
    # ---- flight: the body tips forward to horizontal and the fan closes into a trailing tail
    def flap(t, p, base=88, amp=55, ph=0.0):
        return base + amp * S(TAU * t / p + ph)
    flight_common = {
        'body': {'rotation': lambda t: [66, 0, 0]},
        'neck': {'rotation': lambda t: [-8, 0, 0]},
        'head': {'rotation': lambda t: [-50, 0, 0]},
        'tail': {'rotation': lambda t: [-148, 0, 0], 'scale': lambda t: [0.8, 0.85, 1]},     # fan swung back to trail flat
        'tail_l': {'rotation': lambda t: [0, 0, 32]},
        'tail_r': {'rotation': lambda t: [0, 0, -32]},
        'leg_r': {'rotation': lambda t: [80, 0, 0]},
        'leg_l': {'rotation': lambda t: [80, 0, 0]},
        'foot_r': {'rotation': lambda t: [-70, 0, 0]},
        'foot_l': {'rotation': lambda t: [-70, 0, 0]},
        'root': {'position': lambda t: [0, 3, 0]},
    }
    A['fly'] = anim(0.34, True, dict(flight_common, **{
        'wing_l': {'rotation': lambda t: [0, 0, flap(t, 0.34)]},
        'wing_r': {'rotation': lambda t: [0, 0, -flap(t, 0.34)]},
        'wing_l_tip': {'rotation': lambda t: [0, 0, w(t, 0.34, 28, -0.9)]},
        'wing_r_tip': {'rotation': lambda t: [0, 0, -w(t, 0.34, 28, -0.9)]},
        'root': {'position': lambda t: [0, 3 + w(t, 0.34, 0.6, 1.6), 0]},
    }))
    A['glide'] = anim(2.0, True, dict(flight_common, **{
        'wing_l': {'rotation': lambda t: [0, 0, 98 + w(t, 1, 3)]},
        'wing_r': {'rotation': lambda t: [0, 0, -98 - w(t, 1, 3)]},
        'wing_l_tip': {'rotation': lambda t: [0, 0, -6]},
        'wing_r_tip': {'rotation': lambda t: [0, 0, 6]},
        'body': {'rotation': lambda t: [66, 0, w(t, 2, 5)]},
        'tail_l': {'rotation': lambda t: [0, 0, 18]},
        'tail_r': {'rotation': lambda t: [0, 0, -18]},
    }))
    A['takeoff'] = anim(0.6, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.12, -1.2), (0.35, 1.5), (0.6, 3)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, 12), (0.6, 66)]), 0, 0]},
        'neck': {'rotation': lambda t: [keys(t, [(0, 0), (0.6, -8)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.6, -50)]), 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.12, 60), (0.28, 150), (0.45, 40), (0.6, 140)])]},
        'wing_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.12, 60), (0.28, 150), (0.45, 40), (0.6, 140)])]},
        'tail_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.6, 32)])]},
        'tail_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 0), (0.6, 32)])]},
        'tail': {'scale': lambda t: [keys(t, [(0, 1), (0.6, 0.8)]), keys(t, [(0, 1), (0.6, 0.85)]), 1],
                 'rotation': lambda t: [keys(t, [(0, 0), (0.15, 0), (0.6, -148)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -20), (0.35, 50), (0.6, 80)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -20), (0.35, 50), (0.6, 80)]), 0, 0]},
        'foot_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.35, 0), (0.6, -70)]), 0, 0]},
        'foot_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.35, 0), (0.6, -70)]), 0, 0]},
    })
    A['land'] = anim(0.75, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 3), (0.4, 1), (0.6, 0)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 66), (0.3, -10), (0.55, 6), (0.75, 0)]), 0, 0]},
        'neck': {'rotation': lambda t: [keys(t, [(0, -8), (0.3, 10), (0.75, 0)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, -50), (0.3, -10), (0.75, 0)]), 0, 0]},
        'wing_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -20), (0.75, 0)]), 0, keys(t, [(0, 100), (0.15, 140), (0.3, 70), (0.5, 120), (0.75, 0)])]},
        'wing_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -20), (0.75, 0)]), 0, -keys(t, [(0, 100), (0.15, 140), (0.3, 70), (0.5, 120), (0.75, 0)])]},
        'tail_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 32), (0.3, -10), (0.75, 0)])]},
        'tail_r': {'rotation': lambda t: [0, 0, -keys(t, [(0, 32), (0.3, -10), (0.75, 0)])]},
        'tail': {'scale': lambda t: [keys(t, [(0, 0.8), (0.3, 1.05), (0.75, 1)]), keys(t, [(0, 0.85), (0.3, 1), (0.75, 1)]), 1],
                 'rotation': lambda t: [keys(t, [(0, -148), (0.35, -20), (0.75, 0)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 80), (0.3, -30), (0.55, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 80), (0.3, -30), (0.55, 0)]), 0, 0]},
        'foot_r': {'rotation': lambda t: [keys(t, [(0, -70), (0.25, 20), (0.55, 0)]), 0, 0]},
        'foot_l': {'rotation': lambda t: [keys(t, [(0, -70), (0.25, 20), (0.55, 0)]), 0, 0]},
    })
    # ---- grooming and rest
    A['preen'] = anim(2.6, False, {
        'neck': {'rotation': lambda t: [bend(t, 20, 0.35, 2.2, 2.6), bend(t, 120, 0.35, 2.2, 2.6), 0]},
        'head': {'rotation': lambda t: [bend(t, 35, 0.35, 2.2, 2.6) + (w(t, 0.22, 10) if 0.5 < t < 2.1 else 0), 0, bend(t, 20, 0.35, 2.2, 2.6)]},
        'jaw': {'rotation': lambda t: [(max(0, w(t, 0.22, 15)) if 0.5 < t < 2.1 else 0), 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, bend(t, 20, 0.4, 2.1, 2.6)]},
        'body': {'rotation': lambda t: [0, 0, bend(t, -6, 0.4, 2.1, 2.6)]},
    })
    A['shake'] = anim(1.1, False, {
        'body': {'scale': lambda t: [keys(t, [(0, 1), (0.2, 1.16), (0.9, 1.13), (1.1, 1)])] * 3,
                 'rotation': lambda t: [0, (w(t, 0.12, 10) if 0.2 < t < 0.85 else 0), 0]},
        'neck': {'scale': lambda t: [keys(t, [(0, 1), (0.2, 1.15), (0.9, 1.1), (1.1, 1)])] * 3,
                 'rotation': lambda t: [0, (w(t, 0.12, -16) if 0.2 < t < 0.85 else 0), 0]},
        'wing_l': {'rotation': lambda t: [0, 0, (12 + w(t, 0.12, 6)) if 0.2 < t < 0.85 else 0]},
        'wing_r': {'rotation': lambda t: [0, 0, -(12 + w(t, 0.12, 6)) if 0.2 < t < 0.85 else 0]},
        'tail': {'rotation': lambda t: [0, (w(t, 0.1, 10) if 0.75 < t < 1.05 else 0), (w(t, 0.1, 8) if 0.75 < t < 1.05 else 0)]},
    })
    A['sit'] = anim(4.0, True, {
        'root': {'position': lambda t: [0, -4, 0]},
        'body': {'scale': lambda t: [1.08, 1.03 + w(t, 2, 0.015), 1.1]},
        'neck': {'rotation': lambda t: [-6, 0, 0]},
        'head': {'rotation': lambda t: [0, keys(t, [(0, 0), (1.2, 0), (1.3, 28), (2.2, 28), (2.3, 0), (4, 0)]), 0]},
        'leg_r': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'leg_l': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'wing_l_tip': {'rotation': lambda t: [75, 0, 0]},
        'wing_r_tip': {'rotation': lambda t: [75, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 6]},
        'wing_r': {'rotation': lambda t: [0, 0, -6]},
    })
    A['sleep'] = anim(4.0, True, {
        'root': {'position': lambda t: [0, -4, 0]},
        'body': {'scale': lambda t: [1.14, 1.08 + w(t, 4, 0.03), 1.16]},
        'neck': {'rotation': lambda t: [-30, 140, 0], 'position': lambda t: [0, -1.5, 1], 'scale': lambda t: [1.15, 0.75, 1.15]},
        'head': {'rotation': lambda t: [12, 0, 22]},
        'leg_r': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'leg_l': {'scale': lambda t: [0.01, 0.01, 0.01]},
        'tail_l': {'rotation': lambda t: [0, 0, 8]},
        'tail_r': {'rotation': lambda t: [0, 0, -8]},
        'wing_l_tip': {'rotation': lambda t: [75, 0, 0]},
        'wing_r_tip': {'rotation': lambda t: [75, 0, 0]},
        'wing_l': {'rotation': lambda t: [0, 0, 6]},
        'wing_r': {'rotation': lambda t: [0, 0, -6]},
    })
    A['wing_stretch'] = anim(2.0, False, {
        'wing_l': {'rotation': lambda t: [bend(t, 30, 0.4, 1.6, 2.0), 0, bend(t, 35, 0.4, 1.6, 2.0)]},
        'wing_l_tip': {'rotation': lambda t: [0, 0, bend(t, 15, 0.4, 1.6, 2.0)]},
        'leg_l': {'rotation': lambda t: [bend(t, 50, 0.4, 1.6, 2.0), 0, 0]},
        'tail_l': {'rotation': lambda t: [0, 0, bend(t, -8, 0.4, 1.6, 2.0)]},
        'body': {'rotation': lambda t: [0, 0, bend(t, -5, 0.4, 1.6, 2.0)]},
    })
    A['scratch'] = anim(1.4, False, {
        'leg_r': {'rotation': lambda t: [bend(t, -100, 0.25, 1.15, 1.4) + (w(t, 0.12, 12) if 0.3 < t < 1.1 else 0), 0, -20]},
        'neck': {'rotation': lambda t: [bend(t, 20, 0.25, 1.15, 1.4), 0, bend(t, -25, 0.25, 1.15, 1.4)]},
        'head': {'rotation': lambda t: [0, 0, bend(t, -25, 0.25, 1.15, 1.4)]},
        'body': {'rotation': lambda t: [0, 0, bend(t, 8, 0.25, 1.15, 1.4)]},
    })
    A['look_up'] = anim(1.8, False, {
        'head': {'rotation': lambda t: [bend(t, -10, 0.15, 1.5, 1.8), 0, bend(t, 70, 0.15, 1.5, 1.8)]},
        'neck': {'rotation': lambda t: [bend(t, -6, 0.2, 1.5, 1.8), 0, 0]},
    })
    A['startled'] = anim(0.8, False, {
        'neck': {'rotation': lambda t: [bend(t, 12, 0.1, 0.6, 0.8), 0, 0], 'scale': lambda t: [0.9, keys(t, [(0, 1), (0.1, 1.2), (0.6, 1.2), (0.8, 1)]), 0.9]},
        'body': {'scale': lambda t: [0.95, 1, 0.94]},
        'head': {'rotation': lambda t: [0, keys(t, [(0, 0), (0.2, 25), (0.4, -25), (0.6, 0)]), 0]},
        'tail_l': {'rotation': lambda t: [0, 0, bend(t, 12, 0.1, 0.6, 0.8)]},
        'tail_r': {'rotation': lambda t: [0, 0, -bend(t, 12, 0.1, 0.6, 0.8)]},
        'wing_l': {'rotation': lambda t: [0, 0, bend(t, 8, 0.1, 0.6, 0.8)]},
        'wing_r': {'rotation': lambda t: [0, 0, -bend(t, 8, 0.1, 0.6, 0.8)]},
    })
    return {'format_version': '1.8.0', 'animations': {f'animation.fantail.{k}': v for k, v in A.items()}}


def main(out='/home/claude/pigeon/fantail_out'):
    import os
    os.makedirs(out, exist_ok=True)
    bones = build()
    PG.pack(bones)
    tex = paint(bones)
    geo = PG.geo_json(bones)
    geo['minecraft:geometry'][0]['description']['identifier'] = 'geometry.fantail'
    geo['minecraft:geometry'][0]['description']['visible_bounds_height'] = 3
    anims = animations()
    json.dump(geo, open(f'{out}/fantail.geo.json', 'w'), indent=1)
    json.dump(anims, open(f'{out}/fantail.animation.json', 'w'))
    tex.save(f'{out}/fantail.png')
    import diaper
    for name in diaper.PATTERNS:
        diaper.paint_overlay(bones, (PG.TEX_W, PG.TEX_H), name).save(f'{out}/fantail_diaper_{name}.png')
    return geo, tex, anims


if __name__ == '__main__':
    main()
