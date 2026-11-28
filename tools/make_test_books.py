#!/usr/bin/env python3
"""生成单元测试用的样本电子书：EPUB / MOBI(PalmDOC) / AZW3(KF8)。

三种格式都是手工拼二进制，目的是把解析器**真正**跑一遍，而不是拿假数据糊弄断言。
AZW3 尤其重要：骨架 + 碎片的组装逻辑（FDST / skel / frag 三张 INDX 表）最容易写错，
必须有真样本验证。
"""
import os
import struct
import zipfile
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "test", "resources", "books")


def png():
    """1×1 的透明 PNG。"""
    def chunk(tag, data):
        raw = tag + data
        return struct.pack(">I", len(data)) + raw + struct.pack(">I", zlib.crc32(raw) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)
    idat = zlib.compress(b"\x00\x00\x00\x00\x00")
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


# ---------------------------------------------------------------- EPUB

CHAPTER1 = """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>第一章</title><link rel="stylesheet" type="text/css" href="../Styles/main.css"/></head>
<body>
  <h1 id="top">第一章</h1>
  <p>這是第一章的正文，含一個<em>強調</em>與一個<a href="ch2.xhtml">連到第二章</a>的連結。</p>
  <h2 id="s1">第一節</h2>
  <p>小節內容。</p>
  <p><img src="../Images/pic.png" alt="圖"/></p>
  <script>alert('evil');</script>
</body></html>"""


def make_epub(path):
    container = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

    opf = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>測試書名</dc:title>
    <dc:creator>測試作者</dc:creator>
    <dc:language>zh-TW</dc:language>
    <dc:publisher>測試出版社</dc:publisher>
    <meta name="cover" content="cover-img"/>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="css" href="Styles/main.css" media-type="text/css"/>
    <item id="c1" href="Text/ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="c2" href="Text/ch2.xhtml" media-type="application/xhtml+xml"/>
    <item id="c3" href="Text/ch3.xhtml" media-type="application/xhtml+xml"/>
    <item id="cover-img" href="Images/pic.png" media-type="image/png"/>
    <item id="note" href="Text/notes.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="c1"/>
    <itemref idref="c2"/>
    <itemref idref="c3"/>
    <itemref idref="note" linear="no"/>
  </spine>
</package>"""

    nav = """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>目錄</title></head>
<body>
<nav epub:type="toc" id="toc">
  <ol>
    <li><a href="Text/ch1.xhtml">第一章</a>
      <ol><li><a href="Text/ch1.xhtml#s1">第一節</a></li></ol>
    </li>
    <li><a href="Text/ch2.xhtml">第二章</a></li>
    <li><a href="Text/ch3.xhtml">第三章</a></li>
  </ol>
