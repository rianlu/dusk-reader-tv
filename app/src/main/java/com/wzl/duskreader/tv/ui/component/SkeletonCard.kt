package com.wzl.duskreader.tv.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Apple TV 风格骨架屏卡片
 * 使用 shimmer 动画效果提供优雅的加载状态
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 卡片骨架
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslate - 200f, shimmerTranslate - 200f),
                        end = Offset(shimmerTranslate, shimmerTranslate)
                    )
                )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 标题骨架
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslate - 200f, shimmerTranslate - 200f),
                        end = Offset(shimmerTranslate, shimmerTranslate)
                    )
                )
        )
    }
}

/**
 * Hero 卡片骨架屏
 */
@Composable
fun SkeletonHeroCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero-shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroShimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 58.dp, end = 58.dp, top = 88.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.66f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 标题骨架
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .background(
                            brush = Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset(shimmerTranslate - 300f, shimmerTranslate - 300f),
                                end = Offset(shimmerTranslate, shimmerTranslate)
                            )
                        )
                )

                // 元信息骨架
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .background(
                            brush = Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset(shimmerTranslate - 300f, shimmerTranslate - 300f),
                                end = Offset(shimmerTranslate, shimmerTranslate)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 按钮骨架
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .background(
                            brush = Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset(shimmerTranslate - 300f, shimmerTranslate - 300f),
                                end = Offset(shimmerTranslate, shimmerTranslate)
                            )
                        )
                )
            }
        }
    }
}
