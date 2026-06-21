package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.component.ModItemCard
import eternal.future.tefmanager.ui.component.ModLoaderItemCard
import eternal.future.tefmanager.ui.component.ModuleItemCard
import eternal.future.tefmanager.ui.component.PluginItemCard
import eternal.future.tefmanager.ui.dialogs.AddonInstallOrUpdateDialog
import eternal.future.tefmanager.ui.dialogs.ConfigManager
import eternal.future.tefmanager.ui.dialogs.GlobalConfigEditor
import eternal.future.tefmanager.ui.model.GlobalConfig
import eternal.future.tefmanager.ui.model.ModItem
import eternal.future.tefmanager.ui.model.ModLoaderItem
import eternal.future.tefmanager.ui.model.ModuleItem
import eternal.future.tefmanager.utils.AddonManager
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
 * Created: 2026/2/4
 *******************************************************************************/

object ManagerScreen : Screen, MainScreen.TitledScreen {

    private val kernelModules = AddonManager.modulesDataBase.getAllValues().toMutableStateList()
    private val kernelPlugins = AddonManager.pluginsDataBase.getAllValues().toMutableStateList()
    private val modLoaders = AddonManager.modLoaderDataBase.getAllValues().toMutableStateList()
    private val modList = mutableStateMapOf<String, SnapshotStateList<ModItem>>()
    private var categories = mutableStateListOf<ManagerTab>()

    data class ManagerTab(
        val id: String,
        val title: String,
        val icon: ImageVector? = null,
        val isKernel: Boolean = false,
        val isKernelPlugin: Boolean = false,
        val isModLoader: Boolean = false,
        val fabText: String = "添加",
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

        val globalConfig = remember { mutableStateOf<GlobalConfig?>(null) }
        val showAddonInstallOrUpdateDialog = remember { mutableStateOf(false) }

        val installFiles = remember { mutableListOf<Path>() }
        val filePickerLauncher = rememberFilePickerLauncher(
            mode = FileKitMode.Multiple(maxItems = 10)
        ) { files ->
            installFiles.clear()
            files?.forEach { file ->
                val tmp = kotlinx.io.files.Path((Platform.getData("install_tmp") / file.nameWithoutExtension).toString())

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
                title = "内核模块",
                icon = Icons.Rounded.Memory,
                isKernel = true,
                fabText = "添加模块",
                fabAction = { filePickerLauncher.launch() }
            ),
            ManagerTab(
                id = "kernel_plugins",
                title = "内核插件",
                icon = Icons.Rounded.Extension,
                isKernelPlugin = true,
                fabText = "添加插件",
                fabAction = { filePickerLauncher.launch() }
            ),
            ManagerTab(
                id = "mod_loaders",
                title = "模组加载器",
                icon = Icons.Rounded.Dashboard,
                isModLoader = true,
                fabText = "添加加载器",
                fabAction = { filePickerLauncher.launch() }
            )
        ) + modLoaders.map { loader ->
            ManagerTab(
                id = loader.pkgId,
                title = loader.name,
                fabText = "添加模组",
                fabAction = { filePickerLauncher.launch() },
                iconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon"
            )
        }).toMutableStateList()

