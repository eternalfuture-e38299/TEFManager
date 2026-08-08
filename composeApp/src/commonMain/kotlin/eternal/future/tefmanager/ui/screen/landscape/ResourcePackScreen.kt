package eternal.future.tefmanager.ui.screen.landscape

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.ui.dialogs.ResourcePackInstallDialog
import eternal.future.tefmanager.ui.screen.shared.resourcepack.FontPackScreen
import eternal.future.tefmanager.ui.screen.shared.resourcepack.LanguagePackScreen
import eternal.future.tefmanager.ui.screen.shared.resourcepack.LanguagePatchPackScreen
import eternal.future.tefmanager.ui.screen.shared.resourcepack.TexturePackScreen
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import eternal.future.tefmanager.strings.StringsResource.Strings

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
        val categories = mutableListOf(
            ResourceCategory(
                id = "language",
                title = Strings.resource.language.title,
                icon = Icons.Outlined.Translate,
                iconFilled = Icons.Rounded.Translate,
                screen = LanguagePackScreen
            ),
            ResourceCategory(
                id = "language_patch",
                title = Strings.resource.languagePatch,
                icon = Icons.Outlined.Edit,
                iconFilled = Icons.Rounded.Edit,
                screen = LanguagePatchPackScreen
            )
        )

        if (Platform.isAndroid) {
            categories.addAll(
                listOf(
                    ResourceCategory(
                        id = "texture",
                        title = Strings.resource.texture,
                        icon = Icons.Outlined.Palette,
                        iconFilled = Icons.Rounded.Palette,
                        screen = TexturePackScreen
                    ),
                    ResourceCategory(
                        id = "font",
                        title = Strings.resource.font.title,
                        icon = Icons.Outlined.TextFields,
                        iconFilled = Icons.Rounded.TextFields,
                        screen = FontPackScreen
                    )/*,
                ResourceCategory(
                    id = "music",
                    title = "音乐包",
                    icon = Icons.Outlined.MusicNote,
                    iconFilled = Icons.Rounded.MusicNote,
                    screen = AudioPackScreen
                ) */
                )
            )
        }



        var selectedTab by remember { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { categories.size })
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = Strings.resource.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // M3E 标签栏
                M3ETabRow(
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
                    val category = categories[page]
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            CategoryHeader(
                                category = category
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            categories[page].screen.Content()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun M3ETabRow(
        categories: List<ResourceCategory>,
        selectedTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, category ->
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
        category: ResourceCategory,
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

    @Composable
    private fun CategoryHeader(
        category: ResourceCategory
    ) {
        var showInstallDialog by remember { mutableStateOf(false) }
        val installFiles = remember { mutableListOf<Path>() }

        val filePickerLauncher = rememberFilePickerLauncher(
            mode = FileKitMode.Multiple(maxItems = 10)
        ) { files ->
            installFiles.clear()
            files?.forEach { file ->
                val tmp = kotlinx.io.files.Path((Platform.getDirectory("tmp") / "install_tmp" / file.nameWithoutExtension).toString())
                tmp.parent?.let { SystemFileSystem.createDirectories(it) }
                SystemFileSystem.sink(tmp).buffered().use { sink ->
                    sink.write(file.source(), file.size())
                    sink.flush()
                    installFiles.add(tmp.toString().toPath())
                }
                showInstallDialog = true
            }
        }

        if (showInstallDialog) {
            ResourcePackInstallDialog(
                filePaths = installFiles,
                onDismiss = {
                    FileSystem.SYSTEM.deleteRecursively(Platform.getDirectory("tmp") / "install_tmp")
                    installFiles.clear()
                    showInstallDialog = false
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.iconFilled,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            FilledTonalButton(
                onClick = { filePickerLauncher.launch() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    Strings.resource.add(""),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    override val title: String
        get() = Strings.resource.title
}