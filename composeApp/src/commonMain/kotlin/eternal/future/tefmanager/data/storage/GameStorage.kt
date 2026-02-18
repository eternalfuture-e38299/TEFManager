package eternal.future.tefmanager.data.storage

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.model.GameItem
import eternal.future.tefmanager.utils.AppLogger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import okio.*

@OptIn(ExperimentalSerializationApi::class)
object GameStorage {
    private val dataDir = Platform.getData(null)
    private val gamesFile = dataDir / "games.pb"

    private val protobuf = ProtoBuf {
        encodeDefaults = true
    }

    // 用于序列化的普通列表
    private var serializableGames = mutableListOf<GameItem>()

    // 用于 UI 的状态列表
    private val _uiGames: SnapshotStateList<GameItem> = mutableStateListOf()
    val uiGames: List<GameItem> get() = _uiGames

    init {
        AppLogger.d("Initializing GameStorage with ProtoBuf...")
        loadGames()
    }

    private fun loadGames() {
        try {
            val fileSystem = FileSystem.SYSTEM
            if (fileSystem.exists(gamesFile)) {
                fileSystem.source(gamesFile).use { source ->
                    val buffer = source.buffer()
                    val bytes = buffer.readByteArray()
                    AppLogger.d("Loaded ${bytes.size} bytes from ProtoBuf file")

                    if (bytes.isNotEmpty()) {
                        serializableGames = protobuf.decodeFromByteArray<List<GameItem>>(bytes).toMutableList()
                        // 同步到 UI 列表
                        _uiGames.clear()
                        _uiGames.addAll(serializableGames)
                        AppLogger.i("Successfully loaded ${serializableGames.size} games from ProtoBuf file")
                    } else {
                        AppLogger.w("ProtoBuf file exists but is empty")
                        clearBothLists()
                    }
                }
            } else {
                AppLogger.i("ProtoBuf file not found, starting with empty list")
                clearBothLists()
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to load games from ProtoBuf", e)
            clearBothLists()
        }
    }

    private fun clearBothLists() {
        serializableGames.clear()
        _uiGames.clear()
    }

    private fun saveAllGames() {
        try {
            AppLogger.d("Encoding ${serializableGames.size} games to ProtoBuf...")

            // 检查数据是否为空
            if (serializableGames.isEmpty()) {
                AppLogger.w("No games to save, list is empty")
                return
            }

            val bytes = protobuf.encodeToByteArray(serializableGames)
            AppLogger.d("Encoded ${bytes.size} bytes")

            // 确保目录存在
            gamesFile.parent?.let { parentDir ->
                if (!FileSystem.SYSTEM.exists(parentDir)) {
                    FileSystem.SYSTEM.createDirectories(parentDir)
                }
            }

            // 使用更明确的写入方式
            FileSystem.SYSTEM.sink(gamesFile).use { sink ->
                val bufferedSink = sink.buffer()
                bufferedSink.write(bytes)
                bufferedSink.flush() // 确保立即刷新
            }

            // 验证文件大小
            val fileSize = FileSystem.SYSTEM.metadata(gamesFile).size
            AppLogger.i("Saved ${serializableGames.size} games to ProtoBuf file (${fileSize} bytes)")

        } catch (e: Exception) {
            AppLogger.e("Failed to save games to ProtoBuf", e)
            throw e
        }
    }

    private fun syncToUi() {
        // 同步序列化列表到 UI 列表
        _uiGames.clear()
        _uiGames.addAll(serializableGames)
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

    fun refreshUi() {
        syncToUi()
    }
}