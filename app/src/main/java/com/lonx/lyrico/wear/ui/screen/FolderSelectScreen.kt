package com.lonx.lyrico.wear.ui.screen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lonx.lyrico.wear.R
import com.lonx.lyrico.wear.ui.FilePickerMode
import com.lonx.lyrico.wear.ui.FilePickerScreen
import com.lonx.lyrico.wear.ui.viewmodel.FolderSelectViewModel

/**
 * 文件夹选择界面
 *
 * 用内置文件管理器选择音乐扫描目录。
 * 替代系统 ACTION_OPEN_DOCUMENT_TREE —— 手表无 DocumentsUI 时用此方案。
 */
@Composable
fun FolderSelectScreen(
    onBack: () -> Unit,
    viewModel: FolderSelectViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 处理选择结果
    LaunchedEffect(uiState.selectedFolderPath) {
        uiState.selectedFolderPath?.let { path ->
            Toast.makeText(
                context,
                context.getString(R.string.folder_select_success, path),
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearSelection()
            onBack()
        }
    }

    FilePickerScreen(
        mode = FilePickerMode.SELECT_FOLDER,
        onFileSelected = { },
        onFolderSelected = { path ->
            viewModel.selectFolder(path)
        },
        onBack = onBack
    )
}
