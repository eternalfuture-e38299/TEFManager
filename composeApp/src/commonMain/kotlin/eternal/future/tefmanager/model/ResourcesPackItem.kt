package eternal.future.tefmanager.model

import kotlinx.serialization.Serializable

/*******************************************************************************
 * TEFManager - TexturePackItem
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
 * Created: 2026/4/19
 *******************************************************************************/

@Serializable
data class ResourcesPackItem(
    val name: String = "UNKNOWN",
    val author: String = "UNKNOWN",
    val description: String = "",
    val version: String = "",
    val fileName: String = "",
    val iconPath: String = "",
    val type: Type = Type.Terraria,
    val packType: PackType = PackType.TexturePack
) {
    enum class Type {
        Terraria,
        TLPro,
        TEFManager;

        fun getText(): String {
            return when(this) {
                Terraria -> "Terraria"
                TLPro -> "TL Pro"
                TEFManager -> "TEFManager"
            }
        }
    }

    enum class PackType {
        TexturePack,
        LanguagePack,
        LanguagePatchPack,
        AudioPack,
        FontPack
    }
}