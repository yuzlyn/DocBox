package com.yuzlyn.mdeditor.data.book

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * MOBI / AZW3(KF8) 原生解析。
 *
 * 兩種書其實是同一個容器（PalmDB）的兩個世代：
 * - **MOBI6**：文字記錄直接就是一段 HTML，章節靠 `<mbp:pagebreak>` 切。
 * - **KF8 / AZW3**：文字被拆成「骨架 + 碎片」，骨架是一份殘缺的 XHTML，
 *   碎片按插入位移補進去才是完整章節；索引記在 `FDST` / `skel` / `frag` 三張表裡。
 *
 * 圖片不內嵌在正文，而是獨立記錄，正文用 `kindle:embed:0001` 這種編號引用；
 * 這裡把它換成內部 key，讓上層不必知道編號規則。
 *
 * 全程用 [RandomAccessFile] 隨機讀取，只解真正要看的章節。
 */
class MobiBook private constructor(
    private val file: File,
    private val db: PalmDb
) : Book {

    /** 複合檔（MOBI + KF8）裡 KF8 那半的起始記錄；純 MOBI 是 0。 */
    private val start: Int
    private val compression: Int
    private val textRecordCount: Int
    private val charset: Charset
    private val trailingFlags: Int
    private val resourceStart: Int
    private val huffIndex: Int
    private val huffCount: Int
    private val isKf8: Boolean

    private val exth: Map<Int, List<ByteArray>>
    private val fullName: String?

    private val kf8Fdst: Int
    private val kf8Frag: Int
    private val kf8Skel: Int

    private val sections: List<Section>
    private val fidToChapter: Map<Int, Int>

    /** KF8 的流位移表；`kindle:flow:xxxx` 靠它定位。 */
    private var fdstTable: List<IntArray> = emptyList()

    private var decoder: ((ByteArray) -> ByteArray)? = null
    private var textCache: ByteArray? = null

    override val metadata: BookMetadata
    override val toc: List<TocEntry>
    override val readingDirection: String

    override val chapterCount: Int get() = sections.size

    init {
        val record0 = db.record(0)
        if (record0.size < 132) throw BookException(BookError.FILE_TOO_SMALL)
        compression = Bytes.u16(record0, 0)
        textRecordCount = Bytes.u16(record0, 8)
        val encryption = Bytes.u16(record0, 12)
        if (encryption != 0) {
            throw BookException(BookError.DRM_PROTECTED)
        }
        if (textRecordCount <= 0) throw BookException(BookError.NO_CONTENT)

        charset = charsetOf(Bytes.u32(record0, 28))
        resourceStart = Bytes.u32(record0, 108)
        huffIndex = Bytes.u32(record0, 112)
        huffCount = Bytes.u32(record0, 116)
        trailingFlags = Bytes.u32(record0, 240)
        val indx = Bytes.u32(record0, 244)
        exth = readExth(record0)
        fullName = readFullName(record0)

        // 先假設是純 MOBI；EXTH 帶 boundary 表示後面還有一份 KF8，優先讀 KF8。
        var effectiveStart = 0
        var effectiveVersion = Bytes.u32(record0, 36)
        var kf8Record0 = record0
        val boundary = exthInt(121)
        if (effectiveVersion < 8 && boundary != null && boundary > 0 && boundary < db.recordCount) {
            val candidate = runCatching { db.record(boundary) }.getOrNull()
            if (candidate != null && candidate.size > 132 &&
                String(candidate, 16, 4, Charsets.ISO_8859_1) == "MOBI"
            ) {
                effectiveStart = boundary
                effectiveVersion = Bytes.u32(candidate, 36)
                kf8Record0 = candidate
            }
        }
        start = effectiveStart
        isKf8 = effectiveVersion >= 8
        kf8Fdst = if (isKf8) Bytes.u32(kf8Record0, 192) else -1
        kf8Frag = if (isKf8) Bytes.u32(kf8Record0, 248) else -1
        kf8Skel = if (isKf8) Bytes.u32(kf8Record0, 252) else -1

        if (textRecordCount + start >= db.recordCount) {
            throw BookException(BookError.CORRUPT_STRUCTURE, "text records")
        }
        when (compression) {
            1, 2 -> Unit
            17480 -> if (huffIndex <= 0 || huffCount <= 0) {
                throw BookException(BookError.CORRUPT_STRUCTURE, "HUFF/CDIC")
            }

            else -> throw BookException(BookError.UNSUPPORTED_COMPRESSION, compression.toString())
        }

        if (isKf8) {
            val built = buildKf8Sections()
            sections = built.first
            fidToChapter = built.second
        } else {
            sections = buildMobi6Sections()
            fidToChapter = emptyMap()
        }
        if (sections.isEmpty()) throw BookException(BookError.NO_CONTENT)

        metadata = BookMetadata(
            title = exthString(503) ?: fullName ?: file.nameWithoutExtension,
            author = exthStrings(100).takeIf { it.isNotEmpty() }?.joinToString("、"),
            language = exthString(524) ?: languageTagOf(Bytes.u32(record0, 92)),
            publisher = exthString(101),
            description = exthString(103),
            format = if (isKf8) BookFormat.AZW3 else BookFormat.MOBI
        )
        readingDirection = if (exthString(525)?.equals("rtl", true) == true) "rtl" else "ltr"
        toc = runCatching { buildToc(indx) }.getOrDefault(emptyList()).ifEmpty { fallbackToc() }
    }

    // ------------------------------------------------------------------ 讀取

    override suspend fun readChapter(index: Int): ChapterContent {
        val section = sections.getOrNull(index) ?: throw BookException(BookError.CHAPTER_NOT_FOUND, index.toString())
        val html = readSection(section)
        return ChapterContent(if (isKf8) html else normalizeMobi6(html), "")
    }

    override fun resolveHref(basePath: String, href: String): String? {
        val clean = href.trim()
        if (clean.isEmpty() || clean.startsWith("#")) return null
        if (isExternal(clean) || clean.startsWith("data:")) return null
        if (clean.startsWith(CHAPTER_PREFIX)) return clean
        if (clean.startsWith(EMBED_PREFIX) || clean.startsWith(FLOW_PREFIX)) return clean
        return when {
            clean.startsWith("kindle:embed:") -> {
                val token = clean.removePrefix("kindle:embed:").substringBefore('?')
                val id = token.toIntOrNull(32) ?: return null
                "$EMBED_PREFIX$id"
            }

            clean.startsWith("kindle:flow:") -> {
                val token = clean.removePrefix("kindle:flow:").substringBefore('?')
                val id = token.toIntOrNull(32) ?: return null
                "$FLOW_PREFIX$id"
            }

            clean.startsWith("kindle:pos:") -> {
                val fid = POS_FID.find(clean)?.groupValues?.get(1)?.toIntOrNull(32) ?: return null
                "$CHAPTER_PREFIX${fidToChapter[fid] ?: return null}"
            }

            else -> null
        }
    }

    override fun chapterOfResource(key: String): Int? =
        if (key.startsWith(CHAPTER_PREFIX)) key.removePrefix(CHAPTER_PREFIX).toIntOrNull() else null

    override suspend fun readResource(key: String): BookResource? {
        return when {
            key.startsWith(EMBED_PREFIX) -> {
                // kindle:embed:0001 是 1 基編號，記錄表是 0 基。
                val id = (key.removePrefix(EMBED_PREFIX).toIntOrNull() ?: return null) - 1
                if (id < 0) return null
                val raw = readEmbedded(id) ?: return null
                val (bytes, mime) = decodeFont(raw)
                BookResource(bytes, mime)
            }

            key.startsWith(FLOW_PREFIX) -> {
                val id = key.removePrefix(FLOW_PREFIX).toIntOrNull() ?: return null
                val range = fdstTable.getOrNull(id) ?: return null
                val css = Xml.decode(textAt(range[0], range[1]))
                val rewritten = CssRewriter.rewrite(css) { resolveHref("", it) }
                BookResource(rewritten.toByteArray(Charsets.UTF_8), "text/css")
            }

            else -> null
        }
    }

    override suspend fun cover(): ByteArray? {
        val offset = exthInt(201) ?: return null
        return readEmbedded(offset)
    }

    override fun close() {
        textCache = null
        db.close()
    }

    // ------------------------------------------------------------------ 文字

    private fun decompressor(): (ByteArray) -> ByteArray {
        decoder?.let { return it }
        val created: (ByteArray) -> ByteArray = when (compression) {
            1 -> { data -> data }
            2 -> { data -> PalmDocDecoder.decode(data) }
            else -> {
                val huff = HuffCdicDecoder(db, huffIndex, huffCount)
                val inflate: (ByteArray) -> ByteArray = { data -> huff.decode(data) }
                inflate
            }
        }
        decoder = created
        return created
    }

    /**
     * 把整本書的正文解成一段連續位元組。
     *
     * 為什麼不逐段解：KF8 的骨架位移是「整本書」的絕對位移、會跨記錄取值，
     * 沒有連續緩衝就得反覆重解前綴。正文本身通常只有幾 MB（圖片在別的記錄），
     * 一次解完換來的簡單與速度都值得。
     */
    private fun body(): ByteArray {
        textCache?.let { return it }
        val inflate = decompressor()
        val sink = ByteSink(1 shl 20)
        for (index in 0 until textRecordCount) {
            sink.write(inflate(stripTrailing(db.record(start + index + 1))))
        }
        val bytes = sink.toByteArray()
        textCache = bytes
        return bytes
    }

    /** 文字記錄尾部可能夾著「多媒體索引」這類附加資料，要按旗標剝掉。 */
    private fun stripTrailing(data: ByteArray): ByteArray {
        if (trailingFlags == 0 || data.isEmpty()) return data
        var end = data.size
        val entries = Bytes.popCount(trailingFlags ushr 1)
        repeat(entries) {
            if (end <= 0) return@repeat
            val length = varLenFromEnd(data, end)
            if (length in 1..end) end -= length
        }
        if (trailingFlags and 1 != 0 && end > 0) {
            val length = (data[end - 1].toInt() and 0x03) + 1
            if (length <= end) end -= length
        }
        return if (end == data.size) data else data.copyOf(end)
    }

    /** 從尾端往前讀變長整數（每字元 7 位，最高位標記「這是開頭」）。 */
    private fun varLenFromEnd(data: ByteArray, end: Int): Int {
        var value = 0
        val from = maxOf(0, end - 4)
        for (index in from until end) {
            val byte = data[index].toInt() and 0xFF
            if (byte and 0x80 != 0) value = 0
            value = (value shl 7) or (byte and 0x7F)
        }
        return value
    }

    private fun textAt(offset: Int, length: Int): ByteArray {
        val body = body()
        val from = offset.coerceIn(0, body.size)
        val to = (offset + length).coerceIn(from, body.size)
        return body.copyOfRange(from, to)
    }

    // ------------------------------------------------------------------ MOBI6

    private fun buildMobi6Sections(): List<Section> {
        val body = body()
        val breaks = mutableListOf(0)
        var cursor = 0
        while (cursor < body.size) {
            val at = indexOfAscii(body, PAGEBREAK, cursor)
            if (at < 0) break
            if (at > breaks.last()) breaks += at
            cursor = at + PAGEBREAK.size
        }
        return breaks.mapIndexed { position, begin ->
            val end = breaks.getOrNull(position + 1) ?: body.size
            Section(position, begin, end - begin, emptyList())
        }
    }

    /**
     * MOBI6 的圖片是 `<img recindex="00001">`，改成標準的 `src` 才畫得出來；
     * 內部連結用 `filepos`（整本書的位元組位移），換算成章節跳轉。
     */
    private fun normalizeMobi6(html: String): String {
        var result = RECINDEX.replace(html) { match ->
            val before = match.groupValues[1]
            val after = match.groupValues[3]
            val id = match.groupValues[2].toIntOrNull(32) ?: 0
            "<img$before src=\"$EMBED_PREFIX$id\"$after>"
        }
        result = FILEPOS_ANCHOR.replace(result) { match ->
            val before = match.groupValues[1]
            val after = match.groupValues[3]
            val offset = match.groupValues[2].toIntOrNull() ?: return@replace match.value
            val chapter = sectionOfOffset(offset) ?: return@replace match.value
            "<a$before href=\"$CHAPTER_PREFIX$chapter\"$after>"
        }
        return result
    }

    private fun sectionOfOffset(offset: Int): Int? {
        if (offset < 0) return null
        sections.forEach { section ->
            val end = section.skeletonOffset + section.skeletonLength
            if (offset in section.skeletonOffset until end) return section.index
        }
        return sections.lastIndex.takeIf { sections.isNotEmpty() }
    }

    // ------------------------------------------------------------------ KF8

    /**
     * 拆出 KF8 的章節。
     *
     * `FDST` 給出幾個「流」的位移；`skel` 表列出每個骨架的位移與碎片數，
     * `frag` 表接續給出每一塊碎片要插在哪、長度多少。
     * 章節 = 骨架 + 依序插入的碎片，插入位置是整本書的絕對位移。
     */
    private fun buildKf8Sections(): Pair<List<Section>, Map<Int, Int>> {
        if (kf8Skel <= 0 || kf8Skel >= db.recordCount) {
            throw BookException(BookError.CORRUPT_STRUCTURE, "KF8 skeleton")
        }
        runCatching {
            val fdst = db.record(kf8Fdst)
            if (fdst.size >= 12 && String(fdst, 0, 4, Charsets.ISO_8859_1) == "FDST") {
                val count = Bytes.u32(fdst, 8)
                fdstTable = (0 until count).map { index ->
                    val at = 12 + index * 8
                    intArrayOf(Bytes.u32(fdst, at), Bytes.u32(fdst, at + 4))
                }
            }
        }

        val skelData = IndexReader.read(db, kf8Skel, charset)
        val fragData = IndexReader.read(db, kf8Frag, charset)

        val skeletonSpans = skelData.entries.map { entry ->
            val span = entry.tags[6]
            intArrayOf(
                entry.tags[1]?.firstOrNull() ?: 0,
                span?.getOrNull(0) ?: 0,
                span?.getOrNull(1) ?: 0
            )
        }
        val fragments = fragData.entries.map { entry ->
            val span = entry.tags[6]
            Frag(
                insertOffset = entry.name.trim().toIntOrNull() ?: 0,
                fid = entry.tags[4]?.firstOrNull() ?: -1,
                offset = span?.getOrNull(0) ?: 0,
                length = span?.getOrNull(1) ?: 0
            )
        }

        val result = mutableListOf<Section>()
        val fidMap = mutableMapOf<Int, Int>()
        var cursor = 0
        skeletonSpans.forEach { span ->
            val numFrag = span[0]
            if (numFrag > 0 && span[2] > 0) {
                val slice = fragments.subList(cursor, minOf(fragments.size, cursor + numFrag))
                val index = result.size
                slice.forEach { if (it.fid >= 0) fidMap[it.fid] = index }
                result += Section(index, span[1], span[2], slice.toList())
            }
            cursor += numFrag
        }
        return result to fidMap
    }

    private fun readSection(section: Section): String {
        if (section.fragments.isEmpty()) {
            return String(textAt(section.skeletonOffset, section.skeletonLength), charset)
        }
        var skeleton = textAt(section.skeletonOffset, section.skeletonLength)
        val sink = ByteSink(skeleton.size + 4096)
        for (fragment in section.fragments) {
            val insert = (fragment.insertOffset - section.skeletonOffset).coerceIn(0, skeleton.size)
            sink.write(skeleton, 0, insert)
            sink.write(
                textAt(section.skeletonOffset + section.skeletonLength + fragment.offset, fragment.length)
            )
            sink.write(skeleton, insert, skeleton.size - insert)
            skeleton = sink.toByteArray()
            sink.reset()
        }
        return String(skeleton, charset)
    }

    // ------------------------------------------------------------------ 目錄

    private fun buildToc(indx: Int): List<TocEntry> {
        if (!isKf8 || indx <= 0 || indx >= db.recordCount) return emptyList()
        val data = IndexReader.read(db, indx, charset)
        return data.entries.mapNotNull { entry ->
            val fid = entry.tags[6]?.firstOrNull() ?: return@mapNotNull null
            val chapter = fidToChapter[fid] ?: return@mapNotNull null
            val label = entry.tags[3]?.firstOrNull()?.let { data.cncx[it] }?.trim()
            if (label.isNullOrEmpty()) return@mapNotNull null
            val level = entry.tags[4]?.firstOrNull() ?: 1
            TocEntry(title = label, chapter = chapter, depth = (level - 1).coerceAtLeast(0))
        }
    }

    private fun fallbackToc(): List<TocEntry> = sections.map {
        // 標題留空，交由介面按語言補（「第 N 節」只在中文成立）。
        TocEntry(title = "", chapter = it.index)
    }

    // ------------------------------------------------------------------ 資源

    private fun readEmbedded(index: Int): ByteArray? {
        val record = resourceStart + index
        if (record < 0 || record >= db.recordCount) return null
        return runCatching { db.record(record) }.getOrNull()
    }

    /**
     * KF8 的字型記錄（`FONT`）可能是壓縮的、還可能被 XOR 混淆過，
     * 原樣丟給 WebView 會得到一整片豆腐字。
     */
    private fun decodeFont(raw: ByteArray): Pair<ByteArray, String> {
        if (raw.size < 24 || String(raw, 0, 4, Charsets.ISO_8859_1) != "FONT") {
            return raw to (sniffMime(raw) ?: "image/jpeg")
        }
        val flags = Bytes.u32(raw, 8)
        val dataStart = Bytes.u32(raw, 12)
        val keyLength = Bytes.u32(raw, 16)
        val keyStart = Bytes.u32(raw, 20)
        if (dataStart <= 0 || dataStart >= raw.size) return raw to "application/octet-stream"
        var data = raw.copyOfRange(dataStart, raw.size)
        if (flags and 0x02 != 0 && keyLength in 1..32 && keyStart + keyLength <= raw.size) {
            val key = raw.copyOfRange(keyStart, keyStart + keyLength)
            val limit = minOf(if (keyLength == 16) 1024 else 1040, data.size)
            for (index in 0 until limit) {
                data[index] = (data[index].toInt() xor key[index % key.size].toInt()).toByte()
            }
        }
        if (flags and 0x01 != 0) {
            data = runCatching {
                // 這是 raw deflate，沒有 zlib 包頭，所以 nowrap = true。
                val inflater = java.util.zip.Inflater(true)
                inflater.setInput(data)
                val sink = ByteSink(data.size * 4)
                val buffer = ByteArray(8192)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buffer)
                    if (n <= 0) break
                    sink.write(buffer, 0, n)
                }
                inflater.end()
                sink.toByteArray()
            }.getOrDefault(data)
        }
        val mime = when {
            data.size > 4 && String(data, 0, 4, Charsets.ISO_8859_1) == "OTTO" -> "font/otf"
            data.size > 4 && String(data, 0, 4, Charsets.ISO_8859_1) == "wOFF" -> "font/woff"
            else -> "font/ttf"
        }
        return data to mime
    }

    // ------------------------------------------------------------------ 標頭

    private fun readExth(record0: ByteArray): Map<Int, List<ByteArray>> {
        val flags = Bytes.u32(record0, 128)
        if (flags and 0x40 == 0) return emptyMap()
        val headerLength = Bytes.u32(record0, 20)
        val offset = 16 + headerLength
        if (offset + 12 > record0.size) return emptyMap()
        if (String(record0, offset, 4, Charsets.ISO_8859_1) != "EXTH") return emptyMap()
        val count = Bytes.u32(record0, offset + 8)
        val result = HashMap<Int, MutableList<ByteArray>>()
        var cursor = offset + 12
        for (index in 0 until count) {
            if (cursor + 8 > record0.size) break
            val type = Bytes.u32(record0, cursor)
            val length = Bytes.u32(record0, cursor + 4)
            if (length < 8 || cursor + length > record0.size) break
            result.getOrPut(type) { mutableListOf() }
                .add(record0.copyOfRange(cursor + 8, cursor + length))
            cursor += length
        }
        return result
    }

    private fun exthString(type: Int): String? =
        exth[type]?.firstOrNull()?.let { String(it, charset) }?.trim()?.takeIf { it.isNotEmpty() }

    private fun exthStrings(type: Int): List<String> =
        exth[type].orEmpty().mapNotNull { String(it, charset).trim().takeIf { s -> s.isNotEmpty() } }

    private fun exthInt(type: Int): Int? =
        exth[type]?.firstOrNull()?.takeIf { it.size >= 4 }?.let { Bytes.u32(it, 0) }

    private fun readFullName(record0: ByteArray): String? {
        val offset = Bytes.u32(record0, 84)
        val length = Bytes.u32(record0, 88)
        if (offset <= 0 || length <= 0 || offset + length > record0.size) return null
        return runCatching { String(record0, offset, length, charset).trim() }
            .getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun charsetOf(code: Int): Charset = when (code) {
        65001 -> Charsets.UTF_8
        1252 -> runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)
        936 -> runCatching { Charset.forName("GBK") }.getOrDefault(Charsets.UTF_8)
        950 -> runCatching { Charset.forName("Big5") }.getOrDefault(Charsets.UTF_8)
        932 -> runCatching { Charset.forName("Shift_JIS") }.getOrDefault(Charsets.UTF_8)
        949 -> runCatching { Charset.forName("EUC-KR") }.getOrDefault(Charsets.UTF_8)
        else -> Charsets.UTF_8
    }

    private fun languageTagOf(locale: Int): String? {
        if (locale == 0) return null
        return when (locale and 0xFF) {
            9 -> "en"
            4 -> "zh"
            17 -> "ja"
            18 -> "ko"
            12 -> "fr"
            7 -> "de"
            else -> null
        }
    }

    private fun indexOfAscii(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        var index = from
        val limit = haystack.size - needle.size
        while (index <= limit) {
            var matched = true
            for (k in needle.indices) {
                if (haystack[index + k] != needle[k]) {
                    matched = false
                    break
                }
            }
            if (matched) return index
            index++
        }
        return -1
    }

    private class Section(
        val index: Int,
        val skeletonOffset: Int,
        val skeletonLength: Int,
        val fragments: List<Frag>
    )

    private class Frag(
        val insertOffset: Int,
        val fid: Int,
        val offset: Int,
        val length: Int
    )

    companion object {
        private const val EMBED_PREFIX = "embed:"
        private const val FLOW_PREFIX = "flow:"
        private const val CHAPTER_PREFIX = "chapter:"

        private val PAGEBREAK = "<mbp:pagebreak".toByteArray(Charsets.US_ASCII)
        private val RECINDEX =
            Regex("""<img\b([^>]*?)\brecindex\s*=\s*["']([0-9]+)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        private val FILEPOS_ANCHOR =
            Regex("""<a\b([^>]*?)\bfilepos\s*=\s*["']([0-9]+)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        private val POS_FID = Regex("""fid:(\w+)""")
        fun open(file: File): MobiBook {
            val raf = try {
                RandomAccessFile(file, "r")
            } catch (e: Exception) {
                throw BookException(BookError.OPEN_FAILED, e.message, e)
            }
            val db = try {
                PalmDb(raf)
            } catch (e: Throwable) {
                runCatching { raf.close() }
                if (e is BookException) throw e
                throw BookException(BookError.NOT_MOBI, cause = e)
            }
            return try {
                MobiBook(file, db)
            } catch (e: Throwable) {
                db.close()
                if (e is BookException) throw e
                throw BookException(BookError.PARSE_FAILED, e.message, e)
            }
        }
    }
}
