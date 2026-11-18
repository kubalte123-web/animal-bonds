"""Generates block/item models, blockstates, textures, recipes and loot tables for Animal Bonds.
Run: python3 tools/gen_assets.py  (from the repo root)."""
import json, os, random
from PIL import Image, ImageDraw

R = 'src/main/resources'
A = f'{R}/assets/animalbonds'
D = f'{R}/data/animalbonds'

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)

def face(tex, uv=None, cull=None):
    f = {'texture': tex}
    if uv: f['uv'] = uv
    if cull: f['cullface'] = cull
    return f

def cube(frm, to, tex, rot=None, faces=('north','south','east','west','up','down'), uvs=None):
    e = {'from': frm, 'to': to, 'faces': {}}
    for fc in faces:
        t = tex[fc] if isinstance(tex, dict) else tex
        uv = uvs.get(fc) if uvs else None
        e['faces'][fc] = face(t, uv)
    if rot: e['rotation'] = rot
    return e

# ---------------- textures ----------------
def save(img, rel):
    p = f'{A}/textures/{rel}.png'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)

rnd = random.Random(42)

def noise(base, var, size=16, seed=0):
    r = random.Random(seed)
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            d = r.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    return img

# book pages: cream with ink lines, darker gutter at the inner edge (x=15 for left page)
pages = noise((236, 222, 186), 6, seed=1)
dr = ImageDraw.Draw(pages)
for y in range(3, 15, 2):
    dr.line([(2, y), (13 - (y % 4), y)], fill=(120, 100, 80, 255))
for y in range(16):
    pages.putpixel((15, y), (190, 170, 135, 255))
    pages.putpixel((0, y), (215, 200, 165, 255))
save(pages, 'block/book_pages')

cover = noise((110, 58, 32), 10, seed=2)
dc = ImageDraw.Draw(cover)
dc.rectangle([0, 0, 15, 15], outline=(70, 35, 18, 255))
dc.rectangle([2, 2, 13, 13], outline=(196, 160, 72, 255))
save(cover, 'block/book_cover')

# saddlebags look like small chests strapped to the creature (like a llama's chests)
bag = noise((156, 110, 56), 8, seed=3)
db = ImageDraw.Draw(bag)
for yy in (3, 7, 11):
    db.line([(1, yy), (14, yy)], fill=(128, 88, 42, 255))
db.rectangle([0, 0, 15, 15], outline=(66, 44, 22, 255))
db.line([(0, 5), (15, 5)], fill=(66, 44, 22, 255))
db.rectangle([6, 4, 9, 8], fill=(190, 190, 196, 255), outline=(90, 90, 96, 255))
db.point((7, 6), fill=(40, 40, 44, 255)); db.point((8, 6), fill=(40, 40, 44, 255))
save(bag, 'entity/saddlebag')

item = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
di = ImageDraw.Draw(item)
di.line([(3, 3), (12, 3)], fill=(70, 40, 20, 255))
di.line([(3, 3), (2, 6)], fill=(70, 40, 20, 255))
di.line([(12, 3), (13, 6)], fill=(70, 40, 20, 255))
for (x0, x1) in ((0, 6), (9, 15)):
    di.rectangle([x0, 6, x1, 14], fill=(156, 110, 56, 255), outline=(66, 44, 22, 255))
    di.line([(x0, 9), (x1, 9)], fill=(66, 44, 22, 255))
    cx = (x0 + x1) // 2
    di.rectangle([cx - 1, 8, cx + 1, 10], fill=(200, 200, 206, 255))
save(item, 'item/saddlebags')

