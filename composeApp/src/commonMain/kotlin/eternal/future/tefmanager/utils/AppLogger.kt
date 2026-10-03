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
import kotlin.concurrent.Volatile
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.milliseconds

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
    @Volatile
    private var isInitialized = false
    @Volatile private var enableFileLogging = false

    private var logDirectory: String? = null
    private var kernelLogDirectory: String? = null

    // 使用 @Volatile 保证可见性，替代 Mutex 的部分功能
    @Volatile private var currentLogFile: Path? = null

    private lateinit var logger: Logger
    private val fileSystem = FileSystem.SYSTEM
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 同步初始化（适用于应用启动时）
    fun initializeSync(
        enableFileLog: Boolean = false,
        logDir: String? = null
    ) {
        if (isInitialized) return

        println("Starting synchronous logger initialization...")

        enableFileLogging = enableFileLog
        logDirectory = logDir
        kernelLogDirectory = "${logDir}_kernel"

        val config = StaticConfig(
            minSeverity = Severity.Verbose,
            logWriterList = listOf(platformLogWriter())
        )
        logger = Logger(config, TAG)

        if (enableFileLogging) {
            runBlocking {
                initLogDirectories()
                initializeLogFile()
                setupLogCleanup()
            }
        }

        isInitialized = true
        println("Logger initialized synchronously - FileLog: $enableFileLog")
        logInternal("I", "Logger initialized - FileLog: $enableFileLog")
    }

    private suspend fun initLogDirectories() = withContext(Dispatchers.IO) {
        try {
            val appDir = logDirectory?.toPath() ?: "logs".toPath()
            if (!fileSystem.exists(appDir)) {
                fileSystem.createDirectories(appDir)
            }
            val testFile = appDir / "test.log"
            fileSystem.sink(testFile).use { sink ->
                sink.buffer().writeUtf8("Test log entry\n").flush()
            }
            fileSystem.delete(testFile)
        } catch (e: Exception) {
            println("Log directory initialization failed: ${e.message}")
        }
    }

    private suspend fun initializeLogFile() = withContext(Dispatchers.IO) {
        if (!enableFileLogging) return@withContext
        try {
            val dir = logDirectory?.toPath() ?: "logs".toPath()
            if (!fileSystem.exists(dir)) return@withContext

            val time = now().toLocalDateTime(TimeZone.currentSystemDefault())
            // 使用 StringBuilder 拼接文件名，避免格式化函数和冒号
            val dateString = buildString {
                append(time.year).append('-')
                append(time.month.number.toString().padStart(2, '0')).append('-')
                append(time.day.toString().padStart(2, '0')).append('_')
                append(time.hour.toString().padStart(2, '0')).append('-')
                append(time.minute.toString().padStart(2, '0')).append('-')
                append(time.second.toString().padStart(2, '0'))
            }

            currentLogFile = dir / "app_$dateString.log"
        } catch (e: Exception) {
            println("Error initializing log file: ${e.message}")
        }
    }

    private fun getCurrentTime(): String {
        val time = now().toLocalDateTime(TimeZone.currentSystemDefault())
        return buildString {
            append(time.month.number.toString().padStart(2, '0')).append('-')
            append(time.day.toString().padStart(2, '0')).append(' ')
            append(time.hour.toString().padStart(2, '0')).append(':')
            append(time.minute.toString().padStart(2, '0')).append(':')
            append(time.second.toString().padStart(2, '0')).append('.')
            append((time.nanosecond / 1_000_000).toString().padStart(3, '0'))
        }
    }

    private suspend fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!enableFileLogging || !isInitialized) return
        if (message.isBlank() && throwable == null) return

        val file = currentLogFile ?: return

        val logLine = buildString {
            append(getCurrentTime()).append(' ')
            append(level).append('/').append(tag).append(": ")
            append(message)
            if (throwable != null) {
                append('\n').append(throwable.stackTraceToString())
            }
            append('\n')
        }

        try {
            fileSystem.openReadWrite(file).use { handle ->
                handle.appendingSink().buffer().use { sink ->
                    sink.writeUtf8(logLine).flush()
                }
            }
            // 无锁检查大小
            checkLogFileSize()
        } catch (e: Exception) {
            // 防止刷屏，仅打印一次
            println("Write Log To File Failed: ${e.message}")
        }
    }

    private fun logInternal(level: String, message: String, throwable: Throwable? = null) {
        if (!isInitialized) {
            println("[$level] Logger not initialized yet: $message")
            return
        }
        if (message.isBlank() && throwable == null) return

        when (level) {
            "V" -> logger.v { message }
            "D" -> logger.d { message }
            "I" -> logger.i { message }
            "W" -> logger.w(throwable) { message }
            "E" -> logger.e(throwable) { message }
        }
        scope.launch { writeToFile(level, TAG, message, throwable) }
    }

    fun v(message: String) = logInternal("V", message)
    fun d(message: String) = logInternal("D", message)
    fun i(message: String) = logInternal("I", message)
    fun w(message: String, throwable: Throwable? = null) = logInternal("W", message, throwable)
    fun e(message: String, throwable: Throwable? = null) = logInternal("E", message, throwable)

    // 无锁清理：通过过滤 currentLogFile 实现
    private suspend fun checkLogFileSize() = withContext(Dispatchers.IO) {
        val dir = logDirectory?.toPath() ?: return@withContext
        if (!fileSystem.exists(dir)) return@withContext

        val maxFiles = ConfigurationState.maxAppLogFiles
        val maxSizeBytes = ConfigurationState.maxAppLogSizeMB * 1024 * 1024L
        val current = currentLogFile // 取快照，避免并发修改

        val files = fileSystem.list(dir)
            .filter { it != current } // 核心：排除正在写的文件
            .sortedByDescending { fileSystem.metadata(it).lastModifiedAtMillis ?: 0L }

        // 数量控制
        if (files.size > maxFiles) {
            files.drop(maxFiles).forEach { file ->
                runCatching { fileSystem.delete(file) }
            }
        }

        // 大小控制
        var totalSize = files.sumOf { fileSystem.metadata(it).size ?: 0L }
        if (totalSize > maxSizeBytes) {
            var remainingFiles = files
            while (totalSize > maxSizeBytes && remainingFiles.isNotEmpty()) {
                val file = remainingFiles.last()
                val size = fileSystem.metadata(file).size ?: 0L
                if (runCatching { fileSystem.delete(file) }.isSuccess) {
                    totalSize -= size
                }
                remainingFiles = remainingFiles.dropLast(1)
            }
        }
    }

    fun clearAllLogs() {
        try {
            logDirectory?.toPath()?.let {
                if (fileSystem.exists(it)) fileSystem.deleteRecursively(it)
                fileSystem.createDirectories(it)
            }
            kernelLogDirectory?.toPath()?.let {
                if (fileSystem.exists(it)) fileSystem.deleteRecursively(it)
                if (ConfigurationState.kernelLogEnabled) fileSystem.createDirectories(it)
            }
        } catch (e: Exception) {
            println("Failed to clear logs: ${e.message}")
        }
    }

    private fun setupLogCleanup() {
        scope.launch {
            kotlinx.coroutines.delay((60 * 60 * 1000L).milliseconds)
            if (ConfigurationState.autoCleanLogs) {
                runCatching { checkLogFileSize() }
            }
        }
    }

    fun close() {
        try {
            scope.cancel()
            isInitialized = false
        } catch (e: Exception) {
            println("Error closing logger: ${e.message}")
        }
    }
}