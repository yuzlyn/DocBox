# -*- coding: utf-8 -*-
"""DocBox 图标 · MD3 纯色 + Apple 网格留白"""
import json

F = json.load(open('_final.json'))
BODY = ''.join('<path d="%s"/>' % p for p in F['paths'])

M3 = {
    'primary': '#6750A4', 'onPrimary': '#FFFFFF',
    'primaryContainer': '#EADDFF', 'onPrimaryContainer': '#21005D',
    'secondary': '#625B71', 'onSecondary': '#FFFFFF',
    'secondaryContainer': '#E8DEF8', 'onSecondaryContainer': '#1D192B',
    'tertiary': '#7D5260', 'onTertiary': '#FFFFFF',
    'tertiaryContainer': '#FFD8E4', 'onTertiaryContainer': '#31111D',
    'surface': '#FEF7FF', 'onSurface': '#1D1B20',
    'surfaceContainer': '#F3EDF7',
    'inverseSurface': '#313033', 'inverseOnSurface': '#F4EFF4',
    'outline': '#79747E', 'outlineVariant': '#CAC4D0',
}

CANDS = [
    ('primary', 'onPrimary', '主色 / 主色上的内容'),
    ('primaryContainer', 'onPrimaryContainer', '主色容器 / 其上内容'),
    ('inverseSurface', 'inverseOnSurface', '反色表面 / 其上内容'),
    ('tertiary', 'onTertiary', '第三色 / 第三色上的内容'),
    ('secondaryContainer', 'onSecondaryContainer', '次色容器 / 其上内容'),
]
HERO = CANDS[0]


def _lum(h):
    h = h.lstrip('#')
    r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)


