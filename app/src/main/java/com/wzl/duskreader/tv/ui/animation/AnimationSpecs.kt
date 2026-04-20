package com.wzl.duskreader.tv.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * Apple TV 风格动画规范
 * 参考 Netflix、Disney+、Apple TV+ 等流媒体应用的动画曲线和时长
 */
object AnimationSpecs {

    // Apple TV 标准缓动曲线
    val AppleTVEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val AppleTVEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AppleTVEaseIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    // 卡片焦点动画 - 快速响应,流畅过渡
    val CardFocusScale = tween<Float>(
        durationMillis = 280,
        easing = AppleTVEasing
    )

    val CardFocusElevation = tween<Float>(
        durationMillis = 280,
        easing = AppleTVEasing
    )

    val CardFocusGlow = tween<Float>(
        durationMillis = 320,
        easing = AppleTVEaseOut
    )

    // Hero 卡片动画 - 更慢更优雅
    val HeroScale = tween<Float>(
        durationMillis = 400,
        easing = AppleTVEasing
    )

    val HeroGlow = tween<Float>(
        durationMillis = 450,
        easing = AppleTVEaseOut
    )

    // 页面转场动画
    val PageTransitionFade = tween<Float>(
        durationMillis = 350,
        easing = AppleTVEaseOut
    )

    val PageTransitionSlide = tween<Float>(
        durationMillis = 400,
        easing = AppleTVEasing
    )

    // 列表滚动动画
    val ScrollSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // 内容淡入动画
    val ContentFadeIn = tween<Float>(
        durationMillis = 300,
        delayMillis = 100,
        easing = AppleTVEaseOut
    )

    // 阅读器翻页动画
    val ReaderPageTurn = tween<Float>(
        durationMillis = 350,
        easing = AppleTVEasing
    )

    // Overlay 动画
    val OverlayFadeIn = tween<Float>(
        durationMillis = 250,
        easing = AppleTVEaseOut
    )

    val OverlaySlideIn = tween<Float>(
        durationMillis = 300,
        easing = AppleTVEasing
    )
}
