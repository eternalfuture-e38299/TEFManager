package eternal.future.tefmanager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eternal.future.tefmanager.model.ModItem
import eternal.future.tefmanager.utils.addon.ModSettingsStore
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Compact entry point on the mod card; details are edited in a second-level window. */
@Composable
fun ModSettingsSection(mod: ModItem, store: ModSettingsStore) {
    var values by remember(mod.pkgId) { mutableStateOf(store.load(mod.settings)) }
    var settingsOpen by remember { mutableStateOf(false) }
    var invalidKeys by remember { mutableStateOf(emptySet<String>()) }

    fun update(key: String, value: JsonElement) {
        values = values + (key to value)
    }

    OutlinedButton(
        onClick = {
            invalidKeys = emptySet()
            settingsOpen = true
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("模组设置", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("${mod.settings.size} 项", style = MaterialTheme.typography.labelMedium)
    }

    if (settingsOpen) {
        Dialog(
            onDismissRequest = { settingsOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.62f)
                            .heightIn(min = 360.dp, max = 520.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { settingsOpen = false }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(mod.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("模组设置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "调整后点击“完成”保存，重新启动游戏后生效。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val grouped = mod.settings.groupBy { it.sectionName() }
                        listOf("配方倍率", "钓鱼", "其他").forEach { section ->
                            val sectionSettings = grouped[section].orEmpty()
                            if (sectionSettings.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(section, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        sectionSettings.forEach { setting ->
                                            ModSettingEditor(
                                                setting = setting,
                                                current = values[setting.key] ?: setting.defaultValue,
                                                onChange = { update(setting.key, it) },
                                                onValidityChanged = { valid ->
                                                    invalidKeys = if (valid) invalidKeys - setting.key else invalidKeys + setting.key
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                values = mod.settings.associate { it.key to it.defaultValue }
                                invalidKeys = emptySet()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("恢复默认") }
                        Button(
                            onClick = {
                                if (invalidKeys.isEmpty()) {
                                    store.save(values)
                                    settingsOpen = false
                                }
                            },
                            enabled = invalidKeys.isEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("完成") }
                    }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModSettingEditor(
    setting: ModItem.ModSetting,
    current: JsonElement,
    onChange: (JsonElement) -> Unit,
    onValidityChanged: (Boolean) -> Unit,
) {
    when (setting.type) {
        ModItem.SettingType.SWITCH -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingLabel(setting, Modifier.weight(1f))
            Switch(
                checked = current.jsonPrimitive.booleanOrNull ?: false,
                onCheckedChange = { onChange(JsonPrimitive(it)) }
            )
        }

        ModItem.SettingType.INTEGER -> {
            val min = setting.min.takeIf { it > 0 } ?: 1
            val max = setting.max.takeIf { it > 0 } ?: 10
            val step = setting.step.coerceAtLeast(1)
            val now = (current.jsonPrimitive.intOrNull ?: min).coerceIn(min, max)
            val unit = setting.unit.ifBlank { "×" }
            var input by remember(current, min, max) { mutableStateOf(now.toString()) }
            val parsed = input.toIntOrNull()
            // Manual input only needs to stay inside the declared range. The slider
            // can still snap to its configured step, but typing a valid value such
            // as 50 must not be rejected just because it is not step-aligned.
            val inputValid = parsed != null && parsed in min..max
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${setting.title}：${now}${unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { text ->
                            if (text.all { it.isDigit() } && text.length <= 6) {
                                input = text
                                val value = text.toIntOrNull()
                                val valid = value != null && value in min..max
                                onValidityChanged(valid)
                                if (valid) onChange(JsonPrimitive(value))
                            }
                        },
                        isError = !inputValid,
                        singleLine = true,
                        suffix = { Text(unit) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (inputValid) MaterialTheme.colorScheme.primary else Color(0xFFFF7043),
                            unfocusedBorderColor = if (inputValid) MaterialTheme.colorScheme.outline else Color(0xFFFF7043),
                            errorBorderColor = Color(0xFFFF7043),
                            errorLabelColor = Color(0xFFFF7043),
                            errorTrailingIconColor = Color(0xFFFF7043),
                            errorSupportingTextColor = Color(0xFFFF7043)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(124.dp)
                    )
                }
                if (setting.description.isNotBlank()) Text(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = now.toFloat(),
                    onValueChange = { raw ->
                        // Keep dragging continuous; only the committed integer value is rounded.
                        val snapped = (min + (((raw - min) / step).roundToInt() * step)).coerceIn(min, max)
                        input = snapped.toString()
                        onValidityChanged(true)
                        onChange(JsonPrimitive(snapped))
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = 0
                )
                if (!inputValid) {
                    Text("请输入 ${min}–${max} 之间的有效值", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF7043))
                }
            }
        }

        ModItem.SettingType.CHOICE -> {
            SettingLabel(setting)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                setting.options.forEach { option ->
                    AssistChip(
                        onClick = { onChange(JsonPrimitive(option.value)) },
                        label = {
                            val selected = current.jsonPrimitive.contentOrNull == option.value
                            Text(if (selected) "✓ ${option.label}" else option.label)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(setting: ModItem.ModSetting, modifier: Modifier = Modifier.fillMaxWidth()) {
    Column(modifier) {
        Text(setting.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (setting.description.isNotBlank()) Text(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun ModItem.ModSetting.sectionName(): String = when {
    key.startsWith("fishing_") || key == "bait_save_enabled" -> "钓鱼"
    key.contains("building") || key.contains("potion") || key.contains("boss") || key.contains("torch") -> "配方倍率"
    else -> "其他"
}
