package com.yuzlyn.mdeditor.data.export

/**
 * 稿紙排版的最小單位。
 *
 * 方格（作文格）要求「一個字剛好佔一格」，而 Android 的排版引擎是按比例排字的：
 * 中文 1em、英文 0.4em、標點不一定滿格。兩套座標系放在一起，誤差會逐字累積，
 * 於是出現「只有行首第一字在格子裡、越往右越歪」。編輯器用的是同一套解法：
 * 把文字切成整數格的原子，再自己排（見 `CellAlignedTransformation`）。
 *
 * 原子有兩種：
 * - 漢字、標點、emoji：各佔 1 格。[cells] 記的是佔幾格。
 * - 英數單詞：整串量一次寬再向上取整成 N 格，單詞內部保持連寫（在 N 格裡居中）。
 */
internal data class CellAtom(
    /** 要畫的字。[isBreak] 為 true 時是換段記號，不畫也不佔格。 */
    val text: String,
    /** 佔幾格。 */
    val cells: Int
) {
    /** 換段：橫排換行、直排換到左邊一欄。 */
    val isBreak: Boolean get() = text == BREAK

    companion object {
        const val BREAK = "\n"
        val Break = CellAtom(BREAK, 0)
    }
}

/**
 * 把原子裝成一列一列（橫排是一行一行，直排是一欄一欄——裝箱規則完全一樣）。
 *
 * 規則刻意單純：裝到滿就換下一個，遇到換段記號也換。
 * 單一原子比整列還長（例如一長串英文）時**不切**——寧可讓它溢出，
 * 也不要把單詞劈成兩半，那比排版難看更難讀；呼叫端在建原子時已經按列長切過。
 */
internal fun packCellLines(atoms: List<CellAtom>, cellsPerLine: Int): List<List<CellAtom>> {
    if (cellsPerLine <= 0) return emptyList()
    val lines = ArrayList<List<CellAtom>>()
    var current = ArrayList<CellAtom>()
    var used = 0

    fun flush() {
        if (current.isNotEmpty()) {
            lines.add(current)
            current = ArrayList()
            used = 0
        }
    }

    atoms.forEach { atom ->
        if (atom.isBreak) {
            flush()
            return@forEach
        }
        val need = atom.cells.coerceAtLeast(1)
        if (used + need > cellsPerLine && current.isNotEmpty()) flush()
        current.add(atom)
        used += need
    }
    flush()
    return lines
}

/** 一列已經用掉幾格。 */
internal fun List<CellAtom>.usedCells(): Int = sumOf { it.cells.coerceAtLeast(1) }
