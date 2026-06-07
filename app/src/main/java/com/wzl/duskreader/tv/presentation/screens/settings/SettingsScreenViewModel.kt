package com.wzl.duskreader.tv.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页 v1 仅承载一个真实动作：重新扫描书库。
 * 重扫逻辑镜像 [com.wzl.duskreader.tv.presentation.screens.bookshelf.BookshelfScreenViewModel.rescanLibrary]，
 * 额外在终态后延时复位为 Idle，让设置行副标题自动回到默认文案。
 */
@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _rescanState = MutableStateFlow<RescanUiState>(RescanUiState.Idle)
    val rescanState: StateFlow<RescanUiState> = _rescanState.asStateFlow()

    private var rescanJob: Job? = null

    fun rescan() {
        if (_rescanState.value is RescanUiState.Scanning) return
        // 取消上一轮可能仍在等待的「复位」协程，避免覆盖本次结果。
        rescanJob?.cancel()
        rescanJob = viewModelScope.launch {
            _rescanState.value = RescanUiState.Scanning
            _rescanState.value = runCatching { bookRepository.scanLocalStorage() }.fold(
                onSuccess = { imported -> RescanUiState.Done(imported) },
                onFailure = { error -> RescanUiState.Failed(error.message ?: "未知错误") },
            )
            delay(RESULT_RESET_DELAY_MS)
            _rescanState.value = RescanUiState.Idle
        }
    }

    private companion object {
        const val RESULT_RESET_DELAY_MS = 4_000L
    }
}

sealed interface RescanUiState {
    data object Idle : RescanUiState
    data object Scanning : RescanUiState
    data class Done(val imported: Int) : RescanUiState
    data class Failed(val message: String) : RescanUiState
}
