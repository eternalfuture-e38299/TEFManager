package eternal.future.tefmanager.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ButtonDefaults
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
import eternal.future.tefmanager.ui.model.UpdateInfo
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.openUrl


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
    updates: List<UpdateInfo.Info>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkServices = remember(updates) {
        updates.associate { it.moduleName to NetworkService() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "可用更新",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 关闭按钮
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 更新描述
                Text(
                    text = "发现 ${updates.size} 个模块需要更新",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 更新列表
                if (updates.size > 1) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(updates) { update ->
                            UpdateItem(update = update, networkService = networkServices[update.moduleName]!!)
                        }
                    }
                } else if (updates.isNotEmpty()) {
                    UpdateItem(update = updates.first(), networkService = networkServices[updates.first().moduleName]!!)
                }

                // 底部按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateItem(update: UpdateInfo.Info, networkService: NetworkService) {
    val downloadProgress by networkService.downloadProgress.collectAsState()

    // 防止取消按钮重复点击
    var isCancelling by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 模块名称和版本信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = update.moduleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "版本: ${update.newVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 操作按钮
                when (downloadProgress.status) {
                    NetworkService.DownloadStatus.IDLE -> {
                        if (update.downloadUrl.isEmpty()) {
                            // 外部链接按钮
                            OutlinedButton(
                                onClick = {
                                    openUrl(update.externalUrl)
                                },
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text("查看详情")
                            }
                        } else {
                            // 下载按钮
                            FilledTonalButton(
                                onClick = {
                                    // 直接调用，不需要自己创建协程
                                    networkService.downloadFile(
                                        update.downloadUrl,
                                        Platform.getData(null) / update.fileName
                                    )
                                },
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("下载")
                            }
                        }
                    }

                    NetworkService.DownloadStatus.DOWNLOADING -> {
                        val progress = downloadProgress.percentage
                        // 下载进度显示 + 取消按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 取消按钮 - 防重复点击
                            IconButton(
                                onClick = {
                                    if (!isCancelling) {
                                        isCancelling = true
                                        networkService.cancelDownload()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "取消下载",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // 下载进度显示
                            Column(horizontalAlignment = Alignment.End) {
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.width(80.dp).height(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${progress.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    NetworkService.DownloadStatus.COMPLETED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "完成", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("完成", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    NetworkService.DownloadStatus.ERROR -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, "错误", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("重试", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    NetworkService.DownloadStatus.CANCELLED -> {
                        // 重置取消标志
                        LaunchedEffect(Unit) {
                            isCancelling = false
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, "已取消", tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(4.dp))
                            Text("已取消", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 更新描述
            if (update.description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = update.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 文件大小信息
            if (update.fileSize.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = update.fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// 使用示例
@Composable
fun UpdateDialogExample() {
    val sampleUpdates = listOf(
        UpdateInfo.Info(
            "tefloader.zip",
            moduleName = "tefloader",
            newVersion = "v1.0.0",
            description = "用于内核加载",
            downloadUrl = "https://gitee.com/eternalfuture/tefmod-loader/releases/download/tefmodloader-2025-05-24-core.efml/tefmodloader.efml",
            fileSize = "3.2 MB",
            externalUrl = "" // 直接使用字符串
        ),
        UpdateInfo.Info(
            "tefloader.zip",
            moduleName = "tefloadaer",
            newVersion = "v1.0.0",
            description = "用于内核加载",
            downloadUrl = "",
            fileSize = "3.2 MB",
            externalUrl = "https://gitee.com/eternalfuture/tefmod-loader/releases/download/tefmodloader-2025-05-24-core.efml/tefmodloader.efml" // 直接使用字符串
        )
    )


    UpdateDialog(
        updates = sampleUpdates,
        onDismiss = { /* 处理关闭逻辑 */ }
    )
}