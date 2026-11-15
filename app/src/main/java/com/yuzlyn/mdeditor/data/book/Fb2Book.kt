package com.yuzlyn.mdeditor.data.book

import java.io.File

/**
 * FB2（FictionBook 2）解析。
 *
 * 和 EPUB 完全不同：FB2 是**單一 XML 檔**，正文與圖片都在裡面，圖片是
 * `<binary>` 節點裡的 base64 字串。所以這本書「開檔即全載」，但一份 FB2 通常只有幾 MB。
 *
 * 章節就是 body 底下的第一層 `<section>`；FB2 沒有 CSS，排版全靠閱讀器注入的樣式表，
 * 這裡只把 `<image>`／`<empty-line>` 這類 FB2 專屬標籤換成人看得懂的 HTML。
 */
class Fb2Book private constructor(private val file: File) : Book {

    private val xml: String
    private val chapters: List<IntRange>
    private val binaries: Map<String, String>

    override val metadata: BookMetadata
    override val toc: List<TocEntry>
    override val readingDirection: String = "ltr"
    override val chapterCount: Int get() = chapters.size

    init {
        val bytes = file.readBytes()
        xml = Xml.decode(bytes)
        if (!xml.contains("FictionBook", ignoreCase = true)) {
            throw BookException(BookError.NOT_FB2)
        }
        // 先把 binary 切出來，正文那一段就不必帶著幾 MB 的 base64 一起被掃描。
        binaries = linkedMapOf<String, String>().apply {
            BINARY.findAll(xml).forEach { match ->
                val id = match.groupValues[1]
                put(id, match.groupValues[2].replace(Regex("""\s+"""), ""))
            }
        }
        chapters = topLevelSections(xml)
        if (chapters.isEmpty()) throw BookException(BookError.NO_CONTENT)

        val info = TITLE_INFO.find(xml)?.value.orEmpty()
        metadata = BookMetadata(
            title = tagText(info, "book-title") ?: file.nameWithoutExtension,
            author = authorOf(info),
            language = tagText(info, "lang"),
            publisher = tagText(xml, "publisher"),
            description = tagText(xml, "annotation"),
            format = BookFormat.FB2
        )
        toc = chapters.mapIndexed { index, range ->
            val section = xml.substring(range.first, range.last + 1)
            val title = TITLE.find(section)?.value?.let { Xml.textOf(it) }
            // 沒有標題就留空：目錄那一列由介面補「第 N 節 / Section N」，
            // 資料層寫死中文的話，英文法文介面就只有這幾列是中文。
            TocEntry(
                title = title.orEmpty(),
                chapter = index
            )
        }
    }

    override suspend fun readChapter(index: Int): ChapterContent {
        val range = chapters.getOrNull(index) ?: throw BookException(BookError.CHAPTER_NOT_FOUND, index.toString())
        var body = xml.substring(range.first, range.last + 1)
        // `<title>` 出現在 body 裡會被 HTML 解析器當成文件的標題，直接換成一般元素最保險。
        body = body.replace(Regex("""(?is)<title\s*>(.*?)</title\s*>"""), "<div class=\"fb2-title\">$1</div>")
        body = body.replace(Regex("""(?is)</?subtitle\s*>"""), "")
            .replace(Regex("""(?is)<subtitle\s*>"""), "<div class=\"fb2-subtitle\">")
        body = body.replace(Regex("""(?is)<image\b[^>]*?(?:l:)?href\s*=\s*["']#([^"']+)["'][^>]*/?>""")) { match ->
            "<img class=\"fb2-image\" src=\"$BIN_PREFIX${match.groupValues[1]}\"/>"
        }
        body = body.replace(Regex("""(?i)<empty-line\s*/?>"""), "<br/>")
        body = body.replace(Regex("""(?i)<p(\s[^>]*)?>"""), "<p class=\"fb2-p\"$1>")
        body = body.replace(Regex("""(?i)<(epigraph|cite)\b([^>]*)>"""), "<div class=\"fb2-epigraph\"$2>")
        body = body.replace(Regex("""(?i)</(epigraph|cite)\s*>"""), "</div>")
        body = body.replace(Regex("""(?i)<poem\b([^>]*)>"""), "<div class=\"fb2-poem\"$1>")
        body = body.replace(Regex("""(?i)</poem\s*>"""), "</div>")
        body = body.replace(Regex("""(?i)<v\b([^>]*)>"""), "<div class=\"fb2-v\"$1>")
        body = body.replace(Regex("""(?i)</v\s*>"""), "</div>")
        body = body.replace(Regex("""(?i)<annotation\b([^>]*)>"""), "<div class=\"fb2-note\"$1>")
        body = body.replace(Regex("""(?i)</annotation\s*>"""), "</div>")
        return ChapterContent("<div class=\"fb2\">$body</div>", "")
    }

