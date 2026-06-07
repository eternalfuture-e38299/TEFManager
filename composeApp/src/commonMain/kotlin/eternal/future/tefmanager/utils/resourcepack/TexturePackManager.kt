package eternal.future.tefmanager.utils.resourcepack

import androidx.compose.runtime.mutableStateListOf
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.TexturePackItem
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.LightProtoStore
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
import okio.openZip
import okio.use

/*******************************************************************************
 * TEFManager - TexturePackManager
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
 * Created: 2026/6/7
 *******************************************************************************/

object TexturePackManager {

    enum class InstallProgress {
        STARTING,
        OPENING_PACKAGE,
        READING_MANIFEST,
        PARSING_METADATA,
        CHECKING_EXISTING,
        COPYING_FILES,
        EXTRACTING_ICON,
        UPDATING_DATABASE,
        FINISHING,
        COMPLETED,
        ERROR
    }

    typealias ProgressCallback = (progress: InstallProgress, error: Throwable?) -> Unit

    private val fileSystem = FileSystem.SYSTEM
    private val logger = AppLogger
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val texturePacksDataBase = LightProtoStore(
        Platform.getData("resource_pack") / "texture_packs" / "db",
        TexturePackItem.serializer(), "texture_packs"
    )

    private val _texturePacks = mutableStateListOf<TexturePackItem>()
    val texturePacks: List<TexturePackItem> = _texturePacks

    private val configFile = Platform.getData("module") / "private" / "eternal.future.texturepack" / "config.json"

    @Serializable
    private data class PackConfig(
        val file: String,
        val enable: Boolean,
        val priority: Int,
        val type: Int  // 0: Terraria, 1: TLPro
    )

