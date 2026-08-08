package eternal.future.tefmanager.ui.screen.shared.resourcepack

import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.resourcepack.TexturePackManager

/*******************************************************************************
 * TEFManager - TexturePackScreen
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

object TexturePackScreen : BasePackScreen(
    manager = TexturePackManager,
    title = { Strings.resource.texture }
)