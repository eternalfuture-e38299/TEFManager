package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Quickreply
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*******************************************************************************
 * TEFManager - ModManagerScreen
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

data class ModInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val downloadCount: Int = 0,
    val rating: Float = 0f,
    val enabled: Boolean = false,
    val dependencies: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val modLoader: String = "fabric"
)

data class ModLoader(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean = true,
    val icon: ImageVector = Icons.Rounded.Extension,
    val modCount: Int = 0
)

data class KernelModule(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean = true,
    val isEssential: Boolean = true
)

data class KernelPlugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean = true,
    val providesFunctions: List<String> = emptyList(),
    val supportedLoaders: List<String> = emptyList()
)

object ModManagerScreen : Screen, MainScreen.TitledScreen {

    // 模拟数据
    private val kernelModules = listOf(
        KernelModule(
            id = "core_optimization",
            name = "核心优化模块",
            description = "游戏核心性能优化，提供更好的运行效率",
            version = "1.0.0"
        ),
        KernelModule(
            id = "memory_manager",
            name = "内存管理器",
            description = "高级内存管理，减少内存泄漏和碎片",
            version = "1.2.1"
        ),
        KernelModule(
            id = "network_enhancement",
            name = "网络增强",
            description = "改进网络连接稳定性和速度",
            version = "1.1.5"
        ),
        KernelModule(
            id = "graphics_optimizer",
            name = "图形优化器",
            description = "优化图形渲染，提升帧率和画质",
            version = "2.0.3"
        ),
    )

    private val kernelPlugins = listOf(
        KernelPlugin(
            id = "api_provider",
            name = "API 提供器",
            description = "为模组加载器提供扩展API接口",
            version = "1.0.0",
            providesFunctions = listOf("事件系统", "配置管理", "网络通信"),
            supportedLoaders = listOf("fabric", "forge", "quilt")
        ),
        KernelPlugin(
            id = "dependency_manager",
            name = "依赖管理器",
            description = "管理模组间的依赖关系和版本冲突",
            version = "1.2.3",
            providesFunctions = listOf("依赖解析", "版本检查", "冲突检测"),
            supportedLoaders = listOf("fabric", "forge")
        ),
        KernelPlugin(
            id = "hot_reload",
            name = "热重载引擎",
            description = "支持模组热重载，无需重启游戏",
            version = "0.9.5",
            providesFunctions = listOf("热重载", "实时更新", "状态保存"),
            supportedLoaders = listOf("fabric", "quilt")
        ),
        KernelPlugin(
            id = "performance_monitor",
            name = "性能监控器",
            description = "监控模组性能，提供优化建议",
            version = "1.1.0",
            providesFunctions = listOf("性能分析", "内存监控", "CPU使用率"),
            supportedLoaders = listOf("fabric", "forge", "quilt", "rift")
        ),
    )

    private val modLoaders = listOf(
        ModLoader(
            id = "fabric",
            name = "Fabric",
            description = "轻量级模组加载器，高性能和兼容性好",
            version = "1.4.5",
            icon = Icons.Rounded.Bolt,
            modCount = 8
        ),
        ModLoader(
            id = "forge",
            name = "Forge",
            description = "功能丰富的模组加载器，支持大量模组",
            version = "1.3.2",
            icon = Icons.Rounded.Build,
            modCount = 12
        ),
        ModLoader(
            id = "quilt",
            name = "Quilt",
            description = "现代化的模组加载器，Fabric的增强版",
            version = "1.1.0",
            icon = Icons.Rounded.Quickreply,
            modCount = 5
        ),
        ModLoader(
            id = "rift",
            name = "Rift",
            description = "精简版加载器，适用于特殊需求",
            version = "0.8.3",
            icon = Icons.Rounded.Memory,
            modCount = 3
        ),
    )

    // 模拟 Mod 数据
    private val fabricMods = listOf(
        ModInfo(
            id = "sodium",
            name = "Sodium",
            description = "高性能渲染引擎，显著提升FPS",
            version = "1.4.5",
            author = "jellysquid3",
            downloadCount = 1500000,
            rating = 4.8f,
            enabled = true
        ),
        ModInfo(
            id = "iris",
            name = "Iris Shaders",
            description = "支持光影效果，兼容Sodium",
            version = "1.3.2",
            author = "coderbot",
            downloadCount = 800000,
            rating = 4.5f,
            enabled = true
        ),
        ModInfo(
            id = "lithium",
            name = "Lithium",
            description = "优化游戏逻辑，提升服务器性能",
            version = "1.1.0",
            author = "jellysquid3",
            downloadCount = 600000,
            rating = 4.6f
        ),
    )

    private val forgeMods = listOf(
        ModInfo(
            id = "optifine",
            name = "OptiFine",
            description = "经典性能优化和画质增强模组",
            version = "HD_U_H8",
            author = "sp614x",
            downloadCount = 2000000,
            rating = 4.7f,
            enabled = true
        ),
        ModInfo(
            id = "jei",
            name = "JEI",
            description = "物品管理器，显示所有合成配方",
            version = "7.6.4",
            author = "mezz",
            downloadCount = 1200000,
            rating = 4.9f
        ),
    )

