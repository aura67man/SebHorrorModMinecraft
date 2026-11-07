#!/usr/bin/env python3
"""Generates every texture in the mod (original pixel art) plus the Sebastian Hili skin.

Run from the project root:  python3 tools/generate_textures.py
Requires Pillow. Output is deterministic (fixed random seeds).
"""
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/sebastianhilihorror/textures")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def jitter(rng, c, amt=0.06):
    return shade(c, 1 + rng.uniform(-amt, amt))


def save(img, rel):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


# ---------------------------------------------------------------- skin
def paint_grid(img, x0, y0, rows, palette, rng=None, amt=0.0):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch == ".":
                continue
            c = palette[ch]
            if rng is not None and amt:
                c = jitter(rng, c, amt)
            img.putpixel((x0 + dx, y0 + dy), c)


def fill(img, x0, y0, w, h, c, rng=None, amt=0.0):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), jitter(rng, c, amt) if rng else c)


def make_skin():
    rng = random.Random(1987)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    P = {
        "H": hexc("#33231b"),  # hair
        "h": hexc("#4a3226"),  # hair highlight
        "k": hexc("#1b120e"),  # hair shadow
        "S": hexc("#ecc0a6"),  # skin
        "s": hexc("#d9a68c"),  # skin shade
        "r": hexc("#e8a99a"),  # cheek / ear
        "G": hexc("#141215"),  # glasses frame
        "W": hexc("#e9e0da"),  # eye white
        "E": hexc("#5a3a26"),  # iris
        "n": hexc("#b98370"),  # nostril shade
        "M": hexc("#8f4a44"),  # mouth corner
        "t": hexc("#f0d6cc"),  # teeth peeking
        "l": hexc("#dda394"),  # lower lip
        "B": hexc("#21402f"),  # blazer
        "b": hexc("#18301f"),  # blazer shade
        "v": hexc("#2d5340"),  # blazer highlight
        "w": hexc("#eeeeee"),  # shirt
        "q": hexc("#cfcfd4"),  # shirt shade
        "T": hexc("#16161c"),  # tie
        "y": hexc("#c9a640"),  # crest
        "P": hexc("#24242c"),  # trousers
        "p": hexc("#1b1b21"),  # trouser shade
        "O": hexc("#111111"),  # shoe
        "o": hexc("#2a2a2a"),  # shoe shine
    }
    A = 0.05
    # ---------------- head (base layer)
    paint_grid(img, 8, 0, [  # top (front edge is the bottom row)
        "kHHhHHHk", "HHhHHhHH", "HhHHkHHH", "HHHhHHhH",
        "HkHHHhHH", "HHhHHHHk", "HHHHhHHH", "hHHkHHhH"], P, rng, A)
    paint_grid(img, 16, 0, [  # bottom (chin/neck)
        "ssssssss", "sSSSSSSs", "sSSSSSSs", "sSSSSSSs",
        "sSSSSSSs", "sSSSSSSs", "kHHHHHHk", "kkHHHHkk"], P, rng, 0.03)
    paint_grid(img, 8, 8, [  # FRONT face
        "HHhHHHhH",
        "HhHHkHHH",
        "HSSHsSSH",   # messy fringe, the curl drops at column 3
        "GGGGGGGG",   # top of the rectangular frames
        "GWEGGEWG",   # eyes behind lenses
        "GGGsnGGG",   # bottom of frames + nose
        "SSMttMsS",   # awkward, closed-ish smile (one side a bit lopsided)
        "sSSllSSs"], P, rng, 0.03)
    paint_grid(img, 0, 8, [  # right side (col 7 touches the face)
        "HHHHHhHH", "HhHHHHHH", "HHHkHHHH", "HHHHHHsS",
        "HHHrrGGG", "HHkrrsSS", "HHHsSSSS", "kHsssSSs"], P, rng, A)
    paint_grid(img, 16, 8, [  # left side (col 0 touches the face)
        "HHhHHHHH", "HHHHHhHH", "HHHHkHHH", "SsHHHHHH",
        "GGGrrHHH", "SSsrrkHH", "SSSSsHHH", "sSSssHHk"], P, rng, A)
    paint_grid(img, 24, 8, [  # back
        "HHhHHHkH", "HkHHhHHH", "HHHHHHhH", "hHHkHHHH",
        "HHHHHhHH", "HhHHkHHH", "HHHHHHHh", "kHHhHHkk"], P, rng, A)
    # ---------------- hat layer: wavy, messy volume
    paint_grid(img, 40, 0, [
        "h.H..h.H", ".HH.kH..", "H..h..H.", ".k.HH..h",
        "H.h..H.H", ".H..h..k", "h.kH..H.", "H..h.H.H"], P, rng, A)
    paint_grid(img, 40, 8, [  # front of hat: fringe + the signature curl
        "H.hk.H.h",
        ".H.h..H.",
        "H...h..H",   # curl hooks round at column 4
        "........", "........", "........", "........", "........"], P, rng, A)
    paint_grid(img, 32, 8, [
        "H.hH.H.H", ".H..H.hH", "k.H..H..", "H..h....",
        ".H......", "H.k.....", "........", "........"], P, rng, A)
    paint_grid(img, 48, 8, [
        "H.H.Hh.H", "Hh.H..H.", "..H..H.k", "....h..H",
        "......H.", ".....k.H", "........", "........"], P, rng, A)
    paint_grid(img, 56, 8, [
        "H.hH.H.H", ".H.kH.H.", "H.H..h.H", ".k.H.H..",
        "H..h..Hk", ".H..H.h.", "h.k..H..", "........"], P, rng, A)

    # ---------------- body
    paint_grid(img, 20, 16, ["BBwwwwBB", "bBBwwBBb", "BBBBBBBB", "bBBBBBBb"], P, rng, A)  # top
    paint_grid(img, 28, 16, ["bbbbbbbb", "PPPPPPPP", "PPPPPPPP", "pppppppp"], P, rng, A)  # bottom
    paint_grid(img, 20, 20, [  # front
        "BwwTTwwB",
        "vBwTTwBv",
        "BvqTTqvB",
        "BBvTTvyB",
        "BBBvvBBB",
        "BBBbBBBB",
        "BbBBBBBB",
        "BBBbBBbB",
        "bbbBBbbb",
        "BBBbBBBB",
        "BBBBBBBB",
        "bBBbbBBb"], P, rng, A)
    paint_grid(img, 32, 20, [  # back
        "BBwwwwBB", "BBBBBBBB", "BBBBBBBB", "BvBBBBvB",
        "BBBBBBBB", "BBBBBBBB", "BBBBBBBB", "BBBbBBBB",
        "BBBbBBBB", "BBBbBBBB", "BBBkBBBB", "bbbkbbbb"], {**P, "k": P["b"]}, rng, A)
    for x0 in (16, 28):  # sides
        paint_grid(img, x0, 20, ["BBBB", "BvBB", "BBBB", "BBBB", "BBvB", "BBBB",
                                 "BBBB", "BBBb", "BBBB", "bBBB", "BBBB", "bbbb"], P, rng, A)

    # ---------------- arms (classic 4px)
    sleeve = ["BBBB", "BvBB", "BBBB", "BBBB", "BBvB", "BBBB",
              "BBBB", "BBBb", "BbBB", "wwww", "SSSS", "sSSs"]
    for x0 in (40, 44, 48, 52):  # right arm faces
        paint_grid(img, x0, 20, sleeve, P, rng, A)
    for x0 in (32, 36, 40, 44):  # left arm faces
        paint_grid(img, x0, 52, sleeve, P, rng, A)
    paint_grid(img, 44, 16, ["BBBB", "BvBB", "BBBB", "bBBb"], P, rng, A)  # R arm top
    paint_grid(img, 48, 16, ["sSSs", "SSSS", "SSSS", "sSSs"], P, rng, A)  # R arm bottom (hand)
    paint_grid(img, 36, 48, ["BBBB", "BBvB", "BBBB", "bBBb"], P, rng, A)  # L arm top
    paint_grid(img, 40, 48, ["sSSs", "SSSS", "SSSS", "sSSs"], P, rng, A)  # L arm bottom

    # ---------------- legs
    leg = ["PPPP", "PPPP", "PPpP", "PPPP", "PpPP", "PPPP",
           "PPPP", "PPPp", "PPPP", "pPPp", "OoOO", "OOOO"]
    for x0 in (0, 4, 8, 12):
        paint_grid(img, x0, 20, leg, P, rng, A)
    for x0 in (16, 20, 24, 28):
        paint_grid(img, x0, 52, leg, P, rng, A)
    paint_grid(img, 4, 16, ["pPPp", "PPPP", "PPPP", "pPPp"], P, rng, A)
    paint_grid(img, 8, 16, ["OOOO", "OOOO", "OOOO", "OOOO"], P, rng, 0.02)
    paint_grid(img, 20, 48, ["pPPp", "PPPP", "PPPP", "pPPp"], P, rng, A)
    paint_grid(img, 24, 48, ["OOOO", "OOOO", "OOOO", "OOOO"], P, rng, 0.02)
    # trouser crease lines on the fronts
    for (x, y) in ((5, 21), (5, 23), (5, 25), (5, 27), (22, 53), (22, 55), (22, 57), (22, 59)):
        img.putpixel((x, y), P["p"])
    return img


