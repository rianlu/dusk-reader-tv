package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.ui.component.QrCodeComponent
import com.wzl.duskreader.tv.ui.theme.rememberDimensions

/**
 * 传书页面：左侧说明 + URL，右侧二维码。
 *
 * URL 本身是一个焦点占位的 Surface（左右 Cancel、上 TopBar），不做复制 —— 手机用户看或扫码即可。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TransferScreen(
    server: FileTransferServer,
    entryRequester: FocusRequester,
    tabUpRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val dimensions = rememberDimensions()
    val ip = server.getLocalIpAddress() ?: "127.0.0.1"
    val url = "http://$ip:8080"

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions.contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧说明 + URL
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
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "在同一局域网中打开手机浏览器，输入下方地址或直接扫描右侧二维码上传 TXT / EPUB。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. 手机和电视连接同一 Wi-Fi", color = Color.White.copy(alpha = 0.78f))
                        Text("2. 打开浏览器访问地址或扫描二维码", color = Color.White.copy(alpha = 0.78f))
                        Text("3. 选择文件上传，书籍会自动进入书库", color = Color.White.copy(alpha = 0.78f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // URL 展示（焦点占位）
                    Surface(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .focusRequester(entryRequester)
                            .focusProperties {
                                up = tabUpRequester
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.10f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = url,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // 右侧 QR 码（紧凑，去掉半透明外框）
            Surface(
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(containerColor = Color.White),
                modifier = Modifier.size(300.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeComponent(url = url, sizeDp = 260)
                }
            }
        }
    }
}
