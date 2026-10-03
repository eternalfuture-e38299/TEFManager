package eternal.future.tefmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.model.UpdateInfo
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.ui.dialogs.ModuleType
import eternal.future.tefmanager.ui.dialogs.UpdateDialog
import eternal.future.tefmanager.ui.dialogs.UpdateOutdatedDialog
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.addon.ModuleManager
import eternal.future.tefmanager.utils.isVersionGreater
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.landscape.OnboardingScreen as LandscapeOnboardingScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen
import eternal.future.tefmanager.ui.screen.portrait.OnboardingScreen as PortraitOnboardingScreen

class MainActivity : ComponentActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Activity? = null
        val showNeedRootDialog = mutableStateOf(false)
    }

    val networkService = NetworkService()

    override fun onCreate(savedInstanceState: Bundle?) {
        context = this

        AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
        ConfigManager.getInstance().initialize(Platform.getData(null).toString())

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (ConfigurationState.kernelLogEnabled) {
            val kernelLogDir = Platform.getData("logs") / "tefkernel"
            FileSystem.SYSTEM.createDirectories(kernelLogDir)
        }

        setContent {
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

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
                            showUpdateDialog = true
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("Failed to check for updates", e)
                }
            }

            TEFManagerTheme {
                var showInvalidDialog by remember { mutableStateOf(false) }

                if (BuildConfig.IS_INLINE_GAME) {
                    LaunchedEffect(Unit) {
                        if (!checkEnvironment()) showInvalidDialog = true
                    }
                }

                LaunchedEffect(Unit) {
                    if (ConfigurationState.autoUpdate) {
                        checkForUpdates()
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showInvalidDialog) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(StringsResource.Strings.validation.dialog.title) },
                            text = {
                                Column {
                                    Text(StringsResource.Strings.validation.dialog.text1)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = StringsResource.Strings.validation.dialog.text2,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = StringsResource.Strings.validation.dialog.text3,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        finishAffinity()
                                    }
                                ) {
                                    Text(StringsResource.Strings.validation.dialog.confirm)
                                }
                            }
                        )
                    }

                    if (showNeedRootDialog.value) {
                        AlertDialog(
                            onDismissRequest = {},
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = {
                                Text(
                                    text = StringsResource.Strings.root.dialog.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = StringsResource.Strings.root.dialog.message,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = StringsResource.Strings.root.dialog.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 提示框
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = StringsResource.Strings.root.dialog.warning,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        finishAffinity()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(StringsResource.Strings.root.dialog.confirm)
                                }
                            }
                        )
                    }


                    if (showUpdateDialog && updateInfo != null) {
                        UpdateDialog(
                            updateInfo = updateInfo!!,
                            onDismiss = { showUpdateDialog = false },
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
                    ScreenSwitcher()
                }
            }
        }
    }

    @Composable
    private fun ScreenSwitcher() {
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (!ConfigurationState.initialized) LandscapeOnboardingScreen {
                    ConfigurationState.initialized = true
                }
                else LandscapeMainScreen.Content()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (!ConfigurationState.initialized) PortraitOnboardingScreen {
                    ConfigurationState.initialized = true
                }
                else PortraitMainScreen.Content()
            }
            else -> {
                if (!ConfigurationState.initialized) PortraitOnboardingScreen {
                    ConfigurationState.initialized = true
                }
                else PortraitMainScreen.Content()
            }
        }
    }

    private fun checkEnvironment(): Boolean {
        return if (!isTerrariaInstalled()) {
            isRunningInZhuoyitong() || isHarmonyOS()
        } else {
            true
        }
    }

    @SuppressLint("PrivateApi")
    private fun isHarmonyOS(): Boolean {
        // 检查系统属性
        return try {
            val prop = Class.forName("android.os.SystemProperties")
            val method = prop.getMethod("get", String::class.java)
            val version = method.invoke(null, "hw_sc.build.platform.version") as? String
            version?.contains("HarmonyOS") == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isRunningInZhuoyitong(): Boolean {
        return try {
            val cgroupInfo = java.io.BufferedReader(java.io.FileReader("/proc/self/cgroup"))
                .use { reader -> reader.readText() }

            // 检查是否包含 iSulad 容器的特征标识
            val keywords = listOf("isulad", "lxc", "zhuoyi")
            keywords.any { cgroupInfo.contains(it, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isTerrariaInstalled(): Boolean {
        return try {
            this.packageManager.getPackageInfo("com.and.games505.TerrariaPaid", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}