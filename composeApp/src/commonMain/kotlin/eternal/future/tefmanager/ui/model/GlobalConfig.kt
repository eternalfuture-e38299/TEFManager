package eternal.future.tefmanager.ui.model

import kotlinx.serialization.Serializable

/*******************************************************************************
 * TEFManager - GlobalConfig
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
 * Created: 2026/3/28
 *******************************************************************************/

@Serializable
data class GlobalConfig(
    val fileType: String = "null",
    val generateFile: String = "null",
    val configItems: List<ConfigItem> = listOf()
) {
    companion object {
        val empty = GlobalConfig("null", "null", listOf())
    }

    @Serializable
    data class ConfigItem(
        val type: Type = Type.UNKNOWN,
        val key: String = "",
        val description: String = "",
        val displayName: String = "",
        val placeholder: String = ""
    )

    @Serializable
    enum class Type {
        UNKNOWN,
        INT8,
        INT16,
        INT32,
        INT64,
        UINT8,
        UINT16,
        UINT32,
        UINT64,
        FLOAT,
        DOUBLE,
        BOOL,
        STRING
    }
}