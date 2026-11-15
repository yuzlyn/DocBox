# -*- coding: utf-8 -*-
"""把字形写成 MD3 纯色 VectorDrawable（零渐变）"""
import json, re

F = json.load(open('_final.json'))
path = ''.join(F['paths'])


def wrap(d, per=110):
    parts = re.split(r'(?=L)', d)
    lines, cur = [], ''
    for p in parts:
        if len(cur) + len(p) > per and cur:
            lines.append(cur)
            cur = p
        else:
            cur += p
    if cur:
        lines.append(cur)
    return lines


lines = wrap(path)
pathxml = lines[0] + '\n' + '\n'.join('        ' + l for l in lines[1:])

# M3 基线角色（Material Theme Builder, seed #6750A4 / TonalSpot）
PRIMARY = '#6750A4'
ON_PRIMARY = '#FFFFFF'

RES = 'D:/android_project/MDEditor4/app/src/main/res/drawable/'

BG = '''<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher background — MD3 纯色：colorScheme.primary #6750A4
     无渐变，自适应图标在任意遮罩下裁切结果一致。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="%s"
        android:pathData="M0,0h108v108h-108z" />
</vector>
''' % PRIMARY

FG = '''<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher foreground — 篆书「匣」原稿直描（278 顶点填充轮廓，非描边）
     几何：108dp 画布 · 字形 43.34 x 54.00dp · bbox 居中 (54,54) · 最远角点 33.05dp
     留白：左右 14.33dp / 上下 9.00dp（对照 Apple 1024/824 网格按圆形蒙版反算）
     配色：MD3 纯色 colorScheme.onPrimary #FFFFFF，无渐变。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="%s"
        android:pathData="%s" />
</vector>
''' % (ON_PRIMARY, pathxml)

MONO = '''<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher monochrome — Android 13+ 主题化图标。系统按 alpha 重新着色，故只留形状。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#000000"
        android:pathData="%s" />
</vector>
''' % pathxml

open(RES + 'ic_launcher_background.xml', 'w', encoding='utf-8').write(BG)
open(RES + 'ic_launcher_foreground.xml', 'w', encoding='utf-8').write(FG)
open(RES + 'ic_launcher_monochrome.xml', 'w', encoding='utf-8').write(MONO)
print('written | path lines:', len(lines), '| path chars:', len(path))
