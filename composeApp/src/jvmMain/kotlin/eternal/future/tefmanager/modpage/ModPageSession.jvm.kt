package eternal.future.tefmanager.modpage

import androidx.compose.runtime.Composable
import eternal.future.tefmanager.modpage.api.ModPage
import eternal.future.tefmanager.modpage.api.ModPageApi
import eternal.future.tefmanager.modpage.api.PageNode
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLClassLoader

private class JvmModPageSession(
    jarPath: String,
    className: String,
    private val modPrivatePath: String,
    private val modDataPath: String,
) : ModPageSession {
    private val loader: URLClassLoader
    private val page: ModPage
    private val state = ModPageState(modPrivatePath)
    override val title: String

    init {
        val file = File(jarPath)
        require(file.exists()) { "页面文件不存在: $jarPath" }
        loader = URLClassLoader(
            arrayOf(file.toURI().toURL()),
            JvmModPageSession::class.java.classLoader,
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
        override fun state(key: String): String? = this@JvmModPageSession.state.get(key)
        override fun setState(key: String, value: String) =
            this@JvmModPageSession.state.set(key, value)
        override fun openUrl(url: String) {
            runCatching {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        }
        override fun close() {}
    }

    override fun render(): PageNode = page.render(api())
    override fun event(id: String, value: String?): PageNode {
        page.onEvent(id, value, api())
        return render()
    }
    override fun close() {
        runCatching { loader.close() }
    }
}

actual fun openModPageSession(
    jarPath: String,
    className: String,
    privatePath: String,
    dataPath: String,
    hostContext: Any?,
): ModPageSession = JvmModPageSession(jarPath, className, privatePath, dataPath)

@Composable
actual fun platformModPageContext(): Any? = null
