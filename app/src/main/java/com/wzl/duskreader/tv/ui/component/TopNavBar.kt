package com.wzl.duskreader.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.ui.navigation.Screen
import com.wzl.duskreader.tv.ui.theme.AccentBlue
import com.wzl.duskreader.tv.ui.theme.BackgroundPrimary

/**
 * 一个顶部导航项，绑定屏幕、Tab 焦点锚点和下键跳转的内容区锚点。
 */
data class TopNavItem(
    val screen: Screen,
    val tabRequester: FocusRequester,
    val contentRequester: FocusRequester
)

/**
 * 顶部导航栏：Logo + TabRow（基于 TV Material3 原生 Tab）。
 *
 * 焦点流：
 * - 外层 Row 加 focusRestorer() → 从内容区回 TopBar 时恢复上次聚焦的 Tab
 * - Tab 聚焦即切屏（onFocus），省一次 OK 按键
 * - 按 OK 直接把焦点下移到内容区
 * - 每个 Tab 的 down 通过 focusProperties 显式绑定到对应 contentRequester
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavBar(
    selectedScreen: Screen,
    items: List<TopNavItem>,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val selectedIndex = items.indexOfFirst { it.screen == selectedScreen }.coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundPrimary)
            .padding(horizontal = 48.dp, vertical = 16.dp)
            .focusRestorer(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = AccentBlue
            )
            Text(
                text = "Dusk Reader",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.width(40.dp))

        TabRow(
            selectedTabIndex = selectedIndex,
            separator = { Spacer(modifier = Modifier.width(4.dp)) }
        ) {
            items.forEachIndexed { index, item ->
                key(index) {
                    Tab(
                        modifier = Modifier
                            .height(48.dp)
                            .focusRequester(item.tabRequester)
                            .focusProperties {
                                down = item.contentRequester
                                up = FocusRequester.Cancel
                            },
                        selected = index == selectedIndex,
                        onFocus = { onScreenSelected(item.screen) },
                        onClick = { focusManager.moveFocus(FocusDirection.Down) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = item.screen.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = LocalContentColor.current
                            )
                            Text(
                                text = item.screen.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
