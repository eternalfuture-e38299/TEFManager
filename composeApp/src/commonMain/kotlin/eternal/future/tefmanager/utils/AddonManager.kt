package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.ModuleItem
import eternal.future.tefmanager.ui.model.PluginItem
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

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
    private val fileSystem = FileSystem.SYSTEM

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    enum class ProgressStage {
        DETECTING_TYPE,
        LOADING_METADATA,
        CHECKING_VERSION,
        INSTALLING_MAIN,
        INSTALLING_LOADER,
        INSTALLING_PLUGINS,
        UPDATING_DATABASE,
        COMPLETING,
        FAILED;

        override fun toString(): String = when (this) {
            DETECTING_TYPE -> "Detecting addon type..."
            LOADING_METADATA -> "Loading metadata..."
            CHECKING_VERSION -> "Checking version..."
            INSTALLING_MAIN -> "Installing main file..."
            INSTALLING_LOADER -> "Installing loader..."
            INSTALLING_PLUGINS -> "Installing plugins..."
            UPDATING_DATABASE -> "Updating database..."
            COMPLETING -> "Completing installation..."
            FAILED -> "Installation failed"
        }
    }

    data class ProgressInfo(
        val stage: ProgressStage,
        val current: Int = 0,
        val total: Int = 0,
        val exception: Exception? = null
    )

    typealias ProgressCallback = (ProgressInfo) -> Unit

    suspend fun install(
        filePath: Path,
        modDataBase: LightProtoStore<ModuleItem>,
        loaderDataBase: LightProtoStore<ModuleItem>,
        moduleDataBase: LightProtoStore<ModuleItem>,
        pluginDataBase: LightProtoStore<PluginItem>,
        progressCallback: ProgressCallback? = null
    ): Boolean = try {
        AppLogger.d("Starting addon installation from: $filePath")
        progressCallback?.invoke(ProgressInfo(ProgressStage.DETECTING_TYPE))

        val zip = fileSystem.openZip(filePath)

        val isPlugin = zip.exists("plugin.bin".toPath())
        val isLoader = zip.exists("loader.bin".toPath())
        val isModule = zip.exists("module.bin".toPath())
        val isMod = zip.exists("mod.bin".toPath())

        when {
            isMod -> installMod(zip, modDataBase, loaderDataBase, pluginDataBase, progressCallback)
            isPlugin -> installPlugin(zip, pluginDataBase, progressCallback)
            isLoader -> installModuleOrLoader(
                zip,
                loaderDataBase,
                pluginDataBase,
                isUpdate = false,
                progressCallback
            )

            isModule -> installModuleOrLoader(
                zip,
                moduleDataBase,
                pluginDataBase,
                isUpdate = false,
                progressCallback
            )

            else -> {
                val error = Exception("Unknown addon type")
                AppLogger.e("Unknown addon type: $filePath")
                progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = error))
                return false
            }
        }

        zip.close()

        progressCallback?.invoke(ProgressInfo(ProgressStage.COMPLETING))
        AppLogger.i("Addon installation completed successfully")

        true
    } catch (e: Exception) {
        AppLogger.e("Installation failed for file: $filePath", e)
        progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
        false
    }


    suspend fun update(
        filePath: Path,
        modDataBase: LightProtoStore<ModuleItem>,
        loaderDataBase: LightProtoStore<ModuleItem>,
        moduleDataBase: LightProtoStore<ModuleItem>,
        pluginDataBase: LightProtoStore<PluginItem>,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        return try {
            AppLogger.d("Starting addon update from: $filePath")
            progressCallback?.invoke(ProgressInfo(ProgressStage.DETECTING_TYPE))

            val zip = fileSystem.openZip(filePath)

            val isPlugin = zip.exists("plugin.bin".toPath())
            val isLoader = zip.exists("loader.bin".toPath())
            val isModule = zip.exists("module.bin".toPath())
            val isMod = zip.exists("mod.bin".toPath())

            val result = when {
                isMod -> updateMod(zip, modDataBase, progressCallback)
                isPlugin -> updatePlugin(zip, pluginDataBase, progressCallback)
                isLoader -> updateModuleOrLoader(zip, loaderDataBase, progressCallback)
                isModule -> updateModuleOrLoader(zip, moduleDataBase, progressCallback)
                else -> {
                    val error = Exception("Unknown update type")
                    AppLogger.e("Unknown update type: $filePath")
                    progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = error))
                    false
                }
            }

            zip.close()

            if (result) {
                progressCallback?.invoke(ProgressInfo(ProgressStage.COMPLETING))
                AppLogger.i("Addon update completed successfully")
            } else {
                AppLogger.w("Addon update failed")
            }

            result
        } catch (e: Exception) {
            AppLogger.e("Update failed for file: $filePath", e)
            progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
            false
        }
    }

    private suspend fun installMod(
        zip: FileSystem,
        database: LightProtoStore<ModuleItem>,
        loaderDataBase: LightProtoStore<ModuleItem>? = null,
        pluginDataBase: LightProtoStore<PluginItem>? = null,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        return try {
            AppLogger.d("Starting Mod installation")
            progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

            val moduleItem = json.decodeFromString<ModuleItem>(
                zip.source("info.json".toPath()).buffer().readUtf8()
            )

            var parentLoader: String?

            // Prioritize reading parentLoader.txt
            if (zip.exists("parentLoader.txt".toPath())) {
                AppLogger.d("Reading parent loader from parentLoader.txt")
                progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
                parentLoader = zip.source("parentLoader.txt".toPath()).buffer().readUtf8Line()
            }
            // If no parentLoader.txt, try to install loader.bin
            else if (zip.exists("loader.bin".toPath())) {
                AppLogger.d("Installing loader from loader.bin")
                progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_LOADER))
                val loaderZip = zip.openZip("loader.bin".toPath())
                val loaderItem = installModuleOrLoader(
                    loaderZip,
                    loaderDataBase!!,
                    pluginDataBase,
                    isUpdate = false,
                    progressCallback
                )
                parentLoader = loaderItem.pkgId
                loaderZip.close()
                AppLogger.i("Loader installed: ${loaderItem.pkgId}")
            } else {
                throw Exception("Mod missing parent loader information")
            }

            if (parentLoader == null) {
                throw Exception("Unable to determine parent loader")
            }

            AppLogger.d("Parent loader: $parentLoader")
            progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
            val isUpdate = isModExists(moduleItem.pkgId, parentLoader)
            if (isUpdate) {
                AppLogger.w("Mod already exists: ${moduleItem.pkgId}, use update instead")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("Mod already exists, use update function")
                    )
                )
                return false
            }

            AppLogger.d("Installing Mod file: ${moduleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
            fileSystem.sink(Platform.getData("mod") / parentLoader / "mod" / moduleItem.pkgId)
                .buffer().use {
                val pkgFileSize = zip.metadataOrNull("mod.bin".toPath())?.size
                val pkgFile = zip.source("mod.bin".toPath())
                it.write(pkgFile, pkgFileSize ?: 0)
                it.flush()
            }
            AppLogger.i("Mod file installed: ${moduleItem.pkgId}")

            // Install associated plugins
            val pluginPaths = zip.listOrNull("plugins".toPath())
            if (!pluginPaths.isNullOrEmpty()) {
                AppLogger.d("Installing ${pluginPaths.size} associated plugins")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.INSTALLING_PLUGINS,
                        current = 0,
                        total = pluginPaths.size
                    )
                )

                pluginPaths.forEachIndexed { index, path ->
                    AppLogger.d("Installing plugin: ${path.name}")
                    progressCallback?.invoke(
                        ProgressInfo(
                            ProgressStage.INSTALLING_PLUGINS,
                            current = index + 1,
                            total = pluginPaths.size
                        )
                    )
                    val plugin = zip.openZip(path)
                    installPlugin(plugin, pluginDataBase!!, progressCallback)
                    plugin.close()
                }
                AppLogger.i("All plugins installed successfully")
            }

            AppLogger.d("Updating database for Mod: ${moduleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
            database.put(moduleItem.pkgId, moduleItem)
            AppLogger.i("Mod installation completed: ${moduleItem.pkgId}")

            true
        } catch (e: Exception) {
            AppLogger.e("Mod installation failed", e)
            progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
            false
        }
    }

    private suspend fun updateMod(
        zip: FileSystem,
        database: LightProtoStore<ModuleItem>,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        return try {
            AppLogger.d("Starting Mod update")
            progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

            val newModuleItem = json.decodeFromString<ModuleItem>(
                zip.source("info.json".toPath()).buffer().readUtf8()
            )

            // Get parent loader
            var parentLoader: String?
            if (zip.exists("parentLoader.txt".toPath())) {
                parentLoader = zip.source("parentLoader.txt".toPath()).buffer().readUtf8Line()
                AppLogger.d("Parent loader from file: $parentLoader")
            } else if (zip.exists("loader.bin".toPath())) {
                val loaderZip = zip.openZip("loader.bin".toPath())
                parentLoader = json.decodeFromString<ModuleItem>(loaderZip.source("info.json".toPath()).buffer().readUtf8()).pkgId
                loaderZip.close()
                AppLogger.d("Parent loader from database: $parentLoader")
            } else {
                throw Exception("Mod missing parent loader information")
            }

            if (parentLoader == null) {
                throw Exception("Unable to determine parent loader")
            }

            AppLogger.d("Checking version for Mod: ${newModuleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
            val existingItem = database.get(newModuleItem.pkgId)
            if (existingItem == null) {
                AppLogger.w("Mod not found in database: ${newModuleItem.pkgId}")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("Mod not found, use install function")
                    )
                )
                return false
            }

            if (newModuleItem.versionCode <= existingItem.versionCode) {
                AppLogger.w("New version (${newModuleItem.versionCode}) not higher than current (${existingItem.versionCode})")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("New version not higher than current version")
                    )
                )
                return false
            }

            AppLogger.d("Updating Mod file: ${newModuleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
            fileSystem.sink(Platform.getData("mod") / parentLoader / "mod" / newModuleItem.pkgId)
                .buffer().use {
                val pkgFileSize = zip.metadataOrNull("mod.bin".toPath())?.size
                val pkgFile = zip.source("mod.bin".toPath())
                it.write(pkgFile, pkgFileSize ?: 0)
                it.flush()
            }
            AppLogger.i("Mod file updated: ${newModuleItem.pkgId}")

            AppLogger.d("Updating database for Mod: ${newModuleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
            database.editIf(
                newModuleItem.pkgId,
                { newModuleItem.versionCode > it.versionCode }
            ) { newModuleItem }
            AppLogger.i("Mod update completed: ${newModuleItem.pkgId}")

            true
        } catch (e: Exception) {
            AppLogger.e("Mod update failed", e)
            progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
            false
        }
    }

    private suspend fun installModuleOrLoader(
        zip: FileSystem,
        database: LightProtoStore<ModuleItem>,
        pluginDataBase: LightProtoStore<PluginItem>? = null,
        isUpdate: Boolean = false,
        progressCallback: ProgressCallback? = null
    ): ModuleItem {
        AppLogger.d("Starting Module/Loader installation (update: $isUpdate)")
        progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

        val moduleItem = json.decodeFromString<ModuleItem>(
            zip.source("info.json".toPath()).buffer().readUtf8()
        )

        val isModule = zip.exists("module.bin".toPath())
        val isLoader = zip.exists("loader.bin".toPath())

        if (!isModule && !isLoader) {
            throw Exception("Invalid module/loader file")
        }

        val typeName = if (isModule) "Module" else "Loader"
        val targetFile = if (isModule) "module.bin" else "loader.bin"
        val outDir = if (isModule) "module" else "loader"

        AppLogger.d("Detected type: $typeName, Package ID: ${moduleItem.pkgId}")

        progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
        val exists = isModuleExists(moduleItem.pkgId) || isLoaderExists(moduleItem.pkgId)

        if (exists && !isUpdate) {
            AppLogger.w("$typeName already exists: ${moduleItem.pkgId}, use update instead")
            throw Exception("$typeName already exists, use update function")
        } else if (!exists && isUpdate) {
            AppLogger.w("$typeName not found: ${moduleItem.pkgId}, use install instead")
            throw Exception("$typeName not found, use install function")
        }

        val outFile = Platform.getData(outDir) / "pkg" / "${moduleItem.pkgId}.tefpkg"

        AppLogger.d("Installing $typeName file: ${moduleItem.pkgId}")
        progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
        fileSystem.sink(outFile).buffer().use {
            val pkgFileSize = zip.metadataOrNull(targetFile.toPath())?.size
            val pkgFile = zip.source(targetFile.toPath())
            it.write(pkgFile, pkgFileSize ?: 0)
            it.flush()
        }
        AppLogger.i("$typeName file installed: ${moduleItem.pkgId}")

        // Install associated plugins
        val pluginPaths = zip.listOrNull("plugins".toPath())
        if (!pluginPaths.isNullOrEmpty() && pluginDataBase != null) {
            AppLogger.d("Installing ${pluginPaths.size} associated plugins")
            progressCallback?.invoke(
                ProgressInfo(
                    ProgressStage.INSTALLING_PLUGINS,
                    current = 0,
                    total = pluginPaths.size
                )
            )

            pluginPaths.forEachIndexed { index, path ->
                AppLogger.d("Installing plugin: ${path.name}")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.INSTALLING_PLUGINS,
                        current = index + 1,
                        total = pluginPaths.size
                    )
                )
                val plugin = zip.openZip(path)
                installPlugin(plugin, pluginDataBase, progressCallback)
                plugin.close()
            }
            AppLogger.i("All plugins installed successfully")
        }

        AppLogger.d("Updating database for $typeName: ${moduleItem.pkgId}")
        progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
        if (isUpdate) {
            database.editIf(
                moduleItem.pkgId,
                { moduleItem.versionCode > it.versionCode }
            ) { moduleItem }
            AppLogger.i("$typeName updated in database: ${moduleItem.pkgId}")
        } else {
            database.put(moduleItem.pkgId, moduleItem)
            AppLogger.i("$typeName added to database: ${moduleItem.pkgId}")
        }

        return moduleItem
    }

    private suspend fun updateModuleOrLoader(
        zip: FileSystem,
        database: LightProtoStore<ModuleItem>,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        return try {
            AppLogger.d("Starting Module/Loader update")
            progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

            val newModuleItem = json.decodeFromString<ModuleItem>(
                zip.source("info.json".toPath()).buffer().readUtf8()
            )

            val isModule = zip.exists("module.bin".toPath())
            zip.exists("loader.bin".toPath())
            val typeName = if (isModule) "Module" else "Loader"
            val targetFile = if (isModule) "module.bin" else "loader.bin"
            val outDir = if (isModule) "module" else "loader"

            AppLogger.d("Updating $typeName: ${newModuleItem.pkgId}")

            progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
            val existingItem = database.get(newModuleItem.pkgId)
            if (existingItem == null) {
                AppLogger.w("$typeName not found in database: ${newModuleItem.pkgId}")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("$typeName not found, use install function")
                    )
                )
                return false
            }

            if (newModuleItem.versionCode <= existingItem.versionCode) {
                AppLogger.w("New version (${newModuleItem.versionCode}) not higher than current (${existingItem.versionCode})")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("New version not higher than current version")
                    )
                )
                return false
            }

            val outFile = Platform.getData(outDir) / "pkg" / "${newModuleItem.pkgId}.tefpkg"

            AppLogger.d("Installing updated $typeName file")
            progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
            fileSystem.sink(outFile).buffer().use {
                val pkgFileSize = zip.metadataOrNull(targetFile.toPath())?.size
                val pkgFile = zip.source(targetFile.toPath())
                it.write(pkgFile, pkgFileSize ?: 0)
                it.flush()
            }
            AppLogger.i("$typeName file updated: ${newModuleItem.pkgId}")

            AppLogger.d("Updating database for $typeName: ${newModuleItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
            database.editIf(
                newModuleItem.pkgId,
                { newModuleItem.versionCode > it.versionCode }
            ) { newModuleItem }
            AppLogger.i("$typeName update completed: ${newModuleItem.pkgId}")

            true
        } catch (e: Exception) {
            AppLogger.e("update failed", e)
            progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
            false
        }
    }

    private suspend fun installPlugin(
        pluginZip: FileSystem,
        database: LightProtoStore<PluginItem>,
        progressCallback: ProgressCallback? = null
    ): PluginItem {
        AppLogger.d("Starting Plugin installation")
        progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

        val pluginItem: PluginItem = json.decodeFromString<PluginItem>(
            pluginZip.source("info.json".toPath()).buffer().readUtf8()
        )

        AppLogger.d("Installing Plugin: ${pluginItem.pkgId}")
        progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
        fileSystem.sink(Platform.getData("plugin") / "pkg" / "${pluginItem.pkgId}.tefpkg").buffer()
            .use {
                val pkgFileSize = pluginZip.metadataOrNull("plugin.bin".toPath())?.size
                val pkgFile = pluginZip.source("plugin.bin".toPath())
                it.write(pkgFile, pkgFileSize ?: 0)
                it.flush()
            }
        AppLogger.i("Plugin file installed: ${pluginItem.pkgId}")

        AppLogger.d("Checking if Plugin exists: ${pluginItem.pkgId}")
        progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
        if (isPluginExists(pluginItem.pkgId)) {
            AppLogger.w("Plugin already exists: ${pluginItem.pkgId}, updating if newer")
            database.editIf(
                pluginItem.pkgId,
                { pluginItem.versionCode > it.versionCode }
            ) { pluginItem }
            AppLogger.i("Plugin updated in database: ${pluginItem.pkgId}")
        } else {
            AppLogger.d("Adding Plugin to database: ${pluginItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
            database.put(pluginItem.pkgId, pluginItem)
            AppLogger.i("Plugin added to database: ${pluginItem.pkgId}")
        }

        return pluginItem
    }

    private suspend fun updatePlugin(
        pluginZip: FileSystem,
        database: LightProtoStore<PluginItem>,
        progressCallback: ProgressCallback? = null
    ): Boolean {
        return try {
            AppLogger.d("Starting Plugin update")
            progressCallback?.invoke(ProgressInfo(ProgressStage.LOADING_METADATA))

            val pluginItem: PluginItem = json.decodeFromString<PluginItem>(
                pluginZip.source("info.json".toPath()).buffer().readUtf8()
            )

            AppLogger.d("Checking if Plugin exists: ${pluginItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.CHECKING_VERSION))
            val existingItem = database.get(pluginItem.pkgId)
            if (existingItem == null) {
                AppLogger.w("Plugin not found in database: ${pluginItem.pkgId}")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("Plugin not found, use install function")
                    )
                )
                return false
            }

            if (pluginItem.versionCode <= existingItem.versionCode) {
                AppLogger.w("New version (${pluginItem.versionCode}) not higher than current (${existingItem.versionCode})")
                progressCallback?.invoke(
                    ProgressInfo(
                        ProgressStage.FAILED,
                        exception = Exception("New version not higher than current version")
                    )
                )
                return false
            }

            AppLogger.d("Installing updated Plugin file: ${pluginItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.INSTALLING_MAIN))
            fileSystem.sink(Platform.getData("plugin") / "pkg" / "${pluginItem.pkgId}.tefpkg")
                .buffer()
                .use {
                    val pkgFileSize = pluginZip.metadataOrNull("plugin.bin".toPath())?.size
                    val pkgFile = pluginZip.source("plugin.bin".toPath())
                    it.write(pkgFile, pkgFileSize ?: 0)
                    it.flush()
                }
            AppLogger.i("Plugin file updated: ${pluginItem.pkgId}")

            AppLogger.d("Updating database for Plugin: ${pluginItem.pkgId}")
            progressCallback?.invoke(ProgressInfo(ProgressStage.UPDATING_DATABASE))
            database.editIf(
                pluginItem.pkgId,
                { pluginItem.versionCode > it.versionCode }
            ) { pluginItem }
            AppLogger.i("Plugin update completed: ${pluginItem.pkgId}")

            true
        } catch (e: Exception) {
            AppLogger.e("Plugin update failed", e)
            progressCallback?.invoke(ProgressInfo(ProgressStage.FAILED, exception = e))
            false
        }
    }

    fun isPluginExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("plugin") / "pkg" / "$pkgId.tefpkg")

    fun isModuleExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("module") / "pkg" / "$pkgId.tefpkg")

    fun isLoaderExists(pkgId: String): Boolean =
        fileSystem.exists(Platform.getData("loader") / "pkg" / "$pkgId.tefpkg")

    fun isModExists(pkgId: String, loaderPkgId: String): Boolean =
        fileSystem.exists(Platform.getData("mod") / loaderPkgId / "mod" / pkgId)

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

    private fun getEnableFilePath(type: String, loaderPkgId: String? = null): Path {
        return if (type == "mod" && loaderPkgId != null) {
            Platform.getData("mod") / loaderPkgId / "enables.txt"
        } else {
            Platform.getData(type) / "enables.txt"
        }
    }
}