"""Generates every asset of the "Gołębie" mod (src/golebie/resources): GeckoLib models and animations of the pigeon and the
fantail, their textures (plain and with each diaper), diaper and spawn-egg icons, synthesized coo / purr / peck / wing
sounds, lang files, recipes and spawns.   Run:  python3 tools/golebie/gen_golebie.py"""
import json, math, os, random, subprocess, sys, tempfile, wave
import struct
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import pigeon as PG      # noqa: E402
import fantail as FT     # noqa: E402
import diaper as DP      # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'golebie', 'resources'))
NS = 'golebie'
A = f'{ROOT}/assets/{NS}'
D = f'{ROOT}/data/{NS}'
PATTERNS = list(DP.PATTERNS)


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)


def wi(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ----------------------------------------------------------------------------------------------- birds
def birds():
    out = {}
    # pigeon
    bones = PG.build()
    PG.pack(bones)
    tex = PG.paint(bones)
    geo = PG.geo_json(bones)
    wj(f'{A}/geckolib/models/entity/pigeon.geo.json', geo)
    anims = PG.animations()
    wj(f'{A}/geckolib/animations/entity/pigeon.animation.json', anims)
    wi(f'{A}/textures/entity/pigeon.png', tex)
    for i, p in enumerate(PATTERNS):
        ov = DP.paint_overlay(bones, (PG.TEX_W, PG.TEX_H), p)
        wi(f'{A}/textures/entity/pigeon_{p}.png', Image.alpha_composite(tex.convert('RGBA'), ov))
    out['pigeon'] = sorted(k.split('.')[-1] for k in anims['animations'])
    # fantail
    bones = FT.build()
    PG.pack(bones)
    tex = FT.paint(bones)
    geo = PG.geo_json(bones)
    geo['minecraft:geometry'][0]['description']['identifier'] = 'geometry.fantail'
    wj(f'{A}/geckolib/models/entity/fantail.geo.json', geo)
    anims = FT.animations()
    wj(f'{A}/geckolib/animations/entity/fantail.animation.json', anims)
    wi(f'{A}/textures/entity/fantail.png', tex)
    for p in PATTERNS:
        ov = DP.paint_overlay(bones, (PG.TEX_W, PG.TEX_H), p)
        wi(f'{A}/textures/entity/fantail_{p}.png', Image.alpha_composite(tex.convert('RGBA'), ov))
    out['fantail'] = sorted(k.split('.')[-1] for k in anims['animations'])
    return out


# ----------------------------------------------------------------------------------------------- items
def item(name, img):
    wi(f'{A}/textures/item/{name}.png', img)
    wj(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
    wj(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})


def egg(base, spot, spot2, seed):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.6, (y - 8.6) / 7.0
            if dy < 0:
                dx *= 1.12
            if dx * dx + dy * dy <= 1:
                k = 1.08 - 0.25 * (dx + dy + 1) / 2
                px[x, y] = PG.shade(base, k) + (255,)
    for _ in range(9):
        x, y = rnd.randrange(4, 12), rnd.randrange(3, 14)
        if px[x, y][3]:
            px[x, y] = (spot if rnd.random() < 0.7 else spot2) + (255,)
    out = img.copy()
    o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + a < 16 and 0 <= y + b < 16 and px[x + a, y + b][3] for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = (34, 30, 30, 255)
    return out


def items():
    for p in PATTERNS:
        item(f'diaper_{p}', DP.icon(p))
    item('pigeon_spawn_egg', egg((156, 160, 172), (52, 56, 64), (70, 120, 96), 1))
    item('fantail_spawn_egg', egg((156, 96, 70), (152, 140, 130), (120, 84, 120), 2))
    # recipes: wool + dye + string + a gold nugget for the snap
    dyes = {'gingham_black': ['minecraft:white_wool', 'minecraft:black_dye'], 'stripes_grey': ['minecraft:gray_wool', 'minecraft:black_dye'],
            'gingham_red': ['minecraft:white_wool', 'minecraft:red_dye'], 'gingham_blue': ['minecraft:white_wool', 'minecraft:blue_dye'],
            'dots_pink': ['minecraft:pink_wool', 'minecraft:white_dye'], 'denim': ['minecraft:blue_wool', 'minecraft:string']}
    for p, ing in dyes.items():
        wj(f'{D}/recipe/diaper_{p}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                           'ingredients': ing + ['minecraft:string', 'minecraft:gold_nugget'],
                                           'result': {'id': f'{NS}:diaper_{p}', 'count': 1}})


