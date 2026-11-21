"""GeckoLib goblin: one geo per race (same bone names, built differently), a box-UV texture per race, and one shared
animation file with every action the AI can show.  Coordinates are Bedrock pixels, front is -Z, up is +Y.
The model is built at player-like size and scaled down by the renderer (about 0.5)."""
import math, random
from PIL import Image

TEX = 128

# ------------------------------------------------------------------------------------------------ palettes
PAL = {
    'forest': dict(skin=(104, 150, 64), skin2=(78, 118, 46), skin3=(132, 176, 84), eye=(246, 214, 70), pupil=(40, 24, 12),
                   cloth=(122, 86, 52), cloth2=(96, 66, 40), leather=(70, 48, 30), hood=(58, 98, 46), hood2=(44, 78, 36), metal=(150, 150, 140),
                   teeth=(236, 230, 200), mouth=(48, 26, 22), nail=(54, 70, 30)),
    'stone': dict(skin=(138, 138, 128), skin2=(108, 108, 100), skin3=(164, 162, 150), eye=(255, 150, 40), pupil=(50, 20, 10),
                  cloth=(110, 96, 82), cloth2=(86, 74, 62), leather=(64, 52, 42), hood=(92, 118, 70), hood2=(70, 92, 54), metal=(170, 166, 150),
                  teeth=(232, 226, 210), mouth=(52, 40, 38), nail=(80, 78, 72)),
    'swamp': dict(skin=(96, 122, 70), skin2=(70, 94, 54), skin3=(124, 148, 86), eye=(200, 230, 90), pupil=(20, 30, 10),
                  cloth=(78, 96, 66), cloth2=(58, 74, 50), leather=(64, 56, 36), hood=(112, 86, 50), hood2=(88, 66, 38), metal=(120, 130, 110),
                  teeth=(226, 220, 180), mouth=(40, 34, 24), nail=(48, 58, 34)),
    'cave': dict(skin=(196, 188, 176), skin2=(164, 154, 144), skin3=(220, 214, 204), eye=(150, 220, 255), pupil=(14, 20, 40),
                 cloth=(84, 78, 104), cloth2=(64, 58, 82), leather=(58, 50, 60), hood=(104, 96, 130), hood2=(82, 74, 104), metal=(150, 150, 160),
                 teeth=(240, 236, 226), mouth=(70, 40, 50), nail=(120, 110, 110)),
}


# ------------------------------------------------------------------------------------------------ geometry
class Cube:
    def __init__(self, o, s, mat, deco=None, inflate=0.0, rot=None, pivot=None):
        self.o, self.s, self.mat, self.deco, self.inflate, self.rot, self.pivot = o, s, mat, deco, inflate, rot, pivot
        self.uv = None


class Bone:
    def __init__(self, name, pivot, parent=None, rot=None):
        self.name, self.pivot, self.parent, self.rot, self.cubes = name, pivot, parent, rot, []

    def add(self, *cubes):
        self.cubes.extend(cubes)
        return self


def mirror_x(c):
    o, s = c.o, c.s
    return Cube([-(o[0] + s[0]), o[1], o[2]], list(s), c.mat, c.deco, c.inflate,
                None if c.rot is None else [c.rot[0], -c.rot[1], -c.rot[2]], None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]])


