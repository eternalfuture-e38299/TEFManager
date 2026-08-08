package eternal.future.tefmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.strings.StringsResource.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.time.Duration.Companion.milliseconds

/*******************************************************************************
 * TEFManager - AddonInstallOrUpdateDialog
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
 * Created: 2026/3/29
 *******************************************************************************/

@Composable
fun AddonInstallOrUpdateDialog(
    filePaths: List<Path>,
    onDismiss: () -> Unit
) {
    var isInstalling by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentStatus by remember { mutableStateOf(Strings.manager.install.status.ready) }
    val scope = rememberCoroutineScope()

    // 计算进度
    val progressValue = if (filePaths.isEmpty()) 0f else currentProgress

    Dialog(onDismissRequest = { if (!isInstalling) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isComplete) {
                            if (hasError) Strings.manager.install.failed else Strings.manager.install.success
                        } else {
                            Strings.manager.install.title
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isInstalling
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Strings.close
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 状态信息
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (isInstalling && !isComplete) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else if (isComplete) {
                            val icon =
                                if (hasError) Icons.Default.Error else Icons.Default.CheckCircle
                            val iconColor =
                                if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
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
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isComplete) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(Strings.confirm)
                        }
                    } else if (!isInstalling) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(Strings.cancel)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                isInstalling = true
                                scope.launch {
                                    startInstallation(
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.install)
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
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
                            Text(Strings.manager.install.status.installing)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun startInstallation(
    filePaths: List<Path>,
    onProgress: (Int, String, Boolean) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        var allSuccess = true

        for ((index, filePath) in filePaths.withIndex()) {
            val addonName = filePath.name
            // 更新进度 - 开始安装当前文件
            onProgress(index, Strings.manager.install.status.installingFile(addonName), false)

            try {
                var hasInstallError = false
                var installCompleted = false

                AddonManager.install(filePath) { progress, error ->
                    when (progress) {
                        AddonManager.InstallProgress.COMPLETED -> {
                            if (error == null && !installCompleted) {
                                installCompleted = true
                                onProgress(index, Strings.manager.install.status.success(addonName), false)
                            } else if (error != null) {
                                hasInstallError = true
                                allSuccess = false
                                onProgress(index, Strings.manager.install.status.failed(error.message!!), true)
                            }
                        }
                        AddonManager.InstallProgress.ERROR -> {
                            if (error != null && !hasInstallError) {
                                hasInstallError = true
                                allSuccess = false
                                onProgress(index, Strings.manager.install.status.failed(error.message!!), true)
                            }
                        }
                        AddonManager.InstallProgress.COPYING_FILES -> {
                            onProgress(index, Strings.manager.install.status.copying(addonName), false)
                        }
                        AddonManager.InstallProgress.EXTRACTING_ICON -> {
                            onProgress(index, Strings.manager.install.status.extractingIcon(addonName), false)
                        }
                        AddonManager.InstallProgress.UPDATING_DATABASE -> {
                            onProgress(index, Strings.manager.install.status.updatingDb(addonName), false)
                        }
                        else -> { }
                    }
                }

                // 确保进度更新
                if (!installCompleted && !hasInstallError) {
                    onProgress(index, Strings.manager.install.status.completed(addonName), false)
                }

            } catch (e: Exception) {
                AppLogger.e("Failed to install addon: $addonName", e)
                onProgress(index, Strings.manager.install.status.failed(e.message ?: Strings.error.unknown), true)
                allSuccess = false

            }

            // 短暂延迟，让用户能看到进度
            delay(100.milliseconds)
        }

        if (allSuccess) onProgress(filePaths.size - 1, Strings.manager.install.status.allCompleted, !allSuccess)
        onComplete(allSuccess)
    }
}