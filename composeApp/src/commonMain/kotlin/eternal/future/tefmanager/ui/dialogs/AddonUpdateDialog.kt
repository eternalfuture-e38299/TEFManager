package eternal.future.tefmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.AddonUpdateInfo
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.NetworkService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/*******************************************************************************
 * RotatingArtLauncher - AddonUpdateDialog
 * 
 * This file is part of the RotatingArtLauncher project.
 * 
 * Copyright (C) 2026 RotatingArtLauncher Contributors
 * 
 * Created by: eternalfuture-e38299 (2026/10/31)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

@Composable
fun AddonUpdateDialog(
    title: String,                       // Mod 名 / Loader 名
    currentVersion: String,
    updateInfo: AddonUpdateInfo,
    installPackage: (Path) -> Unit,      // 调用方决定如何安装（ModManager / ModLoaderManager）
    onDismiss: () -> Unit
) {
    val networkService = remember { NetworkService() }
    val scope = rememberCoroutineScope()
    val progress by networkService.downloadProgress.collectAsState()
    var downloading by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { networkService.close() } }

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text(Strings.manager.update.title(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 版本对比
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = {
                        Text(Strings.manager.update.currentVersion(currentVersion))
                    })
                    AssistChip(onClick = {}, label = {
                        Text(Strings.manager.update.newVersion(updateInfo.version))
                    })
                }

                Text(Strings.manager.update.fileSize(updateInfo.fileSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)

                HorizontalDivider()

                Text(Strings.manager.update.changelog,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium)
                Text(updateInfo.updateLog,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))

                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${progress.currentBytes / 1024} / ${progress.totalBytes / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    progress.error?.let {
                        Text(Strings.common.updateFailed(it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                enabled = !downloading,
                onClick = {
                    downloading = true
                    scope.launch {
                        val tmpDir = Platform.getDirectory("tmp") / "addon_update"
                        FileSystem.SYSTEM.createDirectories(tmpDir)
                        val fileName = updateInfo.downloadUrl.substringAfterLast('/')
                        val out = tmpDir / fileName

                        networkService.downloadFile(updateInfo.downloadUrl, out)

                        // 等待下载结果（collect 到 COMPLETED / ERROR / CANCELLED）
                        networkService.downloadProgress.first {
                            it.status in setOf(
                                NetworkService.DownloadStatus.COMPLETED,
                                NetworkService.DownloadStatus.ERROR,
                                NetworkService.DownloadStatus.CANCELLED
                            )
                        }.let { final ->
                            downloading = false
                            if (final.status == NetworkService.DownloadStatus.COMPLETED) {
                                installPackage(out)
                                onDismiss()
                            }
                        }
                    }
                }
            ) { Text(Strings.common.download) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !downloading) {
                Text(Strings.cancel)
            }
        }
    )
}