package com.yuzlyn.mdeditor.data.book

import java.io.RandomAccessFile

/**
 * PalmDB 容器（MOBI / AZW3 / PRC 的外殼）與 MOBI 的兩種壓縮。
 *
 * 這些格式沒有官方規格書，欄位全靠社群逆向（MobileRead wiki、KindleUnpack）；
 * 這裡把「位移 + 語意」集中在一處，上層才不必到處散落魔數。
 */
internal class PalmDb(private val raf: RandomAccessFile) {

    val recordCount: Int
    private val offsets: IntArray
    private val fileLength: Long

    init {
        fileLength = raf.length()
        val header = ByteArray(78)
        raf.seek(0)
        if (raf.read(header) < 78) throw BookException(BookError.FILE_TOO_SMALL)
        recordCount = Bytes.u16(header, 76)
        if (recordCount <= 0) throw BookException(BookError.CORRUPT_STRUCTURE, "no records")
        val table = ByteArray(recordCount * 8)
        raf.seek(78)
        var read = 0
        while (read < table.size) {
            val n = raf.read(table, read, table.size - read)
            if (n <= 0) break
            read += n
        }
        if (read < table.size) throw BookException(BookError.CORRUPT_STRUCTURE, "record table")
        offsets = IntArray(recordCount + 1)
        for (index in 0 until recordCount) {
            offsets[index] = Bytes.u32(table, index * 8)
        }
        offsets[recordCount] = fileLength.toInt()
        for (index in 0 until recordCount) {
            if (offsets[index] <= 0 || offsets[index] > offsets[index + 1]) {
                throw BookException(BookError.CORRUPT_STRUCTURE, "record offset")
            }
        }
    }

    @Synchronized
    fun record(index: Int): ByteArray {
        if (index < 0 || index >= recordCount) throw BookException(BookError.CORRUPT_STRUCTURE, "$index/$recordCount")
        val start = offsets[index].toLong()
        val end = offsets[index + 1].toLong().coerceAtMost(fileLength)
        val size = (end - start).toInt()
        if (size <= 0) return ByteArray(0)
        val bytes = ByteArray(size)
        raf.seek(start)
        var read = 0
        while (read < size) {
            val n = raf.read(bytes, read, size - read)
            if (n <= 0) break
            read += n
        }
        return if (read == size) bytes else bytes.copyOf(read)
    }

    /** 只讀前 4 個位元組，用來判斷記錄是 `FONT` / `RESC` / `FDST` … */
    @Synchronized
    fun magic(index: Int): String {
        if (index < 0 || index >= recordCount) return ""
        val start = offsets[index].toLong()
        val size = (offsets[index + 1].toLong().coerceAtMost(fileLength) - start).toInt()
        if (size < 4) return ""
        val bytes = ByteArray(4)
        raf.seek(start)
        return if (raf.read(bytes) == 4) String(bytes, Charsets.ISO_8859_1) else ""
    }

    @Synchronized
    fun close() {
        runCatching { raf.close() }
    }
}

/** 大端讀取。整份格式都是 big-endian，獨立成物件避免到處寫位移計算。 */
internal object Bytes {

    fun u8(bytes: ByteArray, offset: Int): Int =
        if (offset in bytes.indices) bytes[offset].toInt() and 0xFF else 0

    fun u16(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size || offset < 0) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    fun u32(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size || offset < 0) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    /** 無號 32 位元；位移表用得到（超過 2GB 的檔案雖然罕見，但欄位本身是無號的）。 */
    fun u32Long(bytes: ByteArray, offset: Int): Long = u32(bytes, offset).toLong() and 0xFFFFFFFFL

    /** 可變長度整數：每 byte 取低 7 位，最高位為 1 表示結束。 */
    fun varLen(bytes: ByteArray, start: Int): Int {
        var value = 0
        var index = start
        val limit = minOf(bytes.size, start + 4)
        while (index < limit) {
            val byte = bytes[index].toInt() and 0xFF
            value = (value shl 7) or (byte and 0x7F)
            index++
            if (byte and 0x80 != 0) break
        }
        return value
    }

