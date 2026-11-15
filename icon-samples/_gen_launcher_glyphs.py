"""把字形演变图里的五个古文字转成 Adaptive Icon 前景（VectorDrawable）。

几何规则沿用「匣」那版，两条都是硬约束：

1. **bbox 居中**（dx = dy = 0）。有外框的字按墨迹质心居中会偏，因为笔画分布不对称，
   看着就是歪的；包围盒居中才是视觉居中。
2. **字高 54dp / 画布 108dp**。Apple 的 1024 网格主体是 824（80.47%），直接搬会撑到 57.9dp，
   而自适应图标的安全圆只有 r33 —— 那是给「方形主体」的上限；字是竖长的，
   按 r33 反算出来的字高上限就是 54dp。竖长字撑到 54dp 时最远点仍在 33 以内。

加粗档位逐字手定，不是随便挑的：笔画细的（曾仲游父簋的 S 形弯、微伯簋中部的钩）
必须加到 6 次才不断开；「說文竹部」中部有 6 条横线、间距只有 3~4 个原图像素，
加到 5 次就糊成实心横带，只能停在 4。全部以「与原稿结构是否还认得出」为准。

产出全部由本脚本生成，勿手改。
"""
import os
import re

import _glyphize as G

ROOT = r"D:/android_project/MDEditor4"
DRAW = os.path.join(ROOT, "app/src/main/res/drawable")

# (文件名, 出处标签, 加粗次数)
SPECS = [
    ("weibo", "微伯簋（金）", 6),
    ("zengzhong", "曾仲游父簋（金）", 6),
    ("lusitu", "魯大司徒厚氏", 5),
    ("shuowen_gu", "說文古文", 5),
    ("shuowen_zhu", "說文竹部", 4),
]
THRESH = 165

TMPL = """<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher foreground — {label} 原稿直描（{pts} 顶点 / {rings} 条闭合轮廓）
     几何：108dp 画布 · 字形 {w:.2f} x {h:.2f}dp · bbox 居中 (54,54) · 最远点 {far:.2f}dp（安全圆 r33）
     做法：{ss}x 上采样 → 阈值 {thr} → 加粗 {bold} 次 → 像素边轮廓追踪 + RDP 简化
     evenOdd 是必需的：字形有外框和闭口笔画，只取外轮廓会把内腔填成实心块。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:fillType="evenOdd"
        android:pathData="{path}" />
</vector>
"""


def wrap(d, width=96):
    """把 pathData 按命令边界折行，和现有「匣」那版的排版一致。"""
    lines, line = [], ""
    for tok in re.findall(r"[MLZ][^MLZ]*", d):
        if line and len(line) + len(tok) > width:
            lines.append(line)
            line = tok
        else:
            line += tok
    if line:
        lines.append(line)
    return "\n        ".join(lines)


for glyph_id, label, bold in SPECS:
    r = G.build(os.path.join(ROOT, "icon-samples", "glyphs", glyph_id + ".png"),
                bold=bold, thresh=THRESH)
    xml = TMPL.format(label=label, pts=r["pts"], rings=r["rings"],
                      w=r["w"], h=r["h"], far=r["far"], ss=4,
                      thr=r["thresh"], bold=bold, path=wrap(r["path"]))
    dst = os.path.join(DRAW, "ic_launcher_foreground_%s.xml" % glyph_id)
    with open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(xml)
    print("%-12s %-16s ring=%-2d pt=%-4d %5.2fx%5.2f far=%.2f  %d bytes"
          % (glyph_id, label, r["rings"], r["pts"], r["w"], r["h"], r["far"], len(xml)))
