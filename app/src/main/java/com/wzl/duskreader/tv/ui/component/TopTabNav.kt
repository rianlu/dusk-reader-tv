package com.wzl.duskreader.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.ui.navigation.Screen

@Composable
fun TopTabNav(
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onTabFocused: (Screen) -> Unit,
    focusRequesterFor: (Screen) -> FocusRequester,
    modifier: Modifier = Modifier
) {
    val tabs = Screen.entries.filter { it != Screen.Debug }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00D0D11), // 94% dark - 更强的遮罩
                        Color(0xCC0D0D11), // 80%
                        Color(0x660D0D11), // 40%
                        Color.Transparent
                    )
                )
            )
            .padding(start = 58.dp, end = 58.dp, top = 24.dp, bottom = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // App 名称
        Text(
            text = "Dusk Reader",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.width(48.dp))

        // Tab 项
        tabs.forEach { screen ->
            TabItem(
                screen = screen,
                isSelected = screen == selectedScreen,
                focusRequester = focusRequesterFor(screen),
                onClick = { onScreenSelected(screen) },
                onFocused = { onTabFocused(screen) }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun TabItem(
    screen: Screen,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onFocused: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            pressedContainerColor = Color.White.copy(alpha = 0.18f)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected || isFocused) Color.White else Color.White.copy(alpha = 0.5f)
            )
            // 选中指示线
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
            ) {
                if (isSelected) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.matchParentSize()
                    ) {
                        drawRect(color = Color.White)
                    }
                }
            }
        }
    }
}
