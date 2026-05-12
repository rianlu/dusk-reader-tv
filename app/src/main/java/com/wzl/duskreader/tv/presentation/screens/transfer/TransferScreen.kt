@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.wzl.duskreader.tv.presentation.screens.transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.util.Date

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val childPadding = rememberChildPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF142033),
                            Color(0xFF0A1018),
                            Color(0xFF05070B),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f),
                        ),
                    ),
                ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 40.dp,
                bottom = 84.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                TransferStage()
            }

            item {
                when (val current = state) {
                    is TransferScreenUiState.Idle -> TransferIdleLayout(
                        onStart = viewModel::startTransfer,
                    )

                    is TransferScreenUiState.Loading -> TransferLoadingLayout()

                    is TransferScreenUiState.Ready -> ReadyTransferLayout(
                        url = current.url,
                        helperMessage = current.helperMessage,
                        qrCode = current.qrCode,
                        lastUploadText = formatLastUpload(
                            current.lastUploadMessage,
                            current.lastUploadAtMillis,
                        ),
                        onCopyAddress = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("传书地址", current.url))
                            Toast.makeText(context, "传书地址已复制", Toast.LENGTH_SHORT).show()
                        },
                        onRefresh = viewModel::refresh,
                    )

                    is TransferScreenUiState.Unavailable -> UnavailableTransferLayout(
                        message = current.message,
                        lastUploadText = formatLastUpload(
                            current.lastUploadMessage,
                            current.lastUploadAtMillis,
                        ),
                        onRefresh = viewModel::refresh,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferIdleLayout(onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.07f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "传书服务未开启",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "需要从手机或电脑上传书籍时再手动开启, 页面切换不会启动网络服务。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Button(
                onClick = onStart,
                shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
            ) {
                Text("开启无线传书")
            }
        }
    }
}

@Composable
private fun TransferLoadingLayout() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.07f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "正在启动传书服务",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = "先切换到页面, 服务会在后台完成初始化, 不会阻塞顶部导航切换。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun TransferStage() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "设备连接",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.62f),
        )
        Text(
            text = "无线传书",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = "手机或电脑连接同一局域网后，即可向电视书库投送 TXT / EPUB。",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun ReadyTransferLayout(
    url: String,
    helperMessage: String,
    qrCode: Bitmap,
    lastUploadText: String?,
    onCopyAddress: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Top,
    ) {
        QrHeroCard(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 520.dp),
            url = url,
            qrCode = qrCode,
            onCopyAddress = onCopyAddress,
        )

        Column(
            modifier = Modifier
                .weight(0.72f)
                .widthIn(min = 360.dp, max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TransferStatusCard(
                title = "服务状态",
                body = helperMessage,
                emphasis = true,
            )
            TransferStatusCard(
                title = "书库目录",
                body = "Documents/暮阅",
            )
            lastUploadText?.let {
                TransferStatusCard(
                    title = "最近上传",
                    body = it,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.07f),
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "连接步骤",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    StepLine("1", "手机 / 电脑连接同一 Wi‑Fi")
                    StepLine("2", "扫码或输入地址打开传书页")
                    StepLine("3", "选择 TXT / EPUB 上传，书库自动刷新")
                }
            }
            Row(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onCopyAddress,
                    shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                ) {
                    Text("复制地址")
                }
                Button(
                    onClick = onRefresh,
                    shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("刷新状态")
                }
            }
        }
    }
}

@Composable
private fun QrHeroCard(
    modifier: Modifier = Modifier,
    url: String,
    qrCode: Bitmap,
    onCopyAddress: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "扫描二维码",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Image(
                    bitmap = qrCode.asImageBitmap(),
                    contentDescription = "传书二维码",
                    modifier = Modifier
                        .size(256.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(14.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "浏览器地址",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = "电视与手机在同一局域网时可直接访问",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Button(
                        onClick = onCopyAddress,
                        shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                    ) {
                        Text("复制到剪贴板")
                    }
                }
            }
        }
    }
}

@Composable
private fun UnavailableTransferLayout(
    message: String,
    lastUploadText: String?,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.07f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "传书服务暂不可用",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = "通常是网络尚未就绪或设备 IP 发生变化，刷新后会重新探测可用地址。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Button(
                    onClick = onRefresh,
                    shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                ) {
                    Text("重新探测")
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(0.72f)
                .widthIn(min = 360.dp, max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TransferStatusCard(
                title = "当前状态",
                body = message,
                emphasis = true,
            )
            lastUploadText?.let {
                TransferStatusCard(
                    title = "最近上传",
                    body = it,
                )
            }
            TransferStatusCard(
                title = "建议检查",
                body = "确认电视已联网，并与上传设备处于同一局域网。",
            )
        }
    }
}

@Composable
private fun TransferStatusCard(
    title: String,
    body: String,
    emphasis: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = if (emphasis) 0.1f else 0.06f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.56f),
            )
            Text(
                text = body,
                style = if (emphasis) {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StepLine(
    step: String,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.64f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

private fun formatLastUpload(message: String?, atMillis: Long?): String? {
    if (message.isNullOrBlank()) return null
    if (atMillis == null) return message
    val time = DateFormat.format("MM-dd HH:mm", Date(atMillis)).toString()
    return "$message · $time"
}
