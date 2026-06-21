package eternal.future.tefmanager.utils.resourcepack

import androidx.compose.runtime.mutableStateListOf
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
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

/*******************************************************************************
 * TEFManager - BasePackManager
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

data class PackManagerConfig(
    val name: String,                          // 管理器名称
    val packType: ResourcesPackItem.PackType,  // 包类型
    val dbPath: Path,                          // 数据库路径
    val configPath: Path,                      // 配置文件路径
    val packSubDir: String                     // 包存放子目录
)

// ==================== 基类 ====================

abstract class BasePackManager(
    private val config: PackManagerConfig
) {

    private val fileSystem = FileSystem.SYSTEM
    protected val logger = AppLogger
    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // 数据库
    private val packsDataBase = LightProtoStore(
        config.dbPath,
        ResourcesPackItem.serializer(),
        config.name
    )

    // 包列表
    private val _packs = mutableStateListOf<ResourcesPackItem>()
    val packs: List<ResourcesPackItem> = _packs

    // 配置文件
    private val configFile = config.configPath

    @Serializable
    private data class PackConfig(
        val file: String,
        val enable: Boolean,
        val priority: Int,
        val type: Int  // 0: Terraria, 1: TLPro
    )

    /**
     * 安装包
     */
    suspend fun installPack(
        resourcesPackItem: ResourcesPackItem,
        packInfo: PackAnalysisResult
    ) {
        packsDataBase.put(resourcesPackItem.fileName, resourcesPackItem)
        packsDataBase.flush()

        // 刷新内存列表
        refreshPacksList()

        // 创建默认配置（如果不存在）
        ensurePackConfig(resourcesPackItem.fileName, packInfo.type)
    }

    /**
     * 分析包类型和元数据
     */
    fun analyzePack(
        zip: FileSystem,
        progressCallback: ProgressCallback? = null
    ): PackAnalysisResult? {
        try {
            // 检查 TLPro 格式 (Settings.json)
            val tlproSettingsPath = "Settings.json".toPath()
            if (zip.exists(tlproSettingsPath)) {
                logger.d("Detected TLPro format ${config.packType.displayName}")
                progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

                val settingsContent = zip.source(tlproSettingsPath).buffer().use { it.readUtf8() }
                val settingsJson = json.parseToJsonElement(settingsContent).jsonObject

                val title = settingsJson["title"]?.jsonPrimitive?.content ?: "Unknown"
                val description = settingsJson["descriptionEnglish"]?.jsonPrimitive?.content ?: ""
                val version = settingsJson["version"]?.jsonPrimitive?.content ?: "1"

                // 解析作者信息
                var author = "Unknown"
                val authorsArray = settingsJson["authors"]?.jsonArray
                if (!authorsArray.isNullOrEmpty()) {
                    val firstAuthor = authorsArray[0].jsonObject
                    author = firstAuthor["name"]?.jsonPrimitive?.content ?: "Unknown"
                }

                return PackAnalysisResult(
                    name = title,
                    author = author,
                    description = description,
                    version = version,
                    type = ResourcesPackItem.Type.TLPro,
                    packType = config.packType
                )
            }

            // 检查 Terraria 格式 (pack.json)
            val terrariaPackPath = "pack.json".toPath()
            if (zip.exists(terrariaPackPath)) {
                logger.d("Detected Terraria format ${config.packType.displayName}")
                progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

                val packContent = zip.source(terrariaPackPath).buffer().use { it.readUtf8() }
                val packJson = json.parseToJsonElement(packContent).jsonObject

                val name = packJson["Name"]?.jsonPrimitive?.content ?: "Unknown"
                val author = packJson["Author"]?.jsonPrimitive?.content ?: "Unknown"
                val description = packJson["Description"]?.jsonPrimitive?.content ?: ""

                val versionObj = packJson["Version"]?.jsonObject
                val version = if (versionObj != null) {
                    val major = versionObj["major"]?.jsonPrimitive?.content ?: "0"
                    val minor = versionObj["minor"]?.jsonPrimitive?.content ?: "0"
                    "$major.$minor"
                } else {
                    "1.0"
                }

                return PackAnalysisResult(
                    name = name,
                    author = author,
                    description = description,
                    version = version,
                    type = ResourcesPackItem.Type.Terraria,
                    packType = config.packType
                )
            }

            // 检查 TEFManager 格式 (pack_info.json)
            val packInfoPath = "pack_info.json".toPath()
            if (zip.exists(packInfoPath)) {
                logger.d("Detected pack_info.json format ${config.packType.displayName}")
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
                    packType = config.packType
                )
            }

            return null
        } catch (e: Exception) {
            logger.e("Failed to analyze ${config.packType.displayName}", e)
            return null
        }
    }

    /**
     * 刷新包列表
     */
    protected fun refreshPacksList() {
        val allPacks = packsDataBase.getAllValues()
        _packs.clear()
        _packs.addAll(allPacks)

        // 根据配置文件排序
        val configs = readConfigs()
        if (configs.isNotEmpty()) {
            _packs.sortBy { pack ->
                configs.indexOfFirst { it.file == pack.fileName }.takeIf { it != -1 }
                    ?: Int.MAX_VALUE
            }
        }
    }

    /**
     * 确保包有配置
     */
    private fun ensurePackConfig(fileName: String, type: ResourcesPackItem.Type) {
        val configs = readConfigs()
        if (configs.none { it.file == fileName }) {
            val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
            val typeValue = when (type) {
                ResourcesPackItem.Type.Terraria -> 0
                ResourcesPackItem.Type.TLPro -> 1
                ResourcesPackItem.Type.TEFManager -> 2
            }
            configs.add(PackConfig(fileName, false, maxPriority + 1, typeValue))
            writeConfigs(configs)
        }
    }

    /**
     * 删除包
     */
    suspend fun deletePack(fileName: String): Boolean {
        logger.d("Deleting ${config.packType.displayName}: $fileName")
        return try {
            // 从数据库删除
            packsDataBase.delete(fileName)
            packsDataBase.flush()

            // 删除文件
            val packFile = Platform.getData("resource_pack") / config.packSubDir / fileName
            if (fileSystem.exists(packFile)) {
                fileSystem.delete(packFile)
            }

            // 删除图标
            val packId = fileName.removeSuffix(".zip")
            val iconFile =
                Platform.getData("resource_pack") / config.packSubDir / "icons" / "$packId.png"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            // 从配置中删除
            val configs = readConfigs().filter { it.file != fileName }.toMutableList()
            writeConfigs(configs)

            // 刷新列表
            refreshPacksList()

            logger.i("${config.packType.displayName} deleted successfully: $fileName")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete ${config.packType.displayName}: $fileName", e)
            false
        }
    }

    /**
     * 启用/禁用包
     */
    fun setPackEnabled(packFileName: String, enabled: Boolean) {
        val configs = readConfigs()
        val index = configs.indexOfFirst { it.file == packFileName }

        if (index != -1) {
            configs[index] = configs[index].copy(enable = enabled)
        } else {
            // 如果配置不存在，尝试从数据库获取类型
            val pack = packsDataBase.get(packFileName)
            val typeValue = pack?.type?.let {
                when (it) {
                    ResourcesPackItem.Type.Terraria -> 0
                    ResourcesPackItem.Type.TLPro -> 1
                    ResourcesPackItem.Type.TEFManager -> 2
                }
            } ?: 0
            val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
            configs.add(PackConfig(packFileName, enabled, maxPriority + 1, typeValue))
        }

        writeConfigs(configs)
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

                // 同步列表顺序
                syncListOrderWithConfigs()
            }
        }
    }

    /**
     * 同步列表顺序
     */
    private fun syncListOrderWithConfigs() {
        val configs = readConfigs().sortedBy { it.priority }
        _packs.sortBy { pack ->
            configs.indexOfFirst { it.file == pack.fileName }.takeIf { it != -1 } ?: Int.MAX_VALUE
        }
    }

    /**
     * 获取包启用状态
     */
    fun isPackEnabled(packFileName: String): Boolean {
        return readConfigs().find { it.file == packFileName }?.enable ?: false
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
        logger.d("Initializing ${config.packType.displayName}Manager")
        refreshPacksList()

        // 确保所有包都有配置
        val configs = readConfigs()
        val configFiles = configs.map { it.file }.toSet()

        _packs.forEach { pack ->
            if (!configFiles.contains(pack.fileName)) {
                val typeValue = when (pack.type) {
                    ResourcesPackItem.Type.Terraria -> 0
                    ResourcesPackItem.Type.TLPro -> 1
                    ResourcesPackItem.Type.TEFManager -> 2
                }
                val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
                configs.add(PackConfig(pack.fileName, true, maxPriority + 1, typeValue))
            }
        }

        if (configs.size != _packs.size) {
            writeConfigs(configs)
        }

        logger.d("${config.packType.displayName}Manager initialized with ${_packs.size} packs")
    }

    /**
     * 获取包数量
     */
    fun getPackCount(): Int = _packs.size

    /**
     * 获取已启用的包
     */
    fun getEnabledPacks(): List<ResourcesPackItem> {
        val configs = readConfigs()
        val enabledFiles = configs.filter { it.enable }.map { it.file }.toSet()
        return _packs.filter { it.fileName in enabledFiles }
    }
}

// ==================== PackType 扩展 ====================

private val ResourcesPackItem.PackType.displayName: String
    get() = when (this) {
        ResourcesPackItem.PackType.AudioPack -> "AudioPack"
        ResourcesPackItem.PackType.TexturePack -> "TexturePack"
        ResourcesPackItem.PackType.LanguagePack -> "LanguagePack"
        ResourcesPackItem.PackType.LanguagePatchPack -> "LanguagePatchPack"
        ResourcesPackItem.PackType.FontPack -> "FontPack"
    }