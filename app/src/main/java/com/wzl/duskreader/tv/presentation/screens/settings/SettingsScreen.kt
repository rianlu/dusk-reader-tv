@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.wzl.duskreader.tv.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val settingItems = remember { buildSettingsItems() }
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 40.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item { SettingsHeader() }
            settingItems.forEach { group ->
                item { SettingsGroup(group = group) }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.07f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "应用设置",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = "统一管理书库, 阅读偏好, 数据维护和应用信息。",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(group: SettingsSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                group.items.forEachIndexed { index, item ->
                    SettingsRow(
                        item = item,
                        isFirstInGroup = index == 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItem,
    isFirstInGroup: Boolean,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Surface(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    scope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF111820),
            focusedContainerColor = Color(0xFF182230),
            contentColor = Color.White,
            focusedContentColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    item.badge?.let { badge ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.68f),
                            )
                        }
                    }
                }
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
        }
    }
}

private data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>,
)

private data class SettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
)

private fun buildSettingsItems(): List<SettingsSection> = listOf(
    SettingsSection(
        title = "书库与权限",
        items = listOf(
            SettingsItem(
                title = "书库目录",
                subtitle = "TXT / EPUB 默认保存在 Documents/暮阅",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                badge = "本地",
            ),
            SettingsItem(
                title = "重新扫描",
                subtitle = "刷新本地文件索引, 让新书出现在书库中",
                icon = Icons.AutoMirrored.Filled.ManageSearch,
            ),
            SettingsItem(
                title = "存储权限",
                subtitle = "检查 Android TV 系统的文件访问授权",
                icon = Icons.Default.Security,
            ),
        ),
    ),
    SettingsSection(
        title = "阅读偏好",
        items = listOf(
            SettingsItem(
                title = "阅读设置",
                subtitle = "字号, 主题, 行距和翻页模式会在阅读器内实时保存",
                icon = Icons.Default.Tune,
                badge = "已持久化",
            ),
            SettingsItem(
                title = "继续阅读",
                subtitle = "打开书籍后自动恢复最近阅读章节和进度",
                icon = Icons.Default.AutoStories,
            ),
        ),
    ),
    SettingsSection(
        title = "数据与应用",
        items = listOf(
            SettingsItem(
                title = "数据维护",
                subtitle = "保留阅读历史与书库索引, 后续可扩展清理入口",
                icon = Icons.Default.SettingsBackupRestore,
            ),
            SettingsItem(
                title = "关于暮阅",
                subtitle = "Android TV 本地阅读器, 面向遥控器和大屏优化",
                icon = Icons.Default.Info,
                badge = "1.0",
            ),
        ),
    ),
)
