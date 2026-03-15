package eternal.future.tefmanager.utils

import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.SYSTEM
import okio.Source
import okio.buffer

/*******************************************************************************
 * TEFManager - TefPkgReader
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
 * Created: 2026/2/23
 *******************************************************************************/

class TefPkgReader(val filePath: Path) {
    data class TefPkgHeader(
        val magic: Int,           // 4字节
        val version: Short,       // 2字节
        val fileCount: Short,     // 2字节
        val dataOffset: Int,      // 4字节
        val dataSize: Int,        // 4字节
        val timestamp: Long,      // 8字节
        val checksum: Long,       // 8字节
        val contentHash: Long,    // 8字节
        val signature: Long       // 8字节
    )

    data class TefPkgFileEntry(
        val index: Int,              // 4字节
        val dataOffset: Int,         // 4字节
        val compressedSize: Int,     // 4字节
        val originalSize: Int,       // 4字节
        val checksum: Long,          // 8字节
        val timestamp: Long,         // 8字节
        val compressType: Byte,      // 1字节
        val compressLevel: Byte      // 1字节
    )

    private val fileSystem = FileSystem.SYSTEM
    private var source: Source? = null
    private var bufferedSource: BufferedSource? = null

    var header: TefPkgHeader? = null
    var entries: List<TefPkgFileEntry>? = null

    /**
     * 打开并读取TEF包文件头信息
     */
    fun open(): Boolean {
        return try {
            // 打开文件并保存句柄
            source = fileSystem.source(filePath)
            bufferedSource = source!!.buffer()

            // 读取文件头
            header = readTefPkgHeader()
            entries = readTefPkgEntries()

            AppLogger.i("TEF package opened successfully: $filePath")
            AppLogger.d("Header: magic=0x${header?.magic?.toHexString()}, fileCount=${header?.fileCount}")

            true
        } catch (e: IOException) {
            AppLogger.e("Failed to open TEF package: ${e.message}", e)
            close()
            false
        } catch (e: Exception) {
            AppLogger.e("Unknown error opening TEF package: ${e.message}", e)
            close()
            false
        }
    }

    /**
     * 读取指定条目的数据（不改变文件指针位置）
     * @param entry 文件条目
     * @return 压缩数据的字节数组
     */
    fun readEntryData(entry: TefPkgFileEntry): ByteArray? {
        return try {
            bufferedSource?.let { source ->
                // 绝对偏移量：头部数据偏移 + 条目数据偏移
                val absoluteOffset = header!!.dataOffset.toLong() + entry.dataOffset.toLong()

                // 保存当前位置
                val currentPos = source.buffer.size

                // 定位到数据起始位置
                source.skip(absoluteOffset - currentPos)

                // 读取压缩数据
                val data = source.readByteArray(entry.compressedSize.toLong())

                // 恢复原始位置
                source.skip(-(absoluteOffset + entry.compressedSize.toLong() - currentPos))

                AppLogger.d("Read entry data: index=${entry.index}, offset=0x${absoluteOffset.toHexString()}, " +
                        "size=${entry.compressedSize}, actual=${data.size} bytes")

                data
            }
        } catch (e: IOException) {
            AppLogger.e("Failed to read entry data: index=${entry.index}, offset=0x${entry.dataOffset.toHexString()}", e)
            null
        }
    }

    /**
     * 读取指定索引的文件数据
     * @param index 条目索引
     */
    fun readEntryData(index: Int): ByteArray? {
        val entry = getEntry(index) ?: run {
            AppLogger.w("Entry index $index not found")
            return null
        }
        return readEntryData(entry)
    }

    /**
     * 读取所有条目的数据
     * @return 包含所有条目数据的列表
     */
    fun readAllEntriesData(): List<ByteArray?> {
        return entries?.map { entry ->
            readEntryData(entry)
        } ?: emptyList()
    }

    /**
     * 验证magic字段是否为"TEFP"
     */
    fun validateMagic(): Boolean {
        val header = header ?: return false
        val magicString = magicToString(header.magic)
        val isValid = magicString == "TEFP"

        if (!isValid) {
            AppLogger.w("Invalid magic: expected 'TEFP', got '$magicString' (0x${header.magic.toHexString()})")
        } else {
            AppLogger.d("Magic validation passed: 'TEFP'")
        }

        return isValid
    }

    /**
     * 获取文件条目总数
     */
    fun getEntryCount(): Int = header?.fileCount?.toInt() ?: 0

    /**
     * 获取指定索引的文件条目
     */
    fun getEntry(index: Int): TefPkgFileEntry? {
        return entries?.getOrNull(index)
    }

