package com.yuzlyn.mdeditor.data.book

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * EPUB 解析。
 *
 * EPUB 就是一個 ZIP：`META-INF/container.xml` 指出 OPF，OPF 的 `manifest` 列出所有檔案、
 * `spine` 決定閱讀順序，每一章是一份 XHTML。
 *
 * 這裡只做「取出原始標記 + 解析路徑」，**不轉 Markdown、不改寫內容**——
 * 排版交給 WebView，書自己的 CSS 才有機會生效。這也是為什麼能讀得跟原書一樣。
 *
 * 記憶體策略：用 [ZipFile] 隨機讀取，一次只解一章；不把整包讀成 byte array。
 * （舊版把整本書讀進記憶體再逐章轉字串，兩三百 MB 的圖文書必然 OOM。）
 */
class EpubBook private constructor(
    private val file: File,
    private val zip: ZipFile
) : Book {

    private val entries: Map<String, ZipEntry> = linkedMapOf<String, ZipEntry>().apply {
        val list = zip.entries()
        while (list.hasMoreElements()) {
            val entry = list.nextElement()
            if (entry.isDirectory) continue
            put(normalize(entry.name), entry)
        }
    }

    private val opfPath: String = run {
        val container = readText("META-INF/container.xml")
            ?: throw BookException(BookError.NOT_EPUB, "META-INF/container.xml")
        val rootfile = Regex("""(?is)<rootfile\b[^>]*>""").find(container)?.value
            ?: throw BookException(BookError.EPUB_NO_ROOTFILE)
        normalize(
            Xml.attr(rootfile, "full-path")
                ?: throw BookException(BookError.EPUB_NO_ROOTFILE)
        )
    }

    private val opfDir: String = opfPath.substringBeforeLast('/', "")

    private val opf: String = readText(opfPath)
        ?: throw BookException(BookError.EPUB_OPF_UNREADABLE, opfPath)

    /** manifest：id → 項目。 */
    private val manifest = linkedMapOf<String, ManifestItem>()

    /** spine 裡的正文，索引就是閱讀序號。 */
    private val spine = mutableListOf<ManifestItem>()

    /** 正文路徑 → 章節序號。 */
    private val chapterIndex: Map<String, Int>

    override val metadata: BookMetadata
    override val toc: List<TocEntry>
    override val readingDirection: String
    override val chapterCount: Int get() = spine.size

    private val coverPath: String?

    init {
        parseManifest()
        parseSpine()
        if (spine.isEmpty()) throw BookException(BookError.NO_CONTENT)
        chapterIndex = spine.withIndex().associate { (index, item) -> item.path to index }
        metadata = parseMetadata()
        readingDirection = parseDirection()
        coverPath = findCoverPath()
        toc = runCatching { parseToc() }.getOrDefault(emptyList()).ifEmpty { fallbackToc() }
    }

    override suspend fun readChapter(index: Int): ChapterContent {
        val item = spine.getOrNull(index) ?: throw BookException(BookError.CHAPTER_NOT_FOUND, index.toString())
        val bytes = read(item.path) ?: throw BookException(BookError.CHAPTER_UNREADABLE, item.path)
        return ChapterContent(Xml.decode(bytes), item.path.substringBeforeLast('/', ""))
    }

    override fun resolveHref(basePath: String, href: String): String? {
        val clean = href.trim()
        if (clean.isEmpty() || clean.startsWith("#")) return null
        if (isExternal(clean) || clean.startsWith("data:")) return null
        val path = resolve(basePath, clean.substringBefore('#'))
        if (path.isEmpty() || !entries.containsKey(path)) return null
        return path
    }

    override fun chapterOfResource(key: String): Int? = chapterIndex[key]

    override suspend fun readResource(key: String): BookResource? {
        val bytes = read(key) ?: return null
        val declared = manifest.values.firstOrNull { it.path == key }?.mime
        val mime = declared?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: sniffMime(bytes)
            ?: mimeOfPath(key)
        if (mime == "text/css") {
            // CSS 裡也會引用圖片與其他樣式表，不改寫的話背景圖全是空白。
            val base = key.substringBeforeLast('/', "")
            val css = CssRewriter.rewrite(Xml.decode(bytes)) { resolveHref(base, it) }
            return BookResource(css.toByteArray(Charsets.UTF_8), mime)
        }
        return BookResource(bytes, mime)
    }

    override suspend fun cover(): ByteArray? {
        val path = coverPath ?: return null
        val bytes = read(path) ?: return null
        if (path.endsWith(".xhtml", true) || path.endsWith(".html", true) || path.endsWith(".htm", true)) {
            // 封面常常是一張「只有一張圖」的 XHTML，真正的圖在 img 裡。
            val html = Xml.decode(bytes)
            val src = COVER_IMG.find(html)?.groupValues?.get(1) ?: return null
            return read(resolve(path.substringBeforeLast('/', ""), src.substringBefore('#')))
        }
        return bytes
    }

    override fun close() {
        runCatching { zip.close() }
    }

    // ------------------------------------------------------------------ 解析

    private fun parseManifest() {
        Regex("""(?is)<item\b[^>]*>""").findAll(opf).forEach { match ->
            val attrs = Xml.attrs(match.value)
            val id = attrs["id"] ?: return@forEach
            val href = attrs["href"] ?: return@forEach
            val path = resolve(opfDir, href.substringBefore('#'))
            if (path.isEmpty()) return@forEach
            manifest[id] = ManifestItem(
                path = path,
                mime = attrs["media-type"] ?: "",
                properties = attrs["properties"] ?: ""
            )
        }
    }

    private fun parseSpine() {
        Regex("""(?is)<itemref\b[^>]*>""").findAll(opf).forEach { match ->
            val attrs = Xml.attrs(match.value)
            // linear="no" 是註釋、封面這類不進閱讀序列的頁面。
            if (attrs["linear"].equals("no", true)) return@forEach
            val item = manifest[attrs["idref"]] ?: return@forEach
            if (entries.containsKey(item.path)) spine += item
        }
    }

    private fun parseMetadata(): BookMetadata {
        fun dc(name: String): String? {
            val raw = Regex("""(?is)<dc:$name\b[^>]*>(.*?)</dc:$name>""")
                .find(opf)?.groupValues?.get(1) ?: return null
            return Xml.textOf(raw).takeIf { it.isNotEmpty() }
        }
        val title = dc("title")
            ?: Regex("""(?is)<meta\b[^>]*name\s*=\s*["']title["'][^>]*>""")
                .find(opf)?.value?.let { Xml.attr(it, "content") }
                ?: file.nameWithoutExtension
        val creators = Regex("""(?is)<dc:creator\b[^>]*>(.*?)</dc:creator>""")
            .findAll(opf)
            .map { Xml.textOf(it.groupValues[1]) }
            .filter { it.isNotEmpty() }
            .toList()
        return BookMetadata(
            title = title,
            author = creators.joinToString("、").takeIf { it.isNotEmpty() },
            language = dc("language"),
            publisher = dc("publisher"),
            description = dc("description"),
            format = BookFormat.EPUB
        )
    }

    private fun parseDirection(): String =
        if (Regex("""(?i)page-progression-direction\s*=\s*["']rtl["']""").containsMatchIn(opf)) "rtl" else "ltr"

    private fun findCoverPath(): String? {
        Regex("""(?is)<meta\b[^>]*name\s*=\s*["']cover["'][^>]*>""")
            .find(opf)?.value?.let { Xml.attr(it, "content") }
            ?.let { id -> manifest[id]?.path }
            ?.let { return it }
        manifest.values.firstOrNull { "cover-image" in it.properties }?.let { return it.path }
        return manifest.values.firstOrNull {
            it.mime.startsWith("image/") && it.path.contains("cover", true)
        }?.path
    }

    private fun parseToc(): List<TocEntry> {
        // EPUB3 用 nav 文件（manifest 裡 properties 含 nav），EPUB2 用 toc.ncx。
        val navPath = manifest.values.firstOrNull { "nav" in it.properties }?.path
        if (navPath != null) {
            val dir = navPath.substringBeforeLast('/', "")
            parseNavDocument(readText(navPath).orEmpty(), dir).takeIf { it.isNotEmpty() }?.let { return it }
        }
        val ncxPath = Regex("""(?is)<spine\b[^>]*\btoc\s*=\s*["']([^"']+)["']""")
            .find(opf)?.groupValues?.get(1)?.let { id -> manifest[id]?.path }
            ?: manifest.values.firstOrNull { it.mime == "application/x-dtbncx+xml" }?.path
            ?: entries.keys.firstOrNull { it.endsWith(".ncx", true) }
        if (ncxPath != null) {
            val dir = ncxPath.substringBeforeLast('/', "")
            parseNcx(readText(ncxPath).orEmpty(), dir).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    private fun parseNavDocument(html: String, dir: String): List<TocEntry> {
        if (html.isBlank()) return emptyList()
        // 只取 epub:type="toc" 那個 nav；書裡還有 landmarks／page-list，混進來目錄會亂。
        val match = NAV_TOC.find(html) ?: return emptyList()
        // 只取這個 nav 的內容：後面的 landmarks／page-list 也有 <a>，混進來目錄會多出雜項。
        val close = html.indexOf("</nav", match.range.last).takeIf { it > 0 } ?: html.length
        val body = html.substring(match.range.last + 1, close)
        val items = mutableListOf<TocEntry>()
        var depth = 0
        NAV_TAG.findAll(body).forEach { match ->
            val closing = match.groupValues[1].isNotEmpty()
            when (match.groupValues[2].lowercase()) {
                "ol" -> depth = if (closing) (depth - 1).coerceAtLeast(0) else depth + 1
                "a" -> if (!closing) {
                    val href = Xml.attrs(match.value)["href"] ?: return@forEach
                    val textStart = match.range.last + 1
                    val textEnd = body.indexOf("</a", textStart).takeIf { it > 0 } ?: return@forEach
                    val label = Xml.textOf(body.substring(textStart, textEnd))
                    if (label.isEmpty()) return@forEach
                    entryFor(href, dir, label, (depth - 1).coerceAtLeast(0))?.let { items += it }
                }
            }
        }
        return items
    }

    private fun parseNcx(ncx: String, dir: String): List<TocEntry> {
        if (ncx.isBlank()) return emptyList()
        val items = mutableListOf<TocEntry>()
        var depth = 0
        NCX_TAG.findAll(ncx).forEach { match ->
            val closing = match.groupValues[1].isNotEmpty()
            when (match.groupValues[2].lowercase()) {
                "navpoint" -> depth = if (closing) (depth - 1).coerceAtLeast(0) else depth + 1
                "text" -> if (!closing) {
                    val textStart = match.range.last + 1
                    val textEnd = ncx.indexOf("</text", textStart).takeIf { it > 0 } ?: return@forEach
                    val label = Xml.textOf(ncx.substring(textStart, textEnd))
                    // navPoint 的 <content> 一定接在 <navLabel> 之後、子節點之前。
                    val contentTag = Regex("""(?is)<content\b[^>]*>""").find(ncx, textEnd)?.value
                        ?: return@forEach
                    val src = Xml.attr(contentTag, "src") ?: return@forEach
                    entryFor(src, dir, label, (depth - 1).coerceAtLeast(0))?.let { items += it }
                }
            }
        }
        return items
    }

    private fun entryFor(href: String, dir: String, label: String, depth: Int): TocEntry? {
        val path = resolve(dir, href.substringBefore('#'))
        val chapter = chapterIndex[path] ?: return null
        val anchor = href.substringAfter('#', "").takeIf { it.isNotEmpty() }
        return TocEntry(title = label, chapter = chapter, depth = depth, anchor = anchor)
    }

    /** 沒有目錄文件時，至少給章節一個名字，不然目錄是一片空白。 */
    private fun fallbackToc(): List<TocEntry> = spine.mapIndexed { index, item ->
        TocEntry(
            title = item.path.substringAfterLast('/').substringBeforeLast('.'),
            chapter = index
        )
    }

    // ------------------------------------------------------------------ 讀檔

    private fun readText(path: String): String? = read(path)?.let { Xml.decode(it) }

    private fun read(path: String): ByteArray? {
        if (path.isEmpty()) return null
        val entry = entries[path] ?: return null
        return runCatching { zip.getInputStream(entry).use { it.readBytes() } }.getOrNull()
    }

    private class ManifestItem(val path: String, val mime: String, val properties: String)

    companion object {
        private val NAV_TOC =
            Regex("""(?is)<nav\b[^>]*(?:epub:)?type\s*=\s*["'][^"']*\btoc\b[^"']*["'][^>]*>""")
        private val NAV_TAG = Regex("""(?is)<(/?)(ol|a)\b[^>]*>""")
        private val NCX_TAG = Regex("""(?is)<(/?)(navPoint|text)\b[^>]*>""")
        private val COVER_IMG =
            Regex("""(?is)<(?:img|image)[^>]+(?:src|href)\s*=\s*["']([^"']+)["']""")

        /** 檔頭是不是 ZIP。用來在沒有副檔名時決定要不要當 EPUB 試。 */
        fun isZip(file: File): Boolean = runCatching {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val head = ByteArray(2)
                raf.read(head) == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            }
        }.getOrDefault(false)

        fun open(file: File): EpubBook {
            val zip = try {
                ZipFile(file)
            } catch (e: Exception) {
                throw BookException(BookError.PARSE_FAILED, e.message, e)
            }
            return try {
                EpubBook(file, zip)
            } catch (e: Throwable) {
                runCatching { zip.close() }
                if (e is BookException) throw e
                throw BookException(BookError.PARSE_FAILED, e.message, e)
            }
        }
    }
}

/**
 * 路徑工具。書裡的路徑有 `../`、有 `%20`、有開頭的 `/`，
 * 而且 ZIP 內一律用 `/` 分隔，所以集中在一處處理。
 */
internal fun normalize(path: String): String =
    path.replace('\\', '/').removePrefix("./").trimStart('/')

internal fun resolve(baseDir: String, href: String): String {
    val decoded = percentDecode(href.trim())
    val parts = if (baseDir.isEmpty()) mutableListOf() else baseDir.split('/').toMutableList()
    decoded.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts += segment
        }
    }
    return parts.joinToString("/")
}

