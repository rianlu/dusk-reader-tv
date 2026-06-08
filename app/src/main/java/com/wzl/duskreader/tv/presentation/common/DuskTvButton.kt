@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

private val DuskButtonBorderColor = Color.White.copy(alpha = 0.18f)
private val DuskButtonFocusColor = Color.White

enum class DuskTvButtonStyle {
    Primary,
    Secondary,
}

@Composable
fun DuskTvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: DuskTvButtonStyle = DuskTvButtonStyle.Primary,
    contentDescription: String? = null,
) {
    val containerColor = when (style) {
        DuskTvButtonStyle.Primary -> Color.White.copy(alpha = 0.16f)
        DuskTvButtonStyle.Secondary -> Color.White.copy(alpha = 0.08f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = Color.White,
            focusedContainerColor = DuskButtonFocusColor,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, DuskButtonBorderColor),
                shape = MaterialTheme.shapes.large,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, DuskButtonFocusColor),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevation = 16.dp, elevationColor = DuskButtonFocusColor.copy(alpha = 0.16f)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