    /**
     * 计算压缩率
     */
    fun calculateCompressionRatio(entry: TefPkgFileEntry): Double {
        return if (entry.originalSize > 0) {
            entry.compressedSize.toDouble() / entry.originalSize * 100
        } else {
            0.0
        }
    }

    /**
     * 获取包信息摘要
     */
    fun getPackageSummary(): String {
        val header = header ?: return "Package not loaded"

        return buildString {
            append("TEF Package Summary:\n")
            append("  File: $filePath\n")
            append("  Magic: ${magicToString(header.magic)} (0x${header.magic.toHexString()})\n")
            append("  Version: ${header.version}\n")
            append("  File Count: ${header.fileCount}\n")
            append("  Data Offset: 0x${header.dataOffset.toHexString()}\n")
            append("  Data Size: ${header.dataSize} bytes\n")
            append("  Timestamp: ${header.timestamp}\n")

            entries?.forEachIndexed { index, entry ->
                append("  Entry #$index:\n")
                append("    Index: ${entry.index}\n")
                append("    Data Offset: 0x${entry.dataOffset.toHexString()}\n")
                append("    Compressed Size: ${entry.compressedSize} bytes\n")
                append("    Original Size: ${entry.originalSize} bytes\n")
                append("    Compression Ratio: ${calculateCompressionRatio(entry)}\n")
                append("    Compress Type: ${entry.compressType}\n")
                append("    Compress Level: ${entry.compressLevel}\n")
            }
        }
    }

    /**
     * 关闭文件句柄
     */
    fun close() {
        try {
            bufferedSource?.close()
            source?.close()
            AppLogger.d("TEF package closed: $filePath")
        } catch (e: IOException) {
            // 忽略关闭错误
            AppLogger.w("Error closing TEF package: ${e.message}")
        } finally {
            bufferedSource = null
            source = null
        }
    }

    private fun readTefPkgHeader(): TefPkgHeader {
        val source = bufferedSource ?: throw IOException("Source not opened")

        val magic = source.readIntLe()           // uint32_t
        val version = source.readShortLe()       // uint16_t
        val fileCount = source.readShortLe()     // uint16_t
        val dataOffset = source.readIntLe()      // uint32_t
        val dataSize = source.readIntLe()        // uint32_t
        val timestamp = source.readLongLe()      // uint64_t
        val checksum = source.readLongLe()       // uint64_t
        val contentHash = source.readLongLe()    // uint64_t
        val signature = source.readLongLe()      // uint64_t

        return TefPkgHeader(
            magic = magic,
            version = version,
            fileCount = fileCount,
            dataOffset = dataOffset,
            dataSize = dataSize,
            timestamp = timestamp,
            checksum = checksum,
            contentHash = contentHash,
            signature = signature
        )
    }

    private fun readTefPkgEntries(): List<TefPkgFileEntry> {
        val source = bufferedSource ?: throw IOException("Source not opened")
        val header = header ?: throw IOException("Header not read")

        val entries = mutableListOf<TefPkgFileEntry>()

        (0 until header.fileCount.toInt()).forEach { _ ->
            val index = source.readIntLe()           // uint32_t
            val dataOffset = source.readIntLe()      // uint32_t
            val compressedSize = source.readIntLe()  // uint32_t
            val originalSize = source.readIntLe()    // uint32_t
            val checksum = source.readLongLe()       // uint64_t
            val timestamp = source.readLongLe()      // uint64_t
            val compressType = source.readByte()     // uint8_t
            val compressLevel = source.readByte()    // uint8_t

            entries.add(TefPkgFileEntry(
                index = index,
                dataOffset = dataOffset,
                compressedSize = compressedSize,
                originalSize = originalSize,
                checksum = checksum,
                timestamp = timestamp,
                compressType = compressType,
                compressLevel = compressLevel
            ))
        }

        AppLogger.d("Read ${entries.size} file entries")
        return entries
    }

    private fun magicToString(magic: Int): String {
        val bytes = ByteArray(4)
        bytes[0] = (magic and 0xFF).toByte()
        bytes[1] = ((magic shr 8) and 0xFF).toByte()
        bytes[2] = ((magic shr 16) and 0xFF).toByte()
        bytes[3] = ((magic shr 24) and 0xFF).toByte()
        return bytes.decodeToString(0, 0 + bytes.size)
    }

