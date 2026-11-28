package com.yuzlyn.mdeditor.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    private fun version(raw: String): AppVersion =
        requireNotNull(AppVersion.parse(raw)) { "解析失敗：$raw" }

    @Test
    fun `去掉 tag 的 v 前綴`() {
        assertEquals(listOf(2, 1, 2), version("v2.1.2").segments)
        assertEquals(listOf(2, 1, 2), version("V2.1.2").segments)
        assertEquals(listOf(2, 1, 2), version(" 2.1.2 ").segments)
    }

    @Test
    fun `第十個修補版大於第九個`() {
        // 這正是字串比較會出錯的地方： "2.1.10" < "2.1.9"。
        assertTrue(version("2.1.10") > version("2.1.9"))
        assertTrue(version("2.10.0") > version("2.9.9"))
    }

    @Test
    fun `缺的段補零`() {
        assertEquals(0, version("2.1").compareTo(version("2.1.0")))
        assertEquals(0, version("2").compareTo(version("2.0.0")))
    }

    @Test
    fun `正式版高於同號的預發布版`() {
        assertTrue(version("2.2.0") > version("2.2.0-beta.1"))
        assertTrue(version("2.2.0-rc.1") > version("2.2.0-beta.2"))
    }

    @Test
    fun `預發布的數字段按數值比而不是字典序`() {
        assertTrue(version("2.2.0-beta.10") > version("2.2.0-beta.2"))
    }

    @Test
    fun `build metadata 不參與比較`() {
        assertEquals(0, version("2.1.2+abc123").compareTo(version("2.1.2+def456")))
    }

    @Test
    fun `看不懂的版本一律回 null 而不是猜成零`() {
        assertNull(AppVersion.parse(null))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("   "))
        assertNull(AppVersion.parse("v"))
        assertNull(AppVersion.parse("2.1.x"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("2.1.2.3.4"))
        assertNull(AppVersion.parse("9999999"))
        assertNull(AppVersion.parse("-beta.1"))
    }

    @Test
    fun `專案目前的測試版格式可以被解析`() {
        // version.properties 一律寫成 `<基礎版本>-test.<N>`，這裡把它釘住：
        // 這個格式若被改壞，App 內的「檢查更新」會永遠回「版本資訊無法辨識」，
        // 而那種錯在真機上很難一眼看出來。
        val current = requireNotNull(AppVersion.parse("2.1.3-test.1"))
        assertEquals(listOf(2, 1, 3), current.segments)
        assertEquals("test.1", current.prerelease)
        assertEquals("2.1.3-test.1", current.toString())

        // 同號測試版低於正式版：發布 2.1.3 之後，測試機應該收到更新提示。
        assertTrue(version("2.1.3") > version("2.1.3-test.1"))
        assertTrue(version("2.1.3-test.2") > version("2.1.3-test.1"))
        // 測試版仍然高於上一個正式版，才不會被誤判成「該升級到 2.1.2」。
        assertTrue(version("2.1.3-test.1") > version("2.1.2"))
    }

    @Test
    fun `顯示時不帶 tag 的 v 前綴`() {
        assertEquals("2.1.2", version("v2.1.2").toString())
        assertEquals("2.2.0-beta.1", version("v2.2.0-beta.1").toString())
    }
}
