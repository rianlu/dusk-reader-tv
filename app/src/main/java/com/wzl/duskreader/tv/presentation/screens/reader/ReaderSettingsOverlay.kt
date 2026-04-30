@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import java.util.Locale

@Composable
fun ReaderSettingsOverlay(
    currentFontSize: Int,
    currentTheme: ReaderTheme,
    currentLineSpacing: Float,
    currentParagraphSpacing: Int,
    firstItemRequester: FocusRequester,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onParagraphSpacingChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(460.dp)
            .focusGroup()
            .focusRestorer()
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
                    .fillMaxSize()
                    .padding(horizontal = 36.dp, vertical = 40.dp),
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "调整后选择“确认应用”生效",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.58f),
                )

                Spacer(modifier = Modifier.height(40.dp))

                StepperField(
                    label = "字体大小",
                    value = "${currentFontSize}px",
                    onDecrement = { onFontSizeChange((currentFontSize - 2).coerceIn(18, 80)) },
                    onIncrement = { onFontSizeChange((currentFontSize + 2).coerceIn(18, 80)) },
                    decrementRequester = firstItemRequester,
                    modifier = Modifier.focusProperties { up = FocusRequester.Cancel },
                )

                Spacer(modifier = Modifier.height(28.dp))

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
                )

                Spacer(modifier = Modifier.height(28.dp))

                StepperField(
                    label = "段间距",
                    value = "${currentParagraphSpacing}dp",
                    onDecrement = { onParagraphSpacingChange((currentParagraphSpacing - 2).coerceIn(8, 32)) },
                    onIncrement = { onParagraphSpacingChange((currentParagraphSpacing + 2).coerceIn(8, 32)) },
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "背景主题",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ReaderTheme.values().forEach { theme ->
                        ThemeOption(
                            theme = theme,
                            selected = currentTheme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onThemeChange(theme) },
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                        colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("确认应用", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            androidx.tv.material3.Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusProperties { down = FocusRequester.Cancel },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White.copy(alpha = 0.64f),
                        ),
                    ) {
                        Text("取消更改")
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
    Row(
        modifier = modifier
            .fillMaxWidth()
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
private fun ThemeOption(
    theme: ReaderTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
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
                    modifier = Modifier.size(width = 72.dp, height = 84.dp),
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
