# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dusk Reader TV (暮阅)** — Android TV local book reader app supporting TXT and EPUB formats. The UI is built entirely with Jetpack Compose for TV (`androidx.tv.material3`), no Leanback library. The codebase originated from Google's JetStreamCompose sample with significant modifications for a Chinese-language book reader.

## Build & Test Commands

```bash
# Compile debug build
./gradlew :app:compileDebugKotlin

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.wzl.duskreader.tv.data.reader.EpubReaderEngineTest"

# Install on connected TV device
./gradlew installDebug
```

**Build requirements:** JDK 17, AGP 8.8.2, Kotlin 2.1.0, compileSdk 35, minSdk 28.

**Known build quirk:** `ksp.useKSP2=false` in `gradle.properties` is required to avoid JVM signature errors with KSP1 backend. Do not change this.

**Tests** live in `app/src/test/` (JUnit4 + `kotlinx-coroutines-test`) and cover pure logic only — `EpubReaderEngineTest`, `PageTurnModeTest`, `ReadingHistoryPolicyTest`, `UploadFilePolicyTest`. There are no instrumented/UI tests, so keep new business rules in pure functions/policies (see `data/entities/` below) to stay unit-testable.

## Architecture

### Package Structure (`app/src/main/java/com/wzl/duskreader/tv/`)

- `data/entities/` — Room entities (`Book`, `BookChapter`), `@Immutable` for Compose, serving as both data and domain models (no separate domain layer). **Also holds the project's testable business rules** as framework-free `object` policies and extension functions: `UploadFilePolicy` (upload filename sanitization + `txt`/`epub` whitelist), `ReadingHistoryPolicy` (`hasReadingHistory()`, `progressRatio()`). Put new pure logic here, not in ViewModels.
- `data/local/` — Room database, DAOs, type converters
- `data/reader/` — `TxtReaderEngine` and `EpubReaderEngine` for file parsing (standalone, no framework deps)
- `data/repositories/` — Repository interfaces + implementations bound via Hilt
- `network/` — Embedded Ktor CIO HTTP server for wireless file transfer (port 8080), NOT an HTTP client
- `presentation/App.kt` — Root NavHost
- `presentation/screens/` — Screen composables + ViewModels (one package per screen)
- `presentation/common/` — Shared composables (BookCard, BookCover, DuskTvButton, StoragePermissionHandler)
- `presentation/theme/` — Global dark theme (`JetStreamTheme`), typography (Inter font family), shapes
- `presentation/utils/` — D-pad key handlers, focus utilities, gradient backgrounds
- `tvmaterial/` — Custom TV dialog components (StandardDialog, FullScreenDialog)

### MVVM + Hilt

- All ViewModels are `@HiltViewModel` with `@Inject constructor`
- UI state exposed as `StateFlow`, collected via `collectAsStateWithLifecycle()`
- Hilt modules: `DataModule` (Room/DAOs), `BookRepositoryModule` (repository bindings)
- Application: `JetStreamApplication` (`@HiltAndroidApp`), Entry: `MainActivity` (`@AndroidEntryPoint`)

### Two-Level Navigation

- **Outer NavHost** (`App.kt`): `Dashboard` → `BookDetails/{bookId}` → `Reader/{bookId}`
- **Inner NavHost** (`DashboardScreen.kt`): Tab-based — `Home`, `Bookshelf`, `Transfer`, `Settings`
- Routes defined in `Screens` enum with `invoke()` for route templates and `withArgs()` for concrete paths

### Reader Architecture (Core Feature)

The reader uses **chapter-level loading** — only the current chapter (5-50KB) is in memory:

1. `ReaderViewModel.loadBook()` scans chapter indices via `TxtReaderEngine`/`EpubReaderEngine`, persists to Room
2. Chapter text loaded on-demand via `RandomAccessFile.seek()` (TXT) or `ZipFile.getEntry()` (EPUB)
3. `LruCache<Int, String>(3)` caches the 3 most recently read chapters
4. Pagination via `TextMeasurer` on background thread (`Dispatchers.Default`), produces `List<ReaderPage>`
5. Progress stored as `(lastReadChapter, lastReadPosition)` — invariant to font size changes
6. Settings persisted via `SharedPreferences` through `ReaderSettingsStore`

**Non-obvious invariant:** `Book.totalSize` is **reused as the total chapter count** (not byte size) under chapter-level loading — `ReaderViewModel` writes it after the first chapter scan, and `Book.progressRatio()` computes `lastReadChapter / totalSize`. Don't repurpose this field for file size.

**Reader sub-models** (all in `presentation/screens/reader/`): `ReaderPage` + `buildReaderPages` (pagination model, `ReaderSpread.kt`); `PageTurnMode` HORIZONTAL/VERTICAL/AUTO D-pad turning plus `AutoTurnInterval`; `ReaderTheme` + `ReaderTextBrightness` (4 built-in dark themes with brightness multipliers).

**TxtReaderEngine:** Charset detection (BOM → UTF-8 heuristic → GB18030 fallback), regex-based chapter boundary scanning for Chinese (`第X章/节/回/部/集/卷/篇`) and English (`Chapter N`) patterns.

**EpubReaderEngine:** ZIP-based parsing, reads OPF spine for reading order, strips HTML to plain text.

### TV-Specific Patterns

- **No Leanback.** All TV UI uses `androidx.tv.material3` (Surface, TabRow, Glow, Border).
- **Explicit focus graph** — every interactive element has `focusProperties` with explicit `up/down/left/right` neighbors and `FocusRequester.Cancel` boundaries.
- **D-pad key handling** — `ModifierUtils.kt` provides `handleDPadKeyEvents`; reader has the most complex key handling (page turning, overlay toggle, layered back press).
- **Focus restoration** — `focusRestorer()` on TabRow and lists; `createInitialFocusRestorerModifiers()` utility for first-focus patterns.

## Key Data Flow

```
Documents/暮阅/ (file system)
  → BookRepository.scanLocalStorage() (auto-import on permission grant)
  → Room DB (Book + BookChapter tables)
  → BookshelfScreenViewModel (reactive Flow combine)
  → ReaderViewModel (chapter scan → page render → progress save)
```

## Design Docs

Located in `docs/superpowers/specs/`. **The single source of truth for the visual/interaction design system is `2026-05-31-dusk-reader-design-baseline.md`** — all UI tokens (color, corner radius, spacing, focus treatment, typography) are derived from the **Reader screen**, the approved reference implementation. The Reader's signature focus treatment (white fill + black content + `2dp` white border + `focusedScale=1.04`), near-black panels (`0xFF111111/171717/222222`), `shapes` radii (8/12/16dp), and Serif body text are the standard; align other screens to it.

The older `2026-04-30-dusk-reader-apple-tv-visual-design.md` keeps the "content/poster-first" product direction, but its quantized token rules (≤12dp radius, 1.06 scale, 8/12/16/24/32/48/64-only spacing) are **superseded** by the baseline. `2026-04-30-dusk-reader-functional-design.md` tracks per-screen status (corrected: Settings is a visual skeleton with no-op rows; Reader settings are instant-apply, not draft).

> `AGENTS.md` describes a "Trellis" workflow, but the referenced `.trellis/` directory is **not present** in this repo — treat those instructions as inactive.

## Reference Samples

`.reference_tv_samples/` contains Google's official TV samples (JetStreamCompose, Leanback, etc.) used during development. These are excluded from the build and should not be modified.
