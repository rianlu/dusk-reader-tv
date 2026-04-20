package com.wzl.duskreader.tv.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    Home("主页", Icons.Default.Home),
    Shelf("书架", Icons.Default.LibraryBooks),
    Transfer("传书", Icons.Default.CloudUpload),
    Settings("设置", Icons.Default.Settings),
    Debug("调试模式", Icons.Default.BugReport)
}
