package eternal.future.tefmanager.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.contracts.ExperimentalContracts
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/*******************************************************************************
 * TEFManager - LightProtoStore (Fixed Index & Metadata Saving)
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
 * Created: 2026/2/17
 *******************************************************************************/

@OptIn(ExperimentalContracts::class, ExperimentalSerializationApi::class)
class LightProtoStore<T>(
    private val dataDir: Path,
    private val serializer: KSerializer<T>,
    private val storeName: String = "default",
    private val batchWriteSize: Int = 100,
    private val indexSaveDebounceMs: Long = 5000L,
    private val coroutineScope: CoroutineScope? = null
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val protobuf = ProtoBuf {
        encodeDefaults = true
    }

    private val fileSystem = FileSystem.SYSTEM
    private val mutex = Mutex()

    @Serializable
    data class RecordPointer(val offset: Long, val length: Int)

    @Serializable
    data class DatabaseMetadata(
        val totalRecords: Long = 0L,
        val version: Int = 1,
        val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
        val dataFileSize: Long = 0L,
        val indexFileSize: Long = 0L,
        val indexEntries: Long = 0L
    )

    data class QueryResult<T>(
        val items: List<T>,
        val totalCount: Int,
        val hasMore: Boolean,
        val offset: Int
    )

    enum class SortOrder { ASC, DESC }

    data class QueryConfig<T>(
        val offset: Int = 0,
        val limit: Int = 20,
        val sortOrder: SortOrder = SortOrder.ASC,
        val filter: ((String, T) -> Boolean)? = null
    )

    // 内存索引
    private val memoryIndex = HashMap<String, RecordPointer>()

    // 文件路径
    private val dataFilePath = dataDir / "data.bin"
    private val indexPath = dataDir / "index.bin"
    private val metaPath = dataDir / "meta.bin"

    // 统计信息
    private var metadata = DatabaseMetadata()
    private var currentDataFileSize: Long = 0

    // 写入缓冲区 - 存储待写入的数据
    private val writeBuffer = mutableListOf<Pair<String, T>>()
    private var bufferSize = 0

    // 异步任务
    private val indexSaveTrigger = MutableStateFlow(0)
    private var indexSaveJob: Job? = null
    private val metadataSaveTrigger = MutableStateFlow(0)
    private var metadataSaveJob: Job? = null

    init {
        try {
            if (!fileSystem.exists(dataDir)) {
                fileSystem.createDirectories(dataDir)
                AppLogger.i("[$storeName] Created database directory: $dataDir")
            }

            if (fileSystem.exists(dataFilePath)) {
                currentDataFileSize = fileSystem.metadata(dataFilePath).size ?: 0
            }

            loadIndex()
            loadMetadata()
            initAsyncTasks()

            AppLogger.i("[$storeName] Database initialized - Total: ${metadata.totalRecords}")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to initialize database", e)
            throw e
        }
    }

    // --- 核心操作 ---

    /**
     * 存储或更新记录
     * 如果key已存在，会先删除旧数据再写入新数据
     */
    suspend fun put(key: String, value: T) {
        try {
            mutex.withLock {
                // 检查key是否已存在
                val existingPointer = memoryIndex[key]
                val existingInBuffer = writeBuffer.indexOfFirst { it.first == key }

                if (existingInBuffer != -1) {
                    // 更新缓冲区中的值
                    writeBuffer[existingInBuffer] = key to value
                    AppLogger.d("[$storeName] Updated existing key in buffer: $key")
                    triggerAsyncSaves()
                    return@withLock
                }

                if (existingPointer != null) {
                    // 已存在的记录，需要先移除旧数据
                    // 但暂时不立即重写文件，而是将新数据放入缓冲区
                    // 等到flush时统一重写
                    writeBuffer.add(key to value)
                    bufferSize++
                    // 标记索引指向缓冲区
                    memoryIndex[key] = RecordPointer(-1, -1)
                    AppLogger.d("[$storeName] Updated existing key (will replace old data): $key")
                } else {
                    // 新记录
                    writeBuffer.add(key to value)
                    bufferSize++
                    memoryIndex[key] = RecordPointer(-1, -1)
                    metadata = metadata.copy(totalRecords = metadata.totalRecords + 1)
                    AppLogger.d("[$storeName] Added new key: $key")
                }

                if (bufferSize >= batchWriteSize) {
                    flushBuffer()
                }

                triggerAsyncSaves()
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to put key: $key", e)
            throw e
        }
    }

    /**
     * 刷新缓冲区到磁盘 - 重写整个数据文件
     * 这样可以确保没有重复数据
     */
    private fun flushBuffer() {
        if (writeBuffer.isEmpty()) return

        try {
            // 获取所有有效记录（包括缓冲区中的和已在文件中的）
            val allRecords = mutableMapOf<String, Pair<RecordPointer?, T>>()

            // 先从内存索引中获取所有非缓冲区的记录
            memoryIndex.forEach { (key, pointer) ->
                if (pointer.offset != -1L) {
                    try {
                        val value = readData(pointer.offset, pointer.length)
                        allRecords[key] = pointer to value
                    } catch (e: Exception) {
                        AppLogger.w("[$storeName] Failed to read record for flush: $key", e)
                    }
                }
            }

            // 用缓冲区中的数据覆盖
            writeBuffer.forEach { (key, value) ->
                allRecords[key] = null to value // null表示这是新数据或更新
            }

            // 如果没有记录，清空文件
            if (allRecords.isEmpty()) {
                if (fileSystem.exists(dataFilePath)) {
                    fileSystem.delete(dataFilePath)
                }
                currentDataFileSize = 0
                memoryIndex.clear()
                writeBuffer.clear()
                bufferSize = 0
                metadata = metadata.copy(totalRecords = 0, dataFileSize = 0)
                return
            }

            // 重写数据文件
            val tempDataPath = dataDir / "data.tmp.bin"
            var newOffset = 0L
            val newIndex = mutableMapOf<String, RecordPointer>()
            var validCount = 0L

            fileSystem.sink(tempDataPath).buffer().use { dataSink ->
                allRecords.forEach { (key, pair) ->
                    val (_, value) = pair
                    try {
                        val bytes = protobuf.encodeToByteArray(serializer, value)
                        val length = bytes.size

                        dataSink.writeInt(length)
                        dataSink.write(bytes)

                        newIndex[key] = RecordPointer(newOffset, length)
                        newOffset += length + 4
                        validCount++
                    } catch (e: Exception) {
                        AppLogger.w("[$storeName] Failed to write record during flush: $key", e)
                    }
                }
            }

            // 替换数据文件
            if (fileSystem.exists(dataFilePath)) {
                fileSystem.delete(dataFilePath)
            }
            fileSystem.atomicMove(tempDataPath, dataFilePath)

            // 更新内存索引和统计
            memoryIndex.clear()
            memoryIndex.putAll(newIndex)
            currentDataFileSize = newOffset
            writeBuffer.clear()
            bufferSize = 0
            metadata = metadata.copy(
                totalRecords = validCount,
                dataFileSize = newOffset,
                lastModified = Clock.System.now().toEpochMilliseconds()
            )

            AppLogger.d("[$storeName] Flushed ${validCount} records to disk (rewrote entire file)")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to flush buffer", e)
            throw e
        }
    }

    /**
     * 获取记录
     */
    fun get(key: String): T? {
        try {
            // 先检查缓冲区
            writeBuffer.find { it.first == key }?.let {
                return it.second
            }

            // 再检查文件索引
            val pointer = memoryIndex[key] ?: return null
            if (pointer.offset == -1L) return null // 标记为待删除

            return readData(pointer.offset, pointer.length)
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to get key: $key", e)
            return null
        }
    }

    /**
     * 编辑记录
     */
    suspend fun edit(key: String, editor: (T) -> T?): T? {
        return try {
            mutex.withLock {
                val currentValue = get(key)
                if (currentValue == null) {
                    AppLogger.d("[$storeName] Key not found for editing: $key")
                    return@withLock null
                }

                val newValue = editor(currentValue)
                if (newValue == null) {
                    AppLogger.d("[$storeName] Editor returned null, no changes applied: $key")
                    return@withLock currentValue
                }

                if (currentValue == newValue) {
                    AppLogger.d("[$storeName] No changes detected for key: $key")
                    return@withLock currentValue
                }

                put(key, newValue)
                AppLogger.d("[$storeName] Edited key: $key")
                newValue
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to edit key: $key", e)
            null
        }
    }

    /**
     * 条件编辑
     */
    suspend fun editIf(key: String, condition: (T) -> Boolean, editor: (T) -> T): Boolean {
        return try {
            mutex.withLock {
                val currentValue = get(key)
                if (currentValue == null) {
                    AppLogger.d("[$storeName] Key not found for conditional edit: $key")
                    return@withLock false
                }

                if (!condition(currentValue)) {
                    AppLogger.d("[$storeName] Condition not satisfied for key: $key")
                    return@withLock false
                }

                val newValue = editor(currentValue)
                if (currentValue == newValue) {
                    AppLogger.d("[$storeName] No changes detected (values equal) for key: $key")
                    return@withLock true
                }

                put(key, newValue)
                AppLogger.d("[$storeName] Conditionally edited key: $key")
                true
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to conditionally edit key: $key", e)
            false
        }
    }

    /**
     * 删除记录 - 真正从数据库中移除
     */
    suspend fun delete(key: String): Boolean {
        return try {
            mutex.withLock {
                // 从缓冲区移除
                val removedFromBuffer = writeBuffer.removeAll { it.first == key }
                if (removedFromBuffer) {
                    bufferSize = writeBuffer.size
                    // 从索引中移除
                    memoryIndex.remove(key)
                    metadata = metadata.copy(totalRecords = metadata.totalRecords - 1)

                    // 如果有缓冲区数据，一起刷新
                    if (writeBuffer.isNotEmpty()) {
                        flushBuffer()
                    } else {
                        // 没有缓冲区数据，直接重写文件移除该记录
                        rewriteDataFile()
                    }

                    saveIndex()
                    saveMetadata()

                    AppLogger.d("[$storeName] Deleted key from buffer and file: $key")
                    return@withLock true
                }

                // 从索引中移除
                memoryIndex.remove(key)
                metadata = metadata.copy(totalRecords = metadata.totalRecords - 1)

                // 重写数据文件，移除该记录
                rewriteDataFile()

                // 保存索引和元数据
                saveIndex()
                saveMetadata()

                AppLogger.d("[$storeName] Permanently deleted key: $key")
                true
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to delete key: $key", e)
            false
        }
    }

    /**
     * 批量删除
     */
    suspend fun deleteBatch(keys: List<String>): Map<String, Boolean> {
        return try {
            mutex.withLock {
                val results = mutableMapOf<String, Boolean>()
                var deletedCount = 0

                keys.forEach { key ->
                    // 从缓冲区移除
                    val removedFromBuffer = writeBuffer.removeAll { it.first == key }
                    if (removedFromBuffer) {
                        bufferSize = writeBuffer.size
                    }

                    // 从索引中移除
                    val pointer = memoryIndex.remove(key)
                    if (pointer != null || removedFromBuffer) {
                        deletedCount++
                        results[key] = true
                        AppLogger.d("[$storeName] Marked key for deletion in batch: $key")
                    } else {
                        results[key] = false
                    }
                }

                if (deletedCount > 0) {
                    metadata = metadata.copy(totalRecords = metadata.totalRecords - deletedCount)

                    // 如果有缓冲区数据，一起刷新
                    if (writeBuffer.isNotEmpty()) {
                        flushBuffer()
                    } else {
                        // 重写数据文件
                        rewriteDataFile()
                    }

                    saveIndex()
                    saveMetadata()

                    AppLogger.i("[$storeName] Batch deletion completed: $deletedCount keys deleted")
                }

                results
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Batch deletion failed", e)
            keys.associateWith { false }
        }
    }

    /**
     * 重写数据文件 - 只保留索引中的有效记录
     */
    private suspend fun rewriteDataFile() {
        try {
            val validRecords = memoryIndex.filter { it.value.offset != -1L }

            if (validRecords.isEmpty()) {
                // 如果没有有效记录，清空文件
                if (fileSystem.exists(dataFilePath)) {
                    fileSystem.delete(dataFilePath)
                }
                currentDataFileSize = 0
                memoryIndex.clear()
                return
            }

            val tempDataPath = dataDir / "data.tmp.bin"
            var newOffset = 0L
            val newIndex = mutableMapOf<String, RecordPointer>()

            // 创建新数据文件
            fileSystem.sink(tempDataPath).buffer().use { dataSink ->
                validRecords.forEach { (key, pointer) ->
                    try {
                        val data = readData(pointer.offset, pointer.length)
                        val bytes = protobuf.encodeToByteArray(serializer, data)
                        val length = bytes.size

                        dataSink.writeInt(length)
                        dataSink.write(bytes)

                        newIndex[key] = RecordPointer(newOffset, length)
                        newOffset += length + 4
                    } catch (e: Exception) {
                        AppLogger.w("[$storeName] Failed to rewrite record: $key", e)
                    }
                }
            }

            // 替换数据文件
            if (fileSystem.exists(dataFilePath)) {
                fileSystem.delete(dataFilePath)
            }
            fileSystem.atomicMove(tempDataPath, dataFilePath)

            // 更新内存索引和统计
            memoryIndex.clear()
            memoryIndex.putAll(newIndex)
            currentDataFileSize = newOffset
            metadata = metadata.copy(dataFileSize = newOffset)

            AppLogger.d("[$storeName] Rewrote data file with ${newIndex.size} records")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to rewrite data file", e)
            throw e
        }
    }

    fun contains(key: String): Boolean {
        // 检查缓冲区
        if (writeBuffer.any { it.first == key }) return true

        // 检查文件索引
        val pointer = memoryIndex[key]
        return pointer != null && pointer.offset != -1L
    }

    // --- 索引操作 ---

    private fun saveIndex() {
        try {
            // 过滤掉无效的索引（offset为-1的表示在缓冲区中）
            val validIndex = memoryIndex.filter { it.value.offset != -1L }

            fileSystem.sink(indexPath).buffer().use { sink ->
                validIndex.forEach { (key, pointer) ->
                    val keyBytes = key.encodeToByteArray()
                    val pointerBytes = protobuf.encodeToByteArray(RecordPointer.serializer(), pointer)

                    sink.writeInt(keyBytes.size)
                    sink.write(keyBytes)
                    sink.writeInt(pointerBytes.size)
                    sink.write(pointerBytes)
                }
                sink.flush()
            }

            metadata = metadata.copy(
                indexEntries = validIndex.size.toLong(),
                indexFileSize = if (fileSystem.exists(indexPath)) fileSystem.metadata(indexPath).size ?: 0 else 0
            )
            AppLogger.d("[$storeName] Saved index with ${validIndex.size} entries")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to save index", e)
        }
    }

    // --- 其他方法 ---

    private fun readData(offset: Long, length: Int): T {
        return try {
            fileSystem.source(dataFilePath).use { source ->
                source.buffer().use { bufferedSource ->
                    if (offset > 0) bufferedSource.skip(offset)
                    val actualLength = bufferedSource.readInt()
                    if (actualLength != length) {
                        throw IllegalStateException("Data length mismatch: expected $length, got $actualLength")
                    }
                    val bytes = bufferedSource.readByteArray(actualLength.toLong())
                    protobuf.decodeFromByteArray(serializer, bytes)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to read data at offset: $offset", e)
            throw e
        }
    }

    @OptIn(FlowPreview::class)
    private fun initAsyncTasks() {
        val scope = coroutineScope ?: return

        indexSaveJob = scope.launch {
            indexSaveTrigger.debounce(indexSaveDebounceMs.milliseconds).collectLatest { saveIndex() }
        }

        metadataSaveJob = scope.launch {
            metadataSaveTrigger.debounce(3000L.milliseconds).collectLatest { saveMetadata() }
        }
    }

    private fun triggerAsyncSaves() {
        indexSaveTrigger.value += 1
        metadataSaveTrigger.value += 1
    }

    private fun loadIndex() {
        if (!fileSystem.exists(indexPath)) return
        try {
            fileSystem.source(indexPath).buffer().use { source ->
                while (!source.exhausted()) {
                    try {
                        val keySize = source.readInt()
                        val key = source.readByteArray(keySize.toLong()).decodeToString()
                        val pointerSize = source.readInt()
                        val pointerBytes = source.readByteArray(pointerSize.toLong())
                        val pointer = protobuf.decodeFromByteArray(RecordPointer.serializer(), pointerBytes)
                        memoryIndex[key] = pointer
                    } catch (e: Exception) {
                        AppLogger.e("[$storeName] Failed to read index entry, skipping rest", e)
                        break
                    }
                }
            }
            AppLogger.d("[$storeName] Loaded ${memoryIndex.size} indices")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to load index", e)
        }
    }

    private fun loadMetadata() {
        if (!fileSystem.exists(metaPath)) {
            metadata = DatabaseMetadata()
            return
        }
        try {
            fileSystem.source(metaPath).buffer().use { source ->
                val bytes = source.readByteArray()
                metadata = protobuf.decodeFromByteArray(DatabaseMetadata.serializer(), bytes)
                AppLogger.d("[$storeName] Loaded metadata: $metadata")
            }
        } catch (e: Exception) {
            AppLogger.w("[$storeName] Failed to load metadata, using defaults", e)
            metadata = DatabaseMetadata()
        }
    }

    private fun saveMetadata() {
        try {
            metadata = metadata.copy(
                dataFileSize = currentDataFileSize,
                lastModified = Clock.System.now().toEpochMilliseconds()
            )

            fileSystem.sink(metaPath).buffer().use { sink ->
                val bytes = protobuf.encodeToByteArray(DatabaseMetadata.serializer(), metadata)
                sink.write(bytes)
                sink.flush()
            }
            AppLogger.d("[$storeName] Saved metadata: $metadata")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to save metadata", e)
        }
    }

    fun getStats(): Map<String, Any> = mapOf(
        "totalRecords" to metadata.totalRecords,
        "memoryIndexSize" to memoryIndex.size,
        "dataFileSize" to currentDataFileSize,
        "indexFileSize" to (if (fileSystem.exists(indexPath)) fileSystem.metadata(indexPath).size ?: 0 else 0),
        "indexEntries" to metadata.indexEntries,
        "writeBufferSize" to bufferSize,
        "version" to metadata.version,
        "lastModified" to metadata.lastModified
    )

    suspend fun query(config: QueryConfig<T> = QueryConfig()): QueryResult<T> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    // 如果有缓冲区数据，先刷新确保一致性
                    if (writeBuffer.isNotEmpty()) {
                        flushBuffer()
                    }

                    val allRecords = mutableListOf<Pair<String, T>>()

                    memoryIndex.forEach { (key, pointer) ->
                        if (pointer.offset != -1L) {
                            try {
                                val value = readData(pointer.offset, pointer.length)
                                if (config.filter == null || config.filter(key, value)) {
                                    allRecords.add(key to value)
                                }
                            } catch (e: Exception) {
                                AppLogger.w("[$storeName] Failed to read record for query: $key", e)
                            }
                        }
                    }

                    val sortedRecords = when (config.sortOrder) {
                        SortOrder.ASC -> allRecords
                        SortOrder.DESC -> allRecords.reversed()
                    }

                    val totalCount = sortedRecords.size
                    val startIndex = min(config.offset, totalCount)
                    val endIndex = min(config.offset + config.limit, totalCount)
                    val hasMore = endIndex < totalCount

                    val paginatedItems = sortedRecords
                        .subList(startIndex, endIndex)
                        .map { it.second }

                    QueryResult(paginatedItems, totalCount, hasMore, endIndex)
                } catch (e: Exception) {
                    AppLogger.e("[$storeName] Query failed", e)
                    QueryResult(emptyList(), 0, false, 0)
                }
            }
        }
    }

    suspend fun query(limit: Int = 20, offset: Int = 0): QueryResult<T> {
        return query(QueryConfig(offset = offset, limit = limit))
    }

    suspend fun query(
        limit: Int = 20,
        offset: Int = 0,
        filter: (String, T) -> Boolean
    ): QueryResult<T> {
        return query(QueryConfig(offset = offset, limit = limit, filter = filter))
    }

    fun getAllValues(): List<T> {
        return try {
            val values = mutableListOf<T>()

            // 先检查缓冲区
            writeBuffer.forEach { (_, value) ->
                values.add(value)
            }

            // 再检查文件
            memoryIndex.forEach { (key, pointer) ->
                if (pointer.offset != -1L) {
                    try {
                        val value = readData(pointer.offset, pointer.length)
                        values.add(value)
                    } catch (e: Exception) {
                        AppLogger.w("[$storeName] Failed to read record for getAllValues: $key", e)
                    }
                }
            }

            values
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to get all values", e)
            emptyList()
        }
    }

    fun forceSync() {
        try {
            kotlinx.coroutines.runBlocking {
                mutex.withLock {
                    if (writeBuffer.isNotEmpty()) {
                        flushBuffer()
                    }
                    saveIndex()
                    saveMetadata()
                    AppLogger.d("[$storeName] Force sync completed")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Force sync failed", e)
        }
    }

    fun flush() {
        try {
            kotlinx.coroutines.runBlocking {
                mutex.withLock {
                    if (writeBuffer.isNotEmpty()) {
                        flushBuffer()
                    }
                }
            }

            saveIndex()
            saveMetadata()
            AppLogger.d("[$storeName] Flush completed")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Flush failed", e)
        }
    }

    private fun close() {
        try {
            if (writeBuffer.isNotEmpty()) {
                kotlinx.coroutines.runBlocking { flushBuffer() }
            }
            saveIndex()
            saveMetadata()
            indexSaveJob?.cancel()
            metadataSaveJob?.cancel()
            AppLogger.i("[$storeName] Database closed - Stats: ${getStats()}")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Error during close", e)
        }
    }

    fun destroy() {
        close()
        memoryIndex.clear()
        writeBuffer.clear()
    }
}