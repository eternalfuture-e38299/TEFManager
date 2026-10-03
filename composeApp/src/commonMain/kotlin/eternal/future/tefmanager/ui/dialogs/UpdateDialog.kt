package eternal.future.tefmanager.ui.dialogs

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.UpdateInfo
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - UpdateDialog
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
 * Created: 2026/2/16
 *******************************************************************************/

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onModuleInstalled: ((ModuleType, Path) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 为每个模块创建独立的 NetworkService
    val tefLoaderService = remember { NetworkService() }
    val tefKernelService = remember { NetworkService() }
    val languageService = remember { NetworkService() }
    val materialService = remember { NetworkService() }
    val fontService = remember { NetworkService() }
    val musicService = remember { NetworkService() }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            tefLoaderService.close()
            tefKernelService.close()
            languageService.close()
            materialService.close()
            fontService.close()
            musicService.close()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .wrapContentSize()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier
                .padding(16.dp)
                .wrapContentSize()
                .wrapContentHeight()
            ) {
                Text(
                    text = Strings.update.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {


                    // 软件更新
                    updateInfo.tefmanager?.let {
                        ModuleUpdateItem(
                            title = "TEFManager",
                            info = it,
                            type = ModuleType.External,
                            networkService = null,
                            onInstall = null
                        )
                    }


                    updateInfo.tefloader?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModuleUpdateItem(
                            title = "TEFLoader",
                            info = it,
                            type = ModuleType.TEFLoader,
                            networkService = tefLoaderService,
                            onInstall = onModuleInstalled
                        )
                    }

                    updateInfo.tefkernel?.let {
                        Spacer(modifier = Modifier.height(8.dp))

                        // 内核更新
                        ModuleUpdateItem(
                            title = Strings.update.kernel,
                            info = it,
                            type = ModuleType.TEFKernel,
                            networkService = tefKernelService,
                            onInstall = onModuleInstalled
                        )
                    }

                    // 语言包
                    updateInfo.language?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModuleUpdateItem(
                            title = Strings.update.language,
                            info = it,
                            type = ModuleType.Language,
                            networkService = languageService,
                            onInstall = onModuleInstalled
                        )
                    }

                    // 材质包
                    updateInfo.texture?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModuleUpdateItem(
                            title = Strings.update.texture,
                            info = it,
                            type = ModuleType.Texture,
                            networkService = materialService,
                            onInstall = onModuleInstalled
                        )
                    }

                    // 字体包
                    updateInfo.font?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModuleUpdateItem(
                            title = Strings.update.font,
                            info = it,
                            type = ModuleType.Font,
                            networkService = fontService,
                            onInstall = onModuleInstalled
                        )
                    }

                    // 音乐包
                    updateInfo.music?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModuleUpdateItem(
                            title = Strings.update.music,
                            info = it,
                            type = ModuleType.Music,
                            networkService = musicService,
                            onInstall = onModuleInstalled
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.close)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleUpdateItem(
    title: String,
    info: UpdateInfo.Info,
    type: ModuleType,
    networkService: NetworkService?,
    onInstall: ((ModuleType, Path) -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "v${info.newVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (info.description.isNotEmpty()) {
                    Text(
                        text = info.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (info.fileSize.isNotEmpty()) {
                    Text(
                        text = info.fileSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            when (type) {
                ModuleType.External -> {
                    OutlinedButton(
                        onClick = { openUrl(info.externalUrl) },
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(Strings.update.view)
                    }
                }
                else -> {
                    if (networkService != null && onInstall != null) {
                        DownloadButton(
                            networkService = networkService,
                            downloadUrl = info.downloadUrl,
                            fileName = info.fileName,
                            moduleType = type,
                            onInstall = onInstall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(
    networkService: NetworkService,
    downloadUrl: String,
    fileName: String,
    moduleType: ModuleType,
    onInstall: (ModuleType, Path) -> Unit
) {
    val downloadProgress by networkService.downloadProgress.collectAsState()
    var isCancelling by remember { mutableStateOf(false) }

    // 保存当前下载的路径和URL，用于重试
    var currentDownloadPath by remember { mutableStateOf<Path?>(null) }
    var currentDownloadUrl by remember { mutableStateOf<String?>(null) }

    // 重置取消状态
    LaunchedEffect(downloadProgress.status) {
        when (downloadProgress.status) {
            NetworkService.DownloadStatus.CANCELLED -> {
                isCancelling = false
            }
            NetworkService.DownloadStatus.COMPLETED -> {
                // 下载完成，调用安装回调
                currentDownloadPath?.let { path ->
                    onInstall(moduleType, path)
                }
                // 清空状态
                currentDownloadPath = null
                currentDownloadUrl = null
            }
            else -> {}
        }
    }

    when (downloadProgress.status) {
        NetworkService.DownloadStatus.IDLE -> {
            FilledTonalButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        // 获取临时目录
                        val tempDir = Platform.getDirectory("tmp")
                        val outputPath = tempDir / fileName

                        // 确保临时目录存在
                        FileSystem.SYSTEM.createDirectories(tempDir)

                        // 保存当前下载信息
                        currentDownloadPath = outputPath
                        currentDownloadUrl = downloadUrl

                        networkService.downloadFile(downloadUrl, outputPath)
                    }
                },
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.update.download)
            }
        }

        NetworkService.DownloadStatus.DOWNLOADING -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (!isCancelling) {
                            isCancelling = true
                            networkService.cancelDownload()
                            // 取消时清空保存的状态
                            currentDownloadPath = null
                            currentDownloadUrl = null
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentage / 100f },
                        modifier = Modifier.width(80.dp).height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                    )
                    Text(
                        text = "${downloadProgress.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        NetworkService.DownloadStatus.COMPLETED -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    Strings.update.status.completed,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        NetworkService.DownloadStatus.ERROR -> {
            FilledTonalButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        // 使用保存的路径和URL重试
                        val outputPath = currentDownloadPath ?: run {
                            val tempDir = Platform.getDirectory("tmp")
                            tempDir / fileName
                        }.also { currentDownloadPath = it }

                        val url = currentDownloadUrl ?: downloadUrl
                        currentDownloadUrl = url

                        // 确保临时目录存在
                        val tempDir = Platform.getDirectory("tmp")
                        FileSystem.SYSTEM.createDirectories(tempDir)
                        networkService.downloadFile(url, outputPath)
                    }
                },
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(Icons.Default.Error, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.update.retry)
            }
        }

        NetworkService.DownloadStatus.CANCELLED -> {
            FilledTonalButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val tempDir = Platform.getDirectory("tmp")
                        val outputPath = tempDir / fileName

                        FileSystem.SYSTEM.createDirectories(tempDir)

                        currentDownloadPath = outputPath
                        currentDownloadUrl = downloadUrl

                        networkService.downloadFile(downloadUrl, outputPath)
                    }
                },
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.update.redownload)
            }
        }
    }
}

enum class ModuleType {
    External,      // 软件更新（外部链接）
    TEFLoader,     // TEFLoader
    TEFKernel,     // 内核
    Language,      // 语言包
    Texture,       // 材质包
    Font,          // 字体包
    Music          // 音乐包
}