def skin_preview(skin, scale=12):
    """Flat front + back view (with overlay layers) for checking the skin."""
    W, H = 16 * 2 + 8, 32
    out = Image.new("RGBA", (W, H), (110, 140, 200, 255))

    def blit(sx, sy, w, h, dx, dy):
        region = skin.crop((sx, sy, sx + w, sy + h))
        out.alpha_composite(region, (dx, dy))

    def figure(ox, front=True):
        if front:
            blit(8, 8, 8, 8, ox + 4, 0); blit(40, 8, 8, 8, ox + 4, 0)
            blit(20, 20, 8, 12, ox + 4, 8); blit(20, 36, 8, 12, ox + 4, 8)
            blit(44, 20, 4, 12, ox + 0, 8); blit(44, 36, 4, 12, ox + 0, 8)
            blit(36, 52, 4, 12, ox + 12, 8); blit(52, 52, 4, 12, ox + 12, 8)
            blit(4, 20, 4, 12, ox + 4, 20); blit(4, 36, 4, 12, ox + 4, 20)
            blit(20, 52, 4, 12, ox + 8, 20); blit(4, 52, 4, 12, ox + 8, 20)
        else:
            blit(24, 8, 8, 8, ox + 4, 0); blit(56, 8, 8, 8, ox + 4, 0)
            blit(32, 20, 8, 12, ox + 4, 8)
            blit(44, 52, 4, 12, ox + 0, 8); blit(52, 20, 4, 12, ox + 12, 8)
            blit(28, 52, 4, 12, ox + 4, 20); blit(12, 20, 4, 12, ox + 8, 20)

    figure(0, True)
    figure(24, False)
    return out.resize((W * scale, H * scale), Image.NEAREST)


