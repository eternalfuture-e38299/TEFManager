package eternal.future.tefmanager.ui.data

import androidx.compose.runtime.snapshots.SnapshotStateList
import eternal.future.tefmanager.ui.model.GameItem
import kotlinx.coroutines.flow.StateFlow

/*******************************************************************************
 * TEFManager - GameManager
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
 * Created: 2026/2/28
 *******************************************************************************/

expect object GameManager {
    /**
     * 游戏列表的响应式流
     */
    val games: SnapshotStateList<GameItem>

    /**
     * 添加游戏项目
     * @return 是否添加成功
     */
    fun addGame(gameItem: GameItem)

    /**
     * 移除游戏项目
     * @return 是否移除成功
     */
    fun removeGame(hash: String)

    /**
     * 刷新游戏列表
     */
    fun refreshGames()
}