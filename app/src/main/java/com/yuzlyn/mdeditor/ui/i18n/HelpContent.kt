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
    tagline = "資料夾優先的 Markdown 筆記",
    intro = "文書匣子把筆記留在你自己的資料夾裡。透過系統的資料夾授權挑一個目錄，所有 .md 檔就直接存在那裡，不上傳雲端、不需要帳號。",
    sections = listOf(
        HelpSection(
            heading = "功能",
            bullets = listOf(
                "Markdown 編輯與即時預覽：標題、清單、表格、程式碼、腳註、螢光筆與上下標。",
                "支援內聯 HTML：<br> 換行、<span style> 等標籤會在預覽中生效（原始碼檢視不受影響）。",
                "自動保存：停止輸入約一秒就寫回 .md 檔，切到背景或強制關閉也不會掉內容；異常退出後重開會還原草稿。",
                "橫排／直排：直排為由上而下、由右而左的直書，可顯示行號。",
                "Material 3 Expressive 介面：Monet 取色、深色模式、卡片形狀與字體。",
                "語言預設跟隨系統：改系統語言時整個介面跟著換，也可以在「設定 → 語言」固定成繁中／簡中／英文／法文。",
                "平板與橫屏自動換成左側欄：筆記頁與書庫同一套排列，選單、換版面、排序與頭像都收進左邊那一條。",
                "筆記本標籤、全文搜尋、批次刪除、匯出與分享。",
                "匯入 Word／PDF／EPUB／HTML／Markdown／純文字：自動判斷格式，各自用對應的方式解析，卡片上會標示來源。",
                "書庫與閱讀器：匯入 EPUB／MOBI／FB2 後用原生閱讀器讀，直排橫排、翻頁或滾動、字級行距留白與莫奈紙色都可調。",
                "閱讀統計：讀了多久、讀了哪些書、編輯過哪些文件，熱力圖與排行榜全部在本機算；頁面頂端用你的暱稱與頭像跟你打招呼，點頭像就能去補名片資料。",
                "字體：自己匯入 .ttf／.otf，編輯與閱讀各自指定預設字型；編輯器的「樣式」面板與閱讀器底欄都能隨時換，選完就記住。匯入的字型可以刪除。",
                "筆記背景：預設（莫奈）、純色或自己的圖片三選一，圖片可匯入多張、隨時刪除；圖片上的字可指定深色或淺色。",
                "分享與匯出：Markdown／純文字／Word／PDF／圖片；PDF 與圖片照這篇筆記的方向輸出（直排就直排），有輔助線時格子與編輯器同一套字格。附上的個人名片在圖片裡是左邊一枚圓形頭像、右邊暱稱與信箱與 GitHub 三行，兩邊共用同一條水平中線；GitHub 只填使用者名稱就會自動補成 github.com/…；名片要放正文前面還是後面、要不要頭像、暱稱／信箱／GitHub 各印哪幾項，都能在分享面板逐項開關，選過一次就記住（頭像只在圖片格式顯示）。直排筆記的名片改成頭像在上、三欄資料在下，接在最後一欄的左邊；長文會一直往左延伸，整篇都會匯出，不會只剩開頭。",
                "所有頁面切換共用同一套 Material 3 Expressive 轉場，進頁時內容會依序浮上來。",
            )
        ),
        HelpSection(
            heading = "開始使用",
            bullets = listOf(
                "第一次開啟時選擇筆記資料夾，之後可在側邊選單更換。",
                "點右下角新增筆記，輸入的內容會即時寫回 .md 檔。",
                "編輯頁頂端可切換預覽，工具列能快速插入格式。"
            )
        ),
        HelpSection(
            heading = "直排與行號",
            paragraph = "直排的每一欄就是一行，行號標在欄頂。一行太長自動折到下一欄時，續欄仍算同一行，不標號。行號開關在「設定 → 排版」。"
        ),
        HelpSection(
            heading = "閱讀統計",
            paragraph = "側欄的「統計」會記下閱讀時長、打開與編輯過的文件，並用一整年的格子圖看活躍程度，下面是書與文件的排行榜。開關全部在「設定 → 統計」：可以只記其中一項，也可以整個關掉——關掉之後側欄不再出現統計，也不再寫入任何紀錄。"
        ),
        HelpSection(
            heading = "資料與隱私",
            paragraph = "筆記本體是資料夾中的 Markdown 檔；App 只在本機資料庫保存標題、標籤、排版等中繼資料，不需要網路權限，也不會上傳任何內容。閱讀統計同樣只寫在 App 自己的目錄裡。"
        ),
        HelpSection(
            heading = "支持作者",
            paragraph = "這個 App 沒有廣告、沒有訂閱、沒有帳號。覺得還算好用，點側欄的「作者」一列就能進來（頁尾的「yuzlyn」也通），裡面有一列「支持作者」——點進去就是微信支付與支付寶的收款碼，點一下可以放大到滿版，方便對方掃。純屬自願，掃不掃都不影響任何功能。"
        )
    ),
    linksTitle = "專案連結",
    repoLabel = "專案倉庫",
    githubLabel = "yuzlyn"
)

