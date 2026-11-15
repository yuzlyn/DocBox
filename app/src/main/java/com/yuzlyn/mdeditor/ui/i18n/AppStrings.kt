package com.yuzlyn.mdeditor.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.yuzlyn.mdeditor.data.settings.AppLanguage

/**
 * 介面文字表。
 *
 * 為什麼不是 `data class`：欄位已經超過 350 個，而 JVM 的方法簽章上限是 255 個參數槽，
 * 再多一個就會 `ClassFormatError: Too many arguments in method signature`——編譯過得去，
 * 類別載入時才炸，而且只在 JVM（單元測試）上炸，實機反而不一定看得出來。
 *
 * 所以改成無參數建構子 + `lateinit var`，四個語種用 `apply { }` 填值：
 * 欄位再多也不會撞上限，忘了填哪個欄位會直接丟 `UninitializedPropertyAccessException`，
 * 不會悄悄顯示空白。填完之後沒有地方會再改它（`AppStringsFillTest` 盯著這件事）。
 */
class AppStrings {
    lateinit var appName: String
    lateinit var allNotes: String
    lateinit var notebooks: String
    lateinit var changeFolder: String
    lateinit var settings: String
    lateinit var help: String
    lateinit var searchNotes: String
    lateinit var searchNotebooks: String
    lateinit var selectedCount: (Int) -> String
    lateinit var share: String
    lateinit var export: String
    lateinit var delete: String
    lateinit var noNotes: String
    lateinit var noNotebooks: String
    lateinit var noMatchNotes: String
    lateinit var noMatchNotebooks: String
    lateinit var tryOtherKeywords: String
    lateinit var createFirstNote: String
    lateinit var notebookHint: String
    lateinit var notesCount: (Int) -> String
    lateinit var resultsCount: (Int) -> String
    lateinit var welcomeTitle: String
    lateinit var welcomeBody: String
    lateinit var chooseFolder: String
    lateinit var titlePlaceholder: String
    lateinit var bodyPlaceholder: String
    lateinit var preview: String
    lateinit var backToEdit: String
    lateinit var previewSettings: String
    lateinit var insert: String
    lateinit var style: String
    lateinit var format: String
    lateinit var more: String
    lateinit var image: String
    lateinit var codeBlock: String
    lateinit var table: String
    lateinit var link: String
    lateinit var divider: String
    lateinit var backgroundColor: String
    lateinit var backgroundImage: String
    lateinit var lastEdited: (String) -> String
    lateinit var label: String
    lateinit var send: String
    lateinit var duplicate: String
    lateinit var find: String
    lateinit var cancel: String
    lateinit var done: String
    lateinit var create: String
    lateinit var newNote: String
    lateinit var noteTitle: String
    lateinit var untitled: String
    lateinit var notebookCategory: String
    lateinit var newCategoryName: String
    lateinit var newCategory: String
    lateinit var settingsTitle: String
    lateinit var language: String
    /** 語言清單第一列：跟隨系統。 */
    lateinit var languageFollowSystem: String
    /** 資料夾沒名字（舊資料或 provider 不給名）時的顯示名。 */
    lateinit var libraryFolderFallback: String
    /** 匯入的書連檔名都問不到時的預設名稱（會變成檔案名，帶副檔名）。 */
    lateinit var libraryUntitledBook: String
    /** 跟隨系統時，底下那行小字說明「系統目前是什麼」。 */
    lateinit var languageFollowSystemHint: (String) -> String
    lateinit var theme: String
    lateinit var monet: String
    lateinit var customMonet: String
    lateinit var darkMode: String
    lateinit var followSystem: String
    lateinit var dark: String
    lateinit var light: String
    lateinit var seedColors: String
    /** 「設定 → 主題」裡的圖標字形選擇。 */
    lateinit var launcherIcon: String
    lateinit var glyphXia: String
    lateinit var glyphWeibo: String
    lateinit var glyphZengzhong: String
    lateinit var glyphLusitu: String
    lateinit var glyphShuowengu: String
    lateinit var glyphShuowenzhu: String
    lateinit var colorGuide: String
    lateinit var about: String
    lateinit var back: String
    lateinit var clear: String
    lateinit var openFolder: String
    lateinit var account: String
    lateinit var menu: String
    lateinit var toggleLayout: String
    lateinit var sort: String
    lateinit var sortTitle: String
    lateinit var sortCustom: String
    lateinit var sortCreated: String
    lateinit var sortModified: String
    lateinit var profileTitle: String
    lateinit var nickname: String
    lateinit var email: String
    lateinit var github: String
    /** 名片頁 GitHub 欄位的說明：只要填使用者名稱，分享時會補成完整網址。 */
    lateinit var githubHint: String
    lateinit var githubPreview: (String) -> String
    lateinit var avatar: String
    lateinit var uploadAvatar: String
    lateinit var searchPageTitle: String
    lateinit var cardShape: String
    lateinit var noteBackground: String
    lateinit var font: String
    lateinit var systemFont: String
    /** 字型設定頁：字型庫、匯入、兩個預設（編輯／閱讀）。 */
    lateinit var fontLibrary: String
    lateinit var fontImport: String
    lateinit var fontImportHint: String
    lateinit var fontDefaultEdit: String
    lateinit var fontDefaultReader: String
    lateinit var fontEmpty: String
    lateinit var fontImportFailed: String
    lateinit var fontDeleteTitle: String
    lateinit var fontDeleteMessage: (String) -> String
    /** 筆記背景設定頁：預設／純色／圖片。 */
    lateinit var noteBackgroundPage: String
    lateinit var noteBackgroundDefault: String
    lateinit var noteBackgroundColor: String
    lateinit var noteBackgroundEmpty: String
    lateinit var noteBackgroundImport: String
    lateinit var noteBackgroundDeleteTitle: String
    lateinit var noteBackgroundDeleteMessage: (String) -> String
    /** 背景圖片上的文字顏色：跟隨主題／深色字／淺色字。 */
    lateinit var noteBackgroundInk: String
    lateinit var noteBackgroundInkAuto: String
    lateinit var noteBackgroundInkDark: String
    lateinit var noteBackgroundInkLight: String
    lateinit var noteBackgroundInkHint: String
    lateinit var cardShapeDefault: String
    lateinit var cardShapeRounded: String
    lateinit var cardShapeExtraRounded: String
    lateinit var cardShapeSquare: String
    lateinit var layout: String
    lateinit var defaultLayout: String
    lateinit var horizontalLayout: String
    lateinit var verticalLayoutLabel: String
    lateinit var layoutHint: String
    lateinit var lineNumbersLabel: String
    lateinit var lineNumbersSummary: String
    lateinit var writingGuide: String
    lateinit var guideNone: String
    lateinit var guideRuled: String
    lateinit var guideGrid: String
    lateinit var guideFollowDefault: String
    lateinit var guideHint: String
    /** 閱讀器：目錄按鈕與目錄面板標題。 */
    lateinit var readerContents: String
    lateinit var readerFontSize: String
    lateinit var readerOpening: String
    lateinit var readerOpenFailed: String
    /** 閱讀進度，例如「3 / 210 頁」。 */
    lateinit var readerProgress: (Int, Int) -> String
    /** 目錄裡沒有標題時退用的章號，例如「第 3 章」。 */
    lateinit var readerChapter: (Int) -> String
    /** 排版面板標題（翻頁方式／行距／留白）。 */
    lateinit var readerLayout: String
    lateinit var readerModePaged: String
    /** 連續滾動，往下滑。 */
    lateinit var readerModeScrollVertical: String
    /** 連續滾動，往左滑（直排就是從右往左）。 */
    lateinit var readerModeScrollHorizontal: String
    lateinit var readerLineHeight: String
    lateinit var readerMargin: String

    /** 書庫（匯入的電子書存放處）。 */
    lateinit var library: String
    lateinit var libraryEmpty: String
    lateinit var libraryEmptyHint: String
    /** 從檔案匯入一本書。 */
    lateinit var libraryImport: String
    /** 從整個資料夾匯入。 */
    lateinit var libraryImportFolder: String
    /** 書庫頂欄的搜尋提示（搜書名／作者）。 */
    lateinit var bookSearch: String
    /** 書庫裡「不篩資料夾」的那一檔。 */
    lateinit var libraryAllBooks: String
    /** 書庫的第二種視圖：以資料夾為單位。 */
    lateinit var libraryFolders: String
    /** 把一個資料夾加進書庫（長期追蹤，之後自動讀取）。 */
    lateinit var libraryNewFolder: String
    lateinit var libraryNoMatch: String
    /** 沒有登記任何外部資料夾時，App 自己那份目錄的稱呼。 */
    lateinit var libraryLocalShelf: String
    /** 資料夾卡片上的本數，例如「12 本」。 */
    lateinit var libraryFolderBooks: (Int) -> String
    /** 資料夾面板的說明：資料夾只是「自動收書」的來源。 */
    lateinit var libraryFolderHint: String
    /** 還沒有登記任何資料夾。 */
    lateinit var libraryNoFolderYet: String
    /** 一次同步新收進來的本數。 */
    lateinit var librarySyncAdded: (Int) -> String
    /** 資料夾授權失效（被系統收回、或被使用者移除）。 */
    lateinit var libraryFolderDenied: String
    /** 設定 → 啟動頁。 */
    lateinit var startPage: String
    lateinit var startPageHint: String
    lateinit var startPageNotes: String
    lateinit var startPageLibrary: String
    /** 抽屜裡的「資料夾」：管理登記過的書庫資料夾。 */
    lateinit var libraryManageFolders: String
    /** 書庫排序：最近閱讀。 */
    lateinit var sortRecent: String
    /** 書庫排序：書名。 */
    lateinit var sortByTitle: String
    /** 書庫排序：加入時間。 */
    lateinit var sortByAdded: String
    /** 移除資料夾時的說明（書不會被刪）。 */
    lateinit var libraryRemoveFolderHint: String

    /** 統計頁與統計設定（見 ui/stats）。 */
    lateinit var stats: String
    lateinit var statsTitle: String
    lateinit var statsRange7: String
    lateinit var statsRange30: String
    lateinit var statsRange365: String
    lateinit var statsReadingTime: String
    lateinit var statsBooks: String
    lateinit var statsDocs: String
    lateinit var statsEdits: String
    lateinit var statsDistribution: String
    lateinit var statsMonthly: String
    lateinit var statsActivity: String
    lateinit var statsActivityHint: String
    lateinit var statsLess: String
    lateinit var statsMore: String
    lateinit var statsRanking: String
    lateinit var statsRankingBooks: String
    lateinit var statsRankingDocs: String
    lateinit var statsEmpty: String
    lateinit var statsEmptyHint: String
    lateinit var statsDisabled: String
    lateinit var statsDisabledHint: String
    lateinit var statsEnable: String
    lateinit var statsSettingsTitle: String
    lateinit var statsEnabledLabel: String
    lateinit var statsEnabledSummary: String
    lateinit var statsTrackReading: String
    lateinit var statsTrackReadingSummary: String
    lateinit var statsTrackDocuments: String
    lateinit var statsTrackDocumentsSummary: String
    lateinit var statsShowHeatmap: String
    lateinit var statsShowRanking: String
    lateinit var statsClearData: String
    lateinit var statsClearDataSummary: String
    lateinit var statsCleared: String
    lateinit var statsDuration: (Long) -> String
    lateinit var statsStartFrom: (String) -> String
    lateinit var statsTimesCount: (Int) -> String
    lateinit var statsBooksCount: (Int) -> String
    lateinit var statsDocsCount: (Int) -> String

