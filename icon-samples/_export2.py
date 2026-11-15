from PIL import Image, ImageFilter
import numpy as np, json, math, os
from collections import deque

SRC = r'D:/qq/qq_data/Tencent Files\2586779183\nt_qq\nt_data\Pic\2026-11\Ori\268d4d968cd77e494bfea43dc321ac2f.png'
SS, CANVAS, EPS = 2, 108.0, 1.5

def dil3(m):
    d = m.copy()
    d[1:,:]  |= m[:-1,:];  d[:-1,:] |= m[1:,:]
    d[:,1:]  |= m[:,:-1];  d[:,:-1] |= m[:,1:]
    d[1:,1:] |= m[:-1,:-1]; d[1:,:-1] |= m[:-1,1:]
    d[:-1,1:]|= m[1:,:-1];  d[:-1,:-1]|= m[1:,1:]
    return d

N8=[(1,0),(1,1),(0,1),(-1,1),(-1,0),(-1,-1),(0,-1),(1,-1)]
DIRS=[(-1,0),(-1,1),(0,1),(1,1),(1,0),(1,-1),(0,-1),(-1,-1)]

def build(h_target, bold, thresh=170, dx=0.0, dy=0.0):
    im = Image.open(SRC).convert('L')
    im = im.resize((im.width*SS, im.height*SS), Image.LANCZOS).filter(ImageFilter.MedianFilter(3))
    mask = np.array(im) < thresh
    # 去毛刺：开运算
    e = mask.copy()
    e[1:,:] &= mask[:-1,:]; e[:-1,:] &= mask[1:,:]
    e[:,1:] &= mask[:,:-1]; e[:,:-1] &= mask[:,1:]
    mask = e
    for _ in range(bold): mask = dil3(mask)
    H, W = mask.shape

    def label(m):
        lab=np.zeros((H,W),np.int32); cur=0; sizes={}
        for y in range(H):
            row=m[y]
            for x in range(W):
                if row[x] and lab[y,x]==0:
                    cur+=1; q=deque([(y,x)]); lab[y,x]=cur; n=0
                    while q:
                        cy,cx=q.popleft(); n+=1
                        for dy,dx in N8:
                            ny,nx=cy+dy,cx+dx
                            if 0<=ny<H and 0<=nx<W and m[ny,nx] and lab[ny,nx]==0:
                                lab[ny,nx]=cur; q.append((ny,nx))
                    sizes[cur]=n
        return lab,sizes

    def trace(start, ib, reg):
        p=start; b=ib; pts=[p]
        for _ in range(3000000):
            d=-1
            for k in range(8):
                dd=(b+k)%8; q=(p[0]+DIRS[dd][0], p[1]+DIRS[dd][1])
                if 0<=q[0]<H and 0<=q[1]<W and reg[q]: d=dd; break
            if d<0: break
            b=(d-1)%8; p=(p[0]+DIRS[d][0], p[1]+DIRS[d][1])
            if p==start and len(pts)>3: break
            pts.append(p)
        return pts

    def rdp(pts, eps):
        if len(pts)<3: return pts
        st=[(0,len(pts)-1)]; kp=[False]*len(pts); kp[0]=kp[-1]=True
        while st:
            i,j=st.pop()
            if j<=i+1: continue
            ax,ay=pts[i]; bx,by=pts[j]; dx,dy=bx-ax,by-ay
            L=math.hypot(dx,dy) or 1e-9; best=-1; bi=-1
            for k in range(i+1,j):
                px,py=pts[k]; dist=abs(dx*(ay-py)-(ax-px)*dy)/L
                if dist>best: best,bi=dist,k
            if best>eps: kp[bi]=True; st.append((i,bi)); st.append((bi,j))
        return [p for k,p in enumerate(pts) if kp[k]]

    lab,sizes = label(mask)
    ids = sorted([k for k,v in sizes.items() if v>2000], key=lambda k:-sizes[k])
    polys=[]
    for k in ids:
        reg=(lab==k); ys,xs=np.nonzero(reg)
        y0=int(ys.min()); x0=int(xs[ys==y0].min())
        polys.append(rdp(trace((y0,x0),6,reg), EPS))

    allp=[p for s in polys for p in s]
    ys=[p[0] for p in allp]; xs=[p[1] for p in allp]
    x0,x1,y0,y1=min(xs),max(xs),min(ys),max(ys)
    scale = h_target/(y1-y0)
    ox = CANVAS/2 - (x1-x0)*scale/2 - x0*scale + dx
    oy = CANVAS/2 - (y1-y0)*scale/2 - y0*scale + dy
    def f(v):
        s=f'{v:.2f}'.rstrip('0').rstrip('.'); return s if s else '0'
    paths=[]
    for s in polys:
        c=[(p[1]*scale+ox, p[0]*scale+oy) for p in s]
        d='M'+f(c[0][0])+','+f(c[0][1])+''.join('L'+f(q[0])+','+f(q[1]) for q in c[1:])+'Z'
        paths.append(d)
    return dict(h=h_target, bold=bold, scale=scale,
                w=(x1-x0)*scale, pts=sum(len(s) for s in polys), paths=paths)

if __name__ == '__main__':
    out={}
    for h in (56, 60):
        for b in (0, 4, 8, 12):
            r = build(h, b)
            out[f'{h}_{b}'] = r
            print(f'h={h} bold={b}: w={r["w"]:.1f} pts={r["pts"]} chars={sum(len(p) for p in r["paths"])}')
    json.dump(out, open('_variants.json','w'))
