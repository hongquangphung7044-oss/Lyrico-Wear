package com.lonx.lyrico.wear.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FolderSelectUiState(
    val selectedFolderPath: String? = null
)

/**
 * 文件夹选择 ViewModel
 *
 * TODO: 接入 Lyrico 的 FolderManagerViewModel.addFolderByPath(path)
 *       将选择的路径保存到数据库（FolderEntity, addedBySaf=false）
 */
class FolderSelectViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FolderSelectUiState())
    val uiState: StateFlow<FolderSelectUiState> = _uiState.asStateFlow()

    fun selectFolder(path: String) {
        _uiState.value = _uiState.value.copy(selectedFolderPath = path)
    }

    fun clearSelection() {
        _uiState.value = FolderSelectUiState()
    }
}