    /** 統計頁頂部的個人問候（見 ui/stats/StatsScreen）。 */
    lateinit var statsGreeting: (String) -> String
    lateinit var statsGreetingGuest: String
    lateinit var statsGreetingHint: String

    /** 分享面板裡的「個人名片」區塊。 */
    lateinit var cardSectionTitle: String
    lateinit var cardNotSet: String
    lateinit var cardAllOff: String
    lateinit var cardNotAttached: String
    lateinit var cardPositionLabel: String
    lateinit var cardPositionBefore: String
    lateinit var cardPositionAfter: String
    /** 直排筆記：名片位置不可選，固定排在正文之後。 */
    lateinit var cardVerticalNote: String
    lateinit var cardAvatar: String
    lateinit var cardAvatarHint: String
    lateinit var cardNickname: String
    lateinit var cardEmail: String
    lateinit var cardGithub: String

    /** 作者頁（見 ui/screen/AuthorScreen）。 */
    lateinit var authorTitle: String
    lateinit var authorRole: String
    lateinit var authorEmail: String
    lateinit var authorGithub: String
    lateinit var authorRepo: String
    lateinit var authorBio: String
    /** 作者頁下方的「支持作者」區塊：兩張收款碼。 */
    lateinit var authorSupportTitle: String
    /** 作者頁那條入口的副標題（收款碼放在獨立頁，條上只給一句話）。 */
    lateinit var authorSupportEntry: String
    lateinit var authorSupportLead: String
    lateinit var authorSupportWechat: String
    lateinit var authorSupportAlipay: String
    lateinit var authorSupportZoom: String

    // ── 資料層丟上來的錯誤：這裡只放句子，對應關係在 LocalizedLabels.kt ──
    /** 開書失敗（EPUB／MOBI／FB2 解析、快取、授權）。 */
    lateinit var bookErrUnknownFormat: String
    lateinit var bookErrInvalidPath: String
    lateinit var bookErrFileNotFound: String
    lateinit var bookErrFileUnreadable: String
    lateinit var bookErrFileEmpty: String
    lateinit var bookErrFileTooSmall: String
    lateinit var bookErrNotEpub: String
    lateinit var bookErrNotMobi: String
    lateinit var bookErrNotFb2: String
    lateinit var bookErrEpubRootfile: String
    lateinit var bookErrEpubPackage: String
    lateinit var bookErrNoContent: String
    lateinit var bookErrChapterMissing: String
    lateinit var bookErrChapterUnreadable: String
    lateinit var bookErrDrm: String
    lateinit var bookErrCorrupt: String
    lateinit var bookErrCompression: String
    lateinit var bookErrParse: String
    lateinit var bookErrOpen: String
    /** 匯入檔案失敗。 */
    lateinit var importErrUnreadable: String
    lateinit var importErrPdfNoExtension: String
    lateinit var importErrWord: String
    lateinit var importErrEmpty: String
    lateinit var importErrHtmlEmpty: String
    lateinit var importErrEbook: String
    lateinit var importErrUnsupported: String
    lateinit var importErrPdfUnreadable: String
    lateinit var importErrPdfNoText: String
    lateinit var importErrPdfParse: String
    /** 插入圖片失敗。 */
    lateinit var imageErrNotReady: String
    lateinit var imageErrPermission: String
    lateinit var imageErrUnknown: String
    lateinit var imageErrNotWritable: String
    lateinit var imageErrFormat: String
    lateinit var imageErrDir: String
    lateinit var imageErrCreate: String
    lateinit var imageErrCopy: String

    /** 編輯頁的格式選單：插入什麼、叫什麼名字。 */
    lateinit var editorFormatH1: String
    lateinit var editorFormatH2: String
    lateinit var editorFormatH3: String
    lateinit var editorFormatBold: String
    lateinit var editorFormatItalic: String
    lateinit var editorFormatStrike: String
    lateinit var editorFormatHighlight: String
    lateinit var editorFormatInlineCode: String
    lateinit var editorFormatQuote: String
    lateinit var editorFormatBullet: String
    lateinit var editorFormatOrdered: String
    lateinit var editorFormatTask: String
    lateinit var editorFormatMath: String
    lateinit var editorFormatMathBlock: String

    /** 編輯頁工具列與選單上的動作。 */
    lateinit var editorUndo: String
    lateinit var editorRedo: String
    lateinit var editorSaveCopy: String
    lateinit var editorMathSymbols: String
    /** 把筆記與原始 PDF 關聯起來、以及關聯後開啟它。 */
    lateinit var editorLinkPdf: String
    lateinit var editorOpenPdf: String

    /** 編輯頁的儲存狀態與操作結果。 */
    lateinit var editorSaveRecovered: String
    lateinit var editorSaveFailed: String
    lateinit var editorSaved: String
    lateinit var msgCannotOpenFile: String
    lateinit var msgCopySaved: String
    lateinit var msgCopySaveFailed: String
    lateinit var msgUnknownError: String
    lateinit var msgImageTruncated: String
    lateinit var msgNoShareTarget: String
    lateinit var msgSystemUnsupported: String
    lateinit var msgExportFailed: (String) -> String
    lateinit var msgShareFailed: (String) -> String
    lateinit var msgImagePickerFailed: (String) -> String

    /** 匯出面板。 */
    lateinit var exportChooseFormat: String
    lateinit var exportHint: String
    lateinit var exportDirectionVertical: String
    lateinit var exportDirectionHorizontal: String

    /** 預覽設定對話框。 */
    lateinit var previewFontSize: String
    lateinit var previewSyntax: String
    lateinit var previewCodeHighlight: String
    lateinit var previewHeadingAnchors: String
    lateinit var previewMath: String
    lateinit var previewToc: String

    /** PDF 檢視頁。 */
    lateinit var pdfOpenFailed: String
    lateinit var pdfImportAsNote: String
    lateinit var pdfPage: (Int) -> String

    /** 閱讀器的紙張主題名稱。 */
    lateinit var readerThemeDefault: String
    lateinit var readerThemePlain: String
    lateinit var readerThemeTinted: String
    lateinit var readerThemeDim: String
    lateinit var readerThemeInk: String

    /** 檔案操作失敗時給使用者看的話。 */
    lateinit var msgNoFolder: String
    lateinit var msgCannotCreateNote: String
    lateinit var msgWriteFailed: String
    lateinit var msgNoPersistablePermission: String
    lateinit var msgCannotReadFile: String
    lateinit var msgPdfLinked: String
    /** 匯入時還沒讀到檔名，先用這個當標題。 */
    lateinit var defaultImportTitle: String

    /** 首頁與分類。 */
    lateinit var importDocument: String
    lateinit var defaultNotebook: String

    /** 統計熱力圖的星期標籤，週一到週日七項。 */
    lateinit var statsWeekdays: List<String>

    /** 筆記背景的固定色名稱。 */
    lateinit var backdropDefault: String
    lateinit var backdropRice: String
    lateinit var backdropCream: String
    lateinit var backdropMint: String
    lateinit var backdropSky: String
    lateinit var backdropRose: String
    lateinit var backdropGraphite: String
    lateinit var backdropInk: String

    /** 匯出格式名稱。 */
    lateinit var formatPlainText: String
    lateinit var formatWord: String
    lateinit var formatPdf: String
    lateinit var formatImage: String

    /** 預覽字級檔位。 */
    lateinit var fontSizeSmall: String
    lateinit var fontSizeStandard: String
    lateinit var fontSizeLarge: String
    lateinit var fontSizeHuge: String
}

