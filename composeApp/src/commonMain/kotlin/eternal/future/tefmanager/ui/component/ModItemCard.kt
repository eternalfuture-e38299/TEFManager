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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import eternal.future.tefmanager.ui.model.ModItem
import eternal.future.tefmanager.ui.model.PlatformSupport
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - ModItemCard
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
 * Created: 2026/3/22
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
            // 测试启用的Mod
            ModItemCard(
                mod = ModItem(
                    pkgId = "com.test.mod.optifine",
                    name = "OptiFine",
                    author = "sp614x",
                    brieflyDescribe = "优化游戏性能，提供更多图形选项",
                    description = "OptiFine是一个功能强大的优化Mod，可显著提升游戏帧率，支持高清材质、光影效果，并提供丰富的图形设置选项。",
                    version = "HD_U_H1",
                    versionCode = 1,
                    features = listOf(
                        ModItem.ModFeature.VISUAL_ENHANCEMENT,
                        ModItem.ModFeature.QUALITY_OF_LIFE
                    ),
                    sizeCategory = ModItem.ModSizeCategory.MEDIUM,
                    targetGameVersion = "1.20.1",
                    minGameVersion = "1.20.0",
                    maxGameVersion = "1.20.1",
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true, arm = true),
                        windows = ArchitectureSupport(x64 = true, x86 = true)
                    ),
                    dependence = listOf(
                        Dependence("com.forge.api", 1, 0)
                    ),
                    conflicts = listOf("com.test.mod.fabulous"),
                    detailsURL = "https://optifine.net",
                    stableVerified = true,
                    experimental = false,
                    deprecated = false,
                    hasExtendedContent = true
                ),
                onConfigure = {},
                enabled = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 测试禁用的Mod
            ModItemCard(
                mod = ModItem(
                    pkgId = "com.test.mod.journeymap",
                    name = "JourneyMap",
                    author = "techbrew & mysticdrew",
                    brieflyDescribe = "实时小地图和全屏地图",
                    description = "JourneyMap提供实时小地图、全屏地图、地点标记、路径点、生物雷达等功能，是探索世界的必备工具。",
                    version = "5.9.0",
                    versionCode = 9,
                    features = listOf(
                        ModItem.ModFeature.ASSISTANCE,
                        ModItem.ModFeature.UTILITY,
                        ModItem.ModFeature.UI_IMPROVEMENT
                    ),
                    sizeCategory = ModItem.ModSizeCategory.SMALL,
                    targetGameVersion = "1.20.1",
                    minGameVersion = "1.20.0",
                    maxGameVersion = "1.20.1",
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true),
                        windows = ArchitectureSupport(x64 = true),
                        linux = ArchitectureSupport(x64 = true)
                    ),
                    dependence = listOf(
                        Dependence("com.journeymap.api", 1, 0)
                    ),
                    detailsURL = "https://journeymap.info",
                    stableVerified = false,
                    experimental = true,
                    deprecated = false,
                    hasExtendedContent = false
                ),
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 测试无依赖的Mod
            ModItemCard(
                mod = ModItem(
                    pkgId = "com.test.mod.appleskin",
                    name = "AppleSkin",
                    author = "squeek502",
                    brieflyDescribe = "显示食物和饱和度的更多信息",
                    description = "在HUD上显示食物和饱和度的详细数值，帮助玩家更好地管理饥饿值。",
                    version = "2.5.0",
                    versionCode = 5,
                    features = listOf(
                        ModItem.ModFeature.QUALITY_OF_LIFE,
                        ModItem.ModFeature.UI_IMPROVEMENT
                    ),
                    sizeCategory = ModItem.ModSizeCategory.TINY,
                    targetGameVersion = "1.20.1",
                    minGameVersion = "1.20.0",
                    maxGameVersion = "1.20.1",
                    support = PlatformSupport(
                        android = ArchitectureSupport(arm64 = true, arm = true),
                        windows = ArchitectureSupport(x64 = true, x86 = true)
                    ),
                    dependence = listOf(),
                    detailsURL = "https://github.com/squeek502/AppleSkin",
                    stableVerified = true,
                    experimental = false,
                    deprecated = false,
                    hasExtendedContent = false
                ),
                enabled = true
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModItemCard(
    mod: ModItem,
    enabled: Boolean = false,
    customIconPath: Path? = null,
    onEnableChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    onDetails: () -> Unit = {},
    onConfigure: ((ModItem) -> Unit)? = null
) {
    val fileSystem: FileSystem = FileSystem.SYSTEM
    var expanded by remember { mutableStateOf(false) }
    var internalEnabled by remember { mutableStateOf(enabled) }
    var hasCustomIcon by remember { mutableStateOf(false) }
    var iconLoadError by remember { mutableStateOf<String?>(null) }
    var dependExpanded by remember { mutableStateOf(false) }
    var supportExpanded by remember { mutableStateOf(false) }

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

    // MD3E设计
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
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = "Mod图标",
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
                        // 名称
                        Text(
                            text = mod.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        // 版本标签
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "v${mod.version}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                    text = "自定义",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 特征标签区域
                    if (mod.features.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 规模分类标签
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 1.dp,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = mod.sizeCategory.getDisplayText(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }

                            // Mod特征标签
                            mod.features.take(3).forEach { feature ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    tonalElevation = 1.dp,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = feature.getDisplayText(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // 扩展内容标记
                            if (mod.hasExtendedContent) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    tonalElevation = 1.dp,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "扩展内容",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ID和作者信息
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 简介
                        if (mod.brieflyDescribe.isNotBlank()) {
                            Text(
                                text = mod.brieflyDescribe,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ID标签
                            Text(
                                text = mod.pkgId,
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
                                    contentDescription = "作者",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = mod.author,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 操作区域
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 启用开关
                    Switch(
                        checked = internalEnabled,
                        onCheckedChange = { newValue ->
                            internalEnabled = newValue
                            onEnableChange(newValue)
                        },
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
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    // 分隔线
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 状态标记
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mod.stableVerified) {
                            ElevatedAssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "已验证",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp)
                            )
                        }

                        if (mod.experimental) {
                            ElevatedAssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "实验性",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Science,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp)
                            )
                        }

                        if (mod.deprecated) {
                            ElevatedAssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "已弃用",
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
                        }
                    }

                    // 游戏版本信息
                    if (mod.targetGameVersion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.VideogameAsset,
                                    contentDescription = "游戏版本",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "游戏版本",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "目标版本: ${mod.targetGameVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (mod.minGameVersion.isNotBlank() && mod.maxGameVersion.isNotBlank()) {
                                    Text(
                                        text = "兼容: ${mod.minGameVersion} - ${mod.maxGameVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
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

                    // 详细描述
                    if (mod.description.isNotBlank()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = "描述",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "详细描述",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = mod.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 依赖信息（可折叠）
                    if (mod.dependence.isNotEmpty()) {
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
                                    text = "依赖项",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.weight(1f))

                                // 展开/收起按钮
                                IconButton(
                                    onClick = { dependExpanded = !dependExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = dependExpanded,
                                        transitionSpec = {
                                            (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                                        },
                                        label = "Dependency Expand Icon"
                                    ) { isExpanded ->
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = if (isExpanded) "收起依赖" else "展开依赖",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 依赖项列表
                            AnimatedVisibility(
                                visible = dependExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    mod.dependence.forEach { dep ->
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
                        }

                        Spacer(modifier = Modifier.height(if (dependExpanded) 20.dp else 12.dp))
                    }

                    // 平台兼容性信息（可折叠）
                    if (mod.support.getSupportedPlatforms().isNotEmpty()) {
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.weight(1f))

                                // 展开/收起按钮
                                IconButton(
                                    onClick = { supportExpanded = !supportExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = supportExpanded,
                                        transitionSpec = {
                                            (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                                        },
                                        label = "Support Expand Icon"
                                    ) { isExpanded ->
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = if (isExpanded) "收起平台" else "展开平台",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 平台标签
                            AnimatedVisibility(
                                visible = supportExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    mod.support.getSupportedPlatforms().forEach { platform ->
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
                        }

                        Spacer(modifier = Modifier.height(if (supportExpanded) 20.dp else 12.dp))
                    }

                    // 冲突信息
                    if (mod.conflicts.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = "冲突",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "冲突Mod",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                mod.conflicts.forEach { conflictId ->
                                    Surface(
                                        tonalElevation = 1.dp,
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )

                                            Text(
                                                text = conflictId,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 按钮区域
                    onConfigure?.let { configureCallback ->
                        OutlinedButton(
                            onClick = { configureCallback(mod) },
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

private fun Path.toFileUrlString(): String {
    val pathStr = this.toString()
    val normalizedPath = if (pathStr.contains('\\')) {
        pathStr.replace('\\', '/')
    } else {
        pathStr
    }
    val finalPath = if (normalizedPath.startsWith("/")) {
        normalizedPath
    } else {
        "/$normalizedPath"
    }
    return "file://$finalPath"
}

private fun formatVersionCodeRange(min: Int, max: Int): String {
    return if (max > 0 && max != min) {
        "$min-$max"
    } else {
        "$min+"
    }
}