# ---------------------------------------------------------------- blocks & items
def stone_base(rng, deep=False):
    img = Image.new("RGBA", (16, 16))
    if deep:
        cols = [hexc("#4b4b52"), hexc("#3f3f46"), hexc("#55555c"), hexc("#36363c")]
    else:
        cols = [hexc("#7d7d7d"), hexc("#737373"), hexc("#868686"), hexc("#6a6a6a")]
    for y in range(16):
        for x in range(16):
            if deep:  # horizontal layered feel
                band = (y // 3 + (x // 7)) % 4
                c = cols[band if rng.random() < 0.55 else rng.randrange(4)]
            else:
                c = cols[rng.randrange(4)] if rng.random() < 0.5 else cols[0]
            img.putpixel((x, y), jitter(rng, c, 0.04))
    # a few cracks
    for _ in range(4 if not deep else 6):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(2, 4)):
            img.putpixel((x % 16, y % 16), shade(img.getpixel((x % 16, y % 16)), 0.82))
            x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1))
    return img


ZINC = [hexc("#dfe7ea"), hexc("#b8c6cc"), hexc("#8fa1a9"), hexc("#5f6f78")]  # light -> dark

# Ore textures are OVERLAYS: transparent except for the zinc nuggets. The block model draws
# vanilla stone/deepslate underneath (models/block/zinc_ore.json), so the ore always matches the
# world, including resource packs. Nugget style follows vanilla iron ore: clusters with a light
# top-right, mid body, darker right/bottom edge and a dark rim on the left/bottom.
ORE_STONE = [
    "..dd.........dc.",
    ".dbad......ddbc.",
    "..dbbc....dabbd.",
    "...dcc.....dcd..",
    "......d.........",
    "....ddbad.......",
    "...dbaabbcd..d..",
    "....dcbbcd..dbd.",
    ".d....dd...dabc.",
    "dbad........dcd.",
    "dabcd.....dd....",
    ".dcc....ddbad...",
    "..d....dbaabcd..",
    "........dcbcc...",
    "...dbd...ddd....",
    "...dad..........",
]
ORE_DEEPSLATE = [
    "................",
    "...ddbd....d....",
    "..dbaabd..dbd...",
    "...dcbcc.dbacd..",
    ".....dd...dccd..",
    ".............d..",
    ".dd.............",
    "dbad.....ddbd...",
    "dabcd...dbaabd..",
    ".dcc.....dcbcc..",
    "..d.......ddd...",
    "............ddbd",
    "....ddbd...dbaab",
    "...dbaacd...dcbc",
    "....dcbcc....dd.",
    ".....dd.........",
]


