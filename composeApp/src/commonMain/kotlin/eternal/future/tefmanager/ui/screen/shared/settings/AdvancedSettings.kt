package eternal.future.tefmanager.ui.screen.shared.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.SettingItem
import eternal.future.tefmanager.ui.component.SettingsGroup
import eternal.future.tefmanager.ui.component.Switch
import eternal.future.tefmanager.utils.AppLogger
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.sink
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.RawSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import no.synth.kmpzip.zip.ZipEntry
import okio.SYSTEM
import kotlin.time.Clock.System.now

/*******************************************************************************
 * TEFManager - AdvancedSettings
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
fun AdvancedSettings() {
    val logExporter = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { file ->
        file?.let {
            file.sink().use { sink ->
                mergeAllLogFilesPureKotlin(
                    baseDir = Platform.getData("logs").toString(),
                    outputSink = sink
                )
                sink.flush()
            }
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
                            if (ConfigurationState.kernelLogEnabled) {
                                okio.FileSystem.SYSTEM.createDirectories(kernelLogDir)
                            } else {
                                okio.FileSystem.SYSTEM.deleteRecursively(kernelLogDir)
                            }
                        }
                    )
                }
            )

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

            SettingItem(
                icon = Icons.Rounded.AutoDelete,
                title = Strings.settings.advanced.autoCleanLogs,
                description = Strings.settings.advanced.autoCleanLogsDec,
                showDivider = false,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.autoCleanLogs,
                        onCheckedChange = { ConfigurationState.autoCleanLogs = it }
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
            SettingItem(
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

            SettingItem(
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
            valueRange = 1f..20f,
            steps = 20,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Slider(
            value = ConfigurationState.maxAppLogSizeMB.toFloat(),
            onValueChange = { ConfigurationState.maxAppLogSizeMB = it.toInt() },
            valueRange = 1f..20f,
            steps = 20,
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
                    logExporter.launch("tefmanager-logs-${now().toLocalDateTime(TimeZone.currentSystemDefault())}", defaultExtension = "zip")
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

fun mergeAllLogFilesPureKotlin(
    baseDir: String,
    outputSink: RawSink,
    includeAppLogs: Boolean = true,
    includeKernelLogs: Boolean = true
) {
    val fileSystem = SystemFileSystem

    no.synth.kmpzip.kotlinx.ZipOutputStream(outputSink.buffered()).use { zos ->

        // 处理App日志
        if (includeAppLogs) {
            val appDir = Path(baseDir, "app")
            if (fileSystem.exists(appDir)) {
                fileSystem.list(appDir)
                    .filter { fileSystem.metadataOrNull(it)?.isRegularFile ?: false }
                    .forEach { file ->
                        val content = fileSystem.source(file).buffered().use { source -> source.readByteArray()  }
                        zos.putNextEntry(ZipEntry("App Log/${file.name}"))
                        zos.write(content)
                        zos.closeEntry()
                    }
            }
        }

        // 处理Kernel日志
        if (includeKernelLogs) {
            val kernelDir = Path(baseDir, "tefkernel")
            if (fileSystem.exists(kernelDir)) {
                fileSystem.list(kernelDir)
                    .filter { fileSystem.metadataOrNull(it)?.isRegularFile ?: false }
                    .forEach { file ->
                        val content = fileSystem.source(file).buffered().use { source -> source.readByteArray()  }
                        zos.putNextEntry(ZipEntry("TEFKernel Log/${file.name}"))
                        zos.write(content)
                        zos.closeEntry()
                    }
            }
        }

        zos.finish()
    }
}