package eternal.future.tefmanager.ui.dialogs

/*******************************************************************************
 * TEFManager - GlobalConfigEditor
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
 * Created: 2026/3/28
 *******************************************************************************/

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eternal.future.tefmanager.ui.model.GlobalConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalConfigEditor(
    globalConfig: GlobalConfig,
    fileContent: Map<String, Any?>? = null,
    onConfigChange: (Map<String, Any?>) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    val categories = globalConfig.configItems
        .map { it.type.name }
        .distinct()
        .sorted()

    val filteredItems = globalConfig.configItems.filter { item ->
        val matchesSearch = searchText.isEmpty() ||
                item.key.contains(searchText, ignoreCase = true) ||
                item.description.contains(searchText, ignoreCase = true) || item.displayName.contains(searchText, ignoreCase = true)

        val matchesCategory = selectedCategory.isEmpty() ||
                item.type.name == selectedCategory

        matchesSearch && matchesCategory
    }

    val modifiedCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(fileContent) {
        modifiedCount.intValue = fileContent?.keys?.size ?: 0
    }

    AlertDialog(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部应用栏
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "全局配置编辑器",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (modifiedCount.intValue > 0) {
                                Text(
                                    text = "${modifiedCount.intValue} 项已修改",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ),
                    actions = {
                        // 搜索框
                        var isSearchExpanded by remember { mutableStateOf(false) }

                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("搜索配置项...") },
                                singleLine = true,
                                modifier = Modifier
                                    .width(250.dp)
                                    .height(48.dp),
                                trailingIcon = {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(Icons.Default.Close, "清除")
                                    }
                                }
                            )
                        }

                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(Icons.Default.Search, "搜索")
                        }

                        // 重置按钮
                        IconButton(
                            onClick = {
                                onConfigChange(emptyMap())
                                modifiedCount.intValue = 0
                            },
                            enabled = modifiedCount.intValue > 0
                        ) {
                            Icon(Icons.Default.Restore, "重置")
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    }
                )

                // 分类筛选栏
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = selectedCategory.isEmpty(),
                            onClick = { selectedCategory = "" },
                            label = { Text("全部") },
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }

                // 配置项列表
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "未找到匹配的配置项",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredItems.size) { index ->
                                val configItem = filteredItems[index]
                                ConfigItemCard(
                                    configItem = configItem,
                                    currentValue = fileContent?.get(configItem.key),
                                    onValueChange = { newValue ->
                                        val updatedContent = fileContent?.toMutableMap() ?: mutableMapOf()
                                        if (newValue == null) {
                                            updatedContent.remove(configItem.key)
                                        } else {
                                            updatedContent[configItem.key] = newValue
                                        }
                                        onConfigChange(updatedContent)

                                        // 更新修改计数
                                        val currentValue = fileContent?.get(configItem.key)
                                        modifiedCount.intValue = if (currentValue == newValue) {
                                            maxOf(0, modifiedCount.intValue - 1)
                                        } else {
                                            modifiedCount.intValue + 1
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 底部操作栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = onDismiss,
                            enabled = modifiedCount.intValue > 0
                        ) {
                            Icon(Icons.Default.Save, "保存", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存更改")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItemCard(
    configItem: GlobalConfig.ConfigItem,
    currentValue: Any?,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型标签
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = configItem.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 配置项键名
                Text(
                    text = configItem.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            // 描述文本
            Text(
                text = configItem.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 配置编辑器
            ConfigItemEditor(
                configItem = configItem,
                currentValue = currentValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConfigItemEditor(
    configItem: GlobalConfig.ConfigItem,
    currentValue: Any?,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (configItem.type) {
        GlobalConfig.Type.BOOL -> {
            var isChecked by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Boolean -> currentValue
                        is String -> currentValue.toBooleanStrictOrNull() ?: false
                        is Number -> currentValue.toInt() != 0
                        else -> false
                    }
                )
            }

            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = configItem.placeholder,
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = isChecked,
                    onCheckedChange = { newValue ->
                        isChecked = newValue
                        onValueChange(newValue)
                    }
                )
            }
        }

        GlobalConfig.Type.INT8,
        GlobalConfig.Type.INT16,
        GlobalConfig.Type.INT32 -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toInt())
                    }
                },
                label = { Text(configItem.placeholder) },
                placeholder = { Text("请输入整数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.INT64 -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toLongOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toLong())
                    }
                },
                label = { Text(configItem.placeholder) },
                placeholder = { Text("请输入长整数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.UINT8,
        GlobalConfig.Type.UINT16,
        GlobalConfig.Type.UINT32 -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toUIntOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toUInt())
                    }
                },
                label = { Text(configItem.placeholder) },
                placeholder = { Text("请输入无符号整数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.UINT64 -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toULongOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toULong())
                    }
                },
                label = { Text(configItem.placeholder) },
                placeholder = { Text("请输入无符号长整数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.FLOAT -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toFloatOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toFloat())
                    }
                },
                label = { Text(configItem.placeholder) },
                placeholder = { Text("请输入浮点数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.DOUBLE -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is Number -> currentValue.toString()
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                        text = newValue
                        onValueChange(if (newValue.isEmpty()) null else newValue.toDouble())
                    }
                },
                label = { Text(configItem.placeholder ?: "双精度浮点数") },
                placeholder = { Text("请输入双精度浮点数") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = modifier
            )
        }

        GlobalConfig.Type.STRING -> {
            var text by remember(currentValue) {
                mutableStateOf(
                    when (currentValue) {
                        is String -> currentValue
                        else -> ""
                    }
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    text = newValue
                    onValueChange(newValue.ifEmpty { null })
                },
                label = { Text(configItem.placeholder ?: "字符串") },
                placeholder = { Text("请输入文本") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                modifier = modifier
            )
        }

        else -> {}
    }
}

