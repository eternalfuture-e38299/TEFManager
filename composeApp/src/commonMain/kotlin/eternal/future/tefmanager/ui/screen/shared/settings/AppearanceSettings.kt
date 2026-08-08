package eternal.future.tefmanager.ui.screen.shared.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.materialkolor.ktx.toHex
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.ConfigurationState.AppConfig
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.SettingItem
import eternal.future.tefmanager.ui.component.SettingsGroup
import eternal.future.tefmanager.ui.component.Switch

/*******************************************************************************
 * TEFManager - AppearanceSettings
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
 * Created: 2026/8/6
 *******************************************************************************/

@Composable
fun AppearanceSettings() {
    val appearance = remember { Strings.settings.appearance }

    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showColorPicker) {
            ColorPickerDialog(
                initialColor = ConfigurationState.themeSeedColor,
                onColorSelected = { color ->
                    ConfigurationState.themeSeedColor = color
                },
                onDismiss = { showColorPicker = false }
            )
        }

        SettingsGroup {
            SettingItem(
                icon = Icons.Rounded.DarkMode,
                title = appearance.theme,
                description = appearance.themeDec,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val themes = listOf(
                        AppConfig.ThemeMode.SYSTEM,
                        AppConfig.ThemeMode.LIGHT,
                        AppConfig.ThemeMode.DARK,
                        AppConfig.ThemeMode.AUTO,
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Text(
                                text = ConfigurationState.themeMode.toString(),
                                style = MaterialTheme.typography.labelMedium
                            )
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
                                    text = {
                                        Text(
                                            text = t.toString(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        ConfigurationState.themeMode = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            SettingItem(
                icon = Icons.Rounded.Palette,
                title = appearance.dynamicColor,
                description = appearance.dynamicColorDec,
                enabled = Platform.dynamicColor,
                trailingContent = {
                    Switch(
                        enabled = Platform.dynamicColor,
                        checked = ConfigurationState.dynamicColor,
                        onCheckedChange = { ConfigurationState.dynamicColor = it }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.ColorLens,
                title = appearance.themeColor,
                description = appearance.themeColorDec,
                enabled = !ConfigurationState.dynamicColor,
                showDivider = false,
                trailingContent = {
                    Surface(
                        shape = CircleShape,
                        color = if (!ConfigurationState.dynamicColor) {
                            ConfigurationState.themeSeedColor
                        } else {
                            ConfigurationState.themeSeedColor.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(enabled = !ConfigurationState.dynamicColor) {
                                if (!ConfigurationState.dynamicColor) showColorPicker = true
                            }
                    ) {}
                }
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    val controller = remember { ColorPickerController() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = Strings.settings.appearance.chooseColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = currentColor,
                            modifier = Modifier.size(40.dp)
                        ) {}

                        Column {
                            Text(
                                text = "RGB: ${(currentColor.red * 255).toInt()}, " +
                                        "${(currentColor.green * 255).toInt()}, " +
                                        "${(currentColor.blue * 255).toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "HEX: #${currentColor.toHex()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                Strings.cancel,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = {
                                onColorSelected(currentColor)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                Strings.apply,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
