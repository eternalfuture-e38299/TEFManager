package eternal.future.tefmanager.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eternal.future.tefmanager.ui.model.ArchitectureSupport
import eternal.future.tefmanager.ui.model.Dependence
import eternal.future.tefmanager.ui.model.ModuleItem
import eternal.future.tefmanager.ui.model.PlatformSupport
import eternal.future.tefmanager.utils.openUrl
import eternal.future.tefmanager.utils.toFileUrlString
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path
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

@Composable
@Preview
private fun Preview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 测试正常模块
            ModuleItemCard(
                module = ModuleItem(
                    pkgId = "com.test.module1",
                    name = "性能优化模块",
                    author = "eternalfuture-e38299",
                    brieflyDescribe = "优化渲染",
                    description = "优化游戏渲染性能，提升帧率并减少卡顿，支持多种分辨率适配",
                    version = "2.1.0",
                    versionCode = 21,
                    dependence = listOf(
                        Dependence("com.test.base", 1, 0),
                        Dependence("com.test.graphics", 2, 0)
                    ),
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true, arm = true),
                        windows = ArchitectureSupport(x64 = true, x86 = true)
                    )
                ),
                enabled = true,
                isEssential = false,
                onEnableChange = {},
                onDelete = {},
                onConfigure = { println("配置模块") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 测试核心模块
            ModuleItemCard(
                module = ModuleItem(
                    pkgId = "com.system.core",
                    name = "系统核心模块",
                    author = "System Team",
                    description = "系统级核心组件，负责内存管理和进程调度，禁用可能导致系统不稳定",
                    version = "3.0.0",
                    versionCode = 30,
                    dependence = listOf(),
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true, arm = true),
                        windows = ArchitectureSupport(x64 = true, x86 = true),
                        linux = ArchitectureSupport(x64 = true)
                    )
                ),
                enabled = true,
                isEssential = true,
                onEnableChange = {},
                onDelete = {},
                onConfigure = null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 测试长作者名模块
            ModuleItemCard(
                module = ModuleItem(
                    pkgId = "com.network.enhancement",
                    name = "网络增强模块",
                    author = "Network Development Group with a very long author name",
                    description = "网络传输优化，降低延迟，提升连接稳定性",
                    version = "1.5.3",
                    versionCode = 15,
                    dependence = listOf(
                        Dependence("com.network.base", 1, 0),
                        Dependence("com.security.crypto", 2, 0)
                    ),
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true, arm = true),
                        linux = ArchitectureSupport(x64 = true)
                    )
                ),
                enabled = false,
                isEssential = false,
                onEnableChange = {},
                onDelete = {},
                onConfigure = { println("配置网络模块") }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModuleItemCard(
    module: ModuleItem,
    enabled: Boolean = true,
    isEssential: Boolean = false,
    customIconPath: Path? = null,
    onEnableChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    onConfigure: ((ModuleItem) -> Unit)? = null
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
            iconLoadError = "图标加载失败"
            hasCustomIcon = false
        }
    }

    // MD3E增强的卡片设计
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (internalEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface
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
                    color = if (internalEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
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
                                        iconLoadError = "图标加载失败"
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = "图标不存在",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 名称和ID区域
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = module.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            // ID和版本信息
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // ID标签
                                Text(
                                    text = module.pkgId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                // 自定义图标标记
                                if (hasCustomIcon) {
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                "自定义",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // 操作区域
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 启用开关
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
                                    label = "Expand Icon Animation"
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

                    // 简述
                    if (module.brieflyDescribe.isNotEmpty()) {
                        Text(
                            text = module.brieflyDescribe,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            lineHeight = 20.sp
                        )
                    }

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
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        if (isEssential) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "核心",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )
                        }

                        // 版本标签
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = {
                                Text(
                                    "v${module.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            // 展开的详细信息区域
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // 分隔线
                    HorizontalDivider(
                        Modifier,
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 依赖信息
                    if (module.dependence.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = "依赖",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "依赖项",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 依赖项列表
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                module.dependence.forEach { dep ->
                                    Surface(
                                        tonalElevation = 1.dp,
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Extension,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = dep.pkgId,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "版本: ${formatVersionCodeRange(dep.minVersionCode, dep.maxVersionCode)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 平台兼容性信息
                    if (module.support.getSupportedPlatforms().isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Devices,
                                    contentDescription = "平台支持",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "支持平台",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 平台标签
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                module.support.getSupportedPlatforms().forEach { platform ->
                                    ElevatedSuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                platform,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        elevation = SuggestionChipDefaults.suggestionChipElevation(
                                            elevation = 1.dp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 图标加载错误信息
                    iconLoadError?.let { error ->
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 核心模块警告
                    if (isEssential && !internalEnabled) {
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "核心模块，禁用可能导致系统不稳定",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 描述信息
                    if (module.description.isNotEmpty()) {
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                    }

                    // 按钮区域

                    // 配置按钮（条件显示）
                    onConfigure?.let { configureCallback ->
                        OutlinedButton(
                            onClick = { configureCallback(module) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "配置",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("配置")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (module.detailsURL.isNotEmpty()) {
                            // 详细信息按钮
                            OutlinedButton(
                                onClick = { openUrl(module.detailsURL) },
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

private fun formatVersionCodeRange(min: Int, max: Int): String {
    return if (max > 0 && max != min) {
        "$min-$max"
    } else {
        "$min+"
    }
}