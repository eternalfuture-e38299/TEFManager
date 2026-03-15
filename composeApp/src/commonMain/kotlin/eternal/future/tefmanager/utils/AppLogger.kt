package eternal.future.tefmanager.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import eternal.future.tefmanager.ConfigurationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.time.Clock.System.now

/*******************************************************************************
 * TEFManager - AppLogger
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

object AppLogger {

    private const val TAG = "TEFManager"

    private var isInitialized = false

    private var enableFileLogging = false

    private var logDirectory: String? = null
    private var kernelLogDirectory: String? = null

    private var currentLogFile: Path? = null

    private lateinit var logger: Logger
    private val fileSystem = FileSystem.SYSTEM
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fileCheckLock = Mutex()

    // 同步初始化（适用于应用启动时）
    fun initializeSync(
        enableFileLog: Boolean = false,
        logDir: String? = null
    ) {
        if (isInitialized) return

        println("Starting synchronous logger initialization...")

        enableFileLogging = enableFileLog
        logDirectory = logDir
        kernelLogDirectory = "${logDir}_kernel" // 内核日志目录

        // 配置 Kermit Logger
        val config = StaticConfig(
            minSeverity = Severity.Verbose,
            logWriterList = listOf(platformLogWriter())
        )

        logger = Logger(config, TAG)

        // 同步初始化文件日志目录
        if (enableFileLogging) {
            runBlocking {
                initLogDirectories()
                initializeLogFile()
                setupLogCleanup()
            }
        }

        isInitialized = true
        println("Logger initialized synchronously - FileLog: $enableFileLog")

        // 记录初始化完成
        logInternal("I", "Logger initialized - FileLog: $enableFileLog")
    }

    private suspend fun initLogDirectories() = withContext(Dispatchers.IO) {
        try {
            // 初始化应用日志目录
            val appDir = logDirectory?.toPath() ?: "logs".toPath()
            if (!fileSystem.exists(appDir)) {
                fileSystem.createDirectories(appDir)
                println("Created app log directory: $appDir")
            }

            // 测试写入权限
            val testFile = appDir / "test.log"
            fileSystem.sink(testFile).use { sink ->
                sink.buffer().writeUtf8("Test log entry\n").flush()
            }
            fileSystem.delete(testFile)
            println("Log directory test passed")
        } catch (e: Exception) {
            println("Log directory initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun initializeLogFile() = withContext(Dispatchers.IO) {
        if (!enableFileLogging) return@withContext

        try {
            val dir = logDirectory?.toPath() ?: "logs".toPath()
            if (!fileSystem.exists(dir)) {
                return@withContext
            }

            val now = now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

            // 固定文件名（应用启动时间）
            val dateString = buildString {
                append("${localDateTime.year}-")
                append(localDateTime.month.number.toString().padStart(2, '0'))
                append("-")
                append(localDateTime.day.toString().padStart(2, '0'))
                append("-")
                append(localDateTime.hour.toString().padStart(2, '0'))
                append(":")
                append(localDateTime.minute.toString().padStart(2, '0'))
                append(":")
                append(localDateTime.second.toString().padStart(2, '0'))
            }

            currentLogFile = dir / "app_${dateString}.log"
            println("Log file initialized: $currentLogFile")
        } catch (e: Exception) {
            println("Error initializing log file: ${e.message}")
        }
    }

    private suspend fun getCurrentLogFile(): Path? = withContext(Dispatchers.IO) {
        if (!enableFileLogging) return@withContext null
        return@withContext currentLogFile
    }

    private fun getCurrentTime(): String {
        try {
            val now = now()
            val time = now.toLocalDateTime(TimeZone.currentSystemDefault())

            val month = time.month.number.toString().padStart(2, '0')
            val day = time.day.toString().padStart(2, '0')
            val hour = time.hour.toString().padStart(2, '0')
            val minute = time.minute.toString().padStart(2, '0')
            val second = time.second.toString().padStart(2, '0')
            val millis = (time.nanosecond / 1_000_000).toString().padStart(3, '0')

            return "$month-$day $hour:$minute:$second.$millis"
        } catch (_: Exception) {
            return "00-00 00:00:00.000"
        }
    }

    private suspend fun writeToFile(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (!enableFileLogging || !isInitialized) return

        // 检查日志内容是否为空
        if (message.isBlank() && throwable == null) return

        try {
            // 每次写入时都重新获取当前日志文件路径
            val logFile = getCurrentLogFile() ?: return

            val timeStr = getCurrentTime()
            var logLine = "$timeStr $level/$tag: $message"

            throwable?.let {
                logLine += "\n${it.stackTraceToString()}"
            }
            logLine += "\n"

            // 使用 FileHandle 实现追加写入
            fileSystem.openReadWrite(logFile).use { handle ->
                handle.appendingSink().use { sink ->
                    sink.buffer().writeUtf8(logLine).flush()
                }
            }

            // 检查日志文件大小限制
            checkLogFileSize()

        } catch (e: Exception) {
            println("Write Log To File Failed: ${e.message}")
        }
    }

    private fun logInternal(level: String, message: String, throwable: Throwable? = null) {
        if (!isInitialized) {
            println("[$level] Logger not initialized yet: $message: $throwable")
            return
        }

        // 检查日志内容是否为空
        if (message.isBlank() && throwable == null) {
            return
        }

        // 调用 Kermit logger
        when (level) {
            "V" -> logger.v { message }
            "D" -> logger.d { message }
            "I" -> logger.i { message }
            "W" -> logger.w { message }
            "E" -> logger.e(throwable) { message }
        }

        // 写入文件
        scope.launch {
            writeToFile(level, TAG, message, throwable)
        }
    }

    fun v(message: String, throwable: Throwable? = null) {
        logInternal("V", message, throwable)
    }

    fun d(message: String, throwable: Throwable? = null) {
        logInternal("D", message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        logInternal("I", message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        logInternal("W", message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        logInternal("E", message, throwable)
    }

    // 日志管理功能
    suspend fun cleanupOldLogs() = withContext(Dispatchers.IO) {
        try {
            val cleanTimeMinutes = ConfigurationState.autoCleanTime

            // 计算截止时间的时间戳（毫秒）
            val cutoffTimestamp = now().toEpochMilliseconds() -
                    (cleanTimeMinutes * 60 * 1000L)

            // 清理应用日志
            logDirectory?.toPath()?.let { dir ->
                if (fileSystem.exists(dir)) {
                    fileSystem.list(dir).forEach { file ->
                        val fileMetadata = fileSystem.metadata(file)
                        val fileTime = fileMetadata.lastModifiedAtMillis

                        if (fileTime != null) {
                            if (fileTime < cutoffTimestamp) {
                                fileSystem.delete(file)
                                d("Deleted old app log file: $file")
                            }
                        }
                    }
                }
            }

            // 清理内核日志
            kernelLogDirectory?.toPath()?.let { dir ->
                if (fileSystem.exists(dir)) {
                    fileSystem.list(dir).forEach { file ->
                        val fileMetadata = fileSystem.metadata(file)
                        val fileTime = fileMetadata.lastModifiedAtMillis
                        if (fileTime != null) {
                            if (fileTime < cutoffTimestamp) {
                                fileSystem.delete(file)
                                d("Deleted old kernel log file: $file")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e("Failed to cleanup old logs", e)
        }
    }

    private suspend fun checkLogFileSize() = withContext(Dispatchers.IO) {
        fileCheckLock.withLock {  // 添加互斥锁
            try {
                val maxFiles = ConfigurationState.maxAppLogFiles
                val maxSizeMB = ConfigurationState.maxAppLogSizeMB
                val maxSizeBytes = maxSizeMB * 1024 * 1024L

                logDirectory?.toPath()?.let { dir ->
                    if (fileSystem.exists(dir)) {
                        // 先获取所有文件列表
                        val files = fileSystem.list(dir).sortedBy {
                            try {
                                fileSystem.metadata(it).lastModifiedAtMillis
                            } catch (e: Exception) {
                                Long.MAX_VALUE  // 文件不存在时排到最后
                            }
                        }

                        // 检查文件数量限制
                        if (files.size > maxFiles) {
                            val filesToDelete = files.take(files.size - maxFiles)
                            filesToDelete.forEach { file ->
                                try {
                                    if (fileSystem.exists(file)) {  // 再次检查文件是否存在
                                        fileSystem.delete(file)
                                        d("Deleted app log file due to count limit: $file")
                                    }
                                } catch (e: Exception) {
                                    w("Failed to delete file $file: ${e.message}")
                                }
                            }
                        }

                        // 检查总大小限制
                        var totalSize = 0L
                        val validFiles = mutableListOf<Path>()

                        files.forEach { file ->
                            try {
                                if (fileSystem.exists(file)) {  // 检查文件是否存在
                                    val metadata = fileSystem.metadata(file)
                                    totalSize += metadata.size ?: 0
                                    validFiles.add(file)
                                }
                            } catch (e: Exception) {
                                w("Failed to get metadata for $file: ${e.message}")
                            }
                        }

                        if (totalSize > maxSizeBytes) {
                            var sizeToRemove = totalSize - maxSizeBytes
                            validFiles.forEach { file ->
                                if (sizeToRemove > 0) {
                                    try {
                                        if (fileSystem.exists(file)) {
                                            val fileSize = fileSystem.metadata(file).size ?: 0
                                            fileSystem.delete(file)
                                            sizeToRemove -= fileSize
                                            d("Deleted app log file due to size limit: $file")
                                        }
                                    } catch (e: Exception) {
                                        w("Failed to delete file $file: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e("Failed to check log file size", e)
            }
        }
    }

    fun clearAllLogs() {
        try {
            // 清除应用日志
            logDirectory?.toPath()?.let { dir ->
                if (fileSystem.exists(dir)) {
                    fileSystem.deleteRecursively(dir)
                    fileSystem.createDirectories(dir)
                    i("Cleared all app logs")
                }
            }

            // 清除内核日志
            kernelLogDirectory?.toPath()?.let { dir ->
                if (fileSystem.exists(dir)) {
                    fileSystem.deleteRecursively(dir)
                    if (ConfigurationState.kernelLogEnabled) {
                        fileSystem.createDirectories(dir)
                    }
                    i("Cleared all kernel logs")
                }
            }
        } catch (e: Exception) {
            e("Failed to clear logs", e)
        }
    }

    private fun setupLogCleanup() {
        // 定期清理旧日志
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(60 * 60 * 1000L) // 每小时检查一次
                if (ConfigurationState.autoCleanLogs) {
                    cleanupOldLogs()
                }
            }
        }
    }

    fun close() {
        try {
            i("Closing logger...")
            scope.cancel()
            isInitialized = false
        } catch (e: Exception) {
            println("Error closing logger: ${e.message}")
        }
    }
}