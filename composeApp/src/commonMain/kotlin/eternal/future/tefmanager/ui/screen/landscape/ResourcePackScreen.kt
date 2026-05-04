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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.rememberTopAppBarState
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
import eternal.future.tefmanager.ui.screen.shared.*
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
    data class ResourceCategory(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val iconFilled: ImageVector,
        val screen: Screen
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
                    screen = LanguagePackScreen
                ),
                ResourceCategory(
                    id = "texture",
                    title = "材质包",
                    icon = Icons.Outlined.Palette,
                    iconFilled = Icons.Rounded.Palette,
                    screen = TexturePackScreen
                ),
                ResourceCategory(
                    id = "font",
                    title = "字体包",
                    icon = Icons.Outlined.TextFields,
                    iconFilled = Icons.Rounded.TextFields,
                    screen = FontPackScreen
                ),
                ResourceCategory(
                    id = "music",
                    title = "音乐包",
                    icon = Icons.Outlined.MusicNote,
                    iconFilled = Icons.Rounded.MusicNote,
                    screen = MusicPackScreen
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

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 搜索栏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
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
                            onTabSelected = { index ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    } else {
                        // 搜索模式
                        SearchResultsView()
                    }
                }
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

            // 水平分页器
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val category = categories[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // 分类标题和添加按钮
                    CategoryHeader(
                        category = category,
                        selectedTab = page
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 资源包列表
                    categories[page].screen.Content()
                }
            }
        }
    }

    @Composable
    private fun CategoryHeader(
        category: ResourceCategory,
        selectedTab: Int
    ) {
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

            // 添加按钮
            FilledTonalButton(
                onClick = {
                    // 根据当前分类执行不同的添加逻辑
                    when (selectedTab) {
                        0 -> { /* 添加语言包 */ }
                        1 -> { /* 添加材质包 */ }
                        2 -> { /* 添加字体包 */ }
                        3 -> { /* 添加音乐包 */ }
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("添加${category.title}")
            }
        }
    }

    @Composable
    private fun SearchResultsView() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 2.dp
        ) {
            NoSearchResults()
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
            SearchBar(
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