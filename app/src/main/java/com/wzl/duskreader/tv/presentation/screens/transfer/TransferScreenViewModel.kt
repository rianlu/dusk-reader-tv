package com.wzl.duskreader.tv.presentation.screens.transfer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.wzl.duskreader.tv.network.FileTransferServer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransferScreenViewModel @Inject constructor(
    fileTransferServer: FileTransferServer,
) : ViewModel() {

    val uiState: TransferScreenUiState = run {
        val ip = fileTransferServer.getLocalIpAddress()
        val port = FileTransferServer.DEFAULT_PORT
        if (ip.isNullOrBlank()) {
            TransferScreenUiState.Unavailable
        } else {
            val url = "http://$ip:$port"
            TransferScreenUiState.Ready(
                url = url,
                qrCode = generateQrCode(url, 512),
            )
        }
    }
}

private fun generateQrCode(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bmp
}

sealed interface TransferScreenUiState {
    data object Unavailable : TransferScreenUiState
    data class Ready(val url: String, val qrCode: Bitmap) : TransferScreenUiState
}
