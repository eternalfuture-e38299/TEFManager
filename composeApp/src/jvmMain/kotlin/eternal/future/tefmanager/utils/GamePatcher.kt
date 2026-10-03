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
     * @return 生成的 tefloader 二进制文件路径，如果失败返回 null
     */
    fun patchViaDotNetGrafting(
        filePath: Path,
        tefLoader: String = "",
        architecture: String = Platform.getArchitecture()
    ): Path? {
        val targetDir = filePath.parent
            ?: run {
                AppLogger.e("Failed to get parent directory for: $filePath")
                return null
            }

        val loaderPath = resolveLoaderPath(tefLoader)
        val useCustomLoader = tefLoader.isNotEmpty()
        val runtimePrefix = determineRuntimePrefix(useCustomLoader, architecture)

        logGraftingStart(targetDir, loaderPath, architecture, runtimePrefix)

        val zipFile = openZipFile(loaderPath)
        extractLoaderFiles(zipFile, targetDir, runtimePrefix, useCustomLoader)

        // 返回生成的二进制文件路径
        return if (!Platform.isWindows) {
            processBinaryFiles(targetDir, architecture)
        } else {
            // Windows 平台返回 null 或返回主 exe 路径
            null
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
     * 自定义 loader 直接解压全部，不使用前缀过滤
     */
    private fun determineRuntimePrefix(useCustomLoader: Boolean, architecture: String): String {
        return if (useCustomLoader) {
            ""  // 自定义 loader 不限制目录
        } else if (Platform.isWindows && architecture == "X86") {
            RUNTIME_WINDOWS
        } else {
            RUNTIME_UNIX
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
        runtimePrefix: String,
        useCustomLoader: Boolean
    ) {
        // 确保目标目录存在
        if (!files.exists(targetDir)) {
            files.createDirectories(targetDir)
            AppLogger.d("Created target directory: $targetDir")
        }

        zipFile.entries.forEach { entry ->
            // 跳过目录条目
            if (entry.isDirectory) {
                AppLogger.d("Skipping directory entry: ${entry.name}")
                return@forEach
            }

            // 如果是自定义 loader，提取所有文件
            // 如果不是，只提取匹配前缀的文件
            if (!useCustomLoader && !entry.name.startsWith(runtimePrefix)) {
                return@forEach
            }

            extractSingleEntry(zipFile, entry, targetDir, runtimePrefix, useCustomLoader)
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
        runtimePrefix: String,
        useCustomLoader: Boolean
    ): Result<Path> = runCatching {
        // 计算目标文件名
        val entryNameWithoutPrefix = if (useCustomLoader) {
            // 自定义 loader：直接使用原始名称
            entry.name
        } else {
            // 非自定义：移除前缀
            entry.name.removePrefix(runtimePrefix)
        }

        // 如果去掉前缀后为空，跳过
        if (entryNameWithoutPrefix.isEmpty()) {
            AppLogger.d("Skipping empty entry name: ${entry.name}")
            return@runCatching targetDir
        }

        val targetFile = targetDir.resolve(entryNameWithoutPrefix)

        // 重要：确保目标文件的父目录存在
        val parentDir = targetFile.parent
        if (parentDir != null && !files.exists(parentDir)) {
            files.createDirectories(parentDir)
            AppLogger.d("Created parent directory: $parentDir")
        }

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
     * @return 生成的 tefloader 二进制文件路径
     */
    private fun processBinaryFiles(targetDir: Path, architecture: String): Path? {
        AppLogger.i("Processing binary files for non-Windows platform")

        // 确保目录存在
        if (!files.exists(targetDir)) {
            AppLogger.w("Target directory does not exist: $targetDir")
            return null
        }

        val binFiles = files.list(targetDir)
            .filter { it.name.startsWith(BINARY_PREFIX) }

        AppLogger.d("Found ${binFiles.size} binary files to process")

        var resultPath: Path? = null

        binFiles.forEach { sourceFile ->
            processSingleBinaryFile(sourceFile, targetDir)?.let {
                // 如果匹配当前架构，保存路径
                val fileArch = sourceFile.name.removePrefix(BINARY_PREFIX).lowercase()
                if (fileArch == architecture) {
                    resultPath = it
                    AppLogger.d("Selected binary for architecture $architecture: $it")
                }
            }
        }

        return resultPath
    }

    /**
     * 处理单个二进制文件
     * @return 生成的二进制文件路径
     */
    private fun processSingleBinaryFile(sourceFile: Path, targetDir: Path): Path? {
        return runCatching {
            val architecture = sourceFile.name.removePrefix(BINARY_PREFIX)
            val targetFile = targetDir.resolve("${BINARY_TARGET_PREFIX}$architecture")

            // 确保目标文件可以被覆盖
            if (files.exists(targetFile)) {
                files.delete(targetFile)
                AppLogger.d("Removed existing binary: $targetFile")
            }

            files.copy(sourceFile, targetFile)
            setExecutablePermission(targetFile)

            AppLogger.d("Copied binary: ${sourceFile.name} -> ${targetFile.name}")
            targetFile
        }.onFailure { e ->
            AppLogger.e("Failed to copy binary ${sourceFile.name}", e)
        }.getOrNull()
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
        AppLogger.i("Extracting .NET runtime: ${runtimePrefix.ifEmpty { "ALL (custom loader)" }}")
    }
}