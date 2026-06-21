package eternal.future.tefmanager.ui.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.time.Duration.Companion.milliseconds

/*******************************************************************************
 * TEFManager - ResourcePackInstallDialog
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
 * Created: 2026/6/21
 *******************************************************************************/

@Composable
fun ResourcePackInstallDialog(
    filePaths: List<Path>,
    onDismiss: () -> Unit
) {
    var isInstalling by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentStatus by remember { mutableStateOf("准备安装") }
    val scope = rememberCoroutineScope()

    val progressValue = if (filePaths.isEmpty()) 0f else currentProgress

    Dialog(onDismissRequest = { if (!isInstalling) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(220.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isComplete) {
                                if (hasError) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isComplete) {
                                        if (hasError) Icons.Rounded.Error
                                        else Icons.Rounded.Check
                                    } else {
                                        Icons.Rounded.FileDownload
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isComplete) {
                                        if (hasError) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        Text(
                            text = if (isComplete) {
                                if (hasError) "安装失败" else "安装完成"
                            } else {
                                "安装资源包"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 状态信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInstalling && !isComplete) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (isComplete) {
                        Icon(
                            imageVector = if (hasError) Icons.Rounded.Error else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isComplete) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("确定")
                        }
                    } else if (!isInstalling) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                isInstalling = true
                                scope.launch {
                                    startResourcePackInstallation(
                                        filePaths = filePaths,
                                        onProgress = { index, status, error ->
                                            currentIndex = index
                                            currentProgress = (index + 1).toFloat() / filePaths.size
                                            currentStatus = status
                                            hasError = error
                                        },
                                        onComplete = { success ->
                                            isComplete = true
                                            isInstalling = false
                                            hasError = !success
                                            currentProgress = 1f
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("安装")
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("安装中...")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun startResourcePackInstallation(
    filePaths: List<Path>,
    onProgress: (Int, String, Boolean) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        var allSuccess = true

        for ((index, filePath) in filePaths.withIndex()) {
            val packName = filePath.name
            onProgress(index, "正在安装: $packName", false)

            try {
                var installCompleted = false
                var hasInstallError = false

                ResourcePackManager.installPack(
                    filePath = filePath,
                    progressCallback = { progress, error ->
                        when (progress) {
                            ResourcePackManager.InstallProgress.COMPLETED -> {
                                if (error == null && !installCompleted) {
                                    installCompleted = true
                                    onProgress(index, "安装成功: $packName", false)
                                } else if (error != null) {
                                    hasInstallError = true
                                    allSuccess = false
                                    onProgress(index, "安装失败: ${error.message}", true)
                                }
                            }
                            ResourcePackManager.InstallProgress.ERROR -> {
                                if (error != null && !hasInstallError) {
                                    hasInstallError = true
                                    allSuccess = false
                                    onProgress(index, "安装失败: ${error.message}", true)
                                }
                            }
                            ResourcePackManager.InstallProgress.OPENING_PACKAGE -> {
                                onProgress(index, "打开包: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.READING_MANIFEST -> {
                                onProgress(index, "读取清单: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.PARSING_METADATA -> {
                                onProgress(index, "解析元数据: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.CHECKING_EXISTING -> {
                                onProgress(index, "检查已存在: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.COPYING_FILES -> {
                                onProgress(index, "复制文件: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.EXTRACTING_ICON -> {
                                onProgress(index, "提取图标: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.UPDATING_DATABASE -> {
                                onProgress(index, "更新数据库: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.FINISHING -> {
                                onProgress(index, "完成: $packName", false)
                            }
                            ResourcePackManager.InstallProgress.STARTING -> {
                                onProgress(index, "开始安装: $packName", false)
                            }
                        }
                    }
                )

                if (!installCompleted && !hasInstallError) {
                    onProgress(index, "安装完成: $packName", false)
                }

            } catch (e: Exception) {
                AppLogger.e("Failed to install resource pack: $packName", e)
                onProgress(index, "安装失败: ${e.message ?: "未知错误"}", true)
                allSuccess = false
            }

            delay(100.milliseconds)
        }

        if (allSuccess) onProgress(filePaths.size - 1, "所有资源包安装完成", false)
        onComplete(allSuccess)
    }
}