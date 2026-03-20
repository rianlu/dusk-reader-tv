package com.wzl.duskreader.tv.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * 简单的二维码生成组件
 */
@Composable
fun QrCodeComponent(url: String, sizeDp: Int = 200) {
    val bitmap = remember(url) {
        generateQrCode(url, 512)
    }

    Box(
        modifier = Modifier
            .background(Color.White)
            .padding(12.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "扫码传书二维码",
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}

private fun generateQrCode(text: String, size: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
