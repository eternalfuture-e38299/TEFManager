package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.runtime.*
import eternal.future.tefmanager.model.ModLoaderItem
import eternal.future.tefmanager.model.ModRepositoryIndex
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.addon.AddonManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*******************************************************************************
 * RotatingArtLauncher - AddonStoreState
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

@Stable
class AddonStoreState(
    private val networkService: NetworkService
) {
    companion object {
        fun matchesQuery(entry: ModRepositoryIndex.ModEntry, rawQuery: String): Boolean {
            val q = rawQuery.trim().lowercase()
            if (q.isEmpty()) return true
            return entry.name.lowercase().contains(q) ||
                    entry.pkgId.lowercase().contains(q) ||
                    entry.author.lowercase().contains(q) ||
                    entry.brieflyDescribe.lowercase().contains(q)
        }
    }

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** pkgId -> entry，且已经跨 loader 去重 */
    var entries by mutableStateOf<List<Pair<ModLoaderItem, ModRepositoryIndex.ModEntry>>>(emptyList())
        private set

    /** 只缓存当前启用的 loader 的索引 */
    private val indexCache = mutableMapOf<String, List<ModRepositoryIndex.ModEntry>>()

    /** 最近一次触发加载的启用 loader pkgId 集合 */
    private var lastKey: List<String> = emptyList()

    /** 当前弹窗目标 */
    var dialogTarget by mutableStateOf<Pair<ModLoaderItem, ModRepositoryIndex.ModEntry>?>(null)

    /** 一次重新聚合，仅在 enabled 集合变化或首次加载时调用 */
    suspend fun refresh(enabledLoaders: List<ModLoaderItem>) {
        val key = enabledLoaders.map { it.pkgId }
        if (key == lastKey && entries.isNotEmpty()) return
        lastKey = key

        // 移除已禁用的 loader
        indexCache.keys.retainAll(key.toSet())

        if (enabledLoaders.isEmpty()) {
            entries = emptyList()
            error = null
            loading = false
            return
        }

        // 找出要新拉取的 loader
        val toFetch = enabledLoaders.filter { indexCache[it.pkgId] == null }
        loading = toFetch.isNotEmpty()
        error = null

        if (toFetch.isNotEmpty()) {
            val mutex = Mutex()
            kotlinx.coroutines.coroutineScope {
                toFetch.forEach { loader ->
                    launch {
                        val idx = networkService.getJson<ModRepositoryIndex>(loader.modRepositoryIndexUrl) ?: return@launch
                        if (idx.loaderMinVersionCode > 0 && loader.versionCode < idx.loaderMinVersionCode) {
                            return@launch
                        }
                        mutex.withLock {
                            indexCache[loader.pkgId] = idx.mods
                        }
                    }
                }
            }
            loading = false
        }

        entries = enabledLoaders
            .flatMap { loader ->
                (indexCache[loader.pkgId] ?: emptyList()).map { loader to it }
            }
            .groupBy { it.second.pkgId }
            .map { (_, list) -> list.maxBy { it.second.versionCode } }
    }

    fun isInstalled(loader: ModLoaderItem, entry: ModRepositoryIndex.ModEntry): Boolean {
        val manager = AddonManager.getOrCreateModManager(loader.pkgId) ?: return false
        return manager.packs.any { it.pkgId == entry.pkgId }
    }

    fun localVersion(loader: ModLoaderItem, entry: ModRepositoryIndex.ModEntry): String? {
        val manager = AddonManager.getOrCreateModManager(loader.pkgId) ?: return null
        return manager.packs.firstOrNull { it.pkgId == entry.pkgId }?.version
    }
}