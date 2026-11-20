from PIL import Image, ImageDraw, ImageFont
import os
C = dict(
    ink=(20,10,12), night=(38,18,22), blood=(178,18,31), deep=(110,10,20), ember=(232,64,52),
    bone=(232,220,196), boneShade=(176,162,138), gold=(217,164,65), king=(162,75,216), black=(8,4,5), steel=(190,196,204), steelDark=(110,116,126))
SKULL = """
................
....bbbbbbbb....
...bbbbbbbbbb...
..bbbbbbbrbbbs..
..bbbbbbrbbbbs..
..bbbbbrbbbbbs..
..bbkkkbbkkkbs..
..bbkekbbkekbs..
..bbkkkbbkkkbs..
..bbbbbkkbbbbs..
...bbbbkkbbbs...
....bbbbbbbs....
....bkbkbkbs....
....bbbbbbbs....
.....ssssss.....
................
"""
MAP = {'b':'bone','s':'boneShade','k':'black','e':'ember','r':'blood'}
def sprite(img, rows, x0, y0, px):
    d = ImageDraw.Draw(img)
    for y,row in enumerate(rows):
        for x,ch in enumerate(row):
            if ch in MAP:
                d.rectangle([x0+x*px, y0+y*px, x0+(x+1)*px-1, y0+(y+1)*px-1], fill=C[MAP[ch]])
skull = [r for r in SKULL.strip('\n').split('\n')]

