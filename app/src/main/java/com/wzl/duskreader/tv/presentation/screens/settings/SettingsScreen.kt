@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.wzl.duskreader.tv.presentation.utils.openStoragePermissionSettings
import kotlinx.coroutines.delay

private const val APP_VERSION_NAME = "1.0"

private data class SettingsEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = hiltViewModel(),
) {
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()

    val entries = remember(rescanState) {
        listOf(
            SettingsEntry(
                title = "重新扫描书库",
                subtitle = rescanState.subtitle(),
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                enabled = rescanState !is RescanUiState.Scanning,
                onClick = viewModel::rescan,
            ),
            SettingsEntry(
                title = "存储权限",
                subtitle = "前往系统设置检查或重新授予文件访问权限",
                icon = Icons.Default.Security,
                onClick = { openStoragePermissionSettings(context) },
            ),
            SettingsEntry(
                title = "关于暮阅",
                subtitle = "Android TV 本地 TXT / EPUB 阅读器 · 版本 $APP_VERSION_NAME",
                icon = Icons.Default.Info,
                onClick = {
                    Toast.makeText(
                        context,
                        "暮阅 Dusk Reader TV · 版本 $APP_VERSION_NAME",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        SettingsBackground()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 32.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("header") { SettingsHeader() }

            itemsIndexed(entries, key = { _, e -> e.title }) { index, entry ->
                SettingsRow(
                    listState = listState,
                    entry = entry,
                    index = index,
                )
            }
        }
    }
}

@Composable
private fun SettingsBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF111B28),
                        Color(0xFF0A1018),
                        Color(0xFF05070B),
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.Transparent,
                        Color(0xFF18304E).copy(alpha = 0.18f),
                    ),
                ),
            ),
    )
}

// 精简 header：去掉占空间的大 icon 卡片，只保留标题 + 副标题，与 BookshelfScreen.SectionHeader 风格一致。
@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "应用设置",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Text(
            text = "管理书库扫描、存储权限与应用信息。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun SettingsRow(
    listState: LazyListState,
    entry: SettingsEntry,
    index: Int,
) {
    var isFocused by remember { mutableStateOf(false) }
    val foregroundColor = if (isFocused) Color.Black else Color.White
    val secondaryColor = if (isFocused) Color.Black.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.62f)

    // D-pad 聚焦时自动居中滚动——比 BringIntoViewRequester 更积极，
    // 保证焦点行始终处于屏幕中央，和主流 TV app 体验一致。
    if (isFocused) {
        val viewportDp = LocalConfiguration.current.screenHeightDp
        val centerOffsetPx: Int
        with(androidx.compose.ui.platform.LocalDensity.current) {
            // 视口高度的一半减去预估行高的一半，得到使行居中的 scrollOffset
            centerOffsetPx = ((viewportDp.dp - 90.dp) / 2).roundToPx()
        }
        LaunchedEffect(Unit) {
            // 等一个布局帧让 item 尺寸稳定后再滚动，避免布局抖动。
            requestAnimationFrame()
            listState.animateScrollToItem(index, scrollOffset = -centerOffsetPx)
        }
    }

    Surface(
        onClick = entry.onClick,
        enabled = entry.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF111820),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                colors = SurfaceDefaults.colors(
                    containerColor = if (isFocused) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f),
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = foregroundColor.copy(alpha = 0.86f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = foregroundColor,
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                )
            }
        }
    }
}

private suspend fun requestAnimationFrame() {
    // 16ms ≈ 一帧，让当前布局测量完成后再触发滚动。
    delay(16)
}

private fun RescanUiState.subtitle(): String = when (this) {
    RescanUiState.Idle -> "扫描 Documents/暮阅，导入新的 TXT / EPUB"
    RescanUiState.Scanning -> "正在扫描…"
    is RescanUiState.Done -> if (imported > 0) "已新增 $imported 本，可返回书库查看" else "扫描完成，没有发现新文件"
    is RescanUiState.Failed -> "扫描失败：$message"
}
