package eternal.future.tefmanager.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import eternal.future.tefmanager.utils.AddonManager
import eternal.future.tefmanager.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip

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
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = filePaths.size
    val scope = rememberCoroutineScope()
    var isInstalling by remember { mutableStateOf(false) }
    var currentAddonName by remember { mutableStateOf<String?>(null) }
    var currentAddonType by remember { mutableStateOf<String?>(null) }
    var currentProgress by remember { mutableStateOf<AddonManager.InstallProgress?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var successCount by remember { mutableIntStateOf(0) }
    var failCount by remember { mutableIntStateOf(0) }
    var skipCount by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var installLogs by remember { mutableStateOf<List<InstallLogEntry>>(emptyList()) }
    val lazyListState = rememberLazyListState()

    val addonNames = remember(filePaths) {
        filePaths.map { it.name }
    }

    // 自动滚动到最后一条日志
    LaunchedEffect(installLogs.size) {
        if (installLogs.isNotEmpty()) {
            lazyListState.animateScrollToItem(installLogs.size - 1)
        }
    }

    Dialog(onDismissRequest = { if (!isInstalling) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 标题区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isComplete) "安装完成" else "正在安装附加组件",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${currentStep + 1}/$totalSteps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { if (!isInstalling) onDismiss() },
                        enabled = !isInstalling
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { if (totalSteps > 0) (currentStep.toFloat() + 0.5f) / totalSteps else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap)

                Spacer(modifier = Modifier.height(24.dp))

                // 当前安装信息
                if (currentAddonName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (currentAddonType?.lowercase()) {
                                    "plugin" -> Icons.Default.Extension
                                    "module" -> Icons.Default.Widgets
                                    "modloader" -> Icons.Default.Build
                                    "mod" -> Icons.Default.Animation
                                    else -> Icons.Default.QuestionMark
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = currentAddonName ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (currentAddonType != null) {
                                        Text(
                                            text = "类型: $currentAddonType",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (currentProgress != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = getProgressText(currentProgress!!),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 安装日志
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = CenterVertically
                    ) {
                        Text(
                            text = "安装日志",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " $successCount",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " $skipCount",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " $failCount",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxSize(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (installLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "等待安装开始...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                state = lazyListState,
                                verticalArrangement = Arrangement.Top
                            ) {
                                items(installLogs) { log ->
                                    InstallLogItem(log = log)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isComplete) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("完成")
                        }
                    } else if (!isInstalling) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("取消")
                        }

                        Button(
                            onClick = {
                                isInstalling = true
                                scope.launch {
                                    startInstallation(
                                        filePaths = filePaths,
                                        scope = scope,
                                        onProgress = { progress, e ->
                                            currentProgress = progress
                                            if (e != null) {
                                                error = e
                                            }
                                        },
                                        onStepStart = { step, addonName, addonType ->
                                            currentStep = step
                                            currentAddonName = addonName
                                            currentAddonType = addonType
                                        },
                                        onLog = { log ->
                                            installLogs = installLogs + log
                                        },
                                        onSuccess = { successCount++ },
                                        onSkip = { skipCount++ },
                                        onFail = { failCount++ },
                                        onComplete = {
                                            isComplete = true
                                            isInstalling = false
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始安装")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                // 这里可以添加取消安装的逻辑
                            },
                            enabled = false
                        ) {
                            Text("正在安装...")
                        }
                    }
                }
            }
        }
    }
}

private data class InstallLogEntry(
    val addonName: String,
    val message: String,
    val type: LogType,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

private enum class LogType {
    INFO, SUCCESS, ERROR, SKIP
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstallLogItem(log: InstallLogEntry) {
    val backgroundColor = when (log.type) {
        LogType.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
    }

    val borderColor = when (log.type) {
        LogType.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        LogType.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        LogType.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (log.type) {
                LogType.INFO -> Icons.Default.Info
                LogType.SUCCESS -> Icons.Default.CheckCircle
                LogType.ERROR -> Icons.Default.Error
                LogType.SKIP -> Icons.Default.SkipNext
            }

            val iconColor = when (log.type) {
                LogType.INFO -> MaterialTheme.colorScheme.primary
                LogType.SUCCESS -> MaterialTheme.colorScheme.primary
                LogType.ERROR -> MaterialTheme.colorScheme.error
                LogType.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            val textColor = when (log.type) {
                LogType.INFO -> MaterialTheme.colorScheme.onSurface
                LogType.SUCCESS -> MaterialTheme.colorScheme.primary
                LogType.ERROR -> MaterialTheme.colorScheme.error
                LogType.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.addonName,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = formatTime(log.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    return buildString {
        // 小时
        val hour = localDateTime.hour
        if (hour < 10) {
            append('0')
        }
        append(hour)
        append(':')

        // 分钟
        val minute = localDateTime.minute
        if (minute < 10) {
            append('0')
        }
        append(minute)
        append(':')

        // 秒
        val second = localDateTime.second
        if (second < 10) {
            append('0')
        }
        append(second)
    }
}

private fun getProgressText(progress: AddonManager.InstallProgress): String {
    return when (progress) {
        AddonManager.InstallProgress.STARTING -> "正在开始"
        AddonManager.InstallProgress.OPENING_PACKAGE -> "正在打开包文件"
        AddonManager.InstallProgress.READING_MANIFEST -> "正在读取清单文件"
        AddonManager.InstallProgress.PARSING_METADATA -> "正在解析元数据"
        AddonManager.InstallProgress.CHECKING_EXISTING -> "正在检查现有版本"
        AddonManager.InstallProgress.COPYING_FILES -> "正在复制文件"
        AddonManager.InstallProgress.EXTRACTING_ICON -> "正在提取图标"
        AddonManager.InstallProgress.UPDATING_DATABASE -> "正在更新数据库"
        AddonManager.InstallProgress.INSTALLING_DEPENDENCIES -> "正在安装依赖"
        AddonManager.InstallProgress.PROCESSING_DEPENDENCY -> "正在处理依赖项"
        AddonManager.InstallProgress.FINISHING -> "正在完成安装"
        AddonManager.InstallProgress.COMPLETED -> "安装完成"
        AddonManager.InstallProgress.ERROR -> "安装失败"
    }
}

private suspend fun startInstallation(
    filePaths: List<Path>,
    scope: CoroutineScope,
    onProgress: (AddonManager.InstallProgress, Throwable?) -> Unit,
    onStepStart: (Int, String, String?) -> Unit,
    onLog: (InstallLogEntry) -> Unit,
    onSuccess: () -> Unit,
    onSkip: () -> Unit,
    onFail: () -> Unit,
    onComplete: () -> Unit
) {
    withContext(Dispatchers.IO) {
        for ((index, filePath) in filePaths.withIndex()) {
            val addonName = filePath.name
            var addonType: String? = null

            try {
                // 开始安装当前文件
                onStepStart(index, addonName, null)
                onLog(InstallLogEntry(addonName, "开始安装", LogType.INFO))

                // 先读取清单文件获取类型
                val fileSystem = okio.FileSystem.SYSTEM
                if (fileSystem.exists(filePath)) {
                    val zip = fileSystem.openZip(filePath)
                    try {
                        if (zip.exists("Manifest.json".toPath())) {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            val manifest = json.parseToJsonElement(
                                zip.source("Manifest.json".toPath()).buffer().readUtf8()
                            ).jsonObject
                            addonType = manifest["type"]?.jsonPrimitive?.content
                            onStepStart(index, addonName, addonType)
                        }
                    } finally {
                        zip.close()
                    }
                }

                var installCompleted = false

                AddonManager.installOrUpdate(filePath) { progress, error ->
                    onProgress(progress, error)

                    when (progress) {
                        AddonManager.InstallProgress.COMPLETED -> {
                            if (error == null) {
                                onLog(InstallLogEntry(addonName, "安装成功", LogType.SUCCESS))
                                onSuccess()
                                installCompleted = true
                            } else {
                                onLog(InstallLogEntry(addonName, "安装失败: ${error.message}", LogType.ERROR))
                                onFail()
                                installCompleted = true
                            }
                        }
                        AddonManager.InstallProgress.ERROR -> {
                            if (error != null) {
                                onLog(InstallLogEntry(addonName, "安装失败: ${error.message}", LogType.ERROR))
                                onFail()
                                installCompleted = true
                            }
                        }
                        else -> {
                            // 其他进度更新
                        }
                    }
                }

                // 如果安装没有完成（比如回调没有被调用），标记为完成
                if (!installCompleted) {
                    onLog(InstallLogEntry(addonName, "安装完成", LogType.SUCCESS))
                    onSuccess()
                }

            } catch (e: Exception) {
                AppLogger.e("Failed to install addon: $addonName", e)
                onLog(InstallLogEntry(addonName, "安装失败: ${e.message ?: "未知错误"}", LogType.ERROR))
                onFail()
            }

            // 短暂延迟，让用户能看到进度
            kotlinx.coroutines.delay(100)
        }

        onComplete()
    }
}