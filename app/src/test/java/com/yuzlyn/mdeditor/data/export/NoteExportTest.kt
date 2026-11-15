package com.yuzlyn.mdeditor.data.export

import com.yuzlyn.mdeditor.data.profile.ProfileData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipInputStream

/**
 * 匯出的純邏輯：Markdown 清理、檔名、名片、docx 封裝。
 * PDF／圖片要跑 Android 繪圖，留給真機驗收；這裡鎖住「內容對不對」。
 */
class NoteExportTest {

    @Test
    fun `Markdown 清成純文字時去掉記號但保留內容`() {
        val plain = markdownToPlainText("# 標題\n\n**粗體**與`程式碼`\n\n- 項目一\n- 項目二")

        assertEquals("標題\n\n粗體與程式碼\n\n・項目一\n・項目二", plain)
    }

    @Test
    fun `圖片與連結只留文字`() {
        val plain = markdownToPlainText("前 ![示意圖](images/a.png) 後\n\n[官網](https://example.com)")

        assertEquals("前 示意圖 後\n\n官網", plain)
    }

    @Test
    fun `程式碼圍籬去掉但內容保留`() {
        val plain = markdownToPlainText("```kotlin\nval a = 1\n```")

        assertEquals("val a = 1", plain)
    }

    @Test
    fun `表格分隔列會被清掉`() {
        val plain = markdownToPlainText("| 甲 | 乙 |\n| --- | --- |\n| 1 | 2 |")

        assertEquals("| 甲 | 乙 |\n| 1 | 2 |", plain)
    }

    @Test
    fun `連續空行收斂成一個`() {
        val plain = markdownToPlainText("第一段\n\n\n\n第二段")

        assertEquals("第一段\n\n第二段", plain)
    }

    @Test
    fun `檔名濾掉不合法字元並保留可讀部分`() {
        assertEquals("筆記_2026_11_13", sanitizeFileName("筆記/2026:11:13"))
        assertEquals("note", sanitizeFileName("   "))
        assertEquals("trailing", sanitizeFileName("trailing..."))
    }

    @Test
    fun `檔名過長會截斷`() {
        val name = sanitizeFileName("長".repeat(80))

        assertEquals(40, name.length)
    }

    @Test
    fun `名片只收有填的欄位且信箱網址各佔一行`() {
        val card = buildExportCard(
            ProfileData(nickname = "小明", email = "a@b.c", github = "github.com/x")
        )
        assertEquals("小明\na@b.c\ngithub.com/x", card?.text)

        assertEquals("小明", buildExportCard(ProfileData(nickname = "小明"))?.text)
        assertNull(buildExportCard(ProfileData()))
    }

    @Test
    fun `名片逐欄開關與位置`() {
        val profile = ProfileData(
            nickname = "小明",
            email = "a@b.c",
            github = "yuzlyn",
            avatarPath = "/tmp/avatar.png"
        )

        val onlyGithub = buildExportCard(profile, showNickname = false, showEmail = false)
        assertEquals("github.com/yuzlyn", onlyGithub?.text)
        assertEquals(CardPosition.AFTER, onlyGithub?.position)

        // 只留頭像、一個欄位都不印，等於沒有名片。
        assertNull(
            buildExportCard(profile, showNickname = false, showEmail = false, showGithub = false)
        )

        val before = buildExportCard(profile, position = CardPosition.BEFORE)
        assertEquals(CardPosition.BEFORE, before?.position)
        assertEquals("/tmp/avatar.png", before?.avatarPath)

        // 關掉頭像就完全不帶路徑，渲染端不必再判斷。
        assertNull(buildExportCard(profile, showAvatar = false)?.avatarPath)
    }

    @Test
    fun `名片位置未知時回退到正文之後`() {
        assertEquals(CardPosition.AFTER, CardPosition.fromId(null))
        assertEquals(CardPosition.AFTER, CardPosition.fromId("middle"))
        assertEquals(CardPosition.BEFORE, CardPosition.fromId("before"))
    }

    @Test
    fun `GitHub 只填使用者名稱也會補成完整網址`() {
        // 三種常見輸入都要收斂成同一個結果：手打名稱、帶 @、直接貼瀏覽器網址。
        assertEquals("github.com/yuzlyn", ProfileData(github = "yuzlyn").githubUrl)
        assertEquals("github.com/yuzlyn", ProfileData(github = "@yuzlyn").githubUrl)
        assertEquals(
            "github.com/yuzlyn",
            ProfileData(github = "https://github.com/yuzlyn/").githubUrl
        )
        assertEquals("github.com/yuzlyn", ProfileData(github = "  www.github.com/yuzlyn ").githubUrl)
        assertNull(ProfileData(github = "   ").githubUrl)
        assertNull(ProfileData(github = "github.com/").githubUrl)
    }

    @Test
    fun `docx 具備最小部件且文字有轉義`() {
        val bytes = DocxWriter.build(
            listOf(
                DocxBlock("標題 <測試>", headingLevel = 1),
                DocxBlock("A & B"),
                DocxBlock("")
            )
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }

        assertEquals(
            setOf("[Content_Types].xml", "_rels/.rels", "word/document.xml"),
            entries.keys
        )
        val document = entries.getValue("word/document.xml")
        assertTrue(document.contains("&lt;測試&gt;"))
        assertTrue(document.contains("A &amp; B"))
        // 三個段落 + 一組 sectPr
        assertEquals(3, Regex("<w:p>").findAll(document).count())
        assertTrue(document.contains("<w:sectPr>"))
        assertTrue(entries.getValue("[Content_Types].xml").contains("wordprocessingml"))
    }

    @Test
    fun `docx 轉義處理五個 XML 實體並過濾控制字元`() {
        assertEquals(
            "a&amp;b&lt;c&gt;d&quot;e&apos;f",
            DocxWriter.escapeXml("a&b<c>d\"e'f")
        )
        assertEquals("ab", DocxWriter.escapeXml("a\u0001b"))
    }

    @Test
    fun `Markdown 標題會變成 Word 標題層級`() {
        val blocks = markdownToDocxBlocks("# 大標\n## 小標\n內文")

        assertEquals(1, blocks[0].headingLevel)
        assertEquals("大標", blocks[0].text)
        assertEquals(2, blocks[1].headingLevel)
        assertEquals(0, blocks[2].headingLevel)
        assertEquals("內文", blocks[2].text)
    }
}
