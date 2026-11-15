import json

F = json.load(open('_final.json'))
P = json.load(open('_pals_final.json'))
BODY = ''.join('<path d="%s"/>' % p for p in F['paths'])

HEAD = ('<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">'
        '<title>DocBox — 篆书「匣」图标</title>')

for k, p in P.items():
    svg = (HEAD +
           '<defs><clipPath id="c"><circle cx="54" cy="54" r="36"/></clipPath>'
           '<linearGradient id="b" gradientUnits="userSpaceOnUse" x1="6" y1="6" x2="40" y2="102">'
           '<stop offset="0" stop-color="%s"/><stop offset=".52" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient>'
           '<linearGradient id="f" gradientUnits="userSpaceOnUse" x1="30" y1="20" x2="80" y2="70">'
           '<stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient></defs>'
           '<g clip-path="url(#c)"><rect width="108" height="108" fill="url(#b)"/>'
           '<g fill="url(#f)">%s</g></g></svg>') % (
        p['bg'][0], p['bg'][1], p['bg'][2], p['fg'][0], p['fg'][1], BODY)
    open('svg-monet/%s.svg' % k, 'w').write(svg)

full = (HEAD +
        '<defs><linearGradient id="b" gradientUnits="userSpaceOnUse" x1="4" y1="2" x2="72" y2="108">'
        '<stop offset="0" stop-color="#6FA3AB"/><stop offset=".5" stop-color="#4E7A76"/>'
        '<stop offset="1" stop-color="#2A3A36"/></linearGradient>'
        '<linearGradient id="f" gradientUnits="userSpaceOnUse" x1="30" y1="20" x2="80" y2="70">'
        '<stop offset="0" stop-color="#F6DFB4"/><stop offset="1" stop-color="#FFFAF0"/></linearGradient></defs>'
        '<rect width="108" height="108" fill="url(#b)"/><g fill="url(#f)">%s</g></svg>') % BODY
open('svg-monet/final-fullbleed.svg', 'w').write(full)

open('svg-monet/final-monochrome.svg', 'w').write(
    HEAD + '<g fill="#000000">%s</g></svg>' % BODY)

# 纯矢量（透明底），给需要单色的场景
open('svg-monet/final-glyph-only.svg', 'w').write(
    HEAD + '<g fill="#4E7A76">%s</g></svg>' % BODY)
print('ok')
