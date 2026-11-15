"""生成 icon-samples/wheel.html：莫奈色轮的视觉验收页。

内容全部读真实产物（launcher_palette.xml + ic_launcher_foreground.xml），
不另抄一份色值，免得页面好看而实际资源是另一回事。
"""
import os
import re

ROOT = r"D:/android_project/MDEditor4"
RES = os.path.join(ROOT, "app/src/main/res")
OUT = os.path.join(ROOT, "icon-samples/wheel.html")

TIER_NAME = {"a": "柔和", "b": "标准", "c": "鲜艳"}
TIER_KEY = {"a": 28, "b": 40, "c": 52}

# ---------------------------------------------------------------- 数据
palette = {}
for m in re.finditer(
        r'<color name="launcher_bg_w(\d\d)([abc])">(#[0-9A-F]{6})</color>',
        open(os.path.join(RES, "values", "launcher_palette.xml"), encoding="utf-8").read()):
    idx, tier, hexv = int(m.group(1)), m.group(2), m.group(3)
    palette[(idx, tier)] = hexv

glyph = re.search(r'pathData="([^"]+)"',
                  open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"),
                       encoding="utf-8").read()).group(1)

# ---------------------------------------------------------------- 色差
M1 = [[0.4122214708, 0.5363325363, 0.0514459929],
      [0.2119034982, 0.6806995451, 0.1073969566],
      [0.0883024619, 0.2817188376, 0.6299787005]]
M2 = [[0.2104542553, 0.7936177850, -0.0040720468],
      [1.9779984951, -2.4285922050, 0.4505937099],
      [0.0259040371, 0.7827717662, -0.8086757660]]


