"""내보낸 모델 JSON 파일을 그대로 읽어 그린다 (생성 결과 확인용).

    python tools/preview.py out.png

고리에 건 도구·랜턴은 블록 모델이 아니라 렌더러가 그리므로 여기엔 없다 (preview_icon.py 참고).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import render  # noqa: E402
import tex  # noqa: E402
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets')


def load_model(ref):
    ns, path = ref.split(':')
    data = json.load(open(f'{RES}/{ns}/models/{path}.json', encoding='utf-8'))
    textures, elements = {}, None
    if data.get('parent', '').startswith('trailpack:'):
        textures, elements = load_model(data['parent'])
    textures = {**textures, **data.get('textures', {})}
    return textures, data.get('elements', elements)


def load_texture(ref, textures):
    while ref.startswith('#'):
        ref = textures[ref[1:]]
    if ref.startswith('minecraft:block/'):
        return tex.lantern_preview()
    ns, path = ref.split(':')
    return Image.open(f'{RES}/{ns}/textures/{path}.png').convert('RGBA')


def to_boxes(ref, rot_y=0):
    textures, elements = load_model(ref)
    boxes, texmap = [], {}
    for i, e in enumerate(elements):
        for face, f in e['faces'].items():
            key = f'{ref}#{f["texture"]}'
            texmap[key] = load_texture(f['texture'], textures)
            boxes.append({'from': e['from'], 'to': e['to'], 'tex': key, 'faces': [face], 'uv': {face: f['uv']}})
    return boxes, texmap


def scene(refs):
    boxes, texmap = [], {}
    for ref, dz in refs:
        b, t = to_boxes(ref)
        for x in b:
            x['from'] = [x['from'][0], x['from'][1], x['from'][2] + dz]
            x['to'] = [x['to'][0], x['to'][1], x['to'][2] + dz]
        boxes += b; texmap.update(t)
    return boxes, texmap


def main(out):
    F = ImageFont.truetype('/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc', 22)
    bg = (46, 46, 52, 255)
    pack = [(f'trailpack:block/{m}', 0) for m in
            ('backpack', 'backpack_snack_right', 'backpack_armor', 'backpack_roll_orange')]
    shots = [
        (scene(pack), -35, 28, 20, '배낭 블록 모델 (도구·랜턴 제외)'),
        (scene(pack), 215, 28, 20, '배낭 등쪽'),
        (scene([('trailpack:block/cyan_sleeping_bag_head', -8), ('trailpack:block/cyan_sleeping_bag_foot', 8)]),
         -40, 38, 15, '침낭 (모델 파일에서)'),
    ]
    S = 520
    W = Image.new('RGBA', (S * len(shots), S + 40), bg)
    d = ImageDraw.Draw(W)
    for i, ((boxes, texmap), yaw, pitch, scale, cap) in enumerate(shots):
        im = render.render(boxes, texmap, yaw, pitch_deg=pitch, scale=scale, size=760, bg=bg)
        W.paste(im.crop((120, 130, 120 + S, 130 + S)), (S * i, 0))
        d.text((S * i + 16, S + 4), cap, font=F, fill=(225, 225, 225))
    W.save(out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'preview.png')
