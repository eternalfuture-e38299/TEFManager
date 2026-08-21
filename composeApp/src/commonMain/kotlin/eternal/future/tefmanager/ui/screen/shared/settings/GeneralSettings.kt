package eternal.future.tefmanager.ui.screen.shared.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.SettingItem
import eternal.future.tefmanager.ui.component.SettingsGroup
import eternal.future.tefmanager.ui.component.Switch

/*******************************************************************************
 * TEFManager - GeneralSettings
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
fun GeneralSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettingsGroup {
            SettingItem(
                icon = Icons.Rounded.Update,
                title = Strings.settings.general.update,
                description = Strings.settings.general.updateDec,
                trailingContent = {
                    Switch(
                        checked = ConfigurationState.autoUpdate,
                        onCheckedChange = { ConfigurationState.autoUpdate = it }
                    )
                }
            )

            SettingItem(
                icon = Icons.Rounded.Translate,
                title = Strings.settings.general.language,
                description = Strings.settings.general.languageDec,
                showDivider = false,
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        StringsResource.Language.System,
                        StringsResource.Language.ZhHans,
                        StringsResource.Language.En,
                        StringsResource.Language.Ru
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
                                text = ConfigurationState.language.toString(),
                                style = MaterialTheme.typography.labelMedium
                            )
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
                                    text = {
                                        Text(
                                            text = lang.toString(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        ConfigurationState.language = lang
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
}