    fun verifyPackageIntegrity(): Boolean {
        val header = header ?: return false

        // 验证头部校验和
        val calculatedHeaderChecksum = calculateHeaderChecksum(header)
        if (header.checksum.toULong() != calculatedHeaderChecksum) {
            AppLogger.e("Header integrity check failed: expected ${header.checksum}, calculated $calculatedHeaderChecksum")
            return false
        }

        // 验证内容哈希
        val calculatedContentHash = calculateContentHashChecksum()
        if (header.contentHash.toULong() != calculatedContentHash) {
            AppLogger.e("Content hash mismatch: expected ${header.contentHash}, calculated $calculatedContentHash")
            return false
        }

        AppLogger.i("Package integrity verification passed")
        return true
    }

    private fun calculateHeaderChecksum(header: TefPkgHeader): ULong {
        // 创建临时头部副本，清零相关字段
        val tempHeader = header.copy(
            checksum = 0L,
            signature = 0L,
            contentHash = 0L
        )

        // 总头部大小48字节，排除3个8字节字段=24字节
        val effectiveSize = 48 - 24 // 24字节

        // 序列化头部（排除最后3个字段）- 使用小端序
        val headerBytes = ByteArray(effectiveSize)
        var offset = 0

        headerBytes.writeIntLe(offset, tempHeader.magic)        // 4字节
        offset += 4

        headerBytes.writeShortLe(offset, tempHeader.version)    // 2字节
        offset += 2

        headerBytes.writeShortLe(offset, tempHeader.fileCount)  // 2字节
        offset += 2

        headerBytes.writeIntLe(offset, tempHeader.dataOffset)   // 4字节
        offset += 4

        headerBytes.writeIntLe(offset, tempHeader.dataSize)     // 4字节
        offset += 4

        headerBytes.writeLongLe(offset, tempHeader.timestamp)    // 8字节
        // offset=16 + 8=24，正好填满

        // 使用固定密钥计算SipHash
        return SipHash.siphashStream(
            headerBytes,
            key0 = 0x0318030920211212uL,
            key1 = 0x49204C6F76652059uL
        )
    }

    private fun calculateContentHashChecksum(): ULong {
        val header = header ?: return 0uL
        val entries = entries ?: return 0uL

        // 初始化SipHash上下文
        val ctx = SipHash.SipHashContext()
        SipHash.init(ctx, 0x6F75000000000000uL, 0xE5B08FE9B9ACuL)

        // 处理头部（排除contentHash和signature字段）
        val tempHeader = header.copy(
            contentHash = 0L,
            signature = 0L
        )

        // 排除2个8字节字段，有效大小=48-16=32字节
        val headerEffectiveSize = 48 - 16

        val headerBytes = ByteArray(headerEffectiveSize)
        var offset = 0

        headerBytes.writeIntLe(offset, tempHeader.magic)       // 4
        offset += 4

        headerBytes.writeShortLe(offset, tempHeader.version)   // 2
        offset += 2

        headerBytes.writeShortLe(offset, tempHeader.fileCount)  // 2
        offset += 2

        headerBytes.writeIntLe(offset, tempHeader.dataOffset)  // 4
        offset += 4

        headerBytes.writeIntLe(offset, tempHeader.dataSize)     // 4
        offset += 4

        headerBytes.writeLongLe(offset, tempHeader.timestamp)  // 8
        offset += 8

        headerBytes.writeLongLe(offset, tempHeader.checksum)   // 8
        // offset=16+8+8=32，正好填满

        SipHash.update(ctx, headerBytes, headerBytes.size)

        // 处理所有文件条目 - 确保也使用小端序序列化
        entries.forEach { entry ->
            val entryBytes = serializeFileEntry(entry)
            SipHash.update(ctx, entryBytes, entryBytes.size)
        }

        return SipHash.finalize(ctx)
    }

    /**
     * 序列化文件条目（小端序）
     */
    private fun serializeFileEntry(entry: TefPkgFileEntry): ByteArray {
        val bytes = ByteArray(34) // 所有字段总大小

        var offset = 0
        bytes.writeIntLe(offset, entry.index)
        offset += 4
        bytes.writeIntLe(offset, entry.dataOffset)
        offset += 4
        bytes.writeIntLe(offset, entry.compressedSize)
        offset += 4
        bytes.writeIntLe(offset, entry.originalSize)
        offset += 4
        bytes.writeLongLe(offset, entry.checksum)
        offset += 8
        bytes.writeLongLe(offset, entry.timestamp)
        offset += 8
        bytes[offset] = entry.compressType
        offset += 1
        bytes[offset] = entry.compressLevel

        return bytes
    }

    private fun ByteArray.writeIntLe(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun ByteArray.writeShortLe(offset: Int, value: Short) {
        this[offset] = (value.toInt() and 0xFF).toByte()
        this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }

    private fun ByteArray.writeLongLe(offset: Int, value: Long) {
        for (i in 0 until 8) {
            this[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
    }
}