"""Hand-drawn 16x16 item sprites as text. Each sprite = 16 lines of 16 characters; '.' is transparent and every other
character is looked up in a palette. Grey palettes are meant to be tinted by the game (dyeable items)."""
from PIL import Image


def grey(outline=52, dark=120, mid=176, light=216, high=246):
    return {'o': (outline,) * 3, 'd': (dark,) * 3, 'm': (mid,) * 3, 'l': (light,) * 3, 'h': (high,) * 3}


def ramp(base, outline=0.35, dark=0.68, light=1.18, high=1.38):
    def k(c, f):
        return tuple(max(0, min(255, int(v * f))) for v in c)
    return {'o': k(base, outline), 'd': k(base, dark), 'm': tuple(base), 'l': k(base, light), 'h': k(base, high)}


def render(rows, pal):
    rows = [r.ljust(16, '.')[:16] for r in rows] + ['.' * 16] * (16 - len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    p = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                c = pal[ch]
                p[x, y] = tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,))
    return img


S = {}

# ball of yarn: wound strands + a loose end
S['yarn_ball'] = [
    '................',
    '.....oooooo.....',
    '...oollmmmdoo...',
    '..olhlmmldmmdo..',
    '..ollmmldmmddo..',
    '.olmmldmmldmddo.',
    '.olmldmmldmmddo.',
    '.ommdmmldmmddmo.',
    '.omdmmldmmddmdo.',
    '.odmmldmmddmddo.',
    '..odmdmmddmddo..',
    '..oddmmddmdddo..',
    '...ooddddddooo..',
    '.....oooooo.ldo.',
    '............odl.',
    '.............oo.',
]

# fluffy ball (angora, cashmere): fuzz around the edge
S['yarn_fluffy'] = [
    '......m..m......',
    '...m.oooooo.m...',
    '..moollmmmdoom..',
    '..olhlmmlmmmdo..',
    '.mollmmlmmmddom.',
    '.olmmlmmlmmmddo.',
    'molmlmmlmmmlddom',
    '.ommlmmlmmlmddo.',
    '.omlmmlmmlmmddo.',
    'modmmlmmlmmddmom',
    '.modmmlmmmlmdo..',
    '..oddmmlmmdddom.',
    '..mooddddddoom..',
    '....moooooom....',
    '.....m..m..m....',
    '................',
]

# hank / skein (linen, coarse yarn)
S['yarn_hank'] = [
    '................',
    '....oo....oo....',
    '...olmo..omlo...',
    '...olmdoodmlo...',
    '....odmllmdo....',
    '.....odlldo.....',
    '.....oddddo.....',
    '....ommlmmdo....',
    '...ommdoodmdo...',
    '...olmo..omdo...',
    '...olmo..omdo...',
    '...olmo..omdo...',
    '....olmoomdo....',
    '.....odmmdo.....',
    '......oooo......',
    '................',
]

# folded bolt of cloth with a woven texture
S['cloth'] = [
    '................',
    '................',
    '...oooooooooooo.',
    '..ohlhlhlhlhlhdo',
    '..olmlmlmlmlmldo',
    '..oddddddddddddo',
    '...oooooooooooo.',
    '..ohlhlhlhlhlhdo',
    '..olmlmlmlmlmldo',
    '..oddddddddddddo',
    '...oooooooooooo.',
    '..ohlhlhlhlhlhdo',
    '..olmlmlmlmlmldo',
    '..oddddddddddddo',
    '...oooooooooooo.',
    '................',
]

# felt: a pressed thick sheet, rolled at one end
S['felt'] = [
    '................',
    '................',
    '................',
    '....ooooooooo...',
    '...olmlmmlmmlo..',
    '..olmmlmmmlmmdo.',
    '.olmmmmlmmmmmdo.',
    '.ommlmmmmlmmddo.',
    '.ommmmmlmmmmddo.',
    '.odmmlmmmmmmddoo',
    '.odddmmmlmdddolo',
    '..oodddddddoolmo',
    '....oooooooodmo.',
    '...........ooo..',
    '................',
    '................',
]

# curry comb / grooming brush: wooden back, bristles below
S['curry_comb'] = [
    '................',
    '................',
    '.....oooooo.....',
    '....oHHhhhho....',
    '...oHhhhhhhhoo..',
    '..ohhhhhhhhhhwo.',
    '..owwwwwwwwwwwo.',
    '..oWWWWWWWWWWWo.',
    '..oooooooooooo..',
    '..obbbbbbbbbbo..',
    '..oBbBbBbBbBbo..',
    '..oBbBbBbBbBbo..',
    '..ooBoBoBoBoBo..',
    '....o.o.o.o.o...',
    '................',
    '................',
]
CURRY_PAL = {'o': (52, 34, 20), 'H': (214, 168, 104), 'h': (186, 138, 80), 'w': (150, 104, 58), 'W': (118, 80, 44),
             'b': (90, 72, 60), 'B': (60, 46, 38)}

