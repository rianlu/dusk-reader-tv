# Dusk Reader TV — Agent Guide

This file provides guidance to AI coding agents working in this repository (any model, any harness).

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

**Tests** live in `app/src/test/` (JUnit4 + `kotlinx-coroutines-test`) and cover pure logic only — `EpubReaderEngineTest`, `PageTurnModeTest`, `ReadingHistoryPolicyTest`, `UploadFilePolicyTest`, `LibraryBookFilterTest`, `OpenLibrarySearchParserTest`. There are no instrumented/UI tests, so keep new business rules in pure functions/policies (see `data/entities/` below) to stay unit-testable.

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
- **Focus rules follow `DESIGN.md` §8** — container-level focus governance (restorer with always-composed anchor, pivot scrolling without custom tween spec, `onScroll` purely scroll-driven, minimal per-item `focusProperties`). Do not reintroduce the retired `requestInitialFocusVersion` protocol.
- **D-pad key handling** — `ModifierUtils.kt` provides `handleDPadKeyEvents` and `requestFocusSafely()`; reader has the most complex key handling (page turning, overlay toggle, layered back press).

## Key Data Flow

```
Documents/暮阅/ (file system)
  → BookRepository.scanLocalStorage() (auto-import on permission grant)
  → Room DB (Book + BookChapter tables)
  → BookshelfScreenViewModel (reactive Flow combine)
  → ReaderViewModel (chapter scan → page render → progress save)
```

## Docs

- **`docs/TASKS.md`** — refactoring task checklist with progress tracking (P0–P3). Update it as tasks complete.
- **`DESIGN.md`** (repo root) — the single source of truth for the visual/interaction design system, derived entirely from the Reader screen (the approved reference implementation). Product direction: Apple TV streaming-style content browsing. Follow it for any UI/interaction/navigation change.
- **`docs/analysis/2026-09-01-vs-official-samples.md`** — deep comparison of this app against the two relevant official samples (focus regression root causes, architecture and component-usage findings, prioritized fix plan). Read it before touching focus/scrolling code.
- **`docs/superpowers/specs/2026-07-21-book-source-integration.md`** — the authoritative spec for the custom book-source feature (Legado-compatible sources, network reading), including compliance guardrails and phased plan.

## Reference Samples

`/Users/lu/AIProjects/tv-samples` (sibling checkout of Google's official [android/tv-samples](https://github.com/android/tv-samples)) contains the official TV samples used during development. Only two of them matter for this project:

- **`JetStreamCompose/`** — complete Compose-for-TV streaming app; the architectural origin of this app's Dashboard/TopBar/Details screens.
- **`TvMaterialCatalog/`** — component catalog demonstrating tv-material3 components in isolation (e.g. `PositionFocusedItemInLazyLayout`, the pivot-scroll pattern used in the bookshelf grid).

The other samples (Leanback, LeanbackShowcase, ClassicsKotlin, ReferenceAppKotlin, AccessibilityDemo) use the legacy Leanback stack or focus on video playback / Google TV ecosystem integrations and are not relevant. This checkout lives outside the repo on purpose (not committed, not buildable from here); do not modify it, and do not copy its copyright headers/boilerplate into this repo.
