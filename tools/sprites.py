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
    assert len(rows) == 16, len(rows)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    p = img.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
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
