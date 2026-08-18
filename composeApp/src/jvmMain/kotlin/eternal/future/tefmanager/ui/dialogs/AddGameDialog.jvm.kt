package eternal.future.tefmanager.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.GamePatcher
import eternal.future.tefmanager.utils.TrParser
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.File
import java.security.MessageDigest

// 架构枚举
enum class ArchitectureType(val displayName: String) {
    X86("x86"),
    X86_64("x86_64"),
    ARM64("arm64");

    companion object {
        fun defaultForCurrentPlatform(): ArchitectureType {
            return when {
                Platform.isWindows -> X86
                Platform.isLinux || Platform.isMacOS -> X86_64
                else -> X86_64
            }
        }
    }
}

actual object AddGameDialog {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    actual fun Show(
        onResult: (GameItem?) -> Unit
    ) {
        var gameFilePath by remember { mutableStateOf("") }
        var useCustomTefLoader by remember { mutableStateOf(false) }
        var tefLoaderPath by remember { mutableStateOf("") }
        var gameVersion by remember { mutableStateOf("") }
        var architecture by remember { mutableStateOf(ArchitectureType.defaultForCurrentPlatform()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isProcessing by remember { mutableStateOf(false) }

        val scrollState = rememberScrollState()

        // 文件选择器
        val gameFilePicker = rememberFilePickerLauncher(
            type = FileKitType.File("*.dll | *.exe"),
            onResult = { file -> file?.let { gameFilePath = it.path } }
        )

        val tefLoaderPicker = rememberFilePickerLauncher(
            type = FileKitType.File("*.dll | *.zip"),
            onResult = { file -> file?.let { tefLoaderPath = it.path } }
        )

        // 自动检测版本
        LaunchedEffect(gameFilePath) {
            if (gameFilePath.isNotBlank() && gameVersion.isEmpty()) {
                withContext(Dispatchers.IO) {
                    TrParser.parse(gameFilePath).version?.let { gameVersion = it }
                }
            }
        }

        Dialog(
            onDismissRequest = {
                if (!isProcessing) onResult(null)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.5f),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // 标题
                    Text(
                        text = Strings.home.add.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Strings.home.add.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // 游戏文件
                    OutlinedTextField(
                        value = gameFilePath,
                        onValueChange = { gameFilePath = it },
                        placeholder = { Text(Strings.home.add.gameFile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { gameFilePicker.launch() }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                            }
                        },
                        isError = gameFilePath.isNotBlank() && !File(gameFilePath).exists()
                    )

                    // TEFLoader 切换
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = useCustomTefLoader,
                            onCheckedChange = { useCustomTefLoader = it }
                        )
                        Text(
                            text = Strings.home.add.useCustomTefLoader,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // 自定义TEFLoader路径
                    AnimatedVisibility(visible = useCustomTefLoader) {
                        OutlinedTextField(
                            value = tefLoaderPath,
                            onValueChange = { tefLoaderPath = it },
                            placeholder = { Text(Strings.home.add.tefLoaderPath) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { tefLoaderPicker.launch() }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                                }
                            },
                            isError = tefLoaderPath.isNotBlank() && !File(tefLoaderPath).exists()
                        )
                    }

                    // 版本号
                    OutlinedTextField(
                        value = gameVersion,
                        onValueChange = { gameVersion = it },
                        placeholder = { Text(Strings.home.add.version) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )

                    // 架构选择
                    Text(
                        text = Strings.home.add.architecture,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ArchitectureType.entries.forEach { arch ->
                            FilterChip(
                                selected = architecture == arch,
                                onClick = { architecture = arch },
                                label = { Text(arch.displayName) },
                                modifier = Modifier.weight(1f).padding(4.dp)
                            )
                        }
                    }

                    // 错误信息
                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { if (!isProcessing) onResult(null) },
                            enabled = !isProcessing
                        ) {
                            Text(Strings.cancel)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val gameFile = File(gameFilePath)
                                if (!gameFile.exists() || !gameFile.isFile) {
                                    errorMessage = Strings.home.add.error.invalidGameFile
                                    return@Button
                                }

                                if (useCustomTefLoader) {
                                    val tefFile = File(tefLoaderPath)
                                    if (!tefFile.exists() || !tefFile.isFile) {
                                        errorMessage = Strings.home.add.error.invalidTefLoader
                                        return@Button
                                    }
                                }

                                isProcessing = true
                                errorMessage = null

                                try {
                                    val versionCode = convertVersionToCode(gameVersion)

                                    // 执行补丁并获取生成的二进制路径
                                    val binaryPath = GamePatcher.patchViaDotNetGrafting(
                                            gameFilePath.toPath(),
                                            tefLoaderPath,
                                            architecture.toString().lowercase()
                                        ).toString()

                                    if (binaryPath.isEmpty()) {
                                        errorMessage = Strings.home.add.error.invalidTefLoader
                                        return@Button
                                    }

                                    val gameItem = GameItem(
                                        apkPackName = "",
                                        filePath = gameFilePath,
                                        tefloaderPath = binaryPath,  // 保存生成的二进制路径
                                        version = gameVersion,
                                        versionCode = versionCode,
                                        architecture = architecture.toString(),
                                        hash = calculateFileHash(gameFile)
                                    )

                                    onResult(gameItem)
                                } catch (e: Exception) {
                                    errorMessage = Strings.error.title(e.message ?: Strings.error.unknown)
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing &&
                                    gameFilePath.isNotBlank() &&
                                    File(gameFilePath).exists() &&
                                    (!useCustomTefLoader ||
                                            (tefLoaderPath.isNotBlank() && File(tefLoaderPath).exists()))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isProcessing) Strings.home.add.patching else Strings.home.game.add)
                        }
                    }
                }
            }
        }
    }

    // 将版本号转换为版本代码
    private fun convertVersionToCode(version: String): Int {
        if (version.isEmpty()) return 0

        return try {
            val parts = version.split(".")
            var code = 0
            for ((index, part) in parts.withIndex()) {
                val num = part.toIntOrNull() ?: 0
                code = code or (num shl ((3 - index) * 8))
            }
            code
        } catch (e: Exception) {
            AppLogger.e("Error converting version to code: $version", e)
            0
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
}