def build(race):
    """Returns the bones of one race.  Right side is -X (as in the Bedrock player)."""
    B = {}

    def bone(name, pivot, parent=None, rot=None):
        b = Bone(name, pivot, parent, rot)
        B[name] = b
        return b

    # proportions per race
    P = dict(forest=dict(leg=(3, 9, 3), body=(8, 9, 5), arm=(3, 10, 3), head=(10, 9, 9), ear=(8, 3, 1), ear_rot=-18, ear_y=0, hunch=6),
             stone=dict(leg=(4, 7, 4), body=(11, 9, 6), arm=(4, 10, 4), head=(10, 8, 9), ear=(4, 3, 1), ear_rot=-6, ear_y=-1, hunch=4),
             swamp=dict(leg=(3, 10, 3), body=(7, 9, 4), arm=(3, 12, 3), head=(11, 7, 9), ear=(6, 4, 1), ear_rot=28, ear_y=0, hunch=14),
             cave=dict(leg=(3, 8, 3), body=(7, 8, 4), arm=(3, 10, 3), head=(11, 10, 10), ear=(10, 6, 1), ear_rot=-8, ear_y=1, hunch=12))[race]
    lw, lh, ld = P['leg']
    bw, bh, bd = P['body']
    aw, ah, ad = P['arm']
    hw, hh, hd = P['head']
    hip = lh
    sh = hip + bh                        # shoulder height
    root = bone('root', [0, 0, 0])
    # ---- legs (with pointy shoes)
    gap = 0.5 if race != 'stone' else 0.5
    lx = gap + lw / 2                     # leg centre x
    leg_r = bone('leg_r', [-lx, hip, 0], 'root')
    leg_r.add(Cube([-lx - lw / 2, 1, -ld / 2], [lw, lh - 1, ld], 'trousers'))
    shoe_len = 2 if race != 'swamp' else 3
    leg_r.add(Cube([-lx - lw / 2 - 0.2, 0, -ld / 2 - shoe_len], [lw + 0.4, 2, ld + shoe_len], 'leather', deco='shoe', inflate=0.1))
    if race != 'stone':
        leg_r.add(Cube([-lx - 0.5, 1.2, -ld / 2 - shoe_len - 1.2], [1, 1, 1.2], 'leather', rot=[22, 0, 0],
                       pivot=[-lx, 1.5, -ld / 2 - shoe_len]))      # curled toe
    leg_l = bone('leg_l', [lx, hip, 0], 'root')
    for c in leg_r.cubes:
        leg_l.cubes.append(mirror_x(c))
    # ---- body
    body = bone('body', [0, hip, 0], 'root', rot=[P['hunch'], 0, 0])
    body.add(Cube([-bw / 2, hip, -bd / 2], [bw, bh, bd], 'shirt', deco='shirt'))
    body.add(Cube([-bw / 2, hip, -bd / 2], [bw, 2, bd], 'leather', deco='belt', inflate=0.35))
    if race == 'forest':
        body.add(Cube([-bw / 2, hip + bh - 3, -bd / 2], [bw, 3, bd], 'hood', deco='collar', inflate=0.45))   # leafy cape collar
        body.add(Cube([-bw / 2 + 0.5, hip + 1, bd / 2 + 0.3], [bw - 1, bh - 2, 1], 'hood', deco='cape'))
    elif race == 'stone':
        body.add(Cube([-bw / 2 - 1.5, sh - 2, -bd / 2], [3, 3, bd], 'metal', deco='plate', inflate=0.3))       # stone pauldron
        body.add(Cube([-bw / 2, hip + 2, -bd / 2], [bw, 3, bd], 'cloth2', inflate=0.2))
    elif race == 'swamp':
        body.add(Cube([-bw / 2, hip - 2, -bd / 2], [bw, 4, bd], 'hood', deco='reeds', inflate=0.5))           # reed skirt
        body.add(Cube([-1, sh - 1, bd / 2 - 0.5], [2, 3, 3], 'skin2', rot=[-30, 0, 0], pivot=[0, sh - 1, bd / 2]))  # back fin
    else:
        body.add(Cube([-bw / 2, hip + bh - 4, -bd / 2], [bw, 4, bd], 'hood', deco='collar', inflate=0.5))     # fur shawl
        body.add(Cube([bw / 2 - 3, hip + 2, -bd / 2 - 0.8], [2, 2, 1], 'metal', deco='lantern'))               # glow pouch
    # ---- head
    head = bone('head', [0, sh, 0], 'body', rot=[-P['hunch'], 0, 0])
    hz = -hd / 2 - (1 if race in ('swamp', 'cave') else 0.5)
    head.add(Cube([-hw / 2, sh, hz], [hw, hh, hd], 'skin', deco='face'))
    if race == 'forest':
        head.add(Cube([-1, sh + 2, hz - 3], [2, 3, 3], 'skin', deco='nose'))                 # long nose
        head.add(Cube([-0.5, sh + 1.5, hz - 4.2], [1, 1.5, 1.5], 'skin3', rot=[-20, 0, 0], pivot=[0, sh + 2, hz - 3]))
        head.add(Cube([-hw / 2, sh + hh - 1, hz], [hw, 2, hd], 'hood', deco='hood_top', inflate=0.4))
        head.add(Cube([-2.5, sh + hh + 1, hz + 1.5], [5, 2, 5], 'hood', inflate=0.2))
        head.add(Cube([-1, sh + hh + 3, hz + 3], [2, 3, 2], 'hood2', rot=[-25, 0, 0], pivot=[0, sh + hh + 3, hz + 4]))
    elif race == 'stone':
        head.add(Cube([-2, sh + 2, hz - 2], [4, 3, 2], 'skin', deco='nose'))                 # flat broad nose
        head.add(Cube([-hw / 2, sh + 5, hz - 1.2], [hw, 2, 2], 'skin2', deco='brow'))          # heavy brow
        head.add(Cube([-3, sh, hz - 1], [6, 2, 1.5], 'skin2', deco='jaw'))                     # jutting jaw
        head.add(Cube([-2.5, sh + hh, hz + 2], [5, 1, 4], 'hood', deco='moss', inflate=0.3))  # moss on the head
    elif race == 'swamp':
        head.add(Cube([-1.5, sh + 2, hz - 1.5], [3, 2, 1.5], 'skin2', deco='nose'))           # snub nose
        head.add(Cube([-0.5, sh + hh, hz + 1], [1, 3, 7], 'skin3', deco='fin'))                # head crest
        head.add(Cube([-hw / 2 - 0.3, sh, hz - 0.3], [hw + 0.6, 2, 3], 'skin2', deco='lip'))   # wide frog mouth
    else:
        head.add(Cube([-1, sh + 2, hz - 2], [2, 4, 2], 'skin', deco='nose'))                  # droopy nose
        head.add(Cube([-hw / 2, sh + hh - 2, hz], [hw, 2, hd], 'hood', deco='hood_top', inflate=0.3))
    # ears
    ew, eh, ed = P['ear']
    ear_r = bone('ear_r', [-hw / 2, sh + hh / 2 + 1 + P['ear_y'], hz + hd / 2], 'head', rot=[0, -15, P['ear_rot']])
    ex, ey, ez = ear_r.pivot
    ear_r.add(Cube([ex - ew, ey - eh / 2, ez - ed / 2], [ew, eh, ed], 'skin', deco='ear'))
    if race in ('forest', 'cave'):
        ear_r.add(Cube([ex - ew - 2, ey - 0.5 + (1 if race == 'forest' else 0), ez - ed / 2], [2, 1 if race == 'forest' else 3, ed], 'skin', deco='ear'))
    if race == 'cave':
        ear_r.add(Cube([ex - ew + 1, ey + eh / 2, ez - ed / 2], [ew - 3, 2, ed], 'skin', deco='ear'))
    if race == 'stone':
        ear_r.add(Cube([ex - ew - 0.2, ey - 1, ez - ed / 2 - 0.2], [1, 1, ed + 0.4], 'metal'))     # earring
    ear_l = bone('ear_l', [-ex, ey, ez], 'head', rot=[0, 15, -P['ear_rot']])
    for c in ear_r.cubes:
        ear_l.cubes.append(mirror_x(c))
    # ---- arms
    ax = bw / 2 + aw / 2 + (0 if race != 'stone' else 0.2)
    arm_r = bone('arm_r', [-ax, sh - 1.5, 0], 'body')
    arm_r.add(Cube([-ax - aw / 2, sh - ah, -ad / 2], [aw, ah, ad], 'skin', deco='arm'))
    arm_r.add(Cube([-ax - aw / 2, sh - 4, -ad / 2], [aw, 4, ad], 'shirt', deco='sleeve', inflate=0.3))
    if race == 'stone':
        arm_r.add(Cube([-ax - aw / 2, sh - ah + 1, -ad / 2], [aw, 2, ad], 'leather', inflate=0.4))   # bracer
    if race == 'swamp':
        arm_r.add(Cube([-ax - aw / 2 - 0.6, sh - ah + 2, -0.5], [1, 3, 1], 'skin2'))               # elbow spur
    hand_r = bone('hand_r', [-ax, sh - ah + 1, 0], 'arm_r')
    arm_l = bone('arm_l', [ax, sh - 1.5, 0], 'body')
    for c in arm_r.cubes:
        arm_l.cubes.append(mirror_x(c))
    hand_l = bone('hand_l', [ax, sh - ah + 1, 0], 'arm_l')
    # where the chest on the back sits
    bone('pack', [0, hip + 1, bd / 2], 'body')
    return [root, leg_r, leg_l, body, head, ear_r, ear_l, arm_r, arm_l, hand_r, hand_l, B['pack']]