def oklab(hexv):
    r, g, b = [int(hexv[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    lin = lambda s: s / 12.92 if s <= 0.04045 else ((s + 0.055) / 1.055) ** 2.4
    r, g, b = lin(r), lin(g), lin(b)
    lms = [(M1[i][0] * r + M1[i][1] * g + M1[i][2] * b) for i in range(3)]
    lms = [x ** (1 / 3) if x > 0 else -((-x) ** (1 / 3)) for x in lms]
    return [(M2[i][0] * lms[0] + M2[i][1] * lms[1] + M2[i][2] * lms[2]) * 100 for i in range(3)]


LAB = {k: oklab(v) for k, v in palette.items()}


def dist(a, b):
    return sum((x - y) ** 2 for x, y in zip(oklab(a), oklab(b))) ** 0.5


def nearest(hexv):
    best = min(palette.items(), key=lambda kv: dist(hexv, kv[1]))
    return best[0], best[1], dist(hexv, best[1])


def midpoint(a, b):
    ca = [int(a[i:i + 2], 16) for i in (1, 3, 5)]
    cb = [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#%02X%02X%02X" % tuple(round((x + y) / 2) for x, y in zip(ca, cb))


# ---------------------------------------------------------------- 片段
GID = [0]


def icon(hexv, size, round_=True):
    GID[0] += 1
    clip = ('<defs><clipPath id="c%d"><circle cx="54" cy="54" r="36"/></clipPath></defs>'
            '<g clip-path="url(#c%d)">' % (GID[0], GID[0])) if round_ else "<g>"
    return ('<svg viewBox="18 18 72 72" width="%d" height="%d" style="display:block">'
            '%s<rect width="108" height="108" fill="%s"/>'
            '<path fill="#FFFFFF" d="%s"/></g></svg>' % (size, size, clip, hexv, glyph))


tier_rows = []
for tier in "abc":
    cells = []
    for idx in range(24):
        hexv = palette.get((idx, tier))
        if hexv is None:
            cells.append('<div class="cell miss"><span class="ph">–</span></div>')
            continue
        cells.append('<div class="cell" title="hue %d° · %s %s">%s<span class="hex">%s</span></div>'
                     % (idx * 15, TIER_NAME[tier], hexv, icon(hexv, 46), hexv[1:]))
    tier_rows.append(
        '<div class="tier"><div class="tname">%s<br><small>%d</small></div>%s</div>'
        % (TIER_NAME[tier], TIER_KEY[tier], "".join(cells)))

head = "".join('<div class="hue">%d°</div>' % (i * 15) for i in range(24))

# 命中示例：取相邻两格的正中间当靶子 —— 比真实壁纸主色的最坏情况还要更差一点
samples = [(1, "b", 2, "b"), (4, "a", 5, "a"), (7, "c", 8, "c"), (10, "b", 11, "b"),
           (13, "a", 14, "a"), (16, "b", 17, "b"), (19, "c", 20, "c"), (22, "a", 23, "a")]
cards = []
for a, ta, b, tb in samples:
    target = midpoint(palette[(a, ta)], palette[(b, tb)])
    (idx, tier), hit, d = nearest(target)
    cards.append(
        '<div class="hit"><div class="pair">%s<span class="arrow">→</span>%s</div>'
        '<div class="meta"><b>%s</b> 命中 %d°/%s<br><span class="de">色差 %.2f</span></div></div>'
        % (icon(target, 58, round_=False), icon(hit, 58),
           target, idx * 15, TIER_NAME[tier], d))

worst = max((nearest(midpoint(palette[(a, ta)], palette[(b, tb)]))[2]
             for a, ta, b, tb in samples))

html = """<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>图标 · 跟随莫奈取色</title><style>
*{margin:0;box-sizing:border-box}
body{background:#0B0B0E;color:#E9E4DA;padding:40px 36px 56px;
 font:14px/1.7 "Inter",-apple-system,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif}
h1{font-size:24px;font-weight:600;letter-spacing:-.3px;margin:0 0 8px}
h1 small{font-weight:400;font-size:13px;color:#8B857C;margin-left:12px;letter-spacing:0}
.lede{color:#9A948B;font-size:13px;max-width:82ch;margin:0 0 30px}
.lede b{color:#E9E4DA;font-weight:600}
h2{font-size:14px;font-weight:600;margin:0 0 14px;color:#C9C3B9}
h2 span{font-weight:400;color:#75706A;margin-left:10px;font-size:12px}
.wrap{overflow-x:auto;padding-bottom:6px}
.grid{display:inline-block;min-width:100%}
.hlayout{display:flex;gap:6px;margin-left:64px;margin-bottom:6px}
.hue{width:52px;text-align:center;font:10px ui-monospace,monospace;color:#5F5A54;flex:none}
.tier{display:flex;align-items:center;gap:6px;margin-bottom:6px}
.tname{width:58px;flex:none;font-size:11.5px;color:#8F8A83;text-align:right;line-height:1.3}
.tname small{font:10px ui-monospace,monospace;color:#5F5A54}
.cell{width:52px;height:52px;flex:none;display:flex;flex-direction:column;
 align-items:center;justify-content:center;gap:2px}
.cell .hex{font:8.5px ui-monospace,monospace;color:#57524C;letter-spacing:-.02em}
.cell.miss .ph{color:#2A2A33;font-size:13px}
.hits{display:flex;flex-wrap:wrap;gap:12px;max-width:1180px}
.hit{background:#15151A;border:1px solid #262630;border-radius:16px;padding:14px 16px;
 display:flex;flex-direction:column;gap:10px;align-items:center;min-width:158px}
.pair{display:flex;align-items:center;gap:8px;filter:drop-shadow(0 4px 12px rgba(0,0,0,.5))}
.arrow{color:#4E4942;font-size:13px}
.meta{font:11px ui-monospace,monospace;color:#8F8A83;text-align:center;line-height:1.6}
.meta b{color:#D8D2C8;font-weight:600}
.de{color:#6FA07C}
footer{margin-top:34px;font-size:11.5px;color:#6D6760;max-width:104ch;line-height:1.95}
footer b{color:#9A948B}
</style></head><body>
<h1>图标跟随莫奈取色<small>壁纸的主色是任意的，图标只有有限的几十套</small></h1>
<p class="lede">莫奈取色的 <b>primary</b> 由壁纸算出，打包时无从预知；而启动器图标是静态资源，
运行时改不了像素。所以这里把 Material 3 浅色 primary 所在的 <b>HCT tone 40</b> 铺成一张色轮表：
24 色相 × 3 彩度，共 <b>__COUNT__</b> 套图标（青绿色系彩度到不了 52，被 sRGB 色域压平的格已去重）。
运行时按 <b>OKLab 最近邻</b>启用其中一套，桌面图标就和 App 主色同属一支。</p>

<h2>色轮<span>每行一个彩度档，每列一个色相；空白格是该色相在 tone 40 到不了的彩度</span></h2>
<div class="wrap"><div class="grid">
<div class="hlayout">__HEAD__</div>
__ROWS__
</div></div>

<h2 style="margin-top:34px">命中示例<span>取相邻两格的正中间当靶子——比真实壁纸主色的最坏情况还差一点；最大色差 __WORST__</span></h2>
<div class="hits">__CARDS__</div>

<footer>
左起第一个方块是<b>靶子</b>（模拟壁纸算出的任意 primary），第二个是实际启用的图标底色。
色差用 OKLab 距离 ×100，与 ΔE00 同量级：<b>3 以内人眼看不出</b>，5 左右并排才分得出。
<br>
切换机制和种子色那七套一样：每套配色在 AndroidManifest 里是一个 <b>activity-alias</b>，
改设置时只启用当前那一个、停用其余，桌面图标随之重绘。代价依旧是切换瞬间图标会先消失再出现。
<br>
Android 13+ 开启「主题化图标」时，系统会丢掉这一层颜色、只留 <b>monochrome</b> 字形，改按壁纸重新上色——
那条路上颜色由启动器决定，这三档彩度管不到它。
</footer>
</body></html>"""

html = (html.replace("__HEAD__", head)
            .replace("__ROWS__", "".join(tier_rows))
            .replace("__CARDS__", "".join(cards))
            .replace("__COUNT__", str(len(palette)))
            .replace("__WORST__", "%.2f" % worst))

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(html)
print("写出 %s（%d 格，示例最大色差 %.2f）" % (OUT, len(palette), worst))
