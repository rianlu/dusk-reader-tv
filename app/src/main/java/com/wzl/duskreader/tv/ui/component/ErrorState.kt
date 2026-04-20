package com.wzl.duskreader.tv.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

/**
 * 统一的错误状态组件
 * Apple TV 风格的友好错误提示
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.ErrorOutline,
    actionLabel: String? = "重试",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 58.dp)
        ) {
            // 错误图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // 详细信息
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 500.dp)
            )

            // 操作按钮
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * 网络错误状态
 */
@Composable
fun NetworkErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "网络连接失败",
        message = "请检查网络连接后重试",
        actionLabel = "重试",
        onAction = onRetry,
        modifier = modifier
    )
}

/**
 * 文件读取错误状态
 */
@Composable
fun FileErrorState(
    fileName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "无法打开文件",
        message = "文件 \"$fileName\" 可能已被移动或删除",
        actionLabel = "返回",
        onAction = onBack,
        modifier = modifier
    )
}
