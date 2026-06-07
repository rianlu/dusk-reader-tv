package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Surface

@Composable
fun BookCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedBorderWidth: Dp = 2.dp,
    focusedGlowElevation: Dp = 12.dp,
    focusedGlowAlpha: Float = 0.18f,
    focusedScale: Float = 1.04f,
    title: @Composable () -> Unit = {},
    image: @Composable BoxScope.() -> Unit,
) {
    StandardCardContainer(
        modifier = modifier,
        title = title,
        imageCard = {
            Surface(
                onClick = onClick,
                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = focusedBorderWidth,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(
                        elevation = focusedGlowElevation,
                        elevationColor = MaterialTheme.colorScheme.onSurface.copy(alpha = focusedGlowAlpha),
                    ),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
                content = image,
            )
        },
    )
}
