package eternal.future.tefmanager.utils.addon

import eternal.future.tefmanager.model.ModItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path
import okio.buffer

/** Settings live beside the installed mod, so they are available to its private_dir. */
class ModSettingsStore(private val privateDir: Path) {
    private val fileSystem = FileSystem.SYSTEM
    private val json = Json { prettyPrint = true }
    private val configPath = privateDir / "config.json"

    fun load(schema: List<ModItem.ModSetting>): Map<String, JsonElement> {
        val saved = try {
            if (!fileSystem.exists(configPath)) emptyMap()
            else json.parseToJsonElement(fileSystem.source(configPath).buffer().use { it.readUtf8() })
                .jsonObject["values"]?.jsonObject.orEmpty()
        } catch (_: Exception) {
            emptyMap()
        }
        return schema.associate { setting ->
            setting.key to (saved[setting.key] ?: setting.defaultValue)
        }
    }

    fun save(values: Map<String, JsonElement>) {
        fileSystem.createDirectories(privateDir)
        val tempPath = privateDir / "config.json.tmp"
        val payload = buildJsonObject {
            put("schemaVersion", JsonPrimitive(1))
            put("values", JsonObject(values))
        }
        fileSystem.sink(tempPath).buffer().use { it.writeUtf8(json.encodeToString(JsonObject.serializer(), payload)) }
        if (fileSystem.exists(configPath)) fileSystem.delete(configPath)
        fileSystem.atomicMove(tempPath, configPath)
    }
}
