@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.wzl.duskreader.tv.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.BuildConfig
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

private data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>,
)

private data class SettingsItem(
    val title: String,
    val subtitle: String,
    val meta: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val rescanSummary by viewModel.rescanSummary.collectAsStateWithLifecycle()
    val childPadding = rememberChildPadding()

    val sections = listOf(
        SettingsSection(
            title = "书库与权限",
            items = listOf(
                SettingsItem(
                    title = "重新扫描书库",
                    subtitle = "重新识别 Documents/暮阅 目录中的 TXT / EPUB 文件",
                    meta = rescanSummary,
                    actionLabel = "扫描",
                    action = viewModel::rescanLibrary,
                ),
                SettingsItem(
                    title = "存储权限",
                    subtitle = "进入系统权限页，调整本地书库访问授权",
                    meta = "打开系统设置",
                    actionLabel = "打开",
                    action = { context.openStoragePermissionSettings() },
                ),
            ),
        ),
        SettingsSection(
            title = "阅读偏好",
            items = listOf(
                SettingsItem(
                    title = "阅读界面设置",
                    subtitle = "在阅读时按 OK 键调出字号、行距、段距与背景主题",
                    meta = "阅读页内调整",
                ),
            ),
        ),
        SettingsSection(
            title = "数据与维护",
            items = listOf(
                SettingsItem(
                    title = "本地数据说明",
                    subtitle = "阅读进度保存在本地 Room 数据库，卸载应用后会清空",
                    meta = "仅保存在本机",
                ),
                SettingsItem(
                    title = "传书服务",
                    subtitle = "默认通过局域网地址 `http://<电视 IP>:8080` 接收文件",
                    meta = "端口 8080",
                ),
            ),
        ),
        SettingsSection(
            title = "关于应用",
            items = listOf(
                SettingsItem(
                    title = "暮阅 TV",
                    subtitle = "本地小说海报墙与沉浸式阅读体验",
                    meta = "v${BuildConfig.VERSION_NAME}",
                ),
                SettingsItem(
                    title = "应用标识",
                    subtitle = BuildConfig.APPLICATION_ID,
                    meta = context.getString(context.applicationInfo.labelRes),
                ),
            ),
        ),
    )

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
                            Color(0xFF151E2D),
                            Color(0xFF0C1018),
                            Color(0xFF05070B),
                        ),
                    ),
                ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 40.dp,
                bottom = 84.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                SettingsStage(summary = rescanSummary)
            }

            items(sections) { section ->
                SettingsSectionBlock(section = section)
            }
        }
    }
}

@Composable
private fun SettingsStage(summary: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "系统与维护",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                text = "设置",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = "维护书库访问权限、阅读偏好与本地数据状态，保持 TV 端浏览和阅读体验稳定。",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.74f),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.38f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.08f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "书库状态",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.56f),
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionBlock(section: SettingsSection) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Column(
            modifier = Modifier.focusGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            section.items.forEach { item ->
                SettingsRow(item = item)
            }
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItem,
) {
    if (item.action == null) {
        ReadOnlySettingsRow(item = item)
        return
    }

    Surface(
        onClick = item.action,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f),
            focusedContainerColor = Color.White.copy(alpha = 0.13f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.meta,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.62f),
                )
                Text(
                    text = item.actionLabel ?: "打开",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ReadOnlySettingsRow(
    item: SettingsItem,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Text(
                text = item.meta,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.62f),
            )
        }
    }
}

private fun android.content.Context.openStoragePermissionSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName"),
        )
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
