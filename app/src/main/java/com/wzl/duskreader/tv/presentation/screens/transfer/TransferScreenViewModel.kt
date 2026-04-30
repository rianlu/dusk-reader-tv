package com.wzl.duskreader.tv.presentation.screens.transfer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.network.TransferServerSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TransferScreenViewModel @Inject constructor(
    private val fileTransferServer: FileTransferServer,
) : ViewModel() {

    val uiState: StateFlow<TransferScreenUiState> = fileTransferServer.snapshot
        .map(::snapshotToUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = snapshotToUiState(fileTransferServer.refresh()),
        )

    fun refresh() {
        fileTransferServer.refresh()
    }
}

private fun snapshotToUiState(snapshot: TransferServerSnapshot): TransferScreenUiState {
    return if (snapshot.isRunning && snapshot.url != null) {
        TransferScreenUiState.Ready(
            url = snapshot.url,
            helperMessage = snapshot.message,
            qrCode = generateQrCode(snapshot.url, 512),
            lastUploadMessage = snapshot.lastUploadMessage,
            lastUploadAtMillis = snapshot.lastUploadAtMillis,
        )
    } else {
        TransferScreenUiState.Unavailable(
            message = snapshot.message,
            lastUploadMessage = snapshot.lastUploadMessage,
            lastUploadAtMillis = snapshot.lastUploadAtMillis,
        )
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
    data class Unavailable(
        val message: String,
        val lastUploadMessage: String? = null,
        val lastUploadAtMillis: Long? = null,
    ) : TransferScreenUiState

    data class Ready(
        val url: String,
        val helperMessage: String,
        val qrCode: Bitmap,
        val lastUploadMessage: String? = null,
        val lastUploadAtMillis: Long? = null,
    ) : TransferScreenUiState
}
