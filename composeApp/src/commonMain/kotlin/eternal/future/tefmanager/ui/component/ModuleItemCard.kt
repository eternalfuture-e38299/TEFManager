package eternal.future.tefmanager.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.style.TextOverflow
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

    LaunchedEffect(enabled) {
        internalEnabled = enabled
    }

    LaunchedEffect(customIconPath) {
        if (customIconPath == null) {
            hasCustomIcon = false
            iconLoadError = null
            return@LaunchedEffect
        }

        try {
            hasCustomIcon = fileSystem.exists(customIconPath)
        } catch (_: Exception) {
            iconLoadError = "图标加载失败"
            hasCustomIcon = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (internalEnabled) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (internalEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(40.dp)
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
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onFailure = { _ ->
                                        iconLoadError = "图标加载失败"
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = "图标不存在",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "模块图标",
                                modifier = Modifier.size(20.dp),
                                tint = if (internalEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (internalEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isEssential) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = "核心",
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "核心",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (hasCustomIcon) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "自定义",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = module.pkgId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "v${module.version}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (module.brieflyDescribe.isNotEmpty()) {
                        Text(
                            text = module.brieflyDescribe,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (internalEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

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
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = internalEnabled,
                        onCheckedChange = { newValue ->
                            internalEnabled = newValue
                            onEnableChange(newValue)
                        },
                        enabled = !isEssential,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        thumbContent = {
                            Icon(
                                imageVector = if (internalEnabled) {
                                    Icons.Rounded.Check
                                } else {
                                    Icons.Rounded.Close
                                },
                                contentDescription = if (internalEnabled) "已启用" else "已禁用",
                                modifier = Modifier.size(14.dp),
                                tint = if (internalEnabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        }
                    )

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        AnimatedContent(
                            targetState = expanded,
                            transitionSpec = {
                                (fadeIn() + slideInVertically()).togetherWith(
                                    fadeOut() + slideOutVertically()
                                )
                            },
                            label = "Expand Icon Animation"
                        ) { isExpanded ->
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isExpanded) "收起" else "展开",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

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

                    if (module.description.isNotEmpty()) {
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (module.dependence.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = "依赖",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "依赖项 (${module.dependence.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                module.dependence.forEach { dep ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
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

                    if (module.support.getSupportedPlatforms().isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Devices,
                                    contentDescription = "平台支持",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "支持平台",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

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
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
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

                    if (isEssential && !internalEnabled) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "核心模块，禁用可能导致系统不稳定",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Warning,
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (module.globalConfig.fileType != "null" && module.globalConfig.fileType.isNotEmpty()) {
                            onConfigure?.let { configureCallback ->
                                OutlinedButton(
                                    onClick = { configureCallback(module) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
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
                        }

                        if (module.detailsURL.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { openUrl(module.detailsURL) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
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

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
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