# 5x7 pixel font, drawn for this kit
F = {
'A':["01110","10001","10001","11111","10001","10001","10001"],
'B':["11110","10001","10001","11110","10001","10001","11110"],
'C':["01111","10000","10000","10000","10000","10000","01111"],
'D':["11110","10001","10001","10001","10001","10001","11110"],
'E':["11111","10000","10000","11110","10000","10000","11111"],
'F':["11111","10000","10000","11110","10000","10000","10000"],
'H':["10001","10001","10001","11111","10001","10001","10001"],
'I':["11111","00100","00100","00100","00100","00100","11111"],
'K':["10001","10010","10100","11000","10100","10010","10001"],
'L':["10000","10000","10000","10000","10000","10000","11111"],
'M':["10001","11011","10101","10101","10001","10001","10001"],
'N':["10001","11001","10101","10011","10001","10001","10001"],
'O':["01110","10001","10001","10001","10001","10001","01110"],
'P':["11110","10001","10001","11110","10000","10000","10000"],
'R':["11110","10001","10001","11110","10100","10010","10001"],
'S':["01111","10000","10000","01110","00001","00001","11110"],
'T':["11111","00100","00100","00100","00100","00100","00100"],
'U':["10001","10001","10001","10001","10001","10001","01110"],
'Y':["10001","10001","01010","00100","00100","00100","00100"],
'W':["10001","10001","10001","10101","10101","11011","10001"],
'G':["01111","10000","10000","10011","10001","10001","01111"],
'.':["00000","00000","00000","00000","00000","00000","00100"],
'V':["10001","10001","10001","10001","01010","01010","00100"],
'J':["00111","00010","00010","00010","00010","10010","01100"],
'X':["10001","10001","01010","00100","01010","10001","10001"],
'Z':["11111","00001","00010","00100","01000","10000","11111"],
'Q':["01110","10001","10001","10001","10101","10010","01101"],
',':["000","000","000","000","000","010","100"],
'&':["01100","10010","10100","01000","10101","10010","01101"],
' ':["00000"]*7,
}
def text_w(s, px): return sum((len(F[c][0])+1)*px for c in s) - px
def text(img, s, x, y, px, fill, shadow=None, drip=False):
    d = ImageDraw.Draw(img)
    cx = x
    for c in s:
        g = F[c]
        for gy,row in enumerate(g):
            for gx,v in enumerate(row):
                if v=='1':
                    if shadow: d.rectangle([cx+gx*px+px//2*2, y+gy*px+px//2*2, cx+(gx+1)*px-1+px//2*2, y+(gy+1)*px-1+px//2*2], fill=shadow)
        for gy,row in enumerate(g):
            for gx,v in enumerate(row):
                if v=='1':
                    d.rectangle([cx+gx*px, y+gy*px, cx+(gx+1)*px-1, y+(gy+1)*px-1], fill=fill)
        if drip and c not in ' ':
            # a drip under some bottom pixels, deterministic per letter
            bottom = g[-1]
            for gx,v in enumerate(bottom):
                if v=='1' and (ord(c)+gx) % 3 == 0:
                    L = 1 + (ord(c)*7+gx) % 3
                    d.rectangle([cx+gx*px, y+7*px, cx+(gx+1)*px-1, y+(7+L)*px-1], fill=fill)
                    d.rectangle([cx+gx*px+px//4, y+(7+L)*px, cx+(gx+1)*px-1-px//4, y+(7+L)*px+px//2], fill=fill)
        cx += (len(g[0])+1)*px
    return cx

def banner(img, x, y, w, h, color, px):
    d = ImageDraw.Draw(img)
    # pole
    d.rectangle([x-px, y-px*2, x+w+px-1, y-1], fill=C['steelDark'])
    d.rectangle([x, y, x+w-1, y+h-1], fill=color)
    # notched swallowtail bottom
    n = w//(2*px)
    for i in range(n):
        dy = (n - abs(i - n//2)*1) if False else None
    tri = [(x, y+h), (x+w//2, y+h-w//3), (x+w, y+h), (x+w, y+h+w//3), (x+w//2, y+h), (x, y+h+w//3)]
    d.polygon([(x, y+h-1), (x+w-1, y+h-1), (x+w-1, y+h+w//3), (x+w//2, y+h+w//10), (x, y+h+w//3)], fill=color)
    # border stripe
    d.rectangle([x+px, y+px, x+w-1-px, y+2*px-1], fill=C['deep'])

def blades(img, cx, cy, px):
    d = ImageDraw.Draw(img)
    for sgn in (1,-1):
        for i in range(-13, 14):
            xx = cx + sgn*i*px; yy = cy + i*px
            col = C['steel'] if i < 8 else (C['gold'] if i < 10 else C['deep'])
            if i == 8:
                for k in (-2,-1,1,2):
                    d.rectangle([xx+sgn*k*px, yy-k*px, xx+sgn*k*px+px-1, yy-k*px+px-1], fill=C['gold'])
            d.rectangle([xx, yy, xx+px-1, yy+px-1], fill=col)
            if i < 8: d.rectangle([xx+sgn*px if sgn>0 else xx-px, yy, (xx+sgn*px if sgn>0 else xx-px)+px-1, yy+px-1], fill=C['steelDark'])

def icon(size):
    px = size//32
    img = Image.new('RGB', (size,size), C['ink'])
    d = ImageDraw.Draw(img)
    # vignette blocks
    for i in range(0, size, px*4):
        for j in range(0, size, px*4):
            if (i*7+j*3)//(px*4) % 5 == 0: d.rectangle([i,j,i+px*4-1,j+px*4-1], fill=C['night'])
    banner(img, 8*px, 5*px, 16*px, 15*px, C['blood'], px)
    blades(img, 16*px, 15*px, px)
    sprite(img, skull, 8*px, 7*px, px)
    return img

os.makedirs('out', exist_ok=True)
icon(512).save('out/bloodfeud-icon-512.png')
icon(128).save('out/icon.png')

def wordmark(px, pad):
    w = text_w("BLOOD FEUD", px) + pad*2
    img = Image.new('RGBA', (w, 13*px + pad*2), (0,0,0,0))
    text(img, "BLOOD FEUD", pad, pad, px, C['blood']+(255,), shadow=C['black']+(255,), drip=True)
    return img
wm = wordmark(24, 24); wm.save('out/bloodfeud-wordmark.png')

def hero(W, H, tagline, sub):
    img = Image.new('RGB', (W,H), C['ink'])
    d = ImageDraw.Draw(img)
    px = H//60
    for i in range(0, W, px*6):
        for j in range(0, H, px*6):
            if ((i//(px*6))*7 + (j//(px*6))*13) % 6 == 0: d.rectangle([i,j,i+px*6-1,j+px*6-1], fill=C['night'])
    # ground: dirt blocks with a blood-red top
    g = H - px*6
    for i in range(0, W, px*3):
        d.rectangle([i, g, i+px*3-1, H], fill=(46,26,22) if (i//(px*3)) % 2 else (56,32,26))
        d.rectangle([i, g, i+px*3-1, g+px-1], fill=C['deep'] if (i//(px*3)) % 3 else C['blood'])
    isz = int(H*0.70)
    ix = W - isz - int(H*0.55)
    iy = g - isz - px*2
    bw, bh = int(H*0.16), int(H*0.42)
    banner(img, ix - bw - px*6, iy + px*10, bw, bh, C['gold'], px)
    banner(img, ix + isz + px*6, iy + px*10, bw, bh, C['king'], px)
    ic = icon(isz)
    img.paste(ic, (ix, iy))
    lx = int(H*0.14)
    tp = max(4, int(H/38))
    sp = max(2, int(H/95))
    fsz = max(12, H//20)
    block = 13*tp + int(H*0.07) + 7*sp + int(H*0.06) + fsz
    ty = (g - block)//2
    text(img, "BLOOD FEUD", lx, ty, tp, C['blood'], shadow=C['black'], drip=True)
    y2 = ty + 13*tp + int(H*0.07)
    text(img, tagline, lx, y2, sp, C['bone'])
    f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', fsz)
    d.text((lx, y2 + 7*sp + int(H*0.06)), sub, font=f, fill=C['boneShade'])
    return img
hero(1920, 480, "MOBS THAT REMEMBER YOU.", "NeoForge + Fabric  ·  Minecraft 1.21.1  ·  Server only").save('out/bloodfeud-banner-1920x480.png')
def social(W, H):
    img = Image.new('RGB', (W,H), C['ink'])
    d = ImageDraw.Draw(img)
    px = H//60
    for i in range(0, W, px*6):
        for j in range(0, H, px*6):
            if ((i//(px*6))*7 + (j//(px*6))*13) % 6 == 0: d.rectangle([i,j,i+px*6-1,j+px*6-1], fill=C['night'])
    isz = int(H*0.44)
    ix, iy = (W-isz)//2, int(H*0.05)
    bw, bh = int(H*0.11), int(H*0.26)
    banner(img, ix - bw - px*4, iy + px*8, bw, bh, C['gold'], px)
    banner(img, ix + isz + px*4, iy + px*8, bw, bh, C['king'], px)
    img.paste(icon(isz), (ix, iy))
    tp = 12
    tw = text_w("BLOOD FEUD", tp)
    ty = iy + isz + int(H*0.04)
    text(img, "BLOOD FEUD", (W-tw)//2, ty, tp, C['blood'], shadow=C['black'], drip=True)
    sp = 5
    tag = "MOBS THAT REMEMBER YOU."
    text(img, tag, (W-text_w(tag, sp))//2, ty + 13*tp + 14, sp, C['bone'])
    f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 24)
    sub = "NeoForge + Fabric  ·  Minecraft 1.21.1  ·  Server only"
    tw2 = d.textlength(sub, font=f)
    d.text(((W-tw2)//2, ty + 13*tp + 14 + 7*sp + 22), sub, font=f, fill=C['boneShade'])
    return img
social(1280, 640).save('out/bloodfeud-social-1280x640.png')

# palette card
pal = [('Blood','blood'),('Deep','deep'),('Ember','ember'),('Bone','bone'),('Bone shade','boneShade'),('Gold (Boss)','gold'),('Royal (King)','king'),('Ink','ink'),('Night','night')]
img = Image.new('RGB', (len(pal)*160, 220), C['ink']); d = ImageDraw.Draw(img)
f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 16)
for i,(n,k) in enumerate(pal):
    d.rectangle([i*160+10, 10, i*160+150, 150], fill=C[k], outline=C['boneShade'])
    d.text((i*160+12, 160), n, font=f, fill=C['bone'])
    d.text((i*160+12, 185), '#%02X%02X%02X' % C[k], font=f, fill=C['boneShade'])
img.save('out/bloodfeud-palette.png')

# Pirut Lib icon: a gold P made of blocks on ink
def pl(size):
    px = size//16
    img = Image.new('RGB', (size,size), C['ink']); d = ImageDraw.Draw(img)
    P = ["11110","10001","10001","11110","10000","10000","10000"]
    ox, oy = 3*px, 2*px
    for y,row in enumerate(P):
        for x,v in enumerate(row):
            if v=='1':
                d.rectangle([ox+x*2*px, oy+y*2*px-px//2*0, ox+(x+1)*2*px-1, oy+(y+1)*2*px-1], fill=C['gold'])
                d.rectangle([ox+x*2*px, oy+(y+1)*2*px-px//2, ox+(x+1)*2*px-1, oy+(y+1)*2*px-1], fill=(150,108,36))
    return img
pl(512).save('out/pirutlib-icon-512.png'); pl(128).save('out/pirutlib-icon.png')

# Store page art (Modrinth/CurseForge description), in the style of Earth Platform's store page:
# small pixel buttons that sit in one centered row, and ribbon headers for the sections. Transparent backgrounds.
def outlined_text(img, s, x, y, px, fill, outline):
    for dx in (-px, 0, px):
        for dy in (-px, 0, px, 2*px):
            if dx or dy: text(img, s, x+dx, y+dy, px, outline)
    text(img, s, x, y, px, fill)

def button(label, px=2):
    tw = text_w(label, px); W, H = tw + 16*px, 7*px + 10*px
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W-1, H-1], fill=C['black'])
    d.rectangle([px, px, W-1-px, H-1-px], fill=C['blood'])
    d.rectangle([px, px, W-1-px, 2*px-1], fill=C['ember'])
    d.rectangle([px, H-1-2*px, W-1-px, H-1-px], fill=C['deep'])
    outlined_text(img, label, (W-tw)//2, (H-7*px)//2, px, C['bone'], C['black'])
    return img

def header(label, W=1200, H=102, px=4):
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    tw = text_w(label, px); bw = max(tw + 48*px, 560); x0 = (W-bw)//2; y0, y1 = 14, H-14
    for sgn in (-1, 1):  # swallowtail ends behind the ribbon
        ex = x0 if sgn < 0 else x0+bw
        tail = [(ex, y0+8), (ex+sgn*16*px//2*2, y0+8), (ex+sgn*12*px//2*2 - sgn*6*px, (y0+y1)//2+4), (ex+sgn*16*px//2*2, y1+8), (ex, y1+8)]
        d.polygon(tail, fill=C['deep'], outline=C['black'])
    d.rectangle([x0, y0, x0+bw, y1], fill=C['black'])
    d.rectangle([x0+px, y0+px, x0+bw-px, y1-px], fill=C['blood'])
    d.rectangle([x0+px, y0+px, x0+bw-px, y0+2*px], fill=C['ember'])
    d.rectangle([x0+px, y1-2*px, x0+bw-px, y1-px], fill=C['deep'])
    outlined_text(img, label, (W-tw)//2, (y0+y1)//2 - 7*px//2 - px, px, C['bone'], C['black'])
    return img

os.makedirs('out/store', exist_ok=True)
for slug, label in [('how-it-works', 'HOW IT WORKS'), ('commands', 'COMMANDS'), ('report-a-bug', 'REPORT A BUG'), ('source', 'SOURCE CODE')]:
    button(label).save('out/store/button-%s.png' % slug)
for slug, label in [('rivals', 'A RIVAL FOR EVERY FIGHT'), ('packs', 'PACKS, BOSSES & KINGS'), ('ledger', 'THE FEUD LEDGER'),
                    ('relics', 'RELICS'), ('big-worlds', 'BIG WORLDS, SMALL COST'), ('details', 'DETAILS')]:
    header(label).save('out/store/header-%s.png' % slug)
