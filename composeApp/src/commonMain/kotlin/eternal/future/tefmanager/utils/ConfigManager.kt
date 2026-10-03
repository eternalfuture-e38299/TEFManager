package eternal.future.tefmanager.utils

import eternal.future.tefmanager.BuildConfig
import eternal.future.tefmanager.ConfigurationState.AppConfig
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.concurrent.Volatile

/*******************************************************************************
 * TEFManager - ConfigManager
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
 * Created: 2026/2/5
 *******************************************************************************/

class ConfigManager private constructor() {
    private val json = Json {
        prettyPrint = true // 保存时格式化，便于阅读
        ignoreUnknownKeys = true // 忽略 JSON 中未知的键，提高兼容性
    }

    private val fileSystem = FileSystem.SYSTEM
    private var configFile: Path? = null
    var currentConfig: AppConfig? = null
        private set // 限制外部直接修改

    companion object {
        @Volatile
        private var instance: ConfigManager? = null

        fun getInstance(): ConfigManager {
            return instance ?: ConfigManager().also { instance = it }
        }
    }

    /**
     * 初始化配置管理器
     * @param configDir 配置目录
     * @param fileName 配置文件名（可选，默认为 app_config.json）
     */
    fun initialize(configDir: String, fileName: String = "app_config.json") {
        configFile = configDir.toPath() / fileName
        configFile?.parent?.let { parent ->
            if (!fileSystem.exists(parent)) {
                fileSystem.createDirectories(parent)
            }
        }
        loadConfig()
    }

    /**
     * 加载配置
     */
    fun loadConfig() {
        try {
            configFile?.let { file ->
                if (fileSystem.exists(file)) {
                    fileSystem.source(file).use { source ->
                        val jsonString = source.buffer().readUtf8()
                        currentConfig = json.decodeFromString<AppConfig>(jsonString)
                    }
                } else {
                    AppLogger.d("Config file does not exist, creating default.")
                    currentConfig = AppConfig(kernelVersion = BuildConfig.KERNEL_VERSION)
                    saveConfig() // 保存默认配置
                }
            } ?: run {
                AppLogger.w("Config directory/file not initialized, using default config.")
                currentConfig = AppConfig(kernelVersion = BuildConfig.KERNEL_VERSION)
            }
        } catch (e: Exception) { // 捕获 SerializationException 等
            AppLogger.e(message = "Failed to load or parse config, using default", throwable = e)
            currentConfig = AppConfig(kernelVersion = BuildConfig.KERNEL_VERSION)
        }
    }

    /**
     * 保存配置
     */
    fun saveConfig(): Boolean {
        return try {
            currentConfig?.let { config ->
                configFile?.let { file ->
                    val jsonString = json.encodeToString(AppConfig.serializer(), config)

                    file.parent?.let { parent ->
                        if (!fileSystem.exists(parent)) fileSystem.createDirectories(parent)
                    }

                    fileSystem.sink(file).use { sink ->
                        sink.buffer().use { bufferedSink ->
                            bufferedSink.writeUtf8(jsonString)
                            bufferedSink.flush()
                        }
                    }
                    AppLogger.d("Config saved: ${jsonString.length} bytes to $file")
                    true
                } ?: false
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("Failed to save config", e)
            false
        }
    }

    fun getConfig(): AppConfig {
        return currentConfig?.copy() ?: AppConfig(kernelVersion = BuildConfig.KERNEL_VERSION)
    }

    fun updateConfig(updates: (AppConfig) -> Unit): Boolean {
        return try {
            currentConfig?.let { config ->
                updates(config)
                saveConfig()
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(message = "Failed to update config", throwable = e)
            false
        }
    }
}