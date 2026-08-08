package eternal.future.tefmanager.utils.addon

import androidx.compose.runtime.mutableStateMapOf
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.ModLoaderItem
import eternal.future.tefmanager.utils.AppLogger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.synth.kmpzip.okio.ZipFile
import no.synth.kmpzip.okio.asSource
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipFile
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.random.Random
import kotlin.time.Clock

/*******************************************************************************
 * TEFManager - AddonManager
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
 * Created: 2026/8/3
 *******************************************************************************/

object AddonManager {

    /**
     * 安装进度状态枚举
     * 按实际安装流程顺序排列
     */
    enum class InstallProgress {
        // 开始阶段
        STARTING,
        OPENING_PACKAGE,

        // 解析阶段
        READING_MANIFEST,
        PARSING_METADATA,

        // 文件操作阶段
        COPYING_FILES,
        EXTRACTING_ICON,

        // 数据库操作阶段
        UPDATING_DATABASE,

        // 依赖处理阶段
        INSTALLING_DEPENDENCIES,
        PROCESSING_DEPENDENCY,

        // 完成阶段
        FINISHING,
        COMPLETED,

        // 错误状态
        ERROR
    }

    /**
     * 进度回调函数类型
     */
    typealias ProgressCallback = (progress: InstallProgress, error: Throwable?) -> Unit

    private val fileSystem = FileSystem.SYSTEM
    private val logger = AppLogger
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val modManagersList = mutableStateMapOf<String, ModManager>()

    fun installKernel(filePath: Path) {
        val outDir = Platform.getData("tefkernel")
        val zip = ZipFile(fileSystem.openReadOnly(filePath))

        val files = if (Platform.isAndroid) listOf(
            "libtefkernel.android.arm64-v8a.so",
            "libtefkernel.android.armeabi-v7a.so"
        )
        else if(Platform.isWindows) listOf(
            "libtefkernel.windows.x64.dll",
            "libtefkernel.windows.x86.dll"
        ) else if (Platform.isLinux)
            listOf(
                "libtefkernel.linux.x86.so",
                "libtefkernel.linux.x64.so"
            ) else listOf()

        val infoString = zip.getInputStream("info.json").asSource().buffer().readUtf8()
        val version = json.parseToJsonElement(infoString).jsonObject["version"]?.jsonPrimitive?.content!!

        files.forEach { name ->
            val entry = zip.getEntry(name)
            if (entry != null) {
                zip.getInputStream(entry).asSource().buffer().use { input ->
                    fileSystem
                        .sink(outDir / name)
                        .buffer()
                        .use { output ->
                            output.writeAll(
                                input
                            )
                        }
                }
            }
        }

        ConfigurationState.kernelVersion = version
    }

