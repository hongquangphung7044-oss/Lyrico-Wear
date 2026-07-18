package com.lonx.lyrico.wear.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.lonx.lyrico.wear.R
import com.lonx.lyrico.wear.ui.FilePickerMode
import com.lonx.lyrico.wear.ui.FilePickerScreen
import com.lonx.lyrico.wear.ui.viewmodel.PluginImportViewModel

/**
 * 插件导入界面
 *
 * 先用内置文件管理器选择 ZIP 文件，然后执行导入。
 * 这是替代系统 ACTION_OPEN_DOCUMENT 的方案：
 *   手表无 DocumentsUI → 用 Shizuku + 自建文件浏览器替代
 */
@Composable
fun PluginImportScreen(
    onBack: () -> Unit,
    viewModel: PluginImportViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showFilePicker by remember { mutableStateOf(true) }

    // 处理导入结果
    LaunchedEffect(uiState.result) {
        uiState.result?.let { result ->
            val msg = if (result.success) {
                context.getString(R.string.plugin_import_success)
            } else {
                context.getString(R.string.plugin_import_failed, result.error)
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearResult()
            if (result.success) onBack()
        }
    }

    if (showFilePicker && uiState.selectedFilePath == null) {
        FilePickerScreen(
            mode = FilePickerMode.SELECT_FILE,
            onFileSelected = { path ->
                viewModel.selectFile(path)
                showFilePicker = false
            },
            onFolderSelected = { },
            onBack = onBack
        )
    } else {
        // 确认导入界面
        val selectedPath = uiState.selectedFilePath
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "已选择文件:",
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = selectedPath?.substringAfterLast("/") ?: "",
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (uiState.isImporting) {
                    Text(
                        text = "正在导入...",
                        style = MaterialTheme.typography.body2
                    )
                } else {
                    Chip(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ChipDefaults.primaryChipColors(),
                        label = { Text("确认导入") },
                        onClick = { viewModel.importPlugin(context) }
                    )
                    Chip(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        label = { Text("重新选择") },
                        onClick = {
                            showFilePicker = true
                            viewModel.resetSelection()
                        }
                    )
                }
            }
        }
    }
}
