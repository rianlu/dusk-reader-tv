package com.wzl.duskreader.tv.presentation.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 打开系统的存储权限设置页。复用自 [com.wzl.duskreader.tv.presentation.common.StoragePermissionHandler]
 * 的 intent 逻辑，但这里直接 startActivity（设置页不需要回调结果）。
 *
 * - Android 11+ (R)：跳「所有文件访问权限」授权页（优先带包名的精确入口，否则退回通用入口）。
 * - Android 10 及以下：跳应用详情页，由用户手动开启存储权限。
 */
fun openStoragePermissionSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val specific = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        if (specific.resolveActivity(context.packageManager) != null) {
            specific
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }
    runCatching { context.startActivity(intent) }
}
