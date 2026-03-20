package com.wzl.duskreader.tv.ui.screen

import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel
import com.wzl.duskreader.tv.util.DebugLogger
import java.io.File

/**
 * 调试界面：查看系统日志和权限详情。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: ShelfViewModel) {
    val logs by DebugLogger.logs.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("调试诊断管理", style = MaterialTheme.typography.displaySmall)
            Row {
                Button(onClick = { viewModel.scanStorage() }) {
                    Text("强制扫描")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { DebugLogger.clear() }) {
                    Text("清空日志")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 系统信息卡片
        Card(
            onClick = {}, // TV 端 Card 设置空点击以获得焦点
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "系统信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("API Level: ${Build.VERSION.SDK_INT}")
                Text(
                    "MANAGE_EXTERNAL_STORAGE: ${
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
                            Environment.isExternalStorageManager() else "N/A"
                    }"
                )
                Text("Package: ${context.packageName}")
                
                val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val bookDir = File(docDir, "暮阅")
                Text("扫描路径: ${bookDir.absolutePath}")
                Text("路径是否存在: ${bookDir.exists()}")
                
                if (bookDir.exists()) {
                    val count = bookDir.listFiles()?.size ?: -1
                    Text("目录文件数: $count")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("运行日志 (最近 50 条)", style = MaterialTheme.typography.titleMedium)
        
        Card(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.colors(containerColor = Color.Black.copy(alpha = 0.3f))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                lazyItems(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (log.contains("[ERROR]")) Color.Red else Color.White,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
