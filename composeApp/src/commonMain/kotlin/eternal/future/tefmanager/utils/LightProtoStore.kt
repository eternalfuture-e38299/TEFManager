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
    private val autoCompactThreshold: Double = 0.5, // 提高触发阈值，降低压缩频率
    private val minRecordsForCompact: Int = 100, // 新增：最小记录数才触发压缩

    private val batchWriteSize: Int = 1000,
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
    data class RecordPointer(val offset: Long, val length: Int, val isDeleted: Boolean = false)

    @Serializable
    data class DatabaseMetadata(
        val totalRecords: Long = 0L,
        val deletedRecords: Long = 0L,
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

    // 写入缓冲区
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

            AppLogger.i("[$storeName] Database initialized - Total: ${metadata.totalRecords}, Deleted: ${metadata.deletedRecords}")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to initialize database", e)
            throw e
        }
    }

    // --- 公开的压缩函数 ---
    suspend fun compact() {
        try {
            mutex.withLock {
                if (metadata.deletedRecords == 0L) {
                    AppLogger.i("[$storeName] No need to compact - no deleted records")
                    return
                }

                AppLogger.i("[$storeName] Starting manual compaction...")

                val tempDataPath = dataDir / "data.tmp.bin"
                var newOffset = 0L
                var newTotal = 0L

                // 创建新数据文件
                fileSystem.sink(tempDataPath).buffer().use { dataSink ->
                    // 遍历所有有效记录
                    memoryIndex.entries.forEach { (key, pointer) ->
                        if (!pointer.isDeleted) {
                            try {
                                // 如果偏移量为-1，说明数据在缓冲区中
                                if (pointer.offset == -1L) {
                                    val bufferedRecord = writeBuffer.find { it.first == key }
                                    if (bufferedRecord != null) {
                                        val bytes = protobuf.encodeToByteArray(serializer, bufferedRecord.second)
                                        val length = bytes.size

                                        dataSink.writeInt(length)
                                        dataSink.write(bytes)

                                        memoryIndex[key] = RecordPointer(newOffset, length, false)
                                        newOffset += length + 4
                                        newTotal++
                                    }
                                } else {
                                    val data = readData(pointer.offset, pointer.length)
                                    val bytes = protobuf.encodeToByteArray(serializer, data)
                                    val length = bytes.size

                                    dataSink.writeInt(length)
                                    dataSink.write(bytes)

                                    memoryIndex[key] = RecordPointer(newOffset, length, false)
                                    newOffset += length + 4
                                    newTotal++
                                }
                            } catch (e: Exception) {
                                AppLogger.w("[$storeName] Failed to compact record: $key", e)
                            }
                        }
                    }
                }

                // 替换数据文件
                fileSystem.delete(dataFilePath)
                fileSystem.atomicMove(tempDataPath, dataFilePath)

                // 更新统计
                metadata = metadata.copy(
                    deletedRecords = 0L,
                    totalRecords = newTotal,
                    dataFileSize = newOffset,
                    lastModified = Clock.System.now().toEpochMilliseconds()
                )
                currentDataFileSize = newOffset

                // 清理索引和保存
                cleanupIndex()
                saveIndex()
                saveMetadata()

                AppLogger.i("[$storeName] Manual compaction completed - new total: $newTotal")
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Manual compaction failed", e)
            throw e
        }
    }

    // --- 核心操作 ---

    suspend fun put(key: String, value: T) {
        try {
            mutex.withLock {
                val existingIndex = writeBuffer.indexOfFirst { it.first == key }
                if (existingIndex != -1) {
                    writeBuffer[existingIndex] = key to value
                } else {
                    writeBuffer.add(key to value)
                    bufferSize++
                }

                memoryIndex[key] = RecordPointer(-1, -1, false)

                if (existingIndex == -1) {
                    metadata = metadata.copy(totalRecords = metadata.totalRecords + 1)
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

    private fun flushBuffer() {
        if (writeBuffer.isEmpty()) return

        try {
            fileSystem.appendingSink(dataFilePath).buffer().use { sink ->
                writeBuffer.forEach { (key, value) ->
                    val bytes = protobuf.encodeToByteArray(serializer, value)
                    val length = bytes.size
                    val offset = currentDataFileSize

                    sink.writeInt(length)
                    sink.write(bytes)

                    memoryIndex[key] = RecordPointer(offset, length, false)
                    currentDataFileSize += 4 + length
                }
                sink.flush()
            }

            writeBuffer.clear()
            bufferSize = 0
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to flush buffer", e)
            throw e
        }
    }

    fun get(key: String): T? {
        try {
            val pointer = memoryIndex[key] ?: return null
            if (pointer.isDeleted) return null

            if (pointer.offset == -1L) {
                return writeBuffer.find { it.first == key }?.second
            }

            return readData(pointer.offset, pointer.length)
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to get key: $key", e)
            return null
        }
    }

    /**
     * 编辑一个已存在的记录
     * @param key 记录的唯一键
     * @param editor 编辑函数，传入当前值，返回新的值。返回null表示不修改
     * @return 编辑后的新值，如果记录不存在或编辑失败则返回null
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

                // 检查值是否真的改变了
                if (currentValue == newValue) {
                    AppLogger.d("[$storeName] No changes detected for key: $key")
                    return@withLock currentValue
                }

                // 使用put方法进行更新
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
     * 条件编辑 - 只有满足条件时才进行编辑
     * @param key 记录的唯一键
     * @param condition 条件判断函数，返回true时才执行编辑
     * @param editor 编辑函数，接收当前值并返回新值
     * @return 是否成功编辑
     */
    suspend fun editIf(key: String, condition: (T) -> Boolean, editor: (T) -> T): Boolean {
        return try {
            mutex.withLock {
                val currentValue = get(key)
                if (currentValue == null) {
                    AppLogger.d("[$storeName] Key not found for conditional edit: $key")
                    return@withLock false
                }

                // 检查条件
                if (!condition(currentValue)) {
                    AppLogger.d("[$storeName] Condition not satisfied for key: $key")
                    return@withLock false
                }

                val newValue = editor(currentValue)

                // 检查值是否真的改变了
                if (currentValue == newValue) {
                    AppLogger.d("[$storeName] No changes detected (values equal) for key: $key")
                    return@withLock true  // 条件满足但值未变，也算成功
                }

                // 执行更新
                put(key, newValue)

                AppLogger.d("[$storeName] Conditionally edited key: $key")
                true
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to conditionally edit key: $key", e)
            false
        }
    }

    suspend fun delete(key: String): Boolean {
        try {
            mutex.withLock {
                val pointer = memoryIndex[key] ?: return false
                if (pointer.isDeleted) return false

                memoryIndex[key] = pointer.copy(isDeleted = true)
                metadata = metadata.copy(deletedRecords = metadata.deletedRecords + 1)

                writeBuffer.removeAll { it.first == key }
                bufferSize = writeBuffer.size

                triggerAsyncSaves()

                // 降低触发概率：需要满足最小记录数和阈值
                if (shouldCompact()) {
                    AppLogger.i("[$storeName] Auto-compact triggered (deletion rate: ${getDeletionRate()})")
                    launchCompactAsync()
                }

                AppLogger.d("[$storeName] Deleted key: $key")
                return true
            }
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to delete key: $key", e)
            return false
        }
    }

    fun contains(key: String): Boolean {
        val pointer = memoryIndex[key]
        return pointer != null && !pointer.isDeleted
    }

    // --- 索引清理功能 ---

    private fun cleanupIndex() {
        val entriesToRemove = memoryIndex.filter { it.value.isDeleted }.keys.toList()
        if (entriesToRemove.isNotEmpty()) {
            entriesToRemove.forEach { key ->
                memoryIndex.remove(key)
            }
            AppLogger.d("[$storeName] Cleaned up ${entriesToRemove.size} deleted index entries")
        }
    }

    private fun saveIndex() {
        try {
            // 先清理索引
            cleanupIndex()

            fileSystem.sink(indexPath).buffer().use { sink ->
                memoryIndex.forEach { (key, pointer) ->
                    val keyBytes = key.encodeToByteArray()
                    val pointerBytes = protobuf.encodeToByteArray(RecordPointer.serializer(), pointer)

                    sink.writeInt(keyBytes.size)
                    sink.write(keyBytes)
                    sink.writeInt(pointerBytes.size)
                    sink.write(pointerBytes)
                }
                sink.flush()
            }

            metadata = metadata.copy(indexEntries = memoryIndex.size.toLong())
            AppLogger.d("[$storeName] Saved index with ${memoryIndex.size} entries")
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

    private fun launchCompactAsync() {
        val scope = coroutineScope ?: return
        scope.launch {
            try {
                compact()
            } catch (e: Exception) {
                AppLogger.e("[$storeName] Async compaction failed", e)
            }
        }
    }

    private fun shouldCompact(): Boolean {
        // 降低触发概率：需要满足最小记录数和阈值
        return metadata.totalRecords >= minRecordsForCompact &&
                getDeletionRate() > autoCompactThreshold
    }

    private fun getDeletionRate(): Double {
        return if (metadata.totalRecords > 0) {
            metadata.deletedRecords.toDouble() / metadata.totalRecords
        } else {
            0.0
        }
    }

    @OptIn(FlowPreview::class)
    private fun initAsyncTasks() {
        val scope = coroutineScope ?: return

        indexSaveJob = scope.launch {
            indexSaveTrigger.debounce(indexSaveDebounceMs).collectLatest { saveIndex() }
        }

        metadataSaveJob = scope.launch {
            metadataSaveTrigger.debounce(3000L).collectLatest { saveMetadata() }
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
                indexFileSize = if (fileSystem.exists(indexPath)) fileSystem.metadata(indexPath).size ?: 0 else 0,
                indexEntries = memoryIndex.size.toLong(),
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
        "deletedRecords" to metadata.deletedRecords,
        "deletionRate" to getDeletionRate(),
        "memoryIndexSize" to memoryIndex.size,
        "dataFileSize" to currentDataFileSize,
        "indexFileSize" to (if (fileSystem.exists(indexPath)) fileSystem.metadata(indexPath).size ?: 0 else 0),
        "indexEntries" to metadata.indexEntries,
        "writeBufferSize" to bufferSize,
        "version" to metadata.version,
        "lastModified" to metadata.lastModified,
        "autoCompactThreshold" to autoCompactThreshold,
        "minRecordsForCompact" to minRecordsForCompact
    )

    suspend fun query(config: QueryConfig<T> = QueryConfig()): QueryResult<T> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val allRecords = mutableListOf<Pair<String, T>>()

                    // 处理缓冲区记录
                    writeBuffer.forEach { (key, value) ->
                        if (config.filter == null || config.filter(key, value)) {
                            allRecords.add(key to value)
                        }
                    }

                    // 处理文件记录
                    memoryIndex.forEach { (key, pointer) ->
                        if (!pointer.isDeleted && pointer.offset != -1L) {
                            val value = get(key)
                            if (value != null && (config.filter == null || config.filter(key, value))) {
                                allRecords.add(key to value)
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

    /**
     * 获取所有有效的值
     * @return 所有未删除的值的列表
     */
    fun getAllValues(): List<T> {
        return try {
            val values = mutableListOf<T>()

            // 处理缓冲区记录
            writeBuffer.forEach { (_, value) ->
                values.add(value)
            }

            // 处理文件记录
            memoryIndex.forEach { (key, pointer) ->
                if (!pointer.isDeleted) { // 只包含未删除的
                    if (pointer.offset != -1L) { // 不在缓冲区中
                        val value = get(key)
                        if (value != null) {
                            values.add(value)
                        }
                    }
                }
            }

            values
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Failed to get all values", e)
            emptyList()
        }
    }

    private fun close() {
        try {
            if (writeBuffer.isNotEmpty()) {
                runBlocking { flushBuffer() }
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

    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }

    fun flush() {
        try {
            if (writeBuffer.isNotEmpty()) {
                // 同步执行写入操作
                kotlinx.coroutines.runBlocking {
                    mutex.withLock {
                        fileSystem.appendingSink(dataFilePath).buffer().use { sink ->
                            writeBuffer.forEach { (key, value) ->
                                val bytes = protobuf.encodeToByteArray(serializer, value)
                                val length = bytes.size
                                val offset = currentDataFileSize

                                sink.writeInt(length)
                                sink.write(bytes)

                                memoryIndex[key] = RecordPointer(offset, length, false)
                                currentDataFileSize += 4 + length
                            }
                            sink.flush()
                        }

                        writeBuffer.clear()
                        bufferSize = 0
                    }
                }
            }

            // 无论是否有缓冲数据，都强制保存索引和元数据
            saveIndex()
            saveMetadata()

            AppLogger.d("[$storeName] Flush completed - buffer cleared, index and metadata saved")
        } catch (e: Exception) {
            AppLogger.e("[$storeName] Flush failed", e)
        }
    }
}