    /**
     * 安装材质包
     * @param filePath ZIP文件路径
     * @param progressCallback 进度回调
     */
    suspend fun installTexturePack(
        filePath: Path,
        progressCallback: ProgressCallback? = null
    ): Result<TexturePackItem> {
        logger.d("Starting installTexturePack for file: $filePath")
        progressCallback?.invoke(InstallProgress.STARTING, null)

        var zip: FileSystem? = null

        return try {
            progressCallback?.invoke(InstallProgress.OPENING_PACKAGE, null)
            zip = fileSystem.openZip(filePath)

            progressCallback?.invoke(InstallProgress.READING_MANIFEST, null)

            // 检测材质包类型并解析
            val packInfo = analyzeTexturePack(zip, progressCallback)
                ?: return Result.failure(IllegalArgumentException("无法识别的材质包格式：缺少 pack.json 或 Settings.json"))

            progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)

            // 确保目标目录存在
            val targetDir = Platform.getData("resource_pack") / "texture_packs"
            if (!fileSystem.exists(targetDir)) {
                fileSystem.createDirectories(targetDir)
            }

            // 生成唯一文件名
            val originalFileName = filePath.name
            var targetFileName = originalFileName
            var counter = 1
            while (fileSystem.exists(targetDir / targetFileName)) {
                val baseName = originalFileName.removeSuffix(".zip")
                targetFileName = "${baseName}_${counter}.zip"
                counter++
            }
            val targetPath = targetDir / targetFileName

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            // 复制文件
            fileSystem.copy(filePath, targetPath)

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            // 提取图标
            val iconPath = extractIcon(zip, targetFileName.removeSuffix(".zip"))

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            // 创建材质包数据
            val texturePackItem = TexturePackItem(
                name = packInfo.name,
                author = packInfo.author,
                description = packInfo.description,
                version = packInfo.version,
                fileName = targetFileName,
                iconPath = iconPath ?: "",
                type = packInfo.type
            )

            // 保存到数据库
            texturePacksDataBase.put(texturePackItem.fileName, texturePackItem)
            texturePacksDataBase.flush()

            // 刷新内存列表
            refreshTexturePacksList()

            // 创建默认配置（如果不存在）
            ensurePackConfig(texturePackItem.fileName, packInfo.type)

            progressCallback?.invoke(InstallProgress.FINISHING, null)
            logger.i("Texture pack installed successfully: ${texturePackItem.name}")
            progressCallback?.invoke(InstallProgress.COMPLETED, null)

            Result.success(texturePackItem)
        } catch (e: Exception) {
            logger.e("Failed to install texture pack", e)
            progressCallback?.invoke(InstallProgress.ERROR, e)
            Result.failure(e)
        } finally {
            zip?.close()
        }
    }

    /**
     * 分析材质包类型和元数据
     */
    private fun analyzeTexturePack(
        zip: FileSystem,
        progressCallback: ProgressCallback? = null
    ): PackAnalysisResult? {
        try {
            // 检查 TLPro 格式 (Settings.json)
            val tlproSettingsPath = "Settings.json".toPath()
            if (zip.exists(tlproSettingsPath)) {
                logger.d("Detected TLPro format texture pack")
                progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

                val settingsContent = zip.source(tlproSettingsPath).buffer().use { it.readUtf8() }
                val settingsJson = json.parseToJsonElement(settingsContent).jsonObject

                val title = settingsJson["title"]?.jsonPrimitive?.content ?: "Unknown"
                val description = settingsJson["descriptionEnglish"]?.jsonPrimitive?.content ?: ""
                val version = settingsJson["version"]?.jsonPrimitive?.content?.let { "v$it" } ?: "v1"

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
                    type = TexturePackItem.Type.TLPro
                )
            }

            // 检查 Terraria 格式 (pack.json)
            val terrariaPackPath = "pack.json".toPath()
            if (zip.exists(terrariaPackPath)) {
                logger.d("Detected Terraria format texture pack")
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
                    "v$major.$minor"
                } else {
                    "v1.0"
                }

                return PackAnalysisResult(
                    name = name,
                    author = author,
                    description = description,
                    version = version,
                    type = TexturePackItem.Type.Terraria
                )
            }

            return null
        } catch (e: Exception) {
            logger.e("Failed to analyze texture pack", e)
            return null
        }
    }

    /**
     * 提取图标
     */
    private fun extractIcon(zip: FileSystem, packId: String): String? {
        return try {
            val iconPathInZip = "Icon.png".toPath()
            if (!zip.exists(iconPathInZip)) {
                logger.d("No icon found in texture pack")
                return null
            }

            val iconDir = Platform.getData("resource_pack") / "texture_packs" / "icons"
            if (!fileSystem.exists(iconDir)) {
                fileSystem.createDirectories(iconDir)
            }

            val targetIconPath = iconDir / "$packId.png"

            zip.source(iconPathInZip).use { source ->
                fileSystem.sink(targetIconPath).use { sink ->
                    source.buffer().use { bufferedSource ->
                        sink.buffer().use { bufferedSink ->
                            bufferedSink.writeAll(bufferedSource)
                        }
                    }
                }
            }

            logger.d("Icon extracted to: $targetIconPath")
            targetIconPath.toString()
        } catch (e: Exception) {
            logger.e("Failed to extract icon", e)
            null
        }
    }

    /**
     * 刷新材质包列表
     */
    private fun refreshTexturePacksList() {
        val allPacks = texturePacksDataBase.getAllValues()
        _texturePacks.clear()
        _texturePacks.addAll(allPacks)

        // 根据配置文件排序
        val configs = readConfigs()
        if (configs.isNotEmpty()) {
            _texturePacks.sortBy { pack ->
                configs.indexOfFirst { it.file == pack.fileName }.takeIf { it != -1 } ?: Int.MAX_VALUE
            }
        }
    }

    /**
     * 确保材质包有配置
     */
    private fun ensurePackConfig(fileName: String, type: TexturePackItem.Type) {
        val configs = readConfigs()
        if (configs.none { it.file == fileName }) {
            val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
            val typeValue = when (type) {
                TexturePackItem.Type.Terraria -> 0
                TexturePackItem.Type.TLPro -> 1
            }
            configs.add(PackConfig(fileName, false, maxPriority + 1, typeValue))
            writeConfigs(configs)
        }
    }

    /**
     * 删除材质包
     */
    suspend fun deleteTexturePack(fileName: String): Boolean {
        logger.d("Deleting texture pack: $fileName")
        return try {
            // 从数据库删除
            texturePacksDataBase.delete(fileName)
            texturePacksDataBase.flush()

            // 删除文件
            val packFile = Platform.getData("resource_pack") / "texture_packs" / fileName
            if (fileSystem.exists(packFile)) {
                fileSystem.delete(packFile)
            }

            // 删除图标
            val packId = fileName.removeSuffix(".zip")
            val iconFile = Platform.getData("resource_pack") / "texture_packs" / "icons" / "$packId.png"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            // 从配置中删除
            val configs = readConfigs().filter { it.file != fileName }.toMutableList()
            writeConfigs(configs)

            // 刷新列表
            refreshTexturePacksList()

            logger.i("Texture pack deleted successfully: $fileName")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete texture pack: $fileName", e)
            false
        }
    }

    /**
     * 启用/禁用材质包
     */
    fun setPackEnabled(packFileName: String, enabled: Boolean) {
        val configs = readConfigs()
        val index = configs.indexOfFirst { it.file == packFileName }

        if (index != -1) {
            configs[index] = configs[index].copy(enable = enabled)
        } else {
            // 如果配置不存在，尝试从数据库获取类型
            val pack = texturePacksDataBase.get(packFileName)
            val typeValue = pack?.type?.let {
                when (it) {
                    TexturePackItem.Type.Terraria -> 0
                    TexturePackItem.Type.TLPro -> 1
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
        _texturePacks.sortBy { pack ->
            configs.indexOfFirst { it.file == pack.fileName }.takeIf { it != -1 } ?: Int.MAX_VALUE
        }
    }

    /**
     * 获取材质包启用状态
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
    suspend fun initialize() {
        logger.d("Initializing TexturePackManager")
        refreshTexturePacksList()

        // 确保所有材质包都有配置
        val configs = readConfigs()
        val configFiles = configs.map { it.file }.toSet()

        _texturePacks.forEach { pack ->
            if (!configFiles.contains(pack.fileName)) {
                val typeValue = when (pack.type) {
                    TexturePackItem.Type.Terraria -> 0
                    TexturePackItem.Type.TLPro -> 1
                }
                val maxPriority = configs.maxOfOrNull { it.priority } ?: -1
                configs.add(PackConfig(pack.fileName, true, maxPriority + 1, typeValue))
            }
        }

        if (configs.size != _texturePacks.size) {
            writeConfigs(configs)
        }

        logger.d("TexturePackManager initialized with ${_texturePacks.size} packs")
    }

    /**
     * 刷新
     */
    suspend fun reload() {
        logger.d("Reloading TexturePackManager")
        refreshTexturePacksList()
        initialize()
    }

    /**
     * 分析结果
     */
    private data class PackAnalysisResult(
        val name: String,
        val author: String,
        val description: String,
        val version: String,
        val type: TexturePackItem.Type
    )
}