    fun varLenLength(bytes: ByteArray, start: Int): Int {
        var index = start
        val limit = minOf(bytes.size, start + 4)
        while (index < limit) {
            val byte = bytes[index].toInt() and 0xFF
            index++
            if (byte and 0x80 != 0) break
        }
        return index - start
    }

    fun popCount(value: Int): Int = Integer.bitCount(value)

    /** 尾端連續 0 的位數，INDX 的遮罩換算用。 */
    fun trailingZeros(value: Int): Int = if (value == 0) 0 else Integer.numberOfTrailingZeros(value)
}

/**
 * 可增長的位元組緩衝。
 *
 * PalmDOC 的回溯參照可能「來源還沒寫完就邊讀邊寫」（distance 小於 length），
 * 所以一定要逐位元組往前複製，不能整段 arraycopy。
 * 舊版每次回溯都 `toByteArray()` 複製整份輸出，複雜度 O(n²) —— 一本 2MB 的書
 * 要複製上百 GB，這就是「打開就卡死、甚至閃退」的真正原因。
 */
internal class ByteSink(initialCapacity: Int = 8192) {

    private var buffer = ByteArray(initialCapacity.coerceAtLeast(64))
    var size: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (size + extra <= buffer.size) return
        var capacity = buffer.size
        while (capacity < size + extra) capacity *= 2
        buffer = buffer.copyOf(capacity)
    }

    fun write(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        ensure(length)
        System.arraycopy(bytes, offset, buffer, size, length)
        size += length
    }

    fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

    /** LZ77 回溯：來源在已寫入的區域，可能與目標重疊（逐位元組複製即可）。 */
    fun backReference(distance: Int, length: Int) {
        if (distance <= 0 || distance > size) return
        ensure(length)
        var source = size - distance
        var remaining = length
        while (remaining-- > 0) {
            buffer[size++] = buffer[source++]
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    /** 清空但保留容量，重複組裝章節時不必反覆配置。 */
    fun reset() {
        size = 0
    }
}

/** PalmDOC（LZ77）解壓。 */
internal object PalmDocDecoder {

    fun decode(data: ByteArray): ByteArray {
        val out = ByteSink(data.size * 3)
        var index = 0
        while (index < data.size) {
            val value = data[index++].toInt() and 0xFF
            when {
                value == 0x00 -> out.write(0)
                // 0x01–0x08：後面跟著 N 個不壓縮位元組。
                value <= 0x08 -> {
                    val count = minOf(value, data.size - index)
                    out.write(data, index, count)
                    index += count
                }
                value <= 0x7F -> out.write(value)
                value <= 0xBF -> {
                    if (index >= data.size) break
                    val pair = (value shl 8) or (data[index++].toInt() and 0xFF)
                    val distance = (pair shr 3) and 0x07FF
                    val length = (pair and 0x07) + 3
                    out.backReference(distance, length)
                }
                // 0xC0–0xFF：空白 + (value xor 0x80)。
                else -> {
                    out.write(0x20)
                    out.write(value xor 0x80)
                }
            }
        }
        return out.toByteArray()
    }
}

/**
 * HUFF/CDIC 解壓（Kindle 的字典式 Huffman 編碼）。
 *
 * 兩段結構：
 * - `HUFF` 記錄存 Huffman 表：table1 依「前 8 個位元」查起始碼長，table2 依碼長查碼的最小值。
 * - `CDIC` 記錄存字典本體，一條條 (長度, 內容) —— 字典項自己也可能還是壓縮的，要遞迴解。
 */
internal class HuffCdicDecoder(db: PalmDb, huffIndex: Int, recordCount: Int) {

    private val found = BooleanArray(256)
    private val codeLength1 = IntArray(256)
    private val value1 = IntArray(256)
    private val minCode = IntArray(33)
    private val maxCode = IntArray(33)

    private val dictionary = mutableListOf<DictionaryEntry>()

    init {
        if (huffIndex < 0 || huffIndex >= db.recordCount) throw BookException(BookError.CORRUPT_STRUCTURE, "HUFF")
        val huff = db.record(huffIndex)
        if (String(huff, 0, 4, Charsets.ISO_8859_1) != "HUFF") {
            throw BookException(BookError.CORRUPT_STRUCTURE, "HUFF")
        }
        val offset1 = Bytes.u32(huff, 8)
        val offset2 = Bytes.u32(huff, 12)
        for (index in 0 until 256) {
            val raw = Bytes.u32(huff, offset1 + index * 4)
            found[index] = raw and 0x80 != 0
            codeLength1[index] = raw and 0x1F
            value1[index] = raw ushr 8
        }
        for (index in 1..32) {
            val at = offset2 + (index - 1) * 8
            minCode[index] = Bytes.u32(huff, at)
            maxCode[index] = Bytes.u32(huff, at + 4)
        }
        var collected = 0
        for (index in 1 until recordCount) {
            val record = db.record(huffIndex + index)
            val magic = if (record.size >= 4) String(record, 0, 4, Charsets.ISO_8859_1) else ""
            if (magic != "CDIC") throw BookException(BookError.CORRUPT_STRUCTURE, "CDIC")
            val headerLength = Bytes.u32(record, 4)
            val totalEntries = Bytes.u32(record, 8)
            val codeLength = Bytes.u32(record, 12)
            val inThisRecord = minOf(1 shl codeLength, totalEntries - collected)
            for (slot in 0 until inThisRecord) {
                val offset = Bytes.u16(record, headerLength + slot * 2)
                val start = headerLength + offset
                val meta = Bytes.u16(record, start)
                val length = meta and 0x7FFF
                val preDecompressed = meta and 0x8000 != 0
                val body = if (start + 2 + length <= record.size) {
                    record.copyOfRange(start + 2, start + 2 + length)
                } else {
                    ByteArray(0)
                }
                dictionary += DictionaryEntry(body, preDecompressed)
            }
            collected += inThisRecord
            if (collected >= totalEntries) break
        }
    }

    fun decode(data: ByteArray): ByteArray {
        val out = ByteSink(data.size * 3)
        val bitLength = data.size * 8
        var position = 0
        while (position < bitLength) {
            val bits = readBits(data, position)
            val head = (bits ushr 24).toInt() and 0xFF
            var codeLength = codeLength1[head]
            var value = value1[head]
            if (!found[head]) {
                // 前 8 位不足以決定碼長時，往更長的碼找：比較「目前讀到的前 N 位」與該長度的最小碼。
                while (codeLength <= 32 &&
                    (bits ushr (32 - codeLength)) < minCode[codeLength].toLong()
                ) {
                    codeLength++
                }
                if (codeLength > 32) break
                value = maxCode[codeLength]
            }
            if (codeLength <= 0) break
            position += codeLength
            if (position > bitLength) break
            val code = value.toLong() - (bits ushr (32 - codeLength))
            if (code < 0 || code >= dictionary.size) break
            val entry = dictionary[code.toInt()]
            val bytes = if (entry.preDecompressed) entry.data else decode(entry.data).also {
                // 字典項自己也是壓縮的，解一次就快取住，整本書會反覆用到。
                dictionary[code.toInt()] = DictionaryEntry(it, true)
            }
            out.write(bytes)
        }
        return out.toByteArray()
    }

    /** 從位元位置讀 32 位元（可能跨 5 個位元組）。 */
    private fun readBits(data: ByteArray, from: Int): Long {
        val startByte = from shr 3
        val end = from + 32
        val endByte = end shr 3
        var bits = 0L
        var index = startByte
        while (index <= endByte) {
            bits = (bits shl 8) or (if (index < data.size) data[index].toLong() and 0xFF else 0L)
            index++
        }
        val shift = 8 - (end and 7)
        return (bits ushr shift) and 0xFFFFFFFFL
    }

    private class DictionaryEntry(val data: ByteArray, val preDecompressed: Boolean)
}

/** INDX 表裡的一筆：名稱 + 標籤值。 */
internal class IndexEntry(val name: String, val tags: Map<Int, IntArray>)

internal class IndexData(val entries: List<IndexEntry>, val cncx: Map<Int, String>)

/**
 * INDX 索引表解析（KF8 的骨架表、碎片表、NCX 都是這個結構）。
 *
 * 三層：主記錄放標籤定義（TAGX）、後續記錄放資料列（每列以 IDXT 的位移表定位）、
 * 再後面是 CNCX 字串池。
 */
internal object IndexReader {

    fun read(db: PalmDb, index: Int, charset: java.nio.charset.Charset): IndexData {
        val main = db.record(index)
        if (main.size < 56 || String(main, 0, 4, Charsets.ISO_8859_1) != "INDX") {
            throw BookException(BookError.CORRUPT_STRUCTURE, "INDX $index")
        }
        val headerLength = Bytes.u32(main, 4)
        val numRecords = Bytes.u32(main, 24)
        val numCncx = Bytes.u32(main, 52)

        val tagxOffset = headerLength
        if (tagxOffset + 12 > main.size) throw BookException(BookError.CORRUPT_STRUCTURE, "TAGX")
        val tagxLength = Bytes.u32(main, tagxOffset + 4)
        val numControlBytes = Bytes.u32(main, tagxOffset + 8)
        val numTags = ((tagxLength - 12) / 4).coerceAtLeast(0)
        val tags = ArrayList<TagSpec>(numTags)
        for (i in 0 until numTags) {
            val at = tagxOffset + 12 + i * 4
            tags += TagSpec(
                tag = Bytes.u8(main, at),
                numValues = Bytes.u8(main, at + 1),
                mask = Bytes.u8(main, at + 2),
                endFlag = Bytes.u8(main, at + 3)
            )
        }

        val cncx = HashMap<Int, String>()
        var offsetBase = 0
        for (i in 0 until numCncx) {
            val record = db.record(index + numRecords + i + 1)
            var position = 0
            while (position < record.size) {
                val start = position
                val length = Bytes.varLen(record, position)
                position += Bytes.varLenLength(record, position)
                val end = minOf(record.size, position + length)
                cncx[offsetBase + start] = String(record, position, end - position, charset)
                position = end
            }
            offsetBase += 0x10000
        }

        val entries = ArrayList<IndexEntry>()
        for (i in 0 until numRecords) {
            val record = db.record(index + 1 + i)
            if (record.size < 56 || String(record, 0, 4, Charsets.ISO_8859_1) != "INDX") continue
            val idxt = Bytes.u32(record, 20)
            val count = Bytes.u32(record, 24)
            for (j in 0 until count) {
                val offset = Bytes.u16(record, idxt + 4 + 2 * j)
                if (offset <= 0 || offset >= record.size) continue
                val nameLength = Bytes.u8(record, offset)
                if (offset + 1 + nameLength > record.size) continue
                val name = String(record, offset + 1, nameLength, charset)
                val startPos = offset + 1 + nameLength
                val values = readTags(record, startPos, numControlBytes, tags)
                entries += IndexEntry(name, values)
            }
        }
        return IndexData(entries, cncx)
    }

    private fun readTags(
        record: ByteArray,
        startPos: Int,
        numControlBytes: Int,
        tags: List<TagSpec>
    ): Map<Int, IntArray> {
        val result = HashMap<Int, IntArray>()
        var controlIndex = 0
        var position = startPos + numControlBytes
        for (spec in tags) {
            if (spec.endFlag and 1 != 0) {
                controlIndex++
                continue
            }
            val byte = Bytes.u8(record, startPos + controlIndex)
            val masked = byte and spec.mask
            val count: Int
            if (masked == spec.mask) {
                if (Bytes.popCount(spec.mask) > 1) {
                    count = Bytes.varLen(record, position)
                    position += Bytes.varLenLength(record, position)
                } else {
                    count = 1
                }
            } else {
                // 控制位元組直接編碼「有幾個值」。
                count = masked ushr Bytes.trailingZeros(spec.mask)
            }
            if (count <= 0) continue
            val total = count * spec.numValues
            val values = IntArray(total)
            for (k in 0 until total) {
                if (position >= record.size) break
                values[k] = Bytes.varLen(record, position)
                position += Bytes.varLenLength(record, position)
            }
            result[spec.tag] = values
        }
        return result
    }

    private class TagSpec(val tag: Int, val numValues: Int, val mask: Int, val endFlag: Int)
}