val ZhTwStrings = AppStrings().apply {
    appName = "文書匣子"
    allNotes = "全部筆記"
    notebooks = "筆記本"
    changeFolder = "更換資料夾"
    settings = "設定"
    help = "說明"
    searchNotes = "搜尋筆記"
    searchNotebooks = "搜尋筆記本"
    selectedCount = { "已選 $it 項" }
    share = "分享"
    export = "匯出"
    delete = "刪除"
    noNotes = "尚無筆記"
    noNotebooks = "尚無筆記本"
    noMatchNotes = "找不到符合的筆記"
    noMatchNotebooks = "找不到符合的筆記本"
    tryOtherKeywords = "試試其他關鍵字"
    createFirstNote = "點擊右下角 + 按鈕建立第一條筆記"
    notebookHint = "在筆記中設定分類後會顯示在這裡"
    notesCount = { "$it 篇筆記" }
    resultsCount = { "$it 筆結果" }
    welcomeTitle = "歡迎使用文書匣子"
    welcomeBody = "請先選擇一個存放 Markdown 文件的資料夾，\n應用將自動讀取其中所有 .md 檔案"
    chooseFolder = "選擇資料夾"
    titlePlaceholder = "標題"
    bodyPlaceholder = "開始寫作 Markdown…"
    preview = "預覽"
    backToEdit = "返回編輯"
    previewSettings = "預覽設定"
    insert = "插入"
    style = "樣式"
    format = "格式"
    more = "更多"
    image = "圖片"
    codeBlock = "程式碼區塊"
    table = "表格"
    link = "連結"
    divider = "分隔線"
    backgroundColor = "背景顏色"
    backgroundImage = "背景圖片"
    lastEdited = { "上次編輯 $it" }
    label = "標籤"
    send = "傳送"
    duplicate = "建立副本"
    find = "尋找"
    cancel = "取消"
    done = "完成"
    create = "建立"
    newNote = "新建筆記"
    noteTitle = "筆記標題"
    untitled = "未命名筆記"
    notebookCategory = "筆記本分類"
    newCategoryName = "新分類名稱"
    newCategory = "+ 新建分類"
    settingsTitle = "設定"
    language = "語言"
    languageFollowSystem = "跟隨系統語言"
    libraryFolderFallback = "資料夾"
    libraryUntitledBook = "未命名.epub"
    languageFollowSystemHint = { "系統目前為 $it" }
    theme = "主題"
    monet = "莫奈取色"
    customMonet = "自訂莫奈色"
    darkMode = "深色模式"
    followSystem = "跟隨系統"
    dark = "深色"
    light = "淺色"
    seedColors = "種子色"
    launcherIcon = "圖標字形"
    glyphXia = "匣"
    glyphWeibo = "微伯簋"
    glyphZengzhong = "曾仲游"
    glyphLusitu = "魯大司徒"
    glyphShuowengu = "說文古文"
    glyphShuowenzhu = "說文竹部"
    colorGuide = "莫奈取色會在 Android 12 以上直接採用系統壁紙配色；自訂莫奈色以所選種子色派生 Expressive 色票，對比度固定為 0.5。所有介面顏色都使用 Material 3 的 color role，不會寫死十六進位色值。"
    about = "關於"
    back = "返回"
    clear = "清除"
    openFolder = "打開資料夾"
    account = "帳戶"
    menu = "選單"
    toggleLayout = "切換排版"
    sort = "排序"
    sortTitle = "排序依據"
    sortCustom = "自訂（預設）"
    sortCreated = "建立日期"
    sortModified = "修改日期"
    profileTitle = "個人名片"
    nickname = "暱稱"
    email = "電子郵件"
    github = "GitHub"
    githubHint = "填使用者名稱就好，會自動補成 github.com/…"
    githubPreview = { handle -> "分享名片上顯示 github.com/$handle" }
    avatar = "頭像"
    uploadAvatar = "上傳頭像"
    searchPageTitle = "搜尋"
    cardShape = "卡片形狀"
    noteBackground = "筆記背景圖片"
    font = "字體"
    systemFont = "系統字體"
    fontLibrary = "字型庫"
    fontImport = "匯入字型"
    fontImportHint = "支援 .ttf 與 .otf"
    fontDefaultEdit = "編輯預設字型"
    fontDefaultReader = "閱讀預設字型"
    fontEmpty = "還沒有匯入字型。匯入後這裡會列出它們，套用時立刻看得到差別。"
    fontImportFailed = "這個檔案讀不出字型"
    fontDeleteTitle = "刪除字型？"
    fontDeleteMessage = { name -> "「$name」會從字型庫移除。正在使用它的地方會退回系統字型。" }
    noteBackgroundPage = "筆記背景"
    noteBackgroundDefault = "預設（莫奈）"
    noteBackgroundColor = "純色"
    noteBackgroundEmpty = "還沒有匯入圖片，點右邊的加號挑一張。"
    noteBackgroundImport = "匯入圖片"
    noteBackgroundDeleteTitle = "刪除圖片？"
    noteBackgroundDeleteMessage = { name -> "「$name」會從背景圖庫移除，之後可以重新匯入。" }
    noteBackgroundInk = "文字顏色"
    noteBackgroundInkAuto = "跟隨主題"
    noteBackgroundInkDark = "深色字"
    noteBackgroundInkLight = "淺色字"
    noteBackgroundInkHint = "只對背景圖片生效：照片偏亮選深色字，偏暗選淺色字。"
    cardShapeDefault = "默認"
    cardShapeRounded = "圓潤"
    cardShapeExtraRounded = "更圓"
    cardShapeSquare = "方形"
    layout = "排版"
    defaultLayout = "預設排版"
    horizontalLayout = "橫排"
    verticalLayoutLabel = "直排"
    layoutHint = "新筆記與還沒指定排版的筆記都用這個預設；單篇排版在編輯頁的「排版」切換，會各自記住。"
    lineNumbersLabel = "顯示行號"
    lineNumbersSummary = "直排編輯區每欄顯示行號，折行續欄不編號"
    writingGuide = "書寫輔助線"
    guideNone = "無"
    guideRuled = "分隔線"
    guideGrid = "作文格"
    guideFollowDefault = "跟隨默認"
    guideHint = "只影響沒有單獨設定過輔助線的筆記；橫排是橫線，直排是豎線。"
    readerContents = "目錄"
    readerFontSize = "字號"
    readerOpening = "正在開啟…"
    readerOpenFailed = "打不開這本書"
    readerProgress = { page, pages -> "$page / $pages 頁" }
    readerChapter = { "第 $it 章" }
    readerLayout = "排版"
    readerModePaged = "翻頁"
    readerModeScrollVertical = "上下滑動"
    readerModeScrollHorizontal = "左右滑動"
    readerLineHeight = "行距"
    readerMargin = "頁邊留白"
    library = "書庫"
    libraryEmpty = "書庫是空的"
    libraryEmptyHint = "用上面的「匯入」把電子書放進來。書會複製到 App 自己的目錄，離開閱讀器也不會消失。"
    libraryImport = "匯入書籍"
    libraryImportFolder = "匯入資料夾"
    bookSearch = "搜尋書名或作者"
    libraryAllBooks = "全部圖書"
    libraryFolders = "資料夾"
    libraryNewFolder = "新增資料夾"
    libraryNoMatch = "沒有符合的書"
    libraryLocalShelf = "本機書庫"
    libraryFolderBooks = { "$it 本" }
    librarySyncAdded = { "新收進 $it 本" }
    libraryFolderHint = "資料夾裡的新書會自動收進書庫，不必一本一本匯入。"
    libraryNoFolderYet = "還沒有登記資料夾"
    libraryFolderDenied = "資料夾授權失效，請重新新增"
    startPage = "啟動頁"
    startPageHint = "下次開啟 App 時從這裡開始"
    startPageNotes = "全部筆記"
    startPageLibrary = "書庫 · 全部圖書"
    libraryManageFolders = "資料夾"
    sortRecent = "最近閱讀"
    sortByTitle = "書名"
    sortByAdded = "加入時間"
    libraryRemoveFolderHint = "只移除資料夾登記，書仍留在書庫裡。"
    stats = "統計"
    statsTitle = "閱讀統計"
    statsRange7 = "7 天"
    statsRange30 = "30 天"
    statsRange365 = "1 年"
    statsReadingTime = "閱讀時長"
    statsBooks = "讀過的書"
    statsDocs = "看過的文件"
    statsEdits = "編輯次數"
    statsDistribution = "每日分佈"
    statsMonthly = "每月分佈"
    statsActivity = "活躍度"
    statsActivityHint = "一格一天，顏色越深表示那天讀得越多、寫得越多。"
    statsLess = "少"
    statsMore = "多"
    statsRanking = "排行榜"
    statsRankingBooks = "書籍"
    statsRankingDocs = "文件"
    statsEmpty = "還沒有紀錄"
    statsEmptyHint = "打開一本書，或編輯一份筆記，這裡就會開始累積。"
    statsDisabled = "統計已關閉"
    statsDisabledHint = "關掉之後不會記錄任何閱讀或編輯。你隨時可以在設定裡重新打開。"
    statsEnable = "開啟統計"
    statsSettingsTitle = "統計設定"
    statsEnabledLabel = "記錄統計"
    statsEnabledSummary = "關掉之後側欄不再顯示統計，也不再記錄任何東西。"
    statsTrackReading = "閱讀時長"
    statsTrackReadingSummary = "記下每本書讀了多久。"
    statsTrackDocuments = "文件編輯"
    statsTrackDocumentsSummary = "記下打開過、編輯過哪些筆記。"
    statsShowHeatmap = "活躍熱力圖"
    statsShowRanking = "排行榜"
    statsClearData = "清除統計資料"
    statsClearDataSummary = "刪掉全部紀錄，無法復原。"
    statsCleared = "統計已清除"
    statsDuration = { seconds ->
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        when {
            hours > 0 && minutes > 0 -> "$hours 小時 $minutes 分"
            hours > 0 -> "$hours 小時"
            minutes > 0 -> "$minutes 分鐘"
            seconds > 0 -> "不到 1 分鐘"
            else -> "—"
        }
    }
    statsStartFrom = { "從 $it 開始" }
    statsTimesCount = { "$it 次" }
    statsBooksCount = { "$it 本" }
    statsDocsCount = { "$it 份" }
    statsGreeting = { "嗨，$it" }
    statsGreetingGuest = "嗨，歡迎回來"
    statsGreetingHint = "今天想寫點什麼？"
    cardSectionTitle = "個人名片"
    cardNotSet = "尚未設定；可從右上角頭像填寫"
    cardAllOff = "已把所有欄位關掉"
    cardNotAttached = "這次不附名片"
    cardPositionLabel = "位置"
    cardPositionBefore = "正文之前"
    cardPositionAfter = "正文之後"
    cardVerticalNote = "直排筆記的名片固定排在正文之後"
    cardAvatar = "頭像"
    cardAvatarHint = "只在圖片格式顯示"
    cardNickname = "暱稱"
    cardEmail = "信箱"
    cardGithub = "GitHub"
    authorTitle = "作者"
    authorRole = "軟件工程在讀"
    authorEmail = "電子郵件"
    authorGithub = "GitHub"
    authorRepo = "專案倉庫"
    authorBio = "武漢理工大學軟件工程專業學生，歡迎支持反饋本應用"
    authorSupportTitle = "支持作者"
    authorSupportEntry = "微信與支付寶收款碼，掃哪張都算數"
    authorSupportLead = "兩張碼都在這，掃哪張都算數。點一下可以放大。"
    authorSupportWechat = "微信支付"
    authorSupportAlipay = "支付寶"
    authorSupportZoom = "點一下放大"
    bookErrUnknownFormat = "認不出這個檔案的格式"
    bookErrInvalidPath = "檔案路徑無效"
    bookErrFileNotFound = "找不到這個檔案"
    bookErrFileUnreadable = "讀不到這個檔案，授權可能已失效"
    bookErrFileEmpty = "這個檔案是空的"
    bookErrFileTooSmall = "檔案太小，不像完整的電子書"
    bookErrNotEpub = "這個檔案不是 EPUB"
    bookErrNotMobi = "這個檔案不是有效的 MOBI／AZW3"
    bookErrNotFb2 = "這個檔案不是 FB2 電子書"
    bookErrEpubRootfile = "EPUB 的 container.xml 沒指定內容清單"
    bookErrEpubPackage = "EPUB 的內容清單讀不出來"
    bookErrNoContent = "這本書沒有可讀的章節"
    bookErrChapterMissing = "這個章節不存在"
    bookErrChapterUnreadable = "章節檔案讀不出來"
    bookErrDrm = "這本書有 DRM 保護，無法開啟"
    bookErrCorrupt = "檔案結構不完整，可能下載不完整"
    bookErrCompression = "不支援這種壓縮方式"
    bookErrParse = "這本書解析失敗"
    bookErrOpen = "無法開啟這個檔案"
    importErrUnreadable = "讀不到這個檔案"
    importErrPdfNoExtension = "這個 PDF 沒有副檔名，請確認檔案完整"
    importErrWord = "這個檔案不是可讀取的 Word（.docx）文件"
    importErrEmpty = "這個檔案沒有內容"
    importErrHtmlEmpty = "這個 HTML 沒有可匯入的文字"
    importErrEbook = "這是電子書，請直接開啟閱讀"
    importErrUnsupported = "不支援這個檔案格式"
    importErrPdfUnreadable = "讀不到這個 PDF，請確認檔案完整"
    importErrPdfNoText = "這個 PDF 沒有文字層，多半是掃描檔"
    importErrPdfParse = "解析 PDF 失敗"
    imageErrNotReady = "這篇筆記還沒載入完成，請稍後再試"
    imageErrPermission = "沒有讀取這張圖片的權限，請重新選一次"
    imageErrUnknown = "插入圖片失敗"
    imageErrNotWritable = "這篇筆記不是可寫入的檔案"
    imageErrFormat = "認不出這張圖片的格式"
    imageErrDir = "無法在筆記資料夾建立 images 目錄"
    imageErrCreate = "無法在筆記資料夾建立檔案"
    imageErrCopy = "複製圖片到筆記資料夾時出錯"

    editorFormatH1 = "一級標題"
    editorFormatH2 = "二級標題"
    editorFormatH3 = "三級標題"
    editorFormatBold = "粗體"
    editorFormatItalic = "斜體"
    editorFormatStrike = "刪除線"
    editorFormatHighlight = "螢光筆"
    editorFormatInlineCode = "行內程式碼"
    editorFormatQuote = "引用"
    editorFormatBullet = "項目清單"
    editorFormatOrdered = "編號清單"
    editorFormatTask = "代辦清單"
    editorFormatMath = "行內數學"
    editorFormatMathBlock = "獨立數學"
    editorUndo = "撤銷"
    editorRedo = "重做"
    editorSaveCopy = "保存副本"
    editorMathSymbols = "數學符號"
    editorLinkPdf = "關聯原始 PDF"
    editorOpenPdf = "開啟原始 PDF"
    editorSaveRecovered = "已還原草稿"
    editorSaveFailed = "保存失敗"
    editorSaved = "已保存"
    msgCannotOpenFile = "無法開啟檔案"
    msgCopySaved = "副本已儲存"
    msgCopySaveFailed = "儲存副本失敗"
    msgUnknownError = "未知錯誤"
    msgImageTruncated = "內容較長，圖片只保留前段；長文建議改用 PDF"
    msgNoShareTarget = "沒有可用的分享目標"
    msgSystemUnsupported = "系統不支援"
    msgExportFailed = { "匯出失敗：$it" }
    msgShareFailed = { "分享失敗：$it" }
    msgImagePickerFailed = { "無法開啟圖片選擇器：$it" }
    exportChooseFormat = "選擇要分享的格式"
    exportHint = "選擇格式並存到指定位置，同時就是格式轉換"
    exportDirectionVertical = "圖片與 PDF 依筆記排版輸出：直排"
    exportDirectionHorizontal = "圖片與 PDF 依筆記排版輸出：橫排"
    previewFontSize = "字體大小"
    previewSyntax = "語法"
    previewCodeHighlight = "程式碼高亮"
    previewHeadingAnchors = "標題錨點"
    previewMath = "數學公式"
    previewToc = "目錄"
    pdfOpenFailed = "無法開啟這個 PDF"
    pdfImportAsNote = "匯入為筆記"
    pdfPage = { "第 $it 頁" }
    readerThemeDefault = "默認"
    readerThemePlain = "素白"
    readerThemeTinted = "淡彩"
    readerThemeDim = "夜色"
    readerThemeInk = "墨黑"
    msgNoFolder = "尚未選擇資料夾，無法建立筆記"
    msgCannotCreateNote = "無法在資料夾裡建立筆記"
    msgWriteFailed = "內容寫入失敗，請確認資料夾權限"
    msgNoPersistablePermission = "這個位置不支援長期授權，請換一個資料夾再試一次"
    msgCannotReadFile = "讀不到這個檔案"
    msgPdfLinked = "已關聯原始 PDF"
    defaultImportTitle = "匯入的文件"
    importDocument = "匯入檔案"
    defaultNotebook = "預設筆記本"
    statsWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    backdropDefault = "默認"
    backdropRice = "米紙"
    backdropCream = "米黃"
    backdropMint = "薄荷"
    backdropSky = "天青"
    backdropRose = "玫瑰"
    backdropGraphite = "石墨"
    backdropInk = "墨黑"
    formatPlainText = "純文字"
    formatWord = "Word 文件"
    formatPdf = "PDF 文件"
    formatImage = "圖片"
    fontSizeSmall = "小"
    fontSizeStandard = "標準"
    fontSizeLarge = "大"
    fontSizeHuge = "超大"
}

