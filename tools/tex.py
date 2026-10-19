"""16x16 텍스처와 인벤토리 도트를 코드로 그린다."""
import random
from PIL import Image

def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def shade(c, f):
    """f>1 밝게, f<1 어둡게"""
    if f >= 1:
        return tuple(min(255, int(v + (255 - v) * (f - 1))) for v in c[:3]) + (255,)
    return tuple(max(0, int(v * f)) for v in c[:3]) + (255,)

def noisy(base, seed, amp=0.06, size=16):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            im.putpixel((x, y), shade(base, 1 + rnd.uniform(-amp, amp)))
    return im

# ---------------------------------------------------------------- 모델 텍스처
LEATHER = hexc('#8a5631')
LEATHER_DARK = hexc('#4f301b')
BRASS = hexc('#c9a03e')
IRON = hexc('#4b4d52')
CANVAS_LANTERN = hexc('#3a3a3a')

def edge_shade(im, base, f=0.86):
    for i in range(16):
        for (x, y) in ((i, 15), (15, i)):
            im.putpixel((x, y), shade(im.getpixel((x, y)), f))
        for (x, y) in ((i, 0), (0, i)):
            im.putpixel((x, y), shade(im.getpixel((x, y)), 1.06))
    return im

def leather():
    im = noisy(LEATHER, 1, 0.03)
    # 부드러운 세로 결 (가죽 패널)
    for y in range(16):
        for x in (4, 11):
            im.putpixel((x, y), shade(LEATHER, 0.94))
    # 가장자리 바느질선 (밝은 점선)
    stitch = shade(LEATHER, 1.16)
    for i in range(1, 15, 3):
        im.putpixel((i, 1), stitch); im.putpixel((i, 14), stitch)
    # 주름
    for x in range(3, 13):
        if x % 5 == 0:
            im.putpixel((x, 7), shade(LEATHER, 0.92))
    return edge_shade(im, LEATHER)

def leather_dark():
    im = noisy(LEATHER_DARK, 2, 0.03)
    edge = shade(LEATHER_DARK, 0.7)
    for i in range(16):
        im.putpixel((i, 0), edge); im.putpixel((i, 15), edge)
        im.putpixel((0, i), edge); im.putpixel((15, i), edge)
    hole = shade(LEATHER_DARK, 0.45)
    for y in range(3, 16, 4):  # 끈 구멍
        im.putpixel((7, y), hole); im.putpixel((8, y), hole)
    return im

def brass():
    im = noisy(BRASS, 3, 0.04)
    for i in range(16):
        im.putpixel((i, 0), shade(BRASS, 1.4)); im.putpixel((0, i), shade(BRASS, 1.3))
        im.putpixel((i, 15), shade(BRASS, 0.6)); im.putpixel((15, i), shade(BRASS, 0.65))
    return im

def iron():
    im = noisy(IRON, 4, 0.05)
    for y in range(16):
        im.putpixel((0, y), shade(IRON, 1.35)); im.putpixel((15, y), shade(IRON, 0.7))
    return im

def leather_base():
    im = noisy(shade(LEATHER_DARK, 1.1), 5, 0.03)
    for x in range(0, 16, 3):
        im.putpixel((x, 3), shade(LEATHER_DARK, 1.5))  # 징 박음
    return im

# 염료 색 (바닐라 양털 기준 근사)
WOOL = {
    'white': '#e9ecec', 'orange': '#f07613', 'magenta': '#bd44b3', 'light_blue': '#3aafd9',
    'yellow': '#f8c627', 'lime': '#70b919', 'pink': '#ed8dac', 'gray': '#3e4447',
    'light_gray': '#8e8e86', 'cyan': '#158991', 'purple': '#792aac', 'blue': '#35399d',
    'brown': '#724728', 'green': '#546d1b', 'red': '#a12722', 'black': '#141519',
}

def roll_side(color):
    """말린 침낭 옆면: 감긴 겹이 보이도록 가로줄."""
    c = hexc(color)
    im = noisy(c, hash(color) & 0xffff, 0.05)
    for x in range(16):
        im.putpixel((x, 0), shade(c, 1.25))
        im.putpixel((x, 5), shade(c, 0.82))
        im.putpixel((x, 10), shade(c, 0.82))
        im.putpixel((x, 15), shade(c, 0.6))
    return im

