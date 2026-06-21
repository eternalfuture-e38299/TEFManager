package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SwitchDefaults
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsGroup {
                M3ESettingItem(
                    icon = Icons.Rounded.Update,
                    title = Strings.settings.general.update,
                    description = Strings.settings.general.updateDec,
                    trailingContent = {
                        Switch(
                            checked = ConfigurationState.autoUpdate,
                            onCheckedChange = { ConfigurationState.autoUpdate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.autoUpdate) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.autoUpdate) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.autoUpdate) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )

                M3ESettingItem(
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
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Text(
                                    text = ConfigurationState.language.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
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
                                        text = {
                                            Text(
                                                text = lang.toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
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
    }

    @Composable
    fun SettingsGroup(
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                content()
            }
        }
    }

    @Composable
    fun M3ESettingItem(
        icon: ImageVector,
        title: String,
        description: String,
        enabled: Boolean = true,
        showDivider: Boolean = true,
        trailingContent: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .alpha(if (enabled) 1f else 0.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }

                trailingContent()
            }

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 66.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        }
    }

    @Composable
    fun M3ESettingSectionTitle(
        title: String,
        icon: ImageVector
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun AppearanceSettings() {
        val appearance = remember { Strings.settings.appearance }

        var showColorPicker by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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

            SettingsGroup {
                M3ESettingItem(
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
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Text(
                                    text = ConfigurationState.themeMode.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
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
                                        text = {
                                            Text(
                                                text = t.toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
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

                M3ESettingItem(
                    icon = Icons.Rounded.Palette,
                    title = appearance.dynamicColor,
                    description = appearance.dynamicColorDec,
                    enabled = Platform.dynamicColor,
                    trailingContent = {
                        Switch(
                            enabled = Platform.dynamicColor,
                            checked = ConfigurationState.dynamicColor,
                            onCheckedChange = { ConfigurationState.dynamicColor = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.dynamicColor) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.dynamicColor) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.dynamicColor) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )

                M3ESettingItem(
                    icon = Icons.Rounded.ColorLens,
                    title = appearance.themeColor,
                    description = appearance.themeColorDec,
                    enabled = !ConfigurationState.dynamicColor,
                    showDivider = false,
                    trailingContent = {
                        Surface(
                            shape = CircleShape,
                            color = if (!ConfigurationState.dynamicColor) {
                                ConfigurationState.themeSeedColor
                            } else {
                                ConfigurationState.themeSeedColor.copy(alpha = 0.3f)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(enabled = !ConfigurationState.dynamicColor) {
                                    if (!ConfigurationState.dynamicColor) showColorPicker = true
                                }
                        ) {}
                    }
                )
            }
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
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = Strings.settings.appearance.chooseColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentColor,
                                modifier = Modifier.size(40.dp)
                            ) {}

                            Column {
                                Text(
                                    text = "RGB: ${(currentColor.red * 255).toInt()}, " +
                                            "${(currentColor.green * 255).toInt()}, " +
                                            "${(currentColor.blue * 255).toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "HEX: #${currentColor.toHex()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    Strings.cancel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Button(
                                onClick = {
                                    onColorSelected(currentColor)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    Strings.apply,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AdvancedSettings() {
        val logExporter = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { file ->
            file?.let {
                val sink = file.sink().buffered()
                mergeAllLogFilesPureKotlin(
                    baseDir = Platform.getData("logs").toString(),
                    outputSink = sink
                )
                sink.flush()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsGroup {
                M3ESettingItem(
                    icon = Icons.Rounded.Memory,
                    title = Strings.settings.advanced.kernelLog,
                    description = Strings.settings.advanced.kernelLogDec,
                    trailingContent = {
                        Switch(
                            checked = ConfigurationState.kernelLogEnabled,
                            onCheckedChange = {
                                ConfigurationState.kernelLogEnabled = it
                                val kernelLogDir = Platform.getData("logs") / "tefkernel"
                                if (ConfigurationState.kernelLogEnabled) {
                                    okio.FileSystem.SYSTEM.createDirectories(kernelLogDir)
                                } else {
                                    okio.FileSystem.SYSTEM.deleteRecursively(kernelLogDir)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.kernelLogEnabled) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.kernelLogEnabled) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.kernelLogEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )

                M3ESettingItem(
                    icon = Icons.Rounded.Code,
                    title = Strings.settings.advanced.softwareLog,
                    description = Strings.settings.advanced.softwareLogDec,
                    trailingContent = {
                        Switch(
                            checked = ConfigurationState.softwareLogEnabled,
                            onCheckedChange = { ConfigurationState.softwareLogEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.softwareLogEnabled) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.softwareLogEnabled) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.softwareLogEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )

                M3ESettingItem(
                    icon = Icons.Rounded.AutoDelete,
                    title = Strings.settings.advanced.autoCleanLogs,
                    description = Strings.settings.advanced.autoCleanLogsDec,
                    showDivider = false,
                    trailingContent = {
                        Switch(
                            checked = ConfigurationState.autoCleanLogs,
                            onCheckedChange = { ConfigurationState.autoCleanLogs = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.autoCleanLogs) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.autoCleanLogs) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.autoCleanLogs) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )
            }

            if (ConfigurationState.autoCleanLogs) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.settings.advanced.cleanTimeRange,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Strings.settings.advanced.minutesAgo(ConfigurationState.autoCleanTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Slider(
                            value = ConfigurationState.autoCleanTime.toFloat(),
                            onValueChange = { ConfigurationState.autoCleanTime = it.toInt() },
                            valueRange = 60f..720f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "6${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "12${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val hours = ConfigurationState.autoCleanTime / 60
                        Text(
                            text = "${Strings.settings.advanced.cleanOldLogs} $hours ${Strings.settings.advanced.hours} ${Strings.settings.advanced.createdWithin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            SettingsGroup {
                M3ESettingItem(
                    icon = Icons.Rounded.DataUsage,
                    title = Strings.settings.advanced.maxAppLogFiles,
                    description = Strings.settings.advanced.maxAppLogFilesDec,
                    trailingContent = {
                        Text(
                            text = ConfigurationState.maxAppLogFiles.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )

                M3ESettingItem(
                    icon = Icons.Rounded.DataUsage,
                    title = Strings.settings.advanced.maxAppLogSize,
                    description = Strings.settings.advanced.maxAppLogSizeDec,
                    showDivider = false,
                    trailingContent = {
                        Text(
                            text = "${ConfigurationState.maxAppLogSizeMB}MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }

            Slider(
                value = ConfigurationState.maxAppLogFiles.toFloat(),
                onValueChange = { ConfigurationState.maxAppLogFiles = it.toInt() },
                valueRange = 10f..200f,
                steps = 19,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { AppLogger.clearAllLogs() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = Strings.settings.advanced.clearLogs,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.settings.advanced.clearLogs,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = {
                        logExporter.launch("tefmanager-logs-${now().toLocalDateTime(TimeZone.currentSystemDefault())}.log")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = Strings.settings.advanced.exportLogs,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.settings.advanced.exportLogs,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    @Composable
    fun GameSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsGroup {
                M3ESettingItem(
                    icon = Icons.Rounded.Extension,
                    title = Strings.settings.game.modSupport,
                    description = Strings.settings.game.modSupportDec,
                    enabled = Platform.isDesktop,
                    trailingContent = {
                        Switch(
                            enabled = Platform.isDesktop,
                            checked = ConfigurationState.modSupportEnabled,
                            onCheckedChange = { ConfigurationState.modSupportEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.modSupportEnabled) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.modSupportEnabled) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.modSupportEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )

                M3ESettingItem(
                    icon = Icons.Rounded.Sync,
                    title = Strings.settings.game.redirectSaves,
                    description = Strings.settings.game.redirectSavesDec,
                    showDivider = false,
                    trailingContent = {
                        Switch(
                            checked = ConfigurationState.redirectSavesEnabled,
                            onCheckedChange = { ConfigurationState.redirectSavesEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (ConfigurationState.redirectSavesEnabled) {
                                        Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.Close
                                    },
                                    contentDescription = if (ConfigurationState.redirectSavesEnabled) "已启用" else "已禁用",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (ConfigurationState.redirectSavesEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            }
                        )
                    }
                )
            }
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

        fun checkForUpdates() {
            scope.launch {
                try {
                    val networkService = NetworkService()
                    snackbarHostState.showSnackbar(Strings.common.checkingUpdate)

                    val downloadPath = Platform.getData(null) / "update-download.json"
                    networkService.downloadFile(
                        "https://github.com/eternalfuture-e38299/TEFManager/releases/download/Latest/update.json",
                        downloadPath
                    )

                    val info = json.decodeFromString<UpdateInfo>(
                        okio.FileSystem.SYSTEM.source(downloadPath).buffer().readUtf8()
                    )

                    if (info.tefmanager.newVersion != "1.0.0") {
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = Strings.settings.about.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = Strings.settings.about.version,
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

                M3ESettingSectionTitle(
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
                        value = Strings.settings.about.githubUrl
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
                        onClick = { openUrl(Strings.settings.about.githubUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = Strings.settings.about.about,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Strings.settings.about.about,
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
                    Text("导入内核", style = MaterialTheme.typography.labelLarge)
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
                updates = listOf(updateInfo!!.tefmanager),
                onDismiss = { showUpdateDialog.value = false }
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
                    title = "编译日期",
                    value = "2026年2月4日"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                AboutItem(
                    title = "内核版本",
                    value = "1.0.0"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
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

    private fun mergeAllLogFilesPureKotlin(
        baseDir: String,
        outputSink: Sink,
        includeAppLogs: Boolean = true,
        includeKernelLogs: Boolean = true
    ) {
        val fileSystem = SystemFileSystem

        try {
            if (includeAppLogs) {
                val appDir = Path(baseDir, "app")
                if (fileSystem.exists(appDir)) {
                    processDirectoryPureKotlin(fileSystem, appDir, "APP", outputSink)
                }
            }

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
                    outputSink.writeString("=== $type FILE: ${logFile.name} ===\n")

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