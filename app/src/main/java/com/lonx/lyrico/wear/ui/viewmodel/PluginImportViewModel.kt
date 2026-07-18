package com.lonx.lyrico.wear.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 插件导入结果
 */
data class PluginImportResult(
    val success: Boolean,
    val error: String? = null
)

/**
 * 插件导入 UI 状态
 */
data class PluginImportUiState(
    val selectedFilePath: String? = null,
    val isImporting: Boolean = false,
    val result: PluginImportResult? = null
)

/**
 * 插件导入 ViewModel
 *
 * 处理从文件路径导入插件 ZIP 的逻辑。
 * 注意：实际的 ZIP 解压和插件安装逻辑需要复用 Lyrico 原项目的 SourcePluginInstaller，
 * 这里先提供框架，后续集成时接入。
 */
class PluginImportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PluginImportUiState())
    val uiState: StateFlow<PluginImportUiState> = _uiState.asStateFlow()

    /** 用户选择了文件 */
    fun selectFile(path: String) {
        _uiState.value = _uiState.value.copy(selectedFilePath = path)
    }

    /** 重置选择 */
    fun resetSelection() {
        _uiState.value = PluginImportUiState()
    }

    /** 清除结果 */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    /**
     * 执行插件导入
     * TODO: 接入 Lyrico 的 SourcePluginInstaller.prepareImport + installPrepared
     */
    fun importPlugin(context: Context) {
        val filePath = _uiState.value.selectedFilePath ?: return
        _uiState.value = _uiState.value.copy(isImporting = true)

        viewModelScope.launch {
            try {
                // TODO: 这里需要复用 Lyrico 原项目的插件安装器
                // val installer = SourcePluginInstaller(...)
                // val input = FileInputStream(filePath)
                // val session = installer.prepareImport(input, installRoot)
                // val result = installer.installPrepared(session, enabled = true, ...)
                // 当前占位：验证文件存在且是 ZIP
                val result = withContext(Dispatchers.IO) {
                    val file = java.io.File(filePath)
                    if (!file.exists()) {
                        PluginImportResult(false, "文件不存在")
                    } else if (!file.name.lowercase().endsWith(".zip")) {
                        PluginImportResult(false, "请选择 .zip 插件包")
                    } else {
                        // 占位：实际安装逻辑待接入
                        PluginImportResult(false, "插件安装器待接入（需要集成 Lyrico 核心模块）")
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    result = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    result = PluginImportResult(false, e.message)
                )
            }
        }
    }
}
