package eternal.future.tefmanager.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import eternal.future.tefmanager.storage.GameStorage
import eternal.future.tefmanager.ui.model.GameItem

actual object GameManager {
    private val _games: SnapshotStateList<GameItem> = GameStorage.serializableGames.toMutableStateList()

    // 公开的只读游戏列表
    actual val games: SnapshotStateList<GameItem>
        get() = _games

    actual fun addGame(gameItem: GameItem) {
        games.add(gameItem);
        GameStorage.save(gameItem);
    }

    actual fun removeGame(hash: String) {
        games.removeAll { it.hash == hash }
        GameStorage.delete(hash)
    }


    actual fun refreshGames() {
        AppLogger.d("Refreshing games list")
        // 重新从存储加载
        GameStorage.loadGames()

        // 更新本地列表
        _games.clear()
        _games.addAll(GameStorage.serializableGames)

        AppLogger.i("Games list refreshed, now has ${_games.size} games")
    }
}