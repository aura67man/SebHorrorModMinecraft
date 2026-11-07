#!/usr/bin/env python3
"""Draws the 128x128 'burned penny' map art (Exhibit A). The mod converts it to map colours at runtime."""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/sebastianhilihorror/map_art/burned_penny.png")
rng = random.Random(1)
W = 128
img = Image.new("RGB", (W, W))
px = img.load()

# ash and embers background, darker at the edges
for y in range(W):
    for x in range(W):
        d = math.hypot(x - 64, y - 70) / 90
        base = 46 - 30 * d + rng.uniform(-8, 8)
        px[x, y] = (int(max(8, base + 22)), int(max(4, base * 0.35)), int(max(3, base * 0.25)))
for _ in range(140):  # embers
    x, y = rng.randrange(W), rng.randrange(W)
    c = rng.choice([(255, 140, 30), (255, 200, 60), (210, 60, 20)])
    px[x, y] = c

cx, cy, R = 64, 60, 42
coin = Image.new("RGBA", (W, W), (0, 0, 0, 0))
cp = coin.load()
for y in range(W):
    for x in range(W):
        dx, dy = x - cx, y - cy
        r = math.hypot(dx, dy)
        # melted, sagging bottom-right edge
        sag = 0
        ang = math.atan2(dy, dx)
        if 0.1 < ang < 2.2:
            sag = 6 * math.sin((ang - 0.1) / 2.1 * math.pi)
        if r > R + sag:
            continue
        shade = 1.0 - 0.35 * (dx + dy) / (2 * R)          # light from top-left
        burn = max(0.0, 1.5 * (dx * 0.8 + dy * 0.6) / R + 0.25 + rng.uniform(-0.2, 0.2))  # charring from bottom-right
        cr, cg, cb = 196 * shade, 108 * shade, 58 * shade
        if r > R - 4:  # rim
            cr, cg, cb = cr * 0.75, cg * 0.7, cb * 0.65
        t = min(1.0, burn * 1.1)
        cr = cr * (1 - t) + 30 * t
        cg = cg * (1 - t) + 18 * t
        cb = cb * (1 - t) + 14 * t
        cp[x, y] = (int(cr), int(cg), int(cb), 255)

d = ImageDraw.Draw(coin)
# embossed "1c" (a cent sign drawn by hand: a C with a bar)
for off, col in ((1, (90, 45, 25, 255)), (0, (236, 160, 96, 255))):
    d.text((44 + off, 38 + off), "1", fill=col)
    d.arc((52 + off, 40 + off, 64 + off, 52 + off), 40, 320, fill=col, width=2)
    d.line((58 + off, 37 + off, 58 + off, 55 + off), fill=col, width=1)
d.text((38, 72), "ONE  CENT", fill=(110, 58, 30, 255))
d.text((44, 24), "LIBERTY", fill=(120, 62, 34, 255))
# blisters and holes in the charred part
for _ in range(26):
    a = rng.uniform(-0.2, 1.9)
    rr = rng.uniform(R * 0.35, R * 0.95)
    x, y = cx + math.cos(a) * rr, cy + math.sin(a) * rr
    s = rng.uniform(1.5, 4)
    d.ellipse((x - s, y - s, x + s, y + s), fill=(14, 9, 8, 255) if rng.random() < 0.6 else (64, 30, 20, 255))
img.paste(coin, (0, 0), coin)

d = ImageDraw.Draw(img)
# glowing cracks across the charred half
for i in range(9):
    a = rng.uniform(-0.3, 1.8)
    x, y = cx + math.cos(a) * R * 0.15, cy + math.sin(a) * R * 0.15
    for step in range(rng.randint(12, 30)):
        a += rng.uniform(-0.5, 0.5)
        x += math.cos(a) * 1.4
        y += math.sin(a) * 1.4
        if math.hypot(x - cx, y - cy) > R - 2:
            break
        d.point((x, y), fill=(255, 120 + rng.randint(0, 80), 30))
# molten drips running down from the sagging edge, glowing at the tips
for i in range(7):
    x = 66 + i * 6 + rng.randint(-2, 2)
    top = int(cy + math.sqrt(max(0, R * R - (x - cx) ** 2))) - 2
    length = rng.randint(10, 34)
    for y in range(top, min(W - 1, top + length)):
        k = (y - top) / length
        col = (int(150 + 105 * k), int(70 + 120 * k * k), int(30 + 40 * k))
        w = 2 if k < 0.7 else 1
        d.line((x - w // 2, y, x + w // 2, y), fill=col)
# flames licking up from below
for i in range(16):
    x = rng.randint(30, 120)
    h = rng.randint(10, 30)
    for y in range(W - h, W):
        k = (y - (W - h)) / h
        col = (255, int(220 - 160 * k), int(60 - 50 * k))
        wdt = int(1 + 4 * k)
        d.line((x - wdt, y, x + wdt, y), fill=col)
# smoke wisps
smoke = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(smoke)
for i in range(5):
    x = 70 + i * 9
    for y in range(4, 34):
        sd.point((x + int(4 * math.sin(y / 4 + i)), y), fill=(120, 110, 108, 120))
smoke = smoke.filter(ImageFilter.GaussianBlur(1))
img.paste(smoke, (0, 0), smoke)
# caption
d.text((6, 3), "EXHIBIT A", fill=(232, 220, 210))
d.text((6, 14), "you did this.", fill=(220, 40, 30))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
img.resize((512, 512), Image.NEAREST).save(os.path.join(ROOT, "tools/preview/burned_penny_map.png"))
print("wrote", OUT)
