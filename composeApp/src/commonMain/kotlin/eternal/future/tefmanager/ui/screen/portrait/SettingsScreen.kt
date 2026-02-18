package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.strings.StringsResource.Strings
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
 * Created: 2026/2/4
 *******************************************************************************/

object SettingsScreen : Screen, MainScreen.TitledScreen {

    data class SettingCategory(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val iconFilled: ImageVector
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val categories = listOf(
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
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

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
                    .padding(top = 16.dp)
            ) {
                // 紧凑型标签栏
                CompactTabRow(
                    categories = categories,
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 水平分页器
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
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
    private fun CompactTabRow(
        categories: List<SettingCategory>,
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
        category: SettingCategory,
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

    override val title: String
        get() = Strings.settings.title
}