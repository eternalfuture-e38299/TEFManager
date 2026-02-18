package eternal.future.tefmanager.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use

/*******************************************************************************
 * TEFManager - NetworkService
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
 * Created: 2026/2/15
 *******************************************************************************/

expect fun openUrl(url: String): Boolean

class NetworkService {
    enum class DownloadStatus {
        IDLE, DOWNLOADING, COMPLETED, ERROR, CANCELLED
    }

    // 进度数据类
    data class DownloadProgress(
        val currentBytes: Long = 0,
        val totalBytes: Long = 0,
        val percentage: Float = 0f,
        val status: DownloadStatus = DownloadStatus.IDLE,
        val error: Throwable? = null
    )

    // 进度状态流
    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    // 协程管理
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentDownloadJob: Job? = null
    private var isCancelled = false

    // 创建 HTTP 客户端
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000000
            connectTimeoutMillis = 10000000
            socketTimeoutMillis = 30000000
        }

        defaultRequest {
            header(HttpHeaders.UserAgent, "TEFManager-App/1.0")
        }
    }

    /**
     * 下载单个文件
     */
    fun downloadFile(
        url: String,
        outputPath: Path
    ) {
        // 取消之前的下载
        cancelDownload()

        // 重置状态
        isCancelled = false
        updateProgress(DownloadProgress(status = DownloadStatus.DOWNLOADING))

        AppLogger.i("Starting download: $url to $outputPath")

        // 在协程作用域中启动下载
        currentDownloadJob = scope.launch {
            try {
                performDownload(url, outputPath)
            } catch (e: CancellationException) {
                AppLogger.i("Download was cancelled")
                updateProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
            } catch (e: Exception) {
                AppLogger.e("Download failed: ${e.message}", e)
                updateProgress(DownloadProgress(
                    status = DownloadStatus.ERROR,
                    error = e
                ))
            }
        }
    }

    /**
     * 实际下载逻辑
     */
    private suspend fun performDownload(
        url: String,
        outputPath: Path
    ): Boolean {
        // 创建本地取消标志，用于所有取消检查
        var localCancelled = false

        return try {
            val response: HttpResponse = client.get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    // 检查取消状态
                    if (localCancelled || isCancelled) {
                        AppLogger.d("Download cancelled, stopping progress updates")
                        return@onDownload
                    }

                    val percentage = if (contentLength != null && contentLength > 0) {
                        (bytesSentTotal.toFloat() / contentLength * 100)
                    } else 0f

                    AppLogger.v("Download progress: $bytesSentTotal/$contentLength ($percentage%)")

                    updateProgress(DownloadProgress(
                        currentBytes = bytesSentTotal,
                        totalBytes = contentLength ?: 0L,
                        percentage = percentage,
                        status = DownloadStatus.DOWNLOADING
                    ))
                }
            }

            // 检查取消状态
            if (isCancelled) {
                AppLogger.i("Download cancelled before processing response")
                localCancelled = true
                return false
            }

            if (response.status.value in 200..299) {
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
                AppLogger.i("HTTP response OK, content length: $contentLength bytes")

                // 创建输出目录
                val parentDir = outputPath.parent
                if (parentDir != null && !FileSystem.SYSTEM.exists(parentDir)) {
                    AppLogger.d("Creating directory: $parentDir")
                    FileSystem.SYSTEM.createDirectories(parentDir)
                }

                // 使用 Okio 进行流式写入
                FileSystem.SYSTEM.sink(outputPath).use { sink ->
                    val buffer = sink.buffer()
                    val channel = response.bodyAsChannel()

                    val chunkSize = 8192
                    var totalWritten = 0L

                    while (!channel.isClosedForRead && !localCancelled && !isCancelled) {
                        val packet = channel.readRemaining(chunkSize.toLong())
                        val bytes = packet.readByteArray()

                        // 检查取消状态
                        if (localCancelled || isCancelled) {
                            AppLogger.i("Download cancelled during file write")
                            break
                        }

                        buffer.write(bytes)
                        buffer.flush()
                        totalWritten += bytes.size

                        // 更新进度
                        if (contentLength > 0) {
                            val percentage = (totalWritten.toFloat() / contentLength * 100)
                            updateProgress(DownloadProgress(
                                currentBytes = totalWritten,
                                totalBytes = contentLength,
                                percentage = percentage,
                                status = DownloadStatus.DOWNLOADING
                            ))
                        }
                    }
                }

                // 如果被取消，清理文件并返回
                if (isCancelled || localCancelled) {
                    AppLogger.i("Download was cancelled, cleaning up partial file")
                    if (FileSystem.SYSTEM.exists(outputPath)) {
                        FileSystem.SYSTEM.delete(outputPath)
                        AppLogger.d("Deleted partial file: $outputPath")
                    }
                    return false
                }

                // 验证下载完整性
                val finalSize = FileSystem.SYSTEM.metadata(outputPath).size
                if (contentLength > 0 && finalSize != contentLength) {
                    AppLogger.e("Download incomplete: $finalSize/$contentLength bytes")
                    throw IllegalStateException("Download incomplete: $finalSize/$contentLength bytes")
                }

                AppLogger.i("Download completed successfully: $finalSize bytes")
                updateProgress(DownloadProgress(
                    currentBytes = finalSize ?: 0,
                    totalBytes = contentLength,
                    percentage = 100f,
                    status = DownloadStatus.COMPLETED
                ))

                true
            } else {
                AppLogger.e("HTTP error: ${response.status}")
                throw IllegalStateException("HTTP error: ${response.status}")
            }
        } catch (e: Exception) {
            if (isCancelled) {
                AppLogger.i("Download cancelled with exception: ${e.message}")
                localCancelled = true
                throw CancellationException("Download was cancelled")
            } else {
                throw e
            }
        }
    }

    /**
     * 真正取消下载
     */
    fun cancelDownload() {
        if (isCancelled) return

        AppLogger.i("Cancelling download...")
        isCancelled = true

        // 取消协程
        currentDownloadJob?.cancel("User cancelled download")
        currentDownloadJob = null

        updateProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
    }

    /**
     * 更新进度
     */
    private fun updateProgress(progress: DownloadProgress) {
        // 如果已经取消，避免不必要的状态更新
        if (isCancelled && progress.status != DownloadStatus.CANCELLED) {
            AppLogger.d("Ignoring progress update after cancellation: ${progress.status}")
            return
        }

        AppLogger.v("Updating progress: ${progress.status} - ${progress.percentage}%")
        _downloadProgress.value = progress
    }

    /**
     * 清除进度记录
     */
    fun clearProgress() {
        AppLogger.d("Clearing download progress")
        isCancelled = false
        currentDownloadJob = null
        _downloadProgress.value = DownloadProgress()
    }

    /**
     * 获取当前下载进度
     */
    fun getProgress(): DownloadProgress {
        return _downloadProgress.value
    }

    fun close() {
        AppLogger.i("Closing NetworkService")
        cancelDownload()
        client.close()
    }
}