private val ZhCnHelp = HelpDoc(
    tagline = "文件夹优先的 Markdown 笔记",
    intro = "文书匣子把笔记留在你自己的文件夹里。通过系统的文件夹授权选一个目录，所有 .md 文件就直接存在那里，不上传云端、不需要账号。",
    sections = listOf(
        HelpSection(
            heading = "功能",
            bullets = listOf(
                "Markdown 编辑与实时预览：标题、列表、表格、代码、脚注、荧光笔与上下标。",
                "支持内联 HTML：<br> 换行、<span style> 等标签会在预览中生效（源码视图不受影响）。",
                "自动保存：停止输入约一秒就写回 .md 文件，切到后台或强制关闭也不会丢内容；异常退出后重开会还原草稿。",
                "横排／竖排：竖排为从上到下、从右到左的直书，可显示行号。",
                "Material 3 Expressive 界面：Monet 取色、深色模式、卡片形状与字体。",
                "语言默认跟随系统：改系统语言时整个界面跟着换，也可以在「设置 → 语言」固定为繁中／简中／英文／法文。",
                "平板与横屏自动换成左侧栏：笔记页与书库同一套排列，菜单、换布局、排序与头像都收进左边那一条。",
                "笔记本标签、全文搜索、批量删除、导出与分享。",
                "导入 Word／PDF／EPUB／HTML／Markdown／纯文本：自动识别格式，各自用对应的方式解析，卡片上会标注来源。",
                "书库与阅读器：导入 EPUB／MOBI／FB2 后用原生阅读器读，横排竖排、翻页或滚动、字号行距留白与莫奈纸色都可调。",
                "阅读统计：读了多久、读了哪些书、编辑过哪些文件，热力图与排行榜全部在本机计算；页面顶部用你的昵称与头像跟你打招呼，点头像就能去补名片资料。",
                "字体：自己导入 .ttf／.otf，编辑与阅读各自指定默认字体；编辑器的「样式」面板与阅读器底栏都能随时换，选完就记住。导入的字体可以删除。",
                "笔记背景：默认（莫奈）、纯色或自己的图片三选一，图片可导入多张、随时删除；图片上的字可指定深色或浅色。",
                "分享与导出：Markdown／纯文字／Word／PDF／图片；PDF 与图片照这篇笔记的方向输出（直排就直排），有辅助线时格子与编辑器同一套字格。附上的个人名片在图片里是左边一枚圆形头像、右边昵称与邮箱与 GitHub 三行，两边共用同一条水平中线；GitHub 只填用户名就会自动补成 github.com/…；名片放正文前面还是后面、要不要头像、昵称／邮箱／GitHub 各印哪几项，都能在分享面板逐项开关，选过一次就记住（头像只在图片格式显示）。直排笔记的名片改成头像在上、三栏资料在下，接在最后一栏的左边；长文会一直往左延伸，整篇都会导出，不会只剩开头。",
                "所有页面切换共用同一套 Material 3 Expressive 转场，进页时内容会依次浮上来。",
            )
        ),
        HelpSection(
            heading = "开始使用",
            bullets = listOf(
                "第一次打开时选择笔记文件夹，之后可在侧边菜单更换。",
                "点右下角新建笔记，输入的内容会实时写回 .md 文件。",
                "编辑页顶端可切换预览，工具栏能快速插入格式。"
            )
        ),
        HelpSection(
            heading = "竖排与行号",
            paragraph = "竖排的每一栏就是一行，行号标在栏顶。一行太长自动折到下一栏时，续栏仍算同一行，不编号。行号开关在「设置 → 排版」。"
        ),
        HelpSection(
            heading = "阅读统计",
            paragraph = "侧栏的「统计」会记下阅读时长、打开与编辑过的文件，并用一整年的格子图看活跃程度，下面是书与文件的排行榜。开关全部在「设置 → 统计」：可以只记其中一项，也可以整个关掉——关掉之后侧栏不再出现统计，也不再写入任何记录。"
        ),
        HelpSection(
            heading = "数据与隐私",
            paragraph = "笔记本体是文件夹中的 Markdown 文件；App 只在本机数据库保存标题、标签、排版等元数据，不需要网络权限，也不会上传任何内容。阅读统计同样只写在 App 自己的目录里。"
        ),
        HelpSection(
            heading = "支持作者",
            paragraph = "这个 App 没有广告、没有订阅、没有账号。觉得还算好用，点侧栏的「作者」一列就能进来（页脚的「yuzlyn」也通），里面有一列「支持作者」——点进去就是微信支付与支付宝的收款码，点一下可以放大到满版，方便对方扫。纯属自愿，扫不扫都不影响任何功能。"
        )
    ),
    linksTitle = "项目链接",
    repoLabel = "项目仓库",
    githubLabel = "yuzlyn"
)

