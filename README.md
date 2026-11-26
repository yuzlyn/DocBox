# DocBox

[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.02.01-blueviolet.svg)](https://developer.android.com/develop/ui/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Expressive-6750A4.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

DocBox is a Markdown notebook and book reader for Android, written in Kotlin with Jetpack Compose and Material 3 Expressive.

Notes are plain `.md` files inside a folder you pick through the system folder picker. The app keeps only metadata (title, tags, layout) in a local Room database. There is no account, no sync and no internet permission. Imported books stay in the app's own directory and are read in a built-in reader that shares the editor's typography, including vertical (top-to-bottom, right-to-left) layout. It also registers itself for *Open with*, so a document can come straight from another app.

[简体中文说明](README.zh-CN.md)

## Features

### Writing

- **Markdown with live preview.** markdown-it with the GFM flavour: tables, task lists, footnotes, `==highlight==`, `~sub~`, `^sup^`, definition lists and attribute blocks, plus syntax highlighting and MathJax formulas in a bundled WebView.
- **Inline HTML.** Tags such as `<br>`, `<span style="…">`, `<u>`, `<kbd>` and `<details>` render as written, the way the VS Code Markdown preview does. Scripts, iframes, event-handler attributes and `javascript:` URLs are stripped; the source view always shows the raw text.
- **Auto-save that survives a crash.** A draft lands in Room about 400 ms after you stop typing, the `.md` file is written about a second later, and the app flushes again when it goes to the background. If the process is killed mid-edit, the draft is written back on the next launch.
- **Horizontal or vertical.** Vertical mode reads top-to-bottom and right-to-left. Character advance (1.2em) and column pitch (1.72em) are set separately for vertical text, and a kinsoku pass keeps closing punctuation off the top of a column. When a long line wraps into the next column it stays the same line and is not numbered twice.
- **Writing grid.** Grid or ruled paper behind the text, filling the whole writing area rather than stopping at the last line, and using the same cell pitch as the exported image, so the editor and the shared picture are the same paper. In vertical mode every column — written or still blank — shares one pitch, so the cells stay aligned straight across the page.
- **Two layouts.** A masonry grid, or a single-column list. The list keeps a one-line excerpt and the date so a row is never just a title, and the source-format badge rides at the end of the title's last line in both.
- **Undo and redo** across edits, with the caret scrolled back into view.

### Import

- `.docx`, `.pdf`, `.epub`, `.mobi` / `.azw3`, `.fb2`, `.html`, `.markdown` and plain text. The format is detected from the extension first and the file header second, so a file without an extension still opens.
- Each format has its own reader: Word's OOXML, PDFBox for the PDF text layer, OPF/spine for EPUB, and an HTML to Markdown converter that keeps headings, lists, links and code fences.
- Imported notes carry a small badge with the source format. A note imported from a PDF keeps a link to the original file so the editor can reopen the real layout; older notes can be re-linked once, and a start-up check drops links whose permission is gone.
- **Open with.** Word, PDF, HTML, Markdown, plain text and ebooks are declared in the manifest, both by MIME type and by extension, so DocBox shows up in another app's *Open with* menu. Ebooks go to the reader, everything else is imported as a note; a `.docx` is recognised as a document rather than a zipped book.

### Library and reader

- EPUB, MOBI and FB2 books on a home shelf, with reading progress kept per book.
- Native reader with horizontal or vertical layout, paged or scrolled, adjustable font size, line height and margins, and a Monet-derived paper tint.
- Imported fonts and note backgrounds apply to the reader as well.
- **Long-press to select.** Hold a book to enter selection; the top bar becomes a count with share and delete. Sharing sends the original file itself — no re-encoding and no temporary copy — and works for several books at once. Deleting always goes through selection, so cards keep the same height when you press them.
- **Two layouts** here as well, and the same staggered entrance as the note lists.

### Share and export

- Markdown, plain text, Word (`.docx`), PDF and PNG.
- PDF and PNG follow the note's direction: a vertical note exports vertical, with the same grid as the editor.
- PNG export can carry a profile card. Horizontal notes get the avatar on the left with nickname, email and GitHub on three lines to its right, both centred on the same horizontal axis. Vertical notes get the avatar on top with the three data columns below it. Position (before or after the text), avatar and each field can be toggled in the share sheet and are remembered. Long vertical notes keep extending leftwards instead of being cut off.
- Markdown and plain-text exports print the card as three lines of text; PDF prints the text without the avatar.

### Reading statistics

- Reading time, books read and files edited, all computed on device.
- A year-long heat map plus rankings for books and files.
- The stats screen greets you with your nickname and avatar. Each category can be switched off in Settings, and turning statistics off stops writes entirely.

### Look and feel

- Material 3 Expressive: Monet dynamic colour from the wallpaper, custom seed colours, dark mode, card shapes and bundled fonts.
- Import your own `.ttf` / `.otf` fonts and set a default for the editor and for the reader separately. Imported fonts can be deleted again.
- Note background: Monet, a solid colour, or your own image (several can be imported). Text on an image can be forced dark or light.
- Four languages: 繁體中文, 简体中文, English, Français. Switch at runtime, or leave it on *Follow system* so the app matches whatever the device is set to. Every screen is covered, including error messages.
- On tablets and foldables the library switches to a side rail from 840dp wide: an 80dp navigation rail, a top bar that keeps only the search field, and a card grid that sizes its columns instead of stretching them.
- One set of Material 3 Expressive transitions across screens, with content appearing in sequence.
- Cards rise into place at 45 ms intervals with a spring; only the first screenful animates, so scrolling never replays it.
- The launcher icon is part of the theme: six glyphs over the active palette, chosen in Settings. The splash screen uses that same icon and follows light/dark and the colour scheme, so it never flashes the wrong colour.

## Install

Download the APK from [Releases](https://github.com/yuzlyn/DocBox/releases). Android 8.0 (API 26) or newer.

The APK is signed with the Android debug certificate, the same one used since v1.0.0, so it installs over an older version without uninstalling first. It is not a Play Store build.

## Build

Requirements: JDK 17 or newer, and an Android SDK with platform 37 and its build tools.

```bash
git clone https://github.com/yuzlyn/DocBox.git
cd DocBox
./gradlew assembleDebug
```

`./gradlew test` runs the JVM test suite. `./gradlew connectedDebugAndroidTest` runs the instrumentation tests, which include a rendering suite that writes sample PNG and PDF exports to the app's external files directory for visual inspection.

A release build (`./gradlew assembleRelease`) is signed with the debug keystore named in `app/build.gradle.kts`. Point it at your own keystore before publishing anywhere else.

## Tech stack

| Category | Technology |
|---|---|
| Language | [Kotlin](https://kotlinlang.org) 2.2.10 |
| UI | [Jetpack Compose](https://developer.android.com/develop/ui/compose) (BOM 2026.02.01) + [Material 3 Expressive](https://m3.material.io) 1.5.0-alpha26 |
| Navigation | [Navigation 3](https://developer.android.com/guide/navigation) 1.2.1 |
| Architecture | MVVM, `ViewModel` + `StateFlow` |
| Markdown rendering | [markdown-it](https://github.com/markdown-it/markdown-it) 14 + [highlight.js](https://highlightjs.org) + [MathJax](https://www.mathjax.org), bundled into a WebView preview engine |
| File access | [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) (DocumentFile) 1.0.1 |
| Local data | [Room](https://developer.android.com/training/data-storage/room) 2.8.5, [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.7 |
| PDF text extraction | [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) 2.0.27.0 |
| Colour | [material-kolor](https://github.com/jordond/materialkolor) 4.0.5 |
| Build | Gradle 9.4.1, AGP 9.2.1, KSP 2.2.10-2.0.2, version catalog |

## Project layout

```
app/src/main/java/com/yuzlyn/mdeditor/
├── MainActivity.kt              # Entry point, Navigation 3 back stack, transitions
├── data/
│   ├── local/                   # Room database, DAOs, entities, migrations
│   ├── repository/              # Note repository and merge rules
│   ├── settings/                # DataStore-backed AppSettings
│   ├── preview/                 # Preview palette and rendering config
│   ├── profile/                 # Author profile used by the export card
│   ├── book/                    # EPUB / MOBI / FB2 parsing, reader state
│   ├── importer/                # DOCX, PDF, HTML readers
│   ├── export/                  # DOCX writer, PDF/PNG renderer, export card
│   ├── fonts/                   # User font import
│   ├── backgrounds/             # Note background store
│   └── stats/                   # Reading and editing statistics
├── ui/
│   ├── component/               # Vertical body editor, cell text, writing guide
│   ├── i18n/                    # Four-language strings and help content
│   ├── navigation/              # NavKey destinations
│   ├── screen/                  # Editor, library, settings, profile, help
│   ├── reader/                  # Book reader
│   ├── stats/                   # Stats screen and its settings
│   ├── theme/                   # Colour, type, spacing, launcher icon
│   └── viewmodel/               # Screen state holders

app/src/test/                    # JVM tests (parsers, layout, export, stats)
app/src/androidTest/             # Instrumentation and export rendering tests
preview-engine/                  # Source for the bundled WebView preview engine
icon-samples/                    # Launcher icon generators and samples
tools/                           # Helper scripts (test book generation)
```

## Data and privacy

- Note bodies are `.md` files in the folder you picked. Removing the app does not remove them.
- Titles, tags, layout, drafts, statistics and imported books live in the app's private directory.
- The app declares no internet permission, so nothing leaves the device.

## License

[MIT](LICENSE) © 2025 Yuzlyn

---

<p align="center">
  <sub>Built by <a href="https://github.com/yuzlyn">Yuzlyn</a></sub>
</p>