    override fun resolveHref(basePath: String, href: String): String? {
        val clean = href.trim()
        if (clean.startsWith(BIN_PREFIX)) return clean
        if (isExternal(clean)) return null
        val id = clean.removePrefix("#")
        return if (binaries.containsKey(id)) "$BIN_PREFIX$id" else null
    }

    override fun chapterOfResource(key: String): Int? = null

    override suspend fun readResource(key: String): BookResource? {
        if (!key.startsWith(BIN_PREFIX)) return null
        val encoded = binaries[key.removePrefix(BIN_PREFIX)] ?: return null
        val bytes = runCatching { ReaderPaths.base64UrlDecode(encoded.replace('+', '-').replace('/', '_')) }
            .getOrNull() ?: return null
        return BookResource(bytes, sniffMime(bytes) ?: mimeOfPath(key))
    }

    override suspend fun cover(): ByteArray? {
        val id = COVER_IMAGE.find(xml)?.groupValues?.get(1) ?: return null
        return readResource("$BIN_PREFIX$id")?.bytes
    }

    override fun close() = Unit

    // ------------------------------------------------------------------ 解析

    /** 找出 body 底下第一層的 `<section>`，回傳每段的字元區間。 */
    private fun topLevelSections(source: String): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var depth = 0
        var start = -1
        SECTION_TAG.findAll(source).forEach { match ->
            val closing = match.groupValues[1].isNotEmpty()
            val selfClosing = match.groupValues[2].endsWith("/")
            if (selfClosing) return@forEach
            if (closing) {
                depth--
                if (depth == 0 && start >= 0) {
                    result += start..match.range.last
                    start = -1
                }
            } else {
                if (depth == 0) start = match.range.first
                depth++
            }
        }
        // XML 不完整（少了收尾標籤）時，把剩下的部分當最後一章。
        if (start >= 0) result += start..(source.length - 1)
        return result
    }

    private fun tagText(source: String, tag: String): String? =
        Regex("""(?is)<$tag\b[^>]*>(.*?)</$tag>""")
            .find(source)?.groupValues?.get(1)
            ?.let { Xml.textOf(it) }
            ?.takeIf { it.isNotEmpty() }

    private fun authorOf(info: String): String? {
        val authors = AUTHOR.findAll(info).map { match ->
            val first = tagText(match.value, "first-name").orEmpty()
            val last = tagText(match.value, "last-name").orEmpty()
            "$first$last".ifEmpty { tagText(match.value, "nickname").orEmpty() }
        }.filter { it.isNotEmpty() }.toList()
        return authors.joinToString("、").takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val BIN_PREFIX = "bin:"

        private val BINARY = Regex("""(?is)<binary\b[^>]*\bid\s*=\s*["']([^"']+)["'][^>]*>(.*?)</binary>""")
        private val SECTION_TAG = Regex("""(?is)<(/?)(section\b[^>]*)>""")
        private val TITLE_INFO = Regex("""(?is)<title-info>.*?</title-info>""")
        private val TITLE = Regex("""(?is)<title>.*?</title>""")
        private val AUTHOR = Regex("""(?is)<author>.*?</author>""")
        private val COVER_IMAGE =
            Regex("""(?is)<coverpage>.*?href\s*=\s*["']#([^"']+)["'].*?</coverpage>""")

        fun open(file: File): Fb2Book = try {
            Fb2Book(file)
        } catch (e: BookException) {
            throw e
        } catch (e: Throwable) {
            throw BookException(BookError.PARSE_FAILED, e.message, e)
        }
    }
}
