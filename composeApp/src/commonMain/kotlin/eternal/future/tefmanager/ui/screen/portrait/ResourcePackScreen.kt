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
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import eternal.future.tefmanager.ui.screen.landscape.ResourcePack
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
        ResourcePack("en_us", "English", "默认英语语言包", "1.0.0", "language", true),
        ResourcePack("ja_jp", "日本語", "日语语言包", "1.0.0", "language"),
        ResourcePack("ko_kr", "한국어", "韩语语言包", "1.0.0", "language"),
        ResourcePack("fr_fr", "Français", "法语语言包", "1.0.0", "language"),
        ResourcePack("es_es", "Español", "西班牙语语言包", "1.0.0", "language"),
        ResourcePack("ru_ru", "Русский", "俄语语言包", "1.0.0", "language"),
    )

    private val texturePacks = listOf(
        ResourcePack("default", "默认材质", "原始游戏材质", "1.0.0", "texture", true),
        ResourcePack("faithful", "Faithful 32x", "高清忠实材质", "1.0.0", "texture"),
        ResourcePack("soartex", "Soartex Fanver", "粉丝制作的Soartex材质", "1.0.0", "texture"),
        ResourcePack("purebdcraft", "PureBDcraft", "经典的BDcraft材质", "1.0.0", "texture"),
        ResourcePack("sphax", "Sphax PureBD", "BDcraft的高清版本", "1.0.0", "texture"),
        ResourcePack("chromahills", "Chroma Hills", "中世纪风格材质", "1.0.0", "texture"),
    )

    private val fontPacks = listOf(
        ResourcePack("microsoft", "微软雅黑", "清晰的中文字体", "1.0.0", "font", true),
        ResourcePack("noto", "Noto Sans", "谷歌开源字体", "1.0.0", "font"),
        ResourcePack("minecraft", "Minecraft Font", "原版游戏字体", "1.0.0", "font"),
        ResourcePack("zh_cn_old", "简体中文(旧)", "旧版本中文支持", "1.0.0", "font"),
        ResourcePack("unicode", "Unicode Full", "完整Unicode支持", "1.0.0", "font"),
    )

    private val musicPacks = listOf(
        ResourcePack("vanilla", "原版音乐", "默认游戏音乐", "1.0.0", "music", true),
        ResourcePack("c418", "C418全集", "所有C418创作的音乐", "1.0.0", "music"),
        ResourcePack("custom", "自定义音乐", "玩家自制音乐包", "1.0.0", "music"),
        ResourcePack("relaxing", "放松音乐", "舒缓的背景音乐", "1.0.0", "music"),
        ResourcePack("epic", "史诗音乐", "史诗感的背景音乐", "1.0.0", "music"),
    )

    data class ResourceCategory(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val iconFilled: ImageVector,
        val fabText: String,
        val fabAction: () -> Unit,
        val packs: List<ResourcePack>
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        var selectedTab by remember { mutableIntStateOf(0) }
        var searchQuery by remember { mutableStateOf("") }
        var searchActive by remember { mutableStateOf(false) }
        val pagerState = rememberPagerState(pageCount = { 4 })
        val coroutineScope = rememberCoroutineScope()

        val categories = remember(selectedTab) {
            listOf(
                ResourceCategory(
                    id = "language",
                    title = "语言包",
                    icon = Icons.Outlined.Translate,
                    iconFilled = Icons.Rounded.Translate,
                    fabText = "添加语言包",
                    fabAction = {
                        // 添加语言包的逻辑
                        println("添加语言包")
                    },
                    packs = languagePacks
                ),
                ResourceCategory(
                    id = "texture",
                    title = "材质包",
                    icon = Icons.Outlined.Palette,
                    iconFilled = Icons.Rounded.Palette,
                    fabText = "添加材质包",
                    fabAction = {
                        // 添加材质包的逻辑
                        println("添加材质包")
                    },
                    packs = texturePacks
                ),
                ResourceCategory(
                    id = "font",
                    title = "字体包",
                    icon = Icons.Outlined.TextFields,
                    iconFilled = Icons.Rounded.TextFields,
                    fabText = "添加字体包",
                    fabAction = {
                        // 添加字体包的逻辑
                        println("添加字体包")
                    },
                    packs = fontPacks
                ),
                ResourceCategory(
                    id = "music",
                    title = "音乐包",
                    icon = Icons.Outlined.MusicNote,
                    iconFilled = Icons.Rounded.MusicNote,
                    fabText = "添加音乐包",
                    fabAction = {
                        // 添加音乐包的逻辑
                        println("添加音乐包")
                    },
                    packs = musicPacks
                )
            )
        }

        val currentCategory = categories[selectedTab]

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
                    onSearch = { query ->
                        searchQuery = query
                    },
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
                        searchResults = searchResults
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
            // 紧凑型标签栏
            CompactTabRow(
                categories = categories,
                selectedTab = selectedTab,
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
                val category = categories[page]

                ResourcePackList(
                    category = category,
                    packs = category.packs
                )
            }
        }
    }

    @Composable
    private fun CompactTabRow(
        categories: List<ResourceCategory>,
        selectedTab: Int,
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
                items(categories) { category ->
                    val index = categories.indexOf(category)
                    val isSelected = selectedTab == index

                    CompactTab(
                        category = category,
                        isSelected = isSelected,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactTab(
        category: ResourceCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val animatedColor by animateColorAsState(
            targetValue = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            animationSpec = tween(durationMillis = 300),
            label = "tabColor"
        )

        val backgroundColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

        Surface(
            onClick = onClick,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) category.iconFilled else category.icon,
                    contentDescription = category.title,
                    modifier = Modifier.size(18.dp),
                    tint = animatedColor
                )

                Text(
                    text = category.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = animatedColor,
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun SearchResultsView(
        categories: List<ResourceCategory>,
        searchResults: SearchResult
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (searchResults.allResults.isEmpty() && searchResults.categoryResults.isEmpty()) {
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
                                text = "共 ${searchResults.allResults.size} 个结果",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 搜索结果列表
                    items(searchResults.allResults) { pack ->
                        val category = categories.find { it.id == pack.category }
                            ?: categories.first()

                        CompactResourcePackCard(
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = category.iconFilled,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${packs.count { it.enabled }} 个已启用 • 共 ${packs.size} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                },
                contentColor = if (pack.enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部行：图标和标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (pack.enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = category.iconFilled,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (pack.enabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = pack.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (pack.enabled) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }

                        Text(
                            text = "版本 ${pack.version} • ${category.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pack.enabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Switch(
                        checked = pack.enabled,
                        onCheckedChange = { onToggle(pack) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 描述
                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pack.enabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 底部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${pack.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pack.enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 下载按钮
                        IconButton(
                            onClick = { /* 下载资源包 */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "下载",
                                modifier = Modifier.size(20.dp),
                                tint = if (pack.enabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        // 删除按钮
                        IconButton(
                            onClick = { onDelete(pack) },
                            modifier = Modifier.size(36.dp)
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
    }

    @Composable
    private fun CompactResourcePackCard(
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
                },
                contentColor = if (pack.enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标
                Surface(
                    shape = CircleShape,
                    color = if (pack.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.iconFilled,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
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
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pack.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pack.enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${category.title} • 版本 ${pack.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pack.enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }

                // 操作区域
                Switch(
                    checked = pack.enabled,
                    onCheckedChange = { onToggle(pack) },
                    modifier = Modifier.size(36.dp)
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
        get() = "资源包管理"
}