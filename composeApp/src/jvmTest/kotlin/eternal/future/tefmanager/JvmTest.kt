package eternal.future.tefmanager

import eternal.future.tefmanager.utils.AddonManager
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.LightProtoStore
import eternal.future.tefmanager.utils.TefPkgReader
import eternal.future.tefmanager.utils.resourcepack.TexturePackManager
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/*******************************************************************************
 * TEFManager - JvmTest
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
 * Created: 2026/2/9
 *******************************************************************************/

class JvmTest {
    @Serializable
    data class TestUser(
        val id: String,
        val name: String,
        val age: Int
    )

    init {
        AppLogger.initializeSync()
    }

    @Test
    fun tefPkgRead() {
        val pkg = TefPkgReader("/home/eternalfuture/CLionProjects/tefpackage/cmake-build-debug/test.pkg".toPath())
        pkg.open()

        println("文件完整性验证：${pkg.verifyPackageIntegrity()}")

        pkg.close()
    }

    @Test
    fun example(): Unit = runBlocking {
        val testDir = "/home/eternalfuture/测试目录/simple_test".toPath()
        val store = LightProtoStore(testDir, TestUser.serializer(), "MetadataTest")

        try {
            // 写入数据
            repeat(100) { i ->
                store.put("u$i", TestUser("u$i", "User$i", 20 + i))
            }

            // 删除部分
            repeat(50) { i ->
                store.delete("u$i")
            }

            // 获取统计
            val stats1 = store.getStats()
            assertEquals(100L, stats1["totalRecords"])
            assertEquals(50L, stats1["deletedRecords"])

            // 重启
            store.destroy()
            val store2 = LightProtoStore(testDir, TestUser.serializer(), "MetadataTest")

            // 验证元数据恢复
            val stats2 = store2.getStats()
            assertEquals(100L, stats2["totalRecords"])   // 必须正确恢复
            assertEquals(50L, stats2["deletedRecords"]) // 必须正确恢复

        } finally {
            store.destroy()
        }
    }

    @Test
    fun testLargeDatasetPerformance(): Unit = runBlocking {
        val testDir = "/home/eternalfuture/测试目录/very_large_test".toPath()
        File(testDir.toString()).mkdirs()

        // 使用更大的批量写入大小
        val store = LightProtoStore(
            testDir,
            TestUser.serializer(),
            "VeryLargeTest",
            batchWriteSize = 5000  // 增加批量大小
        )

        try {
            val totalRecords = 1_000_000  // 100万条记录
            val targetKey = "user_500000" // 中间位置的key

            println("=== 超大数据集测试 (1,000,000条记录) ===")
            println("批量写入大小: 5000")

            // 测量写入时间
            val writeTime = measureTimeMillis {
                repeat(totalRecords) { i ->
                    val key = "user_$i"
                    store.put(key, TestUser("id_$i", "User_$i", 20 + (i % 50)))

                    // 每10,000条打印进度
                    if (i % 10_000 == 0 && i > 0) {
                        println("已写入 $i 条记录...")
                    }
                }
            }

            // 强制刷新并重启
            store.destroy()
            println("第一次写入完成，重启存储...")

            val store2 = LightProtoStore(testDir, TestUser.serializer(), "VeryLargeTest")

            // 测量内存占用
            val runtime = Runtime.getRuntime()
            runtime.gc()
            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

            // 测量读取性能
            val readCount = 100
            val readTime = measureTimeMillis {
                repeat(readCount) {
                    val user = store2.get(targetKey)
                    assertNotNull(user)
                    assertEquals("id_500000", user.id)
                }
            }

            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024 // MB

            // 获取统计信息
            val stats = store2.getStats()

            println("=== 超大数据集测试结果 ===")
            println("写入 $totalRecords 条记录总耗时: ${writeTime}ms")
            println("平均写入速度: ${"%.2f".format(writeTime.toDouble() / totalRecords)}ms/条")
            println("读取单个记录平均耗时: ${"%.3f".format(readTime.toDouble() / readCount)}ms")
            println("内存占用增加: ${memoryUsed}MB")
            println("数据文件大小: ${stats["dataFileSize"]} bytes (${"%.2f".format(stats["dataFileSize"] as Long / 1024.0 / 1024.0)} MB)")
            println("索引文件大小: ${stats["indexFileSize"]} bytes (${"%.2f".format(stats["indexFileSize"] as Long / 1024.0 / 1024.0)} MB)")
            println("索引条目数: ${stats["indexEntries"]}")
            println("删除率: ${"%.2f".format(stats["deletionRate"] as Double * 100)}%")

            // 测试随机读取多个位置
            println("=== 随机读取测试 ===")
            val randomKeys = listOf("user_1000", "user_250000", "user_500000", "user_750000", "user_999999")
            randomKeys.forEach { key ->
                val time = measureTimeMillis {
                    repeat(10) {
                        val user = store2.get(key)
                        assertNotNull(user)
                    }
                }
                println("读取 $key 10次平均耗时: ${"%.3f".format(time.toDouble() / 10)}ms/次")
            }

        } finally {
            store.destroy()
            // 清理测试文件（可能需要时间，可以注释掉先查看文件大小）
            // File(testDir.toString()).deleteRecursively()
        }
    }

    @Test
    fun testRandomAccessPerformance(): Unit = runBlocking {
        val testDir = "/home/eternalfuture/测试目录/random_access_test".toPath()
        File(testDir.toString()).mkdirs()

        val store = LightProtoStore(testDir, TestUser.serializer(), "RandomAccessTest")

        try {
            val totalRecords = 5_000
            println("=== 随机访问测试 ===")

            // 写入数据
            repeat(totalRecords) { i ->
                store.put("key_$i", TestUser("id_$i", "User_$i", 20 + i))
            }

            // 测试随机读取
            val randomKeys = listOf("key_123", "key_2500", "key_4999", "key_100", "key_3000")
            val readTimes = mutableListOf<Long>()

            randomKeys.forEach { key ->
                val time = measureTimeMillis {
                    repeat(50) {
                        val user = store.get(key)
                        assertNotNull(user)
                    }
                }
                readTimes.add(time)
                println("读取 $key 50次耗时: ${time}ms (平均: ${"%.3f".format(time.toDouble() / 50)}ms/次)")
            }

            val avgReadTime = readTimes.average() / 50
            println("随机读取平均耗时: ${"%.3f".format(avgReadTime)}ms/次")

        } finally {
            store.destroy()
            // File(testDir.toString()).deleteRecursively()
        }
    }

    @Test
    fun testAddonManager() : Unit = runBlocking {
        FileKit.init("tefmanager")


        // 在 UI 层调用
        TexturePackManager.installTexturePack("/home/eternalfuture/Downloads/色彩boss雕像1.0.zip".toPath())
        TexturePackManager.installTexturePack("/home/eternalfuture/Downloads/Boss彩色圣杯.tl".toPath())

        /*AddonManager.installOrUpdate("/home/eternalfuture/测试目录/test_mod/压缩文件.zip".toPath()) { progress, error ->
            when (progress) {
                AddonManager.InstallProgress.STARTING -> println("Installation started")
                AddonManager.InstallProgress.READING_MANIFEST -> println("Reading package manifest")
                AddonManager.InstallProgress.COPYING_FILES -> println("Copying files")
                AddonManager.InstallProgress.COMPLETED -> println("Installation completed!")
                AddonManager.InstallProgress.ERROR -> {
                    println("Installation failed")
                    error?.printStackTrace()
                    // 显示错误信息
                    // showErrorDialog(error?.message ?: "Unknown error")
                }
                else -> println("Progress: $progress")
            }
        }*/
    }
}