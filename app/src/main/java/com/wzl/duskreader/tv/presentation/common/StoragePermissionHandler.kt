@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.wzl.duskreader.tv.presentation.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

@Composable
fun StoragePermissionHandler(
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }

    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        hasPermission = checkStoragePermission(context)
    }

    // API < 30：直接弹系统权限弹窗（手机端行为）
    LaunchedEffect(Unit) {
        if (!hasPermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // 从系统设置页返回时自动重检权限
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) onPermissionGranted()
    }

    if (hasPermission) {
        content()
    } else {
        PermissionRequiredScreen(
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val specific = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    val target = if (specific.resolveActivity(context.packageManager) != null) {
                        specific
                    } else fallback
                    manageStorageLauncher.launch(target)
                } else {
                    legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
        )
    }
}

@Composable
private fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        // 复用 app 统一的渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111B28),
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
                            Color.Black.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color(0xFF18304E).copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )

        // 内容居中卡片
        Surface(
            modifier = Modifier
                .width(560.dp)
                .align(Alignment.Center),
            shape = MaterialTheme.shapes.extraLarge,
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.07f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 图标
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = SurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "需要存储权限",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "暮阅需要访问设备文件系统，扫描 Documents/暮阅 目录下的 TXT / EPUB 书籍。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }

                // 引导说明卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = SurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                "授权步骤"
                            } else {
                                "操作说明"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            StepLine("1", "点击下方按钮打开系统设置")
                            StepLine("2", "找到「允许访问所有文件」并开启")
                            StepLine("3", "按返回键回到暮阅，书库会自动扫描")
                        } else {
                            StepLine("1", "点击下方按钮授予存储权限")
                            StepLine("2", "授权成功后书库会自动扫描")
                        }
                    }
                }

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DuskTvButton(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            "前往系统设置授权"
                        } else {
                            "授予权限"
                        },
                        icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Icons.Default.FolderOpen
                        } else {
                            Icons.Default.Security
                        },
                        onClick = onRequestPermission,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepLine(step: String, text: String) {
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

private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