private val EnUsHelp = HelpDoc(
    tagline = "Markdown notes that stay in your folder",
    intro = "DocBox keeps every note inside a folder you own. Pick a directory through the system folder picker and every .md file lives right there — no cloud, no account.",
    sections = listOf(
        HelpSection(
            heading = "Features",
            bullets = listOf(
                "Markdown editing with live preview: headings, lists, tables, code, footnotes, highlights, sub- and superscript.",
                "Inline HTML is rendered in the preview: <br> breaks lines, <span style> and other tags apply as written. Source view stays raw.",
                "Auto-save: writes back to the .md file about a second after you stop typing, flushes when the app goes to the background, and restores the draft if the app is killed.",
                "Horizontal and vertical layout — vertical reads top-to-bottom, right-to-left, with optional line numbers.",
                "Material 3 Expressive UI: Monet colors, dark mode, card shapes and fonts.",
                "The language follows your system by default; Settings → Language pins it to Traditional Chinese, Simplified Chinese, English or French.",
                "Tablets and landscape switch to a left rail, shared by the notes page and the library — menu, layout, sort and avatar all move into that strip.",
                "Notebook tags, full-text search, batch delete, export and share.",
                "Import Word, PDF, EPUB, HTML, Markdown or plain text — the format is detected and parsed accordingly, and the card shows where it came from.",
                "Library and reader: import EPUB, MOBI or FB2 and read it natively — vertical or horizontal, paged or scrolling, with your own type size, line height, margins and Monet paper.",
                "Reading stats: how long you read, which books and which documents, with a heat grid and a leaderboard computed entirely on device. The header greets you with your nickname and avatar, and tapping the avatar opens your profile card.",
                "Fonts: import your own .ttf/.otf and set a default for editing and for reading separately. The editor's Style sheet and the reader's bottom bar switch them on the spot, and the choice sticks. Imported fonts can be deleted.",
                "Note background: Monet default, a solid colour, or your own image — import several and delete them anytime; text on an image can be set to dark or light.",
                "Share and export: Markdown, plain text, Word, PDF, image. PDF and images follow the note's direction (vertical stays vertical), and with a writing guide the cells use the same character grid as the editor. In images the attached card is a round avatar on the left with nickname, email and GitHub on three lines to its right, both centred on the same horizontal axis; typing just the GitHub username expands to github.com/…; whether the card goes before or after the text, whether it shows your avatar, and which of nickname, email and GitHub to print are all toggles in the share sheet, remembered after the first time (the avatar only appears in images). In vertical notes the card turns too — avatar on top, the three data columns below it — sitting to the left of the last column; long notes keep extending leftwards instead of being cut off.",
                "Every page transition shares one Material 3 Expressive motion spec; content floats up in sequence as a page opens.",
            )
        ),
        HelpSection(
            heading = "Getting started",
            bullets = listOf(
                "Choose your notes folder on first launch; change it later from the side menu.",
                "Tap the plus button to add a note — typing is written back to the .md file as you go.",
                "Switch to preview from the top bar; the toolbar inserts common formatting."
            )
        ),
        HelpSection(
            heading = "Vertical layout and line numbers",
            paragraph = "In vertical layout each column is one line and the number sits at the top of the column. When a line is too long and flows into the next column, that column is still the same line and stays unnumbered. The switch lives in Settings → Layout."
        ),
        HelpSection(
            heading = "Reading stats",
            paragraph = "The Stats page records how long you read and which documents you opened or edited, shows a year of activity as a grid, and ranks your books and documents below it. Every switch lives in Settings → Stats: keep just one of them, or turn the whole thing off — the page disappears from the drawer and nothing more is written. Records never leave the device."
        ),
        HelpSection(
            heading = "Data and privacy",
            paragraph = "Notes are plain Markdown files in your folder. The app keeps only local metadata (title, tags, layout) in an on-device database, asks for no network permission and uploads nothing. Reading stats stay in the app’s own directory too."
        ),
        HelpSection(
            heading = "Supporting the author",
            paragraph = "No ads, no subscription, no account. If the app earns its place on your phone, the drawer has an Author entry (the “yuzlyn” link in the footer works too) and a “Support the author” row inside: the WeChat Pay and Alipay codes live there, and tapping one enlarges it to full width for scanning. Entirely optional; nothing is locked behind it."
        )
    ),
    linksTitle = "Project links",
    repoLabel = "Repository",
    githubLabel = "yuzlyn"
)

