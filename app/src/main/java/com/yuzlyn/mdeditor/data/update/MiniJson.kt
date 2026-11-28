package com.yuzlyn.mdeditor.data.update

/**
 * 只夠這個功能用的 JSON 讀取器。
 *
 * 為什麼不用 `org.json`：單元測試跑在 JVM 上，`org.json` 是 mockable `android.jar`
 * 的 Stub，一呼叫就拋 `RuntimeException`——解析邏輯等於沒法測，只能上真機試。
 * 也不想為了三個欄位引進 `kotlinx-serialization`（多一個編譯器插件、多一份 keep 規則）。
 *
 * 為什麼不寫正則：release 的 `body` 是一大段 Markdown，裡面完全可能出現
 * `"tag_name"` 這種字樣；正則會抓到錯的那一個，然後把某段程式碼當成版本號。
 * 這裡按結構解析，只在頂層物件取欄位。
 *
 * 回傳型別刻意用 `Any?`：物件是 `Map<String, Any?>`、陣列是 `List<Any?>`、
 * 數字一律 `Double`。呼叫端自己 `as?` 轉型，轉不過就當資料不合法。
 */
internal object MiniJson {

    /**
     * 巢狀深度上限。
     *
     * 正常 release 回應不到 10 層；32 層足夠，同時擋住刻意堆疊括號餵爆遞迴的輸入。
     */
    private const val MAX_DEPTH = 32

    /** 解析失敗一律回 null：這個功能不需要分辨「哪裡錯」，只需要知道「不能用」。 */
    fun parse(text: String): Any? = try {
        val parser = Parser(text)
        val value = parser.readValue(depth = 0)
        parser.skipWhitespace()
        if (parser.index != text.length) null else value
    } catch (failure: Malformed) {
        null
    }

    private class Malformed(message: String) : Exception(message)

    private class Parser(private val text: String) {
        var index = 0

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw Malformed("nesting too deep")
            skipWhitespace()
            return when (val current = text.getOrNull(index)) {
                null -> throw Malformed("unexpected end")
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> if (current == '-' || current in '0'..'9') {
                    readNumber()
                } else {
                    throw Malformed("unexpected '$current' at $index")
                }
            }
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            expect('{')
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (text.getOrNull(index) == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                result[key] = readValue(depth + 1)
                skipWhitespace()
                when (text.getOrNull(index)) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return result
                    }
                    else -> throw Malformed("expected ',' or '}' at $index")
                }
            }
        }

        private fun readArray(depth: Int): List<Any?> {
            expect('[')
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (text.getOrNull(index) == ']') {
                index++
                return result
            }
            while (true) {
                result += readValue(depth + 1)
                skipWhitespace()
                when (text.getOrNull(index)) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return result
                    }
                    else -> throw Malformed("expected ',' or ']' at $index")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val builder = StringBuilder()
            while (true) {
                val current = text.getOrNull(index) ?: throw Malformed("unterminated string")
                index++
                when (current) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(readEscape())
                    else -> builder.append(current)
                }
            }
        }

        private fun readEscape(): Char {
            val escape = text.getOrNull(index) ?: throw Malformed("unterminated escape")
            index++
            return when (escape) {
                '"', '\\', '/' -> escape
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> readUnicodeEscape()
                else -> throw Malformed("bad escape '\\$escape'")
            }
        }

        private fun readUnicodeEscape(): Char {
            if (index + 4 > text.length) throw Malformed("truncated \\u escape")
            val hex = text.substring(index, index + 4)
            index += 4
            return hex.toIntOrNull(16)?.toChar() ?: throw Malformed("bad \\u escape: $hex")
        }

        private fun readNumber(): Double {
            val start = index
            if (text.getOrNull(index) == '-') index++
            while (text.getOrNull(index)?.let { it in '0'..'9' } == true) index++
            if (text.getOrNull(index) == '.') {
                index++
                while (text.getOrNull(index)?.let { it in '0'..'9' } == true) index++
            }
            if (text.getOrNull(index) == 'e' || text.getOrNull(index) == 'E') {
                index++
                if (text.getOrNull(index) == '+' || text.getOrNull(index) == '-') index++
                while (text.getOrNull(index)?.let { it in '0'..'9' } == true) index++
            }
            val token = text.substring(start, index)
            return token.toDoubleOrNull() ?: throw Malformed("bad number '$token'")
        }

        private fun readLiteral(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) throw Malformed("expected '$word' at $index")
            index += word.length
            return value
        }

        private fun expect(expected: Char) {
            if (text.getOrNull(index) != expected) {
                throw Malformed("expected '$expected' at $index")
            }
            index++
        }
    }
}
