package eternal.future.tefmanager.ui.screen.shared.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.BuildConfig
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.UpdateInfo
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.SettingSectionTitle
import eternal.future.tefmanager.ui.component.SettingsGroup
import eternal.future.tefmanager.ui.dialogs.ModuleType
import eternal.future.tefmanager.ui.dialogs.UpdateDialog
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.addon.ModuleManager
import eternal.future.tefmanager.utils.openUrl
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.jetbrains.compose.resources.painterResource
import tefmanager.composeapp.generated.resources.Res
import tefmanager.composeapp.generated.resources.tefmanager_logo

/*******************************************************************************
 * TEFManager - AboutSettings
 * Copyright (C) 2026 eternalfuture-e38299
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Author: eternalfuture-e38299
 * GitHub: https://github.com/eternalfuture-e38299
 * Created: 2026/8/6
 *******************************************************************************/

@Composable
fun AboutSettings() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showUpdateDialog = remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val networkService = remember { NetworkService() }


    val filePickerLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Single
    ) { file ->
        if (file == null) return@rememberFilePickerLauncher

        val tmp = Path((Platform.getDirectory("tmp") / file.name).toString())
        tmp.parent?.let { SystemFileSystem.createDirectories(it) }

        SystemFileSystem.sink(tmp).buffered().use { sink ->
            sink.write(file.source(), file.size())
            sink.flush()
        }

        AddonManager.installKernel(tmp.toString().toPath())

        SystemFileSystem.delete(tmp)
    }

    fun checkForUpdates() {
        scope.launch {
            try {
                snackbarHostState.showSnackbar(Strings.common.checkingUpdate)

                val info = networkService.fetchUpdateInfoWithRetry(
                    "https://raw.githubusercontent.com/eternalfuture-e38299/TEFManager/main/update.json"
                )

                if (info == null) {
                    snackbarHostState.showSnackbar(Strings.common.updateFailed(Strings.error.cantGetUpdate))
                    return@launch
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
                    } else {
                        snackbarHostState.showSnackbar(Strings.common.latestVersion)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to check for updates", e)
                snackbarHostState.showSnackbar(Strings.common.updateFailed(e.message ?: "UNKNOWN"))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.tefmanager_logo),
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TEFManager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = Strings.app.version(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = Strings.settings.about.stableVersion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AboutInfoCard()

            Spacer(modifier = Modifier.height(20.dp))

            SettingSectionTitle(
                title = Strings.settings.about.developerInfo,
                icon = Icons.Rounded.DeveloperMode
            )

            SettingsGroup {
                AboutItemGroup(
                    title = Strings.settings.about.developer,
                    value = Strings.settings.about.developerName
                )

                AboutItemGroup(
                    title = Strings.settings.about.github,
                    value = "https://github.com/eternalfuture-e38299/TEFManager"
                )

                AboutItemGroup(
                    title = Strings.settings.about.license,
                    value = Strings.settings.about.licenseName,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { checkForUpdates() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Update,
                        contentDescription = Strings.settings.about.checkUpdate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.settings.about.checkUpdate,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = { openUrl("https://github.com/eternalfuture-e38299/TEFManager") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.settings.about.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            OutlinedButton(
                onClick = { filePickerLauncher.launch() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(Strings.settings.about.import, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = Strings.settings.about.openSourceStatement,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.settings.about.copyright,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
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
}

@Composable
private fun AboutInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutItem(
                title = Strings.settings.about.buildDate,
                value = BuildConfig.BUILD_DATE
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            AboutItem(
                title = Strings.settings.about.kernelVersion,
                value = ConfigurationState.kernelVersion
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            AboutItem(
                title = Strings.settings.about.supportPlatforms,
                value = "Android/Windows/Linux/macOS"
            )
        }
    }
}

@Composable
private fun AboutItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AboutItemGroup(
    title: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * 比较两个版本号，判断 v1 是否大于 v2
 * 支持格式：1.8.0、1.8、1.8.0.1 等
 */
private fun isVersionGreater(v1: String, v2: String): Boolean {
    val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }

    val maxLen = maxOf(parts1.size, parts2.size)

    for (i in 0 until maxLen) {
        val num1 = if (i < parts1.size) parts1[i] else 0
        val num2 = if (i < parts2.size) parts2[i] else 0
        if (num1 != num2) return num1 > num2
    }
    return false
}