private val FrFrHelp = HelpDoc(
    tagline = "Des notes Markdown dans votre dossier",
    intro = "BoîteDoc garde chaque note dans un dossier qui vous appartient. Choisissez un répertoire via le sélecteur système : tous les fichiers .md y restent — sans cloud ni compte.",
    sections = listOf(
        HelpSection(
            heading = "Fonctionnalités",
            bullets = listOf(
                "Édition Markdown avec aperçu en direct : titres, listes, tableaux, code, notes de bas de page, surlignage, indice et exposant.",
                "Le HTML en ligne est rendu dans l'aperçu : <br> coupe la ligne, <span style> et les autres balises s'appliquent. La vue source reste brute.",
                "Enregistrement automatique : écriture dans le fichier .md une seconde après la dernière frappe, sauvegarde en arrière-plan et restauration du brouillon si l'application est tuée.",
                "Mise en page horizontale ou verticale — la verticale se lit de haut en bas et de droite à gauche, numéros de ligne en option.",
                "Interface Material 3 Expressive : couleurs Monet, mode sombre, formes de carte et polices.",
                "La langue suit le système par défaut ; Réglages → Langue permet de la fixer en chinois traditionnel, chinois simplifié, anglais ou français.",
                "Sur tablette et en paysage, l'interface passe en rail latéral, partagé par les notes et la bibliothèque : menu, disposition, tri et avatar y déménagent.",
                "Étiquettes de carnet, recherche plein texte, suppression par lot, export et partage.",
                "Importez Word, PDF, EPUB, HTML, Markdown ou texte brut : le format est détecté, analysé comme il faut, et la carte indique son origine.",
                "Bibliothèque et lecteur : importez EPUB, MOBI ou FB2 et lisez-les nativement — vertical ou horizontal, page ou défilement, avec vos tailles, interlignes, marges et papier Monet.",
                "Statistiques : durée de lecture, livres et documents, avec une grille d'activité et un classement calculés sur l'appareil. L'en-tête vous salue avec votre pseudo et votre avatar, et toucher l'avatar ouvre votre carte de visite.",
                "Polices : importez vos .ttf/.otf et choisissez une police par défaut pour l'édition et pour la lecture. Le panneau « Style » de l'éditeur et la barre du lecteur en changent à tout moment, le choix est retenu. Les polices importées peuvent être supprimées.",
                "Fond de note : Monet par défaut, couleur unie ou votre image — importez-en plusieurs et supprimez-les quand vous voulez ; le texte sur une image peut être foncé ou clair.",
                "Partage et export : Markdown, texte brut, Word, PDF, image. Les PDF et les images suivent l'orientation de la note (vertical reste vertical) et, avec des lignes d'écriture, la grille est la même que dans l'éditeur. En image, la carte jointe montre un avatar rond à gauche et, à sa droite, le pseudo, l'e-mail et GitHub sur trois lignes, les deux centrés sur le même axe horizontal ; le nom d'utilisateur seul devient github.com/… ; sa position (avant ou après le texte), l'avatar et les champs à imprimer (pseudo, e-mail, GitHub) se règlent séparément dans la feuille de partage et sont mémorisés (l'avatar n'apparaît que dans les images). En vertical, la carte pivote aussi — l'avatar en haut, les trois colonnes de données en dessous — à gauche de la dernière colonne ; les notes longues s'étendent vers la gauche au lieu d'être coupées.",
                "Toutes les transitions d'écran partagent la même spécification Material 3 Expressive ; le contenu monte en séquence à l'ouverture.",
            )
        ),
        HelpSection(
            heading = "Premiers pas",
            bullets = listOf(
                "Choisissez votre dossier de notes au premier lancement ; modifiable depuis le menu latéral.",
                "Touchez le bouton plus pour créer une note : la saisie est écrite dans le fichier .md au fil de l'eau.",
                "Basculez vers l'aperçu depuis la barre du haut ; la barre d'outils insère les formats courants."
            )
        ),
        HelpSection(
            heading = "Mise en page verticale et numéros de ligne",
            paragraph = "En vertical, chaque colonne correspond à une ligne et le numéro s'affiche en haut de la colonne. Une ligne trop longue qui passe dans la colonne suivante reste la même ligne et n'est pas numérotée. L'interrupteur se trouve dans Réglages → Mise en page."
        ),
        HelpSection(
            heading = "Statistiques",
            paragraph = "La page Stats enregistre votre durée de lecture et les documents ouverts ou modifiés, affiche un an d'activité sous forme de grille et classe vos livres et documents. Tous les interrupteurs sont dans Réglages → Stats : n'en garder qu'un, ou tout désactiver — la page disparaît du tiroir et plus rien n'est écrit. Les données ne quittent jamais l'appareil."
        ),
        HelpSection(
            heading = "Données et confidentialité",
            paragraph = "Les notes sont de simples fichiers Markdown dans votre dossier. L'application ne conserve que des métadonnées locales (titre, étiquettes, mise en page), ne demande aucune permission réseau et n'envoie rien. Les statistiques restent dans son propre dossier."
        ),
        HelpSection(
            heading = "Soutenir l'auteur",
            paragraph = "Aucune publicité, aucun abonnement, aucun compte. Si l'application vous est utile, le tiroir latéral propose une entrée « Auteur » (le lien « yuzlyn » en bas de page fonctionne aussi) et une entrée « Soutenir l'auteur » à l'intérieur : les codes WeChat Pay et Alipay s'y trouvent, et un appui les agrandit sur toute la largeur pour le scan. Purement facultatif, rien n'est verrouillé."
        )
    ),
    linksTitle = "Liens du projet",
    repoLabel = "Dépôt",
    githubLabel = "yuzlyn"
)
