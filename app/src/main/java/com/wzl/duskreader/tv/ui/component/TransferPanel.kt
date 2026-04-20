package com.wzl.duskreader.tv.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

@Composable
fun TransferPanel(
    url: String,
    modifier: Modifier = Modifier,
    addressModifier: Modifier = Modifier
) {
    // 添加微妙的呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "transfer-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.08f),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "快速传书",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "在同一局域网中打开手机浏览器，输入下方地址或直接扫码上传 TXT / EPUB。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.68f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. 手机和电视连接同一 Wi‑Fi", color = Color.White.copy(alpha = 0.78f))
                    Text("2. 打开浏览器访问地址", color = Color.White.copy(alpha = 0.78f))
                    Text("3. 选择文件上传，书籍会自动进入书库", color = Color.White.copy(alpha = 0.78f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {},
                    modifier = addressModifier
                ) {
                    Text(url)
                }
            }
        }

        Surface(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight(0.92f),
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.08f),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    colors = SurfaceDefaults.colors(containerColor = Color.White),
                    modifier = Modifier.size(260.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        QrCodeComponent(url = url, sizeDp = 220)
                    }
                }
            }
        }
    }
}