# tuft of animal fibre (cashmere, llama, camel, dog hair)
S['tuft'] = [
    '................',
    '.......o........',
    '....o.olo..o....',
    '....olmlmoolo...',
    '..oolmhhmmlmo...',
    '.olmmhhlmmmmdo..',
    '.olmhlmmlmmmddo.',
    '..ommlmmmmlmddo.',
    '.olmmmmlmmmmdo..',
    '.odmmlmmmmmddo..',
    '..oddmmmlmdddo..',
    '...ooddmdddoo.o.',
    '..o..oodooo.....',
    '.......o........',
    '................',
    '................',
]

# horse hair: a long wavy hank tied in the middle
S['horse_hair'] = [
    '................',
    '.....o..o..o....',
    '....omomomomo...',
    '....omdmdmdmo...',
    '.....omdmdmo....',
    '.....omdmdmo....',
    '......omdmo.....',
    '.....ossssso....',
    '......omdmo.....',
    '.....omdmdmo....',
    '....omdmdmdmo...',
    '....omdmdmdmo...',
    '...omdmdmdmdmo..',
    '...omdo.omodmo..',
    '....o....o..o...',
    '................',
]


# ---------------------------------------------------------------- stage 2: animal and monster materials
S['hide'] = [
    '................',
    '..oo........oo..',
    '.olmo......omlo.',
    '.olmmoooooommdo.',
    '..olmmlmmlmmdo..',
    '...olmmmlmmmo...',
    '...omlmmmmldo...',
    '...ommmlmmmdo...',
    '...olmmmmlmdo...',
    '...ommlmmmmdo...',
    '..olmmmmlmmmdo..',
    '.olmmooooooomdo.',
    '.olmo......omdo.',
    '..oo........oo..',
]
S['horn'] = [
    '................',
    '................',
    '............oo..',
    '...........olo..',
    '..........olmo..',
    '.........olmdo..',
    '........olmdo...',
    '.......olmmdo...',
    '.....oolmmdo....',
    '...oolmmmddo....',
    '..olhlmmddo.....',
    '..olmmdddoo.....',
    '...oddooo.......',
    '....oo..........',
]
S['claw'] = [
    '................',
    '................',
    '................',
    '........ooo.....',
    '.......olmmo....',
    '......olmdmo....',
    '.....olmdo.o....',
    '....olmdo.......',
    '....olmdo.......',
    '...olmmdo.......',
    '...olmdo........',
    '...ommdo........',
    '....odo.........',
    '....oo..........',
]
S['spike'] = [
    '................',
    '.............oo.',
    '............olo.',
    '...........olmo.',
    '..........olmo..',
    '.........olmo...',
    '........olmo....',
    '.......olmo.....',
    '......olmo......',
    '.....olmo.......',
    '....odmo........',
    '...odmo.........',
    '..oddo..........',
    '..ooo...........',
]
S['lump'] = [
    '................',
    '................',
    '................',
    '....oooooooo....',
    '...ohhllllllo...',
    '..ohlllllllldo..',
    '..olllmmmmmldo..',
    '..olmmmmmmmmdo..',
    '..olmmmmmmmmdo..',
    '..olmmmmmmmddo..',
    '..odmmmmmmdddo..',
    '...oddddddddo...',
    '....oooooooo....',
]
S['fatback'] = [
    '................',
    '................',
    '................',
    '................',
    '..oooooooooooo..',
    '.ohhhhhhhhhhhlo.',
    '.ollllllllllldo.',
    '.oppppppppppppo.',
    '.oPPPPPPPPPPPPo.',
    '.ollllllllllldo.',
    '.oddddddddddddo.',
    '..oooooooooooo..',
]
S['cord'] = [
    '................',
    '................',
    '.....oooooo.....',
    '...oolmmmmdoo...',
    '..olmoo..oodmo..',
    '..omo......odo..',
    '..omo..oo..omo..',
    '..odo.olmo.omo..',
    '..odmoomdo.omo..',
    '...odmmdo..odo..',
    '....oooo..odo...',
    '.........odo....',
    '........odo.....',
    '.......oo.......',
]
S['bristles'] = [
    '................',
    '....o.o.o.o.....',
    '....m.d.m.d.o...',
    '...omdmdmdmo....',
    '...omdmdmdmo....',
    '...omdmdmdmo....',
    '...ossssssso....',
    '...omdmdmdmo....',
    '...omdmdmdmo....',
    '...omdmdmdmo....',
    '...omdmdmdmo....',
    '...ooooooooo....',
]
S['small_bone'] = [
    '................',
    '................',
    '................',
    '..........oo....',
    '.........olho...',
    '........olmmo...',
    '.......olmdo....',
    '......olmdo.....',
    '.....olmdo......',
    '....olmdo.......',
    '...olmmo........',
    '...ohlo.........',
    '....oo..........',
]
S['sac'] = [
    '................',
    '................',
    '......oooo......',
    '....oollmmoo....',
    '...olhlmmmmdo...',
    '..olhllmmmmddo..',
    '..ollmmmmmmddo..',
    '..olmmmmmmmddo..',
    '..olmmmmmmddo...',
    '...odmmmmddo....',
    '....oodddoo.....',
    '......ooo.......',
    '.......o........',
    '.......o........',
]
S['rag'] = [
    '................',
    '................',
    '..ooo.ooo..oo...',
    '.olmmolmmoolmo..',
    '.olmlmmlmmmlmdo.',
    '..ommmlmmlmmmdo.',
    '.olmlmmmmlmmdo..',
    '.ommmmlmmmlmmdo.',
    '..odmmmmlmmmdo..',
    '.olmmlmmmmmmddo.',
    '.odmmmmmlmmddo..',
    '..oddoddmddoo...',
    '...oo.oo.oo.....',
]
S['net'] = [
    '................',
    '................',
    '..m...m...m.....',
    '..mm.mmm.mmm....',
    '..m.m.m.m.m.m...',
    '...m...m...m.m..',
    '..m.m.m.m.m.m...',
    '..mm.mmm.mmm....',
    '..m...m...m.m...',
    '...m.m.m.m.m.m..',
    '....m...m...m...',
    '...m.m.m.m.m....',
    '..mm.mm.mm.mm...',
]
S['pouch'] = [
    '................',
    '.....g.gG.......',
    '....gGgGg.g.....',
    '.....oGgoo......',
    '....olmmmo......',
    '...olmmmmdo.....',
    '..olmlmmmmdo....',
    '..olmmmmmmdo....',
    '..olmmlmmmdo....',
    '..odmmmmmddo....',
    '...odddddoo.....',
    '....ooooo.......',
]
S['pile'] = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......oo.......',
    '.....oolmoo.....',
    '....olhlmmdo....',
    '...olllmmmddo...',
    '..olmlmmmmmddo..',
    '.olmmmmmmmmdddo.',
    '.oddddddddddddo.',
    '..oooooooooooo..',
]
S['wisp'] = [
    '................',
    '................',
    '....ooooo.......',
    '...ollllmo......',
    '..olo...omo.....',
    '..omo.oo.omo....',
    '..omo.olo.omo...',
    '...omo.omlomo...',
    '....omo..olo....',
    '.....ommoooo....',
    '......oooo.ooo..',
    '...........omlo.',
    '............oo..',
]
S['core'] = [
    '................',
    '................',
    '.......oo.......',
    '......olho......',
    '.....olhlmo.....',
    '....olllmmdo....',
    '...ollmmmmddo...',
    '...olmmmmmddo...',
    '...odmmmmmddo...',
    '....odmmmddo....',
    '.....odmddo.....',
    '......oddo......',
    '.......oo.......',
]
S['scale'] = [
    '................',
    '................',
    '....oooooooo....',
    '...ohhllllmmo...',
    '..ohllllmmmmdo..',
    '..olllmmmmmmdo..',
    '..ollmmmmmmddo..',
    '..olmmmmmmmddo..',
    '...olmmmmmddo...',
    '....olmmmddo....',
    '.....olmddo.....',
    '......oddo......',
    '.......oo.......',
]
S['tentacle'] = [
    '................',
    '................',
    '.........ooo....',
    '........olmmo...',
    '.......olmo.o...',
    '.......omo......',
    '.......omo......',
    '......olmo......',
    '.....olmdo......',
    '....olmdo.......',
    '...olmdo..o.o...',
    '..olmmdooomo....',
    '..odmmmmmdo.....',
    '...oddddoo......',
    '....oooo........',
]
S['jar'] = [
    '................',
    '................',
    '.....oooooo.....',
    '.....obbbbbo....',
    '....oooooooo....',
    '...occcccccco...',
    '...olmmmmmmdo...',
    '...olmmmmmmdo...',
    '...olmmmmmmdo...',
    '...olmmmmmmdo...',
    '...odmmmmmddo...',
    '....oddddddo....',
    '.....oooooo.....',
]
S['bar'] = [
    '................',
    '................',
    '................',
    '................',
    '....oooooooo....',
    '...ohhhlllllo...',
    '..ohlllllllldo..',
    '..ollllmmmmmdo..',
    '..olmmmmmmmmdo..',
    '..odmmmmmmmddo..',
    '...oddddddddo...',
    '....oooooooo....',
]


