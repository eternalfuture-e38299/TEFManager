package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Extension
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
import eternal.future.tefmanager.model.ModRepositoryIndex
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
        var selectedLoaderPkg by remember { mutableStateOf<String?>(null) }

        val enabledLoaders = remember(ModLoaderManager.packs.toList()) {
            ModLoaderManager.packs.filter {
                it.modRepositoryIndexUrl.isNotBlank() && ModLoaderManager.isEnabled(it.pkgId)
            }
        }

        LaunchedEffect(enabledLoaders.map { it.pkgId }) {
            state.refresh(enabledLoaders)
            if (selectedLoaderPkg == null || enabledLoaders.none { it.pkgId == selectedLoaderPkg }) {
                selectedLoaderPkg = enabledLoaders.firstOrNull()?.pkgId
            }
        }

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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SearchBar(query) { query = it }

                // 只要有启用的 Loader 就显示 Tab 行（单 Loader 时也显示）
                if (enabledLoaders.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LoaderTabRow(
                        loaders = enabledLoaders,
                        selectedPkg = selectedLoaderPkg,
                        onSelect = { selectedLoaderPkg = it }
                    )
                }

                Spacer(Modifier.height(12.dp))

                when {
                    enabledLoaders.isEmpty() -> NoLoaderState()

                    state.loading -> LoadingState()

                    state.error != null -> ErrorState(state.error!!) {
                        scope.launch { state.refresh(enabledLoaders) }
                    }

                    else -> {
                        val currentLoader = enabledLoaders.firstOrNull { it.pkgId == selectedLoaderPkg }
                        if (currentLoader == null) {
                            NoLoaderState()
                        } else {
                            val currentMods = state.entries
                                .filter { it.first.pkgId == currentLoader.pkgId }
                                .map { it.second }
                                .filter { query.isBlank() || matchesQuery(it, query) }

                            if (currentMods.isEmpty()) {
                                EmptyState(query.isNotBlank())
                            } else {
                                ModList(
                                    loader = currentLoader,
                                    mods = currentMods,
                                    state = state
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Loader Tab 行
    // ──────────────────────────────────────────────────────────
    @Composable
    private fun LoaderTabRow(
        loaders: List<ModLoaderItem>,
        selectedPkg: String?,
        onSelect: (String) -> Unit
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(loaders, key = { it.pkgId }) { loader ->
                val selected = loader.pkgId == selectedPkg
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(loader.pkgId) },
                    label = {
                        Text(
                            text = loader.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Mod 列表（当前选中 Loader 下的 mod）
    // ──────────────────────────────────────────────────────────
    @Composable
    private fun ModList(
        loader: ModLoaderItem,
        mods: List<ModRepositoryIndex.ModEntry>,
        state: AddonStoreState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(mods, key = { it.pkgId }) { entry ->
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

    // ──────────────────────────────────────────────────────────
    // 搜索框
    // ──────────────────────────────────────────────────────────
    @Composable
    private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    Strings.store.search,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

    // ──────────────────────────────────────────────────────────
    // 各种状态
    // ──────────────────────────────────────────────────────────
    @Composable
    private fun NoLoaderState() {
        CenterColumn {
            Icon(
                Icons.Rounded.ExtensionOff,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                Strings.store.noLoaderEnabled,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.store.noLoaderEnabledHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun LoadingState() {
        CenterColumn {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                Strings.loading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun ErrorState(message: String, onRetry: () -> Unit) {
        CenterColumn {
            Icon(
                Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Strings.refresh)
            }
        }
    }

    @Composable
    private fun EmptyState(searching: Boolean) {
        CenterColumn {
            Icon(
                Icons.Rounded.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
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
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            content()
        }
    }

    override val title: String
        get() = Strings.store.title
}