    /**
     * 安装或更新附加包
     * @param filePath 包文件路径
     * @param progressCallback 进度回调函数，用于报告安装进度和错误
     */
    suspend fun install(filePath: Path, progressCallback: ProgressCallback? = null) {
        logger.d("Starting install for file: $filePath")
        progressCallback?.invoke(InstallProgress.STARTING, null)

        val zip = ZipFile(fileSystem.openReadOnly(filePath))
        var currentError: Throwable? = null

        try {
            progressCallback?.invoke(InstallProgress.OPENING_PACKAGE, null)

            progressCallback?.invoke(InstallProgress.READING_MANIFEST, null)
            val manifestEntry = zip.getEntry("Manifest.json")
            if (manifestEntry == null) {
                val error = IllegalArgumentException("No Manifest.json found in zip file: $filePath")
                logger.w(error.message ?: "No manifest found")
                progressCallback?.invoke(InstallProgress.ERROR, error)
                return
            }

            val manifest = json.parseToJsonElement(
                zip.getInputStream(manifestEntry).asSource().buffer().readUtf8()
            ).jsonObject
            val type = manifest["type"]?.jsonPrimitive?.content?.lowercase()

            logger.d("Detected addon type: $type")

            when (type) {
                "plugin" -> {
                    logger.i("Installing/updating plugin")
                    progressCallback?.invoke(InstallProgress.STARTING, null)
                    try {
                        PluginManager.install(zip, manifest, progressCallback)
                    } catch (e: Exception) {
                        currentError = e
                        logger.e("Plugin installation failed", e)
                        progressCallback?.invoke(InstallProgress.ERROR, e)
                        throw e
                    }
                }
                "module" -> {
                    logger.i("Installing/updating module")
                    progressCallback?.invoke(InstallProgress.STARTING, null)
                    try {
                        ModuleManager.install(zip, manifest, progressCallback)
                    } catch (e: Exception) {
                        currentError = e
                        logger.e("Module installation failed", e)
                        progressCallback?.invoke(InstallProgress.ERROR, e)
                        throw e
                    }
                }
                "modloader" -> {
                    logger.i("Installing/updating modloader")
                    progressCallback?.invoke(InstallProgress.STARTING, null)
                    try {
                        ModLoaderManager.install(zip, manifest, progressCallback)
                    } catch (e: Exception) {
                        currentError = e
                        logger.e("ModLoader installation failed", e)
                        progressCallback?.invoke(InstallProgress.ERROR, e)
                        throw e
                    }
                }
                "mod" -> {
                    logger.i("Installing/updating mod")
                    progressCallback?.invoke(InstallProgress.STARTING, null)
                    try {
                        installMod(zip, manifest, progressCallback)
                        // installOrUpdateMod(zip, manifest, progressCallback)
                    } catch (e: Exception) {
                        currentError = e
                        logger.e("Mod installation failed", e)
                        progressCallback?.invoke(InstallProgress.ERROR, e)
                        throw e
                    }
                }
                else -> {
                    val error = IllegalArgumentException("Unknown addon type: $type")
                    logger.w("Unknown addon type: $type")
                    progressCallback?.invoke(InstallProgress.ERROR, error)
                    return
                }
            }

            if (currentError == null) {
                progressCallback?.invoke(InstallProgress.FINISHING, null)
                logger.i("Successfully processed addon: $filePath")
                progressCallback?.invoke(InstallProgress.COMPLETED, null)
            }
        } catch (e: Exception) {
            if (currentError == null) {
                logger.e("Error processing addon: $filePath", e)
                progressCallback?.invoke(InstallProgress.ERROR, e)
            }
            throw e
        } finally {
            zip.close()
        }
    }

