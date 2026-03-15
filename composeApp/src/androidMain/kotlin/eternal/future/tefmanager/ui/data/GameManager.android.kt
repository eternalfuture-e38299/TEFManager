package eternal.future.tefmanager.ui.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.ui.model.GameItem
import eternal.future.tefmanager.utils.AppLogger
import java.security.MessageDigest

actual object GameManager {
    // 需要查询的包名列表
    private val targetPackages = listOf(
        "com.and.games505.Terraria",
        "com.and.games505.TerrariaPaid"
    )

    // 元数据键
    private const val META_DATA_KEY = "TEFManager-Patch"

    // 可观察的游戏列表
    private var _games: SnapshotStateList<GameItem> = mutableStateListOf()
    actual val games: SnapshotStateList<GameItem>
        get() = _games

    init {
        AppLogger.d("GameManager initialized")
        _games = loadGamesWithMetaData().toMutableStateList()
    }

    /**
     * 加载具有特定元数据的游戏
     */
    fun loadGamesWithMetaData(): MutableList<GameItem> {
        try {
            val packageManager = MainActivity.context!!.packageManager
            val filteredGames = mutableListOf<GameItem>()

            targetPackages.forEach { packageName ->
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)

                    // 检查是否包含指定的元数据
                    if (hasRequiredMetaData(packageInfo.applicationInfo)) {
                        createGameItemFromPackageInfo(packageInfo)?.let {
                            filteredGames.add(it)
                            AppLogger.i("Find games that meet the criteria: ${packageInfo.packageName}")
                        }
                    }

                } catch (_: PackageManager.NameNotFoundException) {
                    AppLogger.d("App not installed: $packageName")
                } catch (e: Exception) {
                    AppLogger.e("Error occurred while checking the application: $packageName", e)
                }
            }

            // 更新列表

            AppLogger.i("Loaded ${_games.size} games containing TEFManager-Patch metadata")
            return filteredGames
        } catch (e: Exception) {
            AppLogger.e("Failed to load game", e)
        }

        return mutableStateListOf()
    }

    /**
     * 检查应用是否包含指定的元数据
     */
    private fun hasRequiredMetaData(applicationInfo: ApplicationInfo?): Boolean {
        val metaData = applicationInfo?.metaData
        return metaData?.
        getBoolean(META_DATA_KEY, false) ?: false
    }

    /**
     * 从 PackageInfo 创建 GameItem
     */
    private fun createGameItemFromPackageInfo(packageInfo: PackageInfo): GameItem? {
        val applicationInfo: ApplicationInfo = packageInfo.applicationInfo ?: return null
        val packageManager = MainActivity.context!!.packageManager

        // 获取应用名称
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()

        // 获取版本信息
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
            packageInfo.longVersionCode.toInt() else packageInfo.versionCode

        // 只提取关键信息，不计算整个APK大小
        val apkPath = applicationInfo.sourceDir
        val file = java.io.File(apkPath)

        // 获取文件基本信息（快速）
        val fileSize = file.length()
        val lastModified = file.lastModified()

        // 生成基于关键信息的哈希（更快）
        val hash = generateQuickHash(packageInfo.packageName, versionName, versionCode, lastModified, fileSize)

        AppLogger.d("Create GameItem: $appName (${packageInfo.packageName}) v$versionName, size: ${fileSize / 1024}KB")

        return GameItem(
            apkPackName = packageInfo.packageName,
            version = versionName,
            versionCode = versionCode,
            hash = hash
        )
    }

    /**
     * 生成快速哈希（基于关键信息，不读取整个文件）
     */
    private fun generateQuickHash(
        packageName: String,
        versionName: String,
        versionCode: Int,
        lastModified: Long,
        fileSize: Long
    ): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val combinedInfo = "$packageName|$versionName|$versionCode|$lastModified|$fileSize"
            digest.update(combinedInfo.toByteArray())
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            AppLogger.e("Failed to generate quick hash", e)
            "quick_hash_${System.currentTimeMillis()}"
        }
    }

    /**
     * 添加游戏（如果手动添加）
     */
    actual fun addGame(gameItem: GameItem) {
        if (!_games.any { it.apkPackName == gameItem.apkPackName }) {
            _games.add(gameItem)
            AppLogger.i("Game added: ${gameItem.apkPackName}")
        } else {
            AppLogger.d("The game already exists: ${gameItem.apkPackName}")
        }
    }

    /**
     * 根据哈希移除游戏
     */
    actual fun removeGame(hash: String) {
        val removed = _games.removeAll { it.hash == hash }
        if (removed) {
            AppLogger.i("The game has been removed, hash: $hash")
        } else {
            AppLogger.d("Game not found, hash: $hash")
        }
    }

    actual fun refreshGames() {
        AppLogger.d("Refresh game list")
        _games.clear()
        _games.addAll(loadGamesWithMetaData())
    }
}