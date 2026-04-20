package com.wzl.duskreader.tv.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.presentation.screens.Screens
import com.wzl.duskreader.tv.presentation.theme.IconSize
import com.wzl.duskreader.tv.presentation.theme.JetStreamCardShape
import com.wzl.duskreader.tv.presentation.theme.LexendExa
import com.wzl.duskreader.tv.presentation.utils.occupyScreenSize

val TopBarTabs = Screens.entries.toList().filter { it.isTabItem }

val TopBarFocusRequesters = List(size = TopBarTabs.size) { FocusRequester() }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DashboardTopBar(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    screens: List<Screens> = TopBarTabs,
    focusRequesters: List<FocusRequester> = remember { TopBarFocusRequesters },
    onScreenSelection: (screen: Screens) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.surface)
                .focusRestorer(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var isTabRowFocused by remember { mutableStateOf(false) }

                TabRow(
                    modifier = Modifier
                        .onFocusChanged {
                            isTabRowFocused = it.isFocused || it.hasFocus
                        },
                    selectedTabIndex = selectedTabIndex,
                    indicator = { tabPositions, _ ->
                        if (selectedTabIndex >= 0) {
                            DashboardTopBarItemIndicator(
                                currentTabPosition = tabPositions[selectedTabIndex],
                                anyTabFocused = isTabRowFocused,
                                shape = JetStreamCardShape,
                            )
                        }
                    },
                    separator = { Spacer(modifier = Modifier) },
                ) {
                    screens.forEachIndexed { index, screen ->
                        key(index) {
                            Tab(
                                modifier = Modifier
                                    .height(40.dp)
                                    .focusRequester(focusRequesters[index]),
                                selected = index == selectedTabIndex,
                                onFocus = { onScreenSelection(screen) },
                                onClick = { focusManager.moveFocus(FocusDirection.Down) },
                            ) {
                                TabContent(screen = screen)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            DuskReaderLogo(
                modifier = Modifier
                    .alpha(0.75f)
                    .padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun TabContent(screen: Screens) {
    Row(
        modifier = Modifier
            .occupyScreenSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        screen.tabIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(IconSize),
            )
        }
        val label = screen.tabLabel ?: screen()
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                color = LocalContentColor.current,
            ),
        )
    }
}

@Composable
private fun DuskReaderLogo(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.AutoStories,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(IconSize),
        )
        Text(
            text = "暮阅",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            fontFamily = LexendExa,
        )
    }
}
