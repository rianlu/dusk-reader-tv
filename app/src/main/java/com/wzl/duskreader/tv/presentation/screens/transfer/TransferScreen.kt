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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.common.DuskTvButtonStyle
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val firstButtonRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current


    DuskScreenBackground(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer { firstButtonRequester },
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 96.dp,
                bottom = 108.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                PageHeader(
                    eyebrow = "局域网导入",
                    title = "无线传书",
                    subtitle = "手机或电脑连接同一 Wi-Fi, 扫码上传 TXT / EPUB 到电视书库.",
                )
            }
            item {
                when (val current = state) {
                    TransferScreenUiState.Idle -> TransferIdlePanel(
                        buttonRequester = firstButtonRequester,
                        onStart = viewModel::startTransfer,
                    )

                    TransferScreenUiState.Loading -> TransferLoadingPanel()

                    is TransferScreenUiState.Ready -> TransferReadyPanel(
                        url = current.url,
                        helperMessage = current.helperMessage,
                        qrCode = current.qrCode,
                        lastUploadText = formatLastUpload(current.lastUploadMessage, current.lastUploadAtMillis),
                        buttonRequester = firstButtonRequester,
                        onCopyAddress = { clipboard.setText(AnnotatedString(current.url)) },
                        onRefresh = viewModel::refresh,
                    )

                    is TransferScreenUiState.Unavailable -> TransferUnavailablePanel(
                        message = current.message,
                        lastUploadText = formatLastUpload(current.lastUploadMessage, current.lastUploadAtMillis),
                        buttonRequester = firstButtonRequester,
                        onRefresh = viewModel::refresh,
                    )
                }
            }
            item { TransferGuidePanel() }
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
                    text = "开启无线传书服务",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "启动后展示二维码和浏览器地址, 上传完成会自动同步到书库.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.66f),
                )
            }
            Row(modifier = Modifier.focusGroup()) {
                DuskTvButton(
                    text = "开启传书",
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
                text = "正在启动传书服务",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = "请稍等, 系统正在获取电视当前局域网地址.",
                style = MaterialTheme.typography.bodyLarge,
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
    onCopyAddress: () -> Unit,
    onRefresh: () -> Unit,
) {
    PrimaryPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                StatusLine(
                    title = "服务已就绪",
                    subtitle = helperMessage,
                )
                AddressBlock(url = url, lastUploadText = lastUploadText)
                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DuskTvButton(
                        text = "复制地址",
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.focusRequester(buttonRequester),
                        onClick = onCopyAddress,
                    )
                    DuskTvButton(
                        text = "刷新状态",
                        icon = Icons.Default.Refresh,
                        style = DuskTvButtonStyle.Secondary,
                        onClick = onRefresh,
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
                    text = "无法开启传书服务",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
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
                    text = "刷新状态",
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.focusRequester(buttonRequester),
                    onClick = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun TransferGuidePanel() {
    SecondaryPanel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "操作步骤",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GuideStep(
                    index = "1",
                    title = "连接同一 Wi-Fi",
                    subtitle = "手机, 电脑和电视保持在同一局域网.",
                    modifier = Modifier.weight(1f),
                )
                GuideStep(
                    index = "2",
                    title = "扫码打开页面",
                    subtitle = "使用二维码或浏览器地址进入上传页.",
                    modifier = Modifier.weight(1f),
                )
                GuideStep(
                    index = "3",
                    title = "上传 TXT / EPUB",
                    subtitle = "上传完成后回到书库继续阅读.",
                    modifier = Modifier.weight(1f),
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
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "浏览器地址",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.56f),
            )
            Text(
                text = url,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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
        modifier = Modifier.size(292.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White),
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = qrCode.asImageBitmap(),
                contentDescription = "传书二维码",
                modifier = Modifier
                    .size(242.dp)
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
            modifier = Modifier.size(52.dp),
            colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.12f)),
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.64f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GuideStep(
    index: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.045f)),
        shape = MaterialTheme.shapes.large,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = MaterialTheme.shapes.large),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.16f)),
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.58f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransferGlyph() {
    Surface(
        modifier = Modifier.size(88.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp),
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
        Box(modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp)) {
            content()
        }
    }
}

@Composable
private fun SecondaryPanel(content: @Composable () -> Unit) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.045f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = MaterialTheme.shapes.extraLarge),
    ) {
        Box(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.52f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = subtitle,
            modifier = Modifier.widthIn(max = 760.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.64f),
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
