package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.ModItem
import eternal.future.tefmanager.model.ModLoaderItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.ModItemCard
import eternal.future.tefmanager.ui.component.ModLoaderItemCard
import eternal.future.tefmanager.ui.component.ModuleItemCard
import eternal.future.tefmanager.ui.component.PluginItemCard
import eternal.future.tefmanager.ui.dialogs.AddonInstallOrUpdateDialog
import eternal.future.tefmanager.utils.addon.AddonManager
import eternal.future.tefmanager.utils.addon.ModLoaderManager
import eternal.future.tefmanager.utils.addon.ModuleManager
import eternal.future.tefmanager.utils.addon.PluginManager
import eternal.future.tefmanager.utils.toFileUrlString
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Duration.Companion.milliseconds

/*******************************************************************************
 * TEFManager - ManagerScreen
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
 * Created: 2026/2/2
 *******************************************************************************/

object ManagerScreen : Screen, MainScreen.TitledScreen {

    private var categories = mutableStateListOf<ManagerTab>()

    data class ManagerTab(
        val id: String,
        val title: String,
        val icon: ImageVector? = null,
        val isKernel: Boolean = false,
        val isKernelPlugin: Boolean = false,
        val isModLoader: Boolean = false,
        val iconPath: Path? = null
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        var selectedTab by remember { mutableIntStateOf(0) }
        var searchQuery by remember { mutableStateOf("") }
        var debouncedSearchQuery by remember { mutableStateOf("") }
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

        val showAddonInstallOrUpdateDialog = remember { mutableStateOf(false) }

        // 在 categories 初始化之后添加
        LaunchedEffect(ModLoaderManager.packs) {
            // 保存当前选中的 tab id
            val currentTabId = categories.getOrNull(selectedTab)?.id

            val newCategories = mutableListOf<ManagerTab>()
            newCategories.addAll(listOf(
                ManagerTab(
                    id = "kernel",
                    title = Strings.manager.module.title,
                    icon = Icons.Rounded.Memory,
                    isKernel = true
                ),
                ManagerTab(
                    id = "kernel_plugins",
                    title = Strings.manager.plugin.title,
                    icon = Icons.Rounded.Extension,
                    isKernelPlugin = true
                ),
                ManagerTab(
                    id = "mod_loaders",
                    title = Strings.manager.modloader.title,
                    icon = Icons.Rounded.Dashboard,
                    isModLoader = true
                )
            ))

            ModLoaderManager.packs.forEach { loader ->
                newCategories.add(ManagerTab(
                    id = loader.pkgId,
                    title = loader.name,
                    iconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon"
                ))
            }

            // 查找之前选中的 tab 的新索引
            val newIndex = newCategories.indexOfFirst { it.id == currentTabId }
                .takeIf { it >= 0 } ?: 0

            categories.clear()
            categories.addAll(newCategories)

            // 如果选中的索引变化了，更新 selectedTab 和 pagerState
            if (selectedTab != newIndex) {
                selectedTab = newIndex
                coroutineScope.launch {
                    pagerState.scrollToPage(newIndex)
                }
            }
        }

        LaunchedEffect(searchQuery) {
            delay(300.milliseconds)
            debouncedSearchQuery = searchQuery
        }

        val enabledLoaders = remember { mutableStateMapOf<String, Boolean>() }

        LaunchedEffect(ModLoaderManager.packs) {
            AddonManager.refreshModManager(ModLoaderManager.packs)
            enabledLoaders.apply {
                ModLoaderManager.packs.forEach {
                    val enabled = ModLoaderManager.isEnabled(it.pkgId)
                    put(it.pkgId, enabled)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (debouncedSearchQuery.isEmpty()) {
                    NormalTabView(
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        enabledLoaders = enabledLoaders,
                        onTabSelected = { index ->
                            selectedTab = index
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        showAddonInstallOrUpdateDialog = showAddonInstallOrUpdateDialog
                    )
                } else {
                    SearchResultsView(
                        searchQuery = debouncedSearchQuery,
                        enabledLoaders = enabledLoaders
                    )
                }
            }
        }
    }

    @Composable
    private fun SearchResultsView(
        searchQuery: String,
        enabledLoaders: SnapshotStateMap<String, Boolean>
    ) {
        val query = searchQuery.trim()

        // 获取所有启用的加载器包名列表
        val enabledPackages = remember(enabledLoaders) {
            enabledLoaders.filter { it.value }.keys.toList()
        }

        val searchResults = remember(query, enabledPackages) {
            derivedStateOf {
                if (query.isEmpty()) {
                    emptyList()
                } else {
                    val lowerQuery = query.lowercase()
                    val results = mutableListOf<Pair<ModLoaderItem, ModItem>>()

                    // 遍历所有启用的加载器
                    enabledPackages.forEach { pkgId ->
                        // 从 AddonManager 获取对应的 ModManager
                        val modManager = AddonManager.modManagersList[pkgId]
                        if (modManager != null) {
                            // 获取该管理器中的所有 Mod
                            val mods = modManager.packs // 或 modManager.values
                            mods.forEach { mod ->
                                if (isModMatchingSearch(mod, lowerQuery)) {
                                    ModLoaderManager.packs.find { it.pkgId == pkgId }?.let { loader ->
                                        results.add(loader to mod)
                                    }
                                }
                            }
                        }
                    }

                    // 按相关度排序
                    results.sortedByDescending { (_, mod) ->
                        calculateRelevance(mod, lowerQuery)
                    }
                }
            }
        }.value

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = Strings.manager.search.noResults,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isEmpty()) Strings.manager.search.hint else Strings.manager.search.noResults,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (query.isEmpty()) Strings.manager.search.hintDesc
                        else Strings.manager.search.tryOther,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = Strings.manager.search.results,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    Strings.manager.search.resultCount(searchResults.size),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = null,
                            elevation = null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = searchResults,
                            key = { (loader, mod) -> "${loader.pkgId}_${mod.pkgId}" }
                        ) { (loader, mod) ->
                            val manager = AddonManager.modManagersList[loader.pkgId]
                            if (manager != null) {
                                // 检查 Mod 是否存在且已启用
                                var enabled by remember {
                                    mutableStateOf(manager.isEnabled(mod.pkgId))
                                }

                                ModItemCard(
                                    mod = mod,
                                    enabled = enabled,
                                    customIconPath = manager.getIconFilePath(mod.pkgId),
                                    onEnableChange = { enable ->
                                        enabled = enable
                                        if (enable) {
                                            manager.enable(mod.pkgId)
                                        } else {
                                            manager.disable(mod.pkgId)
                                        }
                                    },
                                    onDelete = {
                                        // 删除 Mod
                                        runBlocking {
                                            manager.delete(mod.pkgId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun isModMatchingSearch(mod: ModItem, query: String): Boolean {
        if (query.isEmpty()) return false

        return mod.name.isNotBlank() && mod.name.lowercase().contains(query) ||
                mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query) ||
                mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query) ||
                mod.description.isNotBlank() && mod.description.lowercase().contains(query) ||
                mod.author.isNotBlank() && mod.author.lowercase().contains(query)
    }

    private fun calculateRelevance(mod: ModItem, query: String): Int {
        if (query.isEmpty()) return 0

        var relevance = 0
        if (mod.name.isNotBlank() && mod.name.lowercase().contains(query)) relevance += 5
        if (mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query)) relevance += 4
        if (mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query)) relevance += 3
        if (mod.description.isNotBlank() && mod.description.lowercase().contains(query)) relevance += 2
        if (mod.author.isNotBlank() && mod.author.lowercase().contains(query)) relevance += 2
        return relevance
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NormalTabView(
        selectedTab: Int,
        pagerState: PagerState,
        enabledLoaders: MutableMap<String, Boolean>,
        onTabSelected: (Int) -> Unit,
        showAddonInstallOrUpdateDialog: MutableState<Boolean>
    ) {
        val installFiles = remember { mutableListOf<Path>() }

        val filePickerLauncher = rememberFilePickerLauncher(
            mode = FileKitMode.Multiple(maxItems = 10)
        ) { files ->
            installFiles.clear()
            files?.forEach { file ->
                val tmp = kotlinx.io.files.Path((Platform.getDirectory("tmp") / "install_tmp" / file.nameWithoutExtension).toString())
                tmp.parent?.let { SystemFileSystem.createDirectories(it) }
                SystemFileSystem.sink(tmp).buffered().use { sink ->
                    sink.write(file.source(), file.size())
                    sink.flush()
                    installFiles.add(tmp.toString().toPath())
                }
                showAddonInstallOrUpdateDialog.value = true
            }
        }

        val loaderStates by remember(enabledLoaders) {
            derivedStateOf { enabledLoaders.toMap() }
        }

        if (showAddonInstallOrUpdateDialog.value) {
            AddonInstallOrUpdateDialog(installFiles) {
                FileSystem.SYSTEM.deleteRecursively(Platform.getDirectory("tmp") / "install_tmp")
                installFiles.clear()
                showAddonInstallOrUpdateDialog.value = false

                val newCategories = mutableListOf<ManagerTab>()
                newCategories.addAll(categories.filter { tab ->
                    tab.id == "kernel" || tab.id == "kernel_plugins" || tab.id == "mod_loaders"
                })
                ModLoaderManager.packs.forEach { loader ->
                    newCategories.add(ManagerTab(
                        id = loader.pkgId,
                        title = loader.name,
                        iconPath = ModLoaderManager.getIconFilePath(loader.pkgId)
                    ))
                }
                categories.clear()
                categories.addAll(newCategories)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // M3E Tab Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        val isLoaderTab = !tab.isKernel && !tab.isKernelPlugin && !tab.isModLoader
                        val loaderEnabled = if (isLoaderTab) {
                            loaderStates[tab.id] == true
                        } else true

                        M3ETab(
                            tab = tab,
                            isSelected = isSelected,
                            isEnabled = loaderEnabled,
                            onClick = {
                                if (isLoaderTab && !loaderEnabled) return@M3ETab
                                onTabSelected(index)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val tab = categories.getOrNull(page) ?: return@HorizontalPager

                when {
                    tab.isKernel -> KernelModulesSection(
                        onAddModule = { filePickerLauncher.launch() }
                    )
                    tab.isKernelPlugin -> KernelPluginsSection(
                        onAddPlugin = { filePickerLauncher.launch() }
                    )
                    tab.isModLoader -> ModLoadersSection(
                        enabledLoaders = enabledLoaders,
                        onLoaderToggle = { loaderId, enabled ->
                            enabledLoaders[loaderId] = enabled
                            if (enabled) {
                                ModLoaderManager.enable(loaderId)
                            } else {
                                ModLoaderManager.disable(loaderId)
                                AddonManager.closeModManager(loaderId)
                            }
                        },
                        onAddModLoader = { filePickerLauncher.launch() },
                        onRefresh = {
                            ModLoaderManager.refreshPacksList()
                        }
                    )
                    else -> {
                        val loader = ModLoaderManager.packs.find { it.pkgId == tab.id }
                        if (loader != null && loaderStates[loader.pkgId] == true) {
                            ModListSection(
                                loader = loader,
                                onRefresh = {
                                    AddonManager.getOrCreateModManager(loader.pkgId, true)?.refreshPacksList()
                                },
                                onAddMod = { filePickerLauncher.launch() }
                            )
                        } else {
                            DisabledLoaderSection(loader = loader)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun M3ETab(
        tab: ManagerTab,
        isSelected: Boolean,
        isEnabled: Boolean,
        onClick: () -> Unit
    ) {
        val containerColor = if (isSelected) {
            when {
                tab.isKernel -> MaterialTheme.colorScheme.tertiaryContainer
                tab.isKernelPlugin -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        } else {
            Color.Transparent
        }

        val contentColor = if (isSelected) {
            when {
                tab.isKernel -> MaterialTheme.colorScheme.onTertiaryContainer
                tab.isKernelPlugin -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        } else if (!isEnabled) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 图标
                tab.iconPath?.let { path ->
                    if (FileSystem.SYSTEM.exists(path)) {
                        KamelImage(
                            resource = { asyncPainterResource(data = path.toFileUrlString()) },
                            contentDescription = tab.title,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        Icon(
                            imageVector = tab.icon ?: Icons.Rounded.Dashboard,
                            contentDescription = tab.title,
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                    }
                } ?: Icon(
                    imageVector = tab.icon ?: Icons.Rounded.Dashboard,
                    contentDescription = tab.title,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )

                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun KernelModulesSection(
        onAddModule: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            SectionHeader(
                title = Strings.manager.module.title,
                subtitle = Strings.manager.module.subtitle,
                icon = Icons.Rounded.Memory,
                action = {
                    FilledTonalButton(
                        onClick = onAddModule,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.manager.module.add, style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ModuleManager.packs) { module ->
                    var enabled by remember { mutableStateOf(ModuleManager.isEnabled(module.pkgId)) }

                    ModuleItemCard(
                        module = module,
                        enabled,
                        customIconPath = ModuleManager.getIconFilePath(module.pkgId),
                        onEnableChange = {
                            enabled = it
                            if (it) ModuleManager.enable(module.pkgId)
                            else ModuleManager.disable(module.pkgId)
                        },
                        onDelete = {
                            runBlocking {
                                ModuleManager.delete(module.pkgId)
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun KernelPluginsSection(
        onAddPlugin: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            SectionHeader(
                title = Strings.manager.plugin.title,
                subtitle = Strings.manager.plugin.subtitle,
                icon = Icons.Rounded.Extension,
                action = {
                    FilledTonalButton(
                        onClick = onAddPlugin,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.manager.plugin.add, style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PluginManager.packs) { plugin ->
                    PluginItemCard(
                        plugin = plugin,
                        PluginManager.getIconFilePath(plugin.pkgId)
                    ) {
                        runBlocking {
                            PluginManager.delete(plugin.pkgId)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModLoadersSection(
        enabledLoaders: MutableMap<String, Boolean>,
        onLoaderToggle: (String, Boolean) -> Unit,
        onAddModLoader: () -> Unit,
        onRefresh: () -> Unit
    ) {
        val loaderStates by remember(enabledLoaders) {
            derivedStateOf { enabledLoaders.toMap() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = Strings.manager.modloader.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = Strings.manager.modloader.titleDec,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val enabledCount = loaderStates.values.count { it }
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = {
                        Text(
                            Strings.manager.modloader.enabled("${enabledCount}/${ModLoaderManager.packs.size}"),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddModLoader,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.manager.modloader.add, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.refresh, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.manager.modloader.prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ModLoaderManager.packs) { loader ->
                    val enabled by remember { mutableStateOf(loaderStates[loader.pkgId] ?: ModLoaderManager.isEnabled(
                        loader.pkgId
                    )) }

                    ModLoaderItemCard(
                        modLoader = loader,
                        enabled = enabled,
                        customIconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon",
                        onEnableChange = { onLoaderToggle(loader.pkgId, it) },
                        onDelete = {
                            runBlocking {
                                ModLoaderManager.delete(loader.pkgId)
                                categories.removeAll { it.id == loader.pkgId }
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun ModListSection(
        loader: ModLoaderItem,
        onRefresh: () -> Unit,
        onAddMod: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            SectionHeader(
                title = loader.name,
                subtitle = loader.description,
                icon = Icons.Rounded.GridView,
                action = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onAddMod,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.manager.mod.add, style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.refresh, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AddonManager.modManagersList[loader.pkgId]?.let { currentModList ->
                    items(currentModList.packs) { mod ->
                        val manager = AddonManager.getOrCreateModManager(loader.pkgId)!!
                        var enabled by remember { mutableStateOf(manager.isEnabled(mod.pkgId)) }

                        ModItemCard(
                            mod = mod,
                            enabled = enabled,
                            customIconPath = manager.getIconFilePath(mod.pkgId),
                            onEnableChange = { enable ->
                                enabled = enable
                                if (enable) manager.enable(mod.pkgId)
                                else manager.disable(mod.pkgId)
                            },
                            onDelete = {
                                runBlocking {
                                    manager.delete(mod.pkgId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(
        title: String,
        subtitle: String,
        icon: ImageVector? = null,
        action: @Composable (() -> Unit)? = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            action?.invoke()
        }
    }

    @Composable
    private fun DisabledLoaderSection(loader: ModLoaderItem?) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ToggleOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                Text(
                    text = Strings.manager.modloader.disabled,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = Strings.manager.modloader.disabledHint(loader?.name ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    Strings.manager.searchMod,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }

    override val title: String
        get() = Strings.manager.title
}
