@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.transfer

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.presentation.utils.requestFocusSafely
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.common.DuskTvButtonStyle
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val childPadding = rememberChildPadding()
    val primaryActionRequester = remember { FocusRequester() }
    // 状态就绪后聚焦主操作按钮（官方模式：状态驱动首焦，取代版本号协议）。
    // Idle/Ready/Unavailable 三态都有主按钮；Loading 无可聚焦目标，等待就绪后 effect 重触发。
    // 点击「开启/重启/重新检测」后状态变化也会重进此 effect，主按钮保持焦点。
    LaunchedEffect(state) {
        when (state) {
            TransferScreenUiState.Idle,
            is TransferScreenUiState.Ready,
            is TransferScreenUiState.Unavailable -> {
                delay(50)
                primaryActionRequester.requestFocusSafely()
            }

            TransferScreenUiState.Loading -> Unit
        }
    }

    DuskScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = 34.dp,
                    bottom = 108.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PageHeader(
                eyebrow = "局域网管理",
                title = "书库管理",
                subtitle = "同一 Wi-Fi 下在浏览器上传, 查看和管理电视书库.",
            )
            when (val current = state) {
                TransferScreenUiState.Idle -> TransferIdlePanel(
                    buttonRequester = primaryActionRequester,
                    onStart = {
                        viewModel.startTransfer()
                    },
                )

                TransferScreenUiState.Loading -> TransferLoadingPanel()

                is TransferScreenUiState.Ready -> TransferReadyPanel(
                    url = current.url,
                    helperMessage = current.helperMessage,
                    qrCode = current.qrCode,
                    lastUploadText = formatLastUpload(current.lastUploadMessage, current.lastUploadAtMillis),
                    buttonRequester = primaryActionRequester,
                    onRestart = { viewModel.refresh() },
                )

                is TransferScreenUiState.Unavailable -> TransferUnavailablePanel(
                    message = current.message,
                    lastUploadText = formatLastUpload(current.lastUploadMessage, current.lastUploadAtMillis),
                    buttonRequester = primaryActionRequester,
                    onRefresh = { viewModel.refresh() },
                )
            }
        }
    }
}

@Composable
private fun TransferIdlePanel(
    buttonRequester: FocusRequester,
    onStart: () -> Unit,
) {
    PrimaryPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            TransferGlyph()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "开启书库管理服务",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "启动后可用电脑或手机浏览器上传, 查看和删除本地书籍.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.66f),
                )
            }
            Row(modifier = Modifier.focusGroup()) {
                DuskTvButton(
                    text = "开启管理",
                    icon = Icons.Default.UploadFile,
                    modifier = Modifier.focusRequester(buttonRequester),
                    onClick = onStart,
                )
            }
        }
    }
}

@Composable
private fun TransferLoadingPanel() {
    PrimaryPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "正在启动管理服务",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = "请稍等, 系统正在获取电视当前局域网地址.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun TransferReadyPanel(
    url: String,
    helperMessage: String,
    qrCode: Bitmap,
    lastUploadText: String?,
    buttonRequester: FocusRequester,
    onRestart: () -> Unit,
) {
    PrimaryPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusLine(
                    title = "服务已就绪",
                    subtitle = helperMessage,
                )
                AddressBlock(url = url, lastUploadText = lastUploadText)
                Row(modifier = Modifier.focusGroup()) {
                    DuskTvButton(
                        text = "重启服务",
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.focusRequester(buttonRequester),
                        style = DuskTvButtonStyle.Secondary,
                        onClick = onRestart,
                    )
                }
            }
            QrCodePanel(qrCode = qrCode)
        }
    }
}

@Composable
private fun TransferUnavailablePanel(
    message: String,
    lastUploadText: String?,
    buttonRequester: FocusRequester,
    onRefresh: () -> Unit,
) {
    PrimaryPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            TransferGlyph()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "无法开启管理服务",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.66f),
                )
                lastUploadText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.52f),
                    )
                }
            }
            Row(modifier = Modifier.focusGroup()) {
                DuskTvButton(
                    text = "重新检测",
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.focusRequester(buttonRequester),
                    onClick = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun AddressBlock(url: String, lastUploadText: String?) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.18f)),
        shape = MaterialTheme.shapes.large,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape = MaterialTheme.shapes.large),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "浏览器地址",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.56f),
            )
            Text(
                text = url,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            lastUploadText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QrCodePanel(qrCode: Bitmap) {
    Surface(
        modifier = Modifier.size(216.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White),
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = qrCode.asImageBitmap(),
                contentDescription = "书库管理二维码",
                modifier = Modifier
                    .size(178.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
    }
}

@Composable
private fun StatusLine(
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.12f)),
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.64f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransferGlyph() {
    Surface(
        modifier = Modifier.size(72.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun PrimaryPanel(content: @Composable () -> Unit) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(modifier = Modifier.padding(horizontal = 30.dp, vertical = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.50f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.92f),
        )
        Text(
            text = subtitle,
            modifier = Modifier.widthIn(max = 760.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.60f),
        )
    }
}

@Composable
private fun DuskScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070D15)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF17263A).copy(alpha = 0.52f), Color.Transparent),
                        radius = 980f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1420), Color(0xFF08111B), Color(0xFF070D15)),
                    ),
                ),
        )
        content()
    }
}

private fun formatLastUpload(message: String?, atMillis: Long?): String? {
    if (message.isNullOrBlank() || atMillis == null) return null
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(atMillis))
    return "$time · $message"
}