    data class ManagerTab(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val isKernel: Boolean = false,
        val isKernelPlugin: Boolean = false,
        val isModLoader: Boolean = false,
        val fabText: String = "添加",
        val fabAction: () -> Unit = {}
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val categories = remember {
            listOf(
                ManagerTab(
                    id = "kernel",
                    title = "内核模块",
                    icon = Icons.Rounded.Memory,
                    isKernel = true,
                    fabText = "添加内核模块",
                    fabAction = { println("添加内核模块") }
                ),
                ManagerTab(
                    id = "kernel_plugins",
                    title = "内核插件",
                    icon = Icons.Rounded.Extension,
                    isKernelPlugin = true,
                    fabText = "添加内核插件",
                    fabAction = { println("添加内核插件") }
                ),
                ManagerTab(
                    id = "mod_loaders",
                    title = "模组加载器",
                    icon = Icons.Rounded.Dashboard,
                    isModLoader = true,
                    fabText = "管理加载器",
                    fabAction = { println("管理加载器") }
                )
            ) + modLoaders.map { loader ->
                ManagerTab(
                    id = loader.id,
                    title = loader.name,
                    icon = loader.icon,
                    fabText = "添加${loader.name}模组",
                    fabAction = { println("添加${loader.name}模组") }
                )
            }
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        var searchQuery by remember { mutableStateOf("") }
        var searchActive by remember { mutableStateOf(false) }
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

        // 使用 mutableStateMapOf 来管理加载器状态
        val enabledLoaders = remember { mutableStateMapOf<String, Boolean>().apply {
            modLoaders.forEach { put(it.id, it.enabled) }
        } }

        val currentCategory = categories[selectedTab]

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
                    onQueryChange = { searchQuery = it },
                    onSearch = { searchQuery = it },
                    onActiveChange = { searchActive = it },
                    active = searchActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (searchQuery.isEmpty()) {
                    // 正常标签页模式
                    NormalTabView(
                        tabs = categories,
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        coroutineScope = coroutineScope,
                        enabledLoaders = enabledLoaders,
                        onTabSelected = { index ->
                            selectedTab = index
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                } else {
                    // 搜索模式
                    SearchResultsView(
                        searchQuery = searchQuery,
                        enabledLoaders = enabledLoaders,
                        fabricMods = fabricMods,
                        forgeMods = forgeMods
                    )
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
        enabledLoaders: Map<String, Boolean>,
        fabricMods: List<ModInfo>,
        forgeMods: List<ModInfo>
    ) {
        val query = searchQuery.lowercase()

        // 收集所有启用的加载器的模组
        val allEnabledMods = remember(query, enabledLoaders, fabricMods, forgeMods) {
            val mods = mutableListOf<Pair<String, ModInfo>>()

            if (enabledLoaders["fabric"] == true) {
                fabricMods.filter { mod ->
                    mod.name.lowercase().contains(query) ||
                            mod.description.lowercase().contains(query) ||
                            mod.author.lowercase().contains(query) ||
                            mod.id.lowercase().contains(query)
                }.forEach { mod ->
                    mods.add("Fabric" to mod)
                }
            }

            if (enabledLoaders["forge"] == true) {
                forgeMods.filter { mod ->
                    mod.name.lowercase().contains(query) ||
                            mod.description.lowercase().contains(query) ||
                            mod.author.lowercase().contains(query) ||
                            mod.id.lowercase().contains(query)
                }.forEach { mod ->
                    mods.add("Forge" to mod)
                }
            }

            mods
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (allEnabledMods.isEmpty()) {
                // 无搜索结果
                NoSearchResults()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 搜索结果统计
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "搜索结果",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "共 ${allEnabledMods.size} 个结果",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 搜索结果列表
                    items(allEnabledMods) { (loaderName, mod) ->
                        SearchResultModCard(
                            mod = mod,
                            loaderName = loaderName
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NoSearchResults() {
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
                text = "未找到相关模组",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "尝试使用其他关键词搜索，或确保相关加载器已启用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    @Composable
    private fun SearchResultModCard(
        mod: ModInfo,
        loaderName: String
    ) {
        var enabled by remember { mutableStateOf(mod.enabled) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标区域
                Surface(
                    shape = CircleShape,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 信息区域
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = loaderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "作者: ${mod.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 开关区域
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NormalTabView(
        tabs: List<ManagerTab>,
        selectedTab: Int,
        pagerState: PagerState,
        coroutineScope: CoroutineScope,
        enabledLoaders: MutableMap<String, Boolean>,
        onTabSelected: (Int) -> Unit
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
                tabs = tabs,
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
                val tab = tabs[page]

                when {
                    tab.isKernel -> KernelModulesSection(
                        kernelModules = kernelModules
                    )
                    tab.isKernelPlugin -> KernelPluginsSection(
                        plugins = kernelPlugins
                    )
                    tab.isModLoader -> ModLoadersSection(
                        loaders = modLoaders,
                        enabledLoaders = enabledLoaders,
                        onLoaderToggle = { loaderId, enabled ->
                            enabledLoaders[loaderId] = enabled
                        }
                    )
                    else -> {
                        val loader = modLoaders.find { it.id == tab.id }
                        if (loader != null && loaderStates[loader.id] == true) {
                            ModListSection(
                                loader = loader,
                                mods = when (loader.id) {
                                    "fabric" -> fabricMods
                                    "forge" -> forgeMods
                                    else -> emptyList()
                                }
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
    private fun CompactTabRow(
        tabs: List<ManagerTab>,
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
                items(tabs) { tab ->
                    val index = tabs.indexOf(tab)
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
                    tab.isKernel -> MaterialTheme.colorScheme.tertiary
                    tab.isKernelPlugin -> MaterialTheme.colorScheme.secondary
                    tab.isModLoader -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary
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
        kernelModules: List<KernelModule>
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
                    KernelModuleCard(module = module)
                }
            }
        }
    }

    @Composable
    private fun KernelPluginsSection(
        plugins: List<KernelPlugin>
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
                items(plugins) { plugin ->
                    KernelPluginCard(plugin = plugin)
                }
            }
        }
    }

    @Composable
    private fun KernelPluginCard(plugin: KernelPlugin) {
        var expanded by remember { mutableStateOf(false) }
        var enabled by remember { mutableStateOf(plugin.enabled) }

        Card(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 图标区域
                    Surface(
                        shape = CircleShape,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (enabled) {
                                    MaterialTheme.colorScheme.onSecondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // 信息区域
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "v${plugin.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Text(
                                text = "${plugin.providesFunctions.size} 个功能",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // 操作区域
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )

                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 展开的详细信息
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 提供功能
                        Text(
                            text = "提供功能:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            plugin.providesFunctions.forEach { function ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )

                                    Text(
                                        text = function,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 支持加载器
                        Text(
                            text = "支持加载器: ${plugin.supportedLoaders.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun KernelModuleCard(module: KernelModule) {
        var expanded by remember { mutableStateOf(false) }
        var enabled by remember { mutableStateOf(module.enabled) }

        Card(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 图标区域
                    Surface(
                        shape = CircleShape,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (enabled) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // 信息区域
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (module.isEssential) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "核心",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "v${module.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // 开关区域
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            enabled = !module.isEssential
                        )

                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 展开的详细信息
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (module.isEssential) {
                            Text(
                                text = "⚠️ 这是核心模块，禁用可能导致系统不稳定",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Text(
                            text = "内核模块是系统级的优化组件，它们直接与游戏核心交互，提供性能优化、内存管理、网络增强等基础功能。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ModLoadersSection(
        loaders: List<ModLoader>,
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
                        "${enabledCount}/${loaders.size} 已启用",
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
                items(loaders) { loader ->
                    val enabled = loaderStates[loader.id] ?: loader.enabled

                    ModLoaderCard(
                        loader = loader,
                        enabled = enabled,
                        onToggle = { onLoaderToggle(loader.id, it) }
                    )
                }
            }
        }
    }

    @Composable
    private fun ModLoaderCard(
        loader: ModLoader,
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标区域
                Surface(
                    shape = CircleShape,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = loader.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 信息区域
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = loader.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = loader.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v${loader.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = "${loader.modCount} 个模组",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 开关区域
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    @Composable
    private fun ModListSection(
        loader: ModLoader,
        mods: List<ModInfo>
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
                items(mods) { mod ->
                    ExpandableModCard(mod = mod)
                }
            }
        }
    }

    @Composable
    private fun ExpandableModCard(mod: ModInfo) {
        var expanded by remember { mutableStateOf(false) }
        var enabled by remember { mutableStateOf(mod.enabled) }

        Card(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 图标区域
                    Surface(
                        shape = CircleShape,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (enabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // 信息区域
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = mod.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (enabled) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        "已启用",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                        }

                        Text(
                            text = mod.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "v${mod.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Text(
                                text = "作者: ${mod.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // 操作区域
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )

                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 展开的详细信息
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 统计数据
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatItem(
                                icon = Icons.Rounded.Download,
                                title = "下载量",
                                value = "${mod.downloadCount}"
                            )

                            StatItem(
                                icon = Icons.Rounded.Star,
                                title = "评分",
                                value = "${mod.rating}"
                            )

                            StatItem(
                                icon = Icons.Rounded.Build,
                                title = "ID",
                                value = mod.id
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DisabledLoaderSection(loader: ModLoader?) {
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

    @Composable
    private fun StatItem(
        icon: ImageVector,
        title: String,
        value: String
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        onSearch: (String) -> Unit,
        onActiveChange: (Boolean) -> Unit,
        active: Boolean,
        modifier: Modifier = Modifier
    ) {
        androidx.compose.material3.SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = active,
            onActiveChange = onActiveChange,
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
            modifier = modifier,
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dividerColor = Color.Transparent
            )
        ) {
            // 搜索建议（可选）
        }
    }

    override val title: String
        get() = "模组管理"
}