def ore(deep):
    rng = random.Random(42 if not deep else 43)
    if deep:  # deepslate ores are darker and less saturated, like vanilla
        pal = {"a": hexc("#c6d1d8"), "b": hexc("#9eacb5"), "c": hexc("#707d86"), "d": hexc("#464c54")}
    else:
        pal = {"a": hexc("#d0dae0"), "b": hexc("#b0bdc4"), "c": hexc("#8c9aa3"), "d": hexc("#68727a")}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paint_grid(img, 0, 0, ORE_DEEPSLATE if deep else ORE_STONE, pal, rng, 0.03)
    return img


def raw_zinc_block():
    rng = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    pal = [hexc("#9fb0b7"), hexc("#8a9ba3"), hexc("#b6c4ca"), hexc("#73848c"), hexc("#d2dde1")]
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), jitter(rng, pal[0], 0.04))
    # lumps
    for _ in range(14):
        cx, cy, r = rng.randrange(16), rng.randrange(16), rng.randint(1, 3)
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r, cx + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    xx, yy = x % 16, y % 16
                    d = (x - cx + y - cy)
                    c = pal[4] if d < -r else pal[2] if d < 0 else pal[1] if d < r else pal[3]
                    img.putpixel((xx, yy), jitter(rng, c, 0.03))
    return img


def zinc_block():
    rng = random.Random(9)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = ZINC[1]
            if x == 0 or y == 0:
                c = ZINC[0]
            elif x == 15 or y == 15:
                c = ZINC[3]
            elif x == 1 or y == 1:
                c = shade(ZINC[0], 0.97)
            elif x == 14 or y == 14:
                c = ZINC[2]
            elif (x + y) % 9 == 0 and 3 < x < 13:
                c = shade(ZINC[0], 1.0)  # diagonal sheen
            img.putpixel((x, y), jitter(rng, c, 0.025))
    # panel seam + rivets
    for x in range(2, 14):
        img.putpixel((x, 8), ZINC[2])
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), ZINC[3]); img.putpixel((x - 1, y - 1), ZINC[0])
    return img


