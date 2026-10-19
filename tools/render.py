"""블록 모델(직육면체 목록)을 텍스처 입혀 비스듬히 그리는 간이 렌더러 (미리보기용)."""
import math
import numpy as np
from PIL import Image, ImageDraw

SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
NORMAL = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1),
          'east': (1, 0, 0), 'west': (-1, 0, 0)}

def default_uv(face, f, t):
    x0, y0, z0 = f; x1, y1, z1 = t
    return {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        'south': [x0, 16 - y1, x1, 16 - y0],
        'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0],
        'up': [x0, z0, x1, z1],
        'down': [x0, 16 - z1, x1, 16 - z0],
    }[face]

def fit_uv(uv):
    """0~16 밖으로 나가는 uv 를 방향은 유지한 채 안쪽으로 옮긴다 (게임에서도 같은 값을 쓴다)."""
    u0, v0, u1, v1 = uv
    def fit(a, b):
        lo, hi = min(a, b), max(a, b)
        w = min(hi - lo, 16)
        if lo < 0 or hi > 16:
            lo = 0 if lo < 0 else 16 - w
            hi = lo + w
        return (lo, hi) if a <= b else (hi, lo)
    u0, u1 = fit(u0, u1)
    v0, v1 = fit(v0, v1)
    return [round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3)]

def corners(face, f, t):
    x0, y0, z0 = f; x1, y1, z1 = t
    return {  # 바깥에서 본 TL, TR, BR, BL
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]

def homography(dst, src):
    A, b = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); b.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); b.append(v)
    return np.linalg.solve(np.array(A, float), np.array(b, float)).tolist()

def render(elements, textures, yaw_deg, pitch_deg=28, scale=22, size=640, rot_y=0, bg=(46, 46, 52, 255)):
    a, b = math.radians(yaw_deg), math.radians(pitch_deg)
    ca, sa, cb, sb = math.cos(a), math.sin(a), math.cos(b), math.sin(b)

    def xf(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x1 = x * ca - z * sa
        z1 = x * sa + z * ca
        y2 = y * cb - z1 * sb
        z2 = z1 * cb + y * sb
        return x1, y2, z2

    def scr(p):
        x, y, z = xf(p)
        return (size / 2 + x * scale, size / 2 + 40 - y * scale), z  # 앞(+z)에서 보면 착용자 오른쪽(+x)이 화면 오른쪽

    faces = []
    for e in elements:
        for face in e['faces']:
            n = NORMAL[face]
            nz = n[0] * sa * cb + n[2] * ca * cb + n[1] * sb  # 카메라 쪽(+z')을 향한 면만
            if nz <= 1e-6:
                continue
            pts = corners(face, e['from'], e['to'])
            sp = [scr(p) for p in pts]
            depth = sum(z for _, z in sp) / 4
            faces.append((depth, face, e, [q for q, _ in sp]))
    faces.sort(key=lambda t: t[0])

    canvas = Image.new('RGBA', (size, size), bg)
    for depth, face, e, quad in faces:
        tex = textures[e['tex']]
        u0, v0, u1, v1 = e['uv'].get(face) or fit_uv(default_uv(face, e['from'], e['to']))
        k = tex.width / 16
        src = [(u0 * k, v0 * k), (u1 * k, v0 * k), (u1 * k, v1 * k), (u0 * k, v1 * k)]
        try:
            coeffs = homography(quad, src)
        except np.linalg.LinAlgError:
            continue
        warped = tex.transform((size, size), Image.PERSPECTIVE, coeffs, Image.NEAREST)
        s = SHADE[face]
        arr = np.array(warped).astype(float)
        arr[..., :3] *= s
        warped = Image.fromarray(arr.clip(0, 255).astype('uint8'), 'RGBA')
        mask = Image.new('L', (size, size), 0)
        ImageDraw.Draw(mask).polygon(quad, fill=255)
        canvas.paste(warped, (0, 0), mask)
    return canvas
