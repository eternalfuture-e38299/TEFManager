package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExtensionOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.model.ModLoaderItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.StoreModCard
import eternal.future.tefmanager.ui.dialogs.AddonUpdateDialog
import eternal.future.tefmanager.ui.screen.shared.AddonStoreState
import eternal.future.tefmanager.ui.screen.shared.AddonStoreState.Companion.matchesQuery
import eternal.future.tefmanager.utils.NetworkService
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.addon.ModLoaderManager
import kotlinx.coroutines.launch

/*******************************************************************************
 * RotatingArtLauncher - AddonStoreScreen
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

object AddonStoreScreen : Screen, MainScreen.TitledScreen {
    @Composable
    override fun Content() {
        val networkService = remember { NetworkService() }
        DisposableEffect(Unit) { onDispose { networkService.close() } }

        val state = remember(networkService) { AddonStoreState(networkService) }
        val scope = rememberCoroutineScope()

        var query by remember { mutableStateOf("") }
        var sourceFilter by remember { mutableStateOf<String?>(null) }

        // 已启用 loader 列表（受 ModLoaderManager 状态变化影响）
        // 使用 packs 列表作为 key，配合 isEnabled 过滤
        val enabledLoaders = remember(ModLoaderManager.packs.toList()) {
            ModLoaderManager.packs.filter {
                it.modRepositoryIndexUrl.isNotBlank() && ModLoaderManager.isEnabled(it.pkgId)
            }
        }

        // 首次加载 & 启用集合变化时刷新
        LaunchedEffect(enabledLoaders.map { it.pkgId }) {
            state.refresh(enabledLoaders)
            // 如果当前筛选的来源被禁用，重置筛选
            if (sourceFilter != null && enabledLoaders.none { it.pkgId == sourceFilter }) {
                sourceFilter = null
            }
        }

        // 弹窗
        state.dialogTarget?.let { (loader, entry) ->
            val installed = state.isInstalled(loader, entry)
            val localVersion = state.localVersion(loader, entry)
            AddonUpdateDialog(
                title = if (installed) Strings.manager.update.title(entry.name)
                else Strings.store.installTitle(entry.name),
                currentVersion = if (installed) localVersion!! else Strings.store.notInstalled,
                updateInfo = entry.toAddonUpdateInfo(),
                installPackage = { path ->
                    scope.launch { AddonManager.install(path) }
                },
                onDismiss = { state.dialogTarget = null }
            )
        }

        // 过滤后的条目
        val filtered = remember(state.entries, query, sourceFilter) {
            state.entries.filter { (loader, entry) ->
                (sourceFilter == null || loader.pkgId == sourceFilter) &&
                        (query.isBlank() || matchesQuery(entry, query))
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(Modifier.fillMaxSize()) {
                // ── 左侧来源栏 ──
                SourceSidebar(
                    enabledLoaders = enabledLoaders,
                    selected = sourceFilter,
                    onSelect = { sourceFilter = it }
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── 右侧主区 ──
                Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SearchBar(query) { query = it }
                    Spacer(Modifier.height(12.dp))

                    when {
                        enabledLoaders.isEmpty() -> NoLoaderState()
                        state.loading -> LoadingState()
                        state.error != null -> ErrorState(state.error!!) {
                            scope.launch { state.refresh(enabledLoaders) }
                        }
                        filtered.isEmpty() -> EmptyState(query.isNotBlank())
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered, key = { it.second.pkgId }) { (loader, entry) ->
                                StoreModCard(
                                    entry = entry,
                                    loaderName = loader.name,
                                    installed = state.isInstalled(loader, entry),
                                    iconUrl = entry.iconUrl.takeIf { it.isNotBlank() },
                                    onDownload = { state.dialogTarget = loader to entry }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SourceSidebar(
        enabledLoaders: List<ModLoaderItem>,
        selected: String?,
        onSelect: (String?) -> Unit
    ) {
        Surface(
            modifier = Modifier.width(200.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                SidebarItem(
                    label = Strings.store.allSources,
                    selected = selected == null,
                    onClick = { onSelect(null) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                enabledLoaders.forEach { loader ->
                    SidebarItem(
                        label = loader.name,
                        selected = selected == loader.pkgId,
                        onClick = { onSelect(loader.pkgId) }
                    )
                }
            }
        }
    }

    @Composable
    private fun SidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }

    @Composable
    private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(Strings.store.search, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }

    @Composable
    private fun NoLoaderState() {
        CenterColumn {
            Icon(Icons.Rounded.ExtensionOff, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            Text(Strings.store.noLoaderEnabled,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(Strings.store.noLoaderEnabledHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }

    @Composable
    private fun LoadingState() {
        CenterColumn {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(Strings.loading, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun ErrorState(message: String, onRetry: () -> Unit) {
        CenterColumn {
            Icon(Icons.Rounded.CloudOff, null, Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Strings.refresh)
            }
        }
    }

    @Composable
    private fun EmptyState(searching: Boolean) {
        CenterColumn {
            Icon(Icons.Rounded.SearchOff, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            Text(
                if (searching) Strings.manager.search.noResults else Strings.store.empty,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun CenterColumn(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { content() }
    }

    override val title: String
        get() = Strings.store.title
}