/** 只還原 `%XX`：不能用 `URLDecoder`，它會把路徑裡的 `+` 變成空格。 */
internal fun percentDecode(text: String): String {
    if (text.indexOf('%') < 0) return text
    val builder = StringBuilder(text.length)
    val bytes = java.io.ByteArrayOutputStream()
    var index = 0
    fun flush() {
        if (bytes.size() > 0) {
            builder.append(String(bytes.toByteArray(), Charsets.UTF_8))
            bytes.reset()
        }
    }
    while (index < text.length) {
        val char = text[index]
        if (char == '%' && index + 2 < text.length) {
            val value = text.substring(index + 1, index + 3).toIntOrNull(16)
            if (value != null) {
                bytes.write(value)
                index += 3
                continue
            }
        }
        flush()
        builder.append(char)
        index++
    }
    flush()
    return builder.toString()
}

/** CSS 裡的 `url(...)` 與 `@import` 也要改寫，否則背景圖、字型全都 404。 */
internal object CssRewriter {

    private val URL = Regex("""url\(\s*(['"]?)([^'")]+)\1\s*\)""", RegexOption.IGNORE_CASE)
    private val IMPORT = Regex("""@import\s+(?:url\(\s*)?(['"])([^'"]+)\1""", RegexOption.IGNORE_CASE)

    fun rewrite(css: String, resolve: (String) -> String?): String {
        var result = URL.replace(css) { match ->
            val href = match.groupValues[2].trim()
            if (href.startsWith("data:") || href.startsWith("http") || href.startsWith("#")) {
                match.value
            } else {
                resolve(href)?.let { "url(\"${ReaderPaths.absoluteResource(it)}\")" } ?: match.value
            }
        }
        result = IMPORT.replace(result) { match ->
            val href = match.groupValues[2].trim()
            val key = if (href.startsWith("data:") || href.startsWith("http")) null else resolve(href)
            if (key != null) "@import \"${ReaderPaths.absoluteResource(key)}\"" else match.value
        }
        return result
    }
}
