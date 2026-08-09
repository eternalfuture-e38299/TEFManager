package eternal.future.tefmanager.utils.resourcepack

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.ResourcesPackItem
import eternal.future.tefmanager.utils.AppLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.synth.kmpzip.okio.ZipFile
import no.synth.kmpzip.okio.asSource
import no.synth.kmpzip.zip.ZipFile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.time.Clock

/*******************************************************************************
 * TEFManager - ResourcePackManager
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

object ResourcePackManager {

    data class PackManagerConfig(
        val name: String,                          // 管理器名称
        val packType: ResourcesPackItem.PackType,  // 包类型
        val configName: String,                      // 配置文件名称
        val packSubDir: String,
        val packName: String                     // 模块包名
    )

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

    suspend fun installPack(
        filePath: Path,
        progressCallback: ProgressCallback? = null
    ): Result<ResourcesPackItem> {
        logger.d("========== Starting resource pack installation ==========")
        logger.d("File path: $filePath")
        logger.d("File name: ${filePath.name}")
        logger.d("File size: ${fileSystem.metadata(filePath).size} bytes")
        progressCallback?.invoke(InstallProgress.STARTING, null)

        var zip: ZipFile? = null

        return try {
            logger.d("Opening ZIP package...")
            progressCallback?.invoke(InstallProgress.OPENING_PACKAGE, null)
            zip = ZipFile(fileSystem.openReadOnly(filePath))
            logger.d("ZIP package opened successfully")

            logger.d("Reading manifest file...")
            progressCallback?.invoke(InstallProgress.READING_MANIFEST, null)

            logger.d("Analyzing resource pack type...")
            val packInfo = analyzePack(zip, progressCallback)
            if (packInfo == null) {
                logger.e("Unrecognized resource pack format")
                return Result.failure(IllegalArgumentException("Unrecognized resource pack format: missing pack.json or Settings.json"))
            }
            logger.d("Resource pack analysis completed:")
            logger.d("  - Name: ${packInfo.name}")
            logger.d("  - Author: ${packInfo.author}")
            logger.d("  - Version: ${packInfo.version}")
            logger.d("  - Type: ${packInfo.type}")
            logger.d("  - Pack Type: ${packInfo.packType}")

            progressCallback?.invoke(InstallProgress.CHECKING_EXISTING, null)

            val targetDir = when (packInfo.packType) {
                ResourcesPackItem.PackType.TexturePack ->
                    Platform.getData("module") / "private" / "eternal.future.texturepackextension" / "texture_packs"
                ResourcesPackItem.PackType.LanguagePack ->
                    Platform.getData("module") / "private" / "eternal.future.languagepackextension" / "language_packs"
                ResourcesPackItem.PackType.LanguagePatchPack ->
                    Platform.getData("module") / "private" / "eternal.future.languagepackextension" / "language_patch_packs"
                ResourcesPackItem.PackType.AudioPack ->
                    Platform.getData("module") / "private" / "eternal.future.audiopackextension" / "audio_packs"
                ResourcesPackItem.PackType.FontPack ->
                    Platform.getData("module") / "private" / "eternal.future.fontpackextension" / "font_packs"
            }
            logger.d("Target directory: $targetDir")

            if (!fileSystem.exists(targetDir)) {
                logger.d("Target directory does not exist, creating: $targetDir")
                fileSystem.createDirectories(targetDir)
            }

            // Use timestamp for unique filename
            val timestamp = Clock.System.now()
            val originalFileName = filePath.name
            val fileExtension = if (originalFileName.contains('.')) {
                originalFileName.substringAfterLast('.')
            } else {
                "zip"
            }
            val targetFileName = "pack_$timestamp.$fileExtension"
            logger.d("Generated unique filename: $targetFileName (based on timestamp: $timestamp)")

            val targetPath = targetDir / targetFileName
            logger.d("Target file path: $targetPath")

            progressCallback?.invoke(InstallProgress.COPYING_FILES, null)
            logger.d("Copying file from $filePath to $targetPath")
            fileSystem.copy(filePath, targetPath)
            logger.d("File copy completed, size: ${fileSystem.metadata(targetPath).size} bytes")

            progressCallback?.invoke(InstallProgress.EXTRACTING_ICON, null)
            logger.d("Extracting icon...")
            val iconPackId = "pack_$timestamp"
            val iconPath = extractIcon(zip, iconPackId, targetPath.parent!!)
            if (iconPath != null) {
                logger.d("Icon extracted successfully: $iconPath")
            } else {
                logger.d("No icon found or extraction failed")
            }

            progressCallback?.invoke(InstallProgress.UPDATING_DATABASE, null)
            logger.d("Creating resource pack data object...")

            val resourcesPackItem = ResourcesPackItem(
                name = packInfo.name,
                author = packInfo.author,
                description = packInfo.description,
                version = packInfo.version,
                fileName = targetFileName,
                iconPath = iconPath ?: "",
                type = packInfo.type,
                packType = packInfo.packType
            )
            logger.d("Resource pack data: ${resourcesPackItem.name} (${resourcesPackItem.packType})")

            logger.d("Calling corresponding manager for installation...")
            when (packInfo.packType) {
                ResourcesPackItem.PackType.TexturePack -> {
                    logger.d("Installing to TexturePackManager")
                    TexturePackManager.installPack(resourcesPackItem, packInfo)
                }
                ResourcesPackItem.PackType.LanguagePack -> {
                    logger.d("Installing to LanguagePackManager")
                    LanguagePackManager.installLanguagePack(resourcesPackItem)
                }
                ResourcesPackItem.PackType.LanguagePatchPack -> {
                    logger.d("Installing to LanguagePatchPackManager")
                    LanguagePatchPackManager.installPack(resourcesPackItem, packInfo)
                }
                ResourcesPackItem.PackType.FontPack -> {
                    logger.d("Installing to FontPackManager")
                    FontPackManager.installFontPack(resourcesPackItem)
                }
                ResourcesPackItem.PackType.AudioPack -> {
                    logger.d("Installing to AudioManager")
                    AudioManager.installPack(resourcesPackItem, packInfo)
                }
            }
            logger.d("Manager installation completed")

            progressCallback?.invoke(InstallProgress.FINISHING, null)
            logger.d("========== Resource pack installed successfully ==========")
            progressCallback?.invoke(InstallProgress.COMPLETED, null)

            Result.success(resourcesPackItem)
        } catch (e: Exception) {
            logger.e("Failed to install resource pack", e)
            logger.e("Error message: ${e.message}")
            logger.e("Stack trace: ${e.stackTraceToString()}")
            progressCallback?.invoke(InstallProgress.ERROR, e)
            Result.failure(e)
        } finally {
            zip?.close()
            logger.d("ZIP package closed")
        }
    }

    private fun analyzePack(
        zip: ZipFile,
        progressCallback: ProgressCallback?
    ): PackAnalysisResult? {
        logger.d("Starting resource pack analysis...")
        progressCallback?.invoke(InstallProgress.PARSING_METADATA, null)

        val hasContent = zip.hasDirectory("Content")
        val hasModified = zip.hasDirectory("Modified")
        logger.d("Content directory exists: $hasContent")
        logger.d("Modified directory exists: $hasModified")

        var packType: ResourcesPackItem.PackType?

        if (hasContent) {
            logger.d("Detected Content directory structure")
            val contentPath = "Content".toPath()

            // 检查是否存在 Music 或 Images 文件夹
            val musicFolder = contentPath.resolve("Music").toString()
            val hasMusicFolder = zip.hasDirectory(musicFolder)
            logger.d("Music folder exists: $hasMusicFolder")

            val imagesFolder = contentPath.resolve("Images").toString()
            val hasImagesFolder = zip.hasDirectory(imagesFolder)
            logger.d("Images folder exists: $hasImagesFolder")

            packType = when {
                hasMusicFolder -> {
                    logger.d("Detected as AudioPack (has Music folder)")
                    ResourcesPackItem.PackType.AudioPack
                }
                hasImagesFolder -> {
                    logger.d("Detected as TexturePack (has Images folder)")
                    ResourcesPackItem.PackType.TexturePack
                }
                else -> {
                    logger.d("No Music or Images folder found in Content")
                    null
                }
            }
        } else if (hasModified) {
            logger.d("Detected Modified directory structure")

            // 检查是否存在 .audio 文件
            val hasAudioFiles = hasFilesWithExtension(zip, "audio")
            logger.d("Has audio files: $hasAudioFiles")

            // 检查是否存在 .texture 文件
            val hasTextureFiles = hasFilesWithExtension(zip, "texture")
            logger.d("Has texture files: $hasTextureFiles")

            packType = when {
                hasAudioFiles -> {
                    logger.d("Detected as AudioPack (has .audio files)")
                    ResourcesPackItem.PackType.AudioPack
                }
                hasTextureFiles -> {
                    logger.d("Detected as TexturePack (has .texture files)")
                    ResourcesPackItem.PackType.TexturePack
                }
                else -> {
                    logger.d("No .audio or .texture files found in Modified")
                    null
                }
            }
        } else {
            // 没有 Content/Modified，尝试解析 pack_info.json
            logger.d("No Content/Modified directories, trying pack_info.json")

            val tefManagerEntry = zip.getEntry("pack_info.json")
            if (tefManagerEntry == null) {
                logger.d("pack_info.json not found")
                return null
            }

            logger.d("Reading pack_info.json...")
            val tefManagerJson = try {
                zip.getInputStream(tefManagerEntry).asSource().buffer().use { it.readUtf8() }
            } catch (e: Exception) {
                logger.e("Failed to read pack_info.json", e)
                return null
            }

            try {
                val typeStr = json.parseToJsonElement(tefManagerJson).jsonObject["type"]?.jsonPrimitive?.content?.lowercase()
                logger.d("Parsed type: $typeStr")
                packType = when (typeStr) {
                    "audiopack" -> {
                        logger.d("Detected as AudioPack")
                        ResourcesPackItem.PackType.AudioPack
                    }
                    "texturepack" -> {
                        logger.d("Detected as TexturePack")
                        ResourcesPackItem.PackType.TexturePack
                    }
                    "languagepack" -> {
                        logger.d("Detected as LanguagePack")
                        ResourcesPackItem.PackType.LanguagePack
                    }
                    "languagepatchpack" -> {
                        logger.d("Detected as LanguagePatchPack")
                        ResourcesPackItem.PackType.LanguagePatchPack
                    }
                    "fontpack" -> {
                        logger.d("Detected as FontPack")
                        ResourcesPackItem.PackType.FontPack
                    }
                    else -> {
                        logger.d("Unknown type: $typeStr")
                        null
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to parse pack_info.jsonpack_info.json", e)
                return null
            }
        }

        if (packType == null) {
            logger.d("Could not determine pack type")
            return null
        }

        logger.d("Pack type determined: $packType")

        val result = when (packType) {
            ResourcesPackItem.PackType.TexturePack -> {
                logger.d("Calling TexturePackManager.analyzePack")
                TexturePackManager.analyzePack(zip, progressCallback)
            }
            ResourcesPackItem.PackType.AudioPack -> {
                logger.d("Calling AudioManager.analyzePack")
                AudioManager.analyzePack(zip, progressCallback)
            }
            ResourcesPackItem.PackType.LanguagePatchPack -> {
                logger.d("Calling LanguagePatchPackManager.analyzePack")
                LanguagePatchPackManager.analyzePack(zip, progressCallback)
            }
            ResourcesPackItem.PackType.FontPack -> {
                logger.d("Calling FontPackManager.analyzeFontPack")
                FontPackManager.analyzeFontPack(zip)
            }
            ResourcesPackItem.PackType.LanguagePack -> {
                logger.d("Calling LanguagePackManager.analyzeLanguagePack")
                LanguagePackManager.analyzeLanguagePack(zip)
            }
        }

        if (result != null) {
            logger.d("Analysis completed: ${result.name} (${result.packType})")
        } else {
            logger.d("Analysis failed: null result")
        }

        return result
    }

    private fun hasFilesWithExtension(zip: ZipFile, extension: String): Boolean {
        return try {
            zip.entries.any { file ->
                file.name.endsWith(".$extension", ignoreCase = true) &&
                        !file.isDirectory
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun ZipFile.hasDirectory(dirName: String): Boolean {
        val dir = dirName.trimEnd('/')
        return entries.any { it.name == dir || it.name == "$dir/" || it.name.startsWith("$dir/") }
    }

    private fun extractIcon(zip: ZipFile, packId: String, targetDir: Path): String? {
        return try {
            logger.d("Extracting icon for pack: $packId")
            val iconEntry = zip.getEntry("Icon.png")
            if (iconEntry == null) {
                logger.d("No icon found in resource pack")
                return null
            }

            val iconDir = targetDir / "icons"
            if (!fileSystem.exists(iconDir)) {
                logger.d("Creating icon directory: $iconDir")
                fileSystem.createDirectories(iconDir)
            }

            val targetIconPath = iconDir / "$packId.png"
            logger.d("Target icon path: $targetIconPath")

            zip.getInputStream(iconEntry).asSource().use { source ->
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

    data class PackAnalysisResult(
        val name: String,
        val author: String,
        val description: String,
        val version: String,
        val type: ResourcesPackItem.Type,
        val packType: ResourcesPackItem.PackType
    )
}