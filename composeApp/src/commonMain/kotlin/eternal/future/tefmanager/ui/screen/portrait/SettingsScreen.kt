package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.screen.shared.settings.AboutSettings
import eternal.future.tefmanager.ui.screen.shared.settings.AdvancedSettings
import eternal.future.tefmanager.ui.screen.shared.settings.AppearanceSettings
import eternal.future.tefmanager.ui.screen.shared.settings.GeneralSettings
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
                id = "about",
                title = Strings.settings.about.title,
                icon = Icons.Outlined.Info,
                iconFilled = Icons.Rounded.Info
            )
        )

        var selectedTab by remember { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 标签栏
                CompactTabRow(
                    categories = categories,
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 水平分页器
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (page) {
                            0 -> GeneralSettings()
                            1 -> AppearanceSettings()
                            2 -> AdvancedSettings()
                            3 -> AboutSettings()
                        }
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
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val index = categories.indexOf(category)
                    val isSelected = selectedTab == index

                    M3ETab(
                        category = category,
                        isSelected = isSelected,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    }

    @Composable
    private fun M3ETab(
        category: SettingCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        }

        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) category.iconFilled else category.icon,
                    contentDescription = category.title,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )

                Text(
                    text = category.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    override val title: String
        get() = Strings.settings.title
}