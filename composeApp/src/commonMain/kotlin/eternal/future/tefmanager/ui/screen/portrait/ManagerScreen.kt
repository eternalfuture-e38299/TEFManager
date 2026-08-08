package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt
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
 * Created: 2026/2/4
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
        val fabText: String,
        val fabAction: () -> Unit = {},
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

        categories = (listOf(
            ManagerTab(
                id = "kernel",
                title = Strings.manager.module.title,
                icon = Icons.Rounded.Memory,
                isKernel = true,
                fabText = Strings.manager.module.add,
                fabAction = { filePickerLauncher.launch() }
            ),
            ManagerTab(
                id = "kernel_plugins",
                title = Strings.manager.plugin.title,
                icon = Icons.Rounded.Extension,
                isKernelPlugin = true,
                fabText = Strings.manager.plugin.add,
                fabAction = { filePickerLauncher.launch() }
            ),
            ManagerTab(
                id = "mod_loaders",
                title = Strings.manager.modloader.title,
                icon = Icons.Rounded.Dashboard,
                isModLoader = true,
                fabText = Strings.manager.modloader.add,
                fabAction = { filePickerLauncher.launch() }
            )
        ) + ModLoaderManager.packs.map { loader ->
            ManagerTab(
                id = loader.pkgId,
                title = loader.name,
                fabText = Strings.manager.mod.add,
                fabAction = { filePickerLauncher.launch() },
                iconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon"
            )
        }).toMutableStateList()

        if (showAddonInstallOrUpdateDialog.value) {
            AddonInstallOrUpdateDialog(installFiles) {
                FileSystem.SYSTEM.deleteRecursively(Platform.getDirectory("tmp") / "install_tmp")
                val newCategories = mutableListOf<ManagerTab>()

                newCategories.addAll(categories.filter { tab ->
                    tab.id == "kernel" || tab.id == "kernel_plugins" || tab.id == "mod_loaders"
                })

                ModLoaderManager.packs.forEach { loader ->
                    newCategories.add(ManagerTab(
                        id = loader.pkgId,
                        title = loader.name,
                        fabText = "添加模组",
                        fabAction = { filePickerLauncher.launch() },
                        iconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon"
                    ))
                }

                categories.clear()
                categories.addAll(newCategories)

                showAddonInstallOrUpdateDialog.value = false
            }
        }

        val enabledLoaders = remember {
            mutableStateMapOf<String, Boolean>()
        }

        val currentCategory = categories.getOrNull(selectedTab)

        LaunchedEffect(ModLoaderManager.packs) {
            AddonManager.refreshModManager(ModLoaderManager.packs)
            ModLoaderManager.packs.forEach {
                enabledLoaders[it.pkgId] = ModLoaderManager.isEnabled(it.pkgId)
            }
        }

        LaunchedEffect(searchQuery) {
            delay(300.milliseconds)
            debouncedSearchQuery = searchQuery
        }

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (searchQuery.isEmpty()) {
                    NormalTabView(
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        enabledLoaders = enabledLoaders,
                        onTabSelected = { index ->
                            selectedTab = index
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                } else {
                    SearchResultsView(
                        searchQuery = debouncedSearchQuery,
                        enabledLoaders = enabledLoaders
                    )
                }
            }

            currentCategory?.let { category ->
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                ExtendedFloatingActionButton(
                    onClick = category.fabAction,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp).offset {
                            IntOffset(
                                offsetX.roundToInt(),
                                offsetY.roundToInt()
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = category.fabText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.fabText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
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
                                    ModLoaderManager.packs.find { it.pkgId == pkgId }
                                        ?.let { loader ->
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

        if (mod.name.isNotBlank() && mod.name.lowercase().contains(query)) return true
        if (mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query)) return true
        if (mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query)) return true
        if (mod.description.isNotBlank() && mod.description.lowercase().contains(query)) return true
        if (mod.author.isNotBlank() && mod.author.lowercase().contains(query)) return true

        return false
    }

    private fun calculateRelevance(mod: ModItem, query: String): Int {
        if (query.isEmpty()) return 0

        var relevance = 0

        if (mod.name.isNotBlank() && mod.name.lowercase().contains(query)) {
            relevance += 5
        }
        if (mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query)) {
            relevance += 4
        }
        if (mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query)) {
            relevance += 3
        }
        if (mod.description.isNotBlank() && mod.description.lowercase().contains(query)) {
            relevance += 2
        }
        if (mod.author.isNotBlank() && mod.author.lowercase().contains(query)) {
            relevance += 2
        }

        return relevance
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NormalTabView(
        selectedTab: Int,
        pagerState: PagerState,
        enabledLoaders: MutableMap<String, Boolean>,
        onTabSelected: (Int) -> Unit
    ) {
        val loaderStates by remember(enabledLoaders) {
            derivedStateOf { enabledLoaders.toMap() }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CompactTabRow(
                selectedTab = selectedTab,
                loaderStates = loaderStates,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val tab = categories.getOrNull(page)

                when {
                    tab?.isKernel == true -> KernelModulesSection()
                    tab?.isKernelPlugin == true -> KernelPluginsSection()
                    tab?.isModLoader == true -> ModLoadersSection(
                        enabledLoaders = enabledLoaders,
                        onLoaderToggle = { loaderId, enabled ->
                            enabledLoaders[loaderId] = enabled
                            if (enabled) {
                                ModLoaderManager.enable(loaderId)
                                AddonManager.getOrCreateModManager(loaderId, true)
                            } else {
                                ModLoaderManager.disable(loaderId)
                                AddonManager.closeModManager(loaderId)
                            }
                        }
                    )
                    else -> {
                        val loader = ModLoaderManager.packs.find { it.pkgId == tab?.id }
                        if (loader != null && loaderStates[loader.pkgId] == true) {
                            ModListSection(loader = loader)
                        } else {
                            DisabledLoaderSection(loader = loader)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactTabRow(
        selectedTab: Int,
        loaderStates: Map<String, Boolean>,
        onTabSelected: (Int) -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { tab ->
                    val index = categories.indexOf(tab)
                    val isSelected = selectedTab == index

                    CompactTab(
                        tab = tab,
                        isSelected = isSelected,
                        loaderStates = loaderStates,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactTab(
        tab: ManagerTab,
        isSelected: Boolean,
        loaderStates: Map<String, Boolean>,
        onClick: () -> Unit
    ) {
        val isLoaderTab = !tab.isKernel && !tab.isKernelPlugin && !tab.isModLoader
        val loaderEnabled = if (isLoaderTab) {
            loaderStates[tab.id] == true
        } else true

        val containerColor = if (isSelected) {
            when {
                tab.isKernel -> MaterialTheme.colorScheme.tertiaryContainer
                tab.isKernelPlugin -> MaterialTheme.colorScheme.secondaryContainer
                tab.isModLoader -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        } else if (!loaderEnabled) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

        val contentColor = if (isSelected) {
            when {
                tab.isKernel -> MaterialTheme.colorScheme.onTertiaryContainer
                tab.isKernelPlugin -> MaterialTheme.colorScheme.onSecondaryContainer
                tab.isModLoader -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        } else if (!loaderEnabled) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            onClick = {
                if (isLoaderTab && !loaderEnabled) return@Surface
                onClick()
            },
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tab.iconPath?.let { path ->
                    if (FileSystem.SYSTEM.exists(path)) {
                        KamelImage(
                            resource = { asyncPainterResource(data = path.toFileUrlString()) },
                            contentDescription = tab.title,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = tab.title,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    }
                } ?: run {
                    tab.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = tab.title,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    } ?: Icon(
                        imageVector = Icons.Rounded.Dashboard,
                        contentDescription = tab.title,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                }

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
    private fun KernelModulesSection() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(
                title = Strings.manager.module.title,
                subtitle = Strings.manager.module.subtitle,
                icon = Icons.Rounded.Memory
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
    private fun KernelPluginsSection() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(
                title = Strings.manager.plugin.title,
                subtitle = Strings.manager.plugin.subtitle,
                icon = Icons.Rounded.Extension
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
        onLoaderToggle: (String, Boolean) -> Unit
    ) {
        val loaderStates by remember(enabledLoaders) {
            derivedStateOf { enabledLoaders.toMap() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
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
                    val enabled by remember {
                        mutableStateOf(
                            loaderStates[loader.pkgId] ?: ModLoaderManager.isEnabled(
                        loader.pkgId
                            )
                        )
                    }

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
        loader: ModLoaderItem
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(
                title = loader.name,
                subtitle = loader.description,
                icon = Icons.Rounded.GridView
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
        icon: ImageVector? = null
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun DisabledLoaderSection(loader: ModLoaderItem?) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ToggleOff,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Strings.manager.modloader.disabled,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.manager.modloader.disabledHint(loader?.name ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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