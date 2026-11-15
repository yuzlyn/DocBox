import json, re

F = json.load(open('_final.json'))
path = ''.join(F['paths'])

# 路径太长，按 L 命令切行，方便阅读与 diff（PathParser 忽略空白）
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
one_line = path

RES = 'D:/android_project/MDEditor4/app/src/main/res/drawable/'

BG = '''<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher background — 莫奈《睡莲》(1899) 取色：水光青 → 深青绿 → 橄榄黑 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="4"
                android:startY="2"
                android:endX="72"
                android:endY="108">
                <item android:offset="0" android:color="#6FA3AB" />
                <item android:offset="0.5" android:color="#4E7A76" />
                <item android:offset="1" android:color="#2A3A36" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
'''

FG = '''<?xml version="1.0" encoding="utf-8"?>
<!-- DocBox launcher foreground — 篆书「匣」原稿直描（278 顶点填充轮廓，非描边）
     几何：108dp 画布 · 字形 40.1 x 50dp 居中（中心 54,54）· 最远角点距 32.1dp < 可见圆 r=36
     配色：沙金 → 米白（同取自《睡莲》#F3D7AA / #EEE6DC） -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="%s">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="30"
                android:startY="20"
                android:endX="80"
                android:endY="70">
                <item android:offset="0" android:color="#F6DFB4" />
                <item android:offset="1" android:color="#FFFAF0" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''' % pathxml

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
print('written, path lines:', len(lines), 'path chars:', len(one_line))