        if (showAddonInstallOrUpdateDialog.value) {
            AddonInstallOrUpdateDialog(installFiles) {
                FileSystem.SYSTEM.deleteRecursively(Platform.getData("install_tmp"))

                installFiles.clear()
                showAddonInstallOrUpdateDialog.value = false

                modLoaders.clear()
                modLoaders.addAll(AddonManager.modLoaderDataBase.getAllValues())

                kernelModules.clear()
                kernelModules.addAll(AddonManager.modulesDataBase.getAllValues())

                kernelPlugins.clear()
                kernelPlugins.addAll(AddonManager.pluginsDataBase.getAllValues())

                modList.forEach {
                    it.value.clear()
                    it.value.addAll(AddonManager.getOrCreateModDatabase(it.key)!!.getAllValues())
                }

                val newCategories = mutableListOf<ManagerTab>()

                newCategories.addAll(categories.filter { tab ->
                    tab.id == "kernel" || tab.id == "kernel_plugins" || tab.id == "mod_loaders"
                })

                modLoaders.forEach { loader ->
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
            }
        }

        if (globalConfig.value != null) {
            GlobalConfigEditor(globalConfig.value!!,
                ConfigManager.readConfigFile(globalConfig.value!!.generateFile),
                {
                    ConfigManager.writeConfigFile(globalConfig.value!!.generateFile, it)
                },
                {
                    globalConfig.value = null
                }
            )
        }

        val enabledLoaders = remember {
            mutableStateMapOf<String, Boolean>()
        }

        val currentCategory = categories.getOrNull(selectedTab)

        LaunchedEffect(modLoaders) {
            AddonManager.refreshModDatabases(modLoaders)
            enabledLoaders.apply {
                modLoaders.forEach {
                    val enabled = AddonManager.isAddonEnabled("modloader", it.pkgId)
                    put(
                        it.pkgId,
                        enabled
                    )

                    if (enabled) {
                        AddonManager.modDataBaseList[it.pkgId]?.getAllValues()?.toMutableStateList()?.let { list ->
                            modList[it.pkgId] = list
                        }
                    }
                }
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
                        },
                        globalConfig = globalConfig
                    )
                } else {
                    SearchResultsView(
                        searchQuery = debouncedSearchQuery,
                        enabledLoaders = enabledLoaders
                    ) {
                        if (it.globalConfig.fileType != "null" && it.globalConfig.fileType.isNotEmpty())
                            globalConfig.value = it.globalConfig
                    }
                }
            }

            currentCategory?.let { category ->
                ExtendedFloatingActionButton(
                    onClick = category.fabAction,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
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
        enabledLoaders: SnapshotStateMap<String, Boolean>,
        configureCallback: ((ModItem) -> Unit)?
    ) {
        val query = searchQuery.trim()

        val searchResults = remember(query, enabledLoaders, modList) {
            derivedStateOf {
                if (query.isEmpty()) {
                    emptyList()
                } else {
                    val lowerQuery = query.lowercase()
                    val results = mutableListOf<Pair<ModLoaderItem, ModItem>>()

                    modList.forEach {
                        it.value.forEach { mod ->
                            val matches = isModMatchingSearch(mod, lowerQuery)
                            if (matches) {
                                modLoaders.find { modLoader -> modLoader.pkgId == it.key }?.let { loader ->
                                    results.add(loader to mod)
                                }
                            }
                        }
                    }

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
                        contentDescription = "无结果",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isEmpty()) "输入关键词开始搜索" else "未找到相关模组",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (query.isEmpty()) "搜索模组名称、作者、描述等关键词"
                        else "尝试使用其他关键词，或确保相关加载器已启用",
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
                                    contentDescription = "搜索结果",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "搜索结果",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "${searchResults.size} 个结果",
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
                            ModItemCard(
                                mod = mod,
                                enabled = AddonManager.isAddonEnabled("mods", mod.pkgId, loader.pkgId),
                                customIconPath = Platform.getData("mods") / loader.pkgId / "icons" / "${mod.pkgId}.icon",
                                onEnableChange = { enable ->
                                    if (enable) AddonManager.enableAddon("mods", mod.pkgId, loader.pkgId)
                                    else AddonManager.disableAddon("mods", mod.pkgId, loader.pkgId)
                                },
                                onDelete = {
                                    runBlocking {
                                        AddonManager.deleteMod(mod.pkgId, loader.pkgId)
                                        modList[loader.pkgId]?.remove(mod)
                                    }
                                },
                                configureCallback
                            )
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
        onTabSelected: (Int) -> Unit,
        globalConfig: MutableState<GlobalConfig?>
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
                    tab?.isKernel == true -> KernelModulesSection {
                        globalConfig.value = it.globalConfig
                    }
                    tab?.isKernelPlugin == true -> KernelPluginsSection()
                    tab?.isModLoader == true -> ModLoadersSection(
                        enabledLoaders = enabledLoaders,
                        onLoaderToggle = { loaderId, enabled ->
                            enabledLoaders[loaderId] = enabled
                            if (enabled) {
                                AddonManager.enableAddon("modloader", loaderId)
                                modList[loaderId] = AddonManager.getOrCreateModDatabase(loaderId, true)!!.getAllValues()
                                    .toMutableStateList()
                            } else {
                                AddonManager.disableAddon("modloader", loaderId)
                                AddonManager.closeModDatabase(loaderId)
                                modList.remove(loaderId)
                            }
                        }
                    )
                    else -> {
                        val loader = modLoaders.find { it.pkgId == tab?.id }
                        if (loader != null && loaderStates[loader.pkgId] == true) {
                            ModListSection(loader = loader)
                            {
                                globalConfig.value = it.globalConfig
                            }
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
    private fun KernelModulesSection(
        configureCallback: ((ModuleItem) -> Unit)?
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(
                title = "内核模块",
                subtitle = "系统级核心模块，优化游戏运行性能",
                icon = Icons.Rounded.Memory
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(kernelModules) { module ->
                    ModuleItemCard(
                        module = module,
                        AddonManager.isAddonEnabled("module", module.pkgId),
                        customIconPath = Platform.getData("module") / "icons" / "${module.pkgId}.icon",
                        onEnableChange = {
                            if (it) AddonManager.enableAddon("module", module.pkgId)
                            else AddonManager.disableAddon("module", module.pkgId)
                        },
                        onDelete = {
                            runBlocking {
                                AddonManager.deleteModule(module.pkgId)
                                kernelModules.remove(module)
                            }
                        },
                        onConfigure = configureCallback
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
                title = "内核插件",
                subtitle = "为模组加载器提供扩展功能，增强模组开发能力",
                icon = Icons.Rounded.Extension
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(kernelPlugins) { plugin ->
                    PluginItemCard(
                        plugin = plugin,
                        Platform.getData("plugin") / "icons" / "${plugin.pkgId}.icon"
                    ) {
                        runBlocking {
                            AddonManager.deletePlugin(plugin.pkgId)
                            kernelPlugins.remove(plugin)
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
                        text = "模组加载器",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "启用后可管理对应的模组",
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
                            "${enabledCount}/${modLoaders.size} 已启用",
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
                        contentDescription = "提示",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "禁用加载器可隐藏其模组列表，减少内存占用",
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
                items(modLoaders) { loader ->
                    val enabled = loaderStates[loader.pkgId] ?: AddonManager.isAddonEnabled(
                        "modloader",
                        loader.pkgId
                    )

                    ModLoaderItemCard(
                        modLoader = loader,
                        enabled = enabled,
                        customIconPath = Platform.getData("modloader") / "icons" / "${loader.pkgId}.icon",
                        onEnableChange = { onLoaderToggle(loader.pkgId, it) },
                        onDelete = {
                            runBlocking {
                                AddonManager.deleteModLoader(loader.pkgId)
                                AddonManager.closeModDatabase(loader.pkgId)
                                modList.remove(loader.pkgId)
                                modLoaders.remove(loader)
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
        configureCallback: ((ModItem) -> Unit)?
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
                modList[loader.pkgId]?.let { currentModList ->
                    items(currentModList) { mod ->
                        ModItemCard(
                            mod = mod,
                            enabled = AddonManager.isAddonEnabled("mods", mod.pkgId, loader.pkgId),
                            customIconPath = Platform.getData("mods") / loader.pkgId / "icons" / "${mod.pkgId}.icon",
                            onEnableChange = { enable ->
                                if (enable) AddonManager.enableAddon("mods", mod.pkgId, loader.pkgId)
                                else AddonManager.disableAddon("mods", mod.pkgId, loader.pkgId)
                            },
                            onDelete = {
                                runBlocking {
                                    AddonManager.deleteMod(mod.pkgId, loader.pkgId)
                                    modList[loader.pkgId]?.remove(mod)
                                }
                            },
                            configureCallback
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
                contentDescription = "加载器已禁用",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "加载器已禁用",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "在「模组加载器」页面中启用 ${loader?.name ?: "此加载器"}",
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
                    "搜索模组...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "清除",
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
        get() = "模组管理"
}