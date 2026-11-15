package com.yuzlyn.mdeditor.ui.i18n

import com.yuzlyn.mdeditor.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.InvocationTargetException

/**
 * 四份字串表有沒有填齊。
 *
 * [AppStrings] 為了避開 JVM 的 255 個參數上限，改成無參數建構 + `lateinit var`，
 * 由 `apply { }` 填值——好處是欄位再多也不會炸，代價是**編譯器不再檢查有沒有漏填**。
 * 這個測試就是補回那個檢查：走一遍所有 getter，沒填的會丟
 * `UninitializedPropertyAccessException`，填了空字串的也一併抓出來。
 */
class AppStringsFillTest {

    private val languages = listOf(
        AppLanguage.ZH_TW,
        AppLanguage.ZH_CN,
        AppLanguage.EN_US,
        AppLanguage.FR_FR
    )

    @Test
    fun `every field is filled in every language`() {
        val getters = AppStrings::class.java.methods
            .filter { it.parameterCount == 0 && it.name.startsWith("get") && it.name != "getClass" }

        assertTrue("反射找不到任何欄位，測試本身失效了", getters.size > 300)

        languages.forEach { language ->
            val strings = language.appStrings()
            val missing = mutableListOf<String>()
            val blank = mutableListOf<String>()

            getters.forEach { getter ->
                val value = try {
                    getter.invoke(strings)
                } catch (e: InvocationTargetException) {
                    val cause = e.targetException
                    if (cause is UninitializedPropertyAccessException) {
                        missing += getter.name
                        return@forEach
                    }
                    throw e
                }
                if (value is String && value.isBlank()) blank += getter.name
            }

            if (missing.isNotEmpty()) {
                fail("$language 有欄位沒填：${missing.joinToString()}")
            }
            if (blank.isNotEmpty()) {
                fail("$language 有欄位是空字串：${blank.joinToString()}")
            }
        }
    }

    @Test
    fun `all four languages expose the same number of fields`() {
        val counts = languages.map { language ->
            language to AppStrings::class.java.methods.count {
                it.parameterCount == 0 && it.name.startsWith("get") && it.name != "getClass"
            }
        }
        val distinct = counts.map { it.second }.distinct()
        assertEquals("四份字串表的欄位數不一樣：$counts", 1, distinct.size)
    }
}
