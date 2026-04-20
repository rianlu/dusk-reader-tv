package com.wzl.duskreader.tv.ui.screen

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.ui.component.LoadingIndicator
import com.wzl.duskreader.tv.ui.component.ReaderSettingsOverlay
import com.wzl.duskreader.tv.ui.theme.rememberDimensions
import com.wzl.duskreader.tv.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val dimensions = rememberDimensions()
    val pages by viewModel.pages.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val totalProgress by viewModel.totalProgress.collectAsState()
    val isPaging by viewModel.isPaging.collectAsState()
    val currentChapterTitle by viewModel.currentChapterTitle.collectAsState()
    val snapToEndAfterPaging by viewModel.snapToEndAfterPaging.collectAsState()

    var showControls by remember { mutableStateOf(false) }
    var showTOC by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }

    var fontSize by remember { mutableIntStateOf(22) }
    var currentTheme by remember { mutableStateOf(ReaderTheme.Cinematic) }
    var isVertical by remember { mutableStateOf(false) }
    val isSerif by remember { mutableStateOf(false) }
    val brightness by remember { mutableFloatStateOf(1f) }
    var lineSpacing by remember { mutableFloatStateOf(1.8f) }
    var paragraphSpacing by remember { mutableIntStateOf(16) }

    val readerPanelRequester = remember { FocusRequester() }
    val prevPageRequester = remember { FocusRequester() }
    val tocButtonRequester = remember { FocusRequester() }
    val settingsButtonRequester = remember { FocusRequester() }
    val nextPageRequester = remember { FocusRequester() }
    val tocFirstItemRequester = remember { FocusRequester() }
    val settingsFirstRowRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val pagerState = rememberPagerState(pageCount = { maxOf(1, pages.size) })

    LaunchedEffect(showControls, showTOC, showReaderSettings, isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        delay(80)
        try {
            when {
                showTOC -> tocFirstItemRequester.requestFocus()
                showReaderSettings -> settingsFirstRowRequester.requestFocus()
                showControls -> prevPageRequester.requestFocus()
                else -> readerPanelRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    val textStyle = TextStyle(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineSpacing).sp,
        color = currentTheme.textColor,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Justify,
        fontFamily = if (isSerif) FontFamily.Serif else FontFamily.SansSerif
    )

    LaunchedEffect(isLoaded, fontSize, currentTheme, lineSpacing, dimensions.readerHorizontalPadding) {
        if (isLoaded && pages.isEmpty()) {
            val constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = (1920 - dimensions.readerHorizontalPadding.value.toInt() * 2 - dimensions.readerColumnGap.value.toInt()) / 2,
                maxHeight = (1080 - dimensions.readerVerticalPadding.value.toInt() * 2)
            )
            viewModel.performPaging(textMeasurer, constraints, textStyle)
        }
    }

    LaunchedEffect(pagerState.currentPage, pages) {
        if (pages.isNotEmpty()) {
            viewModel.onPageChanged(pagerState.currentPage)
        }
    }

    LaunchedEffect(pages.size, snapToEndAfterPaging) {
        if (pages.isEmpty()) return@LaunchedEffect
        when {
            snapToEndAfterPaging -> {
                pagerState.scrollToPage(pages.lastIndex)
                viewModel.onPageChanged(pages.lastIndex)
                viewModel.consumeSnapToEndFlag()
            }
            pagerState.currentPage > pages.lastIndex -> pagerState.scrollToPage(pages.lastIndex)
        }
    }

    fun moveForward() {
        if (pages.isEmpty()) return
        if (pagerState.currentPage < pages.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
            viewModel.loadNextWindow()
        }
    }

    fun moveBackward() {
        if (pages.isEmpty()) return
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            viewModel.loadPreviousWindow()
        }
    }

    val nowText = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    val immersive = !showControls && !showTOC && !showReaderSettings

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bgColor)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        if (immersive) {
                            showControls = true
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        when {
                            showTOC -> showTOC = false
                            showReaderSettings -> showReaderSettings = false
                            showControls -> showControls = false
                            else -> onBack()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (immersive && !isVertical) {
                            moveBackward()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (immersive && !isVertical) {
                            moveForward()
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .focusRequester(readerPanelRequester)
            .focusable()
    ) {
        // 主内容（Pager）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensions.readerHorizontalPadding,
                    vertical = dimensions.readerVerticalPadding
                )
        ) {
            when {
                !isLoaded -> LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = (48 * dimensions.scale).dp,
                    strokeWidth = (4 * dimensions.scale).dp,
                    message = "加载中..."
                )
                isPaging -> LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = (48 * dimensions.scale).dp,
                    strokeWidth = (4 * dimensions.scale).dp,
                    message = "排版中..."
                )
                pages.isEmpty() -> Text(
                    "内容为空",
                    color = currentTheme.textColor,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    pageSpacing = dimensions.readerColumnGap
                ) { index ->
                    val pageText = pages.getOrNull(index).orEmpty()
                    val mid = pageText.length / 2
                    val leftText = pageText.substring(0, mid)
                    val rightText = pageText.substring(mid)

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.readerColumnGap)
                    ) {
                        Text(text = leftText, style = textStyle, modifier = Modifier.weight(1f))
                        Text(text = rightText, style = textStyle, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 顶部信息栏（只读）
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(
                        horizontal = dimensions.contentPadding,
                        vertical = dimensions.spacingL
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "正在阅读",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Text(
                            text = viewModel.bookTitle(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = currentChapterTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Surface(
                        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = dimensions.spacingS,
                                vertical = dimensions.spacingXS
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.0f%%", totalProgress * 100),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(dimensions.spacingS))
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size((14 * dimensions.scale).dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(dimensions.spacingXS))
                            Text(
                                text = nowText,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 底部控制栏（4 按钮 focusGroup）
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(
                        horizontal = dimensions.contentPadding,
                        vertical = dimensions.spacingL
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.0f%% 已读", totalProgress * 100),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = currentChapterTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacingM))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        icon = Icons.Default.ChevronLeft,
                        label = "上一页",
                        onClick = { moveBackward() },
                        modifier = Modifier
                            .focusRequester(prevPageRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)) {
                        ControlButton(
                            icon = Icons.Default.AutoStories,
                            label = "目录",
                            onClick = {
                                showTOC = true
                                showControls = false
                            },
                            modifier = Modifier
                                .focusRequester(tocButtonRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = FocusRequester.Cancel
                                }
                        )
                        ControlButton(
                            icon = Icons.Default.Settings,
                            label = "设置",
                            onClick = {
                                showReaderSettings = true
                                showControls = false
                            },
                            modifier = Modifier
                                .focusRequester(settingsButtonRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = FocusRequester.Cancel
                                }
                        )
                    }

                    ControlButton(
                        icon = Icons.Default.ChevronRight,
                        label = "下一页",
                        trailingIcon = true,
                        onClick = { moveForward() },
                        modifier = Modifier
                            .focusRequester(nextPageRequester)
                            .focusProperties {
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            }
                    )
                }
            }
        }

        // 阅读设置面板
        AnimatedVisibility(
            visible = showReaderSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ReaderSettingsOverlay(
                currentFontSize = fontSize,
                currentTheme = currentTheme,
                isVertical = isVertical,
                currentLineSpacing = lineSpacing,
                currentParagraphSpacing = paragraphSpacing,
                firstItemRequester = settingsFirstRowRequester,
                onFontSizeChange = { fontSize = it },
                onThemeChange = { currentTheme = it },
                onFlipModeChange = { isVertical = it },
                onLineSpacingChange = { lineSpacing = it },
                onParagraphSpacingChange = { paragraphSpacing = it },
                onConfirm = {
                    showReaderSettings = false
                    showControls = true
                },
                onDismiss = {
                    showReaderSettings = false
                    showControls = true
                }
            )
        }

        // 目录面板
        AnimatedVisibility(
            visible = showTOC,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((400 * dimensions.scale).dp)
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                    },
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensions.spacingL)
                ) {
                    Text(
                        "章节目录",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(dimensions.spacingS)
                    )
                    Spacer(modifier = Modifier.height(dimensions.spacingS))
                    TvLazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .focusRestorer(),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXS)
                    ) {
                        itemsIndexed(chapters) { index, chapter ->
                            Surface(
                                onClick = {
                                    viewModel.jumpToOffset(chapter.offset)
                                    showTOC = false
                                    showControls = true
                                },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(tocFirstItemRequester)
                                } else Modifier,
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color.Black
                                ),
                                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium)
                            ) {
                                Text(
                                    text = chapter.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(dimensions.spacingM),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 亮度遮罩
        if (brightness < 1.0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1.0f - brightness))
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: Boolean = false
) {
    val dimensions = rememberDimensions()
    Surface(
        onClick = onClick,
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensions.spacingL,
                vertical = dimensions.spacingM
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXS)
        ) {
            if (!trailingIcon) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            } else {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            }
        }
    }
}
