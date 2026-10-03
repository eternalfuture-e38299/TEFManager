package eternal.future.tefmanager.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import eternal.future.tefmanager.BuildConfig
import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem
import java.io.File
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
     * 加载游戏列表
     * 根据模块激活状态决定是否需要检查元数据
     */
    fun loadGamesWithMetaData(): MutableList<GameItem> {
        if (!BuildConfig.IS_INLINE_GAME) {
            try {
                val packageManager = MainActivity.context!!.packageManager
                val filteredGames = mutableListOf<GameItem>()

                // 检查模块激活状态
                val moduleActive = checkModuleActiveState()
                AppLogger.i("Module active state: $moduleActive")

                targetPackages.forEach { packageName ->
                    try {
                        val packageInfo = packageManager.getPackageInfo(
                            packageName,
                            PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES
                        )

                        val shouldInclude: Boolean

                        if (moduleActive) {
                            // 模块已激活，不需要检查元数据，直接包含
                            shouldInclude = true
                            AppLogger.d("Module active, auto-include: $packageName")
                        } else {
                            // 模块未激活，需要检查元数据
                            shouldInclude = hasRequiredMetaData(packageInfo.applicationInfo)
                            if (shouldInclude) {
                                AppLogger.d("Module inactive, but has metadata: $packageName")
                            } else {
                                AppLogger.d("Module inactive, no metadata: $packageName")
                            }
                        }

                        if (shouldInclude) {
                            createGameItemFromPackageInfo(packageInfo)?.let {
                                filteredGames.add(it)
                                AppLogger.i("Found game: ${packageInfo.packageName} (module active: $moduleActive)")
                            }
                        }

                    } catch (_: PackageManager.NameNotFoundException) {
                        AppLogger.d("App not installed: $packageName")
                    } catch (e: Exception) {
                        AppLogger.e(
                            "Error occurred while checking the application: $packageName",
                            e
                        )
                    }
                }

                AppLogger.i("Loaded ${filteredGames.size} games. Module active: $moduleActive")
                return filteredGames

            } catch (e: Exception) {
                AppLogger.e("Failed to load games", e)
            }
        } else {
            val context = MainActivity.context!!
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES)

            return mutableStateListOf(createGameItemFromPackageInfo(packageInfo)!!.copy(
                version = BuildConfig.INLINE_GAME_VERSION,
                versionCode = BuildConfig.INLINE_GAME_VERSION_CODE
            ))
        }

        return mutableStateListOf()
    }

    /**
     * 检查模块激活状态
     * 先检查字段，再检查方法
     */
    private fun checkModuleActiveState(): Boolean {
        return try {
            // 回退到字段检查
            val isActive = Platform.isAndroidModuleActive

            if (isActive) {
                AppLogger.d("Module is active (via isAndroidModuleActive field)")
            } else {
                AppLogger.d("Module is not active")
            }

            isActive

        } catch (e: Exception) {
            AppLogger.e("Failed to check module activation state", e)
            false
        }
    }

    /**
     * 检查应用是否包含指定的元数据
     */
    private fun hasRequiredMetaData(applicationInfo: ApplicationInfo?): Boolean {
        val metaData = applicationInfo?.metaData
        return metaData?.getBoolean(META_DATA_KEY, false) ?: false
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
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            packageInfo.longVersionCode.toInt() else packageInfo.versionCode

        // 只提取关键信息，不计算整个APK大小
        val apkPath = applicationInfo.sourceDir
        val file = File(apkPath)

        // 获取文件基本信息（快速）
        val fileSize = file.length()
        val lastModified = file.lastModified()

        // 生成基于关键信息的哈希（更快）
        val hash = generateQuickHash(packageInfo.packageName, versionName, versionCode, lastModified, fileSize)

        // 检查模块激活状态来决定是否显示额外信息
        val moduleActive = checkModuleActiveState()

        AppLogger.d("Create GameItem: $appName (${packageInfo.packageName}) v$versionName, " +
                "size: ${fileSize / 1024}KB, module active: $moduleActive")

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

        // 记录当前模块状态
        val moduleActive = checkModuleActiveState()
        AppLogger.i("Game list refreshed. Module active: $moduleActive, Total games: ${_games.size}")
    }

}