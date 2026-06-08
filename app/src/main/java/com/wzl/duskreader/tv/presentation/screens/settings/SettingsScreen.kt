@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

private const val APP_VERSION_NAME = "1.0"

private data class SettingsAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val action: SettingsActionType,
)

private enum class SettingsActionType {
    Rescan,
    LibraryPath,
    Cache,
    Version,
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = hiltViewModel(),
) {
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val firstItemRequester = remember { FocusRequester() }
    val actions = remember(rescanState) {
        listOf(
            SettingsAction(
                title = "重新扫描书库",
                subtitle = rescanState.subtitle(),
                icon = Icons.Default.Refresh,
                enabled = rescanState !is RescanUiState.Scanning,
                selected = rescanState !is RescanUiState.Idle,
                action = SettingsActionType.Rescan,
            ),
            SettingsAction(
                title = "本地书库目录",
                subtitle = "Documents/暮阅 · 支持 TXT / EPUB",
                icon = Icons.Default.FolderOpen,
                enabled = false,
                action = SettingsActionType.LibraryPath,
            ),
            SettingsAction(
                title = "缓存状态",
                subtitle = "封面缓存和解析缓存由系统自动维护",
                icon = Icons.Default.DeleteOutline,
                enabled = false,
                action = SettingsActionType.Cache,
            ),
            SettingsAction(
                title = "当前版本",
                subtitle = "暮阅 TV $APP_VERSION_NAME",
                icon = Icons.Default.Info,
                enabled = false,
                action = SettingsActionType.Version,
            ),
        )
    }


    DuskScreenBackground(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer { firstItemRequester },
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 96.dp,
                bottom = 108.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                PageHeader(
                    eyebrow = "系统维护",
                    title = "应用设置",
                    subtitle = "管理书库扫描, 本地目录, 缓存状态和版本信息.",
                )
            }
            item {
                SettingsPanel(
                    actions = actions,
                    firstItemRequester = firstItemRequester,
                    onRescan = viewModel::rescan,
                )
            }
            item { LocalFirstPanel() }
        }
    }
}

@Composable
private fun SettingsPanel(
    actions: List<SettingsAction>,
    firstItemRequester: FocusRequester,
    onRescan: () -> Unit,
) {
    PrimaryPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "设置项目",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEachIndexed { index, action ->
                    SettingsRow(
                        action = action,
                        modifier = Modifier.focusRequesterIf(index == 0, firstItemRequester),
                        onClick = {
                            if (action.action == SettingsActionType.Rescan) onRescan()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    action: SettingsAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (action.enabled) {
        ActionSettingsRow(
            action = action,
            modifier = modifier,
            onClick = onClick,
        )
    } else {
        PassiveSettingsRow(
            action = action,
            modifier = modifier,
        )
    }
}

@Composable
private fun ActionSettingsRow(
    action: SettingsAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (focused) Color.Black else Color.White
    val iconContainer = if (focused) {
        Color.Black.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (action.selected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.055f),
            contentColor = contentColor,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (action.selected) 0.24f else 0.10f)),
                shape = MaterialTheme.shapes.large,
            ),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = MaterialTheme.shapes.large),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        SettingsRowContent(
            action = action,
            contentColor = contentColor,
            iconContainer = iconContainer,
            titleAlpha = 1f,
            subtitleAlpha = if (focused) 0.72f else 0.62f,
        )
    }
}

@Composable
private fun PassiveSettingsRow(
    action: SettingsAction,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.035f)),
        shape = MaterialTheme.shapes.large,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = MaterialTheme.shapes.large),
    ) {
        SettingsRowContent(
            action = action,
            contentColor = Color.White,
            iconContainer = Color.White.copy(alpha = 0.07f),
            titleAlpha = 0.58f,
            subtitleAlpha = 0.44f,
        )
    }
}

@Composable
private fun SettingsRowContent(
    action: SettingsAction,
    contentColor: Color,
    iconContainer: Color,
    titleAlpha: Float,
    subtitleAlpha: Float,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            colors = SurfaceDefaults.colors(containerColor = iconContainer),
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = titleAlpha),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor.copy(alpha = titleAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = action.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = subtitleAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LocalFirstPanel() {
    SecondaryPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "本地优先",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "阅读内容和进度保存在电视本地, 无线传书仅在局域网内临时开放.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrimaryPanel(content: @Composable () -> Unit) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(modifier = Modifier.padding(horizontal = 30.dp, vertical = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun SecondaryPanel(content: @Composable () -> Unit) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.045f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            content()
        }
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.52f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = subtitle,
            modifier = Modifier.widthIn(max = 760.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.64f),
        )
    }
}

@Composable
private fun DuskScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070D15)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF17263A).copy(alpha = 0.52f), Color.Transparent),
                        radius = 980f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1420), Color(0xFF08111B), Color(0xFF070D15)),
                    ),
                ),
        )
        content()
    }
}

private fun RescanUiState.subtitle(): String = when (this) {
    RescanUiState.Idle -> "扫描 Documents/暮阅 目录并同步新增书籍"
    RescanUiState.Scanning -> "正在扫描本地目录..."
    is RescanUiState.Done -> "扫描完成, 新增 $imported 本"
    is RescanUiState.Failed -> "扫描失败: $message"
}

private fun Modifier.focusRequesterIf(condition: Boolean, requester: FocusRequester): Modifier {
    return if (condition) focusRequester(requester) else this
}
