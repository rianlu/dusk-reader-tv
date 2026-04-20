package com.wzl.duskreader.tv.ui.component

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.*

/**
 * 极简性能版权限处理器
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StoragePermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasPermission = checkPermission()
    }

    // 仅在启动时执行一次检查，不再循环监听，降低开销
    LaunchedEffect(Unit) {
        hasPermission = checkPermission()
        if (!hasPermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasPermission) {
        content()
    } else {
        // 极简等待页，减少渲染压力
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("“暮阅”需要存储权限", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        val targetIntent = if (intent.resolveActivity(context.packageManager) != null) {
                            intent
                        } else {
                            fallbackIntent
                        }
                        manageStorageLauncher.launch(targetIntent)
                    } else {
                        legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }) {
                    Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "前往系统设置授权" else "点击授予权限")
                }
            }
        }
    }
}
