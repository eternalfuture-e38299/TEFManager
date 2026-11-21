package eternal.future.tefmanager.modpage

import androidx.compose.runtime.Composable

import eternal.future.tefmanager.modpage.api.PageNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** Persists mod page state in <private>/page-state.json. */
class ModPageState(privatePath: String) {
    private val file = "$privatePath/page-state.json".toPath()
    private val json = Json { ignoreUnknownKeys = true }

    private fun readText(): String =
        FileSystem.SYSTEM.source(file).buffer().use { it.readUtf8() }

    private fun writeText(text: String) {
        FileSystem.SYSTEM.sink(file).buffer().use { it.writeUtf8(text) }
    }

    @Synchronized
    fun get(key: String): String? = runCatching {
        if (!FileSystem.SYSTEM.exists(file)) return null
        val root = json.parseToJsonElement(readText()).jsonObject
        root["values"]?.jsonObject?.get(key)?.toString()?.trim('"')
    }.getOrNull()

    @Synchronized
    fun set(key: String, value: String) {
        val values = mutableMapOf<String, String>()
        runCatching {
            if (FileSystem.SYSTEM.exists(file)) {
                val root = json.parseToJsonElement(readText()).jsonObject
                root["values"]?.jsonObject?.forEach { (k, v) ->
                    values[k] = v.toString().trim('"')
                }
            }
        }
        values[key] = value
        val out = buildJsonObject {
            put("values", buildJsonObject {
                values.forEach { (k, v) -> put(k, v) }
            })
        }
        file.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        writeText(out.toString())
    }
}

interface ModPageSession {
    val title: String
    fun render(): PageNode
    fun event(id: String, value: String?): PageNode
    fun close()
}

/** [hostContext] is an Android Context on Android and unused on JVM. */
expect fun openModPageSession(
    jarPath: String,
    className: String,
    privatePath: String,
    dataPath: String,
    hostContext: Any? = null,
): ModPageSession

@Composable
expect fun platformModPageContext(): Any?
