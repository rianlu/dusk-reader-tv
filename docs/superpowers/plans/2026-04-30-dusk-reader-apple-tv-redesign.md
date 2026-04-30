# Dusk Reader Apple TV Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the non-reader experience around an Apple TV style poster-first browsing model, add robust default visual assets, and prepare the data model for hero backdrops.

**Architecture:** The redesign starts at the data and asset layer so every screen can render consistent poster and backdrop imagery. Shared presentation components are upgraded first, then browsing screens (`Home`, `Library`, `BookDetails`) are rebuilt around a stage-plus-poster-wall structure, and finally utility screens (`Transfer`, `Settings`) are aligned to the same visual system without changing their core behavior.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Room, Coil, Android resource drawables, Gradle unit tests

---

### Task 1: Add Backdrop Support And Default Visual Asset Strategy

**Files:**
- Modify: `app/src/main/java/com/wzl/duskreader/tv/data/entities/Book.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/data/repositories/BookRepositoryImpl.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/network/FileTransferServer.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/common/BookCover.kt`
- Create: `app/src/main/java/com/wzl/duskreader/tv/presentation/common/BookBackdrop.kt`
- Create: `app/src/main/res/drawable/default_book_cover.xml`
- Create: `app/src/main/res/drawable/default_book_backdrop.xml`
- Test: `app/src/test/java/com/wzl/duskreader/tv/data/entities/BookVisualPolicyTest.kt`

- [ ] **Step 1: Write the failing tests for default visual policy**

```kotlin
package com.wzl.duskreader.tv.data.entities

import org.junit.Assert.assertEquals
import org.junit.Test

class BookVisualPolicyTest {

    @Test
    fun resolveBackdropPath_prefersExplicitBackdrop() {
        val book = Book(
            title = "Test",
            path = "/tmp/test.txt",
            format = "TXT",
            coverPath = "/covers/cover.png",
            backdropPath = "/backdrops/backdrop.png",
        )

        assertEquals("/backdrops/backdrop.png", book.preferredBackdropPath())
    }

    @Test
    fun resolveBackdropPath_fallsBackToCoverPath() {
        val book = Book(
            title = "Test",
            path = "/tmp/test.txt",
            format = "TXT",
            coverPath = "/covers/cover.png",
        )

        assertEquals("/covers/cover.png", book.preferredBackdropPath())
    }
}
```

- [ ] **Step 2: Run the targeted test to confirm it fails**

Run: `./gradlew testDebugUnitTest --tests com.wzl.duskreader.tv.data.entities.BookVisualPolicyTest`
Expected: FAIL because `backdropPath` and `preferredBackdropPath()` do not exist yet.

- [ ] **Step 3: Extend the book model and repository defaults**

```kotlin
@Immutable
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val path: String,
    val coverPath: String? = null,
    val backdropPath: String? = null,
    val description: String? = null,
    val format: String,
    val tags: List<String> = emptyList(),
    val importedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val lastReadChapter: Int = 0,
    val lastReadPosition: Int = 0,
    val lastReadTime: Long = System.currentTimeMillis(),
    val totalSize: Long = 0,
)

fun Book.preferredBackdropPath(): String? = when {
    !backdropPath.isNullOrBlank() -> backdropPath
    !coverPath.isNullOrBlank() -> coverPath
    else -> null
}
```

```kotlin
private fun buildImportedBook(file: File): Book {
    return Book(
        title = file.nameWithoutExtension,
        path = file.absolutePath,
        format = file.extension.uppercase(),
        fileSize = file.length(),
        totalSize = file.length(),
    )
}
```

```kotlin
uploadedBook = if (existing != null) {
    existing.copy(
        title = safeName.substringBeforeLast("."),
        format = safeName.substringAfterLast(".").uppercase(),
        fileSize = destFile.length(),
        totalSize = destFile.length(),
        lastReadPosition = 0,
        lastReadTime = System.currentTimeMillis(),
    ).also { repository.update(it) }
} else {
    Book(
        title = safeName.substringBeforeLast("."),
        path = destFile.absolutePath,
        format = safeName.substringAfterLast(".").uppercase(),
        fileSize = destFile.length(),
        totalSize = destFile.length(),
    ).also { repository.insert(it) }
}
```

