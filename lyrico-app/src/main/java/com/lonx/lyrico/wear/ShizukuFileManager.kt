package com.lonx.lyrico.wear

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Shizuku 文件管理器
 *
 * 通过 Shizuku 获取 shell 用户权限，访问 /sdcard 下的任意文件。
 * 这解决 WearOS 手表没有 DocumentsUI（系统文件选择器）的问题：
 *   - 不依赖 ACTION_OPEN_DOCUMENT / ACTION_OPEN_DOCUMENT_TREE
 *   - 不依赖 MANAGE_EXTERNAL_STORAGE（手表上难授予）
 *   - 直接用 java.io.File 通过 Shizuku 的 IPC 访问
 *
 * 工作原理：
 *   Shizuku 以 shell 用户（UID 2000）运行，拥有比普通 App 更高的权限。
 *   App 通过 ShizukuBinderRequester 获取 IShizukuService 接口，
 *   调用其 newProcess / checkPermission 等方法执行 shell 命令。
 *
 * 但本类不执行 shell 命令，而是直接用 File API —— 因为 Shizuku 授权后，
 * App 进程本身就能通过 File 访问 shell 用户可读的文件。
 *
 * 使用前必须：
 *   1. Shizuku 已安装并启动
 *   2. 用户已授权（[requestPermission]）
 *
 * 授权状态实时更新：
 *   通过 [Shizuku.OnBinderReceivedListener] 和 [Shizuku.OnRequestPermissionResultListener]
 *   监听 Shizuku 服务状态和授权结果，状态变化时立即更新 [authState]。
 *   这解决了"授权后界面状态更新慢"的问题。
 */
object ShizukuFileManager {

    private const val PERMISSION_REQUEST_CODE = 0x1001

    /** 授权状态：true=已授权，false=未授权 */
    private val _authState = MutableStateFlow(false)

    /** 授权状态实时流，UI 订阅此流以实时刷新授权按钮状态 */
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    /** Shizuku 服务是否在运行 */
    private val _binderAlive = MutableStateFlow(false)
    val binderAlive: StateFlow<Boolean> = _binderAlive.asStateFlow()

    // 监听 Shizuku binder 状态变化（服务启动/停止）
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _binderAlive.value = true
        refreshAuthState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _binderAlive.value = false
        _authState.value = false
    }

    // 监听授权结果（用户点击授权/拒绝后立即回调）
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            _authState.value = grantResult == PackageManager.PERMISSION_GRANTED
        }

    /**
     * 初始化监听器，应在 Activity.onCreate 中调用。
     * 注册 Shizuku binder 和权限监听器，确保授权状态实时更新。
     */
    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            // Shizuku 未安装时会抛异常，忽略
        }
        refreshAuthState()
    }

    /**
     * 销毁监听器，应在 Activity.onDestroy 中调用。
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            // 忽略
        }
    }

    /** 刷新授权状态 */
    private fun refreshAuthState() {
        _authState.value = isAuthorized()
        _binderAlive.value = isShizukuRunning()
    }

    /** Shizuku 是否已安装 */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Shizuku 服务是否在运行 */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否已获得 Shizuku 授权。
     * @return true 表示已授权，可以执行操作
     */
    fun isAuthorized(): Boolean {
        if (!isShizukuRunning()) return false
        return if (Shizuku.isPreV11()) {
            true
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求 Shizuku 授权。
     * 调用后会弹出系统授权对话框，结果通过 [permissionResultListener] 回调。
     */
    fun requestPermission() {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                // 用户之前拒绝过，需要引导到 Shizuku 应用手动授权
                _authState.value = false
            } else {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (e: Exception) {
            // Shizuku 未运行
            _authState.value = false
        }
    }

    /**
     * 列出目录下的文件和子目录。
     * @param path 目录绝对路径
     * @return 目录下的条目列表，如果无权限或不存在返回空列表
     */
    fun listFiles(path: String): List<FileEntry> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        return files
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    readable = file.canRead(),
                    extension = if (file.isFile) file.extension.lowercase() else ""
                )
            }
    }

    /**
     * 打开文件输入流（用于读取插件 ZIP 等文件）。
     * @param path 文件绝对路径
     */
    fun openInputStream(path: String): InputStream {
        return FileInputStream(path)
    }

    /**
     * 获取存储根目录（通常是 /sdcard 或 /storage/emulated/0）。
     */
    fun getStorageRoot(): String {
        return android.os.Environment.getExternalStorageDirectory().absolutePath
    }

    /** 判断路径是否存在且可读 */
    fun existsAndReadable(path: String): Boolean {
        val f = File(path)
        return f.exists() && f.canRead()
    }
}

/** 文件条目数据类 */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val readable: Boolean,
    val extension: String
) {
    /** 格式化文件大小 */
    val sizeFormatted: String
        get() = when {
            size == 0L -> ""
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
}
