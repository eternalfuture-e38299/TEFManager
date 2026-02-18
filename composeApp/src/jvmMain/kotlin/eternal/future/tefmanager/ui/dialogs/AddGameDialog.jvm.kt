package eternal.future.tefmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eternal.future.tefmanager.ui.model.GameItem
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.TrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun AddGameDialog(
    onGameAdded: (GameItem?) -> Unit
) {
    var gameFilePath by remember { mutableStateOf("") }
    var enableTefLoader by remember { mutableStateOf(true) }
    var gameVersion by remember { mutableStateOf("") }
    val showErrorDialog = remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(gameFilePath) {
        focusRequester.requestFocus()

        if (gameFilePath.isNotBlank() && File(gameFilePath).exists()) {
            val info = withContext(Dispatchers.IO) {
                try {
                    TrParser.parse(gameFilePath)
                } catch (_: Exception) {
                    null
                }
            }
            info?.let {
                gameVersion = it.version ?: ""
            }
        }
    }

        Dialog(
            onDismissRequest = {
                onGameAdded(null)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(600.dp)
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                ) {
                    // 标题栏
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.extraLarge.copy(
                            topStart = MaterialTheme.shapes.extraLarge.topStart,
                            topEnd = MaterialTheme.shapes.extraLarge.topEnd
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = "添加游戏",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "选择游戏可执行文件并进行配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 内容区域
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        // 游戏文件路径
                        GameFilePathSection(
                            filePath = gameFilePath,
                            onFilePathChange = {
                                gameFilePath = it
                            },
                            onBrowseClick = {
                                val path = openSystemFilePicker()
                                if (path.isNotBlank()) gameFilePath = path
                            },
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // 选项区域
                        OptionsSection(
                            enableTefLoader = enableTefLoader,
                            onTefLoaderChange = { enableTefLoader = it },
                            gameVersion = gameVersion,
                            onGameVersionChange = { gameVersion = it },
                            modifier = Modifier.padding(bottom = 28.dp)
                        )

                        // 分隔线
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        // 按钮区域
                        ButtonSection(
                            onCancel = {
                                onGameAdded(null)
                            },
                            onConfirm = {
                                if (gameFilePath.isNotBlank()) {
                                    val file = File(gameFilePath)
                                    if (file.exists() && file.isFile) {
                                        try {
                                            val gameItem = GameItem(
                                                filePath = gameFilePath,
                                                version = gameVersion,
                                                versionCode = 0,
                                                hash = calculateFileHash(file)
                                            )
                                            onGameAdded(gameItem)
                                        } catch (e: Exception) {
                                            errorMessage = "创建游戏项目时出错: ${e.message}"
                                            showErrorDialog.value = true
                                        }
                                    } else {
                                        errorMessage = "请选择一个有效的游戏可执行文件"
                                        showErrorDialog.value = true
                                    }
                                } else {
                                    errorMessage = "请选择游戏文件"
                                    showErrorDialog.value = true
                                }
                            },
                            confirmEnabled = gameFilePath.isNotBlank() && File(gameFilePath).exists(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }


    // 错误对话框
    if (showErrorDialog.value) {
        AlertDialog(
            onDismissRequest = { showErrorDialog.value = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "操作错误",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog.value = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确定")
                }
            },
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameFilePathSection(
    filePath: String,
    onFilePathChange: (String) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = remember(filePath) { File(filePath) }
    val isInvalid = filePath.isNotBlank() && (!file.exists() || !file.isFile)

    Column(modifier = modifier) {
        // 标签
        Text(
            text = "游戏文件路径",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 文件选择卡片
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 输入行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 文本字段
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = onFilePathChange,
                        placeholder = {
                            Text(
                                "选择或输入游戏可执行文件路径",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        singleLine = true,
                        isError = isInvalid,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        ),
                        trailingIcon = {
                            if (filePath.isNotBlank()) {
                                if (file.exists() && file.isFile) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "文件存在",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isInvalid) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "文件无效",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )

                    // 浏览按钮
                    FilledTonalButton(
                        onClick = onBrowseClick,
                        modifier = Modifier.wrapContentSize(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "浏览文件",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("浏览")
                    }
                }

                // 文件信息
                if (filePath.isNotBlank() && file.exists() && file.isFile) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("已选文件: ")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(file.name)
                                }
                                append("\n")
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("文件大小: ")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(formatFileSize(file.length()))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                // 错误信息
                if (isInvalid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!file.exists()) "文件不存在，请检查路径"
                            else if (!file.isFile) "请选择文件而不是文件夹"
                            else "文件不可访问，请检查权限",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 帮助文本
        Text(
            text = "选择游戏的可执行文件（.exe）或主程序文件，文件大小通常较大（100MB-1GB）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }
}

@Composable
private fun OptionsSection(
    enableTefLoader: Boolean,
    onTefLoaderChange: (Boolean) -> Unit,
    gameVersion: String,
    onGameVersionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "高级配置",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 添加 TEFLoader 选项
        OptionCard(
            icon = Icons.Outlined.Settings,
            checked = enableTefLoader,
            onCheckedChange = onTefLoaderChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 版本输入区域
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 游戏版本
                VersionInputRow(
                    icon = Icons.Outlined.Code,
                    value = gameVersion,
                    onValueChange = onGameVersionChange,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 图标容器
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                tonalElevation = 1.dp
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .width(24.dp)
                        .height(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文本内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "启用 TEFLoader 支持",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "为游戏启用模组加载器，支持游戏模组和插件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionInputRow(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = "游戏版本",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(可选)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "留空以自动检测版本号，支持标准版本格式",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 28.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ButtonSection(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(end = 12.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }

        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = MaterialTheme.shapes.small,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
                disabledElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Text(
                text = "添加游戏",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private fun calculateFileHash(file: File): String {
    return try {
        val bytes = file.readBytes()
        val md5 = MessageDigest.getInstance("MD5")
        md5.digest(bytes).joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        AppLogger.e("Error calculating hash", e)
        "unknown"
    }
}
private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.2f %s".format(size, units[unitIndex])
}

private fun openSystemFilePicker(): String {
    return try {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "选择游戏可执行文件"
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

        val filter = FileNameExtensionFilter(
            "可执行文件 (*.exe, *.dll)", "exe", "EXE", "dll", "DLL"
        )
        fileChooser.fileFilter = filter

        val desktop = File(System.getProperty("user.home"), "Desktop")
        if (desktop.exists()) {
            fileChooser.currentDirectory = desktop
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}