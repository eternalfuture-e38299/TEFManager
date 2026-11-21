package eternal.future.tefmanager.modpage.api

/**
 * Stable, Compose-free ABI for mod-provided pages.
 *
 * Mods compile against a frozen copy of these declarations (compileOnly).
 * The manager implements them at runtime and renders the returned [PageNode]
 * tree with whatever UI toolkit it uses, so manager upgrades never require
 * rebuilding mod pages.
 */
data class PageNode(
    val type: String,
    val props: Map<String, Any?> = emptyMap(),
    val children: List<PageNode> = emptyList(),
)

interface ModPageApi {
    val privatePath: String
    val dataPath: String

    fun state(key: String): String?
    fun setState(key: String, value: String)
    fun openUrl(url: String)
    fun close()
}

interface ModPage {
    fun title(): String = "ModPage"
    fun render(api: ModPageApi): PageNode
    fun onEvent(id: String, value: String?, api: ModPageApi) {}
}