# GUI book 320x200: leather border, two cream pages, a spine.
W, H = 320, 200
gui = Image.new('RGBA', (W, H), (0, 0, 0, 0))
g = ImageDraw.Draw(gui)
g.rounded_rectangle([0, 0, W - 1, H - 1], radius=10, fill=(104, 56, 30, 255), outline=(58, 30, 14, 255), width=2)
r2 = random.Random(5)
for _ in range(2500):
    x, y = r2.randint(2, W - 3), r2.randint(2, H - 3)
    c = gui.getpixel((x, y))
    if c[3] == 255:
        d = r2.randint(-12, 12)
        gui.putpixel((x, y), (max(0, c[0] + d), max(0, c[1] + d), max(0, c[2] + d), 255))
g.rounded_rectangle([8, 8, W - 9, H - 9], radius=6, outline=(196, 160, 72, 255), width=1)
for (x0, x1) in ((10, W // 2 - 2), (W // 2 + 2, W - 11)):
    g.rectangle([x0, 10, x1, H - 11], fill=(240, 228, 196, 255))
for x in range(10, W // 2 - 1):
    t = (x - 10) / (W // 2 - 12)
    shade = int(26 * t * t)
    g.line([(x, 10), (x, H - 11)], fill=(240 - shade, 228 - shade, 196 - shade, 255))
for x in range(W // 2 + 2, W - 10):
    t = 1 - (x - (W // 2 + 2)) / (W // 2 - 12)
    shade = int(26 * t * t)
    g.line([(x, 10), (x, H - 11)], fill=(240 - shade, 228 - shade, 196 - shade, 255))
for _ in range(1800):
    x, y = r2.randint(11, W - 12), r2.randint(11, H - 12)
    c = gui.getpixel((x, y))
    if c[0] > 180:
        d = r2.randint(-6, 4)
        gui.putpixel((x, y), (c[0] + d, c[1] + d, c[2] + d, 255))
g.rectangle([W // 2 - 2, 8, W // 2 + 1, H - 9], fill=(84, 44, 22, 255))
# frame for the creature portrait on the left page
g.rounded_rectangle([20, 34, W // 2 - 16, H - 58], radius=4, outline=(170, 140, 100, 255), width=1)
# decorative divider under the title area on the left page
g.line([(30, 31), (W // 2 - 26, 31)], fill=(170, 140, 100, 255))
save(gui, 'gui/bond_book')

# ---------------- models ----------------
PL = 'minecraft:block/oak_planks'
LOG = 'minecraft:block/oak_log'
STRIP = 'minecraft:block/stripped_oak_log'
rotx = {'origin': [8, 12, 8], 'axis': 'x', 'angle': -22.5}
pedestal = {
    'parent': 'minecraft:block/block',
    'textures': {'particle': PL, 'planks': PL, 'log': LOG, 'strip': STRIP,
                 'pages': 'animalbonds:block/book_pages', 'cover': 'animalbonds:block/book_cover'},
    'elements': [
        cube([2, 0, 2], [14, 2, 14], '#planks'),
        cube([3, 2, 3], [13, 3, 13], '#strip'),
        cube([5, 3, 5], [11, 10, 11], '#log', uvs={'north': [5, 0, 11, 7], 'south': [5, 0, 11, 7], 'east': [5, 0, 11, 7], 'west': [5, 0, 11, 7]}),
        cube([4, 10, 4], [12, 11.5, 12], '#strip'),
        cube([1, 11.5, 2], [15, 13, 14], '#planks', rot=rotx),
        cube([1.5, 13, 2.5], [14.5, 13.5, 13.5], '#cover', rot=rotx),
        cube([2, 13.5, 3], [7.9, 14.3, 13], {'up': '#pages', 'north': '#pages', 'south': '#pages', 'east': '#pages', 'west': '#pages', 'down': '#cover'}, rot=rotx,
             uvs={'up': [0, 0, 16, 16], 'north': [0, 14, 16, 16], 'south': [0, 14, 16, 16], 'west': [0, 14, 16, 16], 'east': [0, 14, 16, 16]}),
        cube([8.1, 13.5, 3], [14, 14.3, 13], {'up': '#pages', 'north': '#pages', 'south': '#pages', 'east': '#pages', 'west': '#pages', 'down': '#cover'}, rot=rotx,
             uvs={'up': [16, 0, 0, 16], 'north': [0, 14, 16, 16], 'south': [0, 14, 16, 16], 'west': [0, 14, 16, 16], 'east': [0, 14, 16, 16]}),
        cube([7.6, 13.5, 2.8], [8.4, 14.0, 13.2], '#cover', rot=rotx),
    ],
    'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [0.6, 0.6, 0.6]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    },
}
w(f'{A}/models/block/bond_pedestal.json', pedestal)
w(f'{A}/blockstates/bond_pedestal.json', {'variants': {
    f'facing={d}': ({'model': 'animalbonds:block/bond_pedestal', 'y': y} if y else {'model': 'animalbonds:block/bond_pedestal'})
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})

# trough: 4 walls + bottom; fill layer grows with fill level
for fill in range(5):
    els = [
        cube([0, 0, 0], [16, 2, 16], '#planks'),
        cube([0, 2, 0], [16, 8, 2], '#planks'),
        cube([0, 2, 14], [16, 8, 16], '#planks'),
        cube([0, 2, 2], [2, 8, 14], '#planks'),
        cube([14, 2, 2], [16, 8, 14], '#planks'),
    ]
    if fill > 0:
        els.append(cube([2, 2, 2], [14, 2 + fill * 1.4, 14], '#food', faces=('up', 'north', 'south', 'east', 'west')))
    w(f'{A}/models/block/feeding_trough_{fill}.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': PL, 'planks': 'minecraft:block/spruce_planks', 'food': 'minecraft:block/hay_block_top'},
        'elements': els})
w(f'{A}/blockstates/feeding_trough.json', {'variants': {f'fill={i}': {'model': f'animalbonds:block/feeding_trough_{i}'} for i in range(5)}})

nest = {
    'parent': 'minecraft:block/block',
    'textures': {'particle': 'minecraft:block/hay_block_top', 'hay': 'minecraft:block/hay_block_top', 'side': 'minecraft:block/hay_block_side', 'wool': 'minecraft:block/brown_wool'},
    'elements': [
        cube([1, 0, 1], [15, 3, 15], {'up': '#hay', 'down': '#hay', 'north': '#side', 'south': '#side', 'east': '#side', 'west': '#side'},
             uvs={'north': [0, 0, 16, 3], 'south': [0, 0, 16, 3], 'east': [0, 0, 16, 3], 'west': [0, 0, 16, 3]}),
        cube([1, 3, 1], [15, 4, 3], '#wool'),
        cube([1, 3, 13], [15, 4, 15], '#wool'),
        cube([1, 3, 3], [3, 4, 13], '#wool'),
        cube([13, 3, 3], [15, 4, 13], '#wool'),
    ]}
w(f'{A}/models/block/animal_bed.json', nest)
w(f'{A}/blockstates/animal_bed.json', {'variants': {'': {'model': 'animalbonds:block/animal_bed'}}})

# item definitions (assets/<ns>/items/<id>.json)
for blk, mdl in (('bond_pedestal', 'bond_pedestal'), ('feeding_trough', 'feeding_trough_3'), ('animal_bed', 'animal_bed')):
    w(f'{A}/items/{blk}.json', {'model': {'type': 'minecraft:model', 'model': f'animalbonds:block/{mdl}'}})
w(f'{A}/models/item/saddlebags.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'animalbonds:item/saddlebags'}})
w(f'{A}/items/saddlebags.json', {'model': {'type': 'minecraft:model', 'model': 'animalbonds:item/saddlebags'}})

# ---------------- loot tables ----------------
for blk in ('bond_pedestal', 'feeding_trough', 'animal_bed'):
    w(f'{D}/loot_table/blocks/{blk}.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': f'animalbonds:{blk}'}]}],
        'random_sequence': f'animalbonds:blocks/{blk}'})

# ---------------- recipes ----------------
w(f'{D}/recipe/bond_pedestal.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'key': {'B': 'minecraft:book', 'L': 'minecraft:leather', 'S': '#minecraft:wooden_slabs', 'G': '#minecraft:logs'},
    'pattern': [' B ', 'LGL', ' S '],
    'result': {'id': 'animalbonds:bond_pedestal'}})
w(f'{D}/recipe/feeding_trough.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'key': {'P': '#minecraft:planks', 'W': 'minecraft:wheat'},
    'pattern': ['PWP', 'PPP'],
    'result': {'id': 'animalbonds:feeding_trough', 'count': 2}})
w(f'{D}/recipe/animal_bed.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'key': {'H': 'minecraft:hay_block', 'W': '#minecraft:wool'},
    'pattern': ['WHW'],
    'result': {'id': 'animalbonds:animal_bed', 'count': 2}})
w(f'{D}/recipe/saddlebags.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'key': {'L': 'minecraft:leather', 'C': 'minecraft:chest', 'S': 'minecraft:string'},
    'pattern': ['LSL', 'C C', 'L L'],
    'result': {'id': 'animalbonds:saddlebags'}})

# splicing altar
altar = {
    'parent': 'minecraft:block/block',
    'textures': {'particle': 'minecraft:block/polished_blackstone', 'base': 'minecraft:block/polished_blackstone',
                 'pillar': 'minecraft:block/crying_obsidian', 'top': 'minecraft:block/purpur_block',
                 'crystal': 'minecraft:block/amethyst_cluster', 'gem': 'minecraft:block/amethyst_block'},
    'elements': [
        cube([0, 0, 0], [16, 4, 16], '#base'),
        cube([3, 4, 3], [13, 12, 13], '#pillar'),
        cube([1, 12, 1], [15, 15, 15], '#top'),
        cube([6, 15, 6], [10, 16, 10], '#gem'),
        {'from': [3, 15, 8], 'to': [13, 25, 8], 'rotation': {'origin': [8, 15, 8], 'axis': 'y', 'angle': 45},
         'shade': False, 'faces': {'north': face('#crystal', [0, 0, 16, 16]), 'south': face('#crystal', [0, 0, 16, 16])}},
        {'from': [8, 15, 3], 'to': [8, 25, 13], 'rotation': {'origin': [8, 15, 8], 'axis': 'y', 'angle': 45},
         'shade': False, 'faces': {'east': face('#crystal', [0, 0, 16, 16]), 'west': face('#crystal', [0, 0, 16, 16])}},
    ],
    'display': pedestal['display'],
}
w(f'{A}/models/block/splicing_altar.json', altar)
w(f'{A}/blockstates/splicing_altar.json', {'variants': {'': {'model': 'animalbonds:block/splicing_altar'}}})
w(f'{A}/items/splicing_altar.json', {'model': {'type': 'minecraft:model', 'model': 'animalbonds:block/splicing_altar'}})
w(f'{D}/loot_table/blocks/splicing_altar.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
               'entries': [{'type': 'minecraft:item', 'name': 'animalbonds:splicing_altar'}]}],
    'random_sequence': 'animalbonds:blocks/splicing_altar'})
w(f'{D}/recipe/splicing_altar.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'key': {'S': 'minecraft:amethyst_shard', 'G': 'minecraft:golden_apple', 'B': 'minecraft:book', 'O': 'minecraft:obsidian'},
    'pattern': ['SGS', 'OBO', 'OOO'],
    'result': {'id': 'animalbonds:splicing_altar'}})
w(f'{R}/data/minecraft/tags/block/mineable/pickaxe.json', {'values': ['animalbonds:splicing_altar']})

# mineable tag
w(f'{R}/data/minecraft/tags/block/mineable/axe.json', {'values': ['animalbonds:bond_pedestal', 'animalbonds:feeding_trough']})
print('assets generated')
