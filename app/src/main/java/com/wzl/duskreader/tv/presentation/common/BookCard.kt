@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Surface

@Composable
fun BookCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedBorderWidth: Dp = 2.dp,
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
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                content = image,
            )
        },
    )
}
