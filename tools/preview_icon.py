"""입체 아이콘 미리보기: 배낭 아이템이 인벤토리 칸에 그려지는 모습 (BackpackItemRenderer 와 같은 배치).

    python tools/preview_icon.py out.png

도구·랜턴 그림은 바닐라 텍스처가 없어 자리표시용. 위치·크기·각도는 client/BackpackRenderer 의 상수와 같다.

좌표: 착용자 오른쪽 = +x, 앞(주머니) = +z, 위 = +y. 정면을 앞(+z)에서 보면 +x 가 화면 오른쪽.
"""
import math
import sys
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageChops

sys.path.insert(0, os.path.dirname(__file__))
import tex  # noqa: E402
from model import box, BASE, ARMOR, bedroll  # noqa: E402
from render import default_uv, fit_uv, corners, homography  # noqa: E402

SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
NORMAL = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1),
          'east': (1, 0, 0), 'west': (-1, 0, 0)}


# ------------------------------------------------------------------ 자리표시용 도구 그림 (실제 게임에서는 진짜 아이템)
def _sprite(points):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y), c in points:
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), c)
    return im


def pickaxe():
    wood, wood_d = (137, 103, 39, 255), (92, 66, 24, 255)
    iron, iron_l, iron_d, out = (198, 198, 198, 255), (240, 240, 240, 255), (120, 120, 120, 255), (40, 40, 40, 255)
    pts = []
    for i in range(9):                       # 손잡이: 왼쪽 아래 → 가운데
        pts += [((2 + i, 13 - i), wood), ((3 + i, 13 - i), wood_d), ((2 + i, 14 - i), out)]
    arc = [(3, 3), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 3), (12, 4), (12, 5), (13, 6),
           (13, 7), (13, 8), (13, 9), (13, 10), (12, 11)]
    for (x, y) in arc:                       # 머리: 오른쪽 위 모서리를 감싸는 호
        pts += [((x, y - 1), out), ((x, y), iron_l), ((x - 1, y + 1), iron), ((x - 2 if x > 11 else x, y + 2 if x <= 11 else y), iron_d)]
    return _sprite(pts)


def sword():
    blade, blade_l, blade_d = (205, 210, 215, 255), (245, 245, 250, 255), (120, 125, 130, 255)
    wood = (110, 80, 40, 255)
    pts = []
    for i in range(10):                      # 칼날
        x, y = 5 + i, 10 - i
        pts += [((x, y), blade), ((x + 1, y), blade_d), ((x, y - 1), blade_l)]
    for (x, y) in ((3, 9), (4, 10), (5, 11), (6, 12)):   # 코등이
        pts.append(((x, y), (90, 90, 95, 255)))
    pts += [((2, 13), wood), ((3, 12), wood), ((1, 14), (70, 50, 25, 255))]
    return _sprite(pts)


# ------------------------------------------------------------------ 부착물 모델 (오른쪽 기준, 왼쪽은 x 반전)
def mirror(elements):
    out = []
    for e in elements:
        e2 = dict(e)
        if e.get('kind') == 'quad':
            e2['pts'] = [(16 - x, y, z) for (x, y, z) in e['pts']]
        else:
            e2['from'] = [16 - e['to'][0], e['from'][1], e['from'][2]]
            e2['to'] = [16 - e['from'][0], e['to'][1], e['to'][2]]
            e2['faces'] = [{'east': 'west', 'west': 'east'}.get(f, f) for f in e['faces']]
            e2['uv'] = {}
        out.append(e2)
    return out


# 바닐라 랜턴 블록 모델(서 있는 랜턴)을 그대로 옮겨 옆면에 붙인다. (x+8: 블록 중앙 x=8 → 배낭 옆면 x=13 바깥)
_LX, _LY, _LZ = 8.0, 3.0, 0.0
LANTERN = [
    box([5 + _LX, 0 + _LY, 5 + _LZ], [11 + _LX, 7 + _LY, 11 + _LZ], 'lantern',
        uv={f: [0, 2, 6, 9] for f in ('north', 'south', 'east', 'west')} | {'up': [0, 9, 6, 15], 'down': [0, 9, 6, 15]}),
    box([6 + _LX, 7 + _LY, 6 + _LZ], [10 + _LX, 9 + _LY, 10 + _LZ], 'lantern',
        uv={f: [1, 0, 5, 2] for f in ('north', 'south', 'east', 'west')} | {'up': [1, 10, 5, 14], 'down': [1, 10, 5, 14]}),
    box([6.5 + _LX, 9 + _LY, 8 + _LZ], [9.5 + _LX, 11 + _LY, 8.01 + _LZ], 'lantern',
        faces=['north', 'south'], uv={'north': [11, 1, 14, 3], 'south': [11, 1, 14, 3]}),
]
SNACK = mirror([  # model.SNACK 은 왼쪽(-x)에 있으므로 오른쪽 기준으로 되돌려 둔다
    box([1.2, 2.5, 6.0], [3.0, 8.5, 10.0], 'leather'),
    box([1.0, 7.5, 5.8], [3.0, 9.0, 10.2], 'leather_dark'),
    box([0.8, 6.6, 7.4], [1.2, 7.6, 8.6], 'brass'),
])


