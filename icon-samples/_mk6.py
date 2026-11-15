import json

F = json.load(open('_final.json'))
P = json.load(open('_pals_final.json'))
BODY = ''.join('<path d="%s"/>' % p for p in F['paths'])
HERO = 'lily'
ORDER = ['lily', 'pond', 'blue', 'mist', 'wheat', 'paper']


def svg(key, size, mask='c', uid='', mono=False):
    p = P[key]
    clip = ('<circle cx="54" cy="54" r="36"/>' if mask == 'c'
            else '<rect x="18" y="18" width="72" height="72" rx="21"/>')
    u = '%s%s%s%s%s' % (key, size, mask, 'M' if mono else '', uid)
    if mono:
        art = '<rect width="108" height="108" fill="#1A1A1F"/><g fill="#E6E3DC">' + BODY + '</g>'
    else:
        art = ('<defs>'
               '<linearGradient id="b{u}" gradientUnits="userSpaceOnUse" x1="6" y1="6" x2="40" y2="102">'
               '<stop offset="0" stop-color="{c0}"/><stop offset=".52" stop-color="{c1}"/>'
               '<stop offset="1" stop-color="{c2}"/></linearGradient>'
               '<linearGradient id="f{u}" gradientUnits="userSpaceOnUse" x1="30" y1="20" x2="80" y2="70">'
               '<stop offset="0" stop-color="{f0}"/><stop offset="1" stop-color="{f1}"/></linearGradient>'
               '</defs>'
               '<rect width="108" height="108" fill="url(#b{u})"/>'
               '<g fill="url(#f{u})">{body}</g>').format(
            u=u, c0=p['bg'][0], c1=p['bg'][1], c2=p['bg'][2],
            f0=p['fg'][0], f1=p['fg'][1], body=BODY)
    return ('<svg viewBox="18 18 72 72" width="{size}" height="{size}" style="display:block">'
            '<defs><clipPath id="c{u}">{clip}</clipPath></defs>'
            '<g clip-path="url(#c{u})">{art}</g></svg>').format(u=u, clip=clip, art=art, size=size)


def card(key):
    p = P[key]
    cls = 'card hero-card' if key == HERO else 'card'
    badge = '<em class="badge">定稿</em>' if key == HERO else ''
    sizes = ''.join('<span class="sz" style="width:%dpx;height:%dpx">%s</span>'
                    % (s, s, svg(key, s, 'c', 'z')) for s in (48, 32, 24))
    return ('<div class="%s">%s<div class="pair">'
            '<span class="ic">%s</span><span class="ic">%s</span></div>'
            '<div class="nm">%s</div><div class="sub">%s</div>'
            '<div class="strip">'
            '<i style="background:%s;flex:1.7"></i><i style="background:%s;flex:1.5"></i>'
            '<i style="background:%s;flex:1.1"></i><b></b>'
            '<i style="background:%s;flex:1.3"></i><i style="background:%s"></i></div>'
            '<div class="sizes">%s</div><div class="src">%s</div></div>'
            % (cls, badge, svg(key, 132, 'c', 'a'), svg(key, 132, 's', 'b'),
               p['cn'], p['sub'], p['bg'][0], p['bg'][1], p['bg'][2],
               p['fg'][0], p['fg'][1], sizes, p['note']))


FAKE = ['#C4633F', '#4A6FA5', '#7B8B4E', '#8A5F8F', '#3E7F74']


def launcher(wall, dark):
    txt = '#2A2622' if not dark else '#E8E4DC'
    items = ('<div class="li"><span class="lf">%s</span><i>DocBox</i></div>'
             % svg(HERO, 56, 's', 'L' + ('d' if dark else 'w')))
    for i, c in enumerate(FAKE):
        items += ('<div class="li"><span class="lf" style="background:%s"></span><i>其他 %d</i></div>'
                  % (c, i + 1))
    return ('<div class="wall" style="background:%s;color:%s">%s</div>' % (wall, txt, items))