# ----------------------------------------------------------------------------------------------- sounds
SR = 22050


def write_ogg(name, samples):
    os.makedirs(f'{A}/sounds', exist_ok=True)
    peak = max(1e-6, max(abs(v) for v in samples))
    data = b''.join(struct.pack('<h', int(max(-1, min(1, v / peak * 0.9)) * 32767)) for v in samples)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        tmp = f.name
    with wave.open(tmp, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', f'{A}/sounds/{name}.ogg'], check=True)
    os.unlink(tmp)


def env(t, a, d, L):
    if t < a:
        return t / a
    if t > L - d:
        return max(0.0, (L - t) / d)
    return 1.0


def coo(segments, seed, roll=26.0, breath=0.05):
    """A pigeon coo: soft, low, hollow notes with a throaty roll.  segments: (duration, f_start, f_end, gap after)."""
    rnd = random.Random(seed)
    out = []
    for dur, f0, f1, gap in segments:
        n = int(dur * SR)
        ph = 0.0
        for i in range(n):
            t = i / SR
            f = f0 + (f1 - f0) * (t / dur) ** 0.8
            f *= 1 + 0.012 * math.sin(2 * math.pi * 5 * t)
            ph += 2 * math.pi * f / SR
            tone = math.sin(ph) + 0.45 * math.sin(2 * ph + 0.3) + 0.18 * math.sin(3 * ph) + 0.06 * math.sin(4 * ph)
            am = 0.72 + 0.28 * math.sin(2 * math.pi * roll * t)            # the 'rrr' of the coo
            out.append(tone * am * env(t, 0.05, 0.12, dur) + breath * rnd.uniform(-1, 1) * env(t, 0.02, 0.05, dur))
        out += [0.0] * int(gap * SR)
    # soft low-pass, the throat and closed beak
    y, a = 0.0, 0.18
    res = []
    for v in out:
        y += a * (v - y)
        res.append(y)
    return res


def purr(seed):
    rnd = random.Random(seed)
    out = []
    L = 0.9
    for i in range(int(L * SR)):
        t = i / SR
        f = 240 + 20 * math.sin(2 * math.pi * 1.3 * t)
        out.append((math.sin(2 * math.pi * f * t) + 0.3 * math.sin(4 * math.pi * f * t)) * (0.6 + 0.4 * math.sin(2 * math.pi * 22 * t))
                   * env(t, 0.1, 0.3, L) + 0.03 * rnd.uniform(-1, 1))
    y, res = 0.0, []
    for v in out:
        y += 0.12 * (v - y)
        res.append(y)
    return res


def peck(seed):
    rnd = random.Random(seed)
    out = []
    for k in range(3):
        n = int(0.012 * SR)
        prev = 0.0
        for i in range(n):
            v = rnd.uniform(-1, 1)
            out.append((v - prev) * (1 - i / n))       # high-passed tick
            prev = v
        out += [0.0] * int(0.07 * SR)
    return out


def wings(seed):
    rnd = random.Random(seed)
    out = []
    for k in range(6):
        L = 0.07
        y = 0.0
        for i in range(int(L * SR)):
            t = i / SR
            y += 0.25 * (rnd.uniform(-1, 1) - y)
            out.append(y * math.sin(math.pi * t / L) * (1 - k * 0.1))
        out += [0.0] * int(0.03 * SR)
    return out


def sounds():
    write_ogg('coo1', coo([(0.32, 300, 420, 0.06), (0.55, 440, 330, 0.0)], 1))
    write_ogg('coo2', coo([(0.25, 320, 400, 0.05), (0.25, 400, 380, 0.05), (0.6, 430, 300, 0.0)], 2))
    write_ogg('coo3', coo([(0.8, 280, 460, 0.0)], 3, roll=30))
    write_ogg('bow_coo', coo([(0.35, 260, 380, 0.04), (0.9, 470, 290, 0.08), (0.3, 330, 300, 0.0)], 4, roll=34, breath=0.08))
    write_ogg('purr1', purr(5))
    write_ogg('purr2', purr(6))
    write_ogg('peck', peck(7))
    write_ogg('wings', wings(8))
    s = lambda *names: {'sounds': [f'{NS}:{n}' for n in names]}
    wj(f'{A}/sounds.json', {
        'coo': dict(s('coo1', 'coo2', 'coo3'), subtitle='subtitles.golebie.coo'),
        'bow_coo': dict(s('bow_coo'), subtitle='subtitles.golebie.bow_coo'),
        'purr': dict(s('purr1', 'purr2'), subtitle='subtitles.golebie.purr'),
        'peck': dict(s('peck'), subtitle='subtitles.golebie.peck'),
        'wings': dict(s('wings'), subtitle='subtitles.golebie.wings'),
    })


# ----------------------------------------------------------------------------------------------- lang, spawns
def lang():
    names_pl = {'gingham_black': 'w czarną kratkę', 'stripes_grey': 'w szare paski', 'gingham_red': 'w czerwoną kratkę',
                'gingham_blue': 'w niebieską kratkę', 'dots_pink': 'różowa w kropki', 'denim': 'dżinsowa'}
    names_en = {'gingham_black': 'Black Gingham', 'stripes_grey': 'Grey Striped', 'gingham_red': 'Red Gingham', 'gingham_blue': 'Blue Gingham',
                'dots_pink': 'Pink Polka-Dot', 'denim': 'Denim'}
    pl = {'itemGroup.golebie': 'Gołębie', 'entity.golebie.pigeon': 'Gołąb', 'entity.golebie.fantail': 'Pawik',
          'item.golebie.pigeon_spawn_egg': 'Jajo spawnujące gołębia', 'item.golebie.fantail_spawn_egg': 'Jajo spawnujące pawika',
          'subtitles.golebie.coo': 'Gołąb grucha', 'subtitles.golebie.bow_coo': 'Gołąb kłania się i grucha',
          'subtitles.golebie.purr': 'Gołąb mruczy z zadowolenia', 'subtitles.golebie.peck': 'Gołąb skubie',
          'subtitles.golebie.wings': 'Gołąb trzepocze skrzydłami',
          'golebie.tamed': '%s jest teraz twoim gołębiem!', 'golebie.stay': '%s zostaje tutaj', 'golebie.follow': '%s idzie z tobą',
          'golebie.tip': 'Mizianie: PPM pustą ręką · na ramię: Shift + PPM · zostań/chodź: PPM patykiem · zejdź z ramienia: przytrzymaj Shift',
          'item.golebie.diaper.tooltip': 'Załóż oswojonemu gołębiowi (PPM). Zdejmiesz nożycami.'}
    en = {'itemGroup.golebie': 'Pigeons', 'entity.golebie.pigeon': 'Pigeon', 'entity.golebie.fantail': 'Fantail Pigeon',
          'item.golebie.pigeon_spawn_egg': 'Pigeon Spawn Egg', 'item.golebie.fantail_spawn_egg': 'Fantail Spawn Egg',
          'subtitles.golebie.coo': 'Pigeon coos', 'subtitles.golebie.bow_coo': 'Pigeon bows and coos', 'subtitles.golebie.purr': 'Pigeon purrs',
          'subtitles.golebie.peck': 'Pigeon nibbles', 'subtitles.golebie.wings': 'Pigeon flutters',
          'golebie.tamed': '%s is your pigeon now!', 'golebie.stay': '%s stays here', 'golebie.follow': '%s comes with you',
          'golebie.tip': 'Stroke: right-click with an empty hand · onto your shoulder: Shift + right-click · stay/come: right-click with a stick · get off: hold Shift',
          'item.golebie.diaper.tooltip': 'Put it on a tame pigeon (right-click). Take it off with shears.'}
    for p in PATTERNS:
        pl[f'item.golebie.diaper_{p}'] = f'Pieluszka dla gołębia {names_pl[p]}'
        en[f'item.golebie.diaper_{p}'] = f'{names_en[p]} Pigeon Diaper'
    wj(f'{A}/lang/pl_pl.json', pl)
    wj(f'{A}/lang/en_us.json', en)


def spawns():
    BM = f'{D}/neoforge/biome_modifier'
    wj(f'{BM}/pigeons.json', {'type': 'neoforge:add_spawns', 'biomes': ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow',
                                                                         'minecraft:windswept_hills', 'minecraft:savanna'],
                              'spawners': {'type': f'{NS}:pigeon', 'weight': 6, 'minCount': 2, 'maxCount': 5}})
    wj(f'{BM}/fantails.json', {'type': 'neoforge:add_spawns', 'biomes': ['minecraft:sunflower_plains', 'minecraft:flower_forest'],
                               'spawners': {'type': f'{NS}:fantail', 'weight': 2, 'minCount': 1, 'maxCount': 2}})


if __name__ == '__main__':
    names = birds()
    items()
    sounds()
    lang()
    spawns()
    print(json.dumps(names))
