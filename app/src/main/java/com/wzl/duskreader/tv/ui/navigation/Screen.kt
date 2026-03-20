package com.wzl.duskreader.tv.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 屏幕定义：定义应用内所有主要的功能页面及其对应的图标。
 */
enum class Screen(val title: String, val icon: ImageVector) {
    Shelf("书架", Icons.Default.AutoStories),
    Recent("最近", Icons.Default.History),
    Explorer("文件管理", Icons.Default.FolderOpen),
    Transfer("无线传书", Icons.Default.CloudUpload),
    Settings("设置", Icons.Default.Settings),
    Debug("调试", Icons.Default.BugReport)
}
