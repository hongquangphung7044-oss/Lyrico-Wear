package com.lonx.lyrico.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
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
import com.lonx.lyrico.wear.R

/**
 * 主菜单界面
 *
 * WearOS 圆屏主界面，用 ScalingLazyColumn 展示功能入口：
 *   - 导入插件（跳转文件选择器选 ZIP）
 *   - 选择音乐文件夹（跳转文件选择器选目录）
 *   - 设置
 */
@Composable
fun MainMenuScreen(
    onImportPlugin: () -> Unit,
    onSelectFolder: () -> Unit,
    onSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.title1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 导入插件
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(stringResource(R.string.menu_import_plugin)) },
                    onClick = onImportPlugin
                )
            }

            // 选择音乐文件夹
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(stringResource(R.string.menu_select_folder)) },
                    onClick = onSelectFolder
                )
            }

            // 设置
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    label = { Text(stringResource(R.string.menu_settings)) },
                    onClick = onSettings
                )
            }
        }
    }
}