object ConfigManager {
    private val fileSystem = FileSystem.SYSTEM

    fun readConfigFile(filePath: String): Map<String, Any?>? {
        return try {
            val file = filePath.toPath()
            if (!fileSystem.exists(file)) return emptyMap()

            val jsonString = fileSystem.source(file).buffer().readUtf8()
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<Map<String, JsonElement>>(jsonString)
                .mapValues { entry ->
                    when (val element = entry.value) {
                        is JsonPrimitive -> {
                            if (element.isString) element.content
                            else if (element.booleanOrNull != null) element.boolean
                            else if (element.longOrNull != null) element.long
                            else if (element.doubleOrNull != null) element.double
                            else if (element.intOrNull != null) element.int
                            else if (element.floatOrNull != null) element.float
                            else element.content
                        }
                        is JsonArray -> element.jsonArray
                        is JsonObject -> element.jsonObject
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeConfigFile(filePath: String, updates: Map<String, Any?>): Boolean {
        return try {
            val file = filePath.toPath()

            // 读取现有配置
            val existingConfig = if (fileSystem.exists(file)) {
                val jsonString = fileSystem.source(file).buffer().readUtf8()
                val json = Json { ignoreUnknownKeys = true }
                json.parseToJsonElement(jsonString) as? JsonObject
            } else {
                buildJsonObject { }
            } ?: buildJsonObject { }

            // 创建更新后的配置
            val updatedJsonObject = buildJsonObject {
                // 先添加所有现有配置
                existingConfig.forEach { (key, value) ->
                    put(key, value)
                }
                // 然后更新/添加新配置
                updates.forEach { (key, value) ->
                    when (value) {
                        null -> put(key, JsonNull)
                        is Boolean -> put(key, value)
                        is Int -> put(key, value)
                        is Long -> put(key, value)
                        is Float -> put(key, value)
                        is Double -> put(key, value)
                        is String -> put(key, value)
                        is Number -> put(key, value) // 处理其他数字类型
                        is JsonElement -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }

            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(updatedJsonObject)

            // 写入文件
            file.parent?.let { fileSystem.createDirectories(it) }
            fileSystem.sink(file).buffer().use { sink ->
                sink.writeUtf8(jsonString)
                sink.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

