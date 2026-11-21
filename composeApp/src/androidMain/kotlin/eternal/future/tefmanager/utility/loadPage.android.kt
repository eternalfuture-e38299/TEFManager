package eternal.future.tefmanager.utility

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileNotFoundException

data class ModPageEntry(
    val loader: DexClassLoader,
    val cacheFile: File,
    val dexDir: File,
    val clazz: Class<*>,
    val instance: Any,
)

/** Cached, reusable class loaders for mod pages. One entry per page JAR. */
object ModPageLoader {
    private val entries = mutableMapOf<String, ModPageEntry>()

    @Synchronized
    fun load(context: Context, filePath: String, className: String): Result<ModPageEntry> {
        entries[filePath]?.let { return Result.success(it) }
        return runCatching {
            val original = File(filePath)
            if (!original.exists()) throw FileNotFoundException("页面文件不存在: $filePath")

            val cacheFile = File(
                context.cacheDir,
                "mod_page_${System.currentTimeMillis()}_${original.name}.jar",
            )
            original.inputStream().use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            cacheFile.setReadOnly()

            val dexDir = File(context.cacheDir, "mod_page_dex_${System.currentTimeMillis()}")
            dexDir.mkdirs()

            // Parent must be the app classloader so the mod page can see Compose.
            val loader = DexClassLoader(
                cacheFile.absolutePath,
                dexDir.absolutePath,
                null,
                ModPageLoader::class.java.classLoader,
            )
            val clazz = loader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance()
            val entry = ModPageEntry(loader, cacheFile, dexDir, clazz, instance)
            entries[filePath] = entry
            entry
        }
    }

    @Synchronized
    fun release(filePath: String) {
        val entry = entries.remove(filePath) ?: return
        runCatching {
            entry.cacheFile.delete()
            entry.dexDir.deleteRecursively()
        }
    }
}

@Composable
actual fun LoadAndDisplayPage(
    filePath: File,
    className: String,
    params: Map<String, *>,
) {
    val context = LocalContext.current
    val key = filePath.absolutePath
    val result = remember(key, className) { ModPageLoader.load(context, key, className) }
    DisposableEffect(key) {
        onDispose { ModPageLoader.release(key) }
    }

    val failure = result.exceptionOrNull()
    if (failure != null) {
        ModPageError(failure)
        return
    }

    val entry = result.getOrNull()!!
    val content: (@Composable () -> Unit)? = remember(key, params) {
        runCatching {
            val clazz = entry.clazz
            val instance = entry.instance
            runCatching {
                clazz.getMethod("initialize", Map::class.java).invoke(instance, params)
            }
            runCatching {
                clazz.getDeclaredField("params").apply { isAccessible = true }
                    .set(instance, params)
            }
            @Suppress("UNCHECKED_CAST")
            clazz.getDeclaredField("Content").apply { isAccessible = true }
                .get(instance) as @Composable () -> Unit
        }.getOrElse {
            it.printStackTrace()
            null
        }
    }

    if (content != null) {
        content()
    } else {
        ModPageError(IllegalStateException("页面缺少 Content 可组合字段"))
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
