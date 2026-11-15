package com.yuzlyn.mdeditor.data.book

import java.util.Locale

/**
 * 翻頁方式：整頁翻動，或連續滾動（可選滾動軸）。
 *
 * 滾動軸之所以要能選，是因為閱讀習慣差異很大：橫排的人多半想往下滑，
 * 直排的人習慣從右往左滑；也有人橫排時偏好左右滑（像看長卷）。
 * 所以不把「滾動方向」綁死在排版方向上，三種都給。
 */
enum class ReaderMode(val id: String) {
    /** 一屏一頁，點左右（直排是左右相反）翻頁。 */
    PAGED("paged"),

    /** 連續滾動，往下滑。 */
    SCROLL_VERTICAL("scroll-v"),

    /** 連續滾動，往左滑（直排就是從右往左）。 */
    SCROLL_HORIZONTAL("scroll-h");

    val isScroll: Boolean get() = this != PAGED

    /** 滾動軸是不是橫向。 */
    val scrollsHorizontally: Boolean get() = this == SCROLL_HORIZONTAL

    companion object {
        fun fromId(id: String?): ReaderMode = entries.firstOrNull { it.id == id }
            // 舊版只有 `scroll` 一個值，當時靠排版方向決定軸向；
            // 讀到舊值時由呼叫端用 [defaultFor] 換算，這裡只保證不炸。
            ?: PAGED

        /** 這個 id 認不認得（`scroll` 是舊版的單一滾動值）。 */
        fun isKnown(id: String?): Boolean =
            id == "scroll" || entries.any { it.id == id }

        /** 該排版方向下最順手的預設：直排橫滑、橫排下滾。 */
        fun defaultFor(vertical: Boolean): ReaderMode =
            if (vertical) SCROLL_HORIZONTAL else SCROLL_VERTICAL
    }
}

/**
 * 閱讀版面的外觀設定。
 *
 * 只描述「怎麼排」，不涉及 Compose 型別，所以這個檔案可以進純 JVM 單元測試。
 *
 * 排版數值參考 Apple HIG 與 Apple Books 的閱讀版面：
 * 正文行高 1.7（HIG 的 body 是 17pt/22pt ≈ 1.29，但中文長文獻要更鬆才不擠）、
 * 頁邊留白 20（HIG 建議 16–20pt）、標題不跨頁、段落至少留兩行。
 */
