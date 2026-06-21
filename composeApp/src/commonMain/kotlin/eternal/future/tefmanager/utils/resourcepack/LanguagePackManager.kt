package eternal.future.tefmanager.utils.resourcepack

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.ResourcesPackItem
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.LightProtoStore
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.InstallProgress
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.PackAnalysisResult
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.ProgressCallback
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

/*******************************************************************************
 * TEFManager - LanguagePackManager
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
 * Created: 2026/6/21
 *******************************************************************************/

object LanguagePackManager {

    private val fileSystem = FileSystem.SYSTEM
    private val logger = AppLogger
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // 数据库
    private val languagePacksDataBase = LightProtoStore(
        Platform.getData("resource_pack") / "language_packs" / "db",
        ResourcesPackItem.serializer(),
        "language_packs"
    )

    // 包列表
    private val _languagePacks = mutableStateListOf<ResourcesPackItem>()
    val languagePacks: List<ResourcesPackItem> = _languagePacks

    // 启用的包映射 (fileName -> enable)
    private val _enabledPacks = mutableStateMapOf<String, Boolean>()
    val enabledPacks: Map<String, Boolean> = _enabledPacks

    // 配置文件
    private val configFile = Platform.getData("module") / "private" / "eternal.future.languagepack" / "config.json"

    // 最大启用数量（根据平台）
    val maxEnabledCount: Int
        get() = if (Platform.isAndroid) 7 else 14

    @Serializable
    private data class PackConfig(
        val file: String,
        val enable: Boolean,
        val priority: Int
    )

    /**
     * 安装语言包
     */
    suspend fun installLanguagePack(
        resourcesPackItem: ResourcesPackItem
    ) {
        languagePacksDataBase.put(resourcesPackItem.fileName, resourcesPackItem)
        languagePacksDataBase.flush()

        // 刷新内存列表
        refreshLanguagePacksList()

        // 创建默认配置（默认启用，如果未达到上限）
        ensurePackConfig(resourcesPackItem.fileName)
    }

    /**
     * 分析语言包类型和元数据
     */
    fun analyzeLanguagePack(
        zip: FileSystem,
        progressCallback: ProgressCallback? = null
    ): PackAnalysisResult? {
        try {

            // 检查 TEFManager 格式 (pack_info.json)
            val packInfoPath = "pack_info.json".toPath()
            if (zip.exists(packInfoPath)) {
                progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

                val packInfoContent = zip.source(packInfoPath).buffer().use { it.readUtf8() }
                val packInfoJson = json.parseToJsonElement(packInfoContent).jsonObject

                val name = packInfoJson["name"]?.jsonPrimitive?.content
                if (name == null) {
                    logger.e("pack_info.json missing 'name' field")
                    return null
                }

                val author = packInfoJson["author"]?.jsonPrimitive?.content ?: "Unknown"
                val description = packInfoJson["description"]?.jsonPrimitive?.content ?: ""
                val version = packInfoJson["version"]?.jsonPrimitive?.content ?: "1.0.0"

                return PackAnalysisResult(
                    name = name,
                    author = author,
                    description = description,
                    version = version,
                    type = ResourcesPackItem.Type.TEFManager,
                    packType = ResourcesPackItem.PackType.LanguagePack
                )
            }

            return null
        } catch (e: Exception) {
            logger.e("Failed to analyze LanguagePack", e)
            return null
        }
    }

    /**
     * 刷新语言包列表
     */
    private fun refreshLanguagePacksList() {
        val allPacks = languagePacksDataBase.getAllValues()
        _languagePacks.clear()
        _languagePacks.addAll(allPacks)

        // 读取配置
        val configs = readConfigs()
        _enabledPacks.clear()
        configs.forEach { config ->
            _enabledPacks[config.file] = config.enable
        }

        // 按优先级排序
        if (configs.isNotEmpty()) {
            _languagePacks.sortBy { pack ->
                configs.indexOfFirst { it.file == pack.fileName }.takeIf { it != -1 } ?: Int.MAX_VALUE
            }
        }
    }

    /**
     * 确保语言包有配置
     */
    private fun ensurePackConfig(fileName: String) {
        val configs = readConfigs()
        if (configs.none { it.file == fileName }) {
            // 检查是否已达到最大启用数量
            val enabledCount = configs.count { it.enable }
            val shouldEnable = enabledCount < maxEnabledCount

            val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
            configs.add(PackConfig(fileName, shouldEnable, maxPriority + 1))
            writeConfigs(configs)
        }
    }

