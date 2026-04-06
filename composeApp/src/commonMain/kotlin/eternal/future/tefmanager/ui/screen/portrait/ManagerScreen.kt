package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
                fabText = "添加模组加载器",
                fabAction = { filePickerLauncher.launch() }
            )
        ) + modLoaders.map { loader ->
            ManagerTab(
                id = loader.pkgId,
                title = loader.name,
                fabText = "添加Mod",
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

                // 只添加基础标签
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

        // 使用 mutableStateMapOf 来管理加载器状态
        val enabledLoaders = remember {
            mutableStateMapOf<String, Boolean>()
        }

        val currentCategory = categories[selectedTab]

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

        // 防抖处理：延迟300ms更新搜索
        LaunchedEffect(searchQuery) {
            delay(300)
            debouncedSearchQuery = searchQuery
        }


        // 同步分页状态和标签选择
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 搜索栏
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )


                Spacer(modifier = Modifier.height(16.dp))

                if (searchQuery.isEmpty()) {
                    // 正常标签页模式
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
                    // 搜索模式
                    SearchResultsView(
                        searchQuery = debouncedSearchQuery,
                        enabledLoaders = enabledLoaders
                    ) {
                        if (it.globalConfig.fileType != "null" && it.globalConfig.fileType.isNotEmpty())
                            globalConfig.value = it.globalConfig
                    }
                }
            }

            // 悬浮按钮
            ExtendedFloatingActionButton(
                onClick = { currentCategory.fabAction() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = currentCategory.fabText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentCategory.fabText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
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

        // 使用 derivedStateOf 优化搜索性能
        val searchResults = remember(query, enabledLoaders, modList) {
            derivedStateOf {
                if (query.isEmpty()) {
                    emptyList()
                } else {
                    val lowerQuery = query.lowercase()
                    val results = mutableListOf<Pair<ModLoaderItem, ModItem>>()


                    modList.forEach {
                        // 对每个模组进行搜索
                        it.value.forEach { mod ->
                            // 安全地检查每个字段是否匹配
                            val matches = isModMatchingSearch(mod, lowerQuery)
                            if (matches) {
                                results.add(modLoaders.find { modLoader -> modLoader.pkgId == it.key }!! to mod)
                            }
                        }
                    }

                    // 根据相关性排序
                    results.sortedByDescending { (_, mod) ->
                        calculateRelevance(mod, lowerQuery)
                    }
                }
            }
        }.value

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp
        ) {
            if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = "无结果",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isEmpty()) "输入关键词开始搜索" else "未找到相关模组",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (query.isEmpty()) "在搜索框中输入模组名称、作者、描述等关键词"
                        else "尝试使用其他关键词搜索，或确保相关加载器已启用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // 搜索结果标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索结果",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "搜索结果",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${searchResults.size} 个结果",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 搜索结果列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = searchResults,
                            key = { (loaderName, mod) -> "${loaderName}_${mod.pkgId}" }
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

    // 辅助函数：检查模组是否匹配搜索条件
    private fun isModMatchingSearch(mod: ModItem, query: String): Boolean {
        if (query.isEmpty()) return false

        // 检查 name（非空）
        if (mod.name.isNotBlank() && mod.name.lowercase().contains(query)) {
            return true
        }

        // 检查 pkgId（非空）
        if (mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query)) {
            return true
        }

        // 检查 brieflyDescribe（可能为空或空字符串）
        if (mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query)) {
            return true
        }

        // 检查 description（可能为空或空字符串）
        if (mod.description.isNotBlank() && mod.description.lowercase().contains(query)) {
            return true
        }

        // 检查 author（可能为空或空字符串）
        if (mod.author.isNotBlank() && mod.author.lowercase().contains(query)) {
            return true
        }

        return false
    }

    // 辅助函数：计算搜索相关性分数
    private fun calculateRelevance(mod: ModItem, query: String): Int {
        if (query.isEmpty()) return 0

        var relevance = 0

        // name 匹配得分最高
        if (mod.name.isNotBlank() && mod.name.lowercase().contains(query)) {
            relevance += 5
        }

        // pkgId 匹配得分次高
        if (mod.pkgId.isNotBlank() && mod.pkgId.lowercase().contains(query)) {
            relevance += 4
        }

        // brieflyDescribe 匹配
        if (mod.brieflyDescribe.isNotBlank() && mod.brieflyDescribe.lowercase().contains(query)) {
            relevance += 3
        }

        // description 匹配
        if (mod.description.isNotBlank() && mod.description.lowercase().contains(query)) {
            relevance += 2
        }

        // author 匹配
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
        // 监听 enabledLoaders 的变化
        val loaderStates by remember(enabledLoaders) {
            derivedStateOf { enabledLoaders.toMap() }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 紧凑型标签栏
            CompactTabRow(
                selectedTab = selectedTab,
                loaderStates = loaderStates,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 水平分页器
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val tab = categories[page]

                when {
                    tab.isKernel -> KernelModulesSection {
                        globalConfig.value = it.globalConfig
                    }
                    tab.isKernelPlugin -> KernelPluginsSection()
                    tab.isModLoader -> ModLoadersSection(
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
                        val loader = modLoaders.find { it.pkgId == tab.id }
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
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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

        val animatedColor by animateColorAsState(
            targetValue = if (isSelected) {
                when {
                    tab.isKernel -> MaterialTheme.colorScheme.onTertiaryContainer
                    tab.isKernelPlugin -> MaterialTheme.colorScheme.onSecondaryContainer
                    tab.isModLoader -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            animationSpec = tween(durationMillis = 300),
            label = "tabColor"
        )

        val backgroundColor = if (isSelected) {
            when {
                tab.isKernel -> MaterialTheme.colorScheme.tertiaryContainer
                tab.isKernelPlugin -> MaterialTheme.colorScheme.secondaryContainer
                tab.isModLoader -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        } else if (!loaderEnabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
        }

        Surface(
            onClick = {
                if (isLoaderTab && !loaderEnabled) return@Surface
                onClick()
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 优先显示文件图标
                tab.iconPath?.let { path ->
                    if (FileSystem.SYSTEM.exists(path)) {
                        KamelImage(
                            resource = { asyncPainterResource(data = path.toFileUrlString()) },
                            contentDescription = tab.title,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        )
                    } else if (tab.icon != null) {
                        // 其次使用内置图标
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (!loaderEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            } else {
                                animatedColor
                            }
                        )
                    } else {
                        // 默认的模组加载器图标
                        Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = tab.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (!loaderEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            } else {
                                animatedColor
                            }
                        )
                    }
                } ?: run {
                    // 没有 iconPath 时的处理
                    tab.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = tab.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (!loaderEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            } else {
                                animatedColor
                            }
                        )
                    } ?: Icon(
                        imageVector = Icons.Rounded.Dashboard,
                        contentDescription = tab.title,
                        modifier = Modifier.size(16.dp),
                        tint = if (!loaderEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        } else {
                            animatedColor
                        }
                    )
                }

                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (!loaderEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    } else {
                        animatedColor
                    },
                    maxLines = 1
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
            // 分类标题和信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "内核模块",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "系统级核心模块，优化游戏运行性能",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内核模块列表
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
            // 分类标题和信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "内核插件",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "为模组加载器提供扩展功能，增强模组开发能力",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内核插件列表
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
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "模组加载器管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "管理不同的模组加载器，启用后即可管理对应的模组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val enabledCount = loaderStates.values.count { it }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${enabledCount}/${modLoaders.size} 已启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示信息
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
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
                        text = "禁用加载器可以隐藏其模组列表，减少内存占用。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 加载器列表
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
            // 分类标题和信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = loader.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = loader.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 模组列表
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
    private fun DisabledLoaderSection(loader: ModLoaderItem?) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ToggleOff,
                contentDescription = "加载器已禁用",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "加载器已禁用",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请在\"模组加载器\"页面中启用${loader?.name ?: "此加载器"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
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
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            placeholder = { Text("搜索模组...") },
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
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )
    }

    override val title: String
        get() = "模组管理"
}