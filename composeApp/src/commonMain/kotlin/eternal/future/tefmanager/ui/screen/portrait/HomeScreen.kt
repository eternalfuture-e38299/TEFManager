package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.GameManager
import eternal.future.tefmanager.ui.dialogs.AddGameDialog
import eternal.future.tefmanager.ui.model.GameItem
import eternal.future.tefmanager.utils.GameLauncher


/*******************************************************************************
 * TEFManager - HomeScreen
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

object HomeScreen : Screen, MainScreen.TitledScreen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var selectedItem by remember { mutableStateOf(GameManager.games.firstOrNull()) }
        val showControlPanel = remember { mutableStateOf(false) }
        var showAddGameDialog by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                if (selectedItem != null) {
                    ExtendedFloatingActionButton(
                        onClick = { showControlPanel.value = true },
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "控制面板"
                            )
                        },
                        text = {
                            Text(
                                text = "控制面板",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->

            if (showAddGameDialog) {
                AddGameDialog.Show {
                    showAddGameDialog = false
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 主要内容区域
                if (showControlPanel.value) {
                    ControlPanelSheet(
                        selectedItem = selectedItem,
                        onStartGame = {
                            showControlPanel.value = false
                            GameLauncher.launch(selectedItem)
                        },
                        onDismiss = { showControlPanel.value = false }
                    )
                }

                GameListSection(
                    selectedItem = selectedItem,
                    onItemClick = { item ->
                        selectedItem = if (selectedItem?.hash == item.hash) null else item
                    },
                    onAddGame = {
                        showAddGameDialog = true
                        println("添加游戏")
                    }
                )
            }
        }
    }

    @Composable
    private fun GameListSection(
        selectedItem: GameItem?,
        onItemClick: (GameItem) -> Unit,
        onAddGame: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已安装的游戏",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 添加游戏按钮
                FilledTonalIconButton(
                    onClick = onAddGame,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "添加游戏"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 游戏列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(GameManager.games) { item ->
                    GameListItemCompact(
                        item = item,
                        isSelected = selectedItem?.hash == item.hash,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GameListItemCompact(
        item: GameItem,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标区域
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Gamepad,
                        contentDescription = "Game",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(Modifier.width(16.dp))

                // 版本信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.version,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "版本号: ${item.versionCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.hash,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 选择标记
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ControlPanelSheet(
        selectedItem: GameItem?,
        onStartGame: () -> Unit,
        onDismiss: () -> Unit
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "游戏控制",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭")
                    }
                }

                // 游戏信息区域
                GameInfoSectionCompact(selectedItem)

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // 控制按钮区域
                ControlButtonsSectionCompact(
                    selectedItem = selectedItem,
                    onStartGame = onStartGame
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun GameInfoSectionCompact(selectedItem: GameItem?) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "游戏信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (selectedItem != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LabeledInfoCompact(
                            icon = Icons.Rounded.Tag,
                            label = "游戏版本",
                            value = selectedItem.version,
                            iconColor = MaterialTheme.colorScheme.primary
                        )

                        LabeledInfoCompact(
                            icon = Icons.Rounded.Code,
                            label = "版本代码",
                            value = selectedItem.versionCode.toString(),
                            iconColor = MaterialTheme.colorScheme.tertiary
                        )

                        LabeledInfoCompact(
                            icon = Icons.Rounded.Fingerprint,
                            label = "哈希校验",
                            value = selectedItem.hash,
                            iconColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未选择游戏",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    @Composable
    private fun LabeledInfoCompact(
        icon: ImageVector,
        label: String,
        value: String,
        iconColor: Color
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    private fun ControlButtonsSectionCompact(
        selectedItem: GameItem?,
        onStartGame: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartGame,
                enabled = selectedItem != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = if (selectedItem != null) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "开始游戏",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "开始游戏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    override val title: String
        get() = Strings.home.title

    override val refreshAction: (() -> Unit) = {
        GameManager.refreshGames()
    }
}