package com.lonx.lyrico.wear.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.lonx.lyrico.R
import com.lonx.lyrico.wear.FileEntry
import com.lonx.lyrico.wear.ShizukuFileManager
import com.lonx.lyrico.wear.ui.viewmodel.FilePickerViewModel

/**
 * 文件选择器模式
 */
enum class FilePickerMode {
    /** 选择文件（用于导入插件 ZIP） */
    SELECT_FILE,
    /** 选择文件夹（用于选择音乐扫描目录） */
    SELECT_FOLDER
}

/**
 * 内置文件管理器 —— Wear Material 3 原生实现
 *
 * 这是解决 WearOS 手表无 DocumentsUI 的核心组件：
 *   - 不依赖系统 ACTION_OPEN_DOCUMENT / ACTION_OPEN_DOCUMENT_TREE
 *   - 通过 Shizuku 授权后直接用 File API 浏览目录
 *   - 支持选择文件（导入插件）或选择文件夹（音乐扫描目录）
 *
 * UI 组件全部来自 androidx.wear.compose.material：
 *   - ScalingLazyColumn：圆屏适配的滚动列表（边缘缩放效果）
 *   - Chip：手表专用按钮组件（足够大的触摸区域）
 *   - Scaffold + TimeText + Vignette + PositionIndicator：标准 WearOS 布局
 *
 * @param mode 选择模式：SELECT_FILE（选文件）或 SELECT_FOLDER（选目录）
 * @param onFileSelected 文件选择回调（SELECT_FILE 模式下点击文件触发）
 * @param onFolderSelected 文件夹选择回调（SELECT_FOLDER 模式下点击"选择此目录"触发）
 * @param onBack 返回回调
 * @param title 选择器标题（显示在顶部）
 */
@Composable
fun FilePickerScreen(
    mode: FilePickerMode,
    onFileSelected: (String) -> Unit,
    onFolderSelected: (String) -> Unit,
    onBack: () -> Unit,
    title: String = "",
    viewModel: FilePickerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()

    // 实时订阅 Shizuku 授权状态（授权后立即刷新，不再有延迟）
    val isAuthorized by ShizukuFileManager.authState.collectAsState()
    val binderAlive by ShizukuFileManager.binderAlive.collectAsState()

    // 初始化：如果还没设置路径，从存储根目录开始
    LaunchedEffect(Unit) {
        if (uiState.currentPath.isEmpty()) {
            viewModel.navigateTo(ShizukuFileManager.getStorageRoot())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = {
            if (uiState.entries.isNotEmpty()) {
                PositionIndicator(scalingLazyListState = listState)
            }
        }
    ) {
        if (!binderAlive) {
            // Shizuku 服务未运行
            ShizukuNotRunningScreen(onBack = onBack)
        } else if (!isAuthorized) {
            // 未授权界面
            ShizukuAuthRequired(
                onRequestAuth = { ShizukuFileManager.requestPermission() },
                onBack = onBack
            )
        } else {
            // 文件浏览器
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 标题
                if (title.isNotEmpty()) {
                    item {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.title2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 当前路径显示
                item {
                    PathHeader(path = uiState.currentPath)
                }

                // 返回上级目录
                if (uiState.currentPath != ShizukuFileManager.getStorageRoot() && uiState.currentPath != "/") {
                    item {
                        ParentDirChip(
                            onClick = { viewModel.navigateUp() }
                        )
                    }
                }

                // 文件夹模式下：选择当前目录按钮
                if (mode == FilePickerMode.SELECT_FOLDER) {
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            colors = ChipDefaults.primaryChipColors(),
                            label = { Text(stringResource(R.string.wear_file_picker_select_folder)) },
                            onClick = { onFolderSelected(uiState.currentPath) }
                        )
                    }
                }

                // 空目录提示
                if (uiState.entries.isEmpty() && !uiState.isLoading) {
                    item {
                        Text(
                            text = stringResource(R.string.wear_file_picker_empty),
                            style = MaterialTheme.typography.body1
                        )
                    }
                }

                // 文件/目录列表
                items(uiState.entries) { entry ->
                    FileEntryChip(
                        entry = entry,
                        mode = mode,
                        onClick = {
                            if (entry.isDirectory) {
                                viewModel.navigateTo(entry.path)
                            } else if (mode == FilePickerMode.SELECT_FILE) {
                                onFileSelected(entry.path)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 当前路径显示组件
 */
@Composable
private fun PathHeader(path: String) {
    // 只显示最后两级目录，圆屏装不下完整路径
    val segments = path.split("/").filter { it.isNotEmpty() }
    val displayPath = when {
        segments.isEmpty() -> "/"
        segments.size <= 2 -> "/${segments.joinToString("/")}"
        else -> ".../${segments.takeLast(2).joinToString("/")}"
    }
    Text(
        text = displayPath,
        style = MaterialTheme.typography.caption1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/**
 * 返回上级目录按钮
 */
@Composable
private fun ParentDirChip(onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(0.9f),
        label = { Text(stringResource(R.string.wear_file_picker_parent)) },
        onClick = onClick
    )
}

/**
 * 文件/目录条目
 */
@Composable
private fun FileEntryChip(
    entry: FileEntry,
    mode: FilePickerMode,
    onClick: () -> Unit
) {
    val isSelectable = when {
        entry.isDirectory -> true
        mode == FilePickerMode.SELECT_FILE -> true
        else -> false
    }

    Chip(
        modifier = Modifier.fillMaxWidth(0.9f),
        enabled = isSelectable,
        colors = if (entry.isDirectory) {
            ChipDefaults.secondaryChipColors()
        } else {
            ChipDefaults.chipColors()
        },
        label = {
            Text(
                text = entry.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = if (!entry.isDirectory && entry.size > 0) {
            {
                Text(
                    text = entry.sizeFormatted,
                    style = MaterialTheme.typography.caption2
                )
            }
        } else null,
        onClick = onClick
    )
}

/**
 * Shizuku 未授权时的提示界面
 */
@Composable
private fun ShizukuAuthRequired(
    onRequestAuth: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.wear_shizuku_not_authorized),
                style = MaterialTheme.typography.body1
            )
            Chip(
                colors = ChipDefaults.primaryChipColors(),
                label = { Text(stringResource(R.string.wear_shizuku_request_auth)) },
                onClick = onRequestAuth
            )
            Chip(
                label = { Text(stringResource(R.string.wear_back)) },
                onClick = onBack
            )
        }
    }
}

/**
 * Shizuku 服务未运行时的提示界面
 */
@Composable
private fun ShizukuNotRunningScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.wear_shizuku_not_running),
                style = MaterialTheme.typography.body1
            )
            Text(
                text = stringResource(R.string.wear_shizuku_start_guide),
                style = MaterialTheme.typography.caption1
            )
            Chip(
                label = { Text(stringResource(R.string.wear_back)) },
                onClick = onBack
            )
        }
    }
}