TOOL_TILT = 35      # 옆면 기준, 앞쪽으로 돌아가는 각도
TOOL_HALF = 8.5     # 도구 판 크기의 절반: 16px 그림 × (손에 든 크기 0.85 / 등 축소 0.8) = 17px


def hung_tool(sprite_name):
    """손에 든 아이템 모델(두께 있는 판) 그대로·같은 크기, 손잡이가 위. 옆면에 바짝 붙여 앞쪽으로 비스듬히."""
    phi = math.radians(TOOL_TILT)
    hw, thick = TOOL_HALF, 0.7
    nx, nz = math.cos(phi), math.sin(phi)                 # 판의 바깥 방향
    ux, uz = -math.sin(phi), math.cos(phi)                # 판의 가로 방향 (앞쪽 = 안쪽으로 살짝)
    cz, y0, y1 = 8.2, 7.0 - hw, 7.0 + hw                  # 게임과 같은 중심 (BackpackRenderer TOOL_*)
    cx = 15.0                                             # 손잡이 선이 옆면(x=13)에서 2px
    quads = []
    for off in (0.0, thick):
        ox, oz = cx + nx * off, cz + nz * off
        a = (ox - hw * ux, y1, oz - hw * uz)
        b = (ox + hw * ux, y1, oz + hw * uz)
        c = (ox + hw * ux, y0, oz + hw * uz)
        d = (ox - hw * ux, y0, oz - hw * uz)
        quads.append({'kind': 'quad', 'pts': [a, b, c, d], 'tex': sprite_name, 'shade': 0.85 if off else 1.0})
    return quads


def side(kind, right=True):
    """kind: None / 'tool:<sprite>' / 'lantern' / 'soul' / 'snack'"""
    if kind is None:
        return []
    if kind == 'snack':
        els = SNACK
    elif kind in ('lantern', 'soul'):
        els = [dict(e, tex='soul_lantern' if (kind == 'soul' and e['tex'] == 'lantern') else e['tex'])
               for e in LANTERN]
    else:
        els = hung_tool(kind.split(':')[1])
    return els if right else mirror(els)


def pack(right=None, left=None, roll=None, armor=False):
    els = list(BASE) + side(right, True) + side(left, False)
    if roll:
        els += bedroll(roll)
    if armor:
        els += ARMOR
    return els


