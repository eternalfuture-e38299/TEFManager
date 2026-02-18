package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*******************************************************************************
 * TEFManager - ResourcePackScreen
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

data class ResourcePack(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val enabled: Boolean = false
)

object ResourcePackScreen : Screen, MainScreen.TitledScreen {

    // 模拟数据
    private val languagePacks = listOf(
        ResourcePack("zh_cn", "简体中文", "官方中文语言包", "1.0.0", "language", true),
        ResourcePack("en_us", "English", "Default English", "1.0.0", "language", true),
        ResourcePack("ja_jp", "日本語", "Japanese localization", "1.0.0", "language"),
        ResourcePack("ko_kr", "한국어", "Korean localization", "1.0.0", "language"),
    )

    private val texturePacks = listOf(
        ResourcePack("default", "默认材质", "原始游戏材质", "1.0.0", "texture", true),
        ResourcePack("faithful", "Faithful 32x", "高清忠实材质", "1.0.0", "texture"),
        ResourcePack("soartex", "Soartex Fanver", "粉丝制作的Soartex", "1.0.0", "texture"),
    )

    private val fontPacks = listOf(
        ResourcePack("microsoft", "微软雅黑", "清晰的中文字体", "1.0.0", "font", true),
        ResourcePack("noto", "Noto Sans", "谷歌字体", "1.0.0", "font"),
        ResourcePack("minecraft", "Minecraft Font", "原版字体", "1.0.0", "font"),
    )

    private val musicPacks = listOf(
        ResourcePack("vanilla", "原版音乐", "默认游戏音乐", "1.0.0", "music", true),
        ResourcePack("c418", "C418全集", "所有C418曲目", "1.0.0", "music"),
        ResourcePack("custom", "自定义音乐", "玩家自制音乐", "1.0.0", "music"),
    )

