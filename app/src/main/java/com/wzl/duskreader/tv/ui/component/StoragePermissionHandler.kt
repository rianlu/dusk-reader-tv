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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel

/**
 * 权限处理组件：由于需要 context 和 ActivityResultLauncher，定义为 Composable。
 */
@Composable
fun StoragePermissionHandler(shelfViewModel: ShelfViewModel) {
    val context = LocalContext.current
    
    // 针对 Android 11 (API 30) 及以上的处理：检查是否拥有所有文件访问权
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        LaunchedEffect(Unit) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    // 常规存储权限请求 (针对 API 29 及以下，或作为补足)
    val permissionsToRequest = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // 权限被授予后，立即触发扫描
            shelfViewModel.scanStorage()
        }
    }

    LaunchedEffect(Unit) {
        val needsRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            launcher.launch(permissionsToRequest)
        }
    }
}
