package eternal.future.tefmanager.utils

import androidx.compose.runtime.mutableStateMapOf
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.GlobalConfig
import eternal.future.tefmanager.ui.model.ModItem
import eternal.future.tefmanager.ui.model.ModLoaderItem
import eternal.future.tefmanager.ui.model.ModuleItem
import eternal.future.tefmanager.ui.model.PluginItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
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
 * Created: 2026/3/15
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

        // 检查阶段
        CHECKING_EXISTING,

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
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val logger = AppLogger

    val pluginsDataBase = LightProtoStore(
        Platform.getData("plugin") / "db",
        PluginItem.serializer(), "plugin"
    )

    val modulesDataBase = LightProtoStore(
        Platform.getData("module") / "db",
        ModuleItem.serializer(), "module"
    )

    val modLoaderDataBase = LightProtoStore(
        Platform.getData("modloader") / "db",
        ModLoaderItem.serializer(), "modloader"
    )

    val modDataBaseList = mutableStateMapOf<String, LightProtoStore<ModItem>>()

    /**
     * 安装或更新附加包
     * @param filePath 包文件路径
     * @param progressCallback 进度回调函数，用于报告安装进度和错误
     */
    suspend fun installOrUpdate(filePath: Path, progressCallback: ProgressCallback? = null) {
        logger.d("Starting installOrUpdate for file: $filePath")
        progressCallback?.invoke(InstallProgress.STARTING, null)

        val zip = fileSystem.openZip(filePath)
        var currentError: Throwable? = null

        try {
            progressCallback?.invoke(InstallProgress.OPENING_PACKAGE, null)

            progressCallback?.invoke(InstallProgress.READING_MANIFEST, null)
            if (!zip.exists("Manifest.json".toPath())) {
                val error = IllegalArgumentException("No Manifest.json found in zip file: $filePath")
                logger.w(error.message ?: "No manifest found")
                progressCallback?.invoke(InstallProgress.ERROR, error)
                return
            }

            val manifest = json.parseToJsonElement(
                zip.source("Manifest.json".toPath()).buffer().readUtf8()
            ).jsonObject
            val type = manifest["type"]?.jsonPrimitive?.content?.lowercase()

            logger.d("Detected addon type: $type")

            when (type) {
                "plugin" -> {
                    logger.i("Installing/updating plugin")
                    progressCallback?.invoke(InstallProgress.STARTING, null)
                    try {
                        installOrUpdatePlugin(zip, manifest, progressCallback)
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
                        installOrUpdateModule(zip, manifest, progressCallback)
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
                        installOrUpdateModLoader(zip, manifest, progressCallback)
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
                        installOrUpdateMod(zip, manifest, progressCallback)
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

    private suspend fun installOrUpdatePlugin(
        zip: FileSystem,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installOrUpdatePlugin started")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)
        val targetFilePath = manifest["file"]?.jsonPrimitive?.content?.toPath() ?: run {
            val error = IllegalArgumentException("Plugin manifest missing 'file' field")
            logger.e(error.message ?: "Missing file field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        val pluginItem = json.decodeFromString<PluginItem>(
            zip.source("Info.json".toPath()).buffer().readUtf8()
        )

        logger.d("Processing plugin: ${pluginItem.pkgId}, version: ${pluginItem.versionCode}")

        progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)
        val originalPluginItem = pluginsDataBase.get(pluginItem.pkgId)
        val filePath = Platform.getData("plugin") / "pkg" / "${pluginItem.pkgId}.tefpkg"

        filePath.parent?.let { fileSystem.createDirectories(it) }

        zip.source(targetFilePath).use { source ->
            if (originalPluginItem != null) {
                logger.d("Plugin ${pluginItem.pkgId} already exists (version: ${originalPluginItem.versionCode})")
                if (pluginItem.versionCode > originalPluginItem.versionCode) {
                    progressCallback?.invoke(InstallProgress.STARTING, null)

                    progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
                    fileSystem.sink(filePath).buffer().use { sink ->
                        source.buffer().use { buffer ->
                            sink.writeAll(buffer)
                            sink.flush()
                        }
                    }

                    progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
                    pluginsDataBase.edit(pluginItem.pkgId) { pluginItem }
                    pluginsDataBase.flush()

                    progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
                    releaseIcon(Platform.getData("plugin") / "icons" / "${pluginItem.pkgId}.icon", zip, manifest)

                    logger.i("Plugin ${pluginItem.pkgId} updated successfully")
                } else {
                    logger.w("Plugin ${pluginItem.pkgId} already has same or newer version")
                    progressCallback?.invoke(InstallProgress.COMPLETED, null)
                }
                return
            }

            progressCallback?.invoke(InstallProgress.STARTING, null)

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            fileSystem.sink(filePath).buffer().use { sink ->
                source.buffer().use { buffer ->
                    sink.writeAll(buffer)
                    sink.flush()
                }
            }

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            pluginsDataBase.put(pluginItem.pkgId, pluginItem)
            pluginsDataBase.flush()

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            releaseIcon(Platform.getData("plugin") / "icons" / "${pluginItem.pkgId}.icon", zip, manifest)

            logger.i("Plugin ${pluginItem.pkgId} installed successfully")
        }
    }

    private suspend fun installOrUpdateModule(
        zip: FileSystem,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installOrUpdateModule started")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)
        val targetFilePath = manifest["file"]?.jsonPrimitive?.content?.toPath() ?: run {
            val error = IllegalArgumentException("Module manifest missing 'file' field")
            logger.e(error.message ?: "Missing file field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        val config = manifest["config"]?.jsonPrimitive?.content?.toPath()?.let { configPath ->
            json.decodeFromString<GlobalConfig>(zip.source(configPath).buffer().readUtf8())
        }

        val moduleItem = json.decodeFromString<ModuleItem>(
            zip.source("Info.json".toPath()).buffer().readUtf8()
        ).copy(globalConfig = config ?: GlobalConfig.empty)

        logger.d("Processing module: ${moduleItem.pkgId}, version: ${moduleItem.versionCode}")

        progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)
        val originalModuleItem = modulesDataBase.get(moduleItem.pkgId)
        val filePath = (Platform.getData("module") / "pkg" / "${moduleItem.pkgId}.tefpkg").also {
            it.parent?.let { dir ->
                fileSystem.createDirectories(dir)
            }
        }

        zip.source(targetFilePath).use { source ->
            if (originalModuleItem != null) {
                logger.d("Module ${moduleItem.pkgId} already exists (version: ${originalModuleItem.versionCode})")
                if (moduleItem.versionCode > originalModuleItem.versionCode) {
                    progressCallback?.invoke(InstallProgress.STARTING, null)

                    progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
                    fileSystem.sink(filePath).buffer().use { sink ->
                        source.buffer().use { buffer ->
                            sink.writeAll(buffer)
                            sink.flush()
                        }
                    }

                    progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
                    modulesDataBase.edit(moduleItem.pkgId) { moduleItem }
                    modulesDataBase.flush()

                    progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
                    releaseIcon(Platform.getData("module") / "icons" / "${moduleItem.pkgId}.icon", zip, manifest)

                    logger.i("Module ${moduleItem.pkgId} updated successfully")

                    progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
                    installDependence(zip, manifest, progressCallback)
                } else {
                    logger.w("Module ${moduleItem.pkgId} already has same or newer version")
                    progressCallback?.invoke(InstallProgress.COMPLETED, null)
                }
                return
            }

            progressCallback?.invoke(InstallProgress.STARTING, null)

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            fileSystem.sink(filePath).buffer().use { sink ->
                source.buffer().use { buffer ->
                    sink.writeAll(buffer)
                    sink.flush()
                }
            }

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            modulesDataBase.put(moduleItem.pkgId, moduleItem)
            modulesDataBase.flush()

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            releaseIcon(Platform.getData("module") / "icons" / "${moduleItem.pkgId}.icon", zip, manifest)

            progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
            installDependence(zip, manifest, progressCallback)
            extractResources(zip, manifest, Platform.getData("module") / moduleItem.pkgId / "private", progressCallback)

            logger.i("Module ${moduleItem.pkgId} installed successfully")
        }
    }

    private suspend fun installOrUpdateModLoader(
        zip: FileSystem,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installOrUpdateModLoader started")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)
        val targetFilePath = manifest["file"]?.jsonPrimitive?.content?.toPath() ?: run {
            val error = IllegalArgumentException("ModLoader manifest missing 'file' field")
            logger.e(error.message ?: "Missing file field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        val modLoaderItem = json.decodeFromString<ModLoaderItem>(
            zip.source("Info.json".toPath()).buffer().readUtf8()
        )

        logger.d("Processing modloader: ${modLoaderItem.pkgId}, version: ${modLoaderItem.versionCode}")

        progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)
        val originalModLoaderItem = modLoaderDataBase.get(modLoaderItem.pkgId)
        val filePath = (Platform.getData("modloader") / "pkg" / "${modLoaderItem.pkgId}.tefpkg").also {
            it.parent?.let { dir ->
                fileSystem.createDirectories(dir)
            }
        }

        zip.source(targetFilePath).use { source ->
            if (originalModLoaderItem != null) {
                logger.d("ModLoader ${modLoaderItem.pkgId} already exists (version: ${originalModLoaderItem.versionCode})")
                if (modLoaderItem.versionCode > originalModLoaderItem.versionCode) {
                    progressCallback?.invoke(InstallProgress.STARTING, null)

                    progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
                    fileSystem.sink(filePath).buffer().use { sink ->
                        source.buffer().use { buffer ->
                            sink.writeAll(buffer)
                            sink.flush()
                        }
                    }

                    progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
                    modLoaderDataBase.edit(modLoaderItem.pkgId) { modLoaderItem }
                    modLoaderDataBase.flush()

                    progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
                    releaseIcon(Platform.getData("modloader") / "icons" / "${modLoaderItem.pkgId}.icon", zip, manifest)

                    progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
                    installDependence(zip, manifest, progressCallback)

                    logger.i("ModLoader ${modLoaderItem.pkgId} updated successfully")
                } else {
                    logger.w("ModLoader ${modLoaderItem.pkgId} already has same or newer version")
                    progressCallback?.invoke(InstallProgress.COMPLETED, null)
                }
                return
            }

            progressCallback?.invoke(InstallProgress.STARTING, null)

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            fileSystem.sink(filePath).buffer().use { sink ->
                source.buffer().use { buffer ->
                    sink.writeAll(buffer)
                    sink.flush()
                }
            }

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            modLoaderDataBase.put(modLoaderItem.pkgId, modLoaderItem)
            modLoaderDataBase.flush()

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            releaseIcon(Platform.getData("modloader") / "icons" / "${modLoaderItem.pkgId}.icon", zip, manifest)

            progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
            installDependence(zip, manifest, progressCallback)

            logger.i("ModLoader ${modLoaderItem.pkgId} installed successfully")
        }
    }

    /**
     * 安装或更新模组（自动处理数据库创建）
     */
    private suspend fun installOrUpdateMod(
        zip: FileSystem,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installOrUpdateMod started")

        val parentLoader = manifest["parentLoader"]?.jsonPrimitive?.content ?: run {
            val error = IllegalArgumentException("Mod manifest missing 'parentLoader' field")
            logger.e(error.message ?: "Missing parentLoader field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        progressCallback?.invoke(InstallProgress.INSTALLING_DEPENDENCIES, null)
        installDependence(zip, manifest, progressCallback)

        if (!isModLoaderExists(parentLoader)) {
            val error = IllegalStateException("Parent modloader $parentLoader not found")
            logger.e(error.message ?: "Parent modloader not found")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        logger.d("Parent modloader $parentLoader found, proceeding with mod installation")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)
        val targetFilePath = manifest["file"]?.jsonPrimitive?.content?.toPath() ?: run {
            val error = IllegalArgumentException("Mod manifest missing 'file' field")
            logger.e(error.message ?: "Missing file field")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        // 自动获取或创建数据库
        val db = getOrCreateModDatabase(parentLoader, autoCreate = true) ?: run {
            val error = IllegalStateException("Failed to create database for loader: $parentLoader")
            logger.e(error.message ?: "Failed to create database")
            progressCallback?.invoke(InstallProgress.ERROR, error)
            throw error
        }

        val config = manifest["config"]?.jsonPrimitive?.content?.toPath()?.let { configPath ->
            return@let json.decodeFromString<GlobalConfig>(zip.source(configPath).buffer().readUtf8())
        } ?: GlobalConfig.empty

        val modItem = json.decodeFromString<ModItem>(
            zip.source("Info.json".toPath()).buffer().readUtf8()
        ).copy(globalConfig = config)

        logger.d("Processing mod: ${modItem.pkgId}, version: ${modItem.versionCode}")

        progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)
        val originalModItem = db.get(modItem.pkgId)
        val filePath = (Platform.getData("mods") / parentLoader / "mod" / modItem.pkgId).also {
            it.parent?.let { dir ->
                fileSystem.createDirectories(dir)
            }
        }

        zip.source(targetFilePath).use { source ->
            if (originalModItem != null) {
                logger.d("Mod ${modItem.pkgId} already exists (version: ${originalModItem.versionCode})")
                if (modItem.versionCode > originalModItem.versionCode) {
                    progressCallback?.invoke(InstallProgress.STARTING, null)

                    progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
                    fileSystem.sink(filePath).buffer().use { sink ->
                        source.buffer().use { buffer ->
                            sink.writeAll(buffer)
                            sink.flush()
                        }
                    }

                    progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
                    db.edit(modItem.pkgId) { modItem }
                    db.flush()

                    progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
                    releaseIcon(Platform.getData("mods") / parentLoader / "icons" / "${modItem.pkgId}.icon", zip, manifest)

                    logger.i("Mod ${modItem.pkgId} updated successfully")
                } else {
                    logger.w("Mod ${modItem.pkgId} already has same or newer version")
                    progressCallback?.invoke(InstallProgress.COMPLETED, null)
                }
                return
            }

            progressCallback?.invoke(InstallProgress.STARTING, null)

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            fileSystem.sink(filePath).buffer().use { sink ->
                source.buffer().use { buffer ->
                    sink.writeAll(buffer)
                    sink.flush()
                }
            }

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            db.put(modItem.pkgId, modItem)
            db.flush()

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            releaseIcon(Platform.getData("mods") / parentLoader / "icons" / "${modItem.pkgId}.icon", zip, manifest)
            extractResources(zip, manifest, Platform.getData("mods") / parentLoader /"private" / modItem.pkgId, progressCallback)

            logger.i("Mod ${modItem.pkgId} installed successfully")
        }
    }


    /**
     * 解压ZIP中的资源目录到指定路径
     * @param zip 文件系统实例
     * @param manifest JSON清单对象
     * @param targetDir 目标解压目录
     * @param progressCallback 进度回调（可选）
     * @return 是否成功解压了资源（true表示有资源且解压成功，false表示没有resources字段）
     * @throws IOException 当解压过程中发生IO错误时抛出
     */
    private fun extractResources(
        zip: FileSystem,
        manifest: JsonObject,
        targetDir: Path,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        // 检查是否存在resources字段
        val resourcesPathStr = manifest["resources"]?.jsonPrimitive?.content
        if (resourcesPathStr.isNullOrBlank()) {
            logger.d("No 'resources' field in manifest, skipping resource extraction")
            return false
        }

        val resourcesPath = resourcesPathStr.toPath()
        logger.d("Extracting resources from: $resourcesPath to: $targetDir")

        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

        // 检查资源目录是否存在
        try {
            // 使用Okio的FileSystem遍历ZIP中的目录
            val resourceFiles = mutableListOf<Path>()

            // 递归收集所有需要解压的文件
            fun collectFiles(currentPath: Path) {
                val entries = zip.listOrNull(currentPath) ?: return
                entries.forEach { entry ->
                    val fullPath = currentPath / entry.name
                    if (zip.metadataOrNull(fullPath)?.isDirectory == true) {
                        collectFiles(fullPath)
                    } else {
                        resourceFiles.add(fullPath)
                    }
                }
            }

            // 如果路径是文件而不是目录，直接添加
            if (zip.metadataOrNull(resourcesPath)?.isDirectory == true) {
                collectFiles(resourcesPath)
            } else if (zip.exists(resourcesPath)) {
                resourceFiles.add(resourcesPath)
            } else {
                logger.w("Resources path does not exist in zip: $resourcesPath")
                return false
            }

            if (resourceFiles.isEmpty()) {
                logger.w("No files found in resources path: $resourcesPath")
                return false
            }

            logger.d("Found ${resourceFiles.size} files to extract")

            var extractedCount = 0
            val totalFiles = resourceFiles.size

            resourceFiles.forEach { sourcePath ->
                // 计算相对路径 - 修正这部分逻辑
                val relativePath = calculateRelativePath(sourcePath, resourcesPath)

                // 目标文件路径
                val destPath = if (relativePath.name.isEmpty()) {
                    targetDir / sourcePath.name
                } else {
                    targetDir / relativePath
                }

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
                zip.source(sourcePath).use { source ->
                    fileSystem.sink(destPath).buffer().use { sink ->
                        sink.writeAll(source)
                        sink.flush()
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
    fun getOrCreateModDatabase(loaderPkgId: String, autoCreate: Boolean = false): LightProtoStore<ModItem>? {
        // 检查是否已经有数据库实例
        val existingDb = modDataBaseList[loaderPkgId]
        if (existingDb != null) {
            return existingDb
        }

        // 检查加载器是否启用
        val isEnabled = isAddonEnabled("modloader", loaderPkgId)
        if (!isEnabled && !autoCreate) {
            logger.d("ModLoader $loaderPkgId is disabled, not creating database")
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

            val db = LightProtoStore(
                dbPath,
                ModItem.serializer(),
                "mods_$loaderPkgId"
            )

            modDataBaseList[loaderPkgId] = db
            logger.i("Created mod database for loader: $loaderPkgId")
            db
        } catch (e: Exception) {
            logger.e("Failed to create mod database for loader: $loaderPkgId", e)
            null
        }
    }

    /**
     * 安全地关闭模组数据库
     * @param loaderPkgId 模组加载器包ID
     */
    fun closeModDatabase(loaderPkgId: String) {
        val db = modDataBaseList.remove(loaderPkgId)
        if (db != null) {
            try {
                db.destroy()
                logger.i("Closed mod database for loader: $loaderPkgId")
            } catch (e: Exception) {
                logger.e("Failed to close mod database for loader: $loaderPkgId", e)
            }
        }
    }

    private suspend fun installDependence(
        zip: FileSystem,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null
    ) {
        logger.d("installDependence started")

        // Process modloader dependencies
        manifest["modloader"]?.jsonObject?.let { modLoader ->
            logger.d("Found modloader dependency in manifest")
            val type = modLoader["type"]?.jsonPrimitive?.content?.lowercase()
            if (type == "inline") {
                val file = modLoader["file"]?.jsonPrimitive?.content?.toPath() ?: run {
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

                logger.d("Creating temporary modloader file: $tempZipPath")
                fileSystem.sink(tempZipPath, true).buffer().use {
                    it.writeAll(zip.source(file))
                    it.flush()
                }

                val modLoaderZip = fileSystem.openZip(tempZipPath)
                logger.d("Opened modloader zip for processing")

                try {
                    val manifestJson = modLoaderZip.source("Manifest.json".toPath()).buffer().use { it.readUtf8() }
                    val modLoaderManifest = json.parseToJsonElement(manifestJson).jsonObject
                    logger.i("Installing inline modloader dependency")
                    installOrUpdateModLoader(modLoaderZip, modLoaderManifest, progressCallback)
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

                        logger.d("Creating temporary plugin file: $tempZipPath")
                        zip.source(entryPath.toPath()).buffer().use { source ->
                            fileSystem.sink(tempZipPath).buffer().use { sink ->
                                sink.writeAll(source)
                                sink.flush()
                            }
                        }

                        val pluginZip = fileSystem.openZip(tempZipPath)
                        logger.d("Opened plugin zip for processing")

                        try {
                            val manifestJson = pluginZip.source("Manifest.json".toPath())
                                .buffer()
                                .use { it.readUtf8() }

                            logger.i("Installing inline plugin dependency: $entryPath")
                            installOrUpdatePlugin(pluginZip, json.parseToJsonElement(manifestJson).jsonObject, progressCallback)
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

    private fun releaseIcon(filePath: Path, zip: FileSystem, manifest: JsonObject) {
        manifest["iconFile"]?.jsonPrimitive?.content?.toPath()?.let { iconFile ->
            filePath.parent?.let { fileSystem.createDirectories(it) }

            fileSystem.sink(filePath, true).buffer().use {
                it.writeAll(zip.source(iconFile))
                it.flush()
            }
        }
    }

    fun isPluginExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("plugin") / "pkg" / "$pkgId.tefpkg")

    fun isModuleExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("module") / "pkg" / "$pkgId.tefpkg")

    fun isModLoaderExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("modloader") / "pkg" / "$pkgId.tefpkg")

    fun isModExists(pkgId: String, loaderPkgId: String): Boolean =
        fileSystem.exists(Platform.getData("mods") / loaderPkgId / "mod" / pkgId)

    /**
     * 启用一个附加组件
     */
    fun enableAddon(type: String, pkgId: String, loaderPkgId: String? = null): Boolean {
        return try {
            val enableFile = getEnableFilePath(type, loaderPkgId)

            // 检查是否已启用
            if (isAddonEnabled(type, pkgId, loaderPkgId)) {
                return true
            }

            // 确保目录存在
            val parentDir = enableFile.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            // 追加写入
            fileSystem.appendingSink(enableFile).buffer().use { sink ->
                sink.writeUtf8("$pkgId\n")
                sink.flush()
            }

            AppLogger.d("Enabled $type: $pkgId")
            true
        } catch (e: Exception) {
            AppLogger.e("Failed to enable $type: $pkgId", e)
            false
        }
    }

    /**
     * 禁用一个附加组件
     */
    fun disableAddon(type: String, pkgId: String, loaderPkgId: String? = null): Boolean {
        return try {
            val enableFile = getEnableFilePath(type, loaderPkgId)

            if (!fileSystem.exists(enableFile)) {
                return true
            }

            // 逐行读取，过滤掉要禁用的项
            val tempFile = enableFile.parent?.let { it / "${enableFile.name}.tmp" }
                ?: return false

            var found = false
            fileSystem.source(enableFile).buffer().use { source ->
                fileSystem.sink(tempFile).buffer().use { sink ->
                    var line: String?
                    while (true) {
                        line = source.readUtf8Line()
                        if (line == null) break

                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty()) {
                            if (trimmedLine == pkgId) {
                                found = true
                            } else {
                                sink.writeUtf8("$trimmedLine\n")
                            }
                        }
                    }
                    sink.flush()
                }
            }

            // 替换文件
            if (found) {
                fileSystem.atomicMove(tempFile, enableFile)
                AppLogger.d("Disabled $type: $pkgId")
                true
            } else {
                fileSystem.delete(tempFile)
                true
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to disable $type: $pkgId", e)
            false
        }
    }

    /**
     * 检查附加组件是否启用
     */
    fun isAddonEnabled(type: String, pkgId: String, loaderPkgId: String? = null): Boolean {
        return try {
            val enableFile = getEnableFilePath(type, loaderPkgId)

            if (!fileSystem.exists(enableFile)) {
                return false
            }

            fileSystem.source(enableFile).buffer().use { source ->
                var line: String?
                while (true) {
                    line = source.readUtf8Line()
                    if (line == null) break
                    if (line.trim() == pkgId) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            AppLogger.e("Failed to check if $type is enabled: $pkgId", e)
            false
        }
    }

    /**
     * 删除插件
     * @param pkgId 插件包ID
     * @return 是否成功删除
     */
    suspend fun deletePlugin(pkgId: String): Boolean {
        logger.d("Deleting plugin: $pkgId")
        return try {
            val pluginItem = pluginsDataBase.get(pkgId)
            if (pluginItem == null) {
                logger.w("Plugin not found: $pkgId")
                return false
            }

            // 1. 从数据库删除
            pluginsDataBase.edit(pkgId) { null } ?: false

            // 2. 删除插件文件
            val pluginFile = Platform.getData("plugin") / "pkg" / "${pluginItem.pkgId}.tefpkg"
            if (fileSystem.exists(pluginFile)) {
                fileSystem.delete(pluginFile)
            }

            // 3. 删除图标文件
            val iconFile = Platform.getData("plugin") / "icons" / "${pluginItem.pkgId}.icon"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            // 4. 禁用插件
            disableAddon("plugin", pkgId)

            logger.i("Plugin deleted successfully: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete plugin: $pkgId", e)
            false
        }
    }

    /**
     * 删除模块
     * @param pkgId 模块包ID
     * @return 是否成功删除
     */
    suspend fun deleteModule(pkgId: String): Boolean {
        logger.d("Deleting module: $pkgId")
        return try {
            val moduleItem = modulesDataBase.get(pkgId)
            if (moduleItem == null) {
                logger.w("Module not found: $pkgId")
                return false
            }

            modulesDataBase.edit(pkgId) { null } ?: false

            val moduleFile = Platform.getData("module") / "pkg" / "${moduleItem.pkgId}.tefpkg"
            if (fileSystem.exists(moduleFile)) {
                fileSystem.delete(moduleFile)
            }

            val iconFile = Platform.getData("module") / "icons" / "${moduleItem.pkgId}.icon"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            disableAddon("module", pkgId)

            logger.i("Module deleted successfully: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete module: $pkgId", e)
            false
        }
    }

    /**
     * 删除模组加载器
     * @param pkgId 模组加载器包ID
     * @return 是否成功删除
     */
    suspend fun deleteModLoader(pkgId: String): Boolean {
        logger.d("Deleting modloader: $pkgId")
        return try {
            val modLoaderItem = modLoaderDataBase.get(pkgId)
            if (modLoaderItem == null) {
                logger.w("ModLoader not found: $pkgId")
                return false
            }

            deleteAllModsInLoader(pkgId)

            modLoaderDataBase.delete(pkgId)
            modLoaderDataBase.flush()

            val modLoaderFile = Platform.getData("modloader") / "pkg" / "${modLoaderItem.pkgId}.tefpkg"
            if (fileSystem.exists(modLoaderFile)) {
                fileSystem.delete(modLoaderFile)
            }

            val iconFile = Platform.getData("modloader") / "icons" / "${modLoaderItem.pkgId}.icon"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            disableAddon("modloader", pkgId)

            logger.i("ModLoader deleted successfully: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete modloader: $pkgId", e)
            false
        }
    }

    /**
     * 删除模组
     * @param pkgId 模组包ID
     * @param loaderPkgId 模组加载器包ID
     * @return 是否成功删除
     */
    suspend fun deleteMod(pkgId: String, loaderPkgId: String): Boolean {
        logger.d("Deleting mod: $pkgId in loader: $loaderPkgId")
        return try {
            // 检查加载器是否存在
            if (!isModLoaderExists(loaderPkgId)) {
                logger.w("ModLoader not found: $loaderPkgId")
                return false
            }

            // 获取数据库（如果不存在则创建）
            val db = getOrCreateModDatabase(loaderPkgId, autoCreate = false)
            if (db == null) {
                logger.w("Mod database not found for loader: $loaderPkgId")
                return false
            }

            val modItem = db.get(pkgId)
            if (modItem == null) {
                logger.w("Mod not found: $pkgId")
                return false
            }

            db.delete(pkgId)

            val modFile = Platform.getData("mods") / loaderPkgId / "mod" / pkgId
            if (fileSystem.exists(modFile)) {
                fileSystem.delete(modFile)
            }

            val iconFile = Platform.getData("mods") / loaderPkgId / "icons" / "${pkgId}.icon"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            disableAddon("mods", pkgId, loaderPkgId)

            // 如果数据库为空，可以考虑关闭它
            val remainingMods = db.getAllValues()
            if (remainingMods.isEmpty() && !isAddonEnabled("modloader", loaderPkgId)) {
                closeModDatabase(loaderPkgId)
            }

            logger.i("Mod deleted successfully: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete mod: $pkgId", e)
            false
        }
    }

    /**
     * 删除加载器下的所有模组
     * @param loaderPkgId 模组加载器包ID
     */
    private fun deleteAllModsInLoader(loaderPkgId: String) {
        logger.d("Deleting all mods in loader: $loaderPkgId")
        try {
            val modsDir = Platform.getData("mods") / loaderPkgId
            if (!fileSystem.exists(modsDir)) {
                return
            }

            modDataBaseList[loaderPkgId]?.destroy()

            // 删除整个模组目录
            if (fileSystem.exists(modsDir)) {
                fileSystem.deleteRecursively(modsDir)
            }

            logger.i("All mods deleted in loader: $loaderPkgId")
        } catch (e: Exception) {
            logger.e("Failed to delete all mods in loader: $loaderPkgId", e)
        }
    }

    private fun getEnableFilePath(type: String, loaderPkgId: String? = null): Path {
        return if (type == "mods" && loaderPkgId != null) {
            Platform.getData("mods") / loaderPkgId / "enables.txt"
        } else {
            Platform.getData(type) / "enables.txt"
        }
    }

    /**
     * 刷新所有启用的模组数据库
     * 关闭禁用的，打开启用的
     */
    fun refreshModDatabases(modLoaders: List<ModLoaderItem>) {
        // 检查哪些加载器需要关闭
        val loadersToClose = mutableListOf<String>()

        modDataBaseList.forEach { (loaderId, db) ->
            val isEnabled = isAddonEnabled("modloader", loaderId)
            if (!isEnabled) {
                loadersToClose.add(loaderId)
            }
        }

        // 关闭禁用的加载器
        loadersToClose.forEach { loaderId ->
            closeModDatabase(loaderId)
        }

        // 打开启用的加载器
        modLoaders.forEach { loader ->
            val isEnabled = isAddonEnabled("modloader", loader.pkgId)
            if (isEnabled && !modDataBaseList.containsKey(loader.pkgId)) {
                getOrCreateModDatabase(loader.pkgId, autoCreate = true)
            }
        }
    }
}