# ------------------------------------------------------------------------------------------------ box UV packing
def unwrap_size(c):
    w, h, d = (max(1, int(math.ceil(v))) for v in c.s)
    return 2 * d + 2 * w, d + h


def pack(bones):
    cubes = [c for b in bones for c in b.cubes]
    order = sorted(cubes, key=lambda c: -unwrap_size(c)[1])
    x = y = row = 0
    for c in order:
        w, h = unwrap_size(c)
        if x + w > TEX:
            x, y, row = 0, y + row, 0
        c.uv = (x, y)
        x += w
        row = max(row, h)
    if y + row > TEX:
        raise SystemExit('goblin texture too small')


def faces(c):
    u, v = c.uv
    w, h, d = (max(1, int(math.ceil(q))) for q in c.s)
    return dict(up=(u + d, v, w, d), down=(u + d + w, v, w, d), right=(u, v + d, d, h), front=(u + d, v + d, w, h),
                left=(u + d + w, v + d, d, h), back=(u + 2 * d + w, v + d, w, h))


# ------------------------------------------------------------------------------------------------ painting
def shade(col, k):
    return tuple(max(0, min(255, int(c * k))) for c in col)


def paint(bones, race, seed):
    pal = dict(PAL[race])
    pal['shirt'] = pal['cloth']
    pal['trousers'] = pal['cloth2']
    pal['cloth2'] = pal['cloth2']
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed)

    def fill(r, col, noise=0.06, grad=0.0):
        x0, y0, w, h = r
        for yy in range(h):
            for xx in range(w):
                k = 1 + rnd.uniform(-noise, noise) - grad * (yy / max(1, h - 1) - 0.5)
                px[x0 + xx, y0 + yy] = shade(col, k) + (255,)

    def put(r, xx, yy, col):
        x0, y0, w, h = r
        if 0 <= xx < w and 0 <= yy < h:
            px[x0 + xx, y0 + yy] = tuple(col) + (255,)

    for b in bones:
        for c in b.cubes:
            F = faces(c)
            base = pal[c.mat]
            for name, r in F.items():
                k = {'up': 1.08, 'down': 0.8, 'front': 1.0, 'back': 0.92, 'left': 0.95, 'right': 0.95}[name]
                fill(r, shade(base, k), grad=0.12 if name not in ('up', 'down') else 0)
            d = c.deco
            fr = F['front']
            if d == 'face':
                w, h = fr[2], fr[3]
                cx = w // 2
                ey = max(2, h // 2 - 1) if race != 'cave' else max(2, h // 2 - 2)
                big = race == 'cave'
                for side in (-1, 1):
                    ex = cx + side * (2 if w <= 10 else 3) - (1 if side < 0 else 0)
                    cols = [ex - 1, ex] if side < 0 else [ex, ex + 1]
                    rows = [ey, ey + 1] if big else [ey]
                    for yy in rows:
                        for xx in cols:
                            put(fr, xx, yy, pal['eye'])
                    inner = cols[1] if side < 0 else cols[0]
                    put(fr, inner, ey + (1 if big else 0), pal['pupil'])
                    for xx in cols + ([cols[0] - 1] if side < 0 else [cols[1] + 1]):
                        put(fr, xx, ey - 1, shade(pal['skin2'], 0.75))      # brow
                    put(fr, cols[0] if side < 0 else cols[1], ey + (2 if big else 1), shade(pal['skin2'], 0.9))
                my = h - 2 if race != 'swamp' else h - 2
                mw = 4 if race != 'swamp' else w - 2
                for xx in range(cx - mw // 2, cx + mw // 2):
                    put(fr, xx, my, pal['mouth'])
                put(fr, cx - mw // 2, my, pal['teeth'])
                put(fr, cx + mw // 2 - 1, my, pal['teeth'])
                if race in ('forest', 'stone'):
                    put(fr, cx - mw // 2, my + 1, pal['teeth'])     # tusks
                    put(fr, cx + mw // 2 - 1, my + 1, pal['teeth'])
                for side in (0, w - 1):                                # cheeks
                    put(fr, side, my - 1, shade(pal['skin'], 1.12))
                if race == 'swamp':
                    for _ in range(5):
                        put(fr, rnd.randrange(w), rnd.randrange(h), pal['skin3'])
                if race == 'stone':
                    for _ in range(4):
                        put(fr, rnd.randrange(w), rnd.randrange(h // 2), shade(pal['skin2'], 0.85))
                # warts and spots on the sides of the head
                for name in ('left', 'right', 'up'):
                    r = F[name]
                    for _ in range(3):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), shade(pal['skin2'], 0.9))
            elif d == 'ear':
                for name in ('front', 'back'):
                    r = F[name]
                    for xx in range(1, r[2] - 1):
                        put(r, xx, r[3] // 2, shade(pal['skin'], 0.78))     # inner ear
                    put(r, 0, 0, shade(pal['skin'], 1.1))
            elif d == 'nose':
                for name in F:
                    r = F[name]
                    put(r, r[2] // 2, r[3] - 1, shade(pal['skin2'], 0.85))
                put(fr, 0, fr[3] - 1, shade(pal['skin2'], 0.6))
                put(fr, fr[2] - 1, fr[3] - 1, shade(pal['skin2'], 0.6))
            elif d == 'shirt':
                for name in ('front', 'back'):
                    r = F[name]
                    for xx in range(r[2]):
                        put(r, xx, 0, shade(pal['shirt'], 0.75))          # neckline
                    if name == 'front':
                        for yy in range(1, r[3] - 2):
                            put(r, r[2] // 2, yy, shade(pal['shirt'], 0.7))   # laces
                            if yy % 2:
                                put(r, r[2] // 2 - 1, yy, shade(pal['leather'], 1.1))
                    for k in range(3):
                        put(r, rnd.randrange(r[2]), rnd.randrange(2, r[3]), shade(pal['shirt'], 0.82))   # patches
            elif d == 'belt':
                r = fr
                bx = r[2] // 2 - 1
                for xx in (bx, bx + 1):
                    put(r, xx, 0, pal['metal'])
                    put(r, xx, 1, shade(pal['metal'], 0.8))
                for name in ('left', 'right', 'back'):
                    rr = F[name]
                    put(rr, rr[2] // 2, 0, shade(pal['leather'], 1.25))
            elif d == 'shoe':
                for name in F:
                    r = F[name]
                    for xx in range(r[2]):
                        put(r, xx, 0, shade(pal['leather'], 1.2))
            elif d == 'sleeve':
                for name in ('front', 'back', 'left', 'right'):
                    r = F[name]
                    for xx in range(r[2]):
                        put(r, xx, r[3] - 1, shade(pal['shirt'], 0.7))
            elif d == 'arm':
                for name in ('front', 'back', 'left', 'right'):
                    r = F[name]
                    for xx in range(r[2]):
                        put(r, xx, r[3] - 1, pal['nail'])                   # knuckles / claws
                    put(r, 0, r[3] - 3, shade(pal['skin2'], 0.9))
            elif d in ('collar', 'hood_top', 'cape'):
                for name in F:
                    r = F[name]
                    for _ in range(r[2] * r[3] // 3 + 1):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), shade(pal['hood2'], rnd.uniform(0.85, 1.15)))
                    if race == 'forest':
                        for _ in range(2):
                            put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), (170, 120, 60))   # an autumn leaf
                    if race == 'cave' and d == 'collar':
                        for yy in range(r[3]):
                            for xx in range(r[2]):
                                if (xx + yy) % 3 == 0:
                                    put(r, xx, yy, shade(pal['hood'], 1.2))
            elif d == 'reeds':
                for name in F:
                    r = F[name]
                    for xx in range(r[2]):
                        if xx % 2 == 0:
                            for yy in range(r[3]):
                                put(r, xx, yy, shade(pal['hood2'], 0.85 + 0.1 * (yy % 2)))
            elif d == 'plate':
                for name in F:
                    r = F[name]
                    for _ in range(3):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), shade(pal['metal'], 0.7))
            elif d == 'moss':
                for name in F:
                    r = F[name]
                    for _ in range(4):
                        put(r, rnd.randrange(r[2]), rnd.randrange(r[3]), (120, 150, 70))
            elif d == 'fin':
                for name in ('left', 'right'):
                    r = F[name]
                    for yy in range(r[3]):
                        for xx in range(r[2]):
                            if xx % 2 == 0:
                                put(r, xx, yy, pal['skin2'])
            elif d == 'lip':
                r = fr
                for xx in range(r[2]):
                    put(r, xx, 0, pal['mouth'])
                put(r, 1, 0, pal['teeth'])
                put(r, r[2] - 2, 0, pal['teeth'])
            elif d == 'lantern':
                for name in F:
                    r = F[name]
                    put(r, r[2] // 2, r[3] // 2, (150, 240, 255))
            elif d in ('brow', 'jaw'):
                for name in F:
                    r = F[name]
                    for xx in range(r[2]):
                        put(r, xx, r[3] - 1 if d == 'brow' else 0, shade(pal['skin2'], 0.8))
                if d == 'jaw':
                    put(fr, 1, 0, pal['teeth'])
                    put(fr, fr[2] - 2, 0, pal['teeth'])
    return img


# ------------------------------------------------------------------------------------------------ geo json
def r2(v):
    return round(v, 3)


def geo(bones, ident):
    out = []
    for b in bones:
        jb = {'name': b.name, 'pivot': [r2(v) for v in b.pivot]}
        if b.parent:
            jb['parent'] = b.parent
        if b.rot:
            jb['rotation'] = b.rot
        cubes = []
        for c in b.cubes:
            jc = {'origin': [r2(v) for v in c.o], 'size': [r2(v) for v in c.s], 'uv': list(c.uv)}
            if c.inflate:
                jc['inflate'] = c.inflate
            if c.rot:
                jc['rotation'] = c.rot
                jc['pivot'] = [r2(v) for v in c.pivot]
            cubes.append(jc)
        if cubes:
            jb['cubes'] = cubes
        out.append(jb)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.' + ident, 'texture_width': TEX, 'texture_height': TEX,
                        'visible_bounds_width': 3, 'visible_bounds_height': 3.5, 'visible_bounds_offset': [0, 1.25, 0]},
        'bones': out}]}


# ------------------------------------------------------------------------------------------------ animations
def ease(a, b, f):
    f = max(0.0, min(1.0, f))
    f = f * f * (3 - 2 * f)
    return a + (b - a) * f


def keys(t, pts):
    """Smoothly interpolated value at time t between (time, value) points (values may be numbers or 3-lists)."""
    if t <= pts[0][0]:
        return pts[0][1]
    for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
        if t0 <= t <= t1:
            f = (t - t0) / max(1e-6, t1 - t0)
            if isinstance(v0, (list, tuple)):
                return [ease(a, b, f) for a, b in zip(v0, v1)]
            return ease(v0, v1, f)
    return pts[-1][1]


S = math.sin
TAU = 2 * math.pi


def anim(length, loop, channels, fps=20):
    """channels: {bone: {'rotation'|'position': fn(t) -> [x, y, z]}}; sampled into keyframes."""
    bones = {}
    n = max(1, int(round(length * fps)))
    for bone, ch in channels.items():
        jb = {}
        for kind, fn in ch.items():
            frames = {}
            for i in range(n + 1):
                t = length * i / n
                v = fn(t)
                frames[f'{t:.4g}' if t else '0.0'] = [round(float(x), 2) for x in v]
            jb[kind] = frames
        bones[bone] = jb
    a = {'animation_length': length, 'bones': bones}
    if loop is True:
        a['loop'] = True
    elif loop == 'hold':
        a['loop'] = 'hold_on_last_frame'
    return a


def animations():
    A = {}
    w = lambda t, p, amp, ph=0.0: amp * S(TAU * t / p + ph)
    # ---------------- locomotion and poses
    A['idle'] = anim(3.0, True, {
        'body': {'rotation': lambda t: [w(t, 3, 1.5), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, -2), (1.2, 2), (2.2, -3), (3, -2)]), keys(t, [(0, 0), (0.6, 14), (1.4, 14), (1.9, -10), (2.6, -10), (3, 0)]), 0]},
        'ear_r': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (1.0, 0), (1.08, -18), (1.18, 0), (3, 0)])]},
        'ear_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (2.2, 0), (2.28, 18), (2.38, 0), (3, 0)])]},
        'arm_r': {'rotation': lambda t: [w(t, 3, 3), 0, 3 + w(t, 3, 2)]},
        'arm_l': {'rotation': lambda t: [w(t, 3, -3), 0, -3 - w(t, 3, 2)]},
    })
    A['walk'] = anim(0.8, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.8, 32), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.8, -32), 0, 0]},
        'arm_r': {'rotation': lambda t: [w(t, 0.8, -28), 0, 4]},
        'arm_l': {'rotation': lambda t: [w(t, 0.8, 28), 0, -4]},
        'body': {'rotation': lambda t: [2, w(t, 0.8, 5), 0], 'position': lambda t: [0, abs(w(t, 0.8, 0.8)), 0]},
        'head': {'rotation': lambda t: [w(t, 0.4, 2), w(t, 0.8, -4), 0]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.4, 6)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.4, -6)]},
    })
    A['run'] = anim(0.5, True, {
        'leg_r': {'rotation': lambda t: [w(t, 0.5, 52), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.5, -52), 0, 0]},
        'arm_r': {'rotation': lambda t: [w(t, 0.5, -50) - 10, 0, 8]},
        'arm_l': {'rotation': lambda t: [w(t, 0.5, 50) - 10, 0, -8]},
        'body': {'rotation': lambda t: [16, w(t, 0.5, 7), 0], 'position': lambda t: [0, abs(w(t, 0.5, 1.6)), 0]},
        'head': {'rotation': lambda t: [-12 + w(t, 0.25, 3), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, -20, -20 + w(t, 0.25, 8)]},
        'ear_l': {'rotation': lambda t: [0, 20, 20 + w(t, 0.25, -8)]},
    })
    A['sneak'] = anim(1.2, True, {
        'root': {'position': lambda t: [0, -2, 0]},
        'body': {'rotation': lambda t: [26, 0, 0]},
        'head': {'rotation': lambda t: [-22, w(t, 1.2, 12), 0]},
        'leg_r': {'rotation': lambda t: [w(t, 1.2, 16) - 20, 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 1.2, -16) - 20, 0, 0]},
        'arm_r': {'rotation': lambda t: [-30 + w(t, 1.2, 8), 0, 10]},
        'arm_l': {'rotation': lambda t: [-30 - w(t, 1.2, 8), 0, -10]},
        'ear_r': {'rotation': lambda t: [0, 0, 25]},
        'ear_l': {'rotation': lambda t: [0, 0, -25]},
    })
    A['swim'] = anim(1.0, True, {
        'root': {'position': lambda t: [0, -4 + w(t, 1, 0.6), 0]},
        'body': {'rotation': lambda t: [50, 0, w(t, 1, 6)]},
        'head': {'rotation': lambda t: [-45, 0, 0]},
        'arm_r': {'rotation': lambda t: [-150 + w(t, 1, 50), 0, 20 + w(t, 1, 20, 1.5)]},
        'arm_l': {'rotation': lambda t: [-150 + w(t, 1, -50), 0, -20 - w(t, 1, 20, 1.5)]},
        'leg_r': {'rotation': lambda t: [w(t, 0.5, 25), 0, 0]},
        'leg_l': {'rotation': lambda t: [w(t, 0.5, -25), 0, 0]},
    })
    A['sit'] = anim(4.0, True, {
        'root': {'position': lambda t: [0, -6.5, 0]},
        'leg_r': {'rotation': lambda t: [-85, 12, 0]},
        'leg_l': {'rotation': lambda t: [-85, -12, 0]},
        'body': {'rotation': lambda t: [w(t, 4, 2) - 4, 0, 0]},
        'arm_r': {'rotation': lambda t: [-30, 0, 6]},
        'arm_l': {'rotation': lambda t: [-30, 0, -6]},
        'head': {'rotation': lambda t: [w(t, 4, 3), keys(t, [(0, 0), (1.5, 15), (2.5, 15), (3.5, 0), (4, 0)]), keys(t, [(0, 0), (1.5, 8), (2.5, 8), (3.5, 0), (4, 0)])]},
    })
    A['sleep'] = anim(4.0, True, {            # lying curled on the ground
        'root': {'rotation': lambda t: [0, 0, 90], 'position': lambda t: [-1, 4.5, 0]},
        'body': {'rotation': lambda t: [22 + w(t, 4, 2), 0, 0]},
        'head': {'rotation': lambda t: [18, 0, 0]},
        'leg_r': {'rotation': lambda t: [-60, 0, 0]},
        'leg_l': {'rotation': lambda t: [-45, 0, 0]},
        'arm_r': {'rotation': lambda t: [-70, 0, 0]},
        'arm_l': {'rotation': lambda t: [-55, 20, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, 30]},
        'ear_l': {'rotation': lambda t: [0, 0, -30 + w(t, 4, 4)]},
    })
    A['sleep_bed'] = anim(4.0, True, {        # in a bed the game already lays it down
        'body': {'rotation': lambda t: [12 + w(t, 4, 2), 0, 0]},
        'head': {'rotation': lambda t: [10, 0, 0]},
        'leg_r': {'rotation': lambda t: [-35, 0, 0]},
        'leg_l': {'rotation': lambda t: [-25, 0, 0]},
        'arm_r': {'rotation': lambda t: [-40, 0, 0]},
        'arm_l': {'rotation': lambda t: [-30, 15, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, 30]},
        'ear_l': {'rotation': lambda t: [0, 0, -30]},
    })
    A['peek'] = anim(2.4, True, {
        'root': {'position': lambda t: [0, -3, 0]},
        'body': {'rotation': lambda t: [24, 0, 0]},
        'head': {'rotation': lambda t: [-24, keys(t, [(0, 0), (0.5, 28), (1.1, 28), (1.5, -20), (2.0, -20), (2.4, 0)]), 0]},
        'arm_r': {'rotation': lambda t: [-110, 0, -12]},
        'arm_l': {'rotation': lambda t: [-110, 0, 12]},
        'leg_r': {'rotation': lambda t: [-25, 0, 0]},
        'leg_l': {'rotation': lambda t: [-25, 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, 30]},
        'ear_l': {'rotation': lambda t: [0, 0, -30]},
    })
    A['consider'] = anim(2.0, True, {
        'arm_r': {'rotation': lambda t: [-55, 10, 0]},
        'arm_l': {'rotation': lambda t: [-150, 0, -25 + w(t, 0.5, 8)]},
        'head': {'rotation': lambda t: [18, 10, w(t, 2, 12)]},
        'body': {'rotation': lambda t: [4, 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 2, 10)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 2, 10)]},
    })
    # ---------------- combat
    A['windup'] = anim(0.45, 'hold', {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.45, -3)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, 30)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, -28)]), 0, 0]},
        'arm_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, -175)]), 0, keys(t, [(0, 0), (0.45, -10)])]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, -165)]), 0, keys(t, [(0, 0), (0.45, 10)])]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, -35)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.45, 20)]), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, -25, 25]},
        'ear_l': {'rotation': lambda t: [0, 25, -25]},
    })
    A['leap'] = anim(0.4, 'hold', {
        'root': {'position': lambda t: [0, keys(t, [(0, -2), (0.4, 2)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 25), (0.4, -18)]), 0, 0]},
        'head': {'rotation': lambda t: [-15, 0, 0]},
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -175), (0.4, -195)]), 0, -8]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, -165), (0.4, -190)]), 0, 8]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, -30), (0.4, 45)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 20), (0.4, -40)]), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, -35, 35]},
        'ear_l': {'rotation': lambda t: [0, 35, -35]},
    })
    A['strike'] = anim(0.4, False, {
        'root': {'position': lambda t: [0, keys(t, [(0, 1), (0.15, -2.5), (0.4, -1)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, -18), (0.12, 42), (0.4, 18)]), 0, 0]},
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -195), (0.1, -60), (0.16, -10), (0.4, -25)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, -190), (0.1, -60), (0.16, -15), (0.4, -30)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, -15), (0.15, -30), (0.4, -10)]), 0, 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 40), (0.15, -40), (0.4, -20)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, -40), (0.15, 30), (0.4, 15)]), 0, 0]},
    })
    A['punch'] = anim(0.3, False, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -20), (0.08, -95), (0.3, -15)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [-30, 0, -10]},
        'body': {'rotation': lambda t: [10, keys(t, [(0, 0), (0.08, -22), (0.3, 0)]), 0]},
    })
    for name, side in (('dodge_l', 1), ('dodge_r', -1)):
        A[name] = anim(0.35, False, {
            'root': {'rotation': lambda t, s=side: [0, 0, keys(t, [(0, 0), (0.1, 22 * s), (0.35, 0)])],
                     'position': lambda t: [0, keys(t, [(0, 0), (0.12, 2), (0.35, 0)]), 0]},
            'arm_r': {'rotation': lambda t: [-40, 0, keys(t, [(0, 0), (0.12, 60), (0.35, 0)])]},
            'arm_l': {'rotation': lambda t: [-40, 0, keys(t, [(0, 0), (0.12, -60), (0.35, 0)])]},
            'leg_r': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.12, 25), (0.35, 0)])]},
            'leg_l': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.12, -25), (0.35, 0)])]},
            'head': {'rotation': lambda t, s=side: [0, 0, keys(t, [(0, 0), (0.12, -15 * s), (0.35, 0)])]},
        })
    A['dodge_b'] = anim(0.35, False, {
        'root': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -25), (0.35, 0)]), 0, 0],
                 'position': lambda t: [0, keys(t, [(0, 0), (0.12, 2.5), (0.35, 0)]), 0]},
        'arm_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -70), (0.35, -20)]), 0, 20]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -70), (0.35, -20)]), 0, -20]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, -40), (0.35, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.12, 25), (0.35, 0)]), 0, 0]},
    })
    A['block'] = anim(0.25, 'hold', {
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.25, -1.5)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 16)]), 0, 0]},
        'head': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 6)]), keys(t, [(0, 0), (0.25, -8)]), 0]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -80)]), keys(t, [(0, 0), (0.25, 30)]), 0]},
        'arm_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -35)]), 0, 10]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, 18)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.25, -22)]), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, 28]},
        'ear_l': {'rotation': lambda t: [0, 0, -28]},
    })
    # ---------------- dances
    A['dance1'] = anim(1.0, True, {           # jig: hopping and kicking, arms up
        'root': {'position': lambda t: [0, abs(w(t, 1, 2.2)), 0]},
        'body': {'rotation': lambda t: [0, 0, w(t, 1, 10)]},
        'head': {'rotation': lambda t: [-8, w(t, 1, 12), w(t, 1, -12)]},
        'leg_r': {'rotation': lambda t: [min(0, w(t, 1, 55)), 0, 6]},
        'leg_l': {'rotation': lambda t: [min(0, w(t, 1, -55)), 0, -6]},
        'arm_r': {'rotation': lambda t: [0, 0, 140 + w(t, 0.5, 22)]},
        'arm_l': {'rotation': lambda t: [0, 0, -140 + w(t, 0.5, 22)]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.5, 14)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.5, -14)]},
    })
    A['dance2'] = anim(1.6, True, {           # twirl with arms out
        'root': {'rotation': lambda t: [0, -360 * t / 1.6, 0], 'position': lambda t: [0, abs(w(t, 0.8, 1.5)), 0]},
        'arm_r': {'rotation': lambda t: [w(t, 0.4, 10), 0, 85]},
        'arm_l': {'rotation': lambda t: [w(t, 0.4, -10), 0, -85]},
        'body': {'rotation': lambda t: [-6, 0, 0]},
        'head': {'rotation': lambda t: [-12, 0, w(t, 0.8, 10)]},
        'leg_r': {'rotation': lambda t: [w(t, 0.4, 25), 0, 8]},
        'leg_l': {'rotation': lambda t: [w(t, 0.4, -25), 0, -8]},
        'ear_r': {'rotation': lambda t: [0, -20, 20]},
        'ear_l': {'rotation': lambda t: [0, 20, -20]},
    })
    A['dance3'] = anim(1.2, True, {           # clap and stomp
        'root': {'position': lambda t: [0, max(0, w(t, 0.6, 1.8)), 0]},
        'body': {'rotation': lambda t: [8 + w(t, 0.6, 8), w(t, 1.2, 12), 0]},
        'arm_r': {'rotation': lambda t: [-75 + w(t, 0.6, 15), keys(t % 0.6, [(0, 35), (0.15, -5), (0.3, 35), (0.6, 35)]), 0]},
        'arm_l': {'rotation': lambda t: [-75 + w(t, 0.6, 15), keys(t % 0.6, [(0, -35), (0.15, 5), (0.3, -35), (0.6, -35)]), 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -45), (0.6, 0), (1.2, 0)]), 0, 0]},
        'leg_l': {'rotation': lambda t: [keys(t, [(0, 0), (0.6, 0), (0.9, -45), (1.2, 0)]), 0, 0]},
        'head': {'rotation': lambda t: [w(t, 0.3, 8) - 5, 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.3, 12)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.3, -12)]},
    })
    # ---------------- work
    def swing(down, up, hit=0.4, length=0.6):
        return lambda t: [keys(t, [(0, up), (0.22, up - 12), (hit, down), (length, up)]), 0, 0]
    A['chop'] = anim(0.6, True, {
        'arm_r': {'rotation': swing(-35, -150)},
        'arm_l': {'rotation': swing(-30, -145)},
        'body': {'rotation': lambda t: [keys(t, [(0, -4), (0.22, -8), (0.4, 26), (0.6, -4)]), keys(t, [(0, 8), (0.4, -8), (0.6, 8)]), 0]},
        'head': {'rotation': lambda t: [10, 0, 0]},
        'leg_r': {'rotation': lambda t: [-14, 0, 6]},
        'leg_l': {'rotation': lambda t: [14, 0, -6]},
    })
    A['mine'] = anim(0.7, True, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -125), (0.3, -140), (0.45, -30), (0.7, -125)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [-35, 0, -12]},
        'body': {'rotation': lambda t: [keys(t, [(0, 12), (0.45, 28), (0.7, 12)]), 0, 0]},
        'head': {'rotation': lambda t: [6, 0, 0]},
        'leg_r': {'rotation': lambda t: [-18, 0, 0]},
        'leg_l': {'rotation': lambda t: [16, 0, 0]},
    })
    A['hoe'] = anim(1.0, True, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -60), (0.5, -15), (1, -60)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, -55), (0.5, -10), (1, -55)]), 0, 0]},
        'body': {'rotation': lambda t: [30, w(t, 1, 14), 0]},
        'head': {'rotation': lambda t: [8, w(t, 1, -10), 0]},
        'leg_r': {'rotation': lambda t: [-12, 0, 0]},
        'leg_l': {'rotation': lambda t: [12, 0, 0]},
    })
    A['shear'] = anim(0.5, True, {
        'root': {'position': lambda t: [0, -1, 0]},
        'arm_r': {'rotation': lambda t: [-62, 0, w(t, 0.25, 9)]},
        'arm_l': {'rotation': lambda t: [-52, 0, -8]},
        'body': {'rotation': lambda t: [20, 0, 0]},
        'head': {'rotation': lambda t: [22, 0, w(t, 0.5, 4)]},
    })
    A['work'] = anim(1.0, True, {
        'arm_r': {'rotation': lambda t: [-62 + w(t, 1, 10), w(t, 1, 25, 1.57), 0]},
        'arm_l': {'rotation': lambda t: [-62 + w(t, 1, 10), w(t, 1, 25, 1.57), 0]},
        'body': {'rotation': lambda t: [16, w(t, 1, 8, 1.57), 0]},
        'head': {'rotation': lambda t: [18, 0, w(t, 2, 6)]},
    })
    A['churn'] = anim(0.8, True, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -55), (0.4, -18), (0.8, -55)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [keys(t, [(0, -55), (0.4, -18), (0.8, -55)]), 0, 0]},
        'root': {'position': lambda t: [0, keys(t, [(0, 0), (0.4, -1.2), (0.8, 0)]), 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 6), (0.4, 16), (0.8, 6)]), 0, 0]},
    })
    A['build'] = anim(0.6, True, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, -100), (0.25, -115), (0.38, -30), (0.6, -100)]), 0, 0]},
        'arm_l': {'rotation': lambda t: [-55, 15, 0]},
        'body': {'rotation': lambda t: [18, 0, 0]},
        'head': {'rotation': lambda t: [14, 0, 0]},
    })
    # ---------------- social
    A['pet'] = anim(1.5, False, {
        'arm_r': {'rotation': lambda t: [-75, w(t, 0.5, 22), 0]},
        'head': {'rotation': lambda t: [10, 0, 10]},
        'body': {'rotation': lambda t: [8, 0, 0]},
    })
    A['petted'] = anim(1.5, False, {
        'head': {'rotation': lambda t: [-10, 0, 18 + w(t, 0.75, 6)]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.25, 16)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.25, -16)]},
        'body': {'rotation': lambda t: [0, 0, w(t, 1.5, 5)]},
        'arm_r': {'rotation': lambda t: [0, 0, 12]},
        'arm_l': {'rotation': lambda t: [0, 0, -12]},
    })
    A['poke'] = anim(0.6, False, {
        'arm_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.15, -85), (0.3, -70), (0.6, 0)]), 0, 0]},
        'body': {'rotation': lambda t: [6, keys(t, [(0, 0), (0.15, -18), (0.6, 0)]), 0]},
        'head': {'rotation': lambda t: [0, 0, keys(t, [(0, 0), (0.3, -12), (0.6, 0)])]},
    })
    A['annoyed'] = anim(1.0, False, {
        'arm_r': {'rotation': lambda t: [-62, 38, 0]},
        'arm_l': {'rotation': lambda t: [-62, -38, 0]},
        'head': {'rotation': lambda t: [-6, w(t, 0.33, 20), 0]},
        'leg_r': {'rotation': lambda t: [keys(t, [(0, 0), (0.3, -30), (0.4, 0), (1, 0)]), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, 25]},
        'ear_l': {'rotation': lambda t: [0, 0, -25]},
    })
    A['eat'] = anim(1.5, False, {
        'arm_r': {'rotation': lambda t: [-112 + w(t, 0.5, 12), 25, 0]},
        'head': {'rotation': lambda t: [12 + w(t, 0.5, 6), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.5, 8)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.5, -8)]},
    })
    A['wave'] = anim(1.2, False, {
        'arm_r': {'rotation': lambda t: [0, 0, 150 + w(t, 0.4, 20)]},
        'head': {'rotation': lambda t: [-6, 0, 8]},
    })
    A['sulk'] = anim(3.0, True, {
        'root': {'position': lambda t: [0, -1, 0]},
        'body': {'rotation': lambda t: [keys(t, [(0, 18), (1.5, 26), (1.8, 18), (3, 18)]), 0, 0]},
        'head': {'rotation': lambda t: [30, w(t, 3, 6), 0]},
        'arm_r': {'rotation': lambda t: [-12, 0, -4]},
        'arm_l': {'rotation': lambda t: [-12, 0, 4]},
        'ear_r': {'rotation': lambda t: [0, 0, 35]},
        'ear_l': {'rotation': lambda t: [0, 0, -35]},
    })
    A['cheer'] = anim(1.2, False, {
        'root': {'position': lambda t: [0, max(0, w(t, 0.6, 4)), 0]},
        'arm_r': {'rotation': lambda t: [0, 0, 160 + w(t, 0.3, 12)]},
        'arm_l': {'rotation': lambda t: [0, 0, -160 + w(t, 0.3, 12)]},
        'head': {'rotation': lambda t: [-20, 0, 0]},
        'leg_r': {'rotation': lambda t: [max(0, w(t, 0.6, 25)), 0, 0]},
        'leg_l': {'rotation': lambda t: [max(0, w(t, 0.6, 25)), 0, 0]},
        'ear_r': {'rotation': lambda t: [0, -20, -20]},
        'ear_l': {'rotation': lambda t: [0, 20, 20]},
    })
    A['play'] = anim(0.8, True, {
        'arm_r': {'rotation': lambda t: [w(t, 0.4, 40) - 20, 0, 40 + w(t, 0.4, 25)]},
        'arm_l': {'rotation': lambda t: [w(t, 0.4, -40) - 20, 0, -40 - w(t, 0.4, 25)]},
        'body': {'rotation': lambda t: [6, w(t, 0.8, 14), 0]},
        'head': {'rotation': lambda t: [-12, w(t, 0.8, -14), 0]},
        'ear_r': {'rotation': lambda t: [0, 0, w(t, 0.2, 15)]},
        'ear_l': {'rotation': lambda t: [0, 0, w(t, 0.2, -15)]},
    })
    A['scared'] = anim(0.6, True, {
        'root': {'position': lambda t: [w(t, 0.1, 0.3), -2, 0]},
        'body': {'rotation': lambda t: [24, 0, 0]},
        'head': {'rotation': lambda t: [22, 0, w(t, 0.2, 4)]},
        'arm_r': {'rotation': lambda t: [-140, 0, -14 + w(t, 0.1, 5)]},
        'arm_l': {'rotation': lambda t: [-140, 0, 14 + w(t, 0.1, 5)]},
        'leg_r': {'rotation': lambda t: [-15, 0, w(t, 0.1, 3)]},
        'leg_l': {'rotation': lambda t: [-15, 0, w(t, 0.1, -3)]},
        'ear_r': {'rotation': lambda t: [0, 0, 40]},
        'ear_l': {'rotation': lambda t: [0, 0, -40]},
    })
    return {'format_version': '1.8.0', 'animations': A}


