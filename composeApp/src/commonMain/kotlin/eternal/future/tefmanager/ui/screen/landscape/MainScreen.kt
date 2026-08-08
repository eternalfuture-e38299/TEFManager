package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Widgets
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
import androidx.compose.runtime.remember
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
import cafe.adriel.voyager.transitions.FadeTransition
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.RefreshIconButton
import org.jetbrains.compose.resources.painterResource
import tefmanager.composeapp.generated.resources.Res
import tefmanager.composeapp.generated.resources.tefmanager_logo

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
 * Created: 2026/2/2
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
                                    Surface(
                                        modifier = Modifier.size(36.dp)
                                            .padding(6.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(Res.drawable.tefmanager_logo),
                                                contentDescription = "Logo",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
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
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 现代化的侧边导航栏
                    ModernNavigationRail(navigator = navigator)

                    // 主内容区域 - 使用淡入淡出动画，适合侧边栏导航
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 2.dp
                    ) {
                        // 使用 FadeTransition 替代 SlideTransition
                        // 淡入淡出动画在侧边栏导航中更加自然，不会产生方向冲突
                        FadeTransition(
                            navigator = navigator,
                            animationSpec = tween(durationMillis = 200)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ModernNavigationRail(navigator: Navigator) {
        val navigationItems = mutableListOf(
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
                    label = Strings.resource.title
            ),
            NavigationItem(
                screen = ManagerScreen,
                selectedIcon = Icons.Rounded.Widgets,
                unselectedIcon = Icons.Outlined.Widgets,
                label = Strings.manager.title
            ),
            NavigationItem(
                screen = SettingsScreen,
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                label = Strings.settings.title
            )
        )


        val currentScreen = navigator.lastItem

        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxHeight()
                .width(88.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                // 导航项目
                navigationItems.forEach { item ->
                    ModernNavigationItem(
                        item = item,
                        isSelected = currentScreen::class == item.screen::class,
                        onClick = {
                            // 只有当不是当前页面时才进行导航
                            if (currentScreen::class != item.screen::class) {
                                navigator.push(item.screen)
                            }
                        }
                    )

                    if (item.screen == HomeScreen) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun ModernNavigationItem(
        item: NavigationItem,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val itemWidth by animateDpAsState(
            targetValue = if (isSelected) 64.dp else 56.dp,
            animationSpec = tween(durationMillis = 300),
            label = "itemWidthAnimation"
        )

        val itemHeight by animateDpAsState(
            targetValue = if (isSelected) 64.dp else 56.dp,
            animationSpec = tween(durationMillis = 300),
            label = "itemHeightAnimation"
        )

        val iconSize by animateDpAsState(
            targetValue = if (isSelected) 24.dp else 22.dp,
            animationSpec = tween(durationMillis = 300),
            label = "iconSizeAnimation"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 活动指示器 - 优化为圆点指示器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(bottom = 4.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemHeight),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                } else {
                    Color.Transparent
                },
                onClick = onClick,
                tonalElevation = if (isSelected) 2.dp else 0.dp,
                shadowElevation = if (isSelected) 1.dp else 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 图标容器
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(if (isSelected) 2.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(iconSize - 4.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
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
                    }
                }
            }

            // 底部圆点指示器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(top = 4.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}