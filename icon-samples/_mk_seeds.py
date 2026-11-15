"""生成「种子色 → 桌面图标」对照页，取值与 Android 资源完全一致。"""
import json

F = json.load(open("_final.json"))
BODY = "".join('<path d="%s"/>' % p for p in F["paths"])

# key, 中文名, 种子原文, MaterialKolor 派生 primary（与 res/drawable 里一致）, alias
SEEDS = [
    ("lotus",    "睡莲",  "#5D8895", "#396471", "LauncherLotus"),
    ("lavender", "薰衣草", "#6750A4", "#6750A4", "LauncherLavender"),
    ("ocean",    "海青",  "#00696D", "#00696D", "LauncherOcean"),
    ("forest",   "林绿",  "#2E6B4F", "#2C694D", "LauncherForest"),
    ("clay",     "陶土",  "#8B5000", "#8B5000", "LauncherClay"),
    ("rose",     "玫瑰",  "#8E4585", "#8C4383", "LauncherRose"),
    ("slate",    "岩蓝",  "#4F5B92", "#4F5B92", "LauncherSlate"),
]


def icon(primary, size, mask, uid, mono=False):
    clip = ('<circle cx="54" cy="54" r="36"/>' if mask == "c"
            else '<rect x="18" y="18" width="72" height="72" rx="21"/>')
    if mono:
        art = '<rect width="108" height="108" fill="#1B1B1F"/><g fill="#B9B4C0">%s</g>' % BODY
    else:
        art = ('<rect width="108" height="108" fill="%s"/>'
               '<g fill="#FFFFFF">%s</g>') % (primary, BODY)
    return ('<svg viewBox="18 18 72 72" width="{s}" height="{s}" style="display:block">'
            '<defs><clipPath id="c{u}">{clip}</clipPath></defs>'
            '<g clip-path="url(#c{u})">{art}</g></svg>').format(
                s=size, u=uid, clip=clip, art=art)


cards = []
for i, (key, cn, seed, primary, alias) in enumerate(SEEDS):
    sizes = "".join(
        '<span style="width:{s}px;height:{s}px;border-radius:50%%;overflow:hidden;'
        'display:block;line-height:0">{ic}</span>'.format(
            s=s, ic=icon(primary, s, "c", "%s%d" % (key, s)))
        for s in (48, 32, 24))
    badge = '<i class="badge">默认</i>' if key == "lotus" else ""
    cards.append(
        '<div class="card{c}">'
        '<div class="icos">{circ}</div>'
        '<div class="nm">{cn}{badge}</div>'
        '<div class="sw">'
        '  <span><i style="background:{seed}"></i>{seed}</span>'
        '  <span><i style="background:{primary}"></i>{primary}</span>'
        '</div>'
        '<div class="alias">{alias}</div>'
        '<div class="szs">{sizes}</div>'
        '</div>'.format(
            c=" def" if key == "lotus" else "",
            circ=icon(primary, 112, "c", key + "c"),
            cn=cn, badge=badge, seed=seed, primary=primary,
            alias=alias, sizes=sizes))

HTML = """<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>图标 · 跟随种子色</title><style>
*{margin:0;box-sizing:border-box}
body{background:#0B0B0E;color:#E9E4DA;padding:40px 36px 52px;
 font:14px/1.7 "Inter",-apple-system,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif}
h1{font-size:24px;font-weight:600;letter-spacing:-.3px;margin:0 0 8px}
h1 small{font-weight:400;font-size:13px;color:#8B857C;margin-left:12px;letter-spacing:0}
.lede{color:#9A948B;font-size:13px;max-width:78ch;margin:0 0 26px}
.lede b{color:#E9E4DA;font-weight:600}
.grid{display:grid;grid-template-columns:repeat(7,1fr);gap:14px;max-width:1320px}
.card{position:relative;background:#15151A;border:1px solid #262630;border-radius:18px;
 padding:20px 10px 14px;display:flex;flex-direction:column;align-items:center}
.card.def{border-color:#4A4458;background:#1A1821}
.icos{display:flex;gap:10px;filter:drop-shadow(0 4px 12px rgba(0,0,0,.55))}
.nm{margin-top:13px;font-size:13.5px;font-weight:600}
.badge{position:absolute;top:9px;right:9px;
 font-size:9.5px;font-style:normal;background:#3E6B57;color:#EAF6EE;
 padding:1px 6px;border-radius:20px;white-space:nowrap;letter-spacing:.02em}
.sw{margin-top:9px;display:flex;flex-direction:column;gap:3px;width:100%;align-items:center}
.sw span{display:flex;align-items:center;gap:5px;font:11px ui-monospace,monospace;color:#8F8A83}
.sw i{width:9px;height:9px;border-radius:3px;display:block;flex:none}
.alias{margin-top:8px;font:10px ui-monospace,monospace;color:#5F5A54;word-break:break-all;text-align:center}
.szs{margin-top:12px;display:flex;gap:9px;align-items:center}
footer{margin-top:30px;font-size:11.5px;color:#6D6760;max-width:100ch;line-height:1.9}
footer b{color:#9A948B}
</style></head><body>
<h1>图标跟随种子色<small>七颗种子，七套图标，切换即生效</small></h1>
<p class="lede">背景色不是手填的——每一格的下方第一个色块是<b>种子色</b>，第二个是 MaterialKolor
用 TonalSpot 从它派生出来的 <b>primary</b>，图标背景用的就是后者，和 App 内 <code>colorScheme.primary</code> 同一个值。
所以图标和界面不会各说各话。</p>
<div class="grid">__CARDS__</div>
<footer>
切换机制：每颗种子在 AndroidManifest 里绑定一个 <b>activity-alias</b>，各自一套图标。
改设置时只启用当前那颗、停用其余，桌面图标随之重绘。<br>
代价是切换瞬间图标会先消失再出现，这是别名方案的固有表现，不是缺陷。<br>
另外 Android 13+ 若开启「主题化图标」，系统会丢掉颜色、只留 <b>monochrome</b> 字形，改按壁纸重新上色——
那时图标反过来跟随壁纸，App 内的固定种子就管不到它了。
</footer>
</body></html>"""

HTML = HTML.replace("__CARDS__", "".join(cards))
open("seeds.html", "w", encoding="utf-8", newline="\n").write(HTML)
print("written", len(HTML))
