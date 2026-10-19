"""모델·텍스처·블록 상태·레시피·전리품·태그·언어 파일을 만든다.

    python tools/assets.py

tex.py(그림)와 model.py(모양)를 고친 뒤 다시 돌리면 src/main/resources 아래가 갱신된다.
미리보기는 tools/preview.py, 아이콘 시트는 tools/preview_icons.py.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import model  # noqa: E402
import tex  # noqa: E402
from render import default_uv, fit_uv  # noqa: E402

NS = 'trailpack'
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', NS)
DATA = os.path.join(ROOT, 'data', NS)
COLORS = list(tex.WOOL)  # 바닐라 DyeColor 순서와 같다 (id 0~15)
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def write_png(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def model_json(elements, textures, parent='minecraft:block/block', particle=None):
    """model.py 의 상자 목록 → 블록 모델 JSON. 상자의 tex 이름은 텍스처 변수(#이름)가 된다."""
    els = []
    for e in elements:
        faces = {}
        for face in e['faces']:
            uv = e['uv'].get(face) or fit_uv(default_uv(face, e['from'], e['to']))
            faces[face] = {'uv': uv, 'texture': '#' + e['tex']}
        els.append({'from': [round(v, 3) for v in e['from']], 'to': [round(v, 3) for v in e['to']], 'faces': faces})
    tx = dict(textures)
    if particle:
        tx['particle'] = particle
    out = {'parent': parent, 'textures': tx, 'elements': els} if parent else {'textures': tx, 'elements': els}
    return out


def block_tex(name):
    return f'{NS}:block/{name}'


def main():
    # ------------------------------------------------------------ 블록 텍스처
    for name, fn in [('leather', tex.leather), ('leather_dark', tex.leather_dark), ('brass', tex.brass),
                     ('iron', tex.iron), ('leather_base', tex.leather_base), ('bag_lining', tex.lining)]:
        write_png(f'{ASSETS}/textures/block/{name}.png', fn())
    for c in COLORS:
        h = tex.WOOL[c]
        write_png(f'{ASSETS}/textures/block/roll_side_{c}.png', tex.roll_side(h))
        write_png(f'{ASSETS}/textures/block/roll_end_{c}.png', tex.roll_end(h))
        write_png(f'{ASSETS}/textures/block/bag_top_{c}.png', tex.bag_top(h))
        write_png(f'{ASSETS}/textures/block/bag_side_{c}.png', tex.bag_side(h))

    # ------------------------------------------------------------ 아이템 텍스처
    for name, fn in [('snack_pouch', tex.icon_snack_pouch), ('tool_hook', tex.icon_tool_hook),
                     ('bedroll_strap', tex.icon_bedroll_strap), ('armor_pouch', tex.icon_armor_pouch)]:
        write_png(f'{ASSETS}/textures/item/{name}.png', fn())
    for c in COLORS:
        write_png(f'{ASSETS}/textures/item/{c}_sleeping_bag.png', tex.roll_icon(tex.WOOL[c]))

    # ------------------------------------------------------------ 배낭 블록 모델
    common = {'leather': block_tex('leather'), 'leather_dark': block_tex('leather_dark'), 'brass': block_tex('brass'),
              'iron': block_tex('iron'), 'leather_base': block_tex('leather_base')}
    mb = f'{ASSETS}/models/block'
    write_json(f'{mb}/backpack.json', model_json(model.BASE, common, particle=block_tex('leather')))
    write_json(f'{mb}/backpack_snack_left.json', model_json(model.SNACK, common, parent=None, particle=block_tex('leather')))
    write_json(f'{mb}/backpack_snack_right.json',
               model_json(model.mirror(model.SNACK), common, parent=None, particle=block_tex('leather')))
    write_json(f'{mb}/backpack_armor.json', model_json(model.ARMOR, common, parent=None, particle=block_tex('leather')))
    roll_tpl = model_json(model.bedroll(), {'leather_dark': block_tex('leather_dark')}, parent=None)
    write_json(f'{mb}/template_backpack_roll.json', roll_tpl)
    for c in COLORS:
        write_json(f'{mb}/backpack_roll_{c}.json', {
            'parent': f'{NS}:block/template_backpack_roll',
            'textures': {'side': block_tex(f'roll_side_{c}'), 'end': block_tex(f'roll_end_{c}'),
                         'particle': block_tex(f'roll_side_{c}')}})

    # ------------------------------------------------------------ 배낭 블록 상태 (multipart)
    mp = []

    def part(model_name, cond):
        for face, y in ROT.items():
            c = dict(cond); c['facing'] = face
            a = {'model': f'{NS}:block/{model_name}'}
            if y:
                a['y'] = y
            mp.append({'when': c, 'apply': a})

    part('backpack', {})
    part('backpack_snack_right', {'right_pouch': 'true'})
    part('backpack_snack_left', {'left_pouch': 'true'})
    part('backpack_armor', {'armor': 'true'})
    for c in COLORS:
        part(f'backpack_roll_{c}', {'roll': c})
    write_json(f'{ASSETS}/blockstates/backpack.json', {'multipart': mp})

    # ------------------------------------------------------------ 침낭 블록 모델·상태
    for half, els in (('head', model.bag_head()), ('foot', model.bag_foot())):
        write_json(f'{mb}/template_sleeping_bag_{half}.json',
                   model_json(els, {'lining': block_tex('bag_lining')}, parent=None))
    for c in COLORS:
        for half in ('head', 'foot'):
            write_json(f'{mb}/{c}_sleeping_bag_{half}.json', {
                'parent': f'{NS}:block/template_sleeping_bag_{half}',
                'textures': {'top': block_tex(f'bag_top_{c}'), 'side': block_tex(f'bag_side_{c}'),
                             'particle': block_tex(f'bag_top_{c}')}})
        variants = {}
        for face, y in ROT.items():
            for half in ('head', 'foot'):
                v = {'model': f'{NS}:block/{c}_sleeping_bag_{half}'}
                if y:
                    v['y'] = y
                variants[f'facing={face},part={half}'] = v
        write_json(f'{ASSETS}/blockstates/{c}_sleeping_bag.json', {'variants': variants})

    # ------------------------------------------------------------ 아이템 모델
    mi = f'{ASSETS}/models/item'
    gen = 'minecraft:item/generated'
    for name in ('snack_pouch', 'tool_hook', 'bedroll_strap', 'armor_pouch'):
        write_json(f'{mi}/{name}.json', {'parent': gen, 'textures': {'layer0': f'{NS}:item/{name}'}})
    for c in COLORS:
        write_json(f'{mi}/{c}_sleeping_bag.json', {'parent': gen, 'textures': {'layer0': f'{NS}:item/{c}_sleeping_bag'}})

    # 배낭 아이템: 그림 없이 BackpackItemRenderer 가 착용 모습 그대로 입체로 그린다.
    # display 는 그 입체 모델을 화면별로 놓는 각도·크기. gui 는 오른쪽 앞에서 비스듬히, 칸에 꽉 차게
    # (양옆 도구·랜턴과 침낭까지 들어가는 크기로 고정 — 계산은 preview_icon.py 의 FRAME).
    write_json(f'{mi}/backpack.json', {
        'parent': 'minecraft:builtin/entity',
        'gui_light': 'side',
        'textures': {'particle': block_tex('leather')},
        'display': {
            'gui': {'rotation': [26, -28, 0], 'translation': [0, 0, 0], 'scale': [0.72, 0.72, 0.72]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
            'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.35, 0.35, 0.35]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.35, 0.35, 0.35]},
        }})

    # ------------------------------------------------------------ 데이터: 침낭 조합·염색·전리품·태그
    for c in COLORS:
        write_json(f'{DATA}/recipe/{c}_sleeping_bag.json', {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'sleeping_bag',
            'pattern': ['W', 'W', 'W'], 'key': {'W': {'item': f'minecraft:{c}_wool'}},
            'result': {'id': f'{NS}:{c}_sleeping_bag', 'count': 1}})
        write_json(f'{DATA}/recipe/dye_{c}_sleeping_bag.json', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'sleeping_bag_dye',
            'ingredients': [{'tag': f'{NS}:sleeping_bags'}, {'item': f'minecraft:{c}_dye'}],
            'result': {'id': f'{NS}:{c}_sleeping_bag', 'count': 1}})
        write_json(f'{DATA}/loot_table/blocks/{c}_sleeping_bag.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'bonus_rolls': 0,
                       'conditions': [{'condition': 'minecraft:block_state_property', 'block': f'{NS}:{c}_sleeping_bag',
                                       'properties': {'part': 'head'}}],
                       'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{c}_sleeping_bag'}]}],
            'random_sequence': f'{NS}:blocks/{c}_sleeping_bag'})
    write_json(f'{DATA}/tags/item/hook_tools.json', {'values': [
        '#minecraft:pickaxes', '#minecraft:axes', '#minecraft:shovels', '#minecraft:hoes', '#minecraft:swords',
        {'id': '#c:tools', 'required': False}]})
    write_json(f'{DATA}/tags/item/sleeping_bags.json', {'values': [f'{NS}:{c}_sleeping_bag' for c in COLORS]})

    # ------------------------------------------------------------ 언어
    ko_colors = {'white': '하얀색', 'orange': '주황색', 'magenta': '자홍색', 'light_blue': '하늘색', 'yellow': '노란색',
                 'lime': '연두색', 'pink': '분홍색', 'gray': '회색', 'light_gray': '회백색', 'cyan': '청록색',
                 'purple': '보라색', 'blue': '파란색', 'brown': '갈색', 'green': '초록색', 'red': '빨간색',
                 'black': '검은색'}
    lang_dir = f'{ASSETS}/lang'
    for code, names, word in (('ko_kr', ko_colors, '침낭'), ('en_us', None, 'Sleeping Bag')):
        path = f'{lang_dir}/{code}.json'
        data = json.load(open(path, encoding='utf-8'))
        for c in COLORS:
            if names:
                label = f'{names[c]} {word}'
            else:
                label = ' '.join(w.capitalize() for w in c.split('_')) + ' ' + word
            data[f'block.{NS}.{c}_sleeping_bag'] = label
        write_json(path, data)
    print('assets ok')


if __name__ == '__main__':
    main()
