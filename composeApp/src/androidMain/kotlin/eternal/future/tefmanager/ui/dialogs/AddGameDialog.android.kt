package eternal.future.tefmanager.ui.dialogs

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.model.GameItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.Patcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
actual object AddGameDialog {
    @Composable
    actual fun Show(onResult: (GameItem?) -> Unit) {
        val isApi28OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        val showDialog = remember { mutableStateOf(true) }
        var selectedApkPath by remember { mutableStateOf<String?>(null) }
        var selectedOption by remember { mutableStateOf(
            if (isApi28OrAbove) Patcher.PatchOption.APP_COMPONENT_FACTORY
            else Patcher.PatchOption.APPLICATION
        ) }
        var isPatching by remember { mutableStateOf(false) }
        val patchProgress =  remember { mutableStateOf<Patcher.PatchProgress?>(null) }
        val patchError = remember { mutableStateOf<String?>(null) }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // 使用外部缓存目录
        val externalCacheDir = remember {
            context.externalCacheDir ?: context.cacheDir
        }

        // 文件选择器
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                uri?.let { fileUri ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                // 在外部缓存中保存文件
                                val tempFile = File(
                                    externalCacheDir,
                                    "selected_${System.currentTimeMillis()}.apk"
                                )
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                                selectedApkPath = tempFile.absolutePath
                                AppLogger.d("Selected APK saved to: ${tempFile.absolutePath}")
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error reading selected file", e)
                            withContext(Dispatchers.Main) {
                                patchError.value = e.toString()
                            }
                        }
                    }
                }
            }
        )

        // 保存文件
        val saveFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
            onResult = { uri ->
                uri?.let { outputUri ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // 查找修补后的文件
                            val patchedApkFile = File(externalCacheDir, "patched.apk")
                            if (patchedApkFile.exists()) {
                                context.contentResolver.openOutputStream(outputUri)
                                    ?.use { outputStream ->
                                        FileInputStream(patchedApkFile).use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }

                                // 清理所有临时文件
                                cleanupExternalCacheFiles(externalCacheDir)

                                withContext(Dispatchers.Main) {
                                    isPatching = false
                                    showDialog.value = false
                                    onResult(null)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    patchError.value = Strings.home.patch.error.cantFoundFile
                                    isPatching = false
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error saving patched APK", e)
                            withContext(Dispatchers.Main) {
                                patchError.value = e.toString()
                                isPatching = false
                            }
                        }
                    }
                } ?: run {
                    // 用户取消保存
                    isPatching = false
                }
            }
        )

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    // 清理临时文件
                    cleanupExternalCacheFiles(externalCacheDir)
                    showDialog.value = false
                    onResult(null)
                },
                title = {
                    Text(text = if (isPatching) Strings.home.patching else Strings.home.patch.title)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isPatching) {
                            // 修补进度显示
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (patchProgress.value != null) {
                                    Text(
                                        text = patchProgress.value.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = {
                                        when (patchProgress.value) {
                                            null -> 0f
                                            Patcher.PatchProgress.PREPARING -> 0.1f
                                            Patcher.PatchProgress.OPENING_APK -> 0.2f
                                            Patcher.PatchProgress.UNZIP_DEX -> 0.3f
                                            Patcher.PatchProgress.READING_MANIFEST -> 0.4f
                                            Patcher.PatchProgress.MODIFYING_APPLICATION,
                                            Patcher.PatchProgress.MODIFYING_APP_COMPONENT_FACTORY -> 0.5f

                                            Patcher.PatchProgress.ADDING_METADATA -> 0.55f
                                            Patcher.PatchProgress.ADDING_PROVIDER -> 0.6f
                                            Patcher.PatchProgress.REPLACING_MANIFEST -> 0.65f
                                            Patcher.PatchProgress.ADDING_DEX -> 0.7f
                                            Patcher.PatchProgress.REPACKAGING_APK -> 0.8f
                                            Patcher.PatchProgress.SIGNING_APK -> 0.9f
                                            Patcher.PatchProgress.COMPLETED -> 1f
                                            Patcher.PatchProgress.FAILED -> 0f
                                        }
                                    }
                                )

                                if (patchError.value != null) {
                                    Text(
                                        text = Strings.error.title(patchError),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else {
                            // 文件选择和选项设置
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedApkPath?.let {
                                            File(it).name.takeIf { name -> name.length <= 20 }
                                                ?: "...${File(it).name.takeLast(20)}"
                                        } ?: Strings.home.patch.selectedEmpty,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (selectedApkPath != null) {
                                        val fileSize =
                                            File(selectedApkPath!!).length() / 1024 / 1024
                                        Text(
                                            text = "${fileSize}MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        filePickerLauncher.launch("application/vnd.android.package-archive")
                                    }
                                ) {
                                    Text(text = Strings.home.patch.selected)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = Strings.home.patch.entrance,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedOption == Patcher.PatchOption.APPLICATION,
                                        onClick = {
                                            selectedOption = Patcher.PatchOption.APPLICATION
                                        }
                                    )
                                    Text(
                                        text = "Application",
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedOption == Patcher.PatchOption.APP_COMPONENT_FACTORY,
                                        onClick = {
                                            selectedOption =
                                                Patcher.PatchOption.APP_COMPONENT_FACTORY
                                        },
                                        enabled = isApi28OrAbove
                                    )
                                    Column(modifier = Modifier.padding(start = 4.dp)) {
                                        Text(text = "AppComponentFactory (API 28+)")
                                        if (!isApi28OrAbove) {
                                            Text(
                                                text = Strings.home.patch.entranceWorng(Build.VERSION.SDK_INT),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        } else {
                                            Text(
                                                text = Strings.home.patch.entranceRecommend,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedApkPath == null) {
                                Text(
                                    text = Strings.home.patch.selectedEmpty,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isPatching) {
                        // 修补中显示加载状态
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = Strings.home.patching)
                        }
                    } else {
                        // 修补按钮
                        Button(
                            onClick = {
                                selectedApkPath?.let { apkPath ->
                                    isPatching = true
                                    patchError.value = null
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            // 复制APK到外部缓存
                                            val tempApkFile = File(externalCacheDir, "patch.apk")

                                            FileInputStream(apkPath).use { input ->
                                                FileOutputStream(tempApkFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }

                                            AppLogger.i("Copied APK to temp file: ${tempApkFile.absolutePath}")

                                            // 开始修补
                                            Patcher.patch(
                                                tempApkFile.absolutePath,
                                                selectedOption
                                            ) { progress, error ->
                                                patchProgress.value = progress
                                                patchError.value = error

                                                if (progress == Patcher.PatchProgress.COMPLETED) {
                                                    // 修补完成，重命名临时文件
                                                    val patchedFile = File(externalCacheDir, "patched.apk")
                                                    if (tempApkFile.renameTo(patchedFile)) {
                                                        // 弹出保存对话框
                                                        coroutineScope.launch {
                                                            withContext(Dispatchers.Main) {
                                                                val originalFileName =
                                                                    File(apkPath).nameWithoutExtension
                                                                val timestamp =
                                                                    System.currentTimeMillis()
                                                                val suggestedFileName =
                                                                    "${originalFileName}_patched_${timestamp}.apk"
                                                                saveFileLauncher.launch(
                                                                    suggestedFileName
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        patchError.value = Strings.home.patch.error.cantRename
                                                        patchProgress.value = Patcher.PatchProgress.FAILED
                                                        isPatching = false
                                                    }
                                                } else if (progress == Patcher.PatchProgress.FAILED) {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            isPatching = false
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            AppLogger.e("Error during patching process", e)
                                            patchError.value = e.toString()
                                            patchProgress.value = Patcher.PatchProgress.FAILED
                                            withContext(Dispatchers.Main) {
                                                isPatching = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = selectedApkPath != null && (selectedOption == Patcher.PatchOption.APPLICATION || isApi28OrAbove)
                        ) {
                            Text(text = Strings.home.patch.start)
                        }
                    }
                },
                dismissButton = {
                    if (!isPatching) {
                        TextButton(
                            onClick = {
                                // 清理临时文件
                                cleanupExternalCacheFiles(externalCacheDir)
                                showDialog.value = false
                                onResult(null)
                            }
                        ) {
                            Text(text = Strings.cancel)
                        }
                    }
                }
            )
        }
    }

    private fun cleanupExternalCacheFiles(externalCacheDir: File) {
        try {
            externalCacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("selected_") ||
                    file.name == "patch.apk" ||
                    file.name == "patched.apk") {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error cleaning up external cache files", e)
        }
    }
}