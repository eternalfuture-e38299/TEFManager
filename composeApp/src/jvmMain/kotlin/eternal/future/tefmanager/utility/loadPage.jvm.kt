package eternal.future.tefmanager.utility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.URLClassLoader

data class DesktopPageEntry(
    val loader: URLClassLoader,
    val clazz: Class<*>,
    val instance: Any,
)

object DesktopModPageLoader {
    private val entries = mutableMapOf<String, DesktopPageEntry>()

    @Synchronized
    fun load(filePath: String, className: String): Result<DesktopPageEntry> {
        entries[filePath]?.let { return Result.success(it) }
        return runCatching {
            val file = File(filePath)
            require(file.exists()) { "页面文件不存在: $filePath" }
            val loader = URLClassLoader(
                arrayOf(file.toURI().toURL()),
                DesktopModPageLoader::class.java.classLoader,
            )
            val clazz = loader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance()
            val entry = DesktopPageEntry(loader, clazz, instance)
            entries[filePath] = entry
            entry
        }
    }

    @Synchronized
    fun release(filePath: String) {
        entries.remove(filePath)?.loader?.close()
    }
}

@Composable
actual fun LoadAndDisplayPage(
    filePath: File,
    className: String,
    params: Map<String, *>,
) {
    val result = remember(filePath.absolutePath, className) {
        DesktopModPageLoader.load(filePath.absolutePath, className)
    }
    DisposableEffect(filePath.absolutePath) {
        onDispose { DesktopModPageLoader.release(filePath.absolutePath) }
    }

    val failure = result.exceptionOrNull()
    if (failure != null) {
        ModPageError(failure)
        return
    }

    val entry = result.getOrNull()!!
    val content = remember(entry, params) {
        val clazz = entry.clazz
        val instance = entry.instance
        runCatching {
            runCatching {
                clazz.getMethod("initialize", Map::class.java)
                    .invoke(instance, params)
            }
            // Prefer the Android-compatible field contract so one page JAR
            // works on every platform; fall back to the legacy Content(Map).
            val fieldContent = runCatching {
                clazz.getDeclaredField("params").apply { isAccessible = true }
                    .set(instance, params)
                clazz.getDeclaredField("Content").apply { isAccessible = true }
                    .get(instance)
            }.getOrNull()
            if (fieldContent != null) {
                @Suppress("UNCHECKED_CAST")
                fieldContent as @Composable () -> Unit
            } else {
                val method = clazz.getMethod("Content", Map::class.java)
                val legacy: @Composable () -> Unit = {
                    method.invoke(instance, params)
                }
                legacy
            }
        }.getOrElse {
            it.printStackTrace()
            null
        }
    }

    if (content != null) {
        content()
    } else {
        ModPageError(IllegalStateException("页面缺少 Content 可组合入口"))
    }

}

@Composable
private fun ModPageError(error: Throwable) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "页面加载失败: ${error.message ?: error::class.simpleName}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