def contrast(a, b):
    la, lb = _lum(a), _lum(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def svg(bg, fg, size, mask='c', uid=''):
    clip = ('<circle cx="54" cy="54" r="36"/>' if mask == 'c'
            else ('<rect x="18" y="18" width="72" height="72" rx="21"/>' if mask == 's'
                  else '<rect width="108" height="108"/>'))
    u = '%s%s%s%s' % (uid, size, mask, bg.lstrip('#'))
    return ('<svg viewBox="18 18 72 72" width="{s}" height="{s}" style="display:block">'
            '<defs><clipPath id="m{u}">{clip}</clipPath></defs>'
            '<g clip-path="url(#m{u})"><rect width="108" height="108" fill="{bg}"/>'
            '<g fill="{fg}">{body}</g></g></svg>').format(
        s=size, u=u, clip=clip, bg=bg, fg=fg, body=BODY)


def mono_svg(size, uid=''):
    u = 'mono%s%s' % (uid, size)
    return ('<svg viewBox="18 18 72 72" width="{s}" height="{s}" style="display:block">'
            '<defs><clipPath id="m{u}"><circle cx="54" cy="54" r="36"/></clipPath></defs>'
            '<g clip-path="url(#m{u})"><rect width="108" height="108" fill="#1B1B1F"/>'
            '<g fill="#8E8E96">{body}</g></g></svg>').format(s=size, u=u, body=BODY)


def card(i, cand):
    bgk, fgk, cn = cand
    bg, fg = M3[bgk], M3[fgk]
    hero = cand is HERO
    sizes = ''.join('<span class="sz" style="width:%dpx;height:%dpx">%s</span>'
                    % (s, s, svg(bg, fg, s, 'c', 'z%d%d' % (i, s))) for s in (48, 32, 24))
    cr = contrast(bg, fg)
    return ('<div class="card{h}"><div class="badge">{b}</div>'
            '<div class="pair"><span class="ic">{a}</span><span class="ic">{c}</span></div>'
            '<div class="nm">{cn}</div>'
            '<div class="roles"><span>{bk}</span><i>/</i><span>{fk}</span></div>'
            '<div class="strip"><i style="background:{bg};flex:1"></i><b></b>'
            '<i style="background:{fg};flex:1"></i></div>'
            '<div class="hex"><span><em style="background:{bg}"></em>{bgu}</span>'
            '<span><em style="background:{fg}"></em>{fgu}</span></div>'
            '<div class="cr">对比度 {cr} · {lvl}</div><div class="sizes">{sz}</div></div>').format(
        h=' hero-card' if hero else '', b='默认' if hero else '',
        a=svg(bg, fg, 126, 'c', 'a%d' % i), c=svg(bg, fg, 126, 's', 'b%d' % i),
        cn=cn, bk=bgk, fk=fgk, bg=bg, fg=fg, bgu=bg.upper(), fgu=fg.upper(),
        cr='%.2f:1' % cr, lvl='AAA' if cr >= 7 else 'AA', sz=sizes)


FAKE = ['#8B5000', '#2E6B4F', '#4F5B92', '#8E4585', '#00696D']
hb, hf = M3[HERO[0]], M3[HERO[1]]
CR = contrast(hb, hf)


def launcher(wall, dark):
    txt = '#2A2622' if not dark else '#E8E4DC'
    items = '<div class="li"><span class="lf">%s</span><i>DocBox</i></div>' % svg(hb, hf, 56, 's', 'L' + ('d' if dark else 'w'))
    for i, c in enumerate(FAKE):
        items += '<div class="li"><span class="lf" style="background:%s"></span><i>其他 %d</i></div>' % (c, i + 1)
    return '<div class="wall" style="background:%s;color:%s">%s</div>' % (wall, txt, items)


# ── 图解 1：Apple 1024 网格 ────────────────────────────────────────────
APPLE_FIG = '''<svg viewBox="-6 -6 1036 1036" width="248" height="248" style="display:block">
<rect width="1024" height="1024" fill="#16161C" stroke="#2E2E3A" stroke-width="6"/>
<g stroke="#23232D" stroke-width="2">
<path d="M102,0V1024M205,0V1024M307,0V1024M410,0V1024M512,0V1024M614,0V1024M717,0V1024M819,0V1024M922,0V1024"/>
<path d="M0,102H1024M0,205H1024M0,307H1024M0,410H1024M0,512H1024M0,614H1024M0,717H1024M0,819H1024M0,922H1024"/>
</g>
<rect x="100" y="100" width="824" height="824" rx="150" fill="rgba(103,80,164,.26)"/>
<rect x="100" y="100" width="824" height="824" rx="150" fill="none" stroke="#D0BCFF" stroke-width="8" stroke-dasharray="26 18"/>
<text x="512" y="486" text-anchor="middle" font-family="ui-monospace,monospace" font-size="104" font-weight="600" fill="#E9DDFF">824</text>
<text x="512" y="600" text-anchor="middle" font-family="ui-monospace,monospace" font-size="60" fill="#9C8FC4">80.47%</text>
<text x="512" y="66" text-anchor="middle" font-family="ui-monospace,monospace" font-size="52" fill="#FFB4A9">100 px</text>
<text x="50" y="512" text-anchor="middle" font-family="ui-monospace,monospace" font-size="52" fill="#FFB4A9" transform="rotate(-90 50 512)">100 px</text>
</svg>'''

# ── 图解 2：本图标的 108dp 画布 ────────────────────────────────────────
GEO_FIG = '''<svg viewBox="-14 0 136 108" width="286" height="227" style="display:block">
<rect x="0" y="0" width="108" height="108" fill="#FEF7FF"/>
<rect x="18" y="18" width="72" height="72" fill="#F3EDF7"/>
<g fill="#6750A4">''' + BODY + '''</g>
<circle cx="54" cy="54" r="36" fill="none" stroke="#B3261E" stroke-width=".5"/>
<circle cx="54" cy="54" r="33" fill="none" stroke="#0061A4" stroke-width=".5" stroke-dasharray="1.5 1.5"/>
<rect x="32.33" y="27" width="43.34" height="54" fill="none" stroke="#49454F" stroke-width=".3" stroke-dasharray="1 1.2"/>
<path d="M32.33,20V27M75.67,20V27M32.33,81V88M75.67,81V88" stroke="#49454F" stroke-width=".3"/>
<path d="M25,27H32.33M25,81H32.33M75.67,27H83M75.67,81H83" stroke="#49454F" stroke-width=".3"/>
<text x="54" y="13.6" text-anchor="middle" font-family="ui-monospace,monospace" font-size="4.2" fill="#49454F">9.00</text>
<text x="54" y="95.6" text-anchor="middle" font-family="ui-monospace,monospace" font-size="4.2" fill="#49454F">9.00</text>
<text x="14.4" y="55.6" text-anchor="middle" font-family="ui-monospace,monospace" font-size="4.2" fill="#49454F" transform="rotate(-90 14.4 55.6)">14.33</text>
<text x="93.6" y="55.6" text-anchor="middle" font-family="ui-monospace,monospace" font-size="4.2" fill="#49454F" transform="rotate(-90 93.6 55.6)">14.33</text>
</svg>'''

FACT_ROWS = [
    ('Apple 主体', '824 / 1024', '80.47%', '四边各留 100px = 9.77%'),
    ('圆内切上限', '58.8 dp', '81.7%', '再大就切到 r36 可见圆'),
    ('安全圆上限', '53.9 dp', '74.9%', 'OEM 蒙版更圆时也不切'),
    ('本稿', '54.0 dp', '75.0%', '最远角点 33.05dp ≈ r33'),
]

html = '''<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>匣 · DocBox 图标 · 居中与留白</title>
<style>
:root{--bg:#0A0A0D;--surf:#14141A;--line:#24242E;--tx:#EAE5DB;--tx2:#9C968C;--tx3:#6E6862;
 --acc:#D0BCFF;--acc2:#6750A4}
*{margin:0;padding:0;box-sizing:border-box}
body{background:var(--bg);color:var(--tx);padding:52px 40px 68px;
 font:14px/1.7 "Inter",-apple-system,"SF Pro Text","Segoe UI","PingFang SC","Microsoft YaHei",sans-serif;
 -webkit-font-smoothing:antialiased}
.wrap{max-width:1150px;margin:0 auto}
.eyebrow{font-size:11px;letter-spacing:2.6px;text-transform:uppercase;color:var(--tx3);margin-bottom:14px}
h1{font-size:31px;font-weight:600;letter-spacing:-.5px;line-height:1.2;margin-bottom:12px}
h1 small{font-weight:400;font-size:14px;letter-spacing:0;color:var(--tx2);margin-left:14px}
.lede{color:var(--tx2);font-size:13.5px;max-width:80ch;margin-bottom:6px}
.lede b{color:var(--tx);font-weight:600}
.lede .k,.k{font-family:ui-monospace,"SF Mono",Menlo,Consolas,monospace;font-size:11.5px}
.lede .k{color:var(--acc)}
.k{background:#22222C;border:1px solid #33333F;border-radius:5px;padding:1px 6px;color:#C7C2D2}

.hero{display:grid;grid-template-columns:auto 1fr;gap:52px;align-items:center;
 background:var(--surf);border:1px solid var(--line);border-radius:26px;padding:40px 46px;margin-top:32px}
.hero-ico{display:flex;gap:26px;filter:drop-shadow(0 22px 44px rgba(0,0,0,.62))}
.hero-t{font-size:11px;letter-spacing:1.8px;text-transform:uppercase;color:var(--tx3);margin-bottom:10px}
.hero h2{font-size:22px;font-weight:600;margin-bottom:6px;letter-spacing:-.2px}
.hero .role{font-size:12px;color:var(--acc);font-family:ui-monospace,"SF Mono",Menlo,monospace;margin-bottom:14px}
.hero p{font-size:13px;color:var(--tx2);line-height:1.8;margin-bottom:9px;max-width:56ch}
.hero p b{color:var(--tx);font-weight:600}
.facts{display:flex;gap:26px;margin-top:18px;flex-wrap:wrap}
.fact b{display:block;font-size:19px;font-weight:600;color:var(--tx);letter-spacing:-.3px}
.fact span{font-size:10.5px;letter-spacing:1.1px;text-transform:uppercase;color:var(--tx3)}

h2.sec{font-size:16px;font-weight:600;margin:0 0 4px}
.h2sub{color:var(--tx2);font-size:12.5px;margin-bottom:20px;max-width:80ch}
hr{border:0;height:1px;background:var(--line);margin:46px 0 32px}

.geo{display:grid;grid-template-columns:248px 286px 1fr;gap:38px;align-items:start;
 background:var(--surf);border:1px solid var(--line);border-radius:22px;padding:30px 34px}
.geo figure{display:flex;flex-direction:column;gap:12px;align-items:center}
.geo figcaption{font-size:10.5px;letter-spacing:1px;text-transform:uppercase;color:var(--tx3);text-align:center}
.geo .txt h3{font-size:13px;font-weight:600;margin-bottom:9px}
.geo .txt p{font-size:12.5px;color:var(--tx2);line-height:1.8;margin-bottom:9px}
.geo .txt p b{color:var(--tx)}
.legend{display:flex;gap:16px;margin-top:2px;font-size:10.5px;color:var(--tx3)}
.legend span{display:flex;align-items:center;gap:6px}
.legend i{width:14px;height:0;border-top:2px solid}

table{width:100%;border-collapse:collapse;margin-top:26px;font-size:12.5px}
th,td{text-align:left;padding:9px 12px;border-bottom:1px solid #1E1E26}
th{font-size:10.5px;letter-spacing:1.2px;text-transform:uppercase;color:var(--tx3);font-weight:600}
td:first-child{color:var(--tx2)}
td:nth-child(2),td:nth-child(3){font-family:ui-monospace,"SF Mono",Menlo,monospace;font-size:11.5px}
tr.pick td{color:var(--tx);background:rgba(208,188,255,.06)}
tr.pick td:first-child{color:var(--acc);font-weight:600}

.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:30px}
.card{position:relative;background:var(--surf);border:1px solid var(--line);border-radius:22px;
 padding:24px 20px 18px;display:flex;flex-direction:column;align-items:center}
.hero-card{border-color:#4A3F6B;box-shadow:0 0 0 1px rgba(208,188,255,.14),0 18px 44px -22px rgba(208,188,255,.4)}
.badge{position:absolute;top:14px;right:16px;font-size:10px;letter-spacing:1.4px;
 color:var(--acc);border:1px solid rgba(208,188,255,.35);border-radius:999px;padding:2px 9px;background:rgba(208,188,255,.08)}
.pair{display:flex;gap:14px;filter:drop-shadow(0 8px 18px rgba(0,0,0,.55))}
.ic{display:block;line-height:0}
.nm{margin-top:16px;font-size:14px;font-weight:600;text-align:center}
.roles{margin-top:3px;font-size:10.5px;color:var(--tx3);font-family:ui-monospace,"SF Mono",Menlo,monospace;display:flex;gap:5px}
.roles i{font-style:normal;opacity:.5}
.strip{display:flex;width:100%;height:9px;border-radius:5px;overflow:hidden;margin-top:14px}
.strip i{display:block}.strip b{width:6px;background:var(--surf)}
.hex{display:flex;gap:14px;margin-top:11px;font-family:ui-monospace,"SF Mono",Menlo,monospace;font-size:10px;color:var(--tx2)}
.hex span{display:flex;align-items:center;gap:5px}
.hex em{width:9px;height:9px;border-radius:3px;display:block;box-shadow:0 0 0 1px rgba(255,255,255,.14)}
.cr{margin-top:8px;font-size:10px;letter-spacing:.6px;color:var(--tx3);text-transform:uppercase}
.sizes{display:flex;gap:12px;align-items:center;margin-top:15px}
.sz{display:block;line-height:0;border-radius:50%;overflow:hidden}

.walls{display:grid;grid-template-columns:1fr 1fr;gap:18px}
.wall{border-radius:20px;padding:26px 22px;display:flex;gap:20px;align-items:flex-start;justify-content:center;
 border:1px solid rgba(255,255,255,.07)}
.li{display:flex;flex-direction:column;align-items:center;gap:8px;width:62px}
.lf{width:56px;height:56px;border-radius:15px;display:block;box-shadow:0 6px 16px rgba(0,0,0,.3);overflow:hidden;line-height:0}
.li i{font-style:normal;font-size:9.5px;opacity:.8}

.ladder{display:flex;align-items:flex-end;gap:22px;background:var(--surf);border:1px solid var(--line);
 border-radius:20px;padding:26px 30px;flex-wrap:wrap}
.lad{display:flex;flex-direction:column;align-items:center;gap:10px}
.lad span{font-size:10px;color:var(--tx3);letter-spacing:.8px}
.lad .r{border-radius:50%;overflow:hidden;line-height:0;display:block}
.lad .q{border-radius:18px;overflow:hidden;line-height:0;display:block}

.mono-row{display:flex;gap:16px;align-items:flex-end}
.mb{display:flex;flex-direction:column;align-items:center;gap:9px;background:#1A1A1F;
 border:1px solid var(--line);border-radius:16px;padding:16px 18px;font-size:10px;color:var(--tx3)}

.notes{display:grid;grid-template-columns:1fr 1fr;gap:18px}
.note{background:var(--surf);border:1px solid var(--line);border-radius:16px;padding:22px 24px}
.note h3{font-size:11px;letter-spacing:1.5px;text-transform:uppercase;color:var(--tx3);margin-bottom:11px;font-weight:600}
.note p{font-size:12.5px;color:var(--tx2);line-height:1.78;margin-bottom:9px}
.note p:last-child{margin-bottom:0}
.note b{color:var(--tx);font-weight:600}
footer{margin-top:36px;font-size:11px;color:#56514B;line-height:1.95}
</style></head>
<body><div class="wrap">

<div class="eyebrow">DocBox · 应用图标 · 定稿</div>
<h1>匣<small>篆书原稿直描 · MD3 纯色 · 留白按 Apple 网格</small></h1>
<p class="lede">上一版按<b>墨迹质心</b>做视觉居中，结果四边留白是 17.5 / 14.3 / 12.5 / 9.5 dp —— 左边是下边的近两倍，
所以整个字看起来往右下掉。这一版改回<b>包络框（bbox）精确居中</b>，留白按 Apple 的 1024 网格重新定。</p>

<div class="hero">
 <div class="hero-ico">__ICON_C____ICON_S__</div>
 <div>
  <div class="hero-t">默认 · 已落地</div>
  <h2>主色 / 主色上的内容</h2>
  <div class="role">colorScheme.primary &nbsp;+&nbsp; colorScheme.onPrimary</div>
  <p>字形 <b>43.34 × 54.00 dp</b>，bbox 中心精确落在画布中心 <b>(54.00, 54.00)</b>；
  留白左右各 <b>14.33dp</b>、上下各 <b>9.00dp</b>。</p>
  <p>上下比左右小，不是没对齐——字形本身是 43×54 的竖长方形，
  要四边绝对值相等就得把字缩小一圈，左右反而更空。</p>
  <div class="facts">
   <div class="fact"><b>54<small style="font-size:11px;color:#6E6862"> dp</small></b><span>字形高</span></div>
   <div class="fact"><b>75<small style="font-size:11px;color:#6E6862">%</small></b><span>占可见区</span></div>
   <div class="fact"><b>33.05<small style="font-size:11px;color:#6E6862"> dp</small></b><span>最远点 / 限 33</span></div>
   <div class="fact"><b>__CR__<small style="font-size:11px;color:#6E6862">:1</small></b><span>对比度</span></div>
  </div>
 </div>
</div>

<hr>
<h2 class="sec">留白是算出来的</h2>
<div class="h2sub">左边是 Apple 的 1024 网格原样，右边是本图标在 108dp 画布上的实际位置。两者不是同一个坐标系，
所以不能直接把 100px 搬过来。</div>

<div class="geo">
 <figure>__APPLE_FIG__<figcaption>Apple · 1024 画布 / 824 主体</figcaption></figure>
 <figure>__GEO_FIG__
  <div class="legend">
   <span><i style="border-color:#B3261E"></i>可见圆 r36</span>
   <span><i style="border-color:#0061A4;border-top-style:dashed"></i>安全圆 r33</span>
  </div>
 </figure>
 <div class="txt">
  <h3>Apple 的 9.77% 不能直接搬</h3>
  <p>Apple 的 824 网格是在<b>圆角方形</b>蒙版下量的：主体四边各让 100px，占 80.47%。
  但 Android 主流是<b>圆形</b>蒙版，圆的对角线比方短——这个 0.80 宽高比的竖长方形
  撑到 80.47%（57.9dp）时，四角离圆边只剩 <b>0.5dp</b>，等于贴边。</p>
  <p>所以改成按<b>圆</b>反算：让字形最远角点正好压在 66dp 安全圆（r33）上，
  得到 <b>54.0dp</b>，占可见区 75.0%。</p>
  <p>这样无论 OEM 把蒙版做成圆、方圆角还是别的，<b>四个角都不会被切</b>，
  同时又把圆用满——留白观感和 Apple 是同一个量级。</p>
 </div>
</div>

<table>
<tr><th>口径</th><th>字形高</th><th>占可见区</th><th>说明</th></tr>
__ROWS__
</table>

<hr>
<h2 class="sec">放在桌面上</h2>
<div class="h2sub">左边浅色壁纸、右边深色壁纸。实色图标自带底，两种环境都不需要改。</div>
<div class="walls">__WALL_L____WALL_D__</div>

<hr>
<h2 class="sec">尺寸阶梯</h2>
<div class="h2sub">512 到 24dp。纯色块在小尺寸下比渐变稳：24dp 时笔画约 0.87px，但底色与字色差 6.4:1，不靠细节也能认出轮廓。</div>
<div class="ladder">
 <div class="lad"><span class="q" style="width:200px;height:200px">__L200__</span><span>200</span></div>
 <div class="lad"><span class="q" style="width:120px;height:120px">__L120__</span><span>120</span></div>
 <div class="lad"><span class="r" style="width:72px;height:72px">__L72__</span><span>72</span></div>
 <div class="lad"><span class="r" style="width:48px;height:48px">__L48__</span><span>48</span></div>
 <div class="lad"><span class="r" style="width:36px;height:36px">__L36__</span><span>36</span></div>
 <div class="lad"><span class="r" style="width:24px;height:24px">__L24__</span><span>24</span></div>
</div>

<hr>
<h2 class="sec">单色层</h2>
<div class="h2sub">Android 13+ 主题化图标会把这一层按 alpha 重新染色，所以这里只留形状、不留配色。</div>
<div class="mono-row">__MONO__</div>

<hr>
<h2 class="sec">MD3 角色配对</h2>
<div class="h2sub">MD3 只允许 X 底配 onX 字。下面五对全部合法，换一套只要改两行色值。</div>
<div class="grid">__CARDS__</div>

<hr>
<h2 class="sec">两处取舍</h2>
<div class="notes">
 <div class="note">
  <h3>居中：按包络框，不按墨迹质心</h3>
  <p>这个字左竖粗、底横长，墨迹质心天然偏左上。按质心居中会把整个字往右下推，
  <b>外框看上去就是歪的</b>——「匣」有明确的边框，眼睛认的是那圈边，不是墨的面积。</p>
  <p>所以对齐改用 bbox：中心 (54.00, 54.00)，与画布中心零误差。</p>
 </div>
 <div class="note">
  <h3>纯色为什么比渐变稳</h3>
  <p>自适应图标会被裁成圆、方圆角、圆角方、全出血。线性渐变一旦被裁，光就歪向一侧，
  同一张图在不同启动器里长得不一样。</p>
  <p>背景 <span class="k">primary</span>、字形 <span class="k">onPrimary</span>，
  两个文件里各只有一个 <span class="k">fillColor</span>，没有任何 <span class="k">gradient</span>。</p>
 </div>
</div>

<footer>
几何：108dp 画布 · 可见圆 r=36 · 安全圆 r=33 · 字形 43.34 × 54.00dp · bbox 居中 (54.00, 54.00) · 最远角点 33.05dp<br>
留白：左右 14.33dp / 上下 9.00dp；对照 Apple 1024 网格（主体 824 = 80.47%，边距 9.77%）按圆形蒙版反算<br>
配色：Material 3 基线色板（Material Theme Builder，seed #6750A4，TonalSpot）· 仅 primary / onPrimary · 零渐变<br>
落地：<span class="k">res/drawable/ic_launcher_background.xml</span> ·
<span class="k">ic_launcher_foreground.xml</span> · <span class="k">ic_launcher_monochrome.xml</span> ·
矢量源 <span class="k">icon-samples/svg-md3/</span>
</footer>
</div></body></html>'''

rows = ''
for name, hgt, pct, note in FACT_ROWS:
    cls = ' class="pick"' if name == '本稿' else ''
    rows += '<tr%s><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>' % (cls, name, hgt, pct, note)

mono = ''.join('<div class="mb"><span class="r" style="width:%dpx;height:%dpx;border-radius:50%%;'
               'overflow:hidden;line-height:0;display:block">%s</span><span>%d dp</span></div>'
               % (s, s, mono_svg(s, 'mm'), s) for s in (72, 48, 32, 24))

repl = {
    '__ICON_C__': svg(hb, hf, 214, 'c', 'h'),
    '__ICON_S__': svg(hb, hf, 214, 's', 'h'),
    '__APPLE_FIG__': APPLE_FIG,
    '__GEO_FIG__': GEO_FIG,
    '__ROWS__': rows,
    '__CR__': '%.1f' % CR,
    '__WALL_L__': launcher('#E9E3D7', False),
    '__WALL_D__': launcher('#17171C', True),
    '__L200__': svg(hb, hf, 200, 's', 'd'),
    '__L120__': svg(hb, hf, 120, 's', 'd'),
    '__L72__': svg(hb, hf, 72, 'c', 'd'),
    '__L48__': svg(hb, hf, 48, 'c', 'd'),
    '__L36__': svg(hb, hf, 36, 'c', 'd'),
    '__L24__': svg(hb, hf, 24, 'c', 'd'),
    '__MONO__': mono,
    '__CARDS__': ''.join(card(i, c) for i, c in enumerate(CANDS)),
}
for k, v in repl.items():
    html = html.replace(k, v)

open('md3.html', 'w').write(html)
print('ok', len(html))
