package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.materialkolor.ktx.toHex
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.ConfigManager
import org.jetbrains.compose.resources.painterResource
import tefmanager.composeapp.generated.resources.Res
import tefmanager.composeapp.generated.resources.icon

/*******************************************************************************
 * TEFManager - SettingsContent
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
 * Created: 2026/2/5
 *******************************************************************************/

object SettingsContent {
    @Composable
    fun GeneralSettings() {
        var autoUpdate by remember { mutableStateOf(true) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                icon = Icons.Rounded.Update,
                title = Strings.settings.general.update,
                description = Strings.settings.general.updateDec,
                trailingContent = {
                    Switch(
                        checked = autoUpdate,
                        {
                            autoUpdate = it
                        }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Translate,
                title = Strings.settings.general.language.title,
                description = Strings.settings.general.language.dec,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        StringsResource.Language.System,
                        StringsResource.Language.ZhHans,
                        StringsResource.Language.En
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(StringsResource.currentLanguage.toString())
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Select Language"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.toString()) },
                                    onClick = {
                                        StringsResource.setLanguage(lang)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

        }
    }

    @Composable
    fun SettingSectionTitle(
        title: String,
        icon: ImageVector
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    @Composable
    fun SettingItem(
        icon: ImageVector,
        title: String,
        description: String,
        enabled: Boolean = true,
        trailingContent: @Composable () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailingContent()
        }
    }

    @Composable
    fun AppearanceSettings() {
        val appearance = remember { Strings.settings.appearance }

        var theme by remember { mutableStateOf(ConfigManager.AppConfig.Theme.SYSTEM) }
        var fontSize by remember { mutableStateOf(17.0f) }
        var showColorPicker by remember { mutableStateOf(false) }
        var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) }
        var dynamicColorEnabled by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (showColorPicker) {
                ColorPickerDialog(
                    initialColor = selectedColor,
                    onColorSelected = { color ->
                        selectedColor = color
                        // ConfigManager.saveThemeColor(color)
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            SettingItem(
                icon = Icons.Rounded.DarkMode,
                title = appearance.theme,
                description = appearance.themeDec,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val themes = listOf(
                        ConfigManager.AppConfig.Theme.SYSTEM,
                        ConfigManager.AppConfig.Theme.LIGHT,
                        ConfigManager.AppConfig.Theme.DARK,
                        ConfigManager.AppConfig.Theme.AUTO,
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(theme.toString())
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Choose Theme"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            themes.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.toString()) },
                                    onClick = {
                                        theme = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            SettingItem(
                icon = Icons.Rounded.FontDownload,
                title = appearance.fontSize,
                description = appearance.fontSizeDec,
                trailingContent = {
                    Text("${fontSize.toInt()}sp")
                }
            )

            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                },
                valueRange = 12f..24f,
                steps = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(appearance.small, style = MaterialTheme.typography.labelSmall)
                Text(appearance.default, style = MaterialTheme.typography.labelSmall)
                Text(appearance.big, style = MaterialTheme.typography.labelSmall)
            }

            SettingItem(
                icon = Icons.Rounded.Palette,
                title = appearance.dynamicColor,
                description = appearance.dynamicColorDec,
                enabled = Platform.dynamicColor,
                trailingContent = {
                    Switch(
                        enabled = Platform.dynamicColor,
                        checked = dynamicColorEnabled,
                        onCheckedChange = {
                            dynamicColorEnabled = it
                        }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.ColorLens,
                title = appearance.themeColor,
                description = appearance.themeColorDec,
                enabled = !dynamicColorEnabled,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (!dynamicColorEnabled) selectedColor else selectedColor.copy(0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { if (!dynamicColorEnabled) showColorPicker = true }
                    )
                }
            )
        }
    }

    @Composable
    fun ColorPickerDialog(
        initialColor: Color,
        onColorSelected: (Color) -> Unit,
        onDismiss: () -> Unit
    ) {
        var currentColor by remember { mutableStateOf(initialColor) }
        val controller = remember { ColorPickerController() }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = Strings.settings.appearance.chooseColor,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        initialColor = currentColor,
                        onColorChanged = { colorEnvelope ->
                            currentColor = colorEnvelope.color
                        },
                        controller = controller
                    )

                    AlphaSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        wheelAlpha = currentColor.alpha,
                        initialColor = currentColor.copy(alpha = 1f),
                        controller = controller
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )

                            Column {
                                Text(
                                    text = "RGB: ${(currentColor.red * 255).toInt()}, " +
                                            "${(currentColor.green * 255).toInt()}, " +
                                            "${(currentColor.blue * 255).toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "HEX: #${currentColor.toHex()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(Strings.cancel)
                            }

                            Button(
                                onClick = {
                                    onColorSelected(currentColor)
                                    onDismiss()
                                }
                            ) {
                                Text(Strings.apply)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AdvancedSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 内核日志相关设置
            SettingItem(
                icon = Icons.Rounded.Memory,
                title = Strings.settings.advanced.kernelLog,
                description = Strings.settings.advanced.kernelLogDec,
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Storage,
                title = Strings.settings.advanced.dumpKernelLog,
                description = Strings.settings.advanced.dumpKernelLogDec,
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )

            SettingItem(
                icon = Icons.AutoMirrored.Rounded.CompareArrows, // 使用这个存在的图标
                title = Strings.settings.advanced.redirectKernelLog,
                description = Strings.settings.advanced.redirectKernelLogDec,
                enabled = Platform.isAndroid,
                trailingContent = {
                    Switch(
                        enabled = Platform.isAndroid,
                        checked = false,
                        onCheckedChange = {}
                    )
                }
            )

            // 软件日志相关设置
            SettingItem(
                icon = Icons.Rounded.Code,
                title = Strings.settings.advanced.softwareLog,
                description = Strings.settings.advanced.softwareLogDec,
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.SaveAlt,
                title = Strings.settings.advanced.dumpSoftwareLog,
                description = Strings.settings.advanced.dumpSoftwareLogDec,
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )

            // 自动清理日志开关
            SettingItem(
                icon = Icons.Rounded.AutoDelete,
                title = Strings.settings.advanced.autoCleanLogs,
                description = Strings.settings.advanced.autoCleanLogsDec,
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )

            // 当自动清理开启时显示清理时间设置
            if (true) { // 这里后续替换为autoCleanEnabled状态
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.settings.advanced.cleanTimeRange,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "240${Strings.settings.advanced.minutesAgo}", // 这里后续替换为autoCleanTime状态
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Slider(
                        value = 240f, // 这里后续替换为autoCleanTime状态
                        onValueChange = { /* 后续替换为回调 */ },
                        valueRange = 60f..720f, // 1小时到12小时
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "1${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(60${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "6${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(360${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "12${Strings.settings.advanced.hours}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "(720${Strings.settings.advanced.minutes})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }

                    val timeText = if (240 <= 120) {
                        "2${Strings.settings.advanced.hours}"
                    } else if (240 <= 360) {
                        "6${Strings.settings.advanced.hours}"
                    } else if (240 <= 480) {
                        "8${Strings.settings.advanced.hours}"
                    } else {
                        "12${Strings.settings.advanced.hours}"
                    }

                    Text(
                        text = "${Strings.settings.advanced.cleanOldLogs} $timeText ${Strings.settings.advanced.createdWithin}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* 清除日志 */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = Strings.settings.advanced.clearLogs
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.settings.advanced.clearLogs)
                }

                OutlinedButton(
                    onClick = { /* 导出日志 */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = Strings.settings.advanced.exportLogs
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.settings.advanced.exportLogs)
                }
            }
        }
    }

    @Composable
    fun GameSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                icon = Icons.Rounded.Extension,
                title = "启用模组支持",
                description = "允许加载和管理游戏模组",
                enabled = Platform.isDesktop,
                trailingContent = {
                    Switch(
                        enabled = Platform.isDesktop,
                        checked = true,
                        onCheckedChange = {  }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Sync,
                title = "重定向存档",
                description = "重定向存档到软件私有目录管理",
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            )
        }
    }

    @Composable
    fun AboutSettings() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.icon),
                        contentDescription = "Logo",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 应用信息
            Text(
                text = "TEFManager",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "版本 1.0.0 (Build 202602)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "稳定版",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 信息卡片
            AboutInfoCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 开发者信息
            SettingSectionTitle(
                title = "开发者信息",
                icon = Icons.Rounded.DeveloperMode
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutItem(
                    title = "开发者",
                    value = "eternalfuture-e38299"
                )

                AboutItem(
                    title = "GitHub",
                    value = "github.com/eternalfuture-e38299"
                )

                AboutItem(
                    title = "许可证",
                    value = "GNU Affero General Public License v3.0"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* 检查更新 */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Update,
                        contentDescription = "检查更新"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("检查更新")
                }

                OutlinedButton(
                    onClick = { /* 关于 */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "关于"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("关于")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 开源声明
            Text(
                text = "TEFManager 是基于开源软件开发的游戏管理工具",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "© 2026 eternalfuture-e38299. 保留所有权利。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }


    @Composable
    private fun AboutInfoCard() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutItem(
                    title = "编译日期",
                    value = "2026年2月4日"
                )

                AboutItem(
                    title = "内核版本",
                    value = "1.0.0"
                )

                AboutItem(
                    title = "支持平台",
                    value = "Android/Windows/Linux/macOS"
                )
            }
        }
    }

    @Composable
    private fun AboutItem(
        title: String,
        value: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
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