package eternal.future.tefmanager.utils.addon

import androidx.compose.runtime.mutableStateListOf
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.AddonConfig
import eternal.future.tefmanager.utils.addon.AddonManager.ProgressCallback
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.LightProtoStore
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.synth.kmpzip.okio.asSource
import no.synth.kmpzip.zip.ZipFile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

/*******************************************************************************
 * TEFManager - BaseManager
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

abstract class BaseManager<T>(
    val config: AddonConfig,
    val serializer: KSerializer<T>
) {
    protected val fileSystem = FileSystem.SYSTEM
    protected val logger = AppLogger
    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    protected val packsDataBase = LightProtoStore(
        config.dir / "db",
        serializer,
        "data"
    )

    // 包列表
    val packs = mutableStateListOf<T>()

    init {
        refreshPacksList()
    }

    /**
     * 安装包
     */
    suspend fun install(
        zip: ZipFile,
        manifest: JsonObject,
        progressCallback: ProgressCallback? = null,
    ) {
        AddonManager.installDependence(zip, manifest, progressCallback)

        val typeName = getTypeName()
        logger.d("[$typeName] install started")

        progressCallback?.invoke(AddonManager.InstallProgress.PARSING_METADATA, null)
        val targetFilePath = manifest["file"]?.jsonPrimitive?.content ?: run {
            val error = IllegalArgumentException("Module manifest missing 'file' field")
            logger.e("[$typeName] ${error.message}")
            progressCallback?.invoke(AddonManager.InstallProgress.ERROR, error)
            return
        }

        val infoEntry = zip.getEntry("Info.json")
        if (infoEntry == null) {
            val error = IllegalArgumentException("Zip missing 'Info.json' file")
            logger.e("[$typeName] ${error.message}")
            progressCallback?.invoke(AddonManager.InstallProgress.ERROR, error)
            return
        }

        val infoContent = zip.getInputStream(infoEntry).asSource().buffer().readUtf8()

        val pkgId = json.parseToJsonElement(infoContent).jsonObject["pkgId"]?.jsonPrimitive?.content!!
        val item = json.decodeFromString(serializer,
            infoContent
        )

        val targetEntry = zip.getEntry(targetFilePath)
        if (targetEntry == null) {
            val error = IllegalArgumentException("Zip missing '$targetEntry' file")
            logger.e("[$typeName] ${error.message}")
            progressCallback?.invoke(AddonManager.InstallProgress.ERROR, error)
            return
        }

        zip.getInputStream(targetEntry).asSource().use { source ->
            progressCallback?.invoke(AddonManager.InstallProgress.STARTING, null)

            val filePath = getPkgFilePath(pkgId).also {
                it.parent?.let { dir ->
                    fileSystem.createDirectories(dir)
                }
            }

            progressCallback?.invoke(AddonManager.InstallProgress.COPYING_FILES, null)
            fileSystem.sink(filePath).buffer().use { sink ->
                source.buffer().use { buffer ->
                    sink.writeAll(buffer)
                    sink.flush()
                }
            }

            progressCallback?.invoke(AddonManager.InstallProgress.EXTRACTING_ICON, null)
            AddonManager.extractIcon(getIconFilePath(pkgId), zip, manifest)

            progressCallback?.invoke(AddonManager.InstallProgress.INSTALLING_DEPENDENCIES, null)
            AddonManager.installDependence(zip, manifest, progressCallback)
            AddonManager.extractResources(zip, manifest, config.dir / "private" / pkgId, progressCallback)

            progressCallback?.invoke(AddonManager.InstallProgress.UPDATING_DATABASE, null)

            packsDataBase.put(pkgId, item)
            packsDataBase.flush()

            // 刷新内存列表
            refreshPacksList()

            logger.i("[$typeName] $pkgId installed successfully")
        }
    }

    /**
     * 刷新包列表
     */
    fun refreshPacksList() {
        val allPacks = packsDataBase.getAllValues()
        packs.clear()
        packs.addAll(allPacks)
    }

    private fun getEnableFilePath(): Path = config.dir / "enables.txt"

    protected fun getPkgFilePath(pkgId: String): Path = config.dir / config.type.getPkgDirName() / (pkgId + if (config.type != AddonConfig.AddonType.Mod) ".tefpkg" else "")

    fun getIconFilePath(pkgId: String): Path = config.dir / "icons" / "$pkgId.icon"

    private fun getTypeName(): String = config.type.name

    /**
     * 启用一个附加组件
     */
    fun enable(pkgId: String): Boolean {
        val typeName = getTypeName()
        return try {
            val enableFile = getEnableFilePath()

            // 检查是否已启用
            if (isEnabled(pkgId)) {
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

            logger.d("[$typeName] Enabled: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("[$typeName] Failed to enable: $pkgId", e)
            false
        }
    }

    /**
     * 禁用一个附加组件
     */
    fun disable(pkgId: String): Boolean {
        val typeName = getTypeName()
        return try {
            val enableFile = getEnableFilePath()

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
                logger.d("[$typeName] Disabled: $pkgId")
                true
            } else {
                fileSystem.delete(tempFile)
                true
            }
        } catch (e: Exception) {
            logger.e("[$typeName] Failed to disable: $pkgId", e)
            false
        }
    }

    /**
     * 检查附加组件是否启用
     */
    fun isEnabled(pkgId: String): Boolean {
        val typeName = getTypeName()
        return try {
            val enableFile = getEnableFilePath()

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
            logger.e("[$typeName] Failed to check if enabled: $pkgId", e)
            false
        }
    }

    /**
     * 删除附加组件
     * @param pkgId 包ID
     * @return 是否成功删除
     */
    suspend fun delete(pkgId: String): Boolean {
        val typeName = getTypeName()
        logger.d("[$typeName] Deleting: $pkgId")
        return try {
            val item = packsDataBase.get(pkgId)
            if (item == null) {
                logger.w("[$typeName] Not found: $pkgId")
                return false
            }

            // 从数据库删除
            packsDataBase.delete(pkgId)

            if (config.type == AddonConfig.AddonType.ModLoader) {
                AddonManager.closeModManager(pkgId)
                fileSystem.deleteRecursively(Platform.getData("mods") / pkgId)
            }

            // 删除包文件
            val pkgFile = getPkgFilePath(pkgId)
            if (fileSystem.exists(pkgFile)) {
                fileSystem.delete(pkgFile)
            }

            // 删除图标文件
            val iconFile = getIconFilePath(pkgId)
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            if (fileSystem.exists(config.dir / "private" / pkgId)) {
                fileSystem.deleteRecursively(config.dir / "private" / pkgId)
            }

            // 禁用
            disable(pkgId)

            // 刷新UI
            refreshPacksList()

            logger.i("[$typeName] Deleted successfully: $pkgId")
            true
        } catch (e: Exception) {
            logger.e("[$typeName] Failed to delete: $pkgId", e)
            false
        }
    }


    fun getPackItem(pkgId: String) : T? {
        return packsDataBase.get(pkgId)
    }

    /**
     * 销毁管理器
     */
    fun destroy() {
        val typeName = getTypeName()
        logger.d("[$typeName] Destroying manager...")
        packs.clear()
        packsDataBase.destroy()
        logger.i("[$typeName] Manager destroyed")
    }
}