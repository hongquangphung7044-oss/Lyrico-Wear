package com.lonx.lyrico.wear.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.wear.shizuku.FileEntry
import com.lonx.lyrico.wear.shizuku.ShizukuFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 文件管理器 UI 状态
 */
data class FilePickerUiState(
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 文件管理器 ViewModel
 *
 * 管理文件浏览的状态：
 *   - 当前路径
 *   - 目录列表
 *   - 导航历史（用于返回上级）
 */
class FilePickerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FilePickerUiState())
    val uiState: StateFlow<FilePickerUiState> = _uiState.asStateFlow()

    /** 导航历史栈，用于逐级返回 */
    private val pathStack = ArrayDeque<String>()

    /**
     * 导航到指定目录
     * @param path 目标目录绝对路径
     */
    fun navigateTo(path: String) {
        val current = _uiState.value.currentPath
        if (current.isNotEmpty() && current != path) {
            pathStack.addLast(current)
        }
        _uiState.value = _uiState.value.copy(currentPath = path, isLoading = true)
        loadDirectory(path)
    }

    /**
     * 返回上级目录
     */
    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current.isEmpty()) return

        // 优先用历史栈
        if (pathStack.isNotEmpty()) {
            val previous = pathStack.removeLast()
            _uiState.value = _uiState.value.copy(currentPath = previous, isLoading = true)
            loadDirectory(previous)
        } else {
            // 没有历史，取父目录
            val parent = File(current).parent
            if (parent != null) {
                _uiState.value = _uiState.value.copy(currentPath = parent, isLoading = true)
                loadDirectory(parent)
            }
        }
    }

    /**
     * 加载目录内容（在 IO 线程执行）
     */
    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    ShizukuFileManager.listFiles(path)
                }
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    entries = emptyList(),
                    isLoading = false,
                    error = e.message ?: "无法读取目录"
                )
            }
        }
    }
}
