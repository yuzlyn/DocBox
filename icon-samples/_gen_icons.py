"""为七颗种子色各生成一份背景 vector，供 mipmap 里的 `<background>` 引用。

背景色不是手填的，是 MaterialKolor TonalSpot 从种子派生出的 primary，由 SeedProbeTest 实测输出。
纯色、不用渐变：自适应图标在圆 / 方圆角 / 圆角方 / 全出血各种遮罩下都要被裁，
线性渐变裁完光会歪向一侧，纯色裁切前后一致。

mipmap 本身（字形 × 配色的 438 份）由 `_gen_launcher_icons.py` 生成，不归这里管。
"""
import os

RES = r"D:/android_project/MDEditor4/app/src/main/res"
DRAW = os.path.join(RES, "drawable")
MIP = os.path.join(RES, "mipmap-anydpi-v26")

# seed key -> (种子原文, MaterialKolor 派生的 light primary)
SEEDS = {
    "lavender": ("#6750A4", "#6750A4"),
    "ocean":    ("#00696D", "#00696D"),
    "forest":   ("#2E6B4F", "#2C694D"),
    "clay":     ("#8B5000", "#8B5000"),
    "rose":     ("#8E4585", "#8C4383"),
    "slate":    ("#4F5B92", "#4F5B92"),
    "lotus":    ("#5D8895", "#396471"),
}

BG_TMPL = """<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox 图标背景 · 种子 {seed} → MaterialKolor TonalSpot 派生 primary {primary}
     纯色无渐变：自适应图标在圆 / 方圆角 / 圆角方 / 全出血各种遮罩下，裁切结果都一致。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{primary}"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"""

# application 级的 android:icon / android:roundIcon 用的就是「匣 + LOTUS」这一套，
# 与清单里第一条 enabled="true" 的别名 LauncherXiaLotus 一致。
ICON_TMPL = """<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox 启动器图标 · 默认组合（字形「匣」× 种子 LOTUS），供 application 的 icon / roundIcon 引用。
     其余组合见 mipmap-anydpi-v26/ic_launcher_<字形>_<配色>.xml。 -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg_lotus" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print("write", os.path.relpath(path, RES), len(text))


for key, (seed, primary) in SEEDS.items():
    write(os.path.join(DRAW, "ic_launcher_bg_%s.xml" % key),
          BG_TMPL.format(seed=seed, primary=primary))

for fname in ("ic_launcher.xml", "ic_launcher_round.xml"):
    write(os.path.join(MIP, fname), ICON_TMPL)
