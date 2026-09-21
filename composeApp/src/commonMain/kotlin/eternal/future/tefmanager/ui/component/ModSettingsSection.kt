package eternal.future.tefmanager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.model.ModItem
import eternal.future.tefmanager.utils.addon.ModSettingsStore
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Generic, schema-driven configuration panel shown inside an expanded mod card. */
@Composable
fun ModSettingsSection(mod: ModItem, store: ModSettingsStore) {
    var values by remember(mod.pkgId) { mutableStateOf(store.load(mod.settings)) }

    fun update(key: String, value: JsonElement) {
        values = values + (key to value)
        store.save(values)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text("模组设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        mod.settings.forEach { setting ->
            val current = values[setting.key] ?: setting.defaultValue
            when (setting.type) {
                ModItem.SettingType.SWITCH -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(setting.title, style = MaterialTheme.typography.bodyMedium)
                        if (setting.description.isNotBlank()) Text(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = current.jsonPrimitive.booleanOrNull ?: false,
                        onCheckedChange = { update(setting.key, JsonPrimitive(it)) }
                    )
                }

                ModItem.SettingType.INTEGER -> {
                    val min = setting.min.takeIf { it > 0 } ?: 1
                    val max = setting.max.takeIf { it > 0 } ?: 10
                    val now = (current.jsonPrimitive.intOrNull ?: min).coerceIn(min, max)
                    Text("${setting.title}：${now}×", style = MaterialTheme.typography.bodyMedium)
                    if (setting.description.isNotBlank()) Text(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = now.toFloat(),
                        onValueChange = { raw ->
                            val step = setting.step.coerceAtLeast(1)
                            val snapped = (min + ((raw.toInt() - min + step / 2) / step) * step).coerceIn(min, max)
                            update(setting.key, JsonPrimitive(snapped))
                        },
                        valueRange = min.toFloat()..max.toFloat(),
                        steps = ((max - min) / setting.step.coerceAtLeast(1) - 1).coerceAtLeast(0)
                    )
                }

                ModItem.SettingType.CHOICE -> {
                    Text(setting.title, style = MaterialTheme.typography.bodyMedium)
                    if (setting.description.isNotBlank()) Text(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        setting.options.forEach { option ->
                            AssistChip(
                                onClick = { update(setting.key, JsonPrimitive(option.value)) },
                                label = { Text(if (current.jsonPrimitive.contentOrNull == option.value) "✓ ${option.label}" else option.label) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