def item_from_grid(rows, pal, seed, amt=0.03):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paint_grid(img, 0, 0, rows, pal, rng, amt)
    return img


ITEM_PAL = {"a": ZINC[0], "b": ZINC[1], "c": ZINC[2], "d": ZINC[3], "o": hexc("#3c474d")}

RAW_ZINC = [
    "................",
    "................",
    "......oooo......",
    "....ooaabbo.....",
    "...oabbbbcco....",
    "..oabbabbccdo...",
    "..obbbbbbcccdo..",
    ".oabbbcbbbccdo..",
    ".obbbccbbcccdo..",
    ".obbccbbbbccddo.",
    "..obbbbbccccdo..",
    "..ocbbccccddo...",
    "...occcccddo....",
    "....ooddddo.....",
    "......oooo......",
    "................",
]

ZINC_INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........oooo....",
    "......ooaaabo...",
    "....ooaaabbbco..",
    "..ooaaabbbbccdo.",
    ".oaaabbbbbccddo.",
    ".obbbbbbccdddo..",
    ".occbbbccddoo...",
    "..oddccdddo.....",
    "...ooddoo.......",
    ".....oo.........",
    "................",
    "................",
]


def penny():
    pal = {
        "o": hexc("#4a2414"),  # outline
        "r": hexc("#8c4a28"),  # rim
        "R": hexc("#b56a3c"),  # rim highlight
        "c": hexc("#c97a45"),  # face
        "C": hexc("#e0985e"),  # face highlight
        "d": hexc("#a65c33"),  # relief shade
        "s": hexc("#f2c08e"),  # shine
    }
    rows = [
        "................",
        ".....oooooo.....",
        "...ooRRRRRRoo...",
        "..oRRccccCCRro..",
        "..oRcccddcCsRo..",
        ".oRcccddddcCCro.",
        ".oRccdddddcccro.",
        ".oRccdddddcccro.",
        ".oRcccdddccccro.",
        ".oRccccddcccdro.",
        ".orcccddddccdro.",
        "..orcccccccdro..",
        "..orrccccdddro..",
        "...oorrrrrrroo..",
        ".....oooooo.....",
        "................",
    ]
    return item_from_grid(rows, pal, 11, 0.02)


def main():
    skin = make_skin()
    skin_path = save(skin, "entity/sebastian_hili.png")
    skin.save(os.path.join(ROOT, "SebastianHili_skin.png"))
    os.makedirs(os.path.join(ROOT, "tools/preview"), exist_ok=True)
    skin_preview(skin).save(os.path.join(ROOT, "tools/preview/skin_preview.png"))

    save(ore(False), "block/zinc_ore_overlay.png")
    save(ore(True), "block/deepslate_zinc_ore_overlay.png")
    save(raw_zinc_block(), "block/raw_zinc_block.png")
    save(zinc_block(), "block/zinc_block.png")
    save(item_from_grid(RAW_ZINC, ITEM_PAL, 3), "item/raw_zinc.png")
    save(item_from_grid(ZINC_INGOT, ITEM_PAL, 4), "item/zinc_ingot.png")
    save(penny(), "item/penny.png")

    # contact sheet of all 16x16 art for review
    names = ["block/zinc_ore_overlay", "block/deepslate_zinc_ore_overlay", "block/raw_zinc_block", "block/zinc_block",
             "item/raw_zinc", "item/zinc_ingot", "item/penny"]
    sheet = Image.new("RGBA", (16 * len(names) + 2 * (len(names) + 1), 20), (60, 60, 70, 255))
    for i, n in enumerate(names):
        sheet.alpha_composite(Image.open(os.path.join(ASSETS, n + ".png")), (2 + i * 18, 2))
    sheet.resize((sheet.width * 10, sheet.height * 10), Image.NEAREST).save(
        os.path.join(ROOT, "tools/preview/textures_preview.png"))
    print("wrote", skin_path)


if __name__ == "__main__":
    main()