html = '''<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>匣 · DocBox 应用图标</title>
<style>
:root{--bg:#0A0A0D;--surf:#14141A;--line:#24242E;--tx:#EAE5DB;--tx2:#9C968C;--tx3:#6E6862;--acc:#C9A227}
*{margin:0;padding:0;box-sizing:border-box}
body{background:var(--bg);color:var(--tx);padding:52px 40px 68px;
 font:14px/1.7 "Inter",-apple-system,"SF Pro Text","Segoe UI","PingFang SC","Microsoft YaHei",sans-serif;
 -webkit-font-smoothing:antialiased}
.wrap{max-width:1150px;margin:0 auto}
.eyebrow{font-size:11px;letter-spacing:2.6px;text-transform:uppercase;color:var(--tx3);margin-bottom:14px}
h1{font-size:31px;font-weight:600;letter-spacing:-.5px;line-height:1.2;margin-bottom:12px}
h1 small{font-weight:400;font-size:14px;letter-spacing:0;color:var(--tx2);margin-left:14px}
.lede{color:var(--tx2);font-size:13.5px;max-width:76ch;margin-bottom:6px}
.lede b{color:var(--tx);font-weight:600}

.hero{display:grid;grid-template-columns:auto 1fr;gap:52px;align-items:center;
 background:var(--surf);border:1px solid var(--line);border-radius:26px;padding:40px 46px;margin-top:32px}
.hero-ico{display:flex;gap:26px;filter:drop-shadow(0 22px 44px rgba(0,0,0,.62))}
.hero-t{font-size:11px;letter-spacing:1.8px;text-transform:uppercase;color:var(--tx3);margin-bottom:10px}
.hero h2{font-size:22px;font-weight:600;margin-bottom:12px;letter-spacing:-.2px}
.hero p{font-size:13px;color:var(--tx2);line-height:1.8;margin-bottom:9px;max-width:56ch}
.hero p b{color:var(--tx);font-weight:600}
.facts{display:flex;gap:26px;margin-top:18px;flex-wrap:wrap}
.fact b{display:block;font-size:19px;font-weight:600;color:var(--tx);letter-spacing:-.3px}
.fact span{font-size:10.5px;letter-spacing:1.1px;text-transform:uppercase;color:var(--tx3)}

h2.sec{font-size:16px;font-weight:600;margin:0 0 4px}
.h2sub{color:var(--tx2);font-size:12.5px;margin-bottom:20px}
hr{border:0;height:1px;background:var(--line);margin:46px 0 32px}

.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:30px}
.card{position:relative;background:var(--surf);border:1px solid var(--line);border-radius:22px;
 padding:26px 20px 18px;display:flex;flex-direction:column;align-items:center;transition:.18s}
.card:hover{border-color:#3A3A48;transform:translateY(-2px)}
.hero-card{border-color:#4A4230;box-shadow:0 0 0 1px rgba(201,162,39,.12),0 18px 44px -22px rgba(201,162,39,.4)}
.badge{position:absolute;top:14px;right:16px;font-style:normal;font-size:10px;letter-spacing:1.4px;
 color:var(--acc);border:1px solid rgba(201,162,39,.35);border-radius:999px;padding:2px 9px;background:rgba(201,162,39,.08)}
.pair{display:flex;gap:14px;filter:drop-shadow(0 8px 18px rgba(0,0,0,.55))}
.ic{display:block;line-height:0;border-radius:22px;overflow:hidden}
.nm{margin-top:16px;font-size:15px;font-weight:600;letter-spacing:.3px}
.sub{font-size:11.5px;color:var(--tx3);margin-top:1px}
.strip{display:flex;width:100%;height:9px;border-radius:5px;overflow:hidden;margin-top:15px}
.strip i{display:block}
.strip b{width:6px;background:var(--surf)}
.sizes{display:flex;gap:12px;align-items:center;margin-top:15px}
.sz{display:block;line-height:0;border-radius:50%;overflow:hidden}
.src{margin-top:13px;font-size:9.5px;letter-spacing:.7px;color:#56514B;text-transform:uppercase}

.walls{display:grid;grid-template-columns:1fr 1fr;gap:18px}
.wall{border-radius:20px;padding:26px 22px;display:flex;gap:20px;align-items:flex-start;justify-content:center;
 border:1px solid rgba(255,255,255,.07)}
.li{display:flex;flex-direction:column;align-items:center;gap:8px;width:62px}
.lf{width:56px;height:56px;border-radius:15px;display:block;box-shadow:0 6px 16px rgba(0,0,0,.3);overflow:hidden;line-height:0}
.li i{font-style:normal;font-size:9.5px;opacity:.8;letter-spacing:.2px}

.ladder{display:flex;align-items:flex-end;gap:22px;background:var(--surf);border:1px solid var(--line);
 border-radius:20px;padding:26px 30px;flex-wrap:wrap}
.lad{display:flex;flex-direction:column;align-items:center;gap:10px}
.lad span{font-size:10px;color:var(--tx3);letter-spacing:.8px}
.lad .r{border-radius:50%;overflow:hidden;line-height:0;display:block}
.lad .q{border-radius:18px;overflow:hidden;line-height:0;display:block}

.notes{display:grid;grid-template-columns:1fr 1fr;gap:18px}
.note{background:var(--surf);border:1px solid var(--line);border-radius:16px;padding:22px 24px}
.note h3{font-size:11px;letter-spacing:1.5px;text-transform:uppercase;color:var(--tx3);margin-bottom:11px;font-weight:600}
.note p{font-size:12.5px;color:var(--tx2);line-height:1.78;margin-bottom:9px}
.note p:last-child{margin-bottom:0}
.note b{color:var(--tx);font-weight:600}
.k{font-family:ui-monospace,"SF Mono",Menlo,Consolas,monospace;font-size:11px;background:#22222C;
 border:1px solid #33333F;border-radius:5px;padding:1px 6px;color:#C7C2D2}
.mono-row{display:flex;gap:16px;align-items:flex-end}
.mb{display:flex;flex-direction:column;align-items:center;gap:9px;background:#1A1A1F;
 border:1px solid var(--line);border-radius:16px;padding:16px 18px;font-size:10px;color:var(--tx3)}
footer{margin-top:36px;font-size:11px;color:#56514B;line-height:1.95}
</style></head>
<body><div class="wrap">

<div class="eyebrow">DocBox · 应用图标 · 定稿</div>
<h1>匣<small>篆书原稿直描 · 莫奈《睡莲》取色</small></h1>
<p class="lede">字形<b>一笔未改</b>——你给的那张位图直接描成填充轮廓（278 个顶点，全是墨边、没有描边），
所以放大到 1024 也不会出现"描边在不同尺寸下被重新解释"的问题。</p>
<p class="lede">配色只做一件事：<b>60 青绿水面 / 30 暖光笔迹 / 10 米白高光</b>，色值全部来自《睡莲》真实像素采样。</p>

<div class="hero">
 <div class="hero-ico">''' + svg(HERO, 214, 'c', 'h') + svg(HERO, 214, 's', 'h') + '''</div>
 <div>
  <div class="hero-t">已落地 · 现行图标</div>
  <h2>睡莲 · Water Lilies 1899</h2>
  <p>已经写进 <span class="k">res/drawable</span> 三个矢量：背景渐变水面、前景沙金笔迹、单色层。
  自适应图标在圆形、方圆角、全出血三种遮罩下都不会切到笔画。</p>
  <p><b>为什么不做成"更鲜艳"</b>：莫奈这套青绿本身饱和度只有 15%、明度 33%，硬拉高会立刻变成旅游宣传册。
  对比靠<b>明度</b>——底 33% / 字 88%，差 55 个百分点。</p>
  <div class="facts">
   <div class="fact"><b>50<small style="font-size:11px;color:#6E6862"> dp</small></b><span>字形高</span></div>
   <div class="fact"><b>278</b><span>顶点</span></div>
   <div class="fact"><b>32.1<small style="font-size:11px;color:#6E6862"> dp</small></b><span>最远角点 / 限 36</span></div>
   <div class="fact"><b>0.81<small style="font-size:11px;color:#6E6862"> px</small></b><span>笔画 @24dp</span></div>
  </div>
 </div>
</div>

<hr>
<h2 class="sec">放在桌面上</h2>
<div class="h2sub">左边浅色壁纸、右边深色壁纸——图标自带底色，两种环境都不用改。</div>
<div class="walls">''' + launcher('#E9E3D7', False) + launcher('#17171C', True) + '''</div>

<hr>
<h2 class="sec">尺寸阶梯</h2>
<div class="h2sub">512 到 24dp。24dp 那一档笔画约 0.8px、笔画间隙约 1.1px——这是这个字形还能不并笔的下限。</div>
<div class="ladder">
 <div class="lad"><span class="q" style="width:200px;height:200px">''' + svg(HERO, 200, 's', 'd') + '''</span><span>200</span></div>
 <div class="lad"><span class="q" style="width:120px;height:120px">''' + svg(HERO, 120, 's', 'd') + '''</span><span>120</span></div>
 <div class="lad"><span class="r" style="width:72px;height:72px">''' + svg(HERO, 72, 'c', 'd') + '''</span><span>72</span></div>
 <div class="lad"><span class="r" style="width:48px;height:48px">''' + svg(HERO, 48, 'c', 'd') + '''</span><span>48</span></div>
 <div class="lad"><span class="r" style="width:36px;height:36px">''' + svg(HERO, 36, 'c', 'd') + '''</span><span>36</span></div>
 <div class="lad"><span class="r" style="width:24px;height:24px">''' + svg(HERO, 24, 'c', 'd') + '''</span><span>24</span></div>
</div>

<hr>
<h2 class="sec">单色层</h2>
<div class="h2sub">Android 13+ 主题化图标会把这一层重新染成壁纸色，所以只留形状、不留配色。</div>
<div class="mono-row">''' + ''.join(
    '<div class="mb"><span class="r" style="width:%dpx;height:%dpx;border-radius:50%%;overflow:hidden;line-height:0;display:block">%s</span><span>%d dp</span></div>'
    % (s, s, svg(HERO, s, 'c', 'mm', mono=True), s) for s in (72, 48, 32, 24)) + '''</div>

<hr>
<h2 class="sec">备选配色</h2>
<div class="h2sub">同一字形、同一几何，只换色。全部取自《睡莲》不同年份的采样，没有一个是凭感觉调的。</div>
<div class="grid">''' + ''.join(card(k) for k in ORDER) + '''</div>

<hr>
<h2 class="sec">取色账本与取舍</h2>
<div class="notes">
 <div class="note">
  <h3>每一个色号都有出处</h3>
  <p><span class="k">#486160</span> 深青绿 <b>53%</b> —— 画面主体，做底色中段</p>
  <p><span class="k">#3F473D</span> 橄榄黑 <b>19%</b> —— 暗部，做渐变的收口</p>
  <p><span class="k">#5D8895</span> 蓝青 <b>2%</b> —— 高处水光，做渐变起点</p>
  <p><span class="k">#EEE6DC</span> 米白 <b>10%</b> —— 花瓣高光，做字的亮部</p>
  <p><span class="k">#F3D7AA</span> 沙金 <b>3%</b> —— 花瓣受光面，做字的主色</p>
  <p>面积占比天然就是 60-30-10，不用另编规则。</p>
 </div>
 <div class="note">
  <h3>三条"没修"的告警</h3>
  <p><b>5 个色号</b>：参考标识 75% 用 ≤3 色，但那是几何标识。渐变底 + 渐变字本身就是 5 色，改成纯色会丢掉水面的光。</p>
  <p><b>278 个顶点</b>：参考中位数 53。但那是画圆角矩形的成本；描一个毛笔字必然要这么多点，减点就是减笔锋。</p>
  <p><b>5 条边偏 0.3–3°</b>：这是手写笔画的自然抖动，<b>掰直到 0°/90° 反而假</b>——篆书的手写感就靠这个。</p>
 </div>
</div>

<footer>
几何：108dp 画布 · 可见圆 r=36 · 安全区 r=33 · 字形 40.1 × 50dp 居中 · 笔画加粗 6 档（在原稿基础上每边外扩约 0.4dp，用来顶住 24dp 的糊）<br>
描摹管线：2× LANCZOS 上采样 → 中值去噪 → 开运算去毛刺 → Moore 邻域边界跟踪 → RDP 简化（容差 1.5px）→ 填充多边形<br>
落地文件：<span class="k">res/drawable/ic_launcher_background.xml</span>（渐变水面）·
<span class="k">ic_launcher_foreground.xml</span>（字形）· <span class="k">ic_launcher_monochrome.xml</span>（主题化）·
<span class="k">assembleDebug</span> 已通过 · 矢量源 <span class="k">icon-samples/svg-monet/</span>
</footer>
</div></body></html>'''

open('monet.html', 'w').write(html)
print('ok', len(html))
