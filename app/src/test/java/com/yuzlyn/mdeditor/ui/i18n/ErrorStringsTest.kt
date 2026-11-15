package com.yuzlyn.mdeditor.ui.i18n

import com.yuzlyn.mdeditor.data.ImageInsertError
import com.yuzlyn.mdeditor.data.book.BookError
import com.yuzlyn.mdeditor.data.importer.ImportError
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 資料層錯誤碼的四語覆蓋。
 *
 * 這三組錯誤是使用者真的會看到的（開書失敗、匯入失敗、插圖失敗），以前是資料層
 * 直接寫中文，切語言時只有這幾句不跟著換。現在句子集中在字串表，這裡守兩件事：
 *
 * 1. 每個錯誤碼在四個語種下都有句子，不會漏翻後顯示空白；
 * 2. 技術細節要接在括號裡，不能把整句蓋掉。
 */
class ErrorStringsTest {

    private val languages = listOf(
        AppLanguage.ZH_TW,
        AppLanguage.ZH_CN,
        AppLanguage.EN_US,
        AppLanguage.FR_FR
    )

    @Test
    fun `every book error has a sentence in every language`() {
        languages.forEach { language ->
            val strings = language.appStrings()
            BookError.entries.forEach { error ->
                val text = strings.bookError(error)
                assertTrue(
                    "$language 少了 $error 的句子",
                    text.isNotBlank()
                )
            }
        }
    }

    @Test
    fun `every import error has a sentence in every language`() {
        languages.forEach { language ->
            val strings = language.appStrings()
            ImportError.entries.forEach { error ->
                assertTrue(
                    "$language 少了 $error 的句子",
                    strings.importError(error).isNotBlank()
                )
            }
        }
    }

    @Test
    fun `every image insert error has a sentence in every language`() {
        languages.forEach { language ->
            val strings = language.appStrings()
            ImageInsertError.entries.forEach { error ->
                assertTrue(
                    "$language 少了 $error 的句子",
                    strings.imageInsertError(error).isNotBlank()
                )
            }
        }
    }

    @Test
    fun `detail is appended after the sentence`() {
        val strings = AppLanguage.EN_US.appStrings()
        val plain = strings.bookError(BookError.CHAPTER_UNREADABLE)
        val detailed = strings.bookError(BookError.CHAPTER_UNREADABLE, "OEBPS/ch3.xhtml")
        assertTrue("細節要接在句子後面", detailed.startsWith(plain))
        assertTrue("細節要出現在結果裡", detailed.contains("OEBPS/ch3.xhtml"))
        assertTrue("細節要包在括號裡", detailed.contains("（") || detailed.contains("("))
    }

    @Test
    fun `blank detail does not add empty brackets`() {
        val strings = AppLanguage.ZH_TW.appStrings()
        assertEquals(
            strings.importError(ImportError.EMPTY),
            strings.importError(ImportError.EMPTY, "  ")
        )
    }

    @Test
    fun `following the system resolves to a concrete language`() {
        // SYSTEM 不是真的語種：任何要顯示字串的地方都得先換算。
        val resolved = AppLanguage.SYSTEM.resolved()
        assertTrue(
            "SYSTEM 要換算成四個語種之一，實際是 $resolved",
            resolved != AppLanguage.SYSTEM
        )
        assertTrue(resolved in languages)
        assertTrue(AppLanguage.SYSTEM.appStrings().bookErrFileEmpty.isNotBlank())
    }
}