val ZhCnStrings = AppStrings().apply {
    appName = "文书匣子"
    allNotes = "全部笔记"
    notebooks = "笔记本"
    changeFolder = "更换文件夹"
    settings = "设置"
    help = "说明"
    searchNotes = "搜索笔记"
    searchNotebooks = "搜索笔记本"
    selectedCount = { "已选 $it 项" }
    share = "分享"
    export = "导出"
    delete = "删除"
    noNotes = "暂无笔记"
    noNotebooks = "暂无笔记本"
    noMatchNotes = "找不到匹配的笔记"
    noMatchNotebooks = "找不到匹配的笔记本"
    tryOtherKeywords = "试试其他关键词"
    createFirstNote = "点击右下角 + 按钮创建第一条笔记"
    notebookHint = "在笔记中设置分类后会显示在这里"
    notesCount = { "$it 篇笔记" }
    resultsCount = { "$it 条结果" }
    welcomeTitle = "欢迎使用文书匣子"
    welcomeBody = "请先选择一个存放 Markdown 文件的文件夹，\n应用会自动读取其中所有 .md 文件"
    chooseFolder = "选择文件夹"
    titlePlaceholder = "标题"
    bodyPlaceholder = "开始写 Markdown…"
    preview = "预览"
    backToEdit = "返回编辑"
    previewSettings = "预览设置"
    insert = "插入"
    style = "样式"
    format = "格式"
    more = "更多"
    image = "图片"
    codeBlock = "代码块"
    table = "表格"
    link = "链接"
    divider = "分隔线"
    backgroundColor = "背景颜色"
    backgroundImage = "背景图片"
    lastEdited = { "上次编辑 $it" }
    label = "标签"
    send = "发送"
    duplicate = "创建副本"
    find = "查找"
    cancel = "取消"
    done = "完成"
    create = "创建"
    newNote = "新建笔记"
    noteTitle = "笔记标题"
    untitled = "未命名笔记"
    notebookCategory = "笔记本分类"
    newCategoryName = "新分类名称"
    newCategory = "+ 新建分类"
    settingsTitle = "设置"
    language = "语言"
    languageFollowSystem = "跟随系统语言"
    libraryFolderFallback = "文件夹"
    libraryUntitledBook = "未命名.epub"
    languageFollowSystemHint = { "系统目前为 $it" }
    theme = "主题"
    monet = "莫奈取色"
    customMonet = "自定义莫奈色"
    darkMode = "深色模式"
    followSystem = "跟随系统"
    dark = "深色"
    light = "浅色"
    seedColors = "种子色"
    launcherIcon = "图标字形"
    glyphXia = "匣"
    glyphWeibo = "微伯簋"
    glyphZengzhong = "曾仲游"
    glyphLusitu = "鲁大司徒"
    glyphShuowengu = "说文古文"
    glyphShuowenzhu = "说文竹部"
    colorGuide = "莫奈取色会在 Android 12 及以上直接采用系统壁纸配色；自定义莫奈色以所选种子色派生 Expressive 色板，对比度固定为 0.5。所有界面颜色都使用 Material 3 的 color role，不会写死十六进制色值。"
    about = "关于"
    back = "返回"
    clear = "清除"
    openFolder = "打开文件夹"
    account = "账户"
    menu = "菜单"
    toggleLayout = "切换排版"
    sort = "排序"
    sortTitle = "排序依据"
    sortCustom = "自定义（默认）"
    sortCreated = "创建日期"
    sortModified = "修改日期"
    profileTitle = "个人名片"
    nickname = "昵称"
    email = "电子邮件"
    github = "GitHub"
    githubHint = "填用户名就好，会自动补成 github.com/…"
    githubPreview = { handle -> "分享名片上显示 github.com/$handle" }
    avatar = "头像"
    uploadAvatar = "上传头像"
    searchPageTitle = "搜索"
    cardShape = "卡片形状"
    noteBackground = "笔记背景图片"
    font = "字体"
    systemFont = "系统字体"
    fontLibrary = "字体库"
    fontImport = "导入字体"
    fontImportHint = "支持 .ttf 与 .otf"
    fontDefaultEdit = "编辑默认字体"
    fontDefaultReader = "阅读默认字体"
    fontEmpty = "还没有导入字体。导入后会列在这里，套用时立刻能看到差别。"
    fontImportFailed = "这个文件读不出字体"
    fontDeleteTitle = "删除字体？"
    fontDeleteMessage = { name -> "「$name」会从字体库移除。正在使用它的地方会退回系统字体。" }
    noteBackgroundPage = "笔记背景"
    noteBackgroundDefault = "默认（莫奈）"
    noteBackgroundColor = "纯色"
    noteBackgroundEmpty = "还没有导入图片，点右边的加号挑一张。"
    noteBackgroundImport = "导入图片"
    noteBackgroundDeleteTitle = "删除图片？"
    noteBackgroundDeleteMessage = { name -> "「$name」会从背景图库移除，之后可以重新导入。" }
    noteBackgroundInk = "文字颜色"
    noteBackgroundInkAuto = "跟随主题"
    noteBackgroundInkDark = "深色字"
    noteBackgroundInkLight = "浅色字"
    noteBackgroundInkHint = "只对背景图片生效：照片偏亮选深色字，偏暗选浅色字。"
    cardShapeDefault = "默认"
    cardShapeRounded = "圆润"
    cardShapeExtraRounded = "更圆"
    cardShapeSquare = "方形"
    layout = "排版"
    defaultLayout = "默认排版"
    horizontalLayout = "横排"
    verticalLayoutLabel = "竖排"
    layoutHint = "新笔记和还没指定排版的笔记都用这个默认；单篇排版在编辑页的「排版」里切换，会各自记住。"
    lineNumbersLabel = "显示行号"
    lineNumbersSummary = "竖排编辑区每栏显示行号，折行续栏不编号"
    writingGuide = "书写辅助线"
    guideNone = "无"
    guideRuled = "分隔线"
    guideGrid = "作文格"
    guideFollowDefault = "跟随默认"
    guideHint = "只影响没有单独设置过辅助线的笔记；横排是横线，直排是竖线。"
    readerContents = "目录"
    readerFontSize = "字号"
    readerOpening = "正在打开…"
    readerOpenFailed = "打不开这本书"
    readerProgress = { page, pages -> "$page / $pages 页" }
    readerChapter = { "第 $it 章" }
    readerLayout = "排版"
    readerModePaged = "翻页"
    readerModeScrollVertical = "上下滑动"
    readerModeScrollHorizontal = "左右滑动"
    readerLineHeight = "行距"
    readerMargin = "页边留白"
    library = "书库"
    libraryEmpty = "书库是空的"
    libraryEmptyHint = "用上面的「导入」把电子书放进来。书会复制到 App 自己的目录，离开阅读器也不会消失。"
    libraryImport = "导入书籍"
    libraryImportFolder = "导入文件夹"
    bookSearch = "搜索书名或作者"
    libraryAllBooks = "全部图书"
    libraryFolders = "文件夹"
    libraryNewFolder = "新增文件夹"
    libraryNoMatch = "没有符合的书"
    libraryLocalShelf = "本机书库"
    libraryFolderBooks = { "$it 本" }
    librarySyncAdded = { "新收进 $it 本" }
    libraryFolderHint = "文件夹里的新书会自动收进书库，不必一本一本导入。"
    libraryNoFolderYet = "还没有登记文件夹"
    libraryFolderDenied = "文件夹授权失效，请重新添加"
    startPage = "启动页"
    startPageHint = "下次打开 App 时从这里开始"
    startPageNotes = "全部笔记"
    startPageLibrary = "书库 · 全部图书"
    libraryManageFolders = "文件夹"
    sortRecent = "最近阅读"
    sortByTitle = "书名"
    sortByAdded = "添加时间"
    libraryRemoveFolderHint = "仅移除文件夹登记，书仍留在书库中。"
    stats = "统计"
    statsTitle = "阅读统计"
    statsRange7 = "7 天"
    statsRange30 = "30 天"
    statsRange365 = "1 年"
    statsReadingTime = "阅读时长"
    statsBooks = "读过的书"
    statsDocs = "看过的文件"
    statsEdits = "编辑次数"
    statsDistribution = "每日分布"
    statsMonthly = "每月分布"
    statsActivity = "活跃度"
    statsActivityHint = "一格一天，颜色越深表示那天读得越多、写得越多。"
    statsLess = "少"
    statsMore = "多"
    statsRanking = "排行榜"
    statsRankingBooks = "书籍"
    statsRankingDocs = "文件"
    statsEmpty = "还没有记录"
    statsEmptyHint = "打开一本书，或编辑一份笔记，这里就会开始累积。"
    statsDisabled = "统计已关闭"
    statsDisabledHint = "关掉之后不会记录任何阅读或编辑。你随时可以在设置里重新打开。"
    statsEnable = "开启统计"
    statsSettingsTitle = "统计设置"
    statsEnabledLabel = "记录统计"
    statsEnabledSummary = "关掉之后侧栏不再显示统计，也不再记录任何东西。"
    statsTrackReading = "阅读时长"
    statsTrackReadingSummary = "记下每本书读了多久。"
    statsTrackDocuments = "文件编辑"
    statsTrackDocumentsSummary = "记下打开过、编辑过哪些笔记。"
    statsShowHeatmap = "活跃热力图"
    statsShowRanking = "排行榜"
    statsClearData = "清除统计数据"
    statsClearDataSummary = "删掉全部记录，无法复原。"
    statsCleared = "统计已清除"
    statsDuration = { seconds ->
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        when {
            hours > 0 && minutes > 0 -> "$hours 小时 $minutes 分"
            hours > 0 -> "$hours 小时"
            minutes > 0 -> "$minutes 分钟"
            seconds > 0 -> "不到 1 分钟"
            else -> "—"
        }
    }
    statsStartFrom = { "从 $it 开始" }
    statsTimesCount = { "$it 次" }
    statsBooksCount = { "$it 本" }
    statsDocsCount = { "$it 份" }
    statsGreeting = { "嗨，$it" }
    statsGreetingGuest = "嗨，欢迎回来"
    statsGreetingHint = "今天想写点什么？"
    cardSectionTitle = "个人名片"
    cardNotSet = "尚未设置；可从右上角头像填写"
    cardAllOff = "已把所有栏目关掉"
    cardNotAttached = "这次不附名片"
    cardPositionLabel = "位置"
    cardPositionBefore = "正文之前"
    cardPositionAfter = "正文之后"
    cardVerticalNote = "直排笔记的名片固定排在正文之后"
    cardAvatar = "头像"
    cardAvatarHint = "只在图片格式显示"
    cardNickname = "昵称"
    cardEmail = "邮箱"
    cardGithub = "GitHub"
    authorTitle = "作者"
    authorRole = "软件工程在读"
    authorEmail = "电子邮箱"
    authorGithub = "GitHub"
    authorRepo = "项目仓库"
    authorBio = "武汉理工大学软件工程专业学生，欢迎支持反馈本应用"
    authorSupportTitle = "支持作者"
    authorSupportEntry = "微信与支付宝收款码，扫哪张都算数"
    authorSupportLead = "两张码都在这，扫哪张都算数。点一下可以放大。"
    authorSupportWechat = "微信支付"
    authorSupportAlipay = "支付宝"
    authorSupportZoom = "点一下放大"
    bookErrUnknownFormat = "认不出这个文件的格式"
    bookErrInvalidPath = "文件路径无效"
    bookErrFileNotFound = "找不到这个文件"
    bookErrFileUnreadable = "读不到这个文件，授权可能已失效"
    bookErrFileEmpty = "这个文件是空的"
    bookErrFileTooSmall = "文件太小，不像完整的电子书"
    bookErrNotEpub = "这个文件不是 EPUB"
    bookErrNotMobi = "这个文件不是有效的 MOBI／AZW3"
    bookErrNotFb2 = "这个文件不是 FB2 电子书"
    bookErrEpubRootfile = "EPUB 的 container.xml 没指定内容清单"
    bookErrEpubPackage = "EPUB 的内容清单读不出来"
    bookErrNoContent = "这本书没有可读的章节"
    bookErrChapterMissing = "这个章节不存在"
    bookErrChapterUnreadable = "章节文件读不出来"
    bookErrDrm = "这本书有 DRM 保护，无法打开"
    bookErrCorrupt = "文件结构不完整，可能下载不完整"
    bookErrCompression = "不支持这种压缩方式"
    bookErrParse = "这本书解析失败"
    bookErrOpen = "无法打开这个文件"
    importErrUnreadable = "读不到这个文件"
    importErrPdfNoExtension = "这个 PDF 没有扩展名，请确认文件完整"
    importErrWord = "这个文件不是可读取的 Word（.docx）文件"
    importErrEmpty = "这个文件没有内容"
    importErrHtmlEmpty = "这个 HTML 没有可导入的文字"
    importErrEbook = "这是电子书，请直接打开阅读"
    importErrUnsupported = "不支持这个文件格式"
    importErrPdfUnreadable = "读不到这个 PDF，请确认文件完整"
    importErrPdfNoText = "这个 PDF 没有文字层，多半是扫描件"
    importErrPdfParse = "解析 PDF 失败"
    imageErrNotReady = "这篇笔记还没加载完成，请稍后再试"
    imageErrPermission = "没有读取这张图片的权限，请重新选一次"
    imageErrUnknown = "插入图片失败"
    imageErrNotWritable = "这篇笔记不是可写入的文件"
    imageErrFormat = "认不出这张图片的格式"
    imageErrDir = "无法在笔记文件夹建立 images 目录"
    imageErrCreate = "无法在笔记文件夹建立文件"
    imageErrCopy = "复制图片到笔记文件夹时出错"

    editorFormatH1 = "一级标题"
    editorFormatH2 = "二级标题"
    editorFormatH3 = "三级标题"
    editorFormatBold = "粗体"
    editorFormatItalic = "斜体"
    editorFormatStrike = "删除线"
    editorFormatHighlight = "荧光笔"
    editorFormatInlineCode = "行内代码"
    editorFormatQuote = "引用"
    editorFormatBullet = "项目列表"
    editorFormatOrdered = "编号列表"
    editorFormatTask = "待办列表"
    editorFormatMath = "行内数学"
    editorFormatMathBlock = "独立数学"
    editorUndo = "撤销"
    editorRedo = "重做"
    editorSaveCopy = "保存副本"
    editorMathSymbols = "数学符号"
    editorLinkPdf = "关联原始 PDF"
    editorOpenPdf = "打开原始 PDF"
    editorSaveRecovered = "已还原草稿"
    editorSaveFailed = "保存失败"
    editorSaved = "已保存"
    msgCannotOpenFile = "无法打开文件"
    msgCopySaved = "副本已保存"
    msgCopySaveFailed = "保存副本失败"
    msgUnknownError = "未知错误"
    msgImageTruncated = "内容较长，图片只保留前段；长文建议改用 PDF"
    msgNoShareTarget = "没有可用的分享目标"
    msgSystemUnsupported = "系统不支持"
    msgExportFailed = { "导出失败：$it" }
    msgShareFailed = { "分享失败：$it" }
    msgImagePickerFailed = { "无法打开图片选择器：$it" }
    exportChooseFormat = "选择要分享的格式"
    exportHint = "选择格式并保存到指定位置，同时就是格式转换"
    exportDirectionVertical = "图片与 PDF 依笔记排版输出：竖排"
    exportDirectionHorizontal = "图片与 PDF 依笔记排版输出：横排"
    previewFontSize = "字体大小"
    previewSyntax = "语法"
    previewCodeHighlight = "代码高亮"
    previewHeadingAnchors = "标题锚点"
    previewMath = "数学公式"
    previewToc = "目录"
    pdfOpenFailed = "无法打开这个 PDF"
    pdfImportAsNote = "导入为笔记"
    pdfPage = { "第 $it 页" }
    readerThemeDefault = "默认"
    readerThemePlain = "素白"
    readerThemeTinted = "淡彩"
    readerThemeDim = "夜色"
    readerThemeInk = "墨黑"
    msgNoFolder = "尚未选择文件夹，无法创建笔记"
    msgCannotCreateNote = "无法在文件夹里创建笔记"
    msgWriteFailed = "内容写入失败，请确认文件夹权限"
    msgNoPersistablePermission = "这个位置不支持长期授权，请换一个文件夹再试一次"
    msgCannotReadFile = "读不到这个文件"
    msgPdfLinked = "已关联原始 PDF"
    defaultImportTitle = "导入的文件"
    importDocument = "导入文件"
    defaultNotebook = "默认笔记本"
    statsWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    backdropDefault = "默认"
    backdropRice = "米纸"
    backdropCream = "米黄"
    backdropMint = "薄荷"
    backdropSky = "天青"
    backdropRose = "玫瑰"
    backdropGraphite = "石墨"
    backdropInk = "墨黑"
    formatPlainText = "纯文字"
    formatWord = "Word 文件"
    formatPdf = "PDF 文件"
    formatImage = "图片"
    fontSizeSmall = "小"
    fontSizeStandard = "标准"
    fontSizeLarge = "大"
    fontSizeHuge = "超大"
}

