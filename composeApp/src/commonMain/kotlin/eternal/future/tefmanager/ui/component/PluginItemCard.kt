package eternal.future.tefmanager.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eternal.future.tefmanager.model.PluginItem
import eternal.future.tefmanager.utils.toFileUrlString
import eternal.future.tefmanager.strings.StringsResource.Strings
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - PluginItemCard
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
 * Created: 2026/3/15
 *******************************************************************************/

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PluginItemCard(
    plugin: PluginItem,
    customIconPath: Path? = null,
    onDelete: () -> Unit = {}
) {
    val fileSystem: FileSystem = FileSystem.SYSTEM
    var hasCustomIcon by remember { mutableStateOf(false) }
    var iconLoadError by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // 检查并加载自定义图标
    LaunchedEffect(customIconPath) {
        if (customIconPath == null) {
            hasCustomIcon = false
            iconLoadError = null
            return@LaunchedEffect
        }

        try {
            hasCustomIcon = fileSystem.exists(customIconPath)
        } catch (_: Exception) {
            iconLoadError = Strings.error.iconLoadFailed(customIconPath)
            hasCustomIcon = false
        }
    }

    // MD3E设计
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 8.dp,
            focusedElevation = 6.dp,
            hoveredElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 主内容区域
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图标区域
                Surface(
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCustomIcon && customIconPath != null) {
                            if (fileSystem.exists(customIconPath)) {
                                KamelImage(
                                    resource = { asyncPainterResource(data = customIconPath.toFileUrlString()) },
                                    contentDescription = null,
                                    onFailure = { _ ->
                                        iconLoadError = Strings.error.iconLoadFailed(customIconPath)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }

                // 信息区域
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题和版本区域
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 名称
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        // 版本标签
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "v${plugin.version}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // 自定义图标标记
                        if (hasCustomIcon) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 1.dp,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = Strings.manager.custom,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // ID和作者信息
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ID标签
                        Text(
                            text = plugin.pkgId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            maxLines = 1
                        )

                        // 作者信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = plugin.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = Strings.delete,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 描述区域
            if (plugin.description.isNotBlank()) {
                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // 描述内容
                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    // 图标加载错误信息
                    iconLoadError?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp)
                        )
                    }
                }
            } else {
                // 无描述时显示图标加载错误
                iconLoadError?.let { error ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp)
                        )
                    }
                }
            }
        }
    }
}