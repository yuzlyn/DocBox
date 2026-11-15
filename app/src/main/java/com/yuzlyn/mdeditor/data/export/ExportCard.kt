package com.yuzlyn.mdeditor.data.export

import com.yuzlyn.mdeditor.data.profile.ProfileData

/**
 * 名片貼在正文的哪一側。
 *
 * 兩種場合都真實存在：發到社群時名片壓在最後像簽名；
 * 但當作「自我介紹」轉發時，名片放最前面才有人看。
 */
enum class CardPosition(val id: String) {
    BEFORE("before"),
    AFTER("after");

    companion object {
        fun fromId(id: String?): CardPosition =
            entries.firstOrNull { it.id == id } ?: AFTER
    }
}

/**
 * 匯出時附在正文旁的名片。
 *
 * 只帶「已經決定好要印什麼」的結果，不再回頭看設定：
 * 匯出的東西一旦產出就該是使用者當下看到的那一份，
 * 把 [ProfileData] 與開關一路傳進渲染器，只會讓同一張名片在不同格式裡長得不一樣。
 *
 * [avatarPath] 只有圖片格式吃得到——Markdown／純文字沒有「圖」這個概念，
 * PDF 也不在這裡嵌入（見 [NoteDocumentRenderer] 的說明）。
 */
class ExportCard(
    val lines: List<String>,
    val avatarPath: String?,
    val position: CardPosition
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    /** 純文字型態的名片；一行一項，貼在正文前後。 */
    val text: String get() = lines.joinToString("\n")
}

/**
 * 由個人資料與各開關組出名片；一項都沒勾就回 null（等於不附名片）。
 *
 * GitHub 只存使用者名稱，這裡才補成完整網址（見 [ProfileData.githubUrl]）；
 * 補不出來（只填了 `github.com/`）就當作沒填，不印半條壞連結。
 */
fun buildExportCard(
    profile: ProfileData,
    position: CardPosition = CardPosition.AFTER,
    showAvatar: Boolean = true,
    showNickname: Boolean = true,
    showEmail: Boolean = true,
    showGithub: Boolean = true
): ExportCard? {
    val lines = buildList {
        if (showNickname) profile.nickname.trim().takeIf { it.isNotEmpty() }?.let(::add)
        if (showEmail) profile.email.trim().takeIf { it.isNotEmpty() }?.let(::add)
        if (showGithub) profile.githubUrl?.let(::add)
    }
    if (lines.isEmpty()) return null
    val avatar = if (showAvatar) {
        profile.avatarPath.trim().takeIf { it.isNotEmpty() }
    } else {
        null
    }
    return ExportCard(
        lines = lines,
        // 頭像跟著名片走：勾了才帶，路徑不存在時渲染端自己會退回純文字。
        avatarPath = avatar,
        position = position
    )
}
