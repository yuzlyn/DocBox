import json, sys
sys.path.insert(0, '.')
import _export2 as E

FINAL = E.build(54, 6)   # bbox 精确居中（dx=dy=0）
json.dump(FINAL, open('_final.json', 'w'))
print('final: w=%.2f pts=%d chars=%d' % (FINAL['w'], FINAL['pts'], sum(len(p) for p in FINAL['paths'])))

# ── 莫奈取色（真实画作采样） ──────────────────────────────
# Water Lilies 1899: #486160(53%) #3F473D(19%) #EEE6DC(10%) #85B6D3(8%)
#                    #69A1BF(6%) #F3D7AA(3%) #5D8895(2%) #B8827C #D4A095
# Water Lilies 1916: #5D697A #677068 #5C6791 #475046 #9C96A0 #B0AA93 #965C60 #AFA661
# Water Lilies 1907: #DBC05C #7C734A #53512D #E1CC8E #292615
PALS = {
    'lily': dict(cn='睡莲', sub='青绿水面 · 沙金笔迹', bg=['#6FA3AB', '#47706B', '#2C3A36'], fg=['#F6DFB4', '#FFFAF0'], note='Water Lilies 1899'),
    'pond': dict(cn='深潭', sub='深青 · 米白', bg=['#3E6B6E', '#2A4745', '#1B2725'], fg=['#F3D7AA', '#EEE6DC'], note='Water Lilies 1899 暗调'),
    'blue': dict(cn='蓝睡莲', sub='湖蓝 · 暖白', bg=['#69A1BF', '#4E7E92', '#2F4A58'], fg=['#FFF8EC', '#F3D7AA'], note='Water Lilies 1899 + 1907'),
    'mist': dict(cn='晨雾', sub='灰紫 · 米白', bg=['#5D697A', '#4A5468', '#333A50'], fg=['#EEE6DC', '#C9C2AE'], note='Water Lilies 1916'),
    'wheat':dict(cn='金穗', sub='深橄榄 · 亮金', bg=['#8A7F52', '#53512D', '#292615'], fg=['#F0DC9E', '#DBC05C'], note='Water Lilies 1907'),
    'paper':dict(cn='宣纸', sub='米白底 · 青绿字（浅色）', bg=['#F6E7C8', '#EEE6DC', '#DCCFBB'], fg=['#47706B', '#2C3A36'], note='浅色方向'),
}
json.dump(PALS, open('_pals_final.json', 'w'), ensure_ascii=False)
print('pals', list(PALS))
