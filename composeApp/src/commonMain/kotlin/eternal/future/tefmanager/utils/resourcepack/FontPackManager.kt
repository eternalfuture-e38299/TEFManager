package eternal.future.tefmanager.utils.resourcepack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.ResourcesPackItem
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.LightProtoStore
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.InstallProgress
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.PackAnalysisResult
import eternal.future.tefmanager.utils.resourcepack.ResourcePackManager.ProgressCallback
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use


/*******************************************************************************
 * TEFManager - FontPackManager
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

object FontPackManager {

    private val fileSystem = FileSystem.SYSTEM
    private val logger = AppLogger
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // 数据库
    private val fontPacksDataBase = LightProtoStore(
        Platform.getData("resource_pack") / "font_packs" / "db",
        ResourcesPackItem.serializer(),
        "font_packs"
    )

    // 包列表
    private val _fontPacks = mutableStateListOf<ResourcesPackItem>()
    val fontPacks: List<ResourcesPackItem> = _fontPacks

    // 当前选中的包文件名
    var selectedPackFileName by mutableStateOf<String?>(null)
    
    // 配置文件（只存储选中的文件名）
    private val configFile = Platform.getData("module") / "private" / "eternal.future.fontpack" / "selected.json"

    /**
     * 安装字体包
     */
    suspend fun installFontPack(
        resourcesPackItem: ResourcesPackItem
    ) {
        fontPacksDataBase.put(resourcesPackItem.fileName, resourcesPackItem)
        fontPacksDataBase.flush()

        // 刷新内存列表
        refreshFontPacksList()
    }

    /**
     * 分析字体包类型和元数据
     */
    fun analyzeFontPack(
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
                    packType = ResourcesPackItem.PackType.FontPack
                )
            }

            return null
        } catch (e: Exception) {
            logger.e("Failed to analyze FontPack", e)
            return null
        }
    }

    /**
     * 刷新字体包列表
     */
    private fun refreshFontPacksList() {
        val allPacks = fontPacksDataBase.getAllValues()
        _fontPacks.clear()
        _fontPacks.addAll(allPacks)

        // 读取选中的文件名
        selectedPackFileName = readSelectedPack()
    }

    /**
     * 选中一个字体包（会取消之前的选中）
     */
    fun selectFontPack(packFileName: String) {
        if (!_fontPacks.any { it.fileName == packFileName }) {
            logger.w("Font pack not found: $packFileName")
            return
        }

        writeSelectedPack(packFileName)
        selectedPackFileName = packFileName
        refreshFontPacksList()
    }

    /**
     * 取消选中
     */
    fun deselectFontPack() {
        writeSelectedPack(null)
        selectedPackFileName = null
        refreshFontPacksList()
    }

    /**
     * 获取当前选中的字体包
     */
    fun getSelectedFontPack(): ResourcesPackItem? {
        return selectedPackFileName?.let { fileName ->
            _fontPacks.find { it.fileName == fileName }
        }
    }

    /**
     * 检查字体包是否被选中
     */
    fun isFontPackSelected(packFileName: String): Boolean {
        return selectedPackFileName == packFileName
    }

    /**
     * 删除字体包（如果被选中则自动取消选中）
     */
    suspend fun deleteFontPack(fileName: String): Boolean {
        logger.d("Deleting FontPack: $fileName")
        return try {
            // 如果是当前选中的包，取消选中
            if (selectedPackFileName == fileName) {
                deselectFontPack()
            }

            fontPacksDataBase.delete(fileName)
            fontPacksDataBase.flush()

            val packFile = Platform.getData("resource_pack") / "font_packs" / fileName
            if (fileSystem.exists(packFile)) {
                fileSystem.delete(packFile)
            }

            val packId = fileName.removeSuffix(".zip")
            val iconFile = Platform.getData("resource_pack") / "font_packs" / "icons" / "$packId.png"
            if (fileSystem.exists(iconFile)) {
                fileSystem.delete(iconFile)
            }

            refreshFontPacksList()

            logger.i("FontPack deleted successfully: $fileName")
            true
        } catch (e: Exception) {
            logger.e("Failed to delete FontPack: $fileName", e)
            false
        }
    }

    /**
     * 读取选中的字体包文件名
     */
    private fun readSelectedPack(): String? {
        if (!fileSystem.exists(configFile)) {
            return null
        }

        return try {
            fileSystem.source(configFile).buffer().use { source ->
                val content = source.readUtf8().trim()
                if (content.isBlank() || content == "null") {
                    null
                } else {
                    content.removeSurrounding("\"")
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to read selected font pack", e)
            null
        }
    }

    /**
     * 写入选中的字体包文件名
     */
    private fun writeSelectedPack(fileName: String?) {
        try {
            val parentDir = configFile.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            fileSystem.sink(configFile).buffer().use { bufferedSink ->
                val content = if (fileName != null) {
                    "\"$fileName\""
                } else {
                    "null"
                }
                bufferedSink.writeUtf8(content)
                bufferedSink.flush()
            }
        } catch (e: Exception) {
            logger.e("Failed to write selected font pack", e)
        }
    }

    /**
     * 初始化
     */
    fun initialize() {
        logger.d("Initializing FontPackManager")
        refreshFontPacksList()
        logger.d("FontPackManager initialized with ${_fontPacks.size} font packs, selected: $selectedPackFileName")
    }
}