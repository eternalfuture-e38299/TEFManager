package eternal.future.tefmanager.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eternal.future.tefmanager.model.ResourcesPackItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.toFileUrlString
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - ResourcesPackCard
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
 * Created: 2026/4/19
 *******************************************************************************/

@Composable
fun ResourcesPackCard(
    pack: ResourcesPackItem,
    index: Int,
    totalItems: Int,
    isEnabled: Boolean,
    onEnableChange: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onExport: (ResourcesPackItem) -> Unit,
    switchEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var iconLoadError by remember { mutableStateOf<String?>(null) }

    val showMoveUp = onMoveUp != null && isEnabled && index > 0
    val showMoveDown = onMoveDown != null && isEnabled && index < totalItems - 1
    val showMoveButtons = showMoveUp || showMoveDown

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 主行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 图标
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pack.iconPath.isNotBlank()) {
                            val fileSystem = FileSystem.SYSTEM
                            val customIconPath = pack.iconPath.toPath()

                            if (fileSystem.exists(customIconPath)) {
                                KamelImage(
                                    resource = { asyncPainterResource(data = customIconPath.toFileUrlString()) },
                                    contentDescription = null,
                                    onFailure = { _ ->
                                        iconLoadError = Strings.error.iconLoadFailed(customIconPath)
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Texture,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                // 信息区域
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )

                    Text(
                        text = pack.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )

                    if (pack.version.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "v${pack.version}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // 开关
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnableChange,
                    enabled = switchEnabled
                )

                // 展开按钮
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 展开内容
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)) +
                        slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pack.description.isNotBlank()) {
                        Text(
                            text = pack.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    iconLoadError?.let { error ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = null,
                            elevation = null,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = {
                                    Text(
                                        pack.type.getText(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = when (pack.type) {
                                        ResourcesPackItem.Type.Terraria ->
                                            MaterialTheme.colorScheme.primaryContainer
                                        ResourcesPackItem.Type.TLPro ->
                                            MaterialTheme.colorScheme.secondaryContainer
                                        ResourcesPackItem.Type.TEFManager ->
                                            MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                    labelColor = when (pack.type) {
                                        ResourcesPackItem.Type.Terraria ->
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        ResourcesPackItem.Type.TLPro ->
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        ResourcesPackItem.Type.TEFManager ->
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                    }
                                ),
                                border = null
                            )

                            if (showMoveButtons) {
                                Text(
                                    text = "${index + 1}/$totalItems",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 操作按钮行
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 导出按钮
                            IconButton(
                                onClick = { onExport(pack) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // 上移按钮
                            if (showMoveUp) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 下移按钮
                            if (showMoveDown) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 删除按钮
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (isEnabled && showMoveButtons) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.resource.prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}