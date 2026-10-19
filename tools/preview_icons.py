"""부착물 아이템 아이콘과 침낭 16색 아이콘 확인용 시트.

    python tools/preview_icons.py out.png
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import tex  # noqa: E402
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

F = ImageFont.truetype('/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc', 16)
items = [('간식 주머니', tex.icon_snack_pouch()), ('도구걸이', tex.icon_tool_hook()),
         ('침낭끈', tex.icon_bedroll_strap()), ('갑옷주머니', tex.icon_armor_pouch())]
items += [(c, tex.roll_icon(h)) for c, h in tex.WOOL.items()]
cols = 10
W = Image.new('RGBA', (cols * 110, ((len(items) + cols - 1) // cols) * 130), (46, 46, 52, 255))
d = ImageDraw.Draw(W)
for i, (cap, ic) in enumerate(items):
    x, y = (i % cols) * 110 + 15, (i // cols) * 130 + 10
    big = ic.resize((80, 80), Image.NEAREST)
    W.paste(big, (x, y), big)
    d.text((x, y + 88), cap, font=F, fill=(220, 220, 220))
W.save(sys.argv[1] if len(sys.argv) > 1 else 'icons_preview.png')
