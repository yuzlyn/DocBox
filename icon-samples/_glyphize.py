"""把单个字形位图转成 Adaptive Icon 前景的 pathData。

和老管线 `_export2.py` 的区别：**支持孔洞**。

`_export2.py` 只追踪每个墨块的**外**轮廓，所以「匣」这种内部笔画与边框分离的字能出效果，
但一旦笔画和边框连在一起（本次多数古文字都是这样），内腔会被填成实心色块，字就废了。

这里的做法是：外轮廓照旧追踪，再把「被墨完全包住的背景块」也当成形状追踪一遍，
两批环放进同一条 path，用 `fillType="evenOdd"` 让内腔自动挖空。
"""

from PIL import Image, ImageFilter
import numpy as np
import math
from collections import deque

DIRS = [(-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]
N8 = DIRS


def otsu(gray: np.ndarray) -> int:
    hist = np.bincount(gray.ravel(), minlength=256).astype(np.float64)
    total = hist.sum()
    sum_all = (np.arange(256) * hist).sum()
    w_b = np.cumsum(hist)
    w_f = total - w_b
    sum_b = np.cumsum(np.arange(256) * hist)
    with np.errstate(invalid="ignore", divide="ignore"):
        m_b = sum_b / w_b
        m_f = (sum_all - sum_b) / w_f
        var = w_b * w_f * (m_b - m_f) ** 2
    return int(np.nanargmax(var))


def dilate(m: np.ndarray) -> np.ndarray:
    d = m.copy()
    d[1:, :] |= m[:-1, :]
    d[:-1, :] |= m[1:, :]
    d[:, 1:] |= m[:, :-1]
    d[:, :-1] |= m[:, 1:]
    d[1:, 1:] |= m[:-1, :-1]
    d[1:, :-1] |= m[:-1, 1:]
    d[:-1, 1:] |= m[1:, :-1]
    d[:-1, :-1] |= m[1:, 1:]
    return d


def erode(m: np.ndarray) -> np.ndarray:
    e = m.copy()
    e[1:, :] &= m[:-1, :]
    e[:-1, :] &= m[1:, :]
    e[:, 1:] &= m[:, :-1]
    e[:, :-1] &= m[:, 1:]
    return e


def rdp(pts, eps):
    if len(pts) < 3:
        return pts
    st = [(0, len(pts) - 1)]
    kp = [False] * len(pts)
    kp[0] = kp[-1] = True
    while st:
        i, j = st.pop()
        if j <= i + 1:
            continue
        ax, ay = pts[i]
        bx, by = pts[j]
        dx, dy = bx - ax, by - ay
        L = math.hypot(dx, dy) or 1e-9
        best, bi = -1, -1
        for k in range(i + 1, j):
            px, py = pts[k]
            dist = abs(dx * (ay - py) - (ax - px) * dy) / L
            if dist > best:
                best, bi = dist, k
        if best > eps:
            kp[bi] = True
            st.append((i, bi))
            st.append((bi, j))
    return [p for k, p in enumerate(pts) if kp[k]]


def _turn_rank(d_in, d_out):
    """出边相对入边的转向优先级：右转 0、直行 1、左转 2、掉头 3。

    y 轴向下，所以叉积为负是视觉上的右转。对角相触的 2×2 里，
    候选出边必然是「右转」与「左转」各一，取右转就把两块各自留在自己的环上，
    等价于 4-连通语义，不会让两笔斜着蹭到一起就缝成一块。
    """
    cross = d_in[0] * d_out[1] - d_in[1] * d_out[0]
    dot = d_in[0] * d_out[0] + d_in[1] * d_out[1]
    if cross < 0:
        return 0
    if cross == 0 and dot > 0:
        return 1
    if cross > 0:
        return 2
    return 3


def _boundary_rings(mask):
    """沿像素边走一遍，取出所有闭合轮廓（外轮廓和内腔都有）。

    比 Moore 邻域跟踪稳：每条有向像素边只属于一个环，有向图入度等于出度，
    分解出来必然是闭合环。Moore 跟踪在 1~2 像素宽的细笔画上会沿两侧来回走，
    走到半路状态重复就停了，留下的开口路径被 `Z` 一闭合就是一条横贯全图的斜线
    —— 「說文竹部」那种密排横线正是这么被毁掉的。
    """
    M = np.pad(mask, 1, constant_values=False)
    h, w = mask.shape
    fg = M[1:h + 1, 1:w + 1]
    nb_up, nb_dn = M[0:h, 1:w + 1], M[2:h + 2, 1:w + 1]
    nb_lf, nb_rt = M[1:h + 1, 0:w], M[1:h + 1, 2:w + 2]

    out = {}

    def add(a, b):
        out.setdefault(a, []).append(b)

    for y, x in zip(*np.nonzero(fg & ~nb_up)):
        add((y + 1, x + 1), (y + 1, x + 2))
    for y, x in zip(*np.nonzero(fg & ~nb_rt)):
        add((y + 1, x + 2), (y + 2, x + 2))
    for y, x in zip(*np.nonzero(fg & ~nb_dn)):
        add((y + 2, x + 2), (y + 2, x + 1))
    for y, x in zip(*np.nonzero(fg & ~nb_lf)):
        add((y + 2, x + 1), (y + 1, x + 1))

    rings = []
    while out:
        v0 = next(iter(out))
        ring = [v0]
        v, d_in = v0, None
        while True:
            nbrs = out.get(v)
            if not nbrs:
                break
            if len(nbrs) == 1 or d_in is None:
                nxt = nbrs[0]
            else:
                nxt = min(nbrs, key=lambda u: _turn_rank(d_in, (u[0] - v[0], u[1] - v[1])))
            nbrs.remove(nxt)
            if not nbrs:
                del out[v]
            d_in = (nxt[0] - v[0], nxt[1] - v[1])
            v = nxt
            if v == v0:
                break
            ring.append(v)
        if len(ring) > 3:
            rings.append([(p[0] - 1, p[1] - 1) for p in ring])
    return rings


def _rings(mask, min_area, ss):
    """提取所有环，丢掉面积过小的噪点，再做 RDP 简化。"""
    rings = []
    for r in _boundary_rings(mask):
        # 鞋带公式：面积太小的当噪点
        a2 = 0
        for i in range(len(r)):
            y1, x1 = r[i]
            y2, x2 = r[(i + 1) % len(r)]
            a2 += x1 * y2 - x2 * y1
        if abs(a2) / 2.0 < min_area:
            continue
        s = rdp(r, 0.72 * ss)
        if len(s) >= 3:
            rings.append(s)
    return rings, 0


def build(src, h_target=54.0, canvas=108.0, ss=4, bold=4, thresh=None,
          min_area=None, safe_r=33.0):
    im = Image.open(src).convert("L")
    im = im.resize((im.width * ss, im.height * ss), Image.LANCZOS)
    im = im.filter(ImageFilter.MedianFilter(3))
    g = np.array(im)

    t = otsu(g) if thresh is None else thresh
    mask = g < t
    mask = erode(mask)
    for _ in range(bold):
        mask = dilate(mask)

    if min_area is None:
        min_area = 6 * ss * ss

    rings, _ = _rings(mask, min_area, ss)

    allp = [p for r in rings for p in r]
    ys = [p[0] for p in allp]
    xs = [p[1] for p in allp]
    y0, y1, x0, x1 = min(ys), max(ys), min(xs), max(xs)

    scale = h_target / (y1 - y0)
    cx = (x0 + x1) / 2 * scale
    cy = (y0 + y1) / 2 * scale
    ox = canvas / 2 - cx
    oy = canvas / 2 - cy

    # 安全圆：自适应图标可见圆 r=36，安全圆 r=33。最远点越界就整体缩回来。
    def mapped(p):
        return (p[1] * scale + ox, p[0] * scale + oy)

    corners = [mapped(p) for p in allp]
    far = max(math.hypot(px - canvas / 2, py - canvas / 2) for px, py in corners)
    shrink = 1.0
    if far > safe_r:
        shrink = safe_r / far
        scale *= shrink
        ox = canvas / 2 - cx * shrink
        oy = canvas / 2 - cy * shrink

    def f(v):
        s = f"{v:.2f}".rstrip("0").rstrip(".")
        return s if s else "0"

    parts = []
    for r in rings:
        c = [mapped(p) for p in r]
        parts.append("M" + f(c[0][0]) + "," + f(c[0][1]) +
                     "".join("L" + f(q[0]) + "," + f(q[1]) for q in c[1:]) + "Z")

    return dict(
        path="".join(parts),
        rings=len(rings),
        pts=sum(len(r) for r in rings),
        w=(x1 - x0) * scale,
        h=(y1 - y0) * scale,
        far=far * shrink,
        shrink=shrink,
        thresh=t,
        bold=bold,
    )