data class ReaderStyle(
    val background: Long,
    /** 正文顏色。 */
    val foreground: Long,
    val link: Long,
    /**
     * 字級百分比（100 = 書自己的大小）。
     *
     * 由 WebView 的 `textZoom` 套用，不寫進 CSS：`-webkit-text-size-adjust` 在
     * `width=device-width` 的頁面上會被 Chrome 忽略，而書裡的字級有 px 也有 em，
     * 只有 textZoom 能一次蓋住兩種。
     */
    val textScale: Int = 100,
    /** 直排（由上而下、由右而左）。 */
    val vertical: Boolean = false,
    /** 頁邊留白，單位 px（等於 dp）。寫進 CSS，書頁才會整窗翻動而不是在框裡滑。 */
    val margin: Int = 20,
    val lineHeight: Float = 1.7f,
    val mode: ReaderMode = ReaderMode.PAGED,
    /**
     * 深底淺字。
     *
     * 只用來宣告 `color-scheme`：WebView 的捲軸、選取反白、書裡沒指定顏色的
     * 表單控件都會跟著這個值轉深，否則會在墨黑紙上留下一條亮白捲軸。
     */
    val dark: Boolean = false,
    /**
     * 自訂字型的 CSS（`@font-face` 加上覆蓋書本 font-family 的規則）；空字串＝不套。
     *
     * 由呼叫端組好再傳進來：這一層不該知道字型檔存在哪，也不該碰檔案系統。
     */
    val fontFamilyCss: String = ""
) {

    fun css(): String {
        val bg = hex(background)
        val fg = hex(foreground)
        val accent = hex(link)
        val writing = if (vertical) "vertical-rl" else "horizontal-tb"
        val edge = margin.coerceIn(8, 64)
        val lead = String.format(Locale.US, "%.2f", lineHeight.coerceIn(1.2f, 2.4f))
        return """
            :root { --rd-lh: $lead; }
            $fontFamilyCss
            html {
              writing-mode: $writing;
              background: $bg;
              color: $fg;
              -webkit-text-size-adjust: 100%;
              text-size-adjust: 100%;
              overscroll-behavior: none;
            }
            body {
              margin: 0;
              padding: 0;
              background: $bg !important;
              color: $fg;
              line-height: var(--rd-lh);
              text-align: justify;
              text-justify: inter-ideograph;
              word-break: break-word;
              overflow-wrap: break-word;
              line-break: strict;
              font-kerning: normal;
              -webkit-font-smoothing: antialiased;
              -webkit-tap-highlight-color: transparent;
            }
            /* 書自己的 CSS 常把顏色寫死；閱讀主題要蓋得過，否則夜間模式會出現白底黑字。 */
            body, body p, body div, body span, body section, body article,
            body li, body td, body th, body blockquote, body dd, body dt,
            body h1, body h2, body h3, body h4, body h5, body h6, body pre {
              color: $fg !important;
              background-color: transparent !important;
            }
            /* 行距：書幾乎都把 line-height 寫死在 p / div 的 font 簡寫裡，不加 !important 蓋不掉。 */
            body p, body div, body li, body dd, body dt, body blockquote,
            body td, body th, body pre {
              line-height: var(--rd-lh) !important;
            }
            /* 字重：出版轉檔的 CSS 十之八九寫成 `font: normal normal 300 1.2em/1.7em …`，
               也就是 Light 300。細字重 + 小字號 + 長段落是 HIG 明列的易讀性災難
               （三項同時出現一個都不許有），手機上尤其糊成一團。
               正文提到 400，標題 600，粗體仍留 700；span 不動，避免把書用
               inline style 做的強調一起壓平。 */
            body, body p, body div, body li, body dd, body dt, body td,
            body blockquote, body section, body article, body font, body pre {
              font-weight: 400 !important;
            }
            body h1, body h2, body h3, body h4, body h5, body h6,
            .fb2-title, .fb2-subtitle {
              font-weight: 600 !important;
            }
            body b, body strong, body th, body b *, body strong * {
              font-weight: 700 !important;
            }
            /* 標題：不能被切成兩半，也不能孤零零留在頁尾（HIG 的可讀性原則）。 */
            body h1, body h2, body h3, body h4, body h5, body h6 {
              line-height: 1.35 !important;
              break-inside: avoid;
              break-after: avoid;
              page-break-inside: avoid;
              page-break-after: avoid;
              -webkit-column-break-inside: avoid;
              -webkit-column-break-after: avoid;
            }
            /* 直排時整章的每一行都必須等高，欄寬才會一致、頁界才會落在欄線上；
               標題原本 1.35 的行高會讓那幾欄變窄，翻頁就會露出半個字。 */
            html.rd-vertical #__stage, html.rd-vertical #__stage * {
              line-height: var(--rd-lh) !important;
            }
            /* 直排是「重排」而不是「照原版式擺」：出版轉檔的書幾乎都在外層
               div / p 上掛著左右 padding（例如 `div.tt2 { padding: 0 2em }`），
               橫排時只是縮排、看不出問題，直排時卻會讓整塊往左縮、右邊空出一大條。
               這裡對**整棵子樹**歸零，每列都從留白線起排（text-indent 不受影響，
               段落首行縮進照舊）。
               注意選擇器不能包含 #__stage 自己——留白是它自己的定位值，
               加了 !important 會把腳本算出來的頁邊留白一起清掉。 */
            html.rd-vertical #__stage * {
              margin-left: 0 !important;
              margin-right: 0 !important;
              padding-left: 0 !important;
              padding-right: 0 !important;
            }
            /* 不設 orphans / widows：中文短段（詩詞的一句就是一段）很常見，
               要求「至少留兩行」會讓整句被推到下一頁，頁尾留一大塊空白反而更難看。 */
            body img, body svg, body video, body figure, body table {
              break-inside: avoid;
              page-break-inside: avoid;
              max-width: 100% !important;
            }
            body img, body svg, body video { height: auto; }
            /* 單張大圖（封面、插頁）不可以高過一頁，否則分頁會把它切掉。 */
            body img, body svg { max-height: calc(100vh - ${edge * 2}px); }
            body table { max-width: 100%; }
            body pre, body code { white-space: pre-wrap; word-break: break-word; }
            body a, body a * { color: $accent !important; }
            /* FB2 沒有自己的 CSS，這幾條讓它別看起來像原始 XML。 */
            .fb2-title {
              display: block; font-size: 1.3em; font-weight: 600;
              text-align: center; margin: 1.1em 0 .9em;
            }
            .fb2-subtitle { display: block; font-weight: 600; text-align: center; margin: 1em 0; }
            .fb2-p { margin: 0 0 .8em; text-indent: 2em; }
            .fb2-epigraph { display: block; margin: 1em 2em; font-style: italic; }
            .fb2-poem { display: block; margin: 1em 0 1em 2em; }
            .fb2-v { display: block; }
            .fb2-note { display: block; margin: 1em 0; font-size: .92em; }
            .fb2-image { display: block; text-align: center; }
            /* 頁面窗口：它就是「一頁」的可視範圍。
               用 fixed 而不是 absolute：absolute 的包含塊是 body，書的 CSS 一旦給
               body 加了定位/尺寸，窗口就會跟著 body 走。
               這裡的 margin/padding 必須 !important 壓成 0——出版轉檔的書幾乎都用
               裸標籤選擇器（`div { margin: 1em }`），#__viewport 本身是個 div，
               被加上 16px 外距後 clientWidth 就從 360 變成 328：整個版心按 328 排版，
               右邊和底部各空出一條，看起來就是「正文沒居中、偏到右下」。 */
            #__viewport {
              position: fixed; top: 0; right: 0; bottom: 0; left: 0;
              margin: 0 !important; padding: 0 !important; border: 0 !important;
              overflow: hidden;
            }
            /* 章節容器：一次只裝一章，所以它就是一頁長條。
               margin/padding 壓回 0（不加 !important，滾動模式要能用內聯樣式改）：
               書的 `div { margin: 1em }` 會連 #__stage 一起加框，整頁就往內縮一圈。 */
            #__stage {
              box-sizing: border-box;
              display: block;
              background: $bg;
              margin: 0;
              padding: 0;
            }
            /* 一章一個塊。橫排時腳本把它排成 flex（一頁接一頁往右），
               直排時靠 vertical-rl 的區塊流向自然從右往左排。 */
            #__stage > .__ch {
              display: block;
              margin: 0;
              padding: 0;
            }
            #__stage > .__ch > .__cols {
              margin: 0;
              padding: 0;
            }
            /* 分頁模式＝「一頁就是一個矩形」：書給段落／容器加的左右內外距全部歸零，
               版心左界恆為留白、右界恆為 vw−留白，才可能左右嚴格對稱。
               滾動模式保留書原本的縮排（那是「照原版式讀」的體驗）。 */
            html.rd:not(.rd-scroll) #__stage * {
              margin-left: 0 !important;
              margin-right: 0 !important;
              padding-left: 0 !important;
              padding-right: 0 !important;
            }
            /* 封面：一張圖鋪滿整頁，不要留白也不要分欄。 */
            body.rd-cover #__stage {
              display: flex !important;
              align-items: center;
              justify-content: center;
              padding: 0 !important;
            }
            body.rd-cover #__stage > .__ch,
            body.rd-cover #__stage > .__ch > .__cols {
              width: 100%;
              height: 100%;
              display: flex;
              align-items: center;
              justify-content: center;
              margin: 0 !important;
              padding: 0 !important;
            }
            body.rd-cover #__stage img,
            body.rd-cover #__stage svg {
              width: 100%;
              height: 100%;
              max-height: 100%;
              object-fit: contain;
            }
            /* 章末不放任何「下一章」按鈕：滑到盡頭再推一下就換章，頁面上不該多一塊東西。 */
        """.trimIndent()
    }

    val isDark: Boolean get() = luminance(background) < 0.45

    private fun hex(value: Long): String = "#%06X".format(value and 0xFFFFFF)

    private fun luminance(value: Long): Double {
        fun channel(shift: Int): Double {
            val raw = ((value shr shift) and 0xFF) / 255.0
            return if (raw <= 0.04045) raw / 12.92 else Math.pow((raw + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}

/**
 * 把**一章**的正文包成可以交給 WebView 的完整文件。
 *
 * ## 為什麼一次只裝一章
 *
 * 這是 Readest（它的渲染內核是 foliate-js 的 `paginator.js`）的骨架，
 * 也是「不卡」的唯一原因。
 *
 * 先前試過把整本書排進同一份文件（331 章、51 萬 px 寬的單一圖層），
 * 結果 WebView 的渲染行程直接卡死——真機日誌是
 * `Choreographer: Skipped 10599 frames!`（約 3 分鐘沒有回應）。
 * 而一章只有十來頁、幾千 px，排版與捲動都是毫秒級。
 *
 * 「整本書一路讀到底」不是靠把書塞進一個文件，而是靠腳本在章界**就地換內容**：
 * [fragment] 生成下一章的片段，替換 `#__stage` 的子樹。沒有 `loadData`、
 * 沒有白屏、也沒有重新解析書的樣式表。
 *
 * ## 三件事必須自己做
 *
 * 1. **剝掉腳本**。EPUB 允許書裡帶 JavaScript，那是別人的程式碼，在我們的
 *    WebView 裡跑等於把本機能力交出去；閱讀用不到，一律移除。
 * 2. **改寫資源路徑**。書裡的 `../Images/a.jpg`、`kindle:embed:0001` 全部換成
 *    閱讀器自己的 URL 空間，圖片與樣式才載得到。
 * 3. **注入排版與翻頁腳本**。書原本的 `<head>` 在前、閱讀器的樣式在後，
 *    同優先級時我們才蓋得過它。
 *
 * @param chapter 目前章節的絕對章號（0 基）。腳本回報位置、換章都用它。
 * @param chapterCount 全書章數；腳本靠它判斷「還有沒有下一章」。
 */
object ChapterHtml {

    private const val BLANK_IMAGE =
        "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="

    /** 生成閱讀器主文件；`#__stage` 裡先放好 [chapter] 這一章。 */
    fun build(
        book: Book,
        content: ChapterContent,
        style: ReaderStyle,
        chapter: Int = 0,
        chapterCount: Int = 1,
        initialAnchor: String? = null,
        initialFraction: Float = 0f
    ): String {
        val lang = book.metadata.language?.takeIf { it.isNotBlank() } ?: "zh"
        val cover = isCoverPage(content.html)
        return buildString {
            append("<!DOCTYPE html><html lang=\"").append(escape(lang)).append("\" class=\"rd")
            if (style.vertical) append(" rd-vertical")
            if (style.mode.isScroll) append(" rd-scroll")
            append("\"><head><meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no\">")
            // 告訴內核這一頁是深底：捲軸、選取、原生控件才會用深色版本。
            append("<meta name=\"color-scheme\" content=\"")
            append(if (style.dark) "dark" else "light")
            append("\">")
            append("<title>").append(escape(book.metadata.title)).append("</title>")
            headLinks(book, content).forEach { append(it) }
            append("<style id=\"__rdstyle\">").append(style.css()).append("</style>")
            append("</head><body")
            if (cover) append(" class=\"rd-cover\"")
            append("><div id=\"__viewport\"><div id=\"__stage\">")
            append(sectionOf(book, content, chapter, withLinks = false))
            append("</div></div>")
            append(
                script(
                    vertical = style.vertical,
                    scroll = style.mode.isScroll,
                    scrollX = style.mode.scrollsHorizontally,
                    cover = cover,
                    margin = style.margin.coerceIn(8, 64),
                    chapter = chapter.coerceAtLeast(0),
                    chapterCount = chapterCount.coerceAtLeast(1),
                    initialAnchor = initialAnchor,
                    initialFraction = initialFraction
                )
            )
            append("</body></html>")
        }
    }

    /**
     * 只生成一章的內容片段，供頁內腳本換章時塞進 `#__stage`。
     *
     * 片段帶 `<link>`：換到用另一套樣式表的章時，腳本的 `adoptLinks()`
     * 會把它們搬進 `<head>`，所以換章之後的版式跟第一次打開時完全一樣。
     */
    fun fragment(book: Book, content: ChapterContent, chapter: Int): String =
        sectionOf(book, content, chapter, withLinks = true)

    private fun sectionOf(
        book: Book,
        content: ChapterContent,
        chapter: Int,
        withLinks: Boolean
    ): String {
        val raw = content.html
        val head = HEAD.find(raw)?.groupValues?.get(1).orEmpty()
        val body = BODY.find(raw)?.groupValues?.get(1)
            ?: if (HEAD.containsMatchIn(raw)) raw.substringAfter("</head>", "") else raw
        val cleanHead = rewriteStyleBlocks(
            book, content.basePath, rewriteUrls(book, content.basePath, stripScripts(head))
        )
        val cleanBody = rewriteStyleBlocks(
            book, content.basePath, rewriteUrls(book, content.basePath, stripScripts(body))
        )
        // `<link>` 一律由呼叫端決定放哪裡：主文件放 <head>，換章片段放最前面
        //（腳本會搬去 <head>）。留在這裡會重複載入同一份 CSS。
        // `<title>` / `<meta>` / `<base>` 是文件級的，塞進 body 沒有意義，一併清掉。
        val headStyle = DOC_META.replace(LINK.replace(cleanHead, ""), "")
        return buildString {
            if (withLinks) {
                LINK.findAll(cleanHead)
                    .map { it.value.trim() }
                    .filter { STYLESHEET.containsMatchIn(it) }
                    .distinct()
                    .forEach { append(it) }
            }
            val bodyClass = bodyClassOf(raw)
            append("<section class=\"__ch")
            if (bodyClass.isNotBlank()) append(' ').append(escape(bodyClass))
            append("\" data-c=\"").append(chapter).append("\"><div class=\"__cols\">")
            append(headStyle)
            append(cleanBody)
            append("</div></section>")
        }
    }

    /**
     * 這一章 `<head>` 裡的樣式表連結（已改寫成閱讀器的 URL 空間）。
     *
     * 每個 `<link>` 都打上 `data-ch-style`：它標明「這份樣式表是**這一章**帶來的」。
     * 換章時腳本靠這個記號把上一章的樣式表撤掉，見 `adoptLinks`。
     */
    private fun headLinks(book: Book, content: ChapterContent): List<String> {
        val head = HEAD.find(content.html)?.groupValues?.get(1).orEmpty()
        if (head.isBlank()) return emptyList()
        val clean = rewriteUrls(book, content.basePath, stripScripts(head))
        return LINK.findAll(clean)
            .map { it.value.trim() }
            .filter { STYLESHEET.containsMatchIn(it) }
            .distinct()
            .map { link -> link.replaceFirst("<link", "<link data-ch-style=\"\"") }
            .toList()
    }

    /**
     * 這一章是不是「只有一張圖」。
     *
     * 封面章節各家做法不同：有的 `img`、有的 `svg` 包 `<image>`；共通點是沒有正文。
     * 這種頁面要鋪滿整屏，不能按一般正文排版（否則就是上圖下白）。
     */
    fun isCoverPage(html: String): Boolean {
        val pictures = IMG.findAll(html).count() + SVG_IMAGE.findAll(html).count()
        if (pictures != 1) return false
        return TEXT_ONLY.replace(html, " ").replace("&nbsp;", " ").trim().length < 32
    }

    /** 腳本與事件屬性一律移除；`javascript:` 連結也一併中和。 */
    fun stripScripts(html: String): String {
        var result = SCRIPT.replace(html, "")
        result = HANDLER.replace(result, "")
        result = result.replace(Regex("""(?i)href\s*=\s*(["'])\s*javascript:[^"']*\1"""), "href=\"#\"")
        return result
    }

    /** 改寫所有 URL 屬性（`src` / `href` / `poster` / `xlink:href`）。 */
    private fun rewriteUrls(book: Book, basePath: String, html: String): String =
        ATTR.replace(html) { match ->
            val name = match.groupValues[1]
            val quoted = match.groupValues[2]
            val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
            val quote = if (quoted.startsWith("\"")) "\"" else "'"
            val rewritten = resolveValue(book, basePath, value, name)
            if (rewritten == null) match.value else "$name=$quote$rewritten$quote"
        }

    /** `<style>` 區塊裡的 `url()` 也要改，否則書的裝飾圖全成了空白。 */
    private fun rewriteStyleBlocks(book: Book, basePath: String, html: String): String =
        STYLE_BLOCK.replace(html) { match ->
            val css = CssRewriter.rewrite(match.groupValues[1]) { book.resolveHref(basePath, it) }
            "<style>" + css + "</style>"
        }

    private fun resolveValue(book: Book, basePath: String, value: String, attrName: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        if (lower.startsWith("data:") || lower.startsWith("mailto:") || lower.startsWith("tel:")) return null
        if (lower.startsWith("javascript:")) {
            return if (attrName.equals("href", true)) "#" else null
        }
        // 外部網址留著，點下去由 ViewClient 攔下來交給瀏覽器。
        if (lower.startsWith("http://") || lower.startsWith("https://")) return null
        if (trimmed.startsWith("#")) return null

        val key = book.resolveHref(basePath, trimmed)
        val isImageLike = attrName.equals("src", true) ||
                attrName.equals("poster", true) ||
                attrName.equals("xlink:href", true)
        if (key == null) {
            // 解析不出來：圖片給 1×1 透明圖，連結指向本頁，免得整頁跳去不存在的路徑。
            return when {
                isImageLike -> BLANK_IMAGE
                attrName.equals("href", true) -> "#"
                else -> null
            }
        }
        val base = when {
            key.startsWith("chapter:") -> ReaderPaths.PREFIX + "chapter/" + key.removePrefix("chapter:")
            // EPUB 的章節就是 ZIP 裡的一般檔案，靠 book 自己回答「這個路徑是不是正文」。
            else -> book.chapterOfResource(key)?.let { ReaderPaths.PREFIX + "chapter/$it" }
                ?: ReaderPaths.absoluteResource(key)
        }
        val fragment = if (trimmed.contains('#')) trimmed.substringAfter('#').takeIf { it.isNotEmpty() } else null
        return if (fragment != null) "$base#$fragment" else base
    }

    private fun bodyClassOf(raw: String): String =
        BODY_TAG.find(raw)?.groupValues?.get(1)
            ?.let { Xml.attrs(it)["class"] }
            .orEmpty()
            .replace(Regex("""\brd(-\w+)?\b"""), "")
            .trim()

    @Suppress("LongParameterList", "LongMethod")
    private fun script(
        vertical: Boolean,
        scroll: Boolean,
        scrollX: Boolean,
        cover: Boolean,
        margin: Int,
        chapter: Int,
        chapterCount: Int,
        initialAnchor: String?,
        initialFraction: Float
    ): String {
        val anchor = initialAnchor
            ?.let { "'" + it.replace("\\", "\\\\").replace("'", "\\'") + "'" }
            ?: "null"
        val start = String.format(Locale.US, "%.4f", initialFraction.coerceIn(0f, 1f))
        return """
<script>
(function () {
  var T0 = (window.performance && performance.now) ? performance.now() : 0;

  var V = $vertical;         // 直排（vertical-rl）
  var SCROLL = $scroll;      // 連續滾動
  var SCROLL_X = $scrollX;   // 滾動軸是不是橫向
  var COVER = $cover;        // 只有一張圖的封面頁
  var M = $margin;           // 頁邊留白（px）
  var CUR = $chapter;        // 目前章（絕對章號）
  var COUNT = $chapterCount; // 全書章數
  var START = $start;        // 上次讀到的章內比例
  var ANCHOR = $anchor;

  // ── 只有兩種位移模型 ────────────────────────────────────────────────
  //
  //   PAGED   一屏一頁：腳本算頁、跟手拖、放手吸附。
  //   SCROLL  連續：整章是一條長卷，位移全部交給瀏覽器原生捲動。
  //
  // 差別只在這裡。下面所有東西——頁號、進度、邊界、換章——都從
  // [readPos] 一個出口推出來，沒有任何「目前第幾頁」的變數可以跟實際
  // 位移對不上。舊版把 page 存成狀態，再用兩套公式互相換算，只要其中
  // 一次量測失敗（字型晚到、樣式表晚到、重排），狀態就永久跑掉，
  // 於是出現「章末再往前 → 回到本章開頭」這種繞圈。
  var PAGED = !SCROLL;

  // 直排一定是橫向捲動（vertical-rl 的區塊方向就是從右往左），
  // 「直排 + 上下滾」因此直接併到橫向，不留第三條程式路徑。
  var AXIS_X = PAGED || SCROLL_X || V;

  var view = document.getElementById('__viewport');
  var stage = document.getElementById('__stage');
  var sec = null;

  var VW = 0, VH = 0;
  var NEG = false;   // 捲動容器的 scrollLeft 是否為負方向（直排就是）
  var N = 1;         // 本章正文頁數（不含幻影頁）
  var P = 1;         // 一頁的步進（px）
  var laying = false;           // 排版中：此刻的 scroll 事件不算數
  var glideRaf = 0, glideTimer = 0, nudgeTimer = 0;
  var lastDragAt = 0;
  var switchSeq = 0;            // 換章序號：樣式表晚到時用來判斷「還是不是那一次」

  function clamp(v, lo, hi) { return v < lo ? lo : (v > hi ? hi : v); }
  function stamp() { return Date.now(); }

  // ── 幾何 ────────────────────────────────────────────────────────────
  // 一律取自「頁面窗口」這個真實盒子，不用 window.innerWidth/innerHeight：
  // 那兩個值在縮放、insets、書的 CSS 動到 body 時都會歪。
  function readViewport() {
    var w = view.clientWidth, h = view.clientHeight;
    VW = w > 0 ? w : (window.innerWidth || 1);
    VH = h > 0 ? h : (window.innerHeight || 1);
  }
  function vw() { return VW || 1; }
  function vh() { return VH || 1; }
  /** 一頁的版心（扣除兩邊留白）。 */
  function pageW() { return Math.max(1, vw() - 2 * M); }
  function pageH() { return Math.max(1, vh() - 2 * M); }

  // ── 位移層：唯一出口 ────────────────────────────────────────────────
  // pos = 0 是「最開頭」，越大越往閱讀方向。直排的 scrollLeft 是負方向
  //（negative scroll type），橫排是正的——用 Math.abs 一次蓋掉兩種。
  function readPos() {
    return AXIS_X ? Math.abs(view.scrollLeft) : Math.max(0, view.scrollTop);
  }
  function posMax() {
    return AXIS_X
      ? Math.max(0, view.scrollWidth - view.clientWidth)
      : Math.max(0, view.scrollHeight - view.clientHeight);
  }
  function writePos(p) {
    p = clamp(p, 0, posMax());
    if (AXIS_X) view.scrollLeft = NEG ? -p : p;
    else view.scrollTop = p;
  }

  // ── 頁號：從位移反推，不是狀態 ──────────────────────────────────────
  //
  // 幻影頁：正文前後各留一頁空白，頁號因此是 0 … N+1，正文是 1 … N。
  // 多了這兩頁，「拖到盡頭」就變成一件有位置的事——位移真的走得到 0 或
  // N+1，放手時看落在哪一頁就知道該不該換章，不必再靠「起手時是否在
  // 邊界」去猜（舊版就是猜的，猜錯就跳回章首）。foliate 的做法一模一樣：
  // 它的 element 寬度 = iframe 寬度 + 兩頁。
  function pageOf(p) { return Math.floor((p + P / 2) / P); }
  function posOfPage(k) { return k * P; }
  function rawPage() { return PAGED ? pageOf(readPos()) : 0; }
  /** 正文第 0 頁 = 版面上第 1 頁。 */
  function textPage() { return clamp(rawPage() - 1, 0, N - 1); }
  function fracOf(k) { return N > 1 ? clamp((k - 1) / (N - 1), 0, 1) : 0; }
  function pageForFrac(f) { return N > 1 ? 1 + Math.round(clamp(f, 0, 1) * (N - 1)) : 1; }

  function frac() {
    if (COVER) return 0;
    if (PAGED) return fracOf(rawPage());
    var m = posMax();
    return m > 0 ? clamp(readPos() / m, 0, 1) : 0;
  }

  // ── 排版 ────────────────────────────────────────────────────────────
  function resetInline() {
    var r = document.documentElement, s = stage.style, v = view.style;
    r.style.overflow = 'hidden';
    s.display = ''; s.position = ''; s.left = ''; s.right = ''; s.top = ''; s.bottom = '';
    s.margin = ''; s.width = ''; s.height = ''; s.padding = ''; s.boxSizing = '';
    s.overflow = ''; s.flexDirection = ''; s.flexWrap = '';
    v.position = ''; v.overflow = ''; v.overflowX = ''; v.overflowY = '';
    v.padding = ''; v.boxSizing = ''; v.touchAction = '';
    v.left = ''; v.right = ''; v.top = ''; v.bottom = '';
    v.scrollLeft = 0; v.scrollTop = 0;
    if (sec) {
      sec.style.width = ''; sec.style.height = '';
      var c = sec.firstElementChild;
      if (c && c.style) {
        c.style.width = ''; c.style.height = '';
        c.style.columnWidth = ''; c.style.columnCount = '';
        c.style.columnGap = ''; c.style.columnFill = '';
      }
    }
    // 直排的行距會被校正成能整除版心，換章／換方向時要先還原。
    r.style.removeProperty('--rd-lh');
  }

  // 直排的「行」本身就是一欄。版心寬除不盡欄寬時，翻頁會切掉半個字；
  // 把行高（＝列寬）微調成能整除版心寬，每一頁的邊界就都落在欄線上。
  function fitVerticalPage() {
    var probe = null, list = stage.querySelectorAll('p, div, span');
    for (var i = 0; i < list.length; i++) {
      if (list[i].textContent.replace(/[\s\u00a0\u3000]/g, '') !== '') { probe = list[i]; break; }
    }
    var fallback = pageW() / Math.max(1, Math.round(pageW() / 28));
    if (!probe) return fallback;
    var cs = getComputedStyle(probe);
    var raw = cs.lineHeight;
    var size = parseFloat(cs.fontSize) || 16;
    var lh = raw && raw.indexOf('px') >= 0 ? parseFloat(raw) : (parseFloat(raw) || 0) * size;
    if (!(lh > 4) || lh > pageW() / 2) return fallback;
    var perPage = Math.max(1, Math.round(pageW() / lh));
    var col = pageW() / perPage;
    document.documentElement.style.setProperty('--rd-lh', col + 'px');
    return col;
  }

  // 書裡常夾著空的 `<p></p>`（轉檔留下的換行佔位）。橫排時只佔一行、
  // 幾乎看不出來；直排時一個空段落就是**一整欄**的高度，會把第一欄白白
  // 吃掉，整章往左推一格——真機上就表現成「右邊空一大條」。
  function dropEmptyBlocks() {
    var list = stage.querySelectorAll('p, div');
    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      if (el.classList && el.classList.contains('__cols')) continue;
      if (el.querySelector('img, svg, video, audio, iframe, table, hr, canvas')) continue;
      if (el.textContent.replace(/[\s\u00a0\u3000]/g, '') === '') el.style.display = 'none';
    }
  }

  // ── 分欄 ────────────────────────────────────────────────────────────
  //
  // 用 column-count 而不是 column-width：column-width 只是「期望值」，
  // 容器寬或視口寬只要有一點出入，WebView 就會自己算出別的欄寬（真機
  // 量到過 275 而不是 328），於是每頁平移一屏時版心偏右、右緣切掉半個字。
  // column-count 是強制均分：欄寬 = (容器寬 − (n−1)×gap)/n，而容器寬由
  // 我們自己算成 n×W + (n−1)×gap，欄寬就精確等於 W，一像素不差。

  /** 橫排：一頁一欄，回傳這一章幾頁。 */
  function layoutChapterH(s) {
    var cols = s.firstElementChild || s;
    var W = pageW(), G = 2 * M, cs = cols.style, ss = s.style;
    cs.padding = '0';
    cs.columnGap = '0px';
    cs.columnFill = 'auto';
    cs.columnCount = '1';
    cs.height = 'auto';
    cs.width = W + 'px';
    ss.width = W + 'px';
    ss.height = 'auto';
    var totalH = Math.max(1, Math.ceil(cols.scrollHeight));
    var n = Math.max(1, Math.ceil(totalH / pageH()));
    for (var i = 0; i < 3; i++) {
      var inner = n * W + (n - 1) * G;
      cs.height = pageH() + 'px';
      cs.columnCount = String(n);
      cs.columnGap = G + 'px';
      cs.width = inner + 'px';
      ss.height = pageH() + 'px';
      // 章寬要扣掉最後一個欄距，否則長條末端會多出 2×留白，
      // 最後一頁的版心就往左偏（幻影頁的距離也會跟著算錯）。
      ss.width = inner + 'px';
      if (cols.scrollWidth <= inner + 2) break;
      n++;
    }
    return n;
  }

  /** 直排：不分欄——vertical-rl 的每一「行」本來就是一欄，寬度由內容決定。 */
  function layoutChapterV(s) {
    var cols = s.firstElementChild || s;
    var W = pageW(), cs = cols.style, ss = s.style;
    cs.columnWidth = ''; cs.columnCount = ''; cs.columnGap = ''; cs.columnFill = '';
    cs.width = 'max-content';
    cs.height = pageH() + 'px';
    ss.width = 'auto';
    ss.height = pageH() + 'px';
    var w = Math.max(cols.scrollWidth, cols.offsetWidth);
    var n = Math.max(1, Math.ceil(w / W));
    ss.width = (n * W) + 'px';
    return n;
  }

  function layoutPaged() {
    var W = pageW(), G = 2 * M;
    P = V ? W : (W + G);

    view.style.position = 'fixed';
    view.style.left = '0'; view.style.top = '0';
    view.style.right = '0'; view.style.bottom = '0';
    view.style.overflow = 'hidden';

    stage.style.boxSizing = 'content-box';
    // 左右各多留一頁：這就是幻影頁。留白必須是 padding——塊級的
    // margin 不計入可捲動區域，滾到盡頭時會被吞掉。
    stage.style.padding = M + 'px ' + (M + P) + 'px';
    stage.style.height = pageH() + 'px';
    stage.style.width = 'max-content';

    if (V) {
      // vertical-rl 的區塊流向本身就是從右往左，block 依序排過去就是閱讀順序。
      stage.style.display = 'block';
      fitVerticalPage();
    } else {
      stage.style.display = 'flex';
      stage.style.flexDirection = 'row';
      stage.style.flexWrap = 'nowrap';
    }
    N = Math.max(1, V ? layoutChapterV(sec) : layoutChapterH(sec));
  }

  function layoutScrolled() {
    view.style.position = 'fixed';
    view.style.left = '0'; view.style.top = '0';
    view.style.right = '0'; view.style.bottom = '0';
    if (AXIS_X) {
      view.style.overflowX = 'auto';
      view.style.overflowY = 'hidden';
    } else {
      view.style.overflowX = 'hidden';
      view.style.overflowY = 'auto';
    }
    stage.style.boxSizing = 'content-box';
    stage.style.padding = M + 'px';

    if (AXIS_X) {
      stage.style.height = pageH() + 'px';
      stage.style.width = 'max-content';
      if (V) {
        stage.style.display = 'block';
      } else {
        stage.style.display = 'flex';
        stage.style.flexDirection = 'row';
        stage.style.flexWrap = 'nowrap';
      }
      N = Math.max(1, V ? layoutChapterV(sec) : layoutChapterH(sec));
      P = 1;
    } else {
      // 縱向：書原本的縮排保留，看起來像「照原版式讀」。
      //
      // box-sizing 必須是 border-box：外層統一把它設成 content-box（橫向
      // 捲動要靠 max-content + padding 拼長卷），到了縱向就變成
      // `width:100% + padding:M` ＝ 比視口寬 2M，右邊那一條直接被視口裁掉。
      // 真機上就表現成「左邊有留白、右邊切掉半個字」——看起來像沒居中。
      stage.style.boxSizing = 'border-box';
      stage.style.width = '100%';
      stage.style.height = 'auto';
      stage.style.display = 'block';
      N = 1;
      P = 1;
    }
  }

  // 版面排好之後才有溢出，此刻才量得到 scrollLeft 的正負方向。
  function probeScroll() {
    if (!AXIS_X) { NEG = false; return; }
    if (posMax() < 1) { NEG = false; return; }
    var keep = view.scrollLeft;
    view.scrollLeft = -1;
    var neg = view.scrollLeft < 0;
    view.scrollLeft = 1;
    var pos = view.scrollLeft > 0;
    NEG = neg || !pos;
    view.scrollLeft = keep;
  }

  function measure() {
    laying = true;
    stopGlide();
    readViewport();
    resetInline();
    sec = stage.querySelector('.__ch') || stage;

    if (COVER) {
      view.style.position = 'absolute';
      view.style.left = '0'; view.style.top = '0';
      view.style.right = '0'; view.style.bottom = '0';
      view.style.overflow = 'hidden';
      stage.style.display = 'flex';
      stage.style.position = 'relative';
      stage.style.width = '100%';
      stage.style.height = '100%';
      N = 1;
      P = vw();
      NEG = false;
      laying = false;
      return;
    }

    dropEmptyBlocks();
    if (PAGED) layoutPaged(); else layoutScrolled();
    probeScroll();
    // 手勢歸屬：翻頁由腳本接管，必須告訴瀏覽器「不要搶」——否則它會先在
    // 合成執行緒原生滾一下、再被我們的 preventDefault 拉回來，那個「先動
    // 一下又彈回」就是黏滯。原生捲動模式則相反，要還給瀏覽器。
    view.style.touchAction = PAGED ? 'none' : (AXIS_X ? 'pan-x' : 'pan-y');
    laying = false;
  }

  // ── 平滑位移 ────────────────────────────────────────────────────────
  // 自己逐幀寫 scrollLeft，跟 foliate 一樣。不用 CSS
  // `scroll-behavior: smooth`：那個時長不可控，而且會跟翻頁狀態機打架。
  function stopGlide() {
    if (glideRaf) { cancelAnimationFrame(glideRaf); glideRaf = 0; }
    if (glideTimer) { clearTimeout(glideTimer); glideTimer = 0; }
    if (nudgeTimer) { clearTimeout(nudgeTimer); nudgeTimer = 0; }
  }

  function glideTo(target, ms) {
    stopGlide();
    target = clamp(target, 0, posMax());
    var from = readPos();
    ms = ms || 240;
    if (document.hidden || Math.abs(target - from) < 1) { writePos(target); report(); return; }
    var t0 = 0, done = false;
    function finish() {
      if (done) return;
      done = true;
      stopGlide();
      writePos(target);
      report();
    }
    function frame(t) {
      glideRaf = 0;
      if (!t0) t0 = t;
      var k = Math.min(1, (t - t0) / ms);
      writePos(from + (target - from) * (1 - (1 - k) * (1 - k)));
      if (k < 1) glideRaf = requestAnimationFrame(frame);
      else finish();
    }
    glideRaf = requestAnimationFrame(frame);
    // 保險：頁面切到後台時 rAF 會被暫停，動畫就永遠停在半路。
    glideTimer = setTimeout(finish, ms + 80);
  }

  /** 已經在盡頭還要往前：回彈一下，給「這裡沒有下一頁」一個確認感。 */
  function nudge(dir) {
    var base = posOfPage(clamp(rawPage(), 1, N));
    glideTo(base + dir * Math.min(140, P * 0.3), 130);
    nudgeTimer = setTimeout(function () { glideTo(base, 170); }, 160);
  }

  // ── 換章：就地換內容，不重新載入文件 ────────────────────────────────
  //
  // 這是「整本書一路讀到底」的真正做法，也是 foliate 的骨架（它每章一個
  // iframe 且只保留當前那一個）：DOM 裡永遠只有一章、幾千 px，到邊界時把
  // 下一章的片段換進來。沒有 loadData、沒有白屏、沒有重解析樣式表。
  /**
   * 把這一章帶來的樣式表搬進 <head>，並撤掉上一章留下的。
   *
   * 為什麼要撤：書裡不同章可以用完全不同的樣式表。這本《唐宋詞鑑賞辭典》的封面
   * 章用 `fixed.css`，裡面寫著 `html, body { font-size: 0 }`（固定版面的常見寫法，
   * 只放一張圖、不要文字）。舊版只往 <head> 加、從不刪，於是翻到正文時那條
   * `font-size: 0` 還在——整份文件每一個字都排成 0×0：頁數照算、翻頁照走，
   * 但每一頁都是空白。
   *
   * 做法是**按 href 做差集**：兩章共用同一份樣式表時（絕大多數的書）什麼都不動，
   * 所以不會因為換章而閃一下；只有真的換了樣式表才增刪。
   *
   * 回傳 `{ added, changed }`：`added` 是這次新增的 <link>（呼叫端等它們載入完成
   * 要再量一次版面），`changed` 表示這次換章動到了樣式表，無論增或刪都得重量。
   */
  function adoptLinks(box) {
    var wanted = [];
    var links = box.querySelectorAll('link[href]');
    for (var i = 0; i < links.length; i++) {
      var el = links[i];
      var href = el.getAttribute('href');
      if (el.parentNode) el.parentNode.removeChild(el);
      if (href && wanted.indexOf(href) < 0) wanted.push(href);
    }

    // 先補這一章缺的：<head> 裡已經有同一個 href 就直接沿用。
    var have = {};
    var existing = document.head.querySelectorAll('link[href]');
    for (var j = 0; j < existing.length; j++) {
      have[existing[j].getAttribute('href')] = true;
    }
    var added = [];
    for (var k = 0; k < wanted.length; k++) {
      if (have[wanted[k]]) continue;
      var clone = document.createElement('link');
      clone.rel = 'stylesheet';
      clone.href = wanted[k];
      clone.setAttribute('data-ch-style', '');
      document.head.appendChild(clone);
      added.push(clone);
    }

    // 再撤上一章留下的：只動「章節帶來的」，不動文件自己的。
    var mine = document.head.querySelectorAll('link[data-ch-style][href]');
    var removed = 0;
    for (var m = 0; m < mine.length; m++) {
      var node = mine[m];
      if (wanted.indexOf(node.getAttribute('href')) < 0 && node.parentNode) {
        node.parentNode.removeChild(node);
        removed++;
      }
    }
    return { added: added, changed: added.length > 0 || removed > 0 };
  }

  /**
   * 切到第 abs 章，並定位到章內比例 f（0 = 章首，1 = 章末）。
   *
   * 回傳 false 代表「這次換不了」——沒有這一章、片段拿不到、章號對不上。
   * 呼叫端拿到 false 必須**原地不動**。舊版沒有這個約定，換章失敗時
   * page 已經先被歸零，畫面就退回本章開頭。
   */
  // ── 換章入場 ────────────────────────────────────────────────────────
  //
  // 換章是「同一本書裡往前走／往回走」，不是跳到另一個地方。給它一個方向，
  // 讀者才知道自己剛剛跨過了一條線——不然章與章之間只是一次閃爍。
  //
  // 用 CSS transition 而不是 Web Animations：這裡要的是「一定收得回來」。
  // transition 的終點寫在 inline style 上，動畫沒跑（後台、無頭、舊內核）
  // 就是直接套終點，位置永遠是對的；WAAPI 的 keyframes 沒開播時，
  // 元素反而會停在起始關鍵格（偏了 0.14 頁、還半透明）。
  //
  // 曲線取 M3 的 emphasized decelerate（cubic-bezier(0.05, 0.7, 0.1, 1)）：
  // 起步快、尾巴長，像被吸進定位。位移與透明度分開算——位移帶方向感，
  // 透明度只做單純的減速（M3 的 effects 曲線不 overshoot）；兩者擠在同一條
  // 曲線會顯得拖泥帶水。時長是本專案的設計決定，不是 M3 規定值。
  function animateEnter(el, dir) {
    if (!el || !el.style) return;
    // 系統設了「減少動態」就不演：這種整體位移對暈動症不友善。
    try {
      var mq = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)');
      if (mq && mq.matches) return;
    } catch (e) { /* 沒有 matchMedia 的 WebView：照常演 */ }
    var span = AXIS_X ? vw() : vh();
    var step = span * 0.14 * (dir >= 0 ? 1 : -1);
    // 新內容從「還沒讀到的那一側」進來：
    //   直排（往左讀）→ 從左緣；橫排 → 從右緣；上下捲動 → 從下緣。
    // 版面前後各有一頁幻影頁，往任何一側偏都不會露出內容以外的東西。
    var dx = AXIS_X ? (V ? -step : step) : 0;
    var dy = AXIS_X ? 0 : step;
    el.style.transition = 'none';
    el.style.transform = 'translate3d(' + dx + 'px,' + dy + 'px,0)';
    el.style.opacity = '0.5';
    // 讀一次版面，把上面這組初值算進樣式，否則下一步會被合併成「沒變化」。
    void el.offsetWidth;
    el.style.transition = 'transform 340ms cubic-bezier(0.05, 0.7, 0.1, 1),' +
                          'opacity 220ms cubic-bezier(0.2, 0, 0, 1)';
    el.style.transform = 'translate3d(0,0,0)';
    el.style.opacity = '1';
    // 收尾把 inline 樣式清乾淨：留著會讓這一大塊內容永遠是一個合成層。
    // finish() 是保險——transition 進行到一半時把 inline 樣式拿掉，執行中的
    // transition 仍會壓著計算值（畫面就停在半透明）；先請它直接跳到終點。
    setTimeout(function () {
      if (!el.style) return;
      try {
        if (el.getAnimations) {
          var list = el.getAnimations();
          for (var i = 0; i < list.length; i++) list[i].finish();
        }
      } catch (e) { /* 舊內核：交給下面清 inline */ }
      el.style.transition = '';
      el.style.transform = '';
      el.style.opacity = '';
    }, 420);
  }

  function switchTo(abs, f) {
    if (abs < 0 || abs >= COUNT) return false;
    if (abs === CUR) return false;              // 同一章：只重新定位，不重建 DOM
    if (!window.Android || !Android.chapterFragment) return false;

    var html = '';
    try { html = Android.chapterFragment(abs) || ''; } catch (e) { html = ''; }
    if (!html) return false;

    var box = document.createElement('div');
    box.innerHTML = html;
    var newSec = box.querySelector('section.__ch');
    if (!newSec) return false;
    // 片段自己帶著章號。對不上就是拿錯了——與其排錯版，不如不動。
    var got = newSec.getAttribute('data-c');
    if (got !== null && String(abs) !== String(got)) {
      if (window.Android && Android.onMetrics) {
        Android.onMetrics('{"error":"fragment-mismatch","want":' + abs + ',"got":' + got + '}');
      }
      return false;
    }

    var adopted = adoptLinks(box);
    var seq = ++switchSeq;
    var from = CUR;
    stopGlide();
    stage.innerHTML = '';
    stage.appendChild(newSec);
    CUR = abs;
    // 封面只對「只有一張圖的那一章」有意義；離開之後就是正文了。
    if (COUNT > 1) {
      COVER = false;
      if (document.body.classList) document.body.classList.remove('rd-cover');
    }

    measure();
    if (COVER) { report(); return true; }
    if (PAGED) writePos(posOfPage(pageForFrac(clamp(f, 0, 1))));
    else writePos(clamp(f, 0, 1) * posMax());
    // 定位完成才演：動畫只負責「從哪一側進來」，不負責位置。
    animateEnter(newSec, abs > from ? 1 : -1);
    report();
    reportGeometry();

    // 這一章換了樣式表（增或刪）時，上面那次量測不算數：新增的 CSS 是非同步
    // 載入的，被撤掉的舊 CSS 也要等下一次版面才算真的失效（這本書的封面樣式表
    // 就是 `html{font-size:0}`——撤掉之後正文字才長得出來）。兩種情況都要重量。
    if (adopted.changed) {
      var keep = clamp(f, 0, 1);
      var redone = false;
      var again = function () {
        if (redone || seq !== switchSeq) return;
        redone = true;
        relayout(keep);
      };
      for (var i = 0; i < adopted.added.length; i++) {
        adopted.added[i].addEventListener('load', again);
        adopted.added[i].addEventListener('error', again);
      }
      requestAnimationFrame(again);
      setTimeout(again, 400);
    }
    return true;
  }

  // ── 翻頁 ────────────────────────────────────────────────────────────
  //
  // 只有一個出口：往前就在版面上往前走一格，走到正文最後一頁的下一格
  // （幻影頁）就是換章。換不了就回彈，位置不動。
  function turn(dir) {
    if (COVER) return switchTo(CUR + dir, dir > 0 ? 0 : 1);

    if (!PAGED) {
      var span = AXIS_X ? vw() : vh();
      if (dir > 0) {
        if (readPos() >= posMax() - 1) return switchTo(CUR + 1, 0);
        glideTo(Math.min(posMax(), readPos() + span));
        return true;
      }
      if (readPos() <= 1) return switchTo(CUR - 1, 1);
      glideTo(Math.max(0, readPos() - span));
      return true;
    }

    var k = rawPage();
    if (dir > 0) {
      if (k >= N) return switchTo(CUR + 1, 0);
      glideTo(posOfPage(k + 1));
      return true;
    }
    if (k <= 1) return switchTo(CUR - 1, 1);
    glideTo(posOfPage(k - 1));
    return true;
  }

  function next() { return turn(1); }
  function prev() { return turn(-1); }

  function atEndNow() {
    if (COVER) return true;
    if (PAGED) return rawPage() >= N;
    return posMax() <= 1 || readPos() >= posMax() - 1;
  }
  function atStartNow() {
    if (COVER) return true;
    if (PAGED) return rawPage() <= 1;
    return readPos() <= 1;
  }

  function goAnchor(id) {
    if (!id) return;
    var el = document.getElementById(id);
    if (!el) {
      var list = document.getElementsByName(id);
      if (list && list.length) el = list[0];
    }
    if (!el) return;
    if (!PAGED) { el.scrollIntoView(); report(); return; }
    if (COVER) return;
    var r = el.getBoundingClientRect();
    // rect 是視口座標；補回目前的位移，就是它在長條上的位置。
    var at = V ? readPos() + (vw() - M) - r.right : readPos() + r.left - M;
    writePos(posOfPage(clamp(pageOf(at), 1, N)));
    report();
  }

  // ── 手勢 ────────────────────────────────────────────────────────────
  //
  // 翻頁模式只有**一組**監聽：touchmove 直接寫 scrollLeft。
  // 寫 scrollLeft 只改捲動偏移，不觸發樣式重算、不必重新提交合成層；
  // 換成 transform 推一整條超寬圖層，每幀都要重切 tile，就是黏滯的來源。
  var pan = null;
  if (PAGED) {
    document.addEventListener('touchstart', function (e) {
      if (COVER || !e.touches || e.touches.length !== 1) { pan = null; return; }
      stopGlide();
      var t = e.touches[0];
      pan = { x: t.clientX, y: t.clientY, from: readPos(), lx: t.clientX, lt: stamp(), v: 0, on: false };
    }, { passive: true });

    document.addEventListener('touchmove', function (e) {
      if (!pan || !e.touches || e.touches.length !== 1) return;
      var t = e.touches[0];
      var dx = t.clientX - pan.x, dy = t.clientY - pan.y;
      if (!pan.on) {
        if (Math.abs(dx) < 6 && Math.abs(dy) < 6) return;
        // 明顯是縱向就放手，別跟長按選字／圖片拖曳搶手勢。
        if (Math.abs(dx) <= Math.abs(dy)) { pan = null; return; }
        pan.on = true;
      }
      if (e.cancelable) e.preventDefault();
      var t1 = stamp(), dt = t1 - pan.lt;
      if (dt > 0) { pan.v = (t.clientX - pan.lx) / dt; pan.lx = t.clientX; pan.lt = t1; }
      // 手指往右＝內容往右走：直排露出左邊（下一頁），橫排露出左邊（上一頁）。
      writePos(pan.from + (V ? dx : -dx));
    }, { passive: false });

    document.addEventListener('touchend', function () {
      var p = pan;
      pan = null;
      if (!p || !p.on) return;
      lastDragAt = stamp();
      // 放手時只做一件事：吸附到「離手指最近的那一頁」。
      //
      // 距離決定方向，速度只補一腳：只有當位移幾乎停在頁界上（不到
      // 0.18 頁）又甩得夠快，才再往前走一頁。舊版照 foliate 的
      // `速度 × 一頁` 直接加進位移裡，真機上一甩就是三四頁，
      // 讀者根本猜不到會停在哪——這正是「邏輯混亂」的體感來源。
      var at = readPos();
      var k = pageOf(at);
      var off = at - posOfPage(k);
      var vel = V ? p.v : -p.v;
      if (!isFinite(vel)) vel = 0;
      if (Math.abs(off) < P * 0.18 && Math.abs(vel) > 0.45) k += vel > 0 ? 1 : -1;
      if (k <= 0) {
        if (!switchTo(CUR - 1, 1)) glideTo(posOfPage(1));
        return;
      }
      if (k >= N + 1) {
        if (!switchTo(CUR + 1, 0)) glideTo(posOfPage(N));
        return;
      }
      glideTo(posOfPage(k));
    }, { passive: true });
  }

  // 原生捲動模式：拖動、慣性、邊界回彈全由瀏覽器負責，這裡一個
  // touchmove 都不註冊——只要頁面上有 non-passive 的 touchmove，瀏覽器
  // 就不敢立刻開始滾動（要等 JS 跑完才決定），那 100ms 就是另一種黏滯。
  // 只補一個章界手勢：起手時就已經在盡頭、又繼續往閱讀方向推。
  //
  // 「往閱讀方向」不是固定往左：直排橫滑是手指往右（內容跟著手指走，
  // 露出左邊＝後面的頁），橫排橫滑是往左，上下滾是往上。舊版寫死
  // `main < 0`，於是直排滑到章末往左拖只會後退、往右拖才換章，
  // 讀者看到的就是「方向亂跳」。
  var FWD_SIGN = AXIS_X ? (V ? 1 : -1) : -1;
  var edge = null, edgeStamp = 0;
  if (!PAGED) {
    document.addEventListener('touchstart', function (e) {
      if (!e.touches || e.touches.length !== 1) { edge = null; return; }
      var t = e.touches[0];
      edge = { x: t.clientX, y: t.clientY, end: atEndNow(), start: atStartNow() };
    }, { passive: true });

    document.addEventListener('touchend', function (e) {
      var f = edge;
      edge = null;
      if (!f) return;
      var t = (e.changedTouches && e.changedTouches[0]);
      if (!t) return;
      var dx = t.clientX - f.x, dy = t.clientY - f.y;
      var main = Math.abs(dx) >= Math.abs(dy) ? dx : dy;
      if (Math.abs(main) < 44) return;
      var fwd = main * FWD_SIGN;
      var t1 = stamp();
      if (t1 - edgeStamp < 600) return;
      if (fwd > 0 && f.end) { edgeStamp = t1; switchTo(CUR + 1, 0); }
      else if (fwd < 0 && f.start) { edgeStamp = t1; switchTo(CUR - 1, 1); }
    }, { passive: true });
  }

  // ── 點擊 ────────────────────────────────────────────────────────────
  document.addEventListener('click', function (e) {
    // 剛拖過一次手指：那是翻頁／捲動，不再當成點擊。
    if (stamp() - lastDragAt < 400) return;
    var selection = window.getSelection ? String(window.getSelection()) : '';
    if (selection) return;
    var target = e.target;
    if (target && target.closest) {
      var link = target.closest('a[href]');
      if (link) {
        var href = link.getAttribute('href') || '';
        if (href && href.charAt(0) !== '#') return;   // 交給 ViewClient
      }
    }
    var ratio = e.clientX / vw();
    // 直排是「由右往左」，所以左邊才是下一頁／下一章；橫排相反。
    if (V) ratio = 1 - ratio;

    if (PAGED) {
      if (ratio < 0.3) { if (!prev()) nudge(-1); return; }
      if (ratio > 0.7) { if (!next()) nudge(1); return; }
      if (window.Android && Android.onTap) Android.onTap('center');
      return;
    }
    // 捲動模式：平常點一下只叫出工具列；已經在章末／章首時，點前進區／
    // 後退區才當成換章——章末按鈕拿掉之後，這就是點擊入口。
    if (ratio > 0.7 && atEndNow()) { next(); return; }
    if (ratio < 0.3 && atStartNow()) { prev(); return; }
    if (window.Android && Android.onTap) Android.onTap('center');
  });

  // ── 上報 ────────────────────────────────────────────────────────────
  function report() {
    // 排版途中（#__stage 剛被清掉、位移剛歸零）報出去的位置是假的。
    // 舊版就是在這裡把「章首」寫進進度，於是存檔存到章首、下次打開也回章首。
    if (laying) return;
    var page = PAGED ? textPage() : 0;
    var total = PAGED ? N : 1;
    if (window.Android && Android.onProgress) Android.onProgress(page, total, frac());
    if (window.Android && Android.onLocation) Android.onLocation(CUR, frac());
  }

  function relayout(f) {
    stopGlide();
    measure();
    if (!COVER) {
      if (PAGED) writePos(posOfPage(pageForFrac(f)));
      else writePos(clamp(f, 0, 1) * posMax());
    }
    report();
    reportGeometry();
  }

  // ── 對外介面（Kotlin 與驗證腳本用）──────────────────────────────────
  window.__reader = {
    next: next,
    prev: prev,
    /** 跳到某個絕對章號；章不在 DOM 裡時就地換內容。 */
    goChapter: function (abs, f) {
      var to = clamp(f || 0, 0, 1);
      if (abs === CUR) {
        if (!COVER) {
          if (PAGED) writePos(posOfPage(pageForFrac(to)));
          else writePos(to * posMax());
        }
        report();
        return true;
      }
      return switchTo(abs, to);
    },
    goTop: function () {
      if (!COVER) {
        if (PAGED) writePos(posOfPage(1));
        else writePos(0);
      }
      report();
    },
    goAnchor: goAnchor,
    setStyle: function (css) {
      var el = document.getElementById('__rdstyle');
      if (el) el.textContent = css;
      var keep = frac();
      // 換字級／行距會整份重排，等瀏覽器排完再量才準。
      requestAnimationFrame(function () { relayout(keep); });
    },
    repaginate: function () { relayout(frac()); },
    state: function () {
      return {
        chapter: CUR, count: COUNT,
        page: PAGED ? textPage() : 0,
        pages: N,
        raw: rawPage(),
        frac: frac(),
        pos: Math.round(readPos()),
        max: Math.round(posMax()),
        neg: NEG ? 1 : 0,
        vertical: V ? 1 : 0,
        paged: PAGED ? 1 : 0
      };
    }
  };

  window.addEventListener('resize', function () { relayout(frac()); });

  // 原生捲動沒有結束事件；用 rAF 節流上報，一幀最多一次。
  var scrollTick = 0;
  if (!PAGED) {
    function onScroll() {
      if (scrollTick) return;
      scrollTick = requestAnimationFrame(function () { scrollTick = 0; report(); });
    }
    view.addEventListener('scroll', onScroll, { passive: true });
  }

  /** 診斷：排版結果一行打完，`adb logcat -s ReaderLayout` 就能核對。 */
  function reportGeometry() {
    if (!window.Android || !Android.onMetrics) return;
    var r = view.getBoundingClientRect();
    Android.onMetrics(JSON.stringify({
      ch: CUR, of: COUNT, n: N, page: rawPage(), step: Math.round(P),
      // 分頁模式的自洽條件：可捲動距離必須剛好等於 (正文頁數 + 2) × 步進 減一頁，
      // 也就是「前後各一頁幻影頁」。對不上就代表版面多算或少算了一頁。
      want: PAGED ? Math.round((N + 1) * P) : Math.round(posMax()),
      max: Math.round(posMax()),
      pos: Math.round(readPos()), neg: NEG ? 1 : 0,
      paged: PAGED ? 1 : 0, axisX: AXIS_X ? 1 : 0,
      winW: window.innerWidth, winH: window.innerHeight,
      viewW: Math.round(r.width), viewH: Math.round(r.height),
      stageW: Math.round(stage.getBoundingClientRect().width),
      layoutMs: Math.round(((window.performance && performance.now) ? performance.now() : 0) - T0)
    }));
  }

  function start() {
    measure();
    if (!COVER) {
      if (PAGED) writePos(posOfPage(pageForFrac(START)));
      else writePos(clamp(START, 0, 1) * posMax());
    }
    goAnchor(ANCHOR);
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function () {
        var keep = frac();
        relayout(keep);
      }).catch(function () {});
    }
    report();
    reportGeometry();
    // 版面排完、瀏覽器把捲動位置放穩之後再多報兩次，
    // 免得進度條停在「排版前的舊位置」。
    requestAnimationFrame(function () {
      requestAnimationFrame(function () { report(); });
    });
    setTimeout(function () { report(); }, 80);
    if (window.Android && window.Android.onReady) window.Android.onReady();
  }

  if (document.readyState === 'complete') start();
  else window.addEventListener('load', start);
})();
</script>
        """.trimIndent()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private val HEAD = Regex("""(?is)<head[^>]*>(.*?)</head>""")
    private val BODY = Regex("""(?is)<body[^>]*>(.*?)</body>""")
    private val BODY_TAG = Regex("""(?is)<body\b([^>]*)>""")
    private val LINK = Regex("""(?is)<link\b[^>]*>""")
    /** 文件級的標籤：抽章節片段時要去掉，塞進 `<section>` 裡沒有意義。 */
    private val DOC_META = Regex("""(?is)<title\b[^>]*>.*?</title\s*>|<meta\b[^>]*>|<base\b[^>]*>""")
    private val STYLESHEET = Regex("""(?i)rel\s*=\s*["']?[^"'>]*stylesheet""")
    private val SCRIPT = Regex("""(?is)<script\b[^>]*>.*?</script\s*>|<script\b[^>]*/\s*>""")
    private val HANDLER = Regex("""(?i)\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*')""")
    private val STYLE_BLOCK = Regex("""(?is)<style\b[^>]*>(.*?)</style\s*>""")
    private val ATTR = Regex(
        """(?i)\b(src|href|poster|xlink:href)\s*=\s*("([^"]*)"|'([^']*)')"""
    )
    private val IMG = Regex("""(?i)<img\b""")
    private val SVG_IMAGE = Regex("""(?i)<svg\b[^>]*>[\s\S]*?<image\b""")
    private val TEXT_ONLY = Regex("""(?is)<[^>]+>""")
}
