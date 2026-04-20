package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.ui.component.SectionHeader
import com.wzl.duskreader.tv.ui.theme.TextPrimary
import com.wzl.duskreader.tv.ui.theme.rememberDimensions

@Composable
fun SettingsScreen(
    entryRequester: FocusRequester,
    tabUpRequester: FocusRequester,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = rememberDimensions()
    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions.contentPadding),
        contentPadding = PaddingValues(bottom = dimensions.contentPadding),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
    ) {
        item {
            SectionHeader(
                title = "设置",
                subtitle = "应用配置和信息"
            )
        }

        item {
            SettingPanel(
                title = "返回主页",
                description = "点击返回到书籍列表",
                onClick = onBackHome,
                modifier = Modifier
                    .focusRequester(entryRequester)
                    .focusProperties { up = tabUpRequester }
            )
        }

        item {
            SettingPanel(
                title = "应用信息",
                description = "Dusk Reader TV 专注于本地阅读和电视遥控器交互,提供稳定的浏览、传书和阅读体验。"
            )
        }

        item {
            SettingPanel(
                title = "阅读偏好",
                description = "阅读器内可调整字号、主题和翻页模式。进入阅读器后按确认键呼出控制层即可调整。"
            )
        }

        item {
            SettingPanel(
                title = "缓存与数据",
                description = "书籍文件保存在 Documents/暮阅。移出书架只会删除应用记录,不会主动删除本地原文件。",
                modifier = Modifier.focusProperties { down = FocusRequester.Cancel }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingPanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dimensions = rememberDimensions()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = TextPrimary,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.spacingL),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