val EnUsStrings = AppStrings().apply {
    appName = "DocBox"
    allNotes = "All notes"
    notebooks = "Notebooks"
    changeFolder = "Change folder"
    settings = "Settings"
    help = "Help"
    searchNotes = "Search notes"
    searchNotebooks = "Search notebooks"
    selectedCount = { "$it selected" }
    share = "Share"
    export = "Export"
    delete = "Delete"
    noNotes = "No notes yet"
    noNotebooks = "No notebooks yet"
    noMatchNotes = "No matching notes"
    noMatchNotebooks = "No matching notebooks"
    tryOtherKeywords = "Try another keyword"
    createFirstNote = "Tap + to create your first note"
    notebookHint = "Notebooks appear here once notes have a category"
    notesCount = { "$it notes" }
    resultsCount = { "$it results" }
    welcomeTitle = "Welcome to DocBox"
    welcomeBody = "Choose a folder that stores your Markdown files.\nEvery .md file inside will be imported automatically."
    chooseFolder = "Choose folder"
    titlePlaceholder = "Title"
    bodyPlaceholder = "Start writing Markdown…"
    preview = "Preview"
    backToEdit = "Back to editing"
    previewSettings = "Preview settings"
    insert = "Insert"
    style = "Style"
    format = "Format"
    more = "More"
    image = "Image"
    codeBlock = "Code block"
    table = "Table"
    link = "Link"
    divider = "Divider"
    backgroundColor = "Background color"
    backgroundImage = "Background image"
    lastEdited = { "Last edited $it" }
    label = "Label"
    send = "Send"
    duplicate = "Duplicate"
    find = "Find"
    cancel = "Cancel"
    done = "Done"
    create = "Create"
    newNote = "New note"
    noteTitle = "Note title"
    untitled = "Untitled note"
    notebookCategory = "Notebook"
    newCategoryName = "New category name"
    newCategory = "+ New category"
    settingsTitle = "Settings"
    language = "Language"
    languageFollowSystem = "Follow system language"
    libraryFolderFallback = "Folder"
    libraryUntitledBook = "Untitled.epub"
    languageFollowSystemHint = { "System is set to $it" }
    theme = "Theme"
    monet = "Monet"
    customMonet = "Custom Monet"
    darkMode = "Dark mode"
    followSystem = "Follow system"
    dark = "Dark"
    light = "Light"
    seedColors = "Seed color"
    launcherIcon = "Icon glyph"
    glyphXia = "Box"
    glyphWeibo = "Weibo"
    glyphZengzhong = "Zengzhong"
    glyphLusitu = "Dasitu"
    glyphShuowengu = "Guwen"
    glyphShuowenzhu = "Zhubu"
    colorGuide = "Monet uses the system wallpaper palette on Android 12 and above. Custom Monet derives an Expressive palette from the selected seed color with a fixed 0.5 contrast level. Every UI color maps to a Material 3 color role — no hardcoded hex values."
    about = "About"
    back = "Back"
    clear = "Clear"
    openFolder = "Open folder"
    account = "Account"
    menu = "Menu"
    toggleLayout = "Toggle layout"
    sort = "Sort"
    sortTitle = "Sort by"
    sortCustom = "Custom (default)"
    sortCreated = "Created date"
    sortModified = "Modified date"
    profileTitle = "Profile"
    nickname = "Nickname"
    email = "Email"
    github = "GitHub"
    githubHint = "Just the username — it becomes github.com/…"
    githubPreview = { handle -> "Shared card shows github.com/$handle" }
    avatar = "Avatar"
    uploadAvatar = "Upload avatar"
    searchPageTitle = "Search"
    cardShape = "Card shape"
    noteBackground = "Note background"
    font = "Font"
    systemFont = "System font"
    fontLibrary = "Font library"
    fontImport = "Import font"
    fontImportHint = "TTF and OTF supported"
    fontDefaultEdit = "Default font for editing"
    fontDefaultReader = "Default font for reading"
    fontEmpty = "No fonts imported yet. Imported fonts show up here, so you can see the difference before you pick one."
    fontImportFailed = "That file is not a readable font"
    fontDeleteTitle = "Delete font?"
    fontDeleteMessage = { name -> "“$name” will be removed from the library. Anything using it falls back to the system font." }
    noteBackgroundPage = "Note background"
    noteBackgroundDefault = "Default (Monet)"
    noteBackgroundColor = "Solid colour"
    noteBackgroundEmpty = "No images yet — tap the plus to pick one."
    noteBackgroundImport = "Import image"
    noteBackgroundDeleteTitle = "Delete image?"
    noteBackgroundDeleteMessage = { name -> "“$name” will be removed from the library. You can import it again later." }
    noteBackgroundInk = "Text colour"
    noteBackgroundInkAuto = "Follow theme"
    noteBackgroundInkDark = "Dark text"
    noteBackgroundInkLight = "Light text"
    noteBackgroundInkHint = "Applies to background images only: dark text for bright photos, light text for dim ones."
    cardShapeDefault = "Default"
    cardShapeRounded = "Rounded"
    cardShapeExtraRounded = "Extra rounded"
    cardShapeSquare = "Square"
    layout = "Layout"
    defaultLayout = "Default layout"
    horizontalLayout = "Horizontal"
    verticalLayoutLabel = "Vertical"
    layoutHint = "Used by new notes and notes without their own layout. Switch a single note in the editor sheet; each note remembers its own."
    lineNumbersLabel = "Show line numbers"
    lineNumbersSummary = "Numbered columns in the vertical editor; wrapped columns stay unnumbered"
    writingGuide = "Writing guide"
    guideNone = "None"
    guideRuled = "Ruled lines"
    guideGrid = "Grid"
    guideFollowDefault = "Use default"
    guideHint = "Applies only to notes without their own guide. Horizontal text uses rules, vertical text uses column rules."
    readerContents = "Contents"
    readerFontSize = "Text size"
    readerOpening = "Opening…"
    readerOpenFailed = "Couldn't open this book"
    readerProgress = { page, pages -> "$page / $pages" }
    readerChapter = { "Chapter $it" }
    readerLayout = "Layout"
    readerModePaged = "Pages"
    readerModeScrollVertical = "Scroll ↓"
    readerModeScrollHorizontal = "Scroll ↔"
    readerLineHeight = "Line spacing"
    readerMargin = "Margins"
    library = "Library"
    libraryEmpty = "Your library is empty"
    libraryEmptyHint = "Use Import above to add ebooks. They are copied into the app so they stay after you leave the reader."
    libraryImport = "Import book"
    libraryImportFolder = "Import folder"
    bookSearch = "Search books"
    libraryAllBooks = "All books"
    libraryFolders = "Folders"
    libraryNewFolder = "Add folder"
    libraryNoMatch = "No matching books"
    libraryLocalShelf = "On this device"
    libraryFolderBooks = { "$it books" }
    librarySyncAdded = { "$it books added" }
    libraryFolderHint = "New files in a folder are added to your library automatically — no importing one by one."
    libraryNoFolderYet = "No folders added yet"
    libraryFolderDenied = "Folder access revoked — add it again"
    startPage = "Start page"
    startPageHint = "Where the app opens next time"
    startPageNotes = "All notes"
    startPageLibrary = "Library · All books"
    libraryManageFolders = "Folders"
    sortRecent = "Recently read"
    sortByTitle = "Title"
    sortByAdded = "Date added"
    libraryRemoveFolderHint = "Only the folder is removed; the books stay in your library."
    stats = "Stats"
    statsTitle = "Reading stats"
    statsRange7 = "7 days"
    statsRange30 = "30 days"
    statsRange365 = "1 year"
    statsReadingTime = "Reading time"
    statsBooks = "Books read"
    statsDocs = "Docs opened"
    statsEdits = "Edits"
    statsDistribution = "Daily breakdown"
    statsMonthly = "Monthly breakdown"
    statsActivity = "Activity"
    statsActivityHint = "One square per day — the deeper the colour, the more you read and wrote."
    statsLess = "Less"
    statsMore = "More"
    statsRanking = "Leaderboard"
    statsRankingBooks = "Books"
    statsRankingDocs = "Documents"
    statsEmpty = "Nothing recorded yet"
    statsEmptyHint = "Open a book or edit a note and this page starts filling up."
    statsDisabled = "Stats are off"
    statsDisabledHint = "Nothing is recorded while this is off. Turn it back on in Settings whenever you like."
    statsEnable = "Turn on"
    statsSettingsTitle = "Stats settings"
    statsEnabledLabel = "Record stats"
    statsEnabledSummary = "Turning this off hides the page and stops all recording."
    statsTrackReading = "Reading time"
    statsTrackReadingSummary = "Records how long each book was open."
    statsTrackDocuments = "Document edits"
    statsTrackDocumentsSummary = "Records which notes you open and edit."
    statsShowHeatmap = "Activity grid"
    statsShowRanking = "Leaderboard"
    statsClearData = "Clear stats data"
    statsClearDataSummary = "Deletes every record. This cannot be undone."
    statsCleared = "Stats cleared"
    statsDuration = { seconds ->
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes min"
            seconds > 0 -> "under a minute"
            else -> "—"
        }
    }
    statsStartFrom = { "since $it" }
    statsTimesCount = { "$it times" }
    statsBooksCount = { "$it books" }
    statsDocsCount = { "$it docs" }
    statsGreeting = { "Hi, $it" }
    statsGreetingGuest = "Hi there"
    statsGreetingHint = "What will you write today?"
    cardSectionTitle = "Personal card"
    cardNotSet = "Not set yet — tap the avatar at the top right"
    cardAllOff = "All fields are turned off"
    cardNotAttached = "Not attached this time"
    cardPositionLabel = "Position"
    cardPositionBefore = "Before text"
    cardPositionAfter = "After text"
    cardVerticalNote = "Vertical notes always put the card after the text"
    cardAvatar = "Avatar"
    cardAvatarHint = "Images only"
    cardNickname = "Nickname"
    cardEmail = "Email"
    cardGithub = "GitHub"
    authorTitle = "Author"
    authorRole = "Software engineering student"
    authorEmail = "Email"
    authorGithub = "GitHub"
    authorRepo = "Repository"
    authorBio = "Software engineering student at Wuhan University of Technology — feedback and support are always welcome."
    authorSupportTitle = "Support the author"
    authorSupportEntry = "WeChat Pay and Alipay codes — either one works"
    authorSupportLead = "Both codes are here; either one works. Tap to enlarge."
    authorSupportWechat = "WeChat Pay"
    authorSupportAlipay = "Alipay"
    authorSupportZoom = "Tap to enlarge"
    bookErrUnknownFormat = "Could not recognise this file’s format"
    bookErrInvalidPath = "That file path is not valid"
    bookErrFileNotFound = "That file could not be found"
    bookErrFileUnreadable = "Could not read this file — access may have expired"
    bookErrFileEmpty = "This file is empty"
    bookErrFileTooSmall = "The file is too small to be a complete e-book"
    bookErrNotEpub = "This file is not an EPUB"
    bookErrNotMobi = "This file is not a valid MOBI/AZW3"
    bookErrNotFb2 = "This file is not an FB2 e-book"
    bookErrEpubRootfile = "The EPUB’s container.xml names no package document"
    bookErrEpubPackage = "The EPUB’s package document could not be read"
    bookErrNoContent = "This book has no readable chapters"
    bookErrChapterMissing = "That chapter does not exist"
    bookErrChapterUnreadable = "The chapter file could not be read"
    bookErrDrm = "This book is DRM-protected and cannot be opened"
    bookErrCorrupt = "The file structure is incomplete — the download may be truncated"
    bookErrCompression = "This compression method is not supported"
    bookErrParse = "This book could not be parsed"
    bookErrOpen = "Could not open this file"
    importErrUnreadable = "Could not read this file"
    importErrPdfNoExtension = "This PDF has no extension — check that the file is complete"
    importErrWord = "This is not a readable Word (.docx) document"
    importErrEmpty = "This file has no content"
    importErrHtmlEmpty = "This HTML has no text to import"
    importErrEbook = "This is an e-book — open it in the reader instead"
    importErrUnsupported = "This file format is not supported"
    importErrPdfUnreadable = "Could not read this PDF — check that the file is complete"
    importErrPdfNoText = "This PDF has no text layer — most likely a scan"
    importErrPdfParse = "Could not parse this PDF"
    imageErrNotReady = "This note is still loading — try again in a moment"
    imageErrPermission = "No permission to read that image — pick it again"
    imageErrUnknown = "Could not insert the image"
    imageErrNotWritable = "This note is not a writable file"
    imageErrFormat = "Could not recognise that image format"
    imageErrDir = "Could not create the images folder beside this note"
    imageErrCreate = "Could not create a file in the note folder"
    imageErrCopy = "Copying the image into the note folder failed"

    editorFormatH1 = "Heading 1"
    editorFormatH2 = "Heading 2"
    editorFormatH3 = "Heading 3"
    editorFormatBold = "Bold"
    editorFormatItalic = "Italic"
    editorFormatStrike = "Strikethrough"
    editorFormatHighlight = "Highlight"
    editorFormatInlineCode = "Inline code"
    editorFormatQuote = "Quote"
    editorFormatBullet = "Bulleted list"
    editorFormatOrdered = "Numbered list"
    editorFormatTask = "Task list"
    editorFormatMath = "Inline math"
    editorFormatMathBlock = "Display math"
    editorUndo = "Undo"
    editorRedo = "Redo"
    editorSaveCopy = "Save a copy"
    editorMathSymbols = "Math symbols"
    editorLinkPdf = "Link original PDF"
    editorOpenPdf = "Open original PDF"
    editorSaveRecovered = "Draft restored"
    editorSaveFailed = "Save failed"
    editorSaved = "Saved"
    msgCannotOpenFile = "Could not open the file"
    msgCopySaved = "Copy saved"
    msgCopySaveFailed = "Could not save the copy"
    msgUnknownError = "unknown error"
    msgImageTruncated = "The note is long, so the image keeps only the beginning. Use PDF for long notes."
    msgNoShareTarget = "no app available to share with"
    msgSystemUnsupported = "not supported by the system"
    msgExportFailed = { "Export failed: $it" }
    msgShareFailed = { "Share failed: $it" }
    msgImagePickerFailed = { "Could not open the image picker: $it" }
    exportChooseFormat = "Choose a format to share"
    exportHint = "Pick a format and a location; that choice is the conversion."
    exportDirectionVertical = "Image and PDF follow the note layout: vertical"
    exportDirectionHorizontal = "Image and PDF follow the note layout: horizontal"
    previewFontSize = "Text size"
    previewSyntax = "Syntax"
    previewCodeHighlight = "Code highlighting"
    previewHeadingAnchors = "Heading anchors"
    previewMath = "Math"
    previewToc = "Contents"
    pdfOpenFailed = "Could not open this PDF"
    pdfImportAsNote = "Import as note"
    pdfPage = { "Page $it" }
    readerThemeDefault = "Default"
    readerThemePlain = "Plain"
    readerThemeTinted = "Tinted"
    readerThemeDim = "Dim"
    readerThemeInk = "Ink"
    msgNoFolder = "No folder selected, so the note cannot be created"
    msgCannotCreateNote = "Could not create the note in this folder"
    msgWriteFailed = "Writing failed. Check the folder permission."
    msgNoPersistablePermission = "This location cannot be granted long-term access. Pick another folder."
    msgCannotReadFile = "Could not read this file"
    msgPdfLinked = "Original PDF linked"
    defaultImportTitle = "Imported file"
    importDocument = "Import a file"
    defaultNotebook = "Default notebook"
    statsWeekdays = listOf("M", "T", "W", "T", "F", "S", "S")
    backdropDefault = "Default"
    backdropRice = "Rice paper"
    backdropCream = "Cream"
    backdropMint = "Mint"
    backdropSky = "Sky"
    backdropRose = "Rose"
    backdropGraphite = "Graphite"
    backdropInk = "Ink"
    formatPlainText = "Plain text"
    formatWord = "Word document"
    formatPdf = "PDF document"
    formatImage = "Image"
    fontSizeSmall = "Small"
    fontSizeStandard = "Standard"
    fontSizeLarge = "Large"
    fontSizeHuge = "Extra large"
}

