package eternal.future.tefmanager.modpage

import android.content.Context
import androidx.compose.runtime.Composable
import android.content.Intent
import android.net.Uri
import dalvik.system.DexClassLoader
import eternal.future.tefmanager.modpage.api.ModPage
import eternal.future.tefmanager.modpage.api.ModPageApi
import eternal.future.tefmanager.modpage.api.PageNode
import java.io.File

private class AndroidModPageSession(
    jarPath: String,
    className: String,
    private val modPrivatePath: String,
    private val modDataPath: String,
    context: Context,
) : ModPageSession {
    private val cacheFile: File
    private val dexDir: File
    private val loader: DexClassLoader
    private val page: ModPage
    private val state = ModPageState(modPrivatePath)
    private val hostContext = context

    override val title: String

    init {
        val original = File(jarPath)
        require(original.exists()) { "页面文件不存在: $jarPath" }
        cacheFile = File(hostContext.cacheDir, "modpage_${System.currentTimeMillis()}.jar")
        original.copyTo(cacheFile, overwrite = true)
        cacheFile.setReadOnly()
        dexDir = File(hostContext.cacheDir, "modpage_dex_${System.currentTimeMillis()}")
        dexDir.mkdirs()
        loader = DexClassLoader(
            cacheFile.absolutePath,
            dexDir.absolutePath,
            null,
            AndroidModPageSession::class.java.classLoader,
        )
        val clazz = loader.loadClass(className)
        require(ModPage::class.java.isAssignableFrom(clazz)) {
            "$className 未实现稳定的 ModPage 接口"
        }
        page = clazz.getDeclaredConstructor().newInstance() as ModPage
        title = page.title()
    }

    private fun api() = object : ModPageApi {
        override val privatePath: String get() = modPrivatePath
        override val dataPath: String get() = modDataPath
        override fun state(key: String): String? = this@AndroidModPageSession.state.get(key)
        override fun setState(key: String, value: String) =
            this@AndroidModPageSession.state.set(key, value)
        override fun openUrl(url: String) {
            hostContext.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        override fun close() {}
    }

    override fun render(): PageNode = page.render(api())
    override fun event(id: String, value: String?): PageNode {
        page.onEvent(id, value, api())
        return render()
    }

    override fun close() {
        runCatching { cacheFile.delete() }
        runCatching { dexDir.deleteRecursively() }
    }
}

actual fun openModPageSession(
    jarPath: String,
    className: String,
    privatePath: String,
    dataPath: String,
    hostContext: Any?,
): ModPageSession {
    require(hostContext is Context) { "Android 页面需要 Context" }
    return AndroidModPageSession(jarPath, className, privatePath, dataPath, hostContext)
}

@Composable
actual fun platformModPageContext(): Any? = androidx.compose.ui.platform.LocalContext.current