- [ ] **Step 4: Add shared default cover and backdrop renderers**

```kotlin
@Composable
fun BookBackdrop(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val path = book.preferredBackdropPath()
    if (!path.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(path)
                .crossfade(true)
                .build(),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.default_book_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
```

```kotlin
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val coverPath = book.coverPath
    if (!coverPath.isNullOrBlank()) {
        AsyncImage(
            modifier = modifier,
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(coverPath)
                .build(),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
        )
    } else {
        DefaultBookCover(book = book, modifier = modifier)
    }
}
```

```xml
<!-- app/src/main/res/drawable/default_book_cover.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@android:color/black" />
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="315"
                android:startColor="#171A22"
                android:centerColor="#20283A"
                android:endColor="#0D1118" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</layer-list>
```

```xml
<!-- app/src/main/res/drawable/default_book_backdrop.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="0"
                android:startColor="#0D1118"
                android:centerColor="#162033"
                android:endColor="#0A0E15" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step 5: Run the targeted test and the full unit suite**

Run: `./gradlew testDebugUnitTest --tests com.wzl.duskreader.tv.data.entities.BookVisualPolicyTest`
Expected: PASS

Run: `./gradlew testDebugUnitTest`
Expected: PASS with no failures.

### Task 2: Upgrade Shared Poster Components For A Poster-Wall UI

**Files:**
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/common/BookCard.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/common/BooksRow.kt`
- Create: `app/src/main/java/com/wzl/duskreader/tv/presentation/common/PosterWallRow.kt`
- Test: `app/src/test/java/com/wzl/duskreader/tv/presentation/screens/home/HomePosterGroupingTest.kt`

- [ ] **Step 1: Write a test for home row grouping behavior**

```kotlin
package com.wzl.duskreader.tv.presentation.screens.home

import com.wzl.duskreader.tv.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePosterGroupingTest {

    @Test
    fun buildHomeShelves_keepsRecentFirstAndLimitsShelfSize() {
        val books = (1L..12L).map { id ->
            Book(
                id = id,
                title = "Book $id",
                path = "/tmp/$id.txt",
                format = "TXT",
                importedAt = id,
                lastReadPosition = if (id <= 3) 100 else 0,
                lastReadTime = 1000L - id,
            )
        }

        val shelves = buildHomeShelves(books, books.take(3))

        assertEquals("继续阅读", shelves.first().title)
        assertEquals(3, shelves.first().books.size)
        assertEquals(true, shelves.drop(1).all { it.books.size <= 10 })
    }
}
```

- [ ] **Step 2: Run the targeted test to confirm it fails**

Run: `./gradlew testDebugUnitTest --tests com.wzl.duskreader.tv.presentation.screens.home.HomePosterGroupingTest`
Expected: FAIL because `buildHomeShelves` does not exist.

- [ ] **Step 3: Refactor shared card and row components for poster-first behavior**

```kotlin
@Composable
fun BookCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    image: @Composable BoxScope.() -> Unit,
) {
    StandardCardContainer(
        modifier = modifier,
        title = title,
        imageCard = {
            Surface(
                onClick = onClick,
                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                    ),
                ),
                content = image,
            )
        },
    )
}
```

```kotlin
data class PosterShelf(
    val title: String,
    val books: List<Book>,
)

internal fun buildHomeShelves(
    allBooks: List<Book>,
    recentBooks: List<Book>,
): List<PosterShelf> {
    val recentImports = allBooks
        .sortedByDescending { it.importedAt }
        .take(10)
    val unreadBooks = allBooks
        .filter { it.lastReadPosition <= 0 }
        .take(10)

    return buildList {
        if (recentBooks.isNotEmpty()) add(PosterShelf("继续阅读", recentBooks.take(10)))
        if (recentImports.isNotEmpty()) add(PosterShelf("最近导入", recentImports))
        if (unreadBooks.isNotEmpty()) add(PosterShelf("未开始阅读", unreadBooks))
        add(PosterShelf("全部书库", allBooks.take(20)))
    }
}
```

