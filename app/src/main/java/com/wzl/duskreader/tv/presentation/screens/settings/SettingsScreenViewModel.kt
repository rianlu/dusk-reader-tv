package com.wzl.duskreader.tv.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _rescanSummary = MutableStateFlow("尚未执行重新扫描")
    val rescanSummary = _rescanSummary.asStateFlow()

    fun rescanLibrary() {
        viewModelScope.launch {
            runCatching {
                val imported = bookRepository.scanLocalStorage()
                val total = bookRepository.getAllBooks().first().size
                _rescanSummary.value = if (imported > 0) {
                    "扫描完成，新增 $imported 本，共 $total 本"
                } else {
                    "扫描完成，共 $total 本（无新增）"
                }
            }.onFailure { error ->
                _rescanSummary.value = "扫描失败：${error.message ?: "未知错误"}"
            }
        }
    }
}
