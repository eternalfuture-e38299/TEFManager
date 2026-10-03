package eternal.future.tefmanager.model

import okio.Path

/*******************************************************************************
 * RotatingArtLauncher - AddonConfig
 * 
 * This file is part of the RotatingArtLauncher project.
 * 
 * Copyright (C) 2026 RotatingArtLauncher Contributors
 * 
 * Created by: eternalfuture-e38299 (2026/8/3)
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

data class AddonConfig(
    val dir: Path,    // 模块根目录
    val parentId: String, // 属于什么
    val type: AddonType   // 类型
) {
    enum class AddonType {
        Module,
        Plugin,
        ModLoader,
        Mod;

        fun getPkgDirName() : String =
            when(this) {
                Mod -> "mod"
                else -> "pkg"
            }

    }
}