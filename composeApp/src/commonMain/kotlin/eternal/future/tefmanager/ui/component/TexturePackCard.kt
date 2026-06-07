package eternal.future.tefmanager.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.ui.model.TexturePackItem
import eternal.future.tefmanager.utils.toFileUrlString
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

/*******************************************************************************
 * TEFManager - TexturePackCard
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


@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PreviewTexturePackCard() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // 创建一个可变的材质包列表用于演示交互
            val packs = remember {
                mutableStateListOf(
                    TexturePackItem(
                        name = "官方材质包",
                        author = "Re-Logic",
                        description = "Terraria官方高清材质包，提升游戏画质",
                        version = "1.4.4",
                        fileName = "OfficialTexturePack.zip",
                        iconPath = "",
                        type = TexturePackItem.Type.Terraria
                    ),
                    TexturePackItem(
                        name = "光影增强包",
                        author = "LightMaster",
                        description = "增强游戏光影效果，让画面更真实",
                        version = "2.1.0",
                        fileName = "LightingBoost.zip",
                        iconPath = "",
                        type = TexturePackItem.Type.TLPro
                    ),
                    TexturePackItem(
                        name = "复古像素风格",
                        author = "PixelArtisan",
                        description = "将游戏画面转换为复古像素风格，怀旧体验",
                        version = "1.0.3",
                        fileName = "RetroPixel.zip",
                        iconPath = "",
                        type = TexturePackItem.Type.Terraria
                    ),
                    TexturePackItem(
                        name = "高清武器纹理",
                        author = "WeaponMaster",
                        description = "所有武器高清重制，细节更丰富",
                        version = "3.0.0",
                        fileName = "HDWeapons.zip",
                        iconPath = "",
                        type = TexturePackItem.Type.TLPro
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(packs.size) { index ->
                    val pack = packs[index]
                    var isEnabled by remember { mutableStateOf(index < 2) } // 前两个默认启用

                    TexturePackCard(
                        pack = pack,
                        index = index,
                        totalItems = packs.size,
                        isEnabled = isEnabled,
                        onEnableChange = { enabled ->
                            isEnabled = enabled
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val temp = packs[index]
                                packs[index] = packs[index - 1]
                                packs[index - 1] = temp
                            }
                        },
                        onMoveDown = {
                            if (index < packs.size - 1) {
                                val temp = packs[index]
                                packs[index] = packs[index + 1]
                                packs[index + 1] = temp
                            }
                        },
                        onDelete = {
                            packs.removeAt(index)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 添加一个包含自定义图标的示例
                item {
                    val customIconPack = TexturePackItem(
                        name = "自定义图标材质包",
                        author = "Designer",
                        description = "展示自定义图标功能的材质包",
                        version = "1.0.0",
                        fileName = "CustomIcon.zip",
                        iconPath = "/path/to/icon.png", // 实际使用时替换为真实路径
                        type = TexturePackItem.Type.Terraria
                    )
                    var isEnabled by remember { mutableStateOf(false) }

                    TexturePackCard(
                        pack = customIconPack,
                        index = packs.size,
                        totalItems = packs.size + 1,
                        isEnabled = isEnabled,
                        onEnableChange = { enabled -> isEnabled = enabled },
                        onMoveUp = {},
                        onMoveDown = {},
                        onDelete = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TexturePackCard(
    pack: TexturePackItem,
    index: Int,
    totalItems: Int,
    isEnabled: Boolean,
    onEnableChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var iconLoadError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            // 第一行：图标、信息和开关
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 材质包图标
                if (pack.iconPath.isNotBlank()) {
                    val fileSystem = FileSystem.SYSTEM
                    val customIconPath = pack.iconPath.toPath()

                    if (fileSystem.exists(customIconPath)) {
                        KamelImage(
                            resource = { asyncPainterResource(data = customIconPath.toFileUrlString()) },
                            contentDescription = "自定义图标",
                            onFailure = { _ ->
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
                    // 默认图标
                    Icon(
                        imageVector = Icons.Default.Texture,
                        contentDescription = "默认图标",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 材质包信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "作者: ${pack.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )

                    if (pack.version.isNotBlank()) {
                        Text(
                            text = "版本: ${pack.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isEnabled) 1f else 0.5f)
                        )
                    }
                }

                // 开关
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnableChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 展开/收起按钮
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (expanded) 180f else 0f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // 展开后的详细信息（带动画）
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // 描述文本
                    if (pack.description.isNotBlank()) {
                        Text(
                            text = "描述: ${pack.description}",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 图标加载错误提示
                    iconLoadError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 底部操作行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 类型和顺序信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 类型标签
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (pack.type) {
                                    TexturePackItem.Type.Terraria ->
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    TexturePackItem.Type.TLPro ->
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = pack.type.getText(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (pack.type) {
                                        TexturePackItem.Type.Terraria ->
                                            MaterialTheme.colorScheme.primary
                                        TexturePackItem.Type.TLPro ->
                                            MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // 加载顺序
                            Text(
                                text = "加载顺序: ${index + 1}/$totalItems",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }

                        // 操作按钮
                        Row {
                            // 顺序调整按钮（只有启用时才能修改顺序）
                            IconButton(
                                onClick = onMoveUp,
                                enabled = isEnabled && index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "上移",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isEnabled && index > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = onMoveDown,
                                enabled = isEnabled && index < totalItems - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "下移",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isEnabled && index < totalItems - 1)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }

                            // 删除按钮
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // 顺序说明文字（只有启用时显示）
                    if (isEnabled) {
                        Text(
                            text = "提示：材质包按顺序从上到下加载，下方的会覆盖上方的同名资源",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}