- [ ] **Step 4: Introduce a dedicated poster wall row**

```kotlin
@Composable
fun PosterWallRow(
    title: String,
    books: List<Book>,
    onBookSelected: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(modifier = modifier.focusGroup()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = rememberChildPadding().start, bottom = 14.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(
                start = rememberChildPadding().start,
                end = rememberChildPadding().end,
            ),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .focusRequester(lazyRow)
                .focusRestorer { firstItem },
        ) {
            itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                val itemModifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier
                BookCard(
                    modifier = itemModifier.width(184.dp),
                    onClick = { onBookSelected(book) },
                    image = {
                        BookCover(
                            book = book,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(10.5f / 16f),
                        )
                    },
                    title = {},
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run the new targeted test and the full unit suite**

Run: `./gradlew testDebugUnitTest --tests com.wzl.duskreader.tv.presentation.screens.home.HomePosterGroupingTest`
Expected: PASS

Run: `./gradlew testDebugUnitTest`
Expected: PASS

### Task 3: Rebuild Home And Library As Stage-Plus-Poster-Wall Screens

**Files:**
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/home/HomeScreenViewModel.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreenViewModel.kt`
- Test: `app/src/test/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreenViewModelTest.kt`

- [ ] **Step 1: Extend the home view model for stage data**

```kotlin
data class HomeStage(
    val featuredBook: Book?,
    val shelves: List<PosterShelf>,
)

val uiState: StateFlow<HomeScreenUiState> = combine(
    bookRepository.getRecentBooks(limit = 10),
    bookRepository.getAllBooks(),
) { recent, all ->
    val stageBook = recent.firstOrNull() ?: all.maxByOrNull { it.importedAt }
    HomeScreenUiState.Ready(
        recentBooks = recent,
        allBooks = all,
        stage = HomeStage(
            featuredBook = stageBook,
            shelves = buildHomeShelves(all, recent),
        ),
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = HomeScreenUiState.Loading,
)
```

- [ ] **Step 2: Rebuild the home screen as a hero stage plus poster rows**

```kotlin
@Composable
private fun HomeStageHero(
    book: Book,
    onOpenBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(352.dp),
    ) {
        BookBackdrop(
            book = book,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = childPadding.start, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = book.title, style = MaterialTheme.typography.displaySmall)
            Text(text = stageSubtitleForBook(book), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onOpenBook) { Text("继续阅读") }
        }
    }
}
```

- [ ] **Step 3: Rebuild the library screen into a clean poster wall**

```kotlin
LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Fixed(6),
    contentPadding = PaddingValues(
        start = childPadding.start,
        end = childPadding.end,
        top = 28.dp,
        bottom = 108.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LibraryControls(
            selectedSort = selectedSort,
            selectedFilter = selectedFilter,
            onSortChange = onSortChange,
            onFilterChange = onFilterChange,
        )
    }

    items(books, key = { it.id }) { book ->
        BookCard(
            modifier = Modifier.width(184.dp),
            onClick = { onBookClick(book) },
            image = {
                BookCover(
                    book = book,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(10.5f / 16f),
                )
            },
        )
    }
}
```

- [ ] **Step 4: Extend the library tests for sort and filter stability**

```kotlin
@Test
fun presentLibraryBooks_titleSort_isAlphabeticalWithinFilter() {
    val books = listOf(
        testBook(id = 1, title = "Gamma", format = "TXT"),
        testBook(id = 2, title = "Alpha", format = "TXT"),
        testBook(id = 3, title = "Beta", format = "TXT"),
    )

    val result = presentLibraryBooks(books, LibrarySortOption.Title, LibraryFormatFilter.Txt)

    assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
}
```

- [ ] **Step 5: Run the library and home tests, then the full suite**

Run: `./gradlew testDebugUnitTest --tests com.wzl.duskreader.tv.presentation.screens.home.HomePosterGroupingTest --tests com.wzl.duskreader.tv.presentation.screens.library.LibraryScreenViewModelTest`
Expected: PASS

Run: `./gradlew testDebugUnitTest`
Expected: PASS

### Task 4: Rebuild Book Details, Transfer, And Settings Around The New Visual System