    /**
     * 启用/禁用语言包
     * 启用时会检查是否达到上限
     */
    fun setPackEnabled(packFileName: String, enabled: Boolean): Boolean {
        val configs = readConfigs().toMutableList()
        val index = configs.indexOfFirst { it.file == packFileName }

        if (index == -1) {
            logger.w("Language pack not found: $packFileName")
            return false
        }

        val currentConfig = configs[index]

        // 如果状态没变，直接返回
        if (currentConfig.enable == enabled) {
            return true
        }

        // 如果要启用，检查是否达到上限
        if (enabled) {
            val currentEnabledCount = configs.count { it.enable }
            if (currentEnabledCount >= maxEnabledCount) {
                logger.w("Cannot enable more language packs. Max: $maxEnabledCount")
                return false
            }
        }

        // 更新配置
        configs[index] = currentConfig.copy(enable = enabled)
        writeConfigs(configs)
        refreshLanguagePacksList()
        return true
    }

    /**
     * 删除语言包
     */
    suspend fun deleteLanguagePack(fileName: String): Boolean {
        logger.d("Deleting LanguagePack: $fileName")
        return try {
            languagePacksDataBase.delete(fileName)
            languagePacksDataBase.flush()

            val packFile = Platform.getData("resource_pack") / "language_packs" / fileName
            if (fileSystem.exists(packFile)) {
                fileSystem.delete(packFile)
            }

            val packId = fileName.removeSuffix(".zip")
            val iconFile = Platform.getData("resource_pack") / "language_packs" / "icons" / "$packId.png"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            // 从配置中删除
            val configs = readConfigs().filter { it.file != fileName }.toMutableList()
            writeConfigs(configs)

            refreshLanguagePacksList()

            logger.i("LanguagePack deleted successfully: $fileName")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete LanguagePack: $fileName", e)
            false
        }
    }

    /**
     * 移动优先级
     */
    fun movePackPriority(packFileName: String, moveUp: Boolean) {
        val configs = readConfigs().sortedBy { it.priority }.toMutableList()
        val index = configs.indexOfFirst { it.file == packFileName }

        if (index != -1) {
            val newIndex = if (moveUp) index - 1 else index + 1
            if (newIndex in configs.indices) {
                val currentPriority = configs[index].priority
                val targetPriority = configs[newIndex].priority
                configs[index] = configs[index].copy(priority = targetPriority)
                configs[newIndex] = configs[newIndex].copy(priority = currentPriority)

                configs.sortBy { it.priority }
                writeConfigs(configs)
                refreshLanguagePacksList()
            }
        }
    }

    /**
     * 获取语言包启用状态
     */
    fun isPackEnabled(packFileName: String): Boolean {
        return _enabledPacks[packFileName] ?: false
    }

    /**
     * 获取当前启用的语言包数量
     */
    fun getEnabledCount(): Int {
        return _enabledPacks.count { it.value }
    }


    /**
     * 检查是否可以启用更多语言包
     */
    fun canEnableMore(): Boolean {
        return getEnabledCount() < maxEnabledCount
    }

    /**
     * 读取配置文件
     */
    private fun readConfigs(): MutableList<PackConfig> {
        if (!fileSystem.exists(configFile)) {
            return mutableListOf()
        }

        return try {
            fileSystem.source(configFile).buffer().use { source ->
                val content = source.readUtf8()
                if (content.isBlank()) {
                    mutableListOf()
                } else {
                    json.decodeFromString<List<PackConfig>>(content).toMutableList()
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to read configs", e)
            mutableListOf()
        }
    }

    /**
     * 写入配置文件
     */
    private fun writeConfigs(configs: List<PackConfig>) {
        try {
            val parentDir = configFile.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            fileSystem.sink(configFile).buffer().use { bufferedSink ->
                val jsonString = json.encodeToString(configs.sortedBy { it.priority })
                bufferedSink.writeUtf8(jsonString)
                bufferedSink.flush()
            }
        } catch (e: Exception) {
            logger.e("Failed to write configs", e)
        }
    }

    /**
     * 初始化
     */
    fun initialize() {
        logger.d("Initializing LanguagePackManager")
        refreshLanguagePacksList()

        // 确保所有包都有配置
        val configs = readConfigs()
        val configFiles = configs.map { it.file }.toSet()

        _languagePacks.forEach { pack ->
            if (!configFiles.contains(pack.fileName)) {
                val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
                val enabledCount = configs.count { it.enable }
                configs.add(PackConfig(pack.fileName, enabledCount < maxEnabledCount, maxPriority + 1))
            }
        }

        if (configs.size != _languagePacks.size) {
            writeConfigs(configs)
        }

        logger.d("LanguagePackManager initialized with ${_languagePacks.size} packs, enabled: ${getEnabledCount()}/$maxEnabledCount")
    }
}