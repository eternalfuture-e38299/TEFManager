package eternal.future.tefmanager.utils.addon

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.AddonConfig
import eternal.future.tefmanager.model.ModItem

/*******************************************************************************
 * TEFManager - ModManager
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
 * Created: 2026/8/3
 *******************************************************************************/

class ModManager(loaderId: String) : BaseManager<ModItem>(
    AddonConfig(
        Platform.getData("mods") / loaderId,
        loaderId,
        AddonConfig.AddonType.Mod
    ),
    ModItem.serializer()
)