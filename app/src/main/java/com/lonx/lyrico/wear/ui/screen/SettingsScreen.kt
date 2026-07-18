package com.lonx.lyrico.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.lonx.lyrico.wear.R
import com.lonx.lyrico.wear.shizuku.ShizukuFileManager

/**
 * 设置界面
 *
 * 显示 Shizuku 状态和应用信息
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()

    val shizukuRunning = ShizukuFileManager.isShizukuRunning()
    val shizukuAuthorized = ShizukuFileManager.isAuthorized()

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
            item {
                Text(
                    text = stringResource(R.string.menu_settings),
                    style = MaterialTheme.typography.title1
                )
            }

            // Shizuku 状态
            item {
                Text(
                    text = "Shizuku",
                    style = MaterialTheme.typography.title2
                )
            }

            item {
                Text(
                    text = if (shizukuRunning) "服务: 运行中" else "服务: 未运行",
                    style = MaterialTheme.typography.body1
                )
            }

            item {
                Text(
                    text = if (shizukuAuthorized) "授权: 已授权" else "授权: 未授权",
                    style = MaterialTheme.typography.body1
                )
            }

            if (!shizukuAuthorized) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        label = { Text(stringResource(R.string.shizuku_request_auth)) },
                        onClick = { ShizukuFileManager.requestPermission(0) }
                    )
                }
            }

            // 返回按钮
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    label = { Text("返回") },
                    onClick = onBack
                )
            }
        }
    }
}