</nav>
<nav epub:type="landmarks"><ol><li><a epub:type="bodymatter" href="Text/ch1.xhtml">正文</a></li></ol></nav>
</body></html>"""

    css = """p { text-indent: 2em; color: #111; }
h1 { font-size: 1.5em; }
body { background: #ffffff url(../Images/pic.png) no-repeat; }
"""

    ch2 = """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>第二章</title></head>
<body><h1>第二章</h1><p>第二章正文。</p></body></html>"""

    ch3 = """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>第三章</title></head>
<body><h1>第三章</h1><p>第三章正文。</p></body></html>"""

    notes = """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>註釋</title></head>
<body><p>這頁是非線性內容，不該出現在閱讀序列裡。</p></body></html>"""

    os.makedirs(os.path.dirname(path), exist_ok=True)

    def entry(name, compress):
        """固定时间戳的 zip 条目。"""
        # writestr 收字符串时会把 date_time 设成 time.localtime()，
        # 于是每跑一次脚本，样本二进制就变一次，工作区永远是脏的。
        info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        info.compress_type = compress
        return info

    with zipfile.ZipFile(path, "w") as z:
        # mimetype 必须排第一且不压缩。
        z.writestr(entry("mimetype", zipfile.ZIP_STORED), "application/epub+zip")
        z.writestr(entry("META-INF/container.xml", zipfile.ZIP_DEFLATED), container)
        z.writestr(entry("OEBPS/content.opf", zipfile.ZIP_DEFLATED), opf)
        z.writestr(entry("OEBPS/nav.xhtml", zipfile.ZIP_DEFLATED), nav)
        z.writestr(entry("OEBPS/Styles/main.css", zipfile.ZIP_DEFLATED), css)
        z.writestr(entry("OEBPS/Text/ch1.xhtml", zipfile.ZIP_DEFLATED), CHAPTER1)
        z.writestr(entry("OEBPS/Text/ch2.xhtml", zipfile.ZIP_DEFLATED), ch2)
        z.writestr(entry("OEBPS/Text/ch3.xhtml", zipfile.ZIP_DEFLATED), ch3)
        z.writestr(entry("OEBPS/Text/notes.xhtml", zipfile.ZIP_DEFLATED), notes)
        z.writestr(entry("OEBPS/Images/pic.png", zipfile.ZIP_STORED), png())


# ---------------------------------------------------------------- PalmDB

def pdb(records, name=b"TestBook"):
    """78 字节头 + 记录表（每条 8 字节）+ 记录体。"""
    count = len(records)
    header = bytearray(78)
    header[0:len(name)] = name
    header[60:64] = b"BOOK"
    header[64:68] = b"MOBI"
    struct.pack_into(">H", header, 76, count)

    offset = 78 + 8 * count + 2  # 记录表后面留 2 字节间隙，很多工具这么写
    table = bytearray()
    body = bytearray()
    for index, data in enumerate(records):
        table += struct.pack(">I", offset + len(body))
        table += b"\x00" + index.to_bytes(3, "big")
        body += data
    return bytes(header) + bytes(table) + b"\x00\x00" + bytes(body)


def palmdoc_compress(data):
    """只输出字面量的 PalmDOC 编码（合法但没压缩），用来验证解压路径。"""
    out = bytearray()
    index = 0
    while index < len(data):
        byte = data[index]
        if byte == 0:
            out.append(0x00)
            index += 1
        elif 0x09 <= byte <= 0x7F:
            out.append(byte)
            index += 1
        else:
            run = 0
            while run < 8 and index + run < len(data):
                nxt = data[index + run]
                if 0x09 <= nxt <= 0x7F or nxt == 0:
                    break
                run += 1
            out.append(run)
            out += data[index:index + run]
            index += run
    return bytes(out)


def mobi_record0(text_len, text_records, exth_items, title, version, header_len=232, kf8=None):
    palmdoc = struct.pack(">HHIHHHH", 2, 0, text_len, text_records, 4096, 0, 0)
    mobi = bytearray(header_len)
    mobi[0:4] = b"MOBI"
    struct.pack_into(">I", mobi, 4, header_len)     # header length
    struct.pack_into(">I", mobi, 8, 2)              # mobi type = book
    struct.pack_into(">I", mobi, 12, 65001)         # utf-8
    struct.pack_into(">I", mobi, 16, 12345)         # unique id
    struct.pack_into(">I", mobi, 20, version)       # file version
    struct.pack_into(">I", mobi, 68, 16 + header_len)  # full name offset（相对 record 0）
    struct.pack_into(">I", mobi, 72, len(title.encode("utf-8")))
    struct.pack_into(">I", mobi, 76, 0x0409)        # locale
    struct.pack_into(">I", mobi, 96, 0xFFFFFFFF)    # huff offset
    struct.pack_into(">I", mobi, 100, 0)            # huff count
    struct.pack_into(">I", mobi, 112, 0x40 if exth_items else 0)  # exth flags
    struct.pack_into(">I", mobi, 224, 0)            # extra data flags：不剥尾部
    struct.pack_into(">I", mobi, 228, 0xFFFFFFFF)   # indx（本样本不带 NCX）

    if kf8:
        # 这些字段的偏移都相对 record 0，而 mobi 数组从 record 0 的 16 开始。
        struct.pack_into(">I", mobi, 108 - 16, kf8["resourceStart"])
        struct.pack_into(">I", mobi, 192 - 16, kf8["fdst"])
        struct.pack_into(">I", mobi, 196 - 16, 1)
        struct.pack_into(">I", mobi, 248 - 16, kf8["frag"])
        struct.pack_into(">I", mobi, 252 - 16, kf8["skel"])
        struct.pack_into(">I", mobi, 260 - 16, 0xFFFFFFFF)
    else:
        struct.pack_into(">I", mobi, 108 - 16, 0)   # resourceStart

    exth = bytearray()
    if exth_items:
        exth = bytearray(b"EXTH" + b"\x00" * 8)
        for typ, payload in exth_items:
            exth += struct.pack(">II", typ, 8 + len(payload)) + payload
        struct.pack_into(">I", exth, 4, len(exth))
        struct.pack_into(">I", exth, 8, len(exth_items))

    return bytes(palmdoc) + bytes(mobi) + bytes(exth) + title.encode("utf-8")


MOBI_BODY = ("<html><head><title>样本</title></head><body>"
             "<h1>第一章</h1><p>这是第一章。</p>"
             "<mbp:pagebreak/>"
             "<h1>第二章</h1><p>这是第二章。<img recindex=\"00001\"></p>"
             "<mbp:pagebreak/>"
             "<h1>第三章</h1><p>这是第三章。</p>"
             "</body></html>").encode("utf-8")


def make_mobi(path):
    chunks = [MOBI_BODY[i:i + 4096] for i in range(0, len(MOBI_BODY), 4096)]
    compressed = [palmdoc_compress(chunk) for chunk in chunks]
    exth = [(503, "MOBI 样本".encode("utf-8")), (100, "样本作者".encode("utf-8"))]
    record0 = mobi_record0(len(MOBI_BODY), len(chunks), exth, "MOBI 样本", 6)

    records = [record0] + compressed + [png()]
    data = bytearray(pdb(records, name=b"MobiSample"))
    # first image index 指向最后一条记录
    start = struct.unpack(">I", data[78:82])[0]
    struct.pack_into(">I", data, start + 16 + (108 - 16), len(records) - 1)
    with open(path, "wb") as f:
        f.write(data)


# ---------------------------------------------------------------- INDX

def index_header(header_len, num_records, num_cncx=0):
    head = bytearray(header_len)
    head[0:4] = b"INDX"
    struct.pack_into(">I", head, 4, header_len)
    struct.pack_into(">I", head, 24, num_records)
    struct.pack_into(">I", head, 28, 65001)
    struct.pack_into(">I", head, 52, num_cncx)
    return head


def tagx_block(tags, num_control):
    """tags: [(tag, numValues, mask, endFlag)]"""
    block = bytearray(b"TAGX" + b"\x00" * 8)
    struct.pack_into(">I", block, 4, 12 + 4 * len(tags))
    struct.pack_into(">I", block, 8, num_control)
    for tag, num_values, mask, end in tags:
        block += bytes([tag, num_values, mask, end])
    return bytes(block)


def varlen(value):
    """INDX 的变长整数：7 位一组、大端，最后一个字节最高位为 1。"""
    if value == 0:
        return b"\x80"
    parts = []
    while value:
        parts.append(value & 0x7F)
        value >>= 7
    parts.reverse()
    out = bytearray()
    for index, part in enumerate(parts):
        out.append(part | 0x80 if index == len(parts) - 1 else part)
    return bytes(out)


def split_index(tags, entries):
    """返回 (主记录, 数据记录)。主记录带 TAGX，数据记录带 IDXT 与数据行。"""
    header_len = 192
    # 一个控制字节就够：所有 tag 的 mask 都是 0x01，同一字节同时点亮即可。
    control_bytes = 1
    # 条目布局固定是 [nameLen][name][control...][value...]
    rows = bytearray()
    row_offsets = []
    for name, values in entries:
        row_offsets.append(len(rows))
        encoded = name.encode("utf-8")
        rows += bytes([len(encoded)]) + encoded
        rows += b"\x01" * control_bytes
        for value in values:
            rows += varlen(value)

    main = bytearray(index_header(header_len, 1))
    main += tagx_block(tags, control_bytes)
    main += b"\x00" * (header_len - len(main))

    data = bytearray(index_header(header_len, len(entries)))
    # 数据记录不需要 TAGX，但保留结构对齐，IDXT 紧跟其后
    while len(data) < header_len:
        data += b"\x00"
    while len(data) % 4:
        data += b"\x00"
    idxt_off = len(data)
    struct.pack_into(">I", data, 20, idxt_off)
    data += b"IDXT"
    base = idxt_off + 4 + 2 * len(entries)
    for offset in row_offsets:
        data += struct.pack(">H", base + offset)
    data += rows
    return bytes(main), bytes(data)


def make_azw3(path):
    """KF8：骨架 + 碎片 + FDST + skel/frag 索引。"""
    skel0 = ('<?xml version="1.0" encoding="utf-8"?><html><head><title>第一章</title></head>'
             '<body><!--F0--></body></html>').encode("utf-8")
    frag0 = ('<h1>第一章</h1><p>第一章的内容。</p>'
             '<p><img src="kindle:embed:0001?mime=image/png"/></p>').encode("utf-8")
    skel1 = ('<?xml version="1.0" encoding="utf-8"?><html><head><title>第二章</title></head>'
             '<body><!--F1--></body></html>').encode("utf-8")
    frag1 = "<h1>第二章</h1><p>第二章的内容。</p>".encode("utf-8")

    insert0 = skel0.index(b"<!--F0-->") + len(b"<!--F0-->")
    insert1 = skel1.index(b"<!--F1-->") + len(b"<!--F1-->")

    raw = bytearray()
    raw += skel0
    frag0_off = len(raw) - len(skel0)   # 相对骨架末尾
    raw += frag0
    skel1_off = len(raw)
    raw += skel1
    frag1_off = len(raw) - (skel1_off + len(skel1))
    raw += frag1

    fdst = bytearray(b"FDST" + b"\x00" * 8)
    struct.pack_into(">I", fdst, 4, 12)
    struct.pack_into(">I", fdst, 8, 1)
    fdst += struct.pack(">II", 0, len(raw))

    skel_main, skel_data = split_index(
        [(1, 1, 1, 0), (6, 2, 1, 0)],
        [("skel0", [1, 0, len(skel0)]),
         ("skel1", [1, skel1_off, len(skel1)])]
    )
    frag_main, frag_data = split_index(
        [(2, 1, 1, 0), (4, 1, 1, 0), (6, 2, 1, 0)],
        [(str(insert0), [0, 0, frag0_off, len(frag0)]),
         (str(insert1), [0, 1, frag1_off, len(frag1)])]
    )

    # 记录顺序：0=header 1=text 2=FDST 3=skel主 4=skel数据 5=frag主 6=frag数据 7=资源
    # 封面 EXTH 201 是「相对于首个资源记录」的下标，0 基，所以第一张图写 0。
    exth = [(503, "AZW3 样本".encode("utf-8")),
            (100, "样本作者".encode("utf-8")),
            (201, struct.pack(">I", 0))]
    record0 = mobi_record0(
        len(raw), 1, exth, "AZW3 样本", 8,
        header_len=248,
        kf8={"resourceStart": 7, "fdst": 2, "frag": 5, "skel": 3}
    )
    # 头部声明的 compression=2（PalmDOC），正文记录就必须真的压过，
    # 否则解压器会把明文当压缩流读，解出一堆乱码。
    records = [record0, palmdoc_compress(bytes(raw)), bytes(fdst), skel_main, skel_data,
               frag_main, frag_data, png()]
    with open(path, "wb") as f:
        f.write(pdb(records, name=b"Azw3Sample"))


def main():
    os.makedirs(OUT, exist_ok=True)
    make_epub(os.path.join(OUT, "sample.epub"))
    make_mobi(os.path.join(OUT, "sample.mobi"))
    make_azw3(os.path.join(OUT, "sample.azw3"))
    for name in ("sample.epub", "sample.mobi", "sample.azw3"):
        print(name, os.path.getsize(os.path.join(OUT, name)), "bytes")


if __name__ == "__main__":
    main()