    data class ResourceCategory(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val iconFilled: ImageVector,
        val packs: List<ResourcePack>
    )

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val categories = remember {
            listOf(
                ResourceCategory(
                    id = "language",
                    title = "语言包",
                    icon = Icons.Outlined.Translate,
                    iconFilled = Icons.Rounded.Translate,
                    packs = languagePacks
                ),
                ResourceCategory(
                    id = "texture",
                    title = "材质包",
                    icon = Icons.Outlined.Palette,
                    iconFilled = Icons.Rounded.Palette,
                    packs = texturePacks
                ),
                ResourceCategory(
                    id = "font",
                    title = "字体包",
                    icon = Icons.Outlined.TextFields,
                    iconFilled = Icons.Rounded.TextFields,
                    packs = fontPacks
                ),
                ResourceCategory(
                    id = "music",
                    title = "音乐包",
                    icon = Icons.Outlined.MusicNote,
                    iconFilled = Icons.Rounded.MusicNote,
                    packs = musicPacks
                )
            )
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        var searchQuery by remember { mutableStateOf("") }
        var searchActive by remember { mutableStateOf(false) }
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

        // 同步分页状态和标签选择
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        // 所有资源包
        val allPacks = categories.flatMap { it.packs }

        // 计算搜索结果
        val searchResults = remember(searchQuery, selectedTab, categories) {
            if (searchQuery.isEmpty()) {
                SearchResult(emptyList(), emptyList())
            } else {
                val query = searchQuery.lowercase()
                val currentCategory = categories[selectedTab]

                // 在当前分类中搜索
                val categoryResults = currentCategory.packs.filter { pack ->
                    pack.name.lowercase().contains(query) ||
                            pack.description.lowercase().contains(query) ||
                            pack.id.lowercase().contains(query)
                }

                // 在所有分类中搜索
                val allResults = allPacks.filter { pack ->
                    pack.name.lowercase().contains(query) ||
                            pack.description.lowercase().contains(query) ||
                            pack.id.lowercase().contains(query)
                }.distinctBy { it.id }

                SearchResult(categoryResults, allResults)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { query ->
                    searchQuery = query
                },
                onActiveChange = { searchActive = it },
                active = searchActive,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (searchQuery.isEmpty()) {
                // 正常标签页模式
                NormalTabView(
                    categories = categories,
                    selectedTab = selectedTab,
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            } else {
                // 搜索模式
                SearchResultsView(
                    categories = categories,
                    selectedTab = selectedTab,
                    searchResults = searchResults
                )
            }
        }
    }

    data class SearchResult(
        val categoryResults: List<ResourcePack>,
        val allResults: List<ResourcePack>
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NormalTabView(
        categories: List<ResourceCategory>,
        selectedTab: Int,
        pagerState: PagerState,
        coroutineScope: CoroutineScope,
        onTabSelected: (Int) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 滑动标签栏
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                edgePadding = 0.dp
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = selectedTab == index
                    val animatedColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "tabColor"
                    )

                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSelected) category.iconFilled else category.icon,
                                        contentDescription = category.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = animatedColor
                                    )
                                }
                            }

                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = animatedColor
                            )

                            if (isSelected) {
                                Surface(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {}
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 水平分页器
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val category = categories[page]

                ResourcePackList(
                    category = category,
                    packs = category.packs
                )
            }
        }
    }

    @Composable
    private fun SearchResultsView(
        categories: List<ResourceCategory>,
        selectedTab: Int,
        searchResults: SearchResult
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp
        ) {
            if (searchResults.allResults.isEmpty() && searchResults.categoryResults.isEmpty()) {
                // 无搜索结果
                NoSearchResults()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 当前分类搜索结果
                    if (searchResults.categoryResults.isNotEmpty()) {
                        item {
                            SearchResultSection(
                                title = "在当前分类中 (${categories[selectedTab].title})",
                                icon = categories[selectedTab].iconFilled,
                                packs = searchResults.categoryResults,
                                categories = categories
                            )
                        }
                    }

                    // 所有分类搜索结果
                    if (searchResults.allResults.isNotEmpty() &&
                        searchResults.allResults != searchResults.categoryResults) {
                        item {
                            SearchResultSection(
                                title = "在所有分类中",
                                icon = Icons.Rounded.Search,
                                packs = searchResults.allResults,
                                categories = categories
                            )
                        }
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
                text = "未找到相关资源包",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "尝试使用其他关键词搜索",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    @Composable
    private fun ResourcePackList(
        category: ResourceCategory,
        packs: List<ResourcePack>
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 分类标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = category.iconFilled,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${packs.size} 个项目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 添加按钮
                    FilledTonalButton(
                        onClick = { /* 添加资源包 */ },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "添加",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("添加")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 资源包列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(packs) { pack ->
                        ResourcePackCard(
                            pack = pack,
                            category = category,
                            onToggle = { /* 切换启用状态 */ },
                            onDelete = { /* 删除资源包 */ }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchResultSection(
        title: String,
        icon: ImageVector,
        packs: List<ResourcePack>,
        categories: List<ResourceCategory>
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${packs.size} 个结果",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 搜索结果列表
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                packs.forEach { pack ->
                    val category = categories.find { it.id == pack.category }
                        ?: categories.first()

                    ResourcePackCard(
                        pack = pack,
                        category = category,
                        onToggle = { /* 切换启用状态 */ },
                        onDelete = { /* 删除资源包 */ }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ResourcePackCard(
        pack: ResourcePack,
        category: ResourceCategory,
        onToggle: (ResourcePack) -> Unit,
        onDelete: (ResourcePack) -> Unit
    ) {
        Card(
            onClick = { /* 查看详情 */ },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (pack.enabled) {
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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图标区域
                Surface(
                    shape = CircleShape,
                    color = if (pack.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.iconFilled,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (pack.enabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 信息区域
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pack.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (pack.enabled) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(6.dp)
                            ) {}
                        }
                    }

                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "版本 ${pack.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = "ID: ${pack.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 操作按钮区域
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 开关按钮
                    Switch(
                        checked = pack.enabled,
                        onCheckedChange = { onToggle(pack) }
                    )

                    // 删除按钮
                    IconButton(
                        onClick = { onDelete(pack) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
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
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp
        ) {
            androidx.compose.material3.SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                active = active,
                onActiveChange = onActiveChange,
                placeholder = { Text("搜索资源包...") },
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
                modifier = Modifier.fillMaxWidth(),
                colors = SearchBarDefaults.colors(
                    containerColor = Color.Transparent
                )
            ) {
                // 搜索建议（可选）
            }
        }
    }

    @Composable
    private fun ScrollableTabRow(
        selectedTabIndex: Int,
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        divider: @Composable () -> Unit = {},
        edgePadding: Dp = 0.dp,
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                content()
            }
        }
    }

    override val title: String
        get() = "资源包管理"
}