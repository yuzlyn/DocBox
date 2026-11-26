package com.yuzlyn.mdeditor.ui.i18n

import com.yuzlyn.mdeditor.data.settings.AppLanguage

/** 說明頁的一個段落：小標 + 說明文字 + 條列。 */
data class HelpSection(
    val heading: String,
    val paragraph: String? = null,
    val bullets: List<String> = emptyList()
)

/** 說明頁的完整 README 文案（含頁尾連結）。 */
data class HelpDoc(
    val tagline: String,
    val intro: String,
    val sections: List<HelpSection>,
    val linksTitle: String,
    val repoLabel: String,
    val githubLabel: String
)

/**
 * 專案與作者的公開資訊。
 *
 * 說明頁的頁尾與作者頁都從這裡拿。要更新個人資料只改這一處——
 * 分散在各個畫面裡的話，下次改信箱就會漏掉一頁。
 */
const val PROJECT_REPO_URL = "https://github.com/yuzlyn/DocBox"
const val AUTHOR_GITHUB_URL = "https://github.com/yuzlyn"
const val AUTHOR_GITHUB_USER = "yuzlyn"
const val AUTHOR_EMAIL = "cebrayu@qq.com"

fun AppLanguage.helpDoc(): HelpDoc = when (this.resolved()) {
    AppLanguage.SYSTEM -> ZhTwHelp
    AppLanguage.ZH_TW -> ZhTwHelp
    AppLanguage.ZH_CN -> ZhCnHelp
    AppLanguage.EN_US -> EnUsHelp
    AppLanguage.FR_FR -> FrFrHelp
}

private val ZhTwHelp = HelpDoc(
    tagline = "資料夾裡的 Markdown 筆記",
    intro = "筆記是 .md 檔，存在你指定的資料夾。不上傳，不需帳號。",
    sections = listOf(
        HelpSection(
            heading = "功能",
            bullets = listOf(
                "Markdown 邊寫邊預覽：標題、清單、表格、程式碼、腳註。",
                "自動儲存：停手約一秒寫回 .md。",
                "橫排、直排切換；直排可顯示行號。",
                "匯入 Word、PDF、EPUB、HTML、Markdown、純文字。",
                "可從其他 App 的「開啟方式」直接送進來：Word、PDF、HTML、Markdown、純文字或電子書，不必先存到資料夾。",
                "書庫與閱讀器：EPUB、MOBI、FB2。",
                "匯出 Markdown、純文字、Word、PDF、圖片。",
                "標籤、全文搜尋、批次刪除。",
                "深色模式與莫奈取色。",
                "開屏圖示與底色跟隨莫奈與亮暗模式；平板、分屏與橫屏自動調整版心、欄數與導覽。",
                "所有可用按鈕與可點卡片都有 Material 3 按壓動畫；禁用項不會誤觸。"
            )
        ),
        HelpSection(
            heading = "開始使用",
            bullets = listOf(
                "第一次開啟時選擇筆記資料夾。",
                "資料夾可隨時在側邊選單更換。",
                "點右下角新增筆記；頂端切換預覽。"
            )
        ),
        HelpSection(
            heading = "資料與隱私",
            paragraph = "筆記是資料夾裡的 .md。App 只在本機存標題、標籤等資訊，沒有網路權限。"
        ),
        HelpSection(
            heading = "圖示字源",
            paragraph = "桌面圖示的字形取自 hanzi-glyphs-svg（漢典摹本 CC0；JGW 甲骨骨架 1,203 條 CC BY 4.0，Li Qing-sheng、Bian Yu-lin，DOI 10.5281/zenodo.22467946）。"
        ),
        HelpSection(
            heading = "支持作者",
            paragraph = "沒有廣告、訂閱與帳號。側欄「作者」頁有收款碼，自願支持。"
        )
    ),
    linksTitle = "專案連結",
    repoLabel = "專案倉庫",
    githubLabel = "yuzlyn"
)

private val ZhCnHelp = HelpDoc(
    tagline = "文件夹里的 Markdown 笔记",
    intro = "笔记是 .md 文件，存在你指定的文件夹。不上传，不需要账号。",
    sections = listOf(
        HelpSection(
            heading = "功能",
            bullets = listOf(
                "Markdown 边写边预览：标题、列表、表格、代码、脚注。",
                "自动保存：停手约一秒写回 .md。",
                "横排、竖排切换；竖排可显示行号。",
                "导入 Word、PDF、EPUB、HTML、Markdown、纯文字。",
                "可从其他应用的「打开方式」直接送进来：Word、PDF、HTML、Markdown、纯文字或电子书，不必先存到文件夹。",
                "书库与阅读器：EPUB、MOBI、FB2。",
                "导出 Markdown、纯文字、Word、PDF、图片。",
                "标签、全文搜索、批量删除。",
                "深色模式与莫奈取色。",
                "开屏图标与底色跟随莫奈与亮暗模式；平板、分屏与横屏自动调整版心、列数与导航。",
                "所有可用按钮与可点卡片都有 Material 3 按压动画；禁用项不会误触。"
            )
        ),
        HelpSection(
            heading = "开始使用",
            bullets = listOf(
                "第一次打开时选择笔记文件夹。",
                "文件夹可随时在侧边菜单更换。",
                "点右下角新增笔记；顶部切换预览。"
            )
        ),
        HelpSection(
            heading = "字表格式",
            paragraph = "UTF-8 纯文字，一行一个字。",
            bullets = listOf(
                "名次写成「字 + Tab + 序号」。",
                "「#」开头是注释。",
                "序号相同的繁简与异体算同一格。",
                "字数 10 到 20000；出错会指出行号。"
            )
        ),
        HelpSection(
            heading = "数据与隐私",
            paragraph = "笔记是文件夹里的 .md。App 只在本机存标题、标签等信息，没有网络权限。"
        ),
        HelpSection(
            heading = "图标字源",
            paragraph = "桌面图标字形取自 hanzi-glyphs-svg（汉典摹本 CC0；JGW 甲骨骨架 1,203 条 CC BY 4.0，Li Qing-sheng、Bian Yu-lin，DOI 10.5281/zenodo.22467946）。"
        ),
        HelpSection(
            heading = "支持作者",
            paragraph = "没有广告、订阅与账号。侧栏「作者」页有收款码，自愿支持。"
        )
    ),
    linksTitle = "项目链接",
    repoLabel = "项目仓库",
    githubLabel = "yuzlyn"
)

