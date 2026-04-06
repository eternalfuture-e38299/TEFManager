package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.RefreshIconButton

/*******************************************************************************
 * TEFManager - MainScreen
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

@OptIn(ExperimentalMaterial3Api::class)
object MainScreen : Screen {
    interface TitledScreen {
        val title: String
        val refreshAction: (() -> Unit)?
            get() = null
    }

    data class NavigationItem(
        val screen: Screen,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
        val label: String,
        val isPrimary: Boolean = false
    )

    @OptIn(InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        Navigator(
            screen = HomeScreen,
            key = "main_navigator"
        ) { navigator ->
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.surfaceDim,
                topBar = {
                    val currentScreen = navigator.lastItem
                    var refreshAction: (() -> Unit)? by mutableStateOf(((currentScreen as TitledScreen).refreshAction))
                    val title = (currentScreen as TitledScreen).title

                    Surface(
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Terminal,
                                            contentDescription = "App Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                                navigationIconContentColor = Color.Unspecified,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = Color.Unspecified
                            ),
                            actions = {
                                refreshAction?.let { action ->
                                    RefreshIconButton(
                                        refreshAction = action
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                },
                bottomBar = {
                    // 竖屏版本使用底部导航栏
                    ModernBottomNavigation(navigator = navigator)
                }
            ) { paddingValues ->
                // 主内容区域 - 竖屏版本全屏显示
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    // 使用 SlideTransition 添加页面切换动画
                    SlideTransition(navigator = navigator)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ModernBottomNavigation(navigator: Navigator) {
        val navigationItems = listOf(
                NavigationItem(
                    screen = HomeScreen,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    label = Strings.home.title,
                    isPrimary = true
                ),
                NavigationItem(
                    screen = ResourcePackScreen,
                    selectedIcon = Icons.Rounded.Palette,
                    unselectedIcon = Icons.Outlined.Palette,
                    label = "资源包"
                ),
                NavigationItem(
                    screen = ManagerScreen,
                    selectedIcon = Icons.Rounded.Widgets,
                    unselectedIcon = Icons.Outlined.Widgets,
                    label = "管理"
                ),
                NavigationItem(
                    screen = SettingsScreen,
                    selectedIcon = Icons.Rounded.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    label = Strings.settings.title
                )
            )


        val currentScreen = navigator.lastItem

        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp
        ) {
            navigationItems.forEach { item ->
                ModernBottomNavigationItem(
                    item = item,
                    isSelected = currentScreen::class == item.screen::class,
                    onClick = { navigator.push(item.screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun ModernBottomNavigationItem(
        item: NavigationItem,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val indicatorHeight by animateDpAsState(
            targetValue = if (isSelected) 3.dp else 0.dp,
            animationSpec = tween(durationMillis = 200),
            label = "indicatorAnimation"
        )

        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 图标容器
            Surface(
                modifier = Modifier.size(48.dp),
                shape = if (isSelected) RoundedCornerShape(12.dp) else CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                onClick = onClick,
                tonalElevation = if (isSelected) 1.dp else 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // 标签文字
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // 底部指示器
            Surface(
                modifier = Modifier
                    .width(24.dp)
                    .height(indicatorHeight),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            ) {}
        }
    }
}