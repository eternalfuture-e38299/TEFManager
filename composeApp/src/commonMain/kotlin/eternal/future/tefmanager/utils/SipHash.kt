package eternal.future.tefmanager.utils

/*******************************************************************************
 * TEFManager - SipHash
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


object SipHash {
    data class SipHashContext(
        var v0: ULong = 0uL,
        var v1: ULong = 0uL,
        var v2: ULong = 0uL,
        var v3: ULong = 0uL,
        var totalLen: ULong = 0uL,
        var buffer: ByteArray = ByteArray(8),
        var bufferLen: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SipHashContext

            if (bufferLen != other.bufferLen) return false
            if (v0 != other.v0) return false
            if (v1 != other.v1) return false
            if (v2 != other.v2) return false
            if (v3 != other.v3) return false
            if (totalLen != other.totalLen) return false
            if (!buffer.contentEquals(other.buffer)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bufferLen
            result = 31 * result + v0.hashCode()
            result = 31 * result + v1.hashCode()
            result = 31 * result + v2.hashCode()
            result = 31 * result + v3.hashCode()
            result = 31 * result + totalLen.hashCode()
            result = 31 * result + buffer.contentHashCode()
            return result
        }
    }

    // 初始常量
    private val INIT_V0 = 0x736f6d6570736575uL
    private val INIT_V1 = 0x646f72616e646f6duL
    private val INIT_V2 = 0x6c7967656e657261uL
    private val INIT_V3 = 0x7465646279746573uL

    /**
     * 判断是否小端序
     */
    private fun isLittleEndian(): Boolean {
        val test = 0x01020304
        return (test and 0xFF) == 0x04
    }

    /**
     * 从小端序读取64位整数
     */
    private fun readLe64(data: ByteArray, offset: Int = 0): ULong {
        // 强制使用小端序读取，与C语言完全一致
        return (data[offset].toULong() and 0xFFuL) or
                ((data[offset + 1].toULong() and 0xFFuL) shl 8) or
                ((data[offset + 2].toULong() and 0xFFuL) shl 16) or
                ((data[offset + 3].toULong() and 0xFFuL) shl 24) or
                ((data[offset + 4].toULong() and 0xFFuL) shl 32) or
                ((data[offset + 5].toULong() and 0xFFuL) shl 40) or
                ((data[offset + 6].toULong() and 0xFFuL) shl 48) or
                ((data[offset + 7].toULong() and 0xFFuL) shl 56)
    }

    /**
     * 64位循环左移
     */
    private fun rotl64(x: ULong, b: Int): ULong {
        return (x shl b) or (x shr (64 - b))
    }

    /**
     * 初始化 SipHash 上下文
     */
    fun init(ctx: SipHashContext, key0: ULong, key1: ULong) {
        ctx.v0 = INIT_V0 xor key0
        ctx.v1 = INIT_V1 xor key1
        ctx.v2 = INIT_V2 xor key0
        ctx.v3 = INIT_V3 xor key1
        ctx.totalLen = 0uL
        ctx.bufferLen = 0
        ctx.buffer.fill(0)
    }

    /**
     * 更新 SipHash 计算状态
     */
    fun update(ctx: SipHashContext, data: ByteArray, len: Int) {
        ctx.totalLen += len.toULong()

        var dataOffset = 0
        var remainingLen = len

        // 处理缓冲区中已有的数据
        if (ctx.bufferLen > 0) {
            val copyLen = minOf(8 - ctx.bufferLen, remainingLen)

            // 使用Kotlin标准库替代System.arraycopy
            data.copyInto(
                destination = ctx.buffer,
                destinationOffset = ctx.bufferLen,
                startIndex = 0,
                endIndex = copyLen
            )

            ctx.bufferLen += copyLen
            dataOffset += copyLen
            remainingLen -= copyLen

            if (ctx.bufferLen == 8) {
                processBlock(ctx, ctx.buffer, 0)
                ctx.bufferLen = 0
            }
        }

        // 处理完整的8字节块
        while (remainingLen >= 8) {
            processBlock(ctx, data, dataOffset)
            dataOffset += 8
            remainingLen -= 8
        }

        // 保存剩余字节到缓冲区
        if (remainingLen > 0) {
            // 使用Kotlin标准库替代System.arraycopy
            data.copyInto(
                destination = ctx.buffer,
                destinationOffset = 0,
                startIndex = dataOffset,
                endIndex = dataOffset + remainingLen
            )
            ctx.bufferLen = remainingLen
        }
    }

    /**
     * 处理8字节数据块
     */
    private fun processBlock(ctx: SipHashContext, data: ByteArray, offset: Int) {
        val m = readLe64(data, offset)
        ctx.v3 = ctx.v3 xor m

        // SipHash 轮函数 (2轮)
        repeat(2) {
            ctx.v0 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 13)
            ctx.v1 = ctx.v1 xor ctx.v0
            ctx.v0 = rotl64(ctx.v0, 32)

            ctx.v2 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 16)
            ctx.v3 = ctx.v3 xor ctx.v2

            ctx.v0 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 21)
            ctx.v3 = ctx.v3 xor ctx.v0

            ctx.v2 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 17)
            ctx.v1 = ctx.v1 xor ctx.v2
            ctx.v2 = rotl64(ctx.v2, 32)
        }

        ctx.v0 = ctx.v0 xor m
    }

    /**
     * 完成 SipHash 计算
     */
    fun finalize(ctx: SipHashContext): ULong {
        // 构造最后的块
        var b = (ctx.totalLen and 0xFFuL) shl 56

        // 处理缓冲区中的剩余数据
        if (ctx.bufferLen > 0) {
            val lastBlock = ByteArray(8)

            ctx.buffer.copyInto(
                destination = lastBlock,
                destinationOffset = 0,
                startIndex = 0,
                endIndex = ctx.bufferLen
            )

            // 小端序处理
            for (i in 0 until ctx.bufferLen) {
                b = b or (lastBlock[i].toULong() shl (i * 8))
            }
        }

        ctx.v3 = ctx.v3 xor b
        repeat(2) {
            ctx.v0 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 13)
            ctx.v1 = ctx.v1 xor ctx.v0
            ctx.v0 = rotl64(ctx.v0, 32)

            ctx.v2 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 16)
            ctx.v3 = ctx.v3 xor ctx.v2

            ctx.v0 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 21)
            ctx.v3 = ctx.v3 xor ctx.v0

            ctx.v2 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 17)
            ctx.v1 = ctx.v1 xor ctx.v2
            ctx.v2 = rotl64(ctx.v2, 32)
        }
        ctx.v0 = ctx.v0 xor b

        // 最终轮 (4轮)
        ctx.v2 = ctx.v2 xor 0xFFuL
        repeat(4) {
            ctx.v0 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 13)
            ctx.v1 = ctx.v1 xor ctx.v0
            ctx.v0 = rotl64(ctx.v0, 32)

            ctx.v2 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 16)
            ctx.v3 = ctx.v3 xor ctx.v2

            ctx.v0 += ctx.v3
            ctx.v3 = rotl64(ctx.v3, 21)
            ctx.v3 = ctx.v3 xor ctx.v0

            ctx.v2 += ctx.v1
            ctx.v1 = rotl64(ctx.v1, 17)
            ctx.v1 = ctx.v1 xor ctx.v2
            ctx.v2 = rotl64(ctx.v2, 32)
        }

        return ctx.v0 xor ctx.v1 xor ctx.v2 xor ctx.v3
    }

    /**
     * 单次计算 SipHash 值
     */
    fun siphashStream(data: ByteArray, key0: ULong, key1: ULong): ULong {
        val ctx = SipHashContext()
        init(ctx, key0, key1)
        update(ctx, data, data.size)
        return finalize(ctx)
    }
}