package eternal.future.tefmanager.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.ui.model.ModuleItem
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path
import kotlinx.coroutines.launch
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - ModuleItemCard
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
fun ModuleItemCard(
    module: ModuleItem,
    enabled: Boolean = true,
    isEssential: Boolean = false,
    customIconPath: Path? = null,
    onEnableChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    onDetails: () -> Unit = {}
) {
    val fileSystem: FileSystem = FileSystem.SYSTEM

    var expanded by remember { mutableStateOf(false) }
    var internalEnabled by remember { mutableStateOf(enabled) }
    var hasCustomIcon by remember { mutableStateOf(false) }
    var iconLoadError by remember { mutableStateOf<String?>(null) }

    // 监听外部enabled状态变化
    LaunchedEffect(enabled) {
        internalEnabled = enabled
    }

    // 检查并加载自定义图标
    LaunchedEffect(customIconPath) {
        if (customIconPath == null) {
            hasCustomIcon = false
            iconLoadError = null
            return@LaunchedEffect
        }

        try {
            hasCustomIcon = fileSystem.exists(customIconPath)
        } catch (e: Exception) {
            iconLoadError = "文件访问失败: ${e.message}"
            hasCustomIcon = false
        }
    }

    // MD3E增强的卡片设计
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (internalEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
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
                // 图标区域 - MD3E增强
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (internalEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
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
                                    contentDescription = "自定义图标",
                                    onLoading = { progress ->
                                        // 显示加载指示器
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(20.dp),
                                            color = ProgressIndicatorDefaults.circularColor,
                                            strokeWidth = 2.dp,
                                            trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                                            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                        )
                                    },
                                    onFailure = { exception ->
                                        // 加载失败时显示错误图标
                                        iconLoadError = "图标加载失败: ${exception.message}"
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    "加载失败",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            ),
                                            border = AssistChipDefaults.assistChipBorder(enabled,
                                                borderColor = MaterialTheme.colorScheme.error
                                            ),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Error,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    animationSpec = tween(durationMillis = 300)
                                )
                            } else {
                                // 文件不存在
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = "图标不存在",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // 显示默认图标
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "模块图标",
                                modifier = Modifier.size(28.dp),
                                tint = if (internalEnabled) {
                                    MaterialTheme.colorScheme.onPrimary
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isEssential) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "核心模块",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                border = AssistChipDefaults.assistChipBorder(enabled,
                                    borderColor = MaterialTheme.colorScheme.error
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // 自定义图标标记
                        if (hasCustomIcon) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "自定义",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                border = AssistChipDefaults.assistChipBorder(enabled,
                                    borderColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }
                    }

                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 版本标签
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = {
                                Text(
                                    "v${module.version}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled, enabled,
                                selectedBorderColor = MaterialTheme.colorScheme.secondary
                            )
                        )

                        // ID标签
                        Text(
                            text = "ID: ${module.pkgId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // 作者信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = "作者",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = module.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // 操作区域
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 启用开关 - MD3E增强
                    Switch(
                        checked = internalEnabled,
                        onCheckedChange = { newValue ->
                            internalEnabled = newValue
                            onEnableChange(newValue)
                        },
                        enabled = !isEssential,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // 展开/收起图标按钮
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        AnimatedContent(
                            targetState = expanded,
                            transitionSpec = {
                                (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                            },
                            label = "Expand Icon"
                        ) { isExpanded ->
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isExpanded) "收起" else "展开",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 展开的详细信息区域
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    // 分隔线
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 平台兼容性信息
                    if (module.support.getSupportedPlatforms().isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "支持平台",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 平台标签
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                module.support.getSupportedPlatforms().forEach { platform ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                platform,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 图标加载错误信息
                    iconLoadError?.let { error ->
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled,
                                borderColor = MaterialTheme.colorScheme.error
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 核心模块警告
                    if (isEssential && !internalEnabled) {
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "这是核心模块，禁用可能导致系统不稳定",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled,
                                borderColor = MaterialTheme.colorScheme.error
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 描述信息
                    Text(
                        text = "内核模块是系统级的优化组件，它们直接与游戏核心交互，提供性能优化、内存管理、网络增强等基础功能。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 详细信息按钮
                        OutlinedButton(
                            onClick = onDetails,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "详细信息",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("详细信息")
                        }

                        // 删除按钮
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

fun Path.toFileUrlString(): String {
    val pathStr = this.toString()

    // 处理Windows路径：将反斜杠转换为正斜杠，并确保以/开头
    val normalizedPath = if (pathStr.contains('\\')) {
        // Windows路径，转换为URL格式
        pathStr.replace('\\', '/')
    } else {
        pathStr
    }

    // 确保路径以/开头
    val finalPath = if (normalizedPath.startsWith("/")) {
        normalizedPath
    } else {
        "/$normalizedPath"
    }

    return "file://$finalPath"
}