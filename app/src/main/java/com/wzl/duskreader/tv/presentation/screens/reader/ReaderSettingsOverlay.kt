@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ReaderSettingsOverlay(
    currentFontSize: Int,
    currentTheme: ReaderTheme,
    currentLineSpacing: Float,
    currentPageTurnMode: PageTurnMode,
    currentAutoTurnSeconds: Int,
    firstItemRequester: FocusRequester,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPageTurnModeChange: (PageTurnMode) -> Unit,
    onAutoTurnSecondsChange: (Int) -> Unit,
) {
    val lineSpacingDecRequester = remember { FocusRequester() }
    val pageTurnFirstRequester = remember { FocusRequester() }
    val autoTurnDecRequester = remember { FocusRequester() }
    val themeFirstRequester = remember { FocusRequester() }

    val isAuto = currentPageTurnMode == PageTurnMode.AUTO
    val pageTurnDownTarget = if (isAuto) autoTurnDecRequester else themeFirstRequester
    val themeUpTarget = if (isAuto) autoTurnDecRequester else pageTurnFirstRequester

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(460.dp)
            .focusGroup()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF111111)),
            shape = RectangleShape,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 28.dp),
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "调整即时生效，按返回键关闭",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f),
                )

                Spacer(modifier = Modifier.height(28.dp))

                StepperField(
                    label = "字体大小",
                    value = "${currentFontSize}px",
                    onDecrement = { onFontSizeChange((currentFontSize - 2).coerceIn(18, 80)) },
                    onIncrement = { onFontSizeChange((currentFontSize + 2).coerceIn(18, 80)) },
                    decrementRequester = firstItemRequester,
                    modifier = Modifier.focusProperties {
                        up = FocusRequester.Cancel
                        down = lineSpacingDecRequester
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                StepperField(
                    label = "行间距",
                    value = String.format(Locale.ROOT, "%.1fx", currentLineSpacing),
                    onDecrement = {
                        val next = ((currentLineSpacing * 10).toInt() - 1).coerceAtLeast(13)
                        onLineSpacingChange(next / 10f)
                    },
                    onIncrement = {
                        val next = ((currentLineSpacing * 10).toInt() + 1).coerceAtMost(24)
                        onLineSpacingChange(next / 10f)
                    },
                    decrementRequester = lineSpacingDecRequester,
                    modifier = Modifier.focusProperties {
                        down = pageTurnFirstRequester
                    },
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "翻页方式",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PageTurnMode.values().forEachIndexed { index, mode ->
                        OptionCard(
                            label = mode.displayName,
                            selected = currentPageTurnMode == mode,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (index == 0) Modifier.focusRequester(pageTurnFirstRequester)
                                    else Modifier,
                                )
                                .focusProperties {
                                    down = pageTurnDownTarget
                                },
                            onClick = { onPageTurnModeChange(mode) },
                        )
                    }
                }

                if (isAuto) {
                    Spacer(modifier = Modifier.height(20.dp))

                    StepperField(
                        label = "自动翻页",
                        value = "${currentAutoTurnSeconds}秒",
                        onDecrement = {
                            onAutoTurnSecondsChange(AutoTurnInterval.decrement(currentAutoTurnSeconds))
                        },
                        onIncrement = {
                            onAutoTurnSecondsChange(AutoTurnInterval.increment(currentAutoTurnSeconds))
                        },
                        decrementRequester = autoTurnDecRequester,
                        modifier = Modifier.focusProperties {
                            up = pageTurnFirstRequester
                            down = themeFirstRequester
                        },
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "背景主题",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .focusGroup()
                        .focusProperties { down = FocusRequester.Cancel },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReaderTheme.values().forEachIndexed { index, theme ->
                        ThemeOption(
                            theme = theme,
                            selected = currentTheme == theme,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (index == 0) Modifier.focusRequester(themeFirstRequester)
                                    else Modifier,
                                )
                                .focusProperties { up = themeUpTarget },
                            onClick = { onThemeChange(theme) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperField(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementRequester: FocusRequester? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .focusGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                contentDescription = "减少",
                onClick = onDecrement,
                modifier = Modifier
                    .then(if (decrementRequester != null) Modifier.focusRequester(decrementRequester) else Modifier)
                    .focusProperties { left = FocusRequester.Cancel },
            )

            Text(
                text = value,
                modifier = Modifier
                    .width(84.dp)
                    .focusProperties { canFocus = false },
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )

            StepperButton(
                icon = Icons.Default.Add,
                contentDescription = "增加",
                onClick = onIncrement,
                modifier = Modifier.focusProperties { right = FocusRequester.Cancel },
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.tv.material3.Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun OptionCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF222222),
                focusedContainerColor = Color(0xFF303030),
            ),
            border = ClickableSurfaceDefaults.border(
                border = if (selected) Border(BorderStroke(2.dp, Color.White)) else Border.None,
                focusedBorder = Border(BorderStroke(2.dp, Color.White)),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ReaderTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF222222),
                focusedContainerColor = Color(0xFF303030),
            ),
            border = ClickableSurfaceDefaults.border(
                border = if (selected) Border(BorderStroke(2.dp, Color.White)) else Border.None,
                focusedBorder = Border(BorderStroke(2.dp, Color.White)),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(width = 60.dp, height = 64.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = SurfaceDefaults.colors(containerColor = theme.bgColor),
                    border = if (theme == ReaderTheme.NightBlack) {
                        Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)))
                    } else {
                        Border.None
                    },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Aa",
                            color = theme.textColor,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.56f),
            textAlign = TextAlign.Center,
        )
    }
}