    private suspend fun installMod(
        zip: ZipFile,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null) {

        logger.d("installOrUpdateMod started")

        val parentLoader = manifest["parentLoader"]?.jsonPrimitive?.content ?: run {
            val error = IllegalArgumentException("Mod manifest missing 'parentLoader' field")
            logger.e(error.message ?: "Missing parentLoader field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
        installDependence(zip, manifest, progressCallback)

        if (!ModLoaderManager.isExists(parentLoader)) {
            val error = IllegalStateException("Parent modloader $parentLoader not found")
            logger.e(error.message ?: "Parent modloader not found")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        logger.d("Parent modloader $parentLoader found, proceeding with mod installation")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

        // 自动获取或创建数据库
        val manager = getOrCreateModManager(parentLoader, autoCreate = true) ?: run {
            val error = IllegalStateException("Failed to create database for loader: $parentLoader")
            logger.e(error.message ?: "Failed to create database")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        manager.install(zip, manifest, progressCallback)
    }

    suspend fun installDependence(
        zip: ZipFile,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installDependence started")

        // Process modloader dependencies
        manifest["modloader"]?.jsonObject?.let { modLoader ->
            logger.d("Found modloader dependency in manifest")
            val type = modLoader["type"]?.jsonPrimitive?.content?.lowercase()
            if (type == "inline") {
                val file = modLoader["file"]?.jsonPrimitive?.content ?: run {
                    val error = IllegalArgumentException("Inline modloader missing 'file' field")
                    logger.e(error.message ?: "Missing file field")
                    progressCallback?.invoke(InstallProgress.ERROR, error)
                    return@let
                }

                logger.d("Processing inline modloader: $file")
                progressCallback?.invoke(InstallProgress.PROCESSING_DEPENDENCY, null)

                val cacheDir = Platform.getDirectory("tmp") / "temp_modloaders"
                fileSystem.createDirectories(cacheDir)

                val now = Clock.System.now()
                val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val timestamp = "${localDateTime.year}${localDateTime.month.number}${localDateTime.day}_" +
                        "${localDateTime.hour}${localDateTime.minute}${localDateTime.second}"
                val randomNum = Random.nextInt(1000, 9999)
                val tempZipPath = cacheDir / "modloader_${timestamp}_${randomNum}.zip"

                val fileEntry = zip.getEntry(file)

                if (fileEntry == null) {
                    val error = IllegalArgumentException("Inline modloader zip missing '$file' file")
                    logger.e(error.message ?: "Missing file field")
                    progressCallback?.invoke(InstallProgress.ERROR, error)
                    return@let
                }

                logger.d("Creating temporary modloader file: $tempZipPath")
                fileSystem.sink(tempZipPath, true).buffer().use {
                    it.writeAll(zip.getInputStream(fileEntry).asSource())
                    it.flush()
                }

                val modLoaderZip = ZipFile(fileSystem.openReadOnly(tempZipPath))
                logger.d("Opened modloader zip for processing")

                try {
                    val manifestEntry = modLoaderZip.getEntry("Manifest.json") ?: return@let

                    val manifestJson = modLoaderZip.getInputStream(manifestEntry).asSource().buffer().use { it.readUtf8() }
                    val modLoaderManifest = json.parseToJsonElement(manifestJson).jsonObject
                    logger.i("Installing inline modloader dependency")
                    ModLoaderManager.install(modLoaderZip, modLoaderManifest, progressCallback)
                } catch (e: Exception) {
                    logger.e("Failed to install inline modloader dependency", e)
                    progressCallback?.invoke(InstallProgress.ERROR, e)
                    throw e
                } finally {
                    modLoaderZip.close()
                    try {
                        fileSystem.delete(tempZipPath)
                        logger.d("Cleaned up temporary modloader file")
                    } catch (e: Exception) {
                        logger.w("Failed to delete temporary modloader file: $tempZipPath", e)
                    }
                    try {
                        fileSystem.deleteRecursively(cacheDir)
                        logger.d("Cleaned up temporary modloader directory")
                    } catch (e: Exception) {
                        logger.w("Failed to delete temporary modloader directory: $cacheDir", e)
                    }
                }
            }
        }

        // Process plugin dependencies
        manifest["plugins"]?.jsonObject?.get("type")?.jsonPrimitive?.content?.let { type ->
            if (type.lowercase() == "inline") {
                logger.d("Found inline plugin dependencies in manifest")
                val files = manifest["plugins"]?.jsonObject?.get("files")?.jsonArray
                files?.forEachIndexed { index, entry ->
                    try {
                        val entryPath = entry.jsonPrimitive.content
                        logger.d("Processing plugin dependency ${index + 1}/${files.size}: $entryPath")
                        progressCallback?.invoke(InstallProgress.PROCESSING_DEPENDENCY, null)

                        val cacheDir = Platform.getDirectory("tmp") / "temp_plugins"
                        fileSystem.createDirectories(cacheDir)

                        val now = Clock.System.now()
                        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                        val timestamp = "${localDateTime.year}${localDateTime.month.number}${localDateTime.day}_" +
                                "${localDateTime.hour}${localDateTime.minute}${localDateTime.second}"
                        val randomNum = Random.nextInt(1000, 9999)
                        val tempZipPath = cacheDir / "plugin_${timestamp}_${randomNum}.zip"

                        val fileEntry = zip.getEntry(entryPath)

                        if (fileEntry == null) {
                            val error = IllegalArgumentException("Inline plugin zip missing '$entryPath' file")
                            logger.e(error.message ?: "Missing file field")
                            progressCallback?.invoke(InstallProgress.ERROR, error)
                            return@let
                        }

                        logger.d("Creating temporary plugin file: $tempZipPath")
                        zip.getInputStream(fileEntry).asSource().buffer().use { source ->
                            fileSystem.sink(tempZipPath).buffer().use { sink ->
                                sink.writeAll(source)
                                sink.flush()
                            }
                        }

                        val pluginZip = ZipFile(fileSystem.openReadOnly(tempZipPath))
                        logger.d("Opened plugin zip for processing")

                        try {
                            val manifestEntry = pluginZip.getEntry("Manifest.json") ?: return@let

                            val manifestJson = pluginZip.getInputStream(manifestEntry).asSource()
                                .buffer()
                                .use { it.readUtf8() }

                            logger.i("Installing inline plugin dependency: $entryPath")
                            PluginManager.install(pluginZip, json.parseToJsonElement(manifestJson).jsonObject, progressCallback)
                        } finally {
                            pluginZip.close()
                            try {
                                fileSystem.delete(tempZipPath)
                                logger.d("Cleaned up temporary plugin file")
                            } catch (_: Exception) {
                                logger.w("Failed to delete temporary plugin file: $tempZipPath")
                            }
                        }
                    } catch (e: Exception) {
                        logger.e("Failed to process plugin dependency: ${entry.jsonPrimitive.content}", e)
                        progressCallback?.invoke(InstallProgress.ERROR, e)
                        throw e
                    }
                }
                logger.i("Finished processing plugin dependencies")
            }
        }
        logger.d("installDependence completed")
    }

    fun extractResources(
        zip: ZipFile,
        manifest: JsonObject,
        targetDir: Path,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        val resourcesPath = manifest["resources"]?.jsonPrimitive?.content
        if (resourcesPath.isNullOrBlank()) {
            logger.d("No 'resources' field in manifest, skipping resource extraction")
            return false
        }

        logger.d("Extracting resources from: $resourcesPath to: $targetDir")
        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

        try {
            // 收集所有需要解压的条目
            val entriesToExtract = mutableListOf<ZipEntry>()
            val resourcePathPrefix = resourcesPath.trimEnd('/') + "/"

            zip.entries.forEach { entry ->
                // 检查条目是否在资源目录下
                if (entry.name == resourcesPath ||
                    entry.name == "$resourcesPath/" ||
                    entry.name.startsWith(resourcePathPrefix)) {
                    // 跳过目录条目本身
                    if (!entry.isDirectory) {
                        entriesToExtract.add(entry)
                    }
                }
            }

            if (entriesToExtract.isEmpty()) {
                logger.w("No files found in resources path: $resourcesPath")
                return false
            }

            logger.d("Found ${entriesToExtract.size} files to extract")

            var extractedCount = 0
            val totalFiles = entriesToExtract.size

            entriesToExtract.forEach { entry ->
                // 计算相对路径
                val relativePath = if (entry.name == resourcesPath) {
                    // 直接是资源文件，不是目录
                    entry.name.substringAfterLast('/')
                } else {
                    // 去掉资源目录前缀
                    entry.name.substringAfter(resourcePathPrefix)
                }

                // 目标文件路径
                val destPath = targetDir / relativePath

                // 创建父目录
                destPath.parent?.let { parent ->
                    if (!fileSystem.exists(parent)) {
                        fileSystem.createDirectories(parent)
                    }
                }

                progressCallback?.invoke(
                    InstallProgress.COPYING_FILES,
                    null
                )

                // 解压文件
                zip.getInputStream(entry).use { inputStream ->
                    fileSystem.sink(destPath).buffer().use { sink ->
                        inputStream.asSource().use { source ->
                            sink.writeAll(source)
                            sink.flush()
                        }
                    }
                }

                extractedCount++
                if (extractedCount % 10 == 0) {
                    logger.d("Extracted $extractedCount/$totalFiles files")
                }
            }

            logger.i("Successfully extracted $extractedCount resources to $targetDir")
            progressCallback?.invoke(InstallProgress.COMPLETED, null)

            return true

        } catch (e: Exception) {
            logger.e("Failed to extract resources from $resourcesPath", e)
            progressCallback?.invoke(InstallProgress.ERROR, e)
            throw IOException("Failed to extract resources: ${e.message}", e)
        }
    }

    fun extractIcon(filePath: Path, zip: ZipFile, manifest: JsonObject) {
        manifest["iconFile"]?.jsonPrimitive?.content?.let { iconFile ->
            filePath.parent?.let { fileSystem.createDirectories(it) }

            val iconEntry = zip.getEntry(iconFile) ?: return

            fileSystem.sink(filePath, false).buffer().use {
                it.writeAll(zip.getInputStream(iconEntry).asSource())
                it.flush()
            }
        }
    }

    /**
     * 计算相对路径（处理 okio.Path 类型）
     */
    private fun calculateRelativePath(fullPath: Path, basePath: Path): Path {
        // 将路径转换为字符串进行比较
        val fullPathStr = fullPath.toString()
        val basePathStr = basePath.toString()

        // 如果完整路径以基础路径开头，则计算相对部分
        val relativeStr = if (fullPathStr.startsWith(basePathStr)) {
            // 去掉基础路径部分
            var relative = fullPathStr.substring(basePathStr.length)
            // 移除开头的路径分隔符
            while (relative.startsWith("/") || relative.startsWith("\\")) {
                relative = relative.substring(1)
            }
            relative
        } else {
            // 如果不是以基础路径开头，返回完整路径的名称
            fullPath.name
        }

        return relativeStr.toPath()
    }

    /**
     * 获取或创建模组数据库实例
     * @param loaderPkgId 模组加载器包ID
     * @param autoCreate 如果数据库不存在是否自动创建
     * @return 数据库实例，如果加载器被禁用且autoCreate为false则返回null
     */
    fun getOrCreateModManager(loaderPkgId: String, autoCreate: Boolean = false): ModManager? {
        // 检查是否已经有数据库实例
        val existingManager = modManagersList[loaderPkgId]
        if (existingManager != null) {
            return existingManager
        }

        // 检查加载器是否启用
        val isEnabled = ModLoaderManager.isEnabled(loaderPkgId)
        if (!isEnabled && !autoCreate) {
            logger.d("ModLoader $loaderPkgId is disabled, not creating manager")
            return null
        }

        // 创建新的数据库实例
        return try {
            val dbPath = Platform.getData("mods") / loaderPkgId / "db"

            // 确保目录存在
            val parentDir = dbPath.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            val manager = ModManager(loaderPkgId)

            modManagersList[loaderPkgId] = manager
            logger.i("Created mod manager for loader: $loaderPkgId")
            manager
        } catch (e: Exception) {
            logger.e("Failed to create mod manager for loader: $loaderPkgId", e)
            null
        }
    }

    /**
     * 刷新所有启用的模组数据库
     * 关闭禁用的，打开启用的
     */
    fun refreshModManager(modLoaders: List<ModLoaderItem>) {
        // 检查哪些加载器需要关闭
        val loadersToClose = mutableListOf<String>()

        modManagersList.forEach { (loaderId, _) ->
            val isEnabled = ModLoaderManager.isEnabled(loaderId)
            if (!isEnabled) {
                loadersToClose.add(loaderId)
            }
        }

        // 关闭禁用的加载器
        loadersToClose.forEach { loaderId ->
            closeModManager(loaderId)
        }

        // 打开启用的加载器
        modLoaders.forEach { loader ->
            val isEnabled = ModLoaderManager.isEnabled(loader.pkgId)
            if (isEnabled && !modManagersList.containsKey(loader.pkgId)) {
                getOrCreateModManager(loader.pkgId, autoCreate = true)
            }
        }
    }

    /**
     * 安全地关闭模组数据库
     * @param loaderPkgId 模组加载器包ID
     */
    fun closeModManager(loaderPkgId: String) {
        val manager = modManagersList.remove(loaderPkgId)
        if (manager != null) {
            try {
                manager.destroy()
                logger.i("Closed mod manager for loader: $loaderPkgId")
            } catch (e: Exception) {
                logger.e("Failed to close mod manager for loader: $loaderPkgId", e)
            }
        }
    }
}