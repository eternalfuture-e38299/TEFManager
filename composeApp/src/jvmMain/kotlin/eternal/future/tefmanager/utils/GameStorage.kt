package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.buffer
import okio.use

@OptIn(ExperimentalSerializationApi::class)
object GameStorage {
    private val dataDir = Platform.getData(null)
    private val gamesFile = dataDir / "games.json"

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // 用于序列化的普通列表
    var serializableGames = mutableListOf<GameItem>()

    init {
        AppLogger.d("Initializing GameStorage with JSON...")
        loadGames()
    }

    fun loadGames() {
        try {
            val fileSystem = FileSystem.SYSTEM
            if (fileSystem.exists(gamesFile)) {
                fileSystem.source(gamesFile).use { source ->
                    val buffer = source.buffer()
                    val jsonString = buffer.readUtf8()
                    AppLogger.d("Loaded ${jsonString.length} characters from JSON file")

                    if (jsonString.isNotEmpty()) {
                        serializableGames = json.decodeFromString<List<GameItem>>(jsonString).toMutableList()
                        AppLogger.i("Successfully loaded ${serializableGames.size} games from JSON file")
                    } else {
                        AppLogger.w("JSON file exists but is empty")
                        clearBothLists()
                    }
                }
            } else {
                AppLogger.i("JSON file not found, starting with empty list")
                clearBothLists()
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to load games from JSON", e)
            clearBothLists()
        }
    }

    private fun clearBothLists() {
        serializableGames.clear()
    }

    private fun saveAllGames() {
        try {
            AppLogger.d("Encoding ${serializableGames.size} games to JSON...")

            // 检查数据是否为空
            if (serializableGames.isEmpty()) {
                AppLogger.w("No games to save, list is empty")
                return
            }

            val jsonString = json.encodeToString(serializableGames)
            AppLogger.d("Encoded ${jsonString.length} characters")

            // 确保目录存在
            gamesFile.parent?.let { parentDir ->
                if (!FileSystem.SYSTEM.exists(parentDir)) {
                    FileSystem.SYSTEM.createDirectories(parentDir)
                }
            }

            // 使用更明确的写入方式
            FileSystem.SYSTEM.sink(gamesFile).use { sink ->
                val bufferedSink = sink.buffer()
                bufferedSink.writeUtf8(jsonString)
                bufferedSink.flush() // 确保立即刷新
            }

            // 验证文件大小
            val fileSize = FileSystem.SYSTEM.metadata(gamesFile).size
            AppLogger.i("Saved ${serializableGames.size} games to JSON file (${fileSize} bytes)")

        } catch (e: Exception) {
            AppLogger.e("Failed to save games to JSON", e)
            throw e
        }
    }

    private fun syncToUi() {
        AppLogger.d("Synced ${serializableGames.size} games to UI")
    }

    fun save(game: GameItem?) {
        game?.let { gameItem ->
            AppLogger.d("Saving game: ${gameItem.apkPackName} (${gameItem.hash})")

            // 更新序列化列表
            val existingIndex = serializableGames.indexOfFirst { it.hash == gameItem.hash }
            if (existingIndex != -1) {
                serializableGames[existingIndex] = gameItem
            } else {
                serializableGames.add(gameItem)
            }

            // 保存到文件
            saveAllGames()

            // 同步到 UI
            syncToUi()

            AppLogger.i("Game saved successfully: ${gameItem.apkPackName}")
        }
    }

    fun delete(hash: String?) {
        hash?.let { itemHash ->
            AppLogger.d("Deleting game with hash: $itemHash")

            // 从序列化列表中删除
            val removedFromSerializable = serializableGames.removeAll { it.hash == itemHash }

            if (removedFromSerializable) {
                // 保存到文件
                saveAllGames()

                // 同步到 UI
                syncToUi()

                AppLogger.i("Game deleted successfully: $itemHash")
            } else {
                AppLogger.w("Game not found for deletion: $itemHash")
            }
        }
    }
}