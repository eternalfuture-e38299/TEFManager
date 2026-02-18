package eternal.future.tefmanager.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException

/*******************************************************************************
 * TEFManager - ExeParser
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

object TrParser {
    data class TrInfo(
        val filePath: String,
        val version: String? = null
    )

    fun parse(filePath: String): TrInfo {
        return try {
            val dirPath = File(filePath).parentFile
            val deps = File(dirPath, "Terraria.deps.json")
            val changelog = File(dirPath, "changelog.txt")

            if (deps.exists()) {
                val depsJson = Json.parseToJsonElement(deps.readText()).jsonObject
                var gameVersion : String? = null
                val targets = depsJson["targets"]?.jsonObject
                if (targets != null) {
                    // 遍历所有目标
                    for ((_, targetValue) in targets.jsonObject) {
                        if (targetValue is JsonObject) {
                            // 查找包含 "Terraria/" 的键
                            for ((depName, _) in targetValue.jsonObject) {
                                if (depName.startsWith("Terraria/")) {
                                    // 提取版本号: "Terraria/1.4.4.9" -> "1.4.4.9"
                                    val parts = depName.split("/")
                                    if (parts.size == 2) {
                                        gameVersion = parts[1]
                                        break
                                    }
                                }
                            }
                        }
                        if (gameVersion != null) break
                    }
                }

                return TrInfo(filePath, gameVersion)
            } else {
                val lines = changelog.readLines()
                var gameVersion: String? = null

                for (i in 0 until minOf(lines.size, 5)) { // 检查前5行
                    val line = lines[i].trim()
                    if (line.isNotEmpty()) {
                        gameVersion = extractVersionFromChangelogLine(line)
                        if (gameVersion != null) break
                    }
                }

                return TrInfo(filePath, gameVersion)
            }
        } catch (e : IOException) {
            AppLogger.e(message = "Failed to open file", throwable = e)
            TrInfo(filePath, null)
        }
    }

    private fun extractVersionFromChangelogLine(line: String): String? {
        val trimmedLine = line.trim()

        // 1. 处理 "Version 1.4.5.3 Changes" 这样的格式
        if (trimmedLine.startsWith("Version ", ignoreCase = true)) {
            val afterVersion = trimmedLine.substring(7).trim() // 跳过 "Version"
            val spaceIndex = afterVersion.indexOf(' ')

            val candidate = if (spaceIndex != -1) {
                afterVersion.substring(0, spaceIndex)
            } else {
                afterVersion
            }

            if (isValidTerrariaVersion(candidate)) {
                return candidate
            }
        }

        // 2. 处理 "v1.4.5.3 Changes" 格式
        if (trimmedLine.startsWith("v", ignoreCase = true) && trimmedLine.length > 1) {
            val afterV = trimmedLine.substring(1).trim()
            val spaceIndex = afterV.indexOf(' ')

            val candidate = if (spaceIndex != -1) {
                afterV.substring(0, spaceIndex)
            } else {
                afterV
            }

            if (isValidTerrariaVersion(candidate)) {
                return candidate
            }
        }

        // 3. 如果以数字开头
        if (trimmedLine.isNotEmpty() && trimmedLine[0].isDigit()) {
            val spaceIndex = trimmedLine.indexOf(' ')
            val candidate = if (spaceIndex != -1) {
                trimmedLine.substring(0, spaceIndex)
            } else {
                trimmedLine
            }

            if (isValidTerrariaVersion(candidate)) {
                return candidate
            }
        }

        return null
    }

    private fun isValidTerrariaVersion(version: String): Boolean {
        if (version.length > 15) return false // 版本号不会太长
        if (!version.matches("""\d+\.\d+\.\d+\.\d+""".toRegex())) return false

        val parts = version.split(".")
        if (parts.size != 4) return false

        // 检查每个部分
        for (part in parts) {
            if (part.isEmpty() || part.length > 3) return false
            if (!part.all { it.isDigit() }) return false
        }

        return true
    }
}