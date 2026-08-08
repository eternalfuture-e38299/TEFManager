package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import no.synth.kmpzip.okio.ZipFile
import no.synth.kmpzip.okio.asSource
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipFile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File

/*******************************************************************************
 * TEFManager - GamePatcher
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
 * Created: 2026/2/17
 *******************************************************************************/

object GamePatcher {

    // ==================== Constants ====================
    private const val DEFAULT_LOADER_FILENAME = "tefloader.zip"
    private const val RUNTIME_WINDOWS = "net452/"
    private const val RUNTIME_UNIX = "net472/"
    private const val BINARY_PREFIX = "Terraria.bin."
    private const val BINARY_TARGET_PREFIX = "tefloader.bin."

    private val files = FileSystem.SYSTEM

    // ==================== Public API ====================

    /**
     * 通过 .NET 嫁接方式打补丁
     * @param filePath 目标文件路径
     * @param tefLoader 自定义 TEF 加载器路径（空字符串表示使用默认）
     * @param architecture 目标架构（默认为当前系统架构）
     */
    fun patchViaDotNetGrafting(
        filePath: Path,
        tefLoader: String = "",
        architecture: String = Platform.getArchitecture()
    ) {
        val targetDir = filePath.parent
            ?: return AppLogger.e("Failed to get parent directory for: $filePath")

        val loaderPath = resolveLoaderPath(tefLoader)
        val useCustomLoader = tefLoader.isNotEmpty()
        val runtimePrefix = determineRuntimePrefix(useCustomLoader)

        logGraftingStart(targetDir, loaderPath, architecture, runtimePrefix)

        val zipFile = openZipFile(loaderPath)
        extractLoaderFiles(zipFile, targetDir, runtimePrefix)

        if (!Platform.isWindows) {
            processBinaryFiles(targetDir)
        }
    }

    // ==================== Private Methods ====================

    /**
     * 解析加载器路径
     */
    private fun resolveLoaderPath(customLoader: String): Path {
        return if (customLoader.isNotEmpty()) {
            customLoader.toPath()
        } else {
            Platform.getData("tefkernel") / DEFAULT_LOADER_FILENAME
        }
    }

    /**
     * 确定运行时前缀
     */
    private fun determineRuntimePrefix(useCustomLoader: Boolean): String {
        return when {
            useCustomLoader -> ""
            Platform.isWindows -> RUNTIME_WINDOWS
            else -> RUNTIME_UNIX
        }
    }

    /**
     * 打开 ZIP 文件
     */
    private fun openZipFile(path: Path): ZipFile {
        return ZipFile(FileSystem.SYSTEM.openReadOnly(path))
    }

    /**
     * 提取加载器文件
     */
    private fun extractLoaderFiles(
        zipFile: ZipFile,
        targetDir: Path,
        runtimePrefix: String
    ) {
        zipFile.entries.forEach { entry ->
            extractSingleEntry(zipFile, entry, targetDir, runtimePrefix)
                .onSuccess { targetFile ->
                    AppLogger.d("Extracted: ${entry.name} -> $targetFile")
                }
                .onFailure { e ->
                    AppLogger.e("Failed to extract ${entry.name}", e)
                }
        }
    }

    /**
     * 提取单个 ZIP 条目
     */
    private fun extractSingleEntry(
        zipFile: ZipFile,
        entry: ZipEntry,
        targetDir: Path,
        runtimePrefix: String
    ): Result<Path> = runCatching {
        val entryNameWithoutPrefix = entry.name.removePrefix(runtimePrefix)
        val targetFile = targetDir.resolve(entryNameWithoutPrefix)

        // 写入文件

        files.sink(targetFile).buffer().use { sink ->
            zipFile.getInputStream(entry).asSource().use { source ->
                sink.writeAll(source)
            }
        }

        // 设置执行权限（非 Windows）
        setExecutablePermission(targetFile)

        targetFile
    }

    /**
     * 处理二进制文件（仅非 Windows 平台）
     */
    private fun processBinaryFiles(targetDir: Path) {
        AppLogger.i("Processing binary files for non-Windows platform")

        val binFiles = files.list(targetDir)
            .filter { it.name.startsWith(BINARY_PREFIX) }

        AppLogger.d("Found ${binFiles.size} binary files to process")

        binFiles.forEach { sourceFile ->
            processSingleBinaryFile(sourceFile, targetDir)
        }
    }

    /**
     * 处理单个二进制文件
     */
    private fun processSingleBinaryFile(sourceFile: Path, targetDir: Path) {
        runCatching {
            val architecture = sourceFile.name.removePrefix(BINARY_PREFIX)
            val targetFile = targetDir.resolve("${BINARY_TARGET_PREFIX}$architecture")

            files.copy(sourceFile, targetFile)
            setExecutablePermission(targetFile)

            AppLogger.d("Copied binary: ${sourceFile.name} -> ${targetFile.name}")
        }.onFailure { e ->
            AppLogger.e("Failed to copy binary ${sourceFile.name}", e)
        }
    }

    /**
     * 为文件设置可执行权限（非 Windows 系统）
     */
    private fun setExecutablePermission(filePath: Path) {
        if (Platform.isWindows) return

        runCatching {
            val file = File(filePath.toString())
            if (file.exists() && file.setExecutable(true)) {
                AppLogger.d("Set executable permission for: ${filePath.name}")
            } else {
                AppLogger.w("Failed to set executable permission for: ${filePath.name}")
            }
        }.onFailure { e ->
            when (e) {
                is SecurityException -> AppLogger.e(
                    "Security exception when setting executable permission for ${filePath.name}", e
                )
                else -> AppLogger.e(
                    "Failed to set executable permission for ${filePath.name}", e
                )
            }
        }
    }

    // ==================== Logging Helpers ====================

    private fun logGraftingStart(
        targetDir: Path,
        loaderPath: Path,
        architecture: String,
        runtimePrefix: String
    ) {
        AppLogger.i("Starting .NET grafting patch process")
        AppLogger.d("Target directory: $targetDir")
        AppLogger.d("Loader path: $loaderPath")
        AppLogger.d("Architecture: $architecture")
        AppLogger.i("Extracting .NET runtime: $runtimePrefix")
    }
}