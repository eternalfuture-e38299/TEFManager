package eternal.future.tefmanager.model

import kotlinx.serialization.Serializable

/*******************************************************************************
 * RotatingArtLauncher - ModRepositoryIndex
 * 
 * This file is part of the RotatingArtLauncher project.
 * 
 * Copyright (C) 2026 RotatingArtLauncher Contributors
 * 
 * Created by: eternalfuture-e38299 (2026/10/31)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

@Serializable
data class ModRepositoryIndex(
    val updatedAt: String = "",
    val loaderMinVersionCode: Int = 0,
    val loaderMinVersion: String = "1.0.0",
    val mods: List<ModEntry> = emptyList()
) {
    @Serializable
    data class ModEntry(
        val pkgId: String,
        val name: String = "",
        val author: String = "",
        val version: String = "",
        val versionCode: Int = 0,
        val brieflyDescribe: String = "",
        val features: List<ModItem.ModFeature> = emptyList(),
        val sizeCategory: ModItem.ModSizeCategory = ModItem.ModSizeCategory.TINY,
        val stableVerified: Boolean = false,
        val experimental: Boolean = false,
        val deprecated: Boolean = false,

        val iconUrl: String = "",           // ← 新增，可为空
        val downloadUrl: String = "",
        val fileSize: String = "",
        val updateLog: String = ""
    ) {
        fun toAddonUpdateInfo() = AddonUpdateInfo(
            version = version,
            versionCode = versionCode,
            fileSize = fileSize,
            downloadUrl = downloadUrl,
            updateLog = updateLog
        )
    }
}