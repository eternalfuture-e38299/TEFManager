package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.materialkolor.ktx.toHex
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.ConfigurationState.AppConfig
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.dialogs.UpdateDialog
import eternal.future.tefmanager.ui.model.UpdateInfo
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.openUrl
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.ktor.utils.io.core.readAvailable
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use
import org.jetbrains.compose.resources.painterResource
import tefmanager.composeapp.generated.resources.Res
import tefmanager.composeapp.generated.resources.icon
import kotlin.time.Clock.System.now

/*******************************************************************************
 * TEFManager - SettingsContent
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
 * Created: 2026/2/5
 *******************************************************************************/

object SettingsContent {
    @Composable
    fun GeneralSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                icon = Icons.Rounded.Update,
                title = Strings.settings.general.update,
                description = Strings.settings.general.updateDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.autoUpdate,
                        {
                            ConfigurationState.autoUpdate = it
                        }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Translate,
                title = Strings.settings.general.language,
                description = Strings.settings.general.languageDec,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        StringsResource.Language.System,
                        StringsResource.Language.ZhHans,
                        StringsResource.Language.En
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(ConfigurationState.language.toString())
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Select Language"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.toString()) },
                                    onClick = {
                                        ConfigurationState.language = lang
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

        }
    }

    @Composable
    fun SettingSectionTitle(
        title: String,
        icon: ImageVector
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    @Composable
    fun SettingItem(
        icon: ImageVector,
        title: String,
        description: String,
        enabled: Boolean = true,
        trailingContent: @Composable () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailingContent()
        }
    }

    @Composable
    fun AppearanceSettings() {
        val appearance = remember { Strings.settings.appearance }

        var showColorPicker by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (showColorPicker) {
                ColorPickerDialog(
                    initialColor = ConfigurationState.themeSeedColor,
                    onColorSelected = { color ->
                        ConfigurationState.themeSeedColor = color
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            SettingItem(
                icon = Icons.Rounded.DarkMode,
                title = appearance.theme,
                description = appearance.themeDec,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val themes = listOf(
                        AppConfig.ThemeMode.SYSTEM,
                        AppConfig.ThemeMode.LIGHT,
                        AppConfig.ThemeMode.DARK,
                        AppConfig.ThemeMode.AUTO,
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(ConfigurationState.themeMode.toString())
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Choose Theme"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            themes.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.toString()) },
                                    onClick = {
                                        ConfigurationState.themeMode = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            SettingItem(
                icon = Icons.Rounded.Palette,
                title = appearance.dynamicColor,
                description = appearance.dynamicColorDec,
                enabled = Platform.dynamicColor,
                trailingContent = {
                    Switch(
                        enabled = Platform.dynamicColor,
                        checked = ConfigurationState.dynamicColor,
                        onCheckedChange = {
                            ConfigurationState.dynamicColor = it
                        }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.ColorLens,
                title = appearance.themeColor,
                description = appearance.themeColorDec,
                enabled = !ConfigurationState.dynamicColor,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (!ConfigurationState.dynamicColor) ConfigurationState.themeSeedColor else ConfigurationState.themeSeedColor.copy(0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { if (!ConfigurationState.dynamicColor) showColorPicker = true }
                    )
                }
            )
        }
    }

    @Composable
    fun ColorPickerDialog(
        initialColor: Color,
        onColorSelected: (Color) -> Unit,
        onDismiss: () -> Unit
    ) {
        var currentColor by remember { mutableStateOf(initialColor) }
        val controller = remember { ColorPickerController() }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = Strings.settings.appearance.chooseColor,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        initialColor = currentColor,
                        onColorChanged = { colorEnvelope ->
                            currentColor = colorEnvelope.color
                        },
                        controller = controller
                    )

                    AlphaSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        wheelAlpha = currentColor.alpha,
                        initialColor = currentColor.copy(alpha = 1f),
                        controller = controller
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )

                            Column {
                                Text(
                                    text = "RGB: ${(currentColor.red * 255).toInt()}, " +
                                            "${(currentColor.green * 255).toInt()}, " +
                                            "${(currentColor.blue * 255).toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "HEX: #${currentColor.toHex()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(Strings.cancel)
                            }

                            Button(
                                onClick = {
                                    onColorSelected(currentColor)
                                    onDismiss()
                                }
                            ) {
                                Text(Strings.apply)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AdvancedSettings() {
        val logExporter = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) {  file ->
            file?.let {
                val sink = file.sink().buffered()
                mergeAllLogFilesPureKotlin(baseDir = Platform.getData("logs").toString(),
                    outputSink = sink)
                sink.flush()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 内核日志相关设置
            SettingItem(
                icon = Icons.Rounded.Memory,
                title = Strings.settings.advanced.kernelLog,
                description = Strings.settings.advanced.kernelLogDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.kernelLogEnabled,
                        onCheckedChange = {
                            ConfigurationState.kernelLogEnabled = it
                            val kernelLogDir = Platform.getData("logs") / "tefkernel"
                            if (ConfigurationState.kernelLogEnabled) okio.FileSystem.SYSTEM.createDirectories(kernelLogDir)
                            else okio.FileSystem.SYSTEM.deleteRecursively(kernelLogDir)
                        }
                    )
                }
            )

            // 软件日志相关设置
            SettingItem(
                icon = Icons.Rounded.Code,
                title = Strings.settings.advanced.softwareLog,
                description = Strings.settings.advanced.softwareLogDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.softwareLogEnabled,
                        onCheckedChange = { ConfigurationState.softwareLogEnabled = it }
                    )
                }
            )

            // 自动清理日志开关
            SettingItem(
                icon = Icons.Rounded.AutoDelete,
                title = Strings.settings.advanced.autoCleanLogs,
                description = Strings.settings.advanced.autoCleanLogsDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.autoCleanLogs,
                        onCheckedChange = { ConfigurationState.autoCleanLogs = it }
                    )
                }
            )

            // 当自动清理开启时显示清理时间设置
            if (ConfigurationState.autoCleanLogs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.settings.advanced.cleanTimeRange,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = Strings.settings.advanced.minutesAgo(ConfigurationState.autoCleanTime),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Slider(
                        value = ConfigurationState.autoCleanTime.toFloat(),
                        onValueChange = { ConfigurationState.autoCleanTime = it.toInt() },
                        valueRange = 60f..720f, // 1小时到12小时
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "1${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(60${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "6${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(360${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "12${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(720${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }

                    val hours = ConfigurationState.autoCleanTime / 60
                    Text(
                        text = "${Strings.settings.advanced.cleanOldLogs} $hours ${Strings.settings.advanced.hours} ${Strings.settings.advanced.createdWithin}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            // 应用日志限制设置
            SettingItem(
                icon = Icons.Rounded.DataUsage,
                title = Strings.settings.advanced.maxAppLogFiles,
                description = Strings.settings.advanced.maxAppLogFilesDec,
                trailingContent = {
                    Text(
                        text = ConfigurationState.maxAppLogFiles.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            )

            Slider(
                value = ConfigurationState.maxAppLogFiles.toFloat(),
                onValueChange = { ConfigurationState.maxAppLogFiles = it.toInt() },
                valueRange = 10f..200f,
                steps = 19,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingItem(
                icon = Icons.Rounded.DataUsage,
                title = Strings.settings.advanced.maxAppLogSize,
                description = Strings.settings.advanced.maxAppLogSizeDec,
                trailingContent = {
                    Text(
                        text = "${ConfigurationState.maxAppLogSizeMB}MB",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            )

            Slider(
                value = ConfigurationState.maxAppLogSizeMB.toFloat(),
                onValueChange = { ConfigurationState.maxAppLogSizeMB = it.toInt() },
                valueRange = 10f..500f,
                steps = 49,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AppLogger.clearAllLogs()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = Strings.settings.advanced.clearLogs
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.settings.advanced.clearLogs)
                }

                OutlinedButton(
                    onClick = {
                        logExporter.launch("tefmanager-logs-${now().toLocalDateTime(TimeZone.currentSystemDefault())}.log")
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = Strings.settings.advanced.exportLogs
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.settings.advanced.exportLogs)
                }
            }
        }
    }

    @Composable
    fun GameSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                icon = Icons.Rounded.Extension,
                title = Strings.settings.game.modSupport,
                description = Strings.settings.game.modSupportDec,
                enabled = Platform.isDesktop,
                trailingContent = {
                    Switch(
                        enabled = Platform.isDesktop,
                        checked = ConfigurationState.modSupportEnabled,
                        onCheckedChange = { ConfigurationState.modSupportEnabled = it }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Sync,
                title = Strings.settings.game.redirectSaves,
                description = Strings.settings.game.redirectSavesDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.redirectSavesEnabled,
                        onCheckedChange = { ConfigurationState.redirectSavesEnabled = it }
                    )
                }
            )
        }
    }

    @Composable
    fun AboutSettings() {
        val json = Json {
            encodeDefaults = true
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val showUpdateDialog = remember { mutableStateOf(false) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

        val filePickerLauncher = rememberFilePickerLauncher(
            mode = FileKitMode.Single
        ) { file ->
            if (file == null) return@rememberFilePickerLauncher

            val tmp = kotlinx.io.files.Path((Platform.getDirectory("tmp") / file.name).toString())
            tmp.parent?.let { SystemFileSystem.createDirectories(it) }

            SystemFileSystem.sink(tmp).buffered().use { sink ->
                sink.write(file.source(), file.size())
                sink.flush()
            }

            tmp.toString().toPath().let { zipPath ->
                okio.FileSystem.SYSTEM.openZip(zipPath).let { zipFs ->
                    val outDir = Platform.getData("tefkernel")

                    val files = if (Platform.isAndroid) listOf(
                            "libtefkernel.android.arm64-v8a.so",
                            "libtefkernel.android.armeabi-v7a.so"
                        )
                    else if(Platform.isWindows) listOf(
                            "libtefkernel.windows.x64.dll",
                            "libtefkernel.windows.x86.dll"
                        ) else if (Platform.isLinux)
                        listOf(
                            "libtefkernel.linux.x86.so",
                            "libtefkernel.linux.x64.so"
                        ) else listOf()

                    files.forEach { name ->
                        val src = name.toPath()
                        if (!zipFs.exists(src)) return@forEach

                        zipFs.source(src).buffer().use { input ->
                            okio.FileSystem.SYSTEM
                                .sink(outDir / name)
                                .buffer()
                                .use { output ->
                                    output.write(
                                        input,
                                        zipFs.metadata(src).size!!
                                    )
                                }
                        }
                    }
                }
            }

            SystemFileSystem.delete(tmp)
        }

        // 检查更新函数
        fun checkForUpdates() {
            scope.launch {
                try {
                    val networkService = NetworkService()
                    snackbarHostState.showSnackbar(Strings.common.checkingUpdate)

                    val downloadPath = Platform.getData(null) / "update-download.json"
                    // 这里应该替换为实际的更新检查URL
                    networkService.downloadFile(
                        "https://github.com/eternalfuture-e38299/TEFManager/releases/download/Latest/update.json",
                        downloadPath
                    )

                    val info = json.decodeFromString<UpdateInfo>(
                        okio.FileSystem.SYSTEM.source(downloadPath).buffer().readUtf8()
                    )

                    // 检查版本是否需要更新（这里需要根据实际版本比较逻辑）
                    if (info.tefmanager.newVersion != "1.0.0") { // 临时硬编码版本号
                        updateInfo = info
                        showUpdateDialog.value = true
                    } else {
                        snackbarHostState.showSnackbar(Strings.common.latestVersion)
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(Res.drawable.icon),
                            contentDescription = "Logo",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 应用信息
                Text(
                    text = Strings.settings.about.appName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = Strings.settings.about.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = Strings.settings.about.stableVersion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 信息卡片
                AboutInfoCard()

                Spacer(modifier = Modifier.height(24.dp))

                // 开发者信息
                SettingSectionTitle(
                    title = Strings.settings.about.developerInfo,
                    icon = Icons.Rounded.DeveloperMode
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AboutItem(
                        title = Strings.settings.about.developer,
                        value = Strings.settings.about.developerName
                    )

                    AboutItem(
                        title = Strings.settings.about.github,
                        value = Strings.settings.about.githubUrl
                    )

                    AboutItem(
                        title = Strings.settings.about.license,
                        value = Strings.settings.about.licenseName
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { checkForUpdates() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Update,
                            contentDescription = Strings.settings.about.checkUpdate
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.settings.about.checkUpdate)
                    }

                    OutlinedButton(
                        onClick = { openUrl(Strings.settings.about.githubUrl) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = Strings.settings.about.about
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.settings.about.about)
                    }
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("导入内核")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 开源声明
                Text(
                    text = Strings.settings.about.openSourceStatement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Strings.settings.about.copyright,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 更新对话框
        if (showUpdateDialog.value && updateInfo != null) {
            UpdateDialog(
                updates = listOf(updateInfo!!.tefmanager),
                onDismiss = { showUpdateDialog.value = false }
            )
        }
    }

    @Composable
    private fun AboutInfoCard() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutItem(
                    title = "编译日期",
                    value = "2026年2月4日"
                )

                AboutItem(
                    title = "内核版本",
                    value = "1.0.0"
                )

                AboutItem(
                    title = "支持平台",
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

    private fun mergeAllLogFilesPureKotlin(
        baseDir: String,
        outputSink: Sink,
        includeAppLogs: Boolean = true,
        includeKernelLogs: Boolean = true
    ) {
        val fileSystem = SystemFileSystem

        try {
            // 处理应用日志
            if (includeAppLogs) {
                val appDir = Path(baseDir, "app")
                if (fileSystem.exists(appDir)) {
                    processDirectoryPureKotlin(fileSystem, appDir, "APP", outputSink)
                }
            }

            // 处理内核日志
            if (includeKernelLogs) {
                val kernelDir = Path(baseDir, "tefkernel")
                if (fileSystem.exists(kernelDir)) {
                    processDirectoryPureKotlin(fileSystem, kernelDir, "KERNEL", outputSink)
                }
            }
        } finally {
            outputSink.flush()
        }
    }

    // 处理单个目录
    private fun processDirectoryPureKotlin(
        fileSystem: FileSystem,
        directory: Path,
        type: String,
        outputSink: Sink
    ) {
        try {
            val files = fileSystem.list(directory).filter { fileSystem.metadataOrNull(it)?.isRegularFile ?: false }.sortedBy { it.name }

            files.forEach { logFile ->
                try {
                    // 写入文件头
                    outputSink.writeString("=== $type FILE: ${logFile.name} ===\n")

                    // 流式复制内容
                    fileSystem.source(logFile).buffered().use { source ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (source.readAvailable(buffer).also { bytesRead = it } > 0) {
                            outputSink.write(buffer, 0, bytesRead)
                        }
                    }

                    outputSink.writeString("\n\n")
                    outputSink.flush()

                } catch (e: Exception) {
                    outputSink.writeString("=== ERROR processing $type file ${logFile.name}: ${e.message} ===\n\n")
                }
            }
        } catch (e: Exception) {
            outputSink.writeString("=== ERROR accessing $type directory: ${e.message} ===\n\n")
        }
    }
}