**Files:**
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/bookDetails/BookDetailsScreen.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/transfer/TransferScreen.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/transfer/TransferScreenViewModel.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/wzl/duskreader/tv/presentation/screens/settings/SettingsScreenViewModel.kt`

- [ ] **Step 1: Rebuild the book details screen with a backdrop-led layout**

```kotlin
Box(modifier = modifier.fillMaxSize()) {
    BookBackdrop(
        book = book,
        modifier = Modifier.fillMaxSize(),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.78f),
                        Color.Black.copy(alpha = 0.52f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = childPadding.start, end = childPadding.end, top = 48.dp, bottom = childPadding.bottom),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(
            book = book,
            modifier = Modifier.width(260.dp).aspectRatio(10.5f / 16f),
        )
        DetailsContent(book = book, onStartReading = onStartReading)
    }
}
```

- [ ] **Step 2: Rebuild transfer as a device-pairing style screen**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(56.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TransferHeroCard(qrCode = qrCode, url = url)
    TransferStatusPanel(
        helperMessage = helperMessage,
        lastUploadText = lastUploadText,
        onCopyAddress = onCopyAddress,
        onRefresh = onRefresh,
    )
}
```

- [ ] **Step 3: Rebuild settings as grouped entry rows**

```kotlin
data class SettingsGroupEntry(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val onClick: () -> Unit,
)

val groups = listOf(
    SettingsGroupEntry(
        title = "书库与权限",
        subtitle = rescanSummary,
        actionLabel = "管理",
        onClick = { viewModel.rescanLibrary() },
    ),
    SettingsGroupEntry(
        title = "阅读偏好",
        subtitle = "阅读内可调整字号、行距、段距和背景主题",
        actionLabel = "查看",
        onClick = {},
    ),
    SettingsGroupEntry(
        title = "数据与维护",
        subtitle = "检查书库状态并刷新本地内容",
        actionLabel = "刷新",
        onClick = { viewModel.rescanLibrary() },
    ),
    SettingsGroupEntry(
        title = "关于应用",
        subtitle = "${BuildConfig.APPLICATION_ID} · ${BuildConfig.VERSION_NAME}",
        actionLabel = "信息",
        onClick = {},
    ),
)
```

- [ ] **Step 4: Ensure the transfer and settings view models still drive real actions**

```kotlin
fun refresh() {
    fileTransferServer.refresh()
}

fun rescanLibrary() {
    viewModelScope.launch {
        runCatching {
            val count = bookRepository.scanLocalStorage()
            _rescanSummary.value = "扫描完成，当前书库共 $count 本"
        }.onFailure { error ->
            _rescanSummary.value = "扫描失败：${error.message ?: "未知错误"}"
        }
    }
}
```

- [ ] **Step 5: Run the full unit suite after the screen rebuild**

Run: `./gradlew testDebugUnitTest`
Expected: PASS

### Task 5: Final Verification, Build, And Documentation Sync

**Files:**
- Modify: `docs/superpowers/specs/2026-04-30-dusk-reader-functional-design.md`
- Modify: `docs/superpowers/specs/2026-04-30-dusk-reader-apple-tv-visual-design.md`

- [ ] **Step 1: Update the functional design status to reflect implemented poster-wall direction**

```markdown
### 5.3 首页

- `已实现` 顶部舞台区
- `已实现` 继续阅读海报墙
- `已实现` 最近导入海报墙
- `已实现` 默认封面兜底
- `部分实现` 真实封面来源仍以本地资源为主
```

- [ ] **Step 2: Update the Apple TV visual design doc status markers if implementation deviates**

```markdown
**状态**: 已进入实现

### 已落地范围

- 默认竖版海报与默认横版背景
- 首页舞台区 + 海报墙
- 书库页海报浏览布局
```

- [ ] **Step 3: Run the final build verification**

Run: `./gradlew assembleDebug`
Expected: PASS

- [ ] **Step 4: Run the final test verification**

Run: `./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Report remaining gaps against the spec**

Run: `git diff --stat`
Expected: Shows the redesign scope touching book visuals, poster wall screens, and docs.
