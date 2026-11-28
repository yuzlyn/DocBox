package com.yuzlyn.mdeditor.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteReleaseTest {

    private val realResponse = """
        {
          "url": "https://api.github.com/repos/yuzlyn/DocBox/releases/460267360",
          "tag_name": "v2.2.0",
          "name": "DocBox 2.2.0",
          "draft": false,
          "prerelease": false,
          "html_url": "https://github.com/yuzlyn/DocBox/releases/tag/v2.2.0",
          "body": "修正了若干問題。",
          "assets": [
            {
              "name": "DocBox_v2.2.0.apk",
              "content_type": "application/vnd.android.package-archive",
              "size": 37636999,
              "browser_download_url": "https://github.com/yuzlyn/DocBox/releases/download/v2.2.0/DocBox_v2.2.0.apk"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `解析出 tag 版本與 APK 直連`() {
        val release = requireNotNull(RemoteRelease.fromJson(realResponse))
        assertEquals("v2.2.0", release.tag)
        assertEquals(listOf(2, 2, 0), release.version.segments)
        assertEquals("https://github.com/yuzlyn/DocBox/releases/tag/v2.2.0", release.pageUrl)
        assertEquals(
            "https://github.com/yuzlyn/DocBox/releases/download/v2.2.0/DocBox_v2.2.0.apk",
            release.apkUrl
        )
    }

    @Test
    fun `body 裡出現的欄位字樣不會被當成真欄位`() {
        // 正則式解析就是在這裡出錯：說明文字裡引用了一段設定，裡面剛好有 tag_name。
        val tricky = """
            {
              "tag_name": "v2.2.0",
              "html_url": "https://github.com/yuzlyn/DocBox/releases/tag/v2.2.0",
              "body": "舊版設定：{\"tag_name\": \"v9.9.9\", \"html_url\": \"https://example.com/fake\"}",
              "assets": []
            }
        """.trimIndent()
        val release = requireNotNull(RemoteRelease.fromJson(tricky))
        assertEquals("v2.2.0", release.tag)
        assertEquals("https://github.com/yuzlyn/DocBox/releases/tag/v2.2.0", release.pageUrl)
    }

    @Test
    fun `沒有 APK 附件時仍可提示更新但下載回版本頁`() {
        val noAssets = """
            {
              "tag_name": "v2.3.0",
              "html_url": "https://github.com/yuzlyn/DocBox/releases/tag/v2.3.0"
            }
        """.trimIndent()
        val release = requireNotNull(RemoteRelease.fromJson(noAssets))
        assertNull(release.apkUrl)
        assertEquals("https://github.com/yuzlyn/DocBox/releases/tag/v2.3.0", release.pageUrl)
    }

    @Test
    fun `只認 content_type 也找得到 APK`() {
        val byMime = """
            {
              "tag_name": "v2.3.0",
              "html_url": "https://github.com/yuzlyn/DocBox/releases/tag/v2.3.0",
              "assets": [
                {
                  "name": "DocBox-latest.bin",
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://github.com/yuzlyn/DocBox/releases/download/v2.3.0/DocBox-latest.bin"
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            "https://github.com/yuzlyn/DocBox/releases/download/v2.3.0/DocBox-latest.bin",
            requireNotNull(RemoteRelease.fromJson(byMime)).apkUrl
        )
    }

    @Test
    fun `非 https 的連結一律不採用`() {
        val insecure = """
            {
              "tag_name": "v2.3.0",
              "html_url": "http://github.com/yuzlyn/DocBox/releases/tag/v2.3.0"
            }
        """.trimIndent()
        assertNull(RemoteRelease.fromJson(insecure))
    }

    @Test
    fun `缺欄位或格式錯誤時回 null`() {
        assertNull(RemoteRelease.fromJson(""))
        assertNull(RemoteRelease.fromJson("not json"))
        assertNull(RemoteRelease.fromJson("[]"))
        assertNull(RemoteRelease.fromJson("""{"html_url":"https://github.com/yuzlyn/DocBox"}"""))
        assertNull(
            RemoteRelease.fromJson(
                """{"tag_name":"latest","html_url":"https://github.com/yuzlyn/DocBox"}"""
            )
        )
        // 尾端多一個逗號：寬鬆解析器會放過，這裡必須擋下來。
        assertNull(RemoteRelease.fromJson("""{"tag_name":"v2.1.2",}"""))
    }

    @Test
    fun `字串轉義不影響解析`() {
        val escaped = """
            {
              "tag_name": "v2.2.1",
              "html_url": "https://github.com/yuzlyn/DocBox/releases/tag/v2.2.1",
              "name": "DocBox \"測試\" \u7248",
              "assets": []
            }
        """.trimIndent()
        assertEquals("v2.2.1", requireNotNull(RemoteRelease.fromJson(escaped)).tag)
    }

    @Test
    fun `過深的巢狀結構直接拒絕而不是堆疊溢出`() {
        val deep = "{\"a\":" + "[".repeat(64) + "]".repeat(64) + "}"
        assertNull(MiniJson.parse(deep))
    }

    @Test
    fun `解析結果的數字不會被當成字串`() {
        val value = MiniJson.parse("""{"size": 42}""") as Map<*, *>
        assertTrue(value["size"] is Double)
    }
}
