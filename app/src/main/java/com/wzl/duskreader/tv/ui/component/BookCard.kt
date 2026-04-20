package com.wzl.duskreader.tv.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.ui.animation.AnimationSpecs
import com.wzl.duskreader.tv.ui.theme.AccentBlue
import com.wzl.duskreader.tv.ui.theme.BackgroundSecondary
import com.wzl.duskreader.tv.ui.theme.FocusGlow
import com.wzl.duskreader.tv.ui.theme.FocusIndicator
import com.wzl.duskreader.tv.ui.theme.TextPrimary
import com.wzl.duskreader.tv.ui.theme.TextSecondary
import com.wzl.duskreader.tv.ui.theme.TextTertiary
import com.wzl.duskreader.tv.ui.theme.rememberDimensions

/**
 * 书籍卡片（用于主页 Rail）。
 *
 * [modifier] 作用到可聚焦的 Card 上，调用方可用 focusProperties 注入方向约束。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BookCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    isNew: Boolean = false,
    onFocused: () -> Unit = {}
) {
    val dimensions = rememberDimensions()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) onFocused()
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = AnimationSpecs.CardFocusScale,
        label = "cardScale"
    )

    val animatedElevation by animateFloatAsState(
        targetValue = if (isFocused) dimensions.focusShadowElevation.value else 0f,
        animationSpec = AnimationSpecs.CardFocusElevation,
        label = "cardElevation"
    )

    Column(
        modifier = Modifier.width(dimensions.bookCardWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier
                .aspectRatio(dimensions.bookCardAspectRatio)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    shadowElevation = animatedElevation
                    ambientShadowColor = Color(0x88FFFFFF)
                    spotShadowColor = Color(0xAAFFFFFF)
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(dimensions.cardCornerRadius)
                    clip = false
                },
            shape = CardDefaults.shape(shape = androidx.compose.foundation.shape.RoundedCornerShape(dimensions.cardCornerRadius)),
            scale = CardDefaults.scale(focusedScale = 1.0f),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(dimensions.focusBorderWidth, FocusIndicator))
            ),
            colors = CardDefaults.colors(
                containerColor = BackgroundSecondary,
                focusedContainerColor = BackgroundSecondary
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A1A1A), BackgroundSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(1),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary.copy(alpha = 0.1f)
                    )
                }

                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(TextTertiary.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(AccentBlue)
                        )
                    }
                }

                if (isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(dimensions.spacingS * 0.75f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(dimensions.spacingXS * 0.75f))
                            .background(AccentBlue)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "新",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FocusGlow)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensions.spacingS))

        Text(
            text = title,
            style = if (isFocused) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleSmall
            },
            color = if (isFocused) TextPrimary else TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimensions.spacingXS)
        )
    }
}
