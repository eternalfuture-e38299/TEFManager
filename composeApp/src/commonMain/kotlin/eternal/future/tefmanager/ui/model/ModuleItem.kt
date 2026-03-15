package eternal.future.tefmanager.ui.model

import kotlinx.serialization.Serializable

/*******************************************************************************
 * TEFManager - ModuleItem
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
 * Created: 2026/3/15
 *******************************************************************************/

@Serializable
data class ModuleItem(
    val type: String,
    val pkgId: String = "",
    val name: String = "",
    val author: String = "",
    val description: String = "",
    val version: String = "",
    val versionCode: Int = 0,
    val detailedInformationURL: String = "",
    val support: PlatformSupport = PlatformSupport()
) {
    @Serializable
    data class PlatformSupport(
        val android: ArchitectureSupport = ArchitectureSupport(),
        val windows: ArchitectureSupport = ArchitectureSupport(),
        val linux: ArchitectureSupport = ArchitectureSupport(),
        val mac: ArchitectureSupport = ArchitectureSupport(),
        val ios: ArchitectureSupport = ArchitectureSupport()
    ) {
        fun getSupportedPlatforms(): List<String> {
            val platforms = mutableListOf<String>()

            listOf(
                "Android" to android,
                "Windows" to windows,
                "Linux" to linux,
                "macOS" to mac,
                "iOS" to ios
            ).forEach { (platformName, archSupport) ->
                val archList = archSupport.getSupportedArchs()
                if (archList.isNotEmpty()) {
                    // 格式: "Android (ARM64, x64)" 或 "Android (ARM64)"
                    val displayText = if (archList.size == 1) {
                        "$platformName (${archList[0]})"
                    } else {
                        "$platformName (${archList.joinToString(", ")})"
                    }
                    platforms.add(displayText)
                }
            }

            return platforms
        }

    }

    @Serializable
    data class ArchitectureSupport(
        val arm64: Boolean = false,
        val arm: Boolean = false,
        val x64: Boolean = false,
        val x86: Boolean = false
    ) {
        fun getSupportedArchs(): List<String> = listOf(
            "ARM64" to arm64,
            "ARM" to arm,
            "x64" to x64,
            "x86" to x86
        ).filter { it.second }.map { it.first }

    }
}