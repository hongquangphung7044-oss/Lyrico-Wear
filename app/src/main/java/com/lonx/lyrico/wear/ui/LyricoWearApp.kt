package com.lonx.lyrico.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.lonx.lyrico.wear.ui.screen.MainMenuScreen
import com.lonx.lyrico.wear.ui.screen.PluginImportScreen
import com.lonx.lyrico.wear.ui.screen.FolderSelectScreen
import com.lonx.lyrico.wear.ui.screen.SettingsScreen

/**
 * Lyrico WearOS 主应用入口
 *
 * 使用 Wear Compose Navigation 的 SwipeDismissableNavHost：
 *   - 支持从左边缘右滑返回上一页（WearOS 标准手势）
 *   - 自动处理页面切换动画
 */
@Composable
fun LyricoWearApp() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainMenuScreen(
                onImportPlugin = { navController.navigate("file_picker/plugin") },
                onSelectFolder = { navController.navigate("file_picker/folder") },
                onSettings = { navController.navigate("settings") }
            )
        }

        composable("file_picker/plugin") {
            PluginImportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("file_picker/folder") {
            FolderSelectScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