def roll_end(color):
    """말린 침낭 끝면: 소용돌이."""
    c = hexc(color)
    im = Image.new('RGBA', (16, 16), shade(c, 0.95))
    import math
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            spiral = (r - a / math.pi * 1.6) % 3.2
            if spiral < 1.0:
                im.putpixel((x, y), shade(c, 0.62))
            elif r < 1.5:
                im.putpixel((x, y), shade(c, 0.5))
            else:
                im.putpixel((x, y), shade(c, 1.05 + (7.5 - r) * 0.02))
    return im

def lantern_preview():
    """미리보기 전용 (실제 모드는 바닐라 랜턴 텍스처를 쓴다)."""
    im = Image.new('RGBA', (16, 16), CANVAS_LANTERN)
    glow = hexc('#ffd27a')
    for y in range(4, 13):
        for x in range(3, 13):
            im.putpixel((x, y), shade(glow, 1 - abs(x - 7.5) * 0.04))
    for x in range(16):
        im.putpixel((x, 0), shade(CANVAS_LANTERN, 1.3)); im.putpixel((x, 15), shade(CANVAS_LANTERN, 0.7))
    return im

# ---------------------------------------------------------------- 인벤토리 도트

def roll_icon(color):
    """말아 놓은 침낭 아이콘 (가로 롤, 왼쪽 끝 소용돌이, 가죽끈 두 줄)."""
    import math
    c = hexc(color)
    out = shade(c, 0.38)
    strap, strap_d, buckle = hexc('#7a4a26'), hexc('#4f301b'), hexc('#e0b84c')
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    top, bot = 4, 11               # 몸통 세로 범위
    body_shade = [1.3, 1.18, 1.05, 1.0, 0.92, 0.84, 0.74, 0.64]
    # 몸통
    for y in range(top, bot + 1):
        for x in range(5, 15):
            im.putpixel((x, y), shade(c, body_shade[y - top]))
    for x in range(5, 15):
        im.putpixel((x, top - 1), out); im.putpixel((x, bot + 1), out)
    for y in range(top, bot + 1):
        im.putpixel((15, y), out)
    im.putpixel((14, top), out); im.putpixel((14, bot), shade(c, 0.5))  # 오른쪽 둥근 모서리
    # 끝면 (타원)
    cx, cy, rx, ry = 4.0, 7.5, 2.6, 4.6
    for y in range(top - 1, bot + 2):
        for x in range(0, 8):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1.0:
                a = math.atan2(dy, dx)
                r = math.sqrt(d)
                spiral = (r * 3.2 - a / math.pi) % 1.6
                col = shade(c, 0.6) if spiral < 0.55 else shade(c, 1.15 - r * 0.25)
                if r < 0.25:
                    col = shade(c, 0.45)
                im.putpixel((x, y), col)
            elif d <= 1.45 and im.getpixel((x, y))[3] == 0:
                im.putpixel((x, y), out)
    # 끈 두 줄 + 버클
    for sx in (8, 12):
        for y in range(top - 1, bot + 2):
            im.putpixel((sx, y), strap)
            im.putpixel((sx + 1, y), strap_d)
        im.putpixel((sx, top + 3), buckle)
        im.putpixel((sx + 1, top + 3), shade(buckle, 0.7))
    return im


# ---------------------------------------------------------------- 설치된 침낭
LINING = hexc('#e6d9b8')
PILLOW = hexc('#dcd6c8')

def bag_top(color):
    """누비 겉감: 가로 박음질 줄, 지퍼선."""
    c = hexc(color)
    im = noisy(c, (hash(color) >> 4) & 0xffff, 0.03)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3:
                im.putpixel((x, y), shade(c, 0.8))      # 박음질 골
            elif y % 4 == 0:
                im.putpixel((x, y), shade(c, 1.12))     # 누빔 볼록한 부분
    for y in range(16):
        im.putpixel((14, y), shade(c, 0.62))           # 지퍼
        if y % 2 == 0:
            im.putpixel((14, y), hexc('#b8b8b8'))
    return im

def bag_side(color):
    c = hexc(color)
    im = noisy(shade(c, 0.85), 77, 0.03)
    for x in range(16):
        im.putpixel((x, 15), shade(c, 0.55))
    return im

def lining():
    im = noisy(LINING, 31, 0.025)
    for y in range(2, 16, 5):
        for x in range(16):
            im.putpixel((x, y), shade(LINING, 0.92))
    return im



def _blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


# ---------------------------------------------------------------- 부착물 아이템 아이콘

