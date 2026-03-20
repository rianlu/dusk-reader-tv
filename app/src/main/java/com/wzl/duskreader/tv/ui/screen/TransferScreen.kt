package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.ui.component.QrCodeComponent

/**
 * 无线传书页面：提供二维码导航和操作指引。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TransferScreen(server: FileTransferServer) {
    val ip = server.getLocalIpAddress() ?: "127.0.0.1"
    val port = 8080
    val url = "http://$ip:$port"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("无线传书", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "确保手机和电视连接在同一个 WiFi 下",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(Modifier.height(32.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            QrCodeComponent(url = url, sizeDp = 240)
            
            Column {
                Text("操作步骤：", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("1. 手机扫描左侧二维码", style = MaterialTheme.typography.bodyLarge)
                Text("2. 在手机浏览器中选择小说文件", style = MaterialTheme.typography.bodyLarge)
                Text("3. 点击“立即上传”", style = MaterialTheme.typography.bodyLarge)
                
                Spacer(Modifier.height(24.dp))
                Text(
                    "或者在浏览器输入：",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                // 增加一个可点击的 Surface，确保页面有焦点落脚点
                Surface(
                    onClick = {},
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        url,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
