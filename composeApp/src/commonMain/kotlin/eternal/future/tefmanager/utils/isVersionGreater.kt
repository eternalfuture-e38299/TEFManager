package eternal.future.tefmanager.utils

/*******************************************************************************
 * TEFManager - isVersionGreater
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
 * Created: 2026/8/21
 *******************************************************************************/


/**
 * 比较两个版本号，判断 v1 是否大于 v2
 * 支持格式：1.8.0、1.8、1.8.0.1 等
 */
fun isVersionGreater(v1: String, v2: String): Boolean {
    val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }

    val maxLen = maxOf(parts1.size, parts2.size)

    for (i in 0 until maxLen) {
        val num1 = if (i < parts1.size) parts1[i] else 0
        val num2 = if (i < parts2.size) parts2[i] else 0
        if (num1 != num2) return num1 > num2
    }
    return false
}