def icon_snack_pouch():
    """작은 가죽 주머니, 덮개와 놋쇠 단추."""
    im = _blank(); L, Ld = LEATHER, LEATHER_DARK
    out = shade(Ld, 0.5)
    for y in range(6, 14):
        for x in range(4, 12):
            if y == 13 and x in (4, 11):
                continue
            im.putpixel((x, y), shade(L, 1.05 - (y - 6) * 0.04 - (x - 4) * 0.015))
    for y in range(4, 8):                      # 덮개
        for x in range(4, 12):
            if y == 4 and x in (4, 11):
                continue
            im.putpixel((x, y), shade(Ld, 1.2 if y == 4 else 1.0))
    im.putpixel((7, 8), BRASS); im.putpixel((8, 8), shade(BRASS, 0.7))
    for y in range(5, 14):
        im.putpixel((3, y), out); im.putpixel((12, y), out)
    for x in range(5, 11):
        im.putpixel((x, 14), out); im.putpixel((x, 3), out)
    im.putpixel((4, 14), out) if False else None
    for x in (6, 9):                            # 벨트 고리
        im.putpixel((x, 2), Ld)
    for x in range(6, 10):
        im.putpixel((x, 1), Ld)
    return im

def icon_tool_hook():
    """도구걸이: 가죽 끈 고리에 달린 철 갈고리."""
    im = _blank(); I, Ld = IRON, LEATHER_DARK
    for x in range(5, 11):                      # 위 가죽 고리
        im.putpixel((x, 1), shade(Ld, 1.15)); im.putpixel((x, 4), Ld)
    for y in range(1, 5):
        im.putpixel((5, y), Ld); im.putpixel((10, y), shade(Ld, 0.8))
    im.putpixel((7, 4), BRASS); im.putpixel((8, 4), shade(BRASS, 0.7))
    for y in range(5, 11):                      # 철 기둥
        im.putpixel((8, y), shade(I, 1.25)); im.putpixel((9, y), shade(I, 0.85))
    for x in range(5, 9):                       # 아래 갈고리
        im.putpixel((x, 12), shade(I, 1.1)); im.putpixel((x, 13), shade(I, 0.75))
    im.putpixel((8, 11), shade(I, 1.0)); im.putpixel((9, 11), shade(I, 0.8))
    im.putpixel((4, 10), shade(I, 1.2)); im.putpixel((4, 11), shade(I, 1.1)); im.putpixel((5, 11), shade(I, 0.9))
    return im

def icon_bedroll_strap():
    """둥글게 감은 가죽 끈 두 가닥과 놋쇠 버클."""
    import math
    im = _blank(); Ld = LEATHER_DARK; L2 = hexc('#7a4a26')
    for cx, cy, col in ((6.5, 8.5, Ld), (9.5, 7.5, L2)):
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - cx, y - cy)
                if 3.6 <= r <= 4.9:
                    im.putpixel((x, y), shade(col, 1.15 if y < cy else 0.85))
    for (x, y) in ((12, 9), (13, 9), (12, 10), (13, 10)):
        im.putpixel((x, y), BRASS)
    im.putpixel((13, 10), shade(BRASS, 0.7)); im.putpixel((12, 9), shade(BRASS, 1.3))
    return im

def icon_armor_pouch():
    """크고 납작한 갑옷주머니, 넓은 덮개와 버클."""
    im = _blank(); L, Ld = LEATHER, LEATHER_DARK
    out = shade(Ld, 0.5)
    for y in range(4, 14):
        for x in range(2, 14):
            im.putpixel((x, y), shade(L, 0.98 - (y - 4) * 0.025 - (x - 2) * 0.012))
    for y in range(3, 8):
        for x in range(2, 14):
            im.putpixel((x, y), shade(Ld, 1.2 if y == 3 else 1.0))
    for y in range(7, 10):
        im.putpixel((7, y), shade(Ld, 0.85)); im.putpixel((8, y), shade(Ld, 0.7))
    im.putpixel((7, 9), BRASS); im.putpixel((8, 9), shade(BRASS, 0.7))
    for x in range(2, 14):
        im.putpixel((x, 14), out); im.putpixel((x, 2), out)
    for y in range(3, 14):
        im.putpixel((1, y), out); im.putpixel((14, y), out)
    for x in (4, 11):                           # 스티치
        for y in range(9, 13, 2):
            im.putpixel((x, y), shade(L, 1.2))
    return im