# ------------------------------------------------------------------ 렌더러 (화면 크기에 맞춰 자동 맞춤, 투명 텍스처 지원)
def render(elements, textures, size, yaw=-28, pitch=26, margin=0.04, bg=(0, 0, 0, 0), frame=None):
    """frame: 크기 기준이 되는 상자 목록. 주면 모든 상태가 같은 크기·위치로 그려진다 (게임의 고정 표시 변환과 같음)."""
    a, b = math.radians(yaw), math.radians(pitch)
    ca, sa, cb, sb = math.cos(a), math.sin(a), math.cos(b), math.sin(b)

    def xf(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x1 = x * ca + z * sa
        z1 = -x * sa + z * ca
        return x1, y * cb - z1 * sb, z1 * cb + y * sb       # 화면 x, 화면 y, 깊이(클수록 앞)

    faces = []
    for e in elements:
        if e.get('kind') == 'quad':
            pts = [xf(p) for p in e['pts']]
            faces.append((sum(p[2] for p in pts) / 4, e, None, pts))
            continue
        for face in e['faces']:
            n = NORMAL[face]
            nz = n[1] * sb + (-n[0] * sa + n[2] * ca) * cb
            if nz <= 1e-6:
                continue
            pts = [xf(p) for p in corners(face, e['from'], e['to'])]
            faces.append((sum(p[2] for p in pts) / 4, e, face, pts))
    faces.sort(key=lambda t: t[0])

    ref = frame if frame is not None else elements
    allpts = []
    for e in ref:
        if e.get('kind') == 'quad':
            allpts += [xf(p) for p in e['pts']]
        else:
            (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
            allpts += [xf((x, y, z)) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    xs = [p[0] for p in allpts]
    ys = [p[1] for p in allpts]
    span = max(max(xs) - min(xs), max(ys) - min(ys))
    scale = size * (1 - 2 * margin) / span
    ox = size / 2 - (max(xs) + min(xs)) / 2 * scale
    oy = size / 2 + (max(ys) + min(ys)) / 2 * scale

    canvas = Image.new('RGBA', (size, size), bg)
    for _, e, face, pts in faces:
        quad = [(ox + p[0] * scale, oy - p[1] * scale) for p in pts]
        t = textures[e['tex']]
        if face is None:
            src = [(0, 0), (t.width, 0), (t.width, t.height), (0, t.height)]
            s = e.get('shade', 1.0)
        else:
            u0, v0, u1, v1 = e['uv'].get(face) or fit_uv(default_uv(face, e['from'], e['to']))
            k = t.width / 16
            src = [(u0 * k, v0 * k), (u1 * k, v0 * k), (u1 * k, v1 * k), (u0 * k, v1 * k)]
            s = SHADE[face]
        try:
            coeffs = homography(quad, src)
        except np.linalg.LinAlgError:
            continue
        warped = t.transform((size, size), Image.PERSPECTIVE, coeffs, Image.NEAREST)
        arr = np.array(warped).astype(float)
        arr[..., :3] *= s
        warped = Image.fromarray(arr.clip(0, 255).astype('uint8'), 'RGBA')
        mask = Image.new('L', (size, size), 0)
        ImageDraw.Draw(mask).polygon(quad, fill=255)
        mask = ImageChops.multiply(mask, warped.split()[3])
        canvas.paste(warped, (0, 0), mask)
    return canvas


def textures():
    T = {'leather': tex.leather(), 'leather_dark': tex.leather_dark(), 'brass': tex.brass(), 'iron': tex.iron(),
         'leather_base': tex.leather_base(), 'lantern': tex.lantern_preview()}
    soul = np.array(tex.lantern_preview()).astype(float)
    glow = (soul[..., 0] > 200)
    soul[glow, 0], soul[glow, 1], soul[glow, 2] = 110, 215, 235
    T['soul_lantern'] = Image.fromarray(soul.astype('uint8'), 'RGBA')
    for c, h in tex.WOOL.items():
        T[f'roll_side_{c}'] = tex.roll_side(h)
        T[f'roll_end_{c}'] = tex.roll_end(h)
    T['pickaxe'] = pickaxe().rotate(225, Image.NEAREST, expand=True)
    T['sword'] = sword().rotate(225, Image.NEAREST, expand=True)
    return T


# ------------------------------------------------------------------ 시안 시트
CASES = [
    ('기본', pack()),
    ('오른쪽 곡괭이', pack(right='tool:pickaxe')),
    ('오른쪽 랜턴\n왼쪽 간식 주머니', pack(right='lantern', left='snack')),
    ('오른쪽 곡괭이\n왼쪽 검', pack(right='tool:pickaxe', left='tool:sword')),
    ('침낭(파랑) + 갑옷주머니', pack(roll='blue', armor=True)),
    ('전부: 랜턴 / 곡괭이\n침낭(빨강) / 갑옷', pack(right='lantern', left='tool:pickaxe', roll='red', armor=True)),
    ('영혼 랜턴 / 간식\n침낭(초록)', pack(right='soul', left='snack', roll='green')),
]


def slot(img, px):
    """바닐라 인벤토리 칸 모양 (가로세로 18 단위, 아이콘 16 단위) 위에 아이콘을 얹는다."""
    k = px / 16
    s = int(18 * k)
    im = Image.new('RGBA', (s, s), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, s - 1, int(k) - 1], fill=(55, 55, 55, 255))
    d.rectangle([0, 0, int(k) - 1, s - 1], fill=(55, 55, 55, 255))
    d.rectangle([0, s - int(k), s - 1, s - 1], fill=(255, 255, 255, 255))
    d.rectangle([s - int(k), 0, s - 1, s - 1], fill=(255, 255, 255, 255))
    im.paste(img, (int(k), int(k)), img)
    return im


FRAME = pack()  # 기본 배낭 기준 크기 (게임과 같음)


def main(out):
    T = textures()
    F = ImageFont.truetype('/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc', 17)
    FB = ImageFont.truetype('/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc', 20)
    col, n = 210, len(CASES)
    H = 120 + 210 + 70 + 230 + 60
    W = Image.new('RGBA', (col * n + 30, H), (46, 46, 52, 255))
    d = ImageDraw.Draw(W)
    d.text((16, 10), '입체 아이콘 시안 — 위: 크게 본 모습 / 가운데: 실제 칸 크기(GUI 3배, 2배) / 아래: 실제 크기 48px 를 4배 확대',
           font=FB, fill=(235, 235, 235))
    d.text((16, 40), '도구 그림(곡괭이·검)과 랜턴은 이 환경에 바닐라 텍스처가 없어 자리표시용으로 그렸습니다. 게임에서는 실제 아이템 그림이 들어갑니다.',
           font=F, fill=(190, 190, 190))
    for i, (cap, els) in enumerate(CASES):
        x = 15 + i * col
        big = render(els, T, 190, frame=FRAME)
        W.paste(big, (x + 10, 80), big)
        d.multiline_text((x + 12, 275), cap, font=F, fill=(225, 225, 225), spacing=2)
        s3 = slot(render(els, T, 48, frame=FRAME), 48)
        s2 = slot(render(els, T, 32, frame=FRAME), 32)
        W.paste(s3, (x + 20, 335))
        W.paste(s2, (x + 110, 343))
        zoom = render(els, T, 48, frame=FRAME).resize((192, 192), Image.NEAREST)
        z = slot(zoom, 192)
        W.paste(z.resize((200, 200), Image.NEAREST), (x + 2, 405))
    W.save(out)


if __name__ == '__main__':
    main(sys.argv[1])
