@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.reader

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ReaderScreen {
    const val BookIdBundleKey = "bookId"
}

@Composable
fun ReaderScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val totalProgress by viewModel.totalProgress.collectAsStateWithLifecycle()
    val currentChapterTitle by viewModel.currentChapterTitle.collectAsStateWithLifecycle()
    val pagingRequestVersion by viewModel.pagingRequestVersion.collectAsStateWithLifecycle()
    val pendingPageIndex by viewModel.pendingPageIndex.collectAsStateWithLifecycle()
    val bookTitle by viewModel.bookTitle.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var consumedMenuKeyDownAt by remember { mutableLongStateOf(0L) }

    val fontSize = readerSettings.fontSize
    val currentTheme = readerSettings.theme
    val lineSpacing = readerSettings.lineSpacing
    val pageTurnMode = readerSettings.pageTurnMode
    val autoTurnSeconds = readerSettings.autoTurnSeconds
    var readerViewportSize by remember { mutableStateOf(IntSize.Zero) }

    val readerPanelRequester = remember { FocusRequester() }
    val controlPrimaryRequester = remember { FocusRequester() }
    val tocFirstItemRequester = remember { FocusRequester() }
    val settingsFirstRowRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val pagerState = rememberPagerState(pageCount = { maxOf(1, pages.size) })

    val animatedBgColor by animateColorAsState(
        targetValue = currentTheme.bgColor,
        animationSpec = tween(durationMillis = 320),
        label = "readerBg",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = currentTheme.textColor,
        animationSpec = tween(durationMillis = 320),
        label = "readerText",
    )

    val textStyle = TextStyle(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineSpacing).sp,
        color = animatedTextColor,
        letterSpacing = 0.4.sp,
        textAlign = TextAlign.Justify,
        fontFamily = FontFamily.Serif,
        shadow = Shadow(
            color = Color.Black.copy(alpha = if (currentTheme == ReaderTheme.NightBlack) 0.28f else 0.08f),
            offset = Offset(0f, 2f),
            blurRadius = 4f,
        ),
    )

    val immersive = !showControls && !showToc && !showSettings

    LaunchedEffect(showControls, showToc, showSettings, isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        delay(80)
        runCatching {
            when {
                showToc -> tocFirstItemRequester.requestFocus()
                showSettings -> settingsFirstRowRequester.requestFocus()
                showControls -> controlPrimaryRequester.requestFocus()
                else -> readerPanelRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(isLoaded, pagingRequestVersion, readerViewportSize) {
        if (!isLoaded || readerViewportSize == IntSize.Zero) return@LaunchedEffect
        viewModel.performPaging(
            textMeasurer = textMeasurer,
            containerConstraints = Constraints(
                maxWidth = readerViewportSize.width,
                maxHeight = readerViewportSize.height,
            ),
            textStyle = textStyle,
        )
    }

    LaunchedEffect(fontSize, lineSpacing) {
        if (!isLoaded || pages.isEmpty()) return@LaunchedEffect
        delay(200)
        viewModel.repaginate()
    }

    LaunchedEffect(pagerState.currentPage, pages.size) {
        if (pages.isNotEmpty()) {
            viewModel.onPageChanged(pagerState.currentPage)
        }
    }

    LaunchedEffect(pages.size, pendingPageIndex) {
        if (pages.isEmpty()) return@LaunchedEffect
        when {
            pendingPageIndex != null -> {
                val targetIndex = pendingPageIndex!!.coerceIn(0, pages.lastIndex)
                pagerState.scrollToPage(targetIndex)
                viewModel.onPageChanged(targetIndex)
                viewModel.consumePendingPageIndex()
            }
            pagerState.currentPage > pages.lastIndex -> {
                pagerState.scrollToPage(pages.lastIndex)
            }
        }
    }

    fun moveForward() {
        if (pages.isEmpty()) return
        if (pagerState.currentPage < pages.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
            viewModel.loadNextChapter()
        }
    }

    fun moveBackward() {
        if (pages.isEmpty()) return
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            viewModel.loadPreviousChapter()
        }
    }

    // 自动翻页：进入 immersive + AUTO 模式后启动；任何菜单弹出 / 模式切换 / 章节切换 → 协程取消（暂停）
    LaunchedEffect(pageTurnMode, autoTurnSeconds, immersive, isLoaded, pages.size) {
        if (pageTurnMode != PageTurnMode.AUTO) return@LaunchedEffect
        if (!immersive || !isLoaded || pages.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(autoTurnSeconds * 1000L)
            moveForward()
        }
    }

    var nowText by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) {
        while (true) {
            nowText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            delay(60_000)
        }
    }

    fun exitReader() {
        if (isExiting) return
        isExiting = true
        viewModel.saveProgressBeforeExit(onBackPressed)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .onPreviewKeyEvent { event ->
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> when (event.type) {
                        KeyEventType.KeyDown -> when {
                            event.nativeKeyEvent.downTime == consumedMenuKeyDownAt -> true
                            immersive -> {
                                consumedMenuKeyDownAt = event.nativeKeyEvent.downTime
                                showControls = true
                                true
                            }
                            else -> false
                        }

                        KeyEventType.KeyUp -> {
                            val shouldConsume = event.nativeKeyEvent.downTime == consumedMenuKeyDownAt
                            if (shouldConsume) consumedMenuKeyDownAt = 0L
                            shouldConsume
                        }

                        else -> false
                    }

                    else -> {
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_BACK -> {
                                    when {
                                        showToc -> showToc = false
                                        showSettings -> {
                                            showSettings = false
                                            showControls = true
                                        }

                                        showControls -> showControls = false
                                        else -> exitReader()
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_DPAD_LEFT -> if (immersive && pageTurnMode == PageTurnMode.HORIZONTAL) {
                                    moveBackward()
                                    true
                                } else {
                                    false
                                }

                                KeyEvent.KEYCODE_DPAD_RIGHT -> if (immersive && pageTurnMode == PageTurnMode.HORIZONTAL) {
                                    moveForward()
                                    true
                                } else {
                                    false
                                }

                                KeyEvent.KEYCODE_DPAD_UP -> if (immersive && pageTurnMode == PageTurnMode.VERTICAL) {
                                    moveBackward()
                                    true
                                } else {
                                    false
                                }

                                KeyEvent.KEYCODE_DPAD_DOWN -> if (immersive && pageTurnMode == PageTurnMode.VERTICAL) {
                                    moveForward()
                                    true
                                } else {
                                    false
                                }

                                else -> false
                            }
                        }
                    }
                }
            }
            .focusRequester(readerPanelRequester)
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = READER_HORIZONTAL_PADDING.dp, vertical = READER_VERTICAL_PADDING.dp)
                .onSizeChanged { newSize ->
                    if (newSize.width > 0 && newSize.height > 0 && newSize != readerViewportSize) {
                        readerViewportSize = newSize
                    }
                },
        ) {
            when {
                !isLoaded -> LoadingText(
                    text = "加载中…",
                    color = animatedTextColor,
                    modifier = Modifier.align(Alignment.Center),
                )

                pages.isEmpty() -> LoadingText(
                    text = "准备正文中…",
                    color = animatedTextColor,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                ) { index ->
                    ReaderPageContent(
                        page = pages[index],
                        textStyle = textStyle,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 52.dp, vertical = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = currentChapterTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.66f),
                        )
                    }

                    Surface(
                        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = String.format(Locale.ROOT, "%.0f%%", totalProgress * 100),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                            Text(
                                text = nowText,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(horizontal = 52.dp, vertical = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "第 ${pagerState.currentPage + 1} 页 / ${pages.size.coerceAtLeast(1)} 页",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                    Text(
                        text = when (pageTurnMode) {
                            PageTurnMode.HORIZONTAL -> "左右键翻页，确定键呼出菜单"
                            PageTurnMode.VERTICAL -> "上下键翻页，确定键呼出菜单"
                            PageTurnMode.AUTO -> "自动翻页中，确定键呼出菜单可暂停"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.56f),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(
                        icon = Icons.Default.AutoStories,
                        label = "目录",
                        onClick = {
                            showToc = true
                            showControls = false
                        },
                        modifier = Modifier
                            .focusRequester(controlPrimaryRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                    )

                    ControlButton(
                        icon = Icons.Default.Settings,
                        label = "设置",
                        onClick = {
                            showSettings = true
                            showControls = false
                        },
                        modifier = Modifier.focusProperties {
                            up = FocusRequester.Cancel
                            down = FocusRequester.Cancel
                        },
                    )

                    ControlButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "退出",
                        onClick = { exitReader() },
                        modifier = Modifier.focusProperties {
                            right = FocusRequester.Cancel
                            up = FocusRequester.Cancel
                            down = FocusRequester.Cancel
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ReaderSettingsOverlay(
                currentFontSize = fontSize,
                currentTheme = currentTheme,
                currentLineSpacing = lineSpacing,
                currentPageTurnMode = pageTurnMode,
                currentAutoTurnSeconds = autoTurnSeconds,
                firstItemRequester = settingsFirstRowRequester,
                onFontSizeChange = viewModel::updateFontSize,
                onThemeChange = viewModel::updateTheme,
                onLineSpacingChange = viewModel::updateLineSpacing,
                onPageTurnModeChange = viewModel::updatePageTurnMode,
                onAutoTurnSecondsChange = viewModel::updateAutoTurnSeconds,
            )
        }

        AnimatedVisibility(
            visible = showToc,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                    },
                colors = SurfaceDefaults.colors(
                    containerColor = Color(0xFF171717),
                    contentColor = Color.White,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Text(
                        text = "章节目录",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .focusRestorer(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(chapters) { index, chapter ->
                            Surface(
                                onClick = {
                                    viewModel.jumpToChapter(chapter.index)
                                    showToc = false
                                    showControls = false
                                },
                                modifier = if (index == 0) Modifier.focusRequester(tocFirstItemRequester) else Modifier,
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color.Black,
                                    contentColor = Color.White,
                                ),
                                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                            ) {
                                Text(
                                    text = chapter.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LoadingText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
private fun ReaderPageContent(
    page: ReaderPage,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = page.content,
            style = textStyle,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val READER_HORIZONTAL_PADDING = 72
private const val READER_VERTICAL_PADDING = 56
