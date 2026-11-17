"""Paints the scar overlays Blood Feud draws on Brutes (client only). Run: python3 make-scars.py <assets dir>.

Each overlay matches a vanilla mob texture layout; everything but the scar is transparent. Two variants per scar
(a and b, mirrored) so Brutes with the same scar don't all look the same.
"""
import os, random, sys
from PIL import Image

# Front faces (x, y, w, h) of the parts we paint, per texture layout.
LAYOUTS = {
    'humanoid': ((64, 64), dict(face=(8, 8, 8, 8), body=(20, 20, 8, 12), arm=(44, 20, 4, 12), leg=(4, 20, 4, 12),
                                leg2=(20, 52, 4, 12), arm2=(36, 52, 4, 12))),
    'skeleton': ((64, 32), dict(face=(8, 8, 8, 8), body=(20, 20, 8, 12), arm=(42, 18, 2, 12), leg=(2, 18, 2, 12))),
    'piglin': ((64, 64), dict(face=(9, 8, 8, 8), body=(20, 20, 8, 12), arm=(44, 20, 4, 12), leg=(4, 20, 4, 12),
                              leg2=(20, 52, 4, 12), arm2=(36, 52, 4, 12))),
    'villager': ((64, 64), dict(face=(8, 8, 8, 10), body=(22, 26, 8, 12), leg=(4, 26, 4, 12))),
    'witch': ((64, 128), dict(face=(8, 8, 8, 10), body=(22, 26, 8, 12), leg=(4, 26, 4, 12))),
    'spider': ((64, 32), dict(face=(40, 12, 8, 8), body=(12, 12, 10, 12))),
}


class Painter:
    def __init__(self, img, parts, flip, rnd):
        self.img, self.parts, self.flip, self.rnd = img, parts, flip, rnd

    def px(self, part, x, y, c):
        if part not in self.parts:
            return
        x0, y0, w, h = self.parts[part]
        if not (0 <= x < w and 0 <= y < h):
            return
        if self.flip:
            x = w - 1 - x
        self.img.putpixel((x0 + x, y0 + y), c + (255,))

    def size(self, part):
        return self.parts[part][2:] if part in self.parts else (0, 0)

    def blob(self, part, cx, cy, r, colors, density):
        w, h = self.size(part)
        for y in range(h):
            for x in range(w):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= r and self.rnd.random() < density * (1.15 - d / (r + 0.01) * 0.5):
                    self.px(part, x, y, self.rnd.choice(colors))

    def speckle(self, part, density, colors, rows=None):
        w, h = self.size(part)
        for y in range(h):
            if rows and not (rows[0] <= y < rows[1]):
                continue
            for x in range(w):
                if self.rnd.random() < density:
                    self.px(part, x, y, self.rnd.choice(colors))


CHAR = [(34, 22, 16), (52, 30, 18), (24, 16, 12), (70, 38, 20)]
EMBER = [(150, 60, 20), (190, 90, 30)]
BLOOD = (122, 18, 18)
DARK_BLOOD = (70, 8, 8)
STITCH = (205, 190, 160)
SOOT = [(30, 30, 30), (55, 55, 55), (85, 85, 85), (20, 20, 20), (150, 148, 144), (190, 186, 180)]
ALGAE = [(58, 112, 98), (40, 88, 80), (74, 130, 110), (90, 140, 120)]
BANDAGE = (222, 214, 192)
BANDAGE_LINE = (170, 160, 140)


def burned(p):
    w, h = p.size('face')
    p.blob('face', w * 0.7, h * 0.45, w * 0.45, CHAR, 0.9)
    p.blob('face', w * 0.75, h * 0.4, w * 0.18, EMBER, 0.4)
    bw, bh = p.size('body')
    p.blob('body', bw * 0.3, bh * 0.25, bw * 0.45, CHAR, 0.85)
    for part in ('arm', 'arm2'):
        aw, ah = p.size(part)
        p.blob(part, aw / 2, ah * 0.3, aw * 0.9, CHAR, 0.7)


def cut(p):
    w, h = p.size('face')
    for i in range(min(w, h) - 1):
        x, y = 1 + i, i + (1 if h > w else 0)
        p.px('face', x, y, BLOOD)
        if i % 2 == 1:
            p.px('face', x + 1, y - 1, STITCH)
            p.px('face', x - 1, y + 1, STITCH)
    bw, bh = p.size('body')
    x = bw // 2 + 1
    for y in range(2, bh - 2):
        p.px('body', x, y, BLOOD if y % 3 else DARK_BLOOD)
        if y % 2 == 0:
            p.px('body', x - 1, y, STITCH)
            p.px('body', x + 1, y, STITCH)


def hole(p, part, x, y):
    p.px(part, x, y, (24, 8, 8))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if p.rnd.random() < 0.7:
            p.px(part, x + dx, y + dy, BLOOD)


def pierced(p):
    w, h = p.size('face')
    hole(p, 'face', w - 3, h // 2)
    bw, bh = p.size('body')
    for x, y in ((2, 3), (bw - 3, 5), (bw // 2, bh - 4)):
        hole(p, 'body', x, y)
    for part in ('arm', 'leg'):
        aw, ah = p.size(part)
        if aw:
            hole(p, part, aw // 2, ah // 3)


def mauled(p):
    bw, bh = p.size('body')
    for k in range(3):
        for i in range(min(bw, bh) - 2):
            x, y = k * 2 + i // 2, 1 + i
            p.px('body', x, y, BLOOD if i % 3 else DARK_BLOOD)
    w, h = p.size('face')
    for k in range(3):
        for i in range(3):
            p.px('face', w - 4 + k, h - 4 + i, BLOOD)
    for part in ('leg', 'arm'):
        aw, ah = p.size(part)
        for i in range(ah // 2):
            p.px(part, i % max(aw, 1), ah // 3 + i, BLOOD)


def blasted(p):
    w, h = p.size('face')
    p.speckle('face', 0.45, SOOT, rows=(h // 2, h))
    p.speckle('body', 0.4, SOOT)
    for part in ('arm', 'arm2', 'leg'):
        p.speckle(part, 0.35, SOOT)


def drowned(p):
    for part, d in (('face', 0.35), ('body', 0.35), ('leg', 0.45), ('leg2', 0.45), ('arm', 0.3), ('arm2', 0.3)):
        p.speckle(part, d, ALGAE)


def broken(p):
    w, h = p.size('face')
    for x in range(w):
        p.px('face', x, 1, BANDAGE)
        p.px('face', x, 2, BANDAGE if x % 3 else BANDAGE_LINE)
    p.px('face', w - 3, 3, BLOOD)
    for part in ('leg', 'leg2'):
        lw, lh = p.size(part)
        for y in range(lh // 3, lh - 2):
            for x in range(lw):
                p.px(part, x, y, BANDAGE_LINE if (x + y) % 4 == 0 else BANDAGE)


SCARS = dict(burned=burned, cut=cut, pierced=pierced, mauled=mauled, blasted=blasted, drowned=drowned, broken=broken)

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for layout, (size, parts) in LAYOUTS.items():
    for scar, paint in SCARS.items():
        for variant, flip in (('a', False), ('b', True)):
            img = Image.new('RGBA', size, (0, 0, 0, 0))
            paint(Painter(img, parts, flip, random.Random(f'{layout}/{scar}/{variant}')))
            img.save(os.path.join(out, f'{layout}_{scar}_{variant}.png'), optimize=True)
print('ok')
