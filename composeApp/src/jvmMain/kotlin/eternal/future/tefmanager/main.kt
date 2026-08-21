package eternal.future.tefmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import eternal.future.tefmanager.model.UpdateInfo
import eternal.future.tefmanager.ui.dialogs.ModuleType
import eternal.future.tefmanager.ui.dialogs.UpdateDialog
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.addon.ModuleManager
import eternal.future.tefmanager.utils.isVersionGreater
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.landscape.OnboardingScreen as LandscapeOnboardingScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen
import eternal.future.tefmanager.ui.screen.portrait.OnboardingScreen as PortraitOnboardingScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    FileKit.init("tefmanager")
    AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
    ConfigManager.getInstance().initialize(Platform.getData(null).toString())

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized
    )

    if (ConfigurationState.kernelLogEnabled) {
        val kernelLogDir = Platform.getData("logs") / "tefkernel"
        FileSystem.SYSTEM.createDirectories(kernelLogDir)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "TEFManager",
        state = windowState,
        undecorated = true,
        transparent = false,
        resizable = true,
    ) {
        val showUpdateDialog = remember { mutableStateOf(false) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        val networkService = remember { NetworkService() }

        suspend fun checkForUpdates() {
            try {
                val info = networkService.fetchUpdateInfoWithRetry(
                    "https://raw.githubusercontent.com/eternalfuture-e38299/TEFManager/main/update.json"
                )

                if (info == null) {
                    return
                } else {
                    // 获取当前已安装模块的版本
                    val currentTefManagerVersion = BuildConfig.VERSION_NAME
                    val currentKernelVersion = ConfigurationState.kernelVersion
                    val currentLoaderVersion = ConfigurationState.kernelVersion  // 相同
                    val currentLanguageVersion = ModuleManager.getPackItem("eternal.future.languagepackextension")?.version
                    val currentFontVersion = ModuleManager.getPackItem("eternal.future.fontpackextension")?.version
                    val currentTextureVersion = ModuleManager.getPackItem("eternal.future.texturepackextension")?.version
                    val currentMusicVersion = ModuleManager.getPackItem("eternal.future.audiopackextension")?.version

                    // 逐个判断并构建 UpdateInfo
                    updateInfo = UpdateInfo(
                        tefmanager = info.tefmanager?.takeIf {
                            isVersionGreater(it.newVersion, currentTefManagerVersion)
                        },
                        tefkernel = info.tefkernel?.takeIf {
                            isVersionGreater(it.newVersion, currentKernelVersion)
                        },
                        tefloader = info.tefloader?.takeIf {
                            isVersionGreater(it.newVersion, currentLoaderVersion) && Platform.isDesktop
                        },
                        language = info.language?.takeIf {
                            // 如果 currentLanguageVersion 为 null（未安装），或者版本更新
                            currentLanguageVersion == null || isVersionGreater(it.newVersion, currentLanguageVersion)
                        },
                        font = info.font?.takeIf {
                            currentFontVersion == null || isVersionGreater(it.newVersion, currentFontVersion)
                        },
                        texture = info.texture?.takeIf {
                            currentTextureVersion == null || isVersionGreater(it.newVersion, currentTextureVersion)
                        },
                        music = info.music?.takeIf {
                            currentMusicVersion == null || isVersionGreater(it.newVersion, currentMusicVersion)
                        }
                    )

                    // 检查是否有任何更新
                    val hasUpdate = updateInfo!!.tefmanager != null ||
                            updateInfo!!.tefkernel != null ||
                            updateInfo!!.tefloader != null ||
                            updateInfo!!.language != null ||
                            updateInfo!!.font != null ||
                            updateInfo!!.texture != null ||
                            updateInfo!!.music != null

                    if (hasUpdate) {
                        showUpdateDialog.value = true
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to check for updates", e)
            }
        }

        val isLandscape = windowState.size.width > windowState.size.height

        LaunchedEffect(isLandscape) {
            AppLogger.d("Window orientation changed: ${if (isLandscape) "Landscape" else "Portrait"}")
        }

        LaunchedEffect(Unit) {
            if (ConfigurationState.autoUpdate) {
                checkForUpdates()
            }
        }

        TEFManagerTheme(
            themeMode = ConfigurationState.themeMode,
        ) {
            Column {
                WindowDraggableArea {
                    // 自定义标题栏
                    CustomTitleBar(
                        windowState = windowState,
                        onClose = ::exitApplication
                    )
                }

                if (showUpdateDialog.value && updateInfo != null) {
                    UpdateDialog(
                        updateInfo = updateInfo!!,
                        onDismiss = { showUpdateDialog.value = false },
                        onModuleInstalled = { moduleType, downloadedPath ->
                            // downloadedPath 是临时目录中的文件路径
                            when (moduleType) {
                                ModuleType.TEFLoader -> {
                                    // 安装 TEFLoader 到指定位置
                                    val targetFile = Platform.getData("tefkernel") / "tefloader.zip"
                                    // 确保目标目录存在
                                    if (!FileSystem.SYSTEM.exists(downloadedPath))
                                        return@UpdateDialog

                                    // 从临时目录移动/复制到目标目录
                                    FileSystem.SYSTEM.copy(downloadedPath, targetFile)
                                    FileSystem.SYSTEM.delete(downloadedPath)
                                }
                                ModuleType.TEFKernel -> {
                                    AddonManager.installKernel(downloadedPath)
                                }
                                else -> {
                                    runBlocking {
                                        AddonManager.install(downloadedPath)
                                    }
                                }
                            }
                        }
                    )
                }

                // 主内容区域
                Box(modifier = Modifier.fillMaxSize()) {
                    key(isLandscape) {
                        if (isLandscape) {
                            if (!ConfigurationState.initialized) LandscapeOnboardingScreen {
                                ConfigurationState.initialized = true
                            }
                            else LandscapeMainScreen.Content()
                        } else {
                            if (!ConfigurationState.initialized) PortraitOnboardingScreen {
                                ConfigurationState.initialized = true
                            }
                            else PortraitMainScreen.Content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTitleBar(
    windowState: WindowState,
    onClose: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEFManager",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // 窗口控制按钮（这些按钮区域不可拖动）
                Row {
                    IconButton(
                        onClick = { windowState.isMinimized = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HorizontalRule,
                            contentDescription = "Minimize",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            windowState.placement =
                                if (windowState.placement == WindowPlacement.Maximized) {
                                    WindowPlacement.Floating
                                } else {
                                    WindowPlacement.Maximized
                                }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "Maximize/Restore",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

    }
}