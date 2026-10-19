"""배낭 모델 정의 (마인크래프트 블록 모델 좌표, 북쪽 = 등쪽 면).
착용자 기준: 오른쪽 = +x, 앞(주머니 쪽) = +z, 위 = +y.
같은 정의로 미리보기도 그리고, 나중에 모델 JSON 으로 그대로 내보낸다.
"""

def mirror(elements):
    """좌우(x) 반전. 왼쪽 부착물 모델로 오른쪽 모델을 만들 때 쓴다."""
    out = []
    for e in elements:
        e2 = dict(e)
        e2['from'] = [16 - e['to'][0], e['from'][1], e['from'][2]]
        e2['to'] = [16 - e['from'][0], e['to'][1], e['to'][2]]
        e2['faces'] = [{'east': 'west', 'west': 'east'}.get(f, f) for f in e['faces']]
        e2['uv'] = {}
        out.append(e2)
    return out


def box(frm, to, tex, faces=None, uv=None):
    """faces: 그릴 면 목록(기본 6면). uv: 면별 uv 덮어쓰기 {dir: [u0,v0,u1,v1]}"""
    return {'from': frm, 'to': to, 'tex': tex, 'faces': faces or ['north', 'south', 'east', 'west', 'up', 'down'],
            'uv': uv or {}}

# ---- 몸통 ----
BASE = [
    box([3, 2, 5], [13, 14, 11], 'leather'),                     # 몸통
    box([2.8, 0, 4.8], [13.2, 2, 11.2], 'leather_base'),          # 두꺼운 가죽 바닥
    box([2.5, 13, 4.5], [13.5, 15.5, 11.8], 'leather'),           # 덮개
    box([4.5, 10, 11.0], [11.5, 13.2, 12.0], 'leather'),          # 덮개 앞 늘어진 부분
    box([7.2, 7.5, 11.0], [8.8, 13.4, 12.3], 'leather_dark'),     # 덮개 끈
    box([7.0, 8.0, 12.3], [9.0, 9.4, 12.7], 'brass'),             # 덮개 버클
    # 등쪽 철 프레임 (위로 솟음)
    box([3, 0, 3.6], [4, 17.5, 4.6], 'iron'),
    box([12, 0, 3.6], [13, 17.5, 4.6], 'iron'),
    box([3, 16, 3.7], [13, 17, 4.5], 'iron'),                     # 위 가로대
    box([3, 6, 3.7], [13, 6.8, 4.5], 'iron'),                     # 아래 가로대
    # 어깨끈 (등쪽)
    box([4.6, 3, 4.2], [6.2, 13.5, 5.0], 'leather_dark'),
    box([9.8, 3, 4.2], [11.4, 13.5, 5.0], 'leather_dark'),
    # 가로 압축끈 (앞·옆을 한 바퀴)
    box([2.85, 5, 4.9], [13.15, 6, 11.15], 'leather_dark'),
    box([12.9, 5.1, 9.0], [13.4, 5.9, 10.2], 'brass'),            # 오른쪽 압축끈 버클
    # 앞면 데이지체인
    box([5.0, 2.5, 11.0], [6.0, 9.5, 11.4], 'leather_dark'),
    box([10.0, 2.5, 11.0], [11.0, 9.5, 11.4], 'leather_dark'),
    # 위 손잡이 고리
    box([7.0, 15.5, 6.5], [9.0, 16.2, 7.5], 'leather_dark'),
]

# ---- 부착물 ----
SNACK = [  # 왼쪽(-x) 옆주머니. 오른쪽은 mirror(SNACK)
    box([1.2, 2.5, 6.0], [3.0, 8.5, 10.0], 'leather'),
    box([1.0, 7.5, 5.8], [3.0, 9.0, 10.2], 'leather_dark'),       # 주머니 덮개
    box([0.8, 6.6, 7.4], [1.2, 7.6, 8.6], 'brass'),
]
ARMOR = [  # 앞면 큰 덮개 주머니
    box([4.0, 1.5, 11.0], [12.0, 9.0, 13.2], 'leather'),
    box([3.8, 7.6, 10.9], [12.2, 9.4, 13.5], 'leather_dark'),     # 덮개
    box([7.2, 6.6, 13.2], [8.8, 7.8, 13.7], 'brass'),
]

def bedroll(color=None):
    """덮개 위에 묶은 침낭 롤 (가로 원통을 상자 3개로 둥글게 근사).
    color 가 None 이면 텍스처 변수 side/end 를 쓰는 틀(모델 파일용)을 만든다."""
    side, end = (f'roll_side_{color}', f'roll_end_{color}') if color else ('side', 'end')
    roll = [
        box([1.0, 15.5, 6.4], [15.0, 19.0, 9.6], side, faces=['up', 'down', 'north', 'south']),
        box([1.0, 16.1, 5.8], [15.0, 18.4, 10.2], side, faces=['north', 'south']),
        box([1.0, 15.5, 5.8], [15.0, 19.0, 10.2], end, faces=['east', 'west']),
        # 묶는 끈
        box([4.0, 15.3, 5.6], [5.2, 19.2, 10.4], 'leather_dark'),
        box([10.8, 15.3, 5.6], [12.0, 19.2, 10.4], 'leather_dark'),
    ]
    return roll


def bag_foot(color=None):
    """설치된 침낭 발 칸 (북쪽 = 머리 쪽). color 가 None 이면 top/side 변수를 쓰는 틀."""
    top, side = (f'bag_top_{color}', f'bag_side_{color}') if color else ('top', 'side')
    sides = ['east', 'west', 'north', 'south', 'down']
    return [
        box([1, 0, 0], [15, 2.2, 15.5], side, faces=sides),
        box([1, 0, 0], [15, 2.2, 15.5], top, faces=['up']),
        box([1.5, 2.2, 14.0], [14.5, 2.6, 15.0], side),                   # 발끝 솔기
    ]

def bag_head(color=None):
    """설치된 침낭 머리 칸: 겉감을 젖혀 안감이 보이고, 머리를 감싸는 후드 테두리."""
    top, side = (f'bag_top_{color}', f'bag_side_{color}') if color else ('top', 'side')
    sides = ['east', 'west', 'north', 'south', 'down']
    return [
        box([1, 0, 1], [15, 1.6, 12], side, faces=sides),
        box([1, 0, 1], [15, 1.6, 12], 'lining', faces=['up']),
        box([1, 0, 12], [15, 2.2, 16], side, faces=sides),
        box([1, 0, 12], [15, 2.2, 16], top, faces=['up']),
        box([0.8, 1.4, 9.8], [15.2, 3.4, 12.6], top),                      # 젖힌 겉감 접힘
        box([0.8, 3.0, 9.8], [15.2, 3.4, 12.6], 'lining', faces=['north']),
        box([1, 0, 0.6], [15, 2.6, 2.4], top),                            # 후드 테두리
        box([1, 0, 2.4], [2.4, 2.4, 9.8], top),
        box([13.6, 0, 2.4], [15, 2.4, 9.8], top),
    ]

def shift(elements, dz):
    out = []
    for e in elements:
        e2 = dict(e); e2['from'] = [e['from'][0], e['from'][1], e['from'][2] + dz]
        e2['to'] = [e['to'][0], e['to'][1], e['to'][2] + dz]; out.append(e2)
    return out

def placed_bag(color):
    """미리보기용: 머리 칸(z -8~8)과 발 칸(z 8~24)을 붙인다."""
    return shift(bag_head(color), -8) + shift(bag_foot(color), 8)