def generate(A_DIR, wj, wi):
    for i, race in enumerate(('forest', 'stone', 'swamp', 'cave')):
        bones = build(race)
        pack(bones)
        wj(f'{A_DIR}/geckolib/models/entity/goblin_{race}.geo.json', geo(bones, 'goblin_' + race))
        wi(f'{A_DIR}/textures/entity/goblin_geo_{race}.png', paint(bones, race, 1000 + i))
    wj(f'{A_DIR}/geckolib/animations/entity/goblin.animation.json', animations())


def preview(A_DIR, out):
    """A quick front/side render of each race's geo for checking the build (orthographic, no animation)."""
    import json
    tiles = []
    for race in ('forest', 'stone', 'swamp', 'cave'):
        g = json.load(open(f'{A_DIR}/geckolib/models/entity/goblin_{race}.geo.json'))
        tex = Image.open(f'{A_DIR}/textures/entity/goblin_geo_{race}.png').convert('RGBA')
        tiles.append(render_front(g, tex))
    W = sum(t.width for t in tiles)
    img = Image.new('RGBA', (W, max(t.height for t in tiles)), (200, 220, 240, 255))
    x = 0
    for t in tiles:
        img.paste(t, (x, 0), t)
        x += t.width
    img = img.resize((img.width * 4, img.height * 4), Image.NEAREST)
    img.save(out)


def render_front(g, tex, scale=1):
    """Front orthographic view: paints the north faces of unrotated cubes, nearest first (ignores bone rotations)."""
    bones = g['minecraft:geometry'][0]['bones']
    W, H = 48, 44
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    zbuf = [[1e9] * W for _ in range(H)]
    px = img.load()
    for b in bones:
        for c in b.get('cubes', []):
            if 'rotation' in c:
                continue
            (x0, y0, z0), (w, h, d) = c['origin'], c['size']
            u, v = c['uv']
            iw, ih, idd = (max(1, int(math.ceil(q))) for q in (w, h, d))
            for yy in range(int(round(h))):
                for xx in range(int(round(w))):
                    sx = int(round(W / 2 + x0 + xx))
                    sy = int(round(H - 2 - (y0 + h - 1 - yy)))
                    if 0 <= sx < W and 0 <= sy < H and z0 < zbuf[sy][sx]:
                        col = tex.getpixel((u + idd + min(xx, iw - 1), v + idd + min(yy, ih - 1)))
                        if col[3]:
                            zbuf[sy][sx] = z0
                            px[sx, sy] = col
    return img
