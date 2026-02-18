package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.model.SettingCategory
import eternal.future.tefmanager.ui.screen.shared.SettingsContent.AboutSettings
import eternal.future.tefmanager.ui.screen.shared.SettingsContent.AdvancedSettings
import eternal.future.tefmanager.ui.screen.shared.SettingsContent.AppearanceSettings
import eternal.future.tefmanager.ui.screen.shared.SettingsContent.GameSettings
import eternal.future.tefmanager.ui.screen.shared.SettingsContent.GeneralSettings
import kotlinx.coroutines.launch

/*******************************************************************************
 * TEFManager - SettingsScreen
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

object SettingsScreen : Screen, MainScreen.TitledScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val settingCategories = listOf(
            SettingCategory(
                id = "general",
                title = Strings.settings.general.title,
                icon = Icons.Outlined.Settings,
                iconFilled = Icons.Rounded.Settings
            ),
            SettingCategory(
                id = "appearance",
                title = Strings.settings.appearance.title,
                icon = Icons.Outlined.Palette,
                iconFilled = Icons.Rounded.Palette
            ),
            SettingCategory(
                id = "advanced",
                title = Strings.settings.advanced.title,
                icon = Icons.Outlined.Code,
                iconFilled = Icons.Rounded.Code
            ),
            SettingCategory(
                id = "game",
                title = Strings.settings.game.title,
                icon = Icons.Outlined.SportsEsports,
                iconFilled = Icons.Rounded.SportsEsports
            ),
            SettingCategory(
                id = "about",
                title = Strings.settings.about.title,
                icon = Icons.Outlined.Info,
                iconFilled = Icons.Rounded.Info
            )
        )

        var selectedTab by remember { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { settingCategories.size })
        val coroutineScope = rememberCoroutineScope()

        // 同步分页状态和标签选择
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 标签栏
            ScrollableTabRow(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                settingCategories.forEachIndexed { index, category ->
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
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
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
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 2.dp
                ) {
                    when (page) {
                        0 -> GeneralSettings()
                        1 -> AppearanceSettings()
                        2 -> AdvancedSettings()
                        3 -> GameSettings()
                        4 -> AboutSettings()
                    }
                }
            }
        }
    }

    @Composable
    private fun ScrollableTabRow(
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
        get() = Strings.settings.title
}