val FrFrStrings = AppStrings().apply {
    appName = "BoîteDoc"
    allNotes = "Toutes les notes"
    notebooks = "Carnets"
    changeFolder = "Changer de dossier"
    settings = "Paramètres"
    help = "Aide"
    searchNotes = "Rechercher des notes"
    searchNotebooks = "Rechercher des carnets"
    selectedCount = { "$it sélectionné(s)" }
    share = "Partager"
    export = "Exporter"
    delete = "Supprimer"
    noNotes = "Aucune note"
    noNotebooks = "Aucun carnet"
    noMatchNotes = "Aucune note correspondante"
    noMatchNotebooks = "Aucun carnet correspondant"
    tryOtherKeywords = "Essayez un autre mot-clé"
    createFirstNote = "Appuyez sur + pour créer votre première note"
    notebookHint = "Les carnets apparaissent ici dès qu'une note a une catégorie"
    notesCount = { "$it notes" }
    resultsCount = { "$it résultats" }
    welcomeTitle = "Bienvenue dans BoîteDoc"
    welcomeBody = "Choisissez un dossier contenant vos fichiers Markdown.\nTous les fichiers .md seront importés automatiquement."
    chooseFolder = "Choisir un dossier"
    titlePlaceholder = "Titre"
    bodyPlaceholder = "Commencez à écrire en Markdown…"
    preview = "Aperçu"
    backToEdit = "Retour à l'édition"
    previewSettings = "Réglages de l'aperçu"
    insert = "Insérer"
    style = "Style"
    format = "Format"
    more = "Plus"
    image = "Image"
    codeBlock = "Bloc de code"
    table = "Tableau"
    link = "Lien"
    divider = "Séparateur"
    backgroundColor = "Couleur de fond"
    backgroundImage = "Image de fond"
    lastEdited = { "Modifié le $it" }
    label = "Étiquette"
    send = "Envoyer"
    duplicate = "Dupliquer"
    find = "Rechercher"
    cancel = "Annuler"
    done = "Terminé"
    create = "Créer"
    newNote = "Nouvelle note"
    noteTitle = "Titre de la note"
    untitled = "Note sans titre"
    notebookCategory = "Carnet"
    newCategoryName = "Nom de la nouvelle catégorie"
    newCategory = "+ Nouvelle catégorie"
    settingsTitle = "Paramètres"
    language = "Langue"
    languageFollowSystem = "Suivre la langue du système"
    libraryFolderFallback = "Dossier"
    libraryUntitledBook = "Sans titre.epub"
    languageFollowSystemHint = { "Système en $it" }
    theme = "Thème"
    monet = "Monet"
    customMonet = "Monet personnalisé"
    darkMode = "Mode sombre"
    followSystem = "Suivre le système"
    dark = "Sombre"
    light = "Clair"
    seedColors = "Couleur source"
    launcherIcon = "Glyphe d'icône"
    glyphXia = "Boîte"
    glyphWeibo = "Weibo"
    glyphZengzhong = "Zengzhong"
    glyphLusitu = "Dasitu"
    glyphShuowengu = "Guwen"
    glyphShuowenzhu = "Zhubu"
    colorGuide = "Monet utilise la palette du fond d'écran système sur Android 12 et versions ultérieures. Monet personnalisé dérive une palette Expressive de la couleur source choisie, avec un niveau de contraste fixé à 0,5. Toutes les couleurs de l'interface correspondent à un rôle Material 3 — aucune valeur hexadécimale codée en dur."
    about = "À propos"
    back = "Retour"
    clear = "Effacer"
    openFolder = "Ouvrir le dossier"
    account = "Compte"
    menu = "Menu"
    toggleLayout = "Changer la disposition"
    sort = "Trier"
    sortTitle = "Trier par"
    sortCustom = "Personnalisé (par défaut)"
    sortCreated = "Date de création"
    sortModified = "Date de modification"
    profileTitle = "Profil"
    nickname = "Pseudo"
    email = "E-mail"
    github = "GitHub"
    githubHint = "Le nom d'utilisateur suffit — il devient github.com/…"
    githubPreview = { handle -> "La carte partagée affiche github.com/$handle" }
    avatar = "Avatar"
    uploadAvatar = "Importer un avatar"
    searchPageTitle = "Recherche"
    cardShape = "Forme de carte"
    noteBackground = "Fond de note"
    font = "Police"
    systemFont = "Police système"
    fontLibrary = "Bibliothèque de polices"
    fontImport = "Importer une police"
    fontImportHint = "Formats .ttf et .otf"
    fontDefaultEdit = "Police d'édition par défaut"
    fontDefaultReader = "Police de lecture par défaut"
    fontEmpty = "Aucune police importée. Elles apparaîtront ici, pour comparer avant de choisir."
    fontImportFailed = "Ce fichier n'est pas une police lisible"
    fontDeleteTitle = "Supprimer la police ?"
    fontDeleteMessage = { name -> "« $name » sera retirée de la bibliothèque. Ce qui l'utilise reviendra à la police système." }
    noteBackgroundPage = "Fond de note"
    noteBackgroundDefault = "Par défaut (Monet)"
    noteBackgroundColor = "Couleur unie"
    noteBackgroundEmpty = "Aucune image — touchez le plus pour en choisir une."
    noteBackgroundImport = "Importer une image"
    noteBackgroundDeleteTitle = "Supprimer l'image ?"
    noteBackgroundDeleteMessage = { name -> "« $name » sera retirée de la bibliothèque. Vous pourrez la réimporter plus tard." }
    noteBackgroundInk = "Couleur du texte"
    noteBackgroundInkAuto = "Suivre le thème"
    noteBackgroundInkDark = "Texte foncé"
    noteBackgroundInkLight = "Texte clair"
    noteBackgroundInkHint = "S'applique aux images de fond : texte foncé sur une photo claire, texte clair sur une photo sombre."
    cardShapeDefault = "Par défaut"
    cardShapeRounded = "Arrondi"
    cardShapeExtraRounded = "Très arrondi"
    cardShapeSquare = "Carré"
    layout = "Mise en page"
    defaultLayout = "Mise en page par défaut"
    horizontalLayout = "Horizontale"
    verticalLayoutLabel = "Verticale"
    layoutHint = "Utilisée par les nouvelles notes et celles sans mise en page propre. Changez une note dans l'éditeur : chacune garde la sienne."
    lineNumbersLabel = "Afficher les numéros de ligne"
    lineNumbersSummary = "Colonnes numérotées dans l'éditeur vertical ; les colonnes de continuation restent sans numéro"
    writingGuide = "Guide d'écriture"
    guideNone = "Aucun"
    guideRuled = "Lignes"
    guideGrid = "Grille"
    guideFollowDefault = "Par défaut"
    guideHint = "S'applique aux notes sans réglage propre. L'horizontal utilise des lignes, le vertical des colonnes."
    readerContents = "Sommaire"
    readerFontSize = "Taille du texte"
    readerOpening = "Ouverture…"
    readerOpenFailed = "Impossible d'ouvrir ce livre"
    readerProgress = { page, pages -> "$page / $pages" }
    readerChapter = { "Chapitre $it" }
    readerLayout = "Mise en page"
    readerModePaged = "Pages"
    readerModeScrollVertical = "Défiler ↓"
    readerModeScrollHorizontal = "Défiler ↔"
    readerLineHeight = "Interligne"
    readerMargin = "Marges"
    library = "Bibliothèque"
    libraryEmpty = "Votre bibliothèque est vide"
    libraryEmptyHint = "Utilisez Importer pour ajouter des livres. Ils sont copiés dans l'application et restent disponibles."
    libraryImport = "Importer un livre"
    libraryImportFolder = "Importer un dossier"
    bookSearch = "Rechercher des livres"
    libraryAllBooks = "Tous les livres"
    libraryFolders = "Dossiers"
    libraryNewFolder = "Ajouter un dossier"
    libraryNoMatch = "Aucun livre correspondant"
    libraryLocalShelf = "Sur cet appareil"
    libraryFolderBooks = { "$it livres" }
    librarySyncAdded = { "$it livres ajoutés" }
    libraryFolderHint = "Les nouveaux fichiers d'un dossier rejoignent la bibliothèque automatiquement."
    libraryNoFolderYet = "Aucun dossier ajouté"
    libraryFolderDenied = "Accès au dossier révoqué — à rajouter"
    startPage = "Page d'accueil"
    startPageHint = "Page affichée au prochain lancement"
    startPageNotes = "Toutes les notes"
    startPageLibrary = "Bibliothèque · Tous les livres"
    libraryManageFolders = "Dossiers"
    sortRecent = "Lus récemment"
    sortByTitle = "Titre"
    sortByAdded = "Date d'ajout"
    libraryRemoveFolderHint = "Seul le dossier est retiré ; les livres restent."
    stats = "Stats"
    statsTitle = "Statistiques de lecture"
    statsRange7 = "7 jours"
    statsRange30 = "30 jours"
    statsRange365 = "1 an"
    statsReadingTime = "Temps de lecture"
    statsBooks = "Livres lus"
    statsDocs = "Docs ouverts"
    statsEdits = "Modifications"
    statsDistribution = "Répartition par jour"
    statsMonthly = "Répartition par mois"
    statsActivity = "Activité"
    statsActivityHint = "Un carré par jour : plus la couleur est dense, plus vous avez lu et écrit."
    statsLess = "Moins"
    statsMore = "Plus"
    statsRanking = "Classement"
    statsRankingBooks = "Livres"
    statsRankingDocs = "Documents"
    statsEmpty = "Rien d’enregistré"
    statsEmptyHint = "Ouvrez un livre ou modifiez une note : cette page se remplira."
    statsDisabled = "Statistiques désactivées"
    statsDisabledHint = "Rien n’est enregistré tant que c’est désactivé. Réactivez-le dans Réglages."
    statsEnable = "Activer"
    statsSettingsTitle = "Réglages des statistiques"
    statsEnabledLabel = "Enregistrer les stats"
    statsEnabledSummary = "Désactive la page et arrête tout enregistrement."
    statsTrackReading = "Temps de lecture"
    statsTrackReadingSummary = "Note la durée de chaque livre ouvert."
    statsTrackDocuments = "Modifications"
    statsTrackDocumentsSummary = "Note quelles notes vous ouvrez et modifiez."
    statsShowHeatmap = "Grille d’activité"
    statsShowRanking = "Classement"
    statsClearData = "Effacer les données"
    statsClearDataSummary = "Supprime tout. Irréversible."
    statsCleared = "Statistiques effacées"
    statsDuration = { seconds ->
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes min"
            seconds > 0 -> "moins d’une minute"
            else -> "—"
        }
    }
    statsStartFrom = { "depuis $it" }
    statsTimesCount = { "$it fois" }
    statsBooksCount = { "$it livres" }
    statsDocsCount = { "$it docs" }
    statsGreeting = { "Bonjour $it" }
    statsGreetingGuest = "Bonjour"
    statsGreetingHint = "Qu'allez-vous écrire aujourd'hui ?"
    cardSectionTitle = "Carte personnelle"
    cardNotSet = "Pas encore remplie — touchez l'avatar en haut à droite"
    cardAllOff = "Tous les champs sont désactivés"
    cardNotAttached = "Non jointe cette fois"
    cardPositionLabel = "Position"
    cardPositionBefore = "Avant le texte"
    cardPositionAfter = "Après le texte"
    cardVerticalNote = "En vertical, la carte est toujours placée après le texte"
    cardAvatar = "Avatar"
    cardAvatarHint = "Images uniquement"
    cardNickname = "Pseudo"
    cardEmail = "E-mail"
    cardGithub = "GitHub"
    authorTitle = "Auteur"
    authorRole = "Étudiant en génie logiciel"
    authorEmail = "E-mail"
    authorGithub = "GitHub"
    authorRepo = "Dépôt"
    authorBio = "Étudiant en génie logiciel à l'Université de technologie de Wuhan — retours et soutien toujours bienvenus."
    authorSupportTitle = "Soutenir l'auteur"
    authorSupportEntry = "Codes WeChat Pay et Alipay — au choix"
    authorSupportLead = "Les deux codes sont ici, l'un ou l'autre fonctionne. Touchez pour agrandir."
    authorSupportWechat = "WeChat Pay"
    authorSupportAlipay = "Alipay"
    authorSupportZoom = "Toucher pour agrandir"
    bookErrUnknownFormat = "Format de fichier non reconnu"
    bookErrInvalidPath = "Chemin de fichier invalide"
    bookErrFileNotFound = "Fichier introuvable"
    bookErrFileUnreadable = "Impossible de lire ce fichier : l’autorisation a peut-être expiré"
    bookErrFileEmpty = "Ce fichier est vide"
    bookErrFileTooSmall = "Le fichier est trop petit pour être un livre complet"
    bookErrNotEpub = "Ce fichier n’est pas un EPUB"
    bookErrNotMobi = "Ce fichier n’est pas un MOBI/AZW3 valide"
    bookErrNotFb2 = "Ce fichier n’est pas un livre FB2"
    bookErrEpubRootfile = "Le container.xml de l’EPUB ne désigne aucun document de paquet"
    bookErrEpubPackage = "Le document de paquet de l’EPUB est illisible"
    bookErrNoContent = "Ce livre n’a aucun chapitre lisible"
    bookErrChapterMissing = "Ce chapitre n’existe pas"
    bookErrChapterUnreadable = "Le fichier du chapitre est illisible"
    bookErrDrm = "Ce livre est protégé par DRM et ne peut pas être ouvert"
    bookErrCorrupt = "Structure du fichier incomplète : le téléchargement est peut-être interrompu"
    bookErrCompression = "Cette méthode de compression n’est pas prise en charge"
    bookErrParse = "Ce livre n’a pas pu être analysé"
    bookErrOpen = "Impossible d’ouvrir ce fichier"
    importErrUnreadable = "Impossible de lire ce fichier"
    importErrPdfNoExtension = "Ce PDF n’a pas d’extension : vérifiez que le fichier est complet"
    importErrWord = "Ce fichier n’est pas un document Word (.docx) lisible"
    importErrEmpty = "Ce fichier ne contient rien"
    importErrHtmlEmpty = "Ce HTML ne contient aucun texte à importer"
    importErrEbook = "C’est un livre numérique : ouvrez-le dans le lecteur"
    importErrUnsupported = "Ce format de fichier n’est pas pris en charge"
    importErrPdfUnreadable = "Impossible de lire ce PDF : vérifiez que le fichier est complet"
    importErrPdfNoText = "Ce PDF n’a pas de couche texte : un scan, probablement"
    importErrPdfParse = "Impossible d’analyser ce PDF"
    imageErrNotReady = "La note charge encore : réessayez dans un instant"
    imageErrPermission = "Pas d’autorisation pour lire cette image : sélectionnez-la à nouveau"
    imageErrUnknown = "Impossible d’insérer l’image"
    imageErrNotWritable = "Cette note n’est pas un fichier modifiable"
    imageErrFormat = "Format d’image non reconnu"
    imageErrDir = "Impossible de créer le dossier images à côté de la note"
    imageErrCreate = "Impossible de créer un fichier dans le dossier de la note"
    imageErrCopy = "La copie de l’image dans le dossier de la note a échoué"

    editorFormatH1 = "Titre 1"
    editorFormatH2 = "Titre 2"
    editorFormatH3 = "Titre 3"
    editorFormatBold = "Gras"
    editorFormatItalic = "Italique"
    editorFormatStrike = "Barré"
    editorFormatHighlight = "Surlignage"
    editorFormatInlineCode = "Code en ligne"
    editorFormatQuote = "Citation"
    editorFormatBullet = "Liste à puces"
    editorFormatOrdered = "Liste numérotée"
    editorFormatTask = "Liste de tâches"
    editorFormatMath = "Formule en ligne"
    editorFormatMathBlock = "Formule centrée"
    editorUndo = "Annuler"
    editorRedo = "Rétablir"
    editorSaveCopy = "Enregistrer une copie"
    editorMathSymbols = "Symboles mathématiques"
    editorLinkPdf = "Lier le PDF d'origine"
    editorOpenPdf = "Ouvrir le PDF d'origine"
    editorSaveRecovered = "Brouillon restauré"
    editorSaveFailed = "Échec de l'enregistrement"
    editorSaved = "Enregistré"
    msgCannotOpenFile = "Impossible d'ouvrir le fichier"
    msgCopySaved = "Copie enregistrée"
    msgCopySaveFailed = "Impossible d'enregistrer la copie"
    msgUnknownError = "erreur inconnue"
    msgImageTruncated = "Note longue : l'image ne garde que le début. Utilisez le PDF pour un texte long."
    msgNoShareTarget = "aucune application de partage disponible"
    msgSystemUnsupported = "non pris en charge par le système"
    msgExportFailed = { "Échec de l'export : $it" }
    msgShareFailed = { "Échec du partage : $it" }
    msgImagePickerFailed = { "Impossible d'ouvrir le sélecteur d'images : $it" }
    exportChooseFormat = "Choisir le format à partager"
    exportHint = "Choisissez un format et un emplacement : c'est aussi la conversion."
    exportDirectionVertical = "Image et PDF suivent la mise en page de la note : verticale"
    exportDirectionHorizontal = "Image et PDF suivent la mise en page de la note : horizontale"
    previewFontSize = "Taille de la police"
    previewSyntax = "Syntaxe"
    previewCodeHighlight = "Coloration du code"
    previewHeadingAnchors = "Ancres des titres"
    previewMath = "Formules mathématiques"
    previewToc = "Sommaire"
    pdfOpenFailed = "Impossible d'ouvrir ce PDF"
    pdfImportAsNote = "Importer comme note"
    pdfPage = { "Page $it" }
    readerThemeDefault = "Par défaut"
    readerThemePlain = "Blanc uni"
    readerThemeTinted = "Teinté"
    readerThemeDim = "Nuit"
    readerThemeInk = "Encre"
    msgNoFolder = "Aucun dossier sélectionné : impossible de créer la note"
    msgCannotCreateNote = "Impossible de créer la note dans ce dossier"
    msgWriteFailed = "Écriture impossible. Vérifiez l'autorisation du dossier."
    msgNoPersistablePermission = "Cet emplacement ne permet pas un accès durable. Choisissez un autre dossier."
    msgCannotReadFile = "Impossible de lire ce fichier"
    msgPdfLinked = "PDF d'origine lié"
    defaultImportTitle = "Fichier importé"
    importDocument = "Importer un fichier"
    defaultNotebook = "Carnet par défaut"
    statsWeekdays = listOf("L", "M", "M", "J", "V", "S", "D")
    backdropDefault = "Par défaut"
    backdropRice = "Papier de riz"
    backdropCream = "Crème"
    backdropMint = "Menthe"
    backdropSky = "Ciel"
    backdropRose = "Rose"
    backdropGraphite = "Graphite"
    backdropInk = "Encre"
    formatPlainText = "Texte brut"
    formatWord = "Document Word"
    formatPdf = "Document PDF"
    formatImage = "Image"
    fontSizeSmall = "Petite"
    fontSizeStandard = "Standard"
    fontSizeLarge = "Grande"
    fontSizeHuge = "Très grande"
}

fun AppLanguage.appStrings(): AppStrings = when (this.resolved()) {
    AppLanguage.SYSTEM -> ZhTwStrings
    AppLanguage.ZH_TW -> ZhTwStrings
    AppLanguage.ZH_CN -> ZhCnStrings
    AppLanguage.EN_US -> EnUsStrings
    AppLanguage.FR_FR -> FrFrStrings
}

val LocalAppStrings = staticCompositionLocalOf { ZhTwStrings }