S['roll'] = [
    '................',
    '................',
    '................',
    '..........ooo...',
    '.........olhlo..',
    '........olmdmlo.',
    '.......olmdodmo.',
    '......olmmdddo..',
    '.....olmcmmdo...',
    '....olmmcmdo....',
    '...olmmmcdo.....',
    '..olmmmmdo......',
    '..odmmmdo.......',
    '...oddoo........',
    '....oo..........',
]


def pelt(base, kind='plain', tip=None, seed=1):
    """A whole pelt seen from above: head, ears, four legs and a tail, with fur streaks and an outline."""
    import math as _m
    import random as _r
    rnd = _r.Random(seed)
    mask = [[False] * 16 for _ in range(16)]
    tail = set()
    big = kind == 'bear'
    small = kind == 'rabbit'
    rx, ry = (5.2, 5.0) if big else (3.4, 3.8) if small else (4.2, 4.4)
    cy = 8.6
    for y in range(16):
        for x in range(16):
            if ((x + 0.5 - 8) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                mask[y][x] = True
            if (x + 0.5 - 8) ** 2 + (y + 0.5 - 3.3) ** 2 <= (2.6 if big else 2.1) ** 2:
                mask[y][x] = True
    ears = [(6, 1), (9, 1)] if not small else [(6, 0), (6, 1), (9, 0), (9, 1)]
    for x, y in ears:
        mask[y][x] = True
    legs = [(2, 5), (3, 5), (12, 5), (13, 5), (2, 12), (3, 12), (12, 12), (13, 12), (1, 4), (14, 4), (1, 13), (14, 13)]
    if big:
        legs = [(x - (1 if x < 8 else -1), y) for x, y in legs if 0 <= x - (1 if x < 8 else -1) < 16] + legs
    for x, y in legs:
        if 0 <= x < 16:
            mask[y][x] = True
    if kind == 'fox':
        for x, y in [(8, 13), (8, 14), (9, 14), (9, 15), (10, 15), (7, 13), (7, 14), (8, 15)]:
            mask[y][x] = True
            tail.add((x, y))
    elif kind == 'rabbit':
        for x, y in [(7, 12), (8, 12), (7, 13), (8, 13)]:
            mask[y][x] = True
            tail.add((x, y))
    elif not big:
        for x, y in [(8, 13), (8, 14), (8, 15)]:
            mask[y][x] = True
    from PIL import Image
    img = Image.new('RGBA', (16, 16))
    p = img.load()
    b = tuple(base)

    def k(c, f):
        return tuple(max(0, min(255, int(v * f))) for v in c)
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            c = b
            if tip and (x, y) in tail and y >= 14 or (tip and kind == 'rabbit' and (x, y) in tail):
                c = tuple(tip)
            if edge:
                p[x, y] = k(c, 0.38) + (255,)
                continue
            light = 1.0 + 0.16 * (((8 - x) + (8 - y)) / 8.0)
            streak = 0.9 if (x + y * 2 + rnd.randint(0, 1)) % 4 == 0 else 1.0
            if rnd.random() < 0.12:
                streak *= 1.12
            p[x, y] = k(c, light * streak) + (255,)
    # little dark eyes on the head... no: it is a pelt, mark the spine with a slightly darker line
    for y in range(4, 13):
        if mask[y][8] and p[8, y][3]:
            r, g, bb, a = p[8, y]
            if p[9, y][3] and p[7, y][3]:
                p[8, y] = (int(r * 0.88), int(g * 0.88), int(bb * 0.88), 255)
    return img


def spots(img, color, pts):
    p = img.load()
    for x, y in pts:
        if p[x, y][3]:
            p[x, y] = tuple(color) + (255,)
    return img


S['pelt'] = [
    '......o..o......',
    '.....olmmlo.....',
    '.....omoomo.....',
    '..oo..olmo..oo..',
    '.olloolmmmooldo.',
    '..olmmlmmmmmdo..',
    '...olmmmlmmmo...',
    '...omlmmmmldo...',
    '...ommmlmmmdo...',
    '...olmmmmlmdo...',
    '...ommlmmmmdo...',
    '..olmmmmlmmmdo..',
    '.olmmoommmooddo.',
    '..oo..ommdo.oo..',
    '.......otto.....',
    '........oo......',
]