private val EnUsHelp = HelpDoc(
    tagline = "Markdown notes in your own folder",
    intro = "Notes are .md files in a folder you pick. Nothing is uploaded, no account needed.",
    sections = listOf(
        HelpSection(
            heading = "Features",
            bullets = listOf(
                "Markdown with live preview: headings, lists, tables, code, footnotes.",
                "Autosave: writes back to the .md about a second after you stop typing.",
                "Horizontal or vertical text; line numbers in vertical.",
                "Import Word, PDF, EPUB, HTML, Markdown, plain text.",
                "Send files straight from another app's Open with: Word, PDF, HTML, Markdown, plain text or ebooks — no need to copy them into the folder first.",
                "Library and reader for EPUB, MOBI and FB2.",
                "Export Markdown, plain text, Word, PDF or images.",
                "Tags, full-text search, bulk delete.",
                "Dark mode and Monet colour.",
                "Splash colours follow Monet and light/dark mode; content width, grids and navigation adapt to tablets, split screen and landscape.",
                "All enabled buttons and tappable cards have Material 3 press feedback; disabled controls cannot be activated."
            )
        ),
        HelpSection(
            heading = "Get started",
            bullets = listOf(
                "Pick a notes folder on first launch.",
                "Change the folder any time from the sidebar.",
                "Tap + to add a note; switch preview at the top."
            )
        ),
        HelpSection(
            heading = "Data and privacy",
            paragraph = "Notes are .md files in your folder. The app stores only local metadata and asks for no network access."
        ),
        HelpSection(
            heading = "Sources",
            paragraph = "Launcher icon glyphs from hanzi-glyphs-svg (Handian tracings CC0; 1,203 JGW oracle skeletons CC BY 4.0 by Li Qing-sheng and Bian Yu-lin, DOI 10.5281/zenodo.22467946)."
        ),
        HelpSection(
            heading = "Support",
            paragraph = "No ads, no subscription, no account. Payment codes are on the Author page in the sidebar, if you want to support."
        )
    ),
    linksTitle = "Project links",
    repoLabel = "Repository",
    githubLabel = "yuzlyn"
)

private val FrFrHelp = HelpDoc(
    tagline = "Des notes Markdown dans votre dossier",
    intro = "Les notes sont des fichiers .md dans un dossier que vous choisissez. Rien n'est envoyé, aucun compte requis.",
    sections = listOf(
        HelpSection(
            heading = "Fonctions",
            bullets = listOf(
                "Markdown avec aperçu en direct : titres, listes, tableaux, code, notes.",
                "Enregistrement auto : le .md est écrit une seconde après la dernière frappe.",
                "Texte horizontal ou vertical ; numéros de ligne en vertical.",
                "Importer Word, PDF, EPUB, HTML, Markdown, texte brut.",
                "Ouvrez directement depuis « Ouvrir avec » d'une autre application : Word, PDF, HTML, Markdown, texte brut ou livres — sans les copier d'abord dans le dossier.",
                "Bibliothèque et lecteur pour EPUB, MOBI et FB2.",
                "Exporter en Markdown, texte brut, Word, PDF ou images.",
                "Étiquettes, recherche plein texte, suppression en lot.",
                "Mode sombre et couleurs Monet.",
                "L'écran de démarrage suit Monet et le mode clair/sombre ; largeur du contenu, grilles et navigation s'adaptent aux tablettes, à l'écran partagé et au mode paysage.",
                "Tous les boutons actifs et les cartes tactiles ont un retour Material 3 ; les contrôles désactivés ne répondent pas."
            )
        ),
        HelpSection(
            heading = "Premiers pas",
            bullets = listOf(
                "Choisissez un dossier de notes au premier lancement.",
                "Changez de dossier depuis le panneau latéral.",
                "Touchez + pour ajouter une note ; l'aperçu se bascule en haut."
            )
        ),
        HelpSection(
            heading = "Données et confidentialité",
            paragraph = "Les notes sont des .md dans votre dossier. L'application ne garde que des métadonnées locales et ne demande aucun accès réseau."
        ),
        HelpSection(
            heading = "Sources",
            paragraph = "Glyphes des icônes de lancement : hanzi-glyphs-svg (tracés Handian CC0 ; 1 203 squelettes oracle JGW CC BY 4.0, Li Qing-sheng et Bian Yu-lin, DOI 10.5281/zenodo.22467946)."
        ),
        HelpSection(
            heading = "Soutenir l'auteur",
            paragraph = "Pas de publicité, d'abonnement ni de compte. Les codes de paiement sont sur la page Auteur du panneau latéral."
        )
    ),
    linksTitle = "Liens du projet",
    repoLabel = "Dépôt",
    githubLabel = "yuzlyn"
)
