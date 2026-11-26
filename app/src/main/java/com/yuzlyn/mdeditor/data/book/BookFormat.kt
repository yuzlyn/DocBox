package com.yuzlyn.mdeditor.data.book

import java.io.File
import java.io.RandomAccessFile

/**
 * 可以**原生閱讀**的電子書格式。
 *
 * 這裡和 `data.importer.ImportFormat` 是兩件事：那邊是「匯入成 Markdown 筆記」，
 * 這邊是「原樣排版讀出來」。電子書不該被拆成純文字——書裡的目錄、註釋、
 * 圖片、CSS 排版都是內容的一部分，轉成 Markdown 等於把它們全丟了。
 */
enum class BookFormat(
    val id: String,
    val label: String,
    val extensions: List<String>,
    val mimeTypes: List<String>
) {
    EPUB(
        "epub",
        "EPUB",
        listOf("epub"),
        listOf("application/epub+zip")
    ),
    MOBI(
        "mobi",
        "MOBI",
        listOf("mobi", "prc"),
        listOf("application/x-mobipocket-ebook")
    ),
    AZW3(
        "azw3",
        "AZW3",
        listOf("azw3", "azw"),
        listOf("application/vnd.amazon.ebook")
    ),
    FB2(
        "fb2",
        "FB2",
        listOf("fb2"),
        listOf("application/x-fictionbook+xml", "text/xml")
    );

    companion object {
        fun fromFileName(name: String?): BookFormat? {
            val extension = name?.substringAfterLast('.', "")?.lowercase() ?: return null
            if (extension.isEmpty()) return null
            return entries.firstOrNull { extension in it.extensions }
        }

        fun fromId(id: String?): BookFormat? =
            id?.takeIf { it.isNotBlank() }?.let { value -> entries.firstOrNull { it.id == value } }

        /**
         * 依檔頭判斷格式，認不得就回 null。
         *
         * 使用者手上的電子書常常沒有副檔名（雲端硬碟轉存、聊天軟體下載），
         * 只靠檔名會整批漏掉，所以匯入前一定要問一次檔頭。
         */
        fun sniff(file: File): BookFormat? {
            if (!file.isFile) return null
            return runCatching {
                RandomAccessFile(file, "r").use { raf ->
                    val head = ByteArray(80)
                    val read = raf.read(head)
                    if (read < 8) return null
                    // ZIP：EPUB 一定是 ZIP，但 ZIP 不一定是 EPUB，稍後還要看 container.xml。
                    if (head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) return EPUB
                    // PalmDB："BOOKMOBI" / "TEXtREAd" 位於 60。
                    if (read >= 68) {
                        val type = String(head, 60, 8, Charsets.US_ASCII)
                        if (type == "BOOKMOBI" || type == "TEXtREAd") {
                            // MOBI 與 AZW3 是同一個容器，靠 record 0 的檔案版本分辨：
                            // KF8（AZW3）版本號 ≥ 8，另外複合檔的 EXTH 會帶 boundary。
                            return sniffPalmDb(raf)
                        }
                    }
                    // FB2 是 XML 文字檔。
                    val text = String(head, 0, read, Charsets.UTF_8)
                        .trimStart('\uFEFF', ' ', '\t', '\r', '\n')
                        .lowercase()
                    if (text.startsWith("<?xml") || text.startsWith("<fictionbook")) {
                        if (String(head, 0, read, Charsets.UTF_8).contains("FictionBook", true)) {
                            return FB2
                        }
                    }
                    null
                }
            }.getOrNull()
        }

        /**
         * 只看開頭幾十個位元組的判斷，給「還沒落地成檔案」的 Uri 用。
         *
         * 分不出 MOBI 與 AZW3 沒關係——兩者共用同一個容器，[MobiBook] 自己會看
         * 版本號決定走 MOBI6 還是 KF8。這裡只需要回答「這是不是書」。
         */
        fun sniffHead(head: ByteArray): BookFormat? {
            if (head.size < 8) return null
            if (head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) {
                // PK 不等於 EPUB：DOCX／XLSX／ODT 也都是 zip。標準 EPUB 的第一個
                // 條目固定是不壓縮的 `mimetype`，內容 `application/epub+zip` 會落在
                // 前 80 個位元組內；沒有這個字串就當「不是書」，交給匯入器處理。
                // 少了這道檢查，.docx 會被丟進閱讀器，使用者只看到「不是 EPUB」。
                val zipHead = String(head, 0, head.size, Charsets.US_ASCII)
                return if (zipHead.contains("application/epub+zip", ignoreCase = true)) {
                    EPUB
                } else {
                    null
                }
            }
            if (head.size >= 68) {
                val type = String(head, 60, 8, Charsets.US_ASCII)
                if (type == "BOOKMOBI" || type == "TEXtREAd") return MOBI
            }
            val text = String(head, 0, head.size, Charsets.UTF_8)
                .trimStart('\uFEFF', ' ', '\t', '\r', '\n')
            if (text.startsWith("<?xml", true) && text.contains("FictionBook", true)) return FB2
            return null
        }

        /** 讀 PalmDB 的 record 0，看 MOBI header 的版本號與 EXTH boundary。 */        private fun sniffPalmDb(raf: RandomAccessFile): BookFormat {
            val header = ByteArray(78)
            raf.seek(0)
            if (raf.read(header) < 78) return MOBI
            val recordCount = u16(header, 76)
            if (recordCount <= 0) return MOBI
            val offsetBytes = ByteArray(4)
            raf.seek(78)
            if (raf.read(offsetBytes) < 4) return MOBI
            val firstRecord = u32(offsetBytes, 0)
            if (firstRecord <= 0) return MOBI
            val record = ByteArray(300)
            raf.seek(firstRecord.toLong())
            val read = raf.read(record)
            if (read < 40) return MOBI
            if (String(record, 16, 4, Charsets.US_ASCII) != "MOBI") return MOBI
            val version = u32(record, 36)
            if (version >= 8) return AZW3
            return MOBI
        }

        private fun u16(bytes: ByteArray, offset: Int): Int {
            if (offset + 2 > bytes.size) return 0
            return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        }

        private fun u32(bytes: ByteArray, offset: Int): Int {
            if (offset + 4 > bytes.size) return 0
            return ((bytes[offset].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 3].toInt() and 0xFF)
        }

        /** 檔案選擇器的 MIME 清單；最後補上「所有檔案」，避免某些 provider 把少見格式濾掉。 */
        val pickerMimeTypes: Array<String> = (entries.flatMap { it.mimeTypes } + "*/*")
            .distinct()
            .toTypedArray()
    }
}
