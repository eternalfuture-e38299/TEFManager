package eternal.future.tefmanager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eternal.future.tefmanager.modpage.api.PageNode
import eternal.future.tefmanager.modpage.openModPageSession
import eternal.future.tefmanager.modpage.platformModPageContext
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path

class ModPageScreen(
    override val title: String,
    private val pagePath: String,
    private val pageClass: String,
    private val privatePath: String,
    private val dataPath: String,
) : Screen, PortraitMainScreen.TitledScreen, LandscapeMainScreen.TitledScreen {
    override val refreshAction: (() -> Unit)? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = platformModPageContext()
        val loaded = remember(pagePath, pageClass) {
            runCatching {
                val s = openModPageSession(pagePath, pageClass, privatePath, dataPath, context)
                s to s.render()
            }
        }
        val session = loaded.getOrNull()?.first
        var tree by remember(session) {
            mutableStateOf(loaded.getOrNull()?.second ?: PageNode("text"))
        }
        var pendingPickId by remember { mutableStateOf<String?>(null) }
        val filePicker = rememberFilePickerLauncher(
            mode = FileKitMode.Multiple(maxItems = 10),
        ) { files ->
            val id = pendingPickId
            val picked = session
            if (id != null && picked != null) {
                val value = files.orEmpty().joinToString("\n") { it.path }
                runCatching { picked.event(id, value) }
                    .onSuccess { tree = it }
                    .onFailure { it.printStackTrace() }
            }
            pendingPickId = null
        }
        DisposableEffect(session) {
            onDispose { session?.close() }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(session?.title?.ifEmpty { title } ?: title) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
        ) { innerPadding ->
            if (session == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        Strings.manager.mod.pageLoadFailed(
                            loaded.exceptionOrNull()?.message ?: Strings.error.unknown,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navigator.pop() }) { Text(Strings.manager.mod.pageBack) }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RenderNode(
                        node = tree,
                        event = { id, value ->
                            runCatching { session.event(id, value) }
                                .onSuccess { tree = it }
                                .onFailure { it.printStackTrace() }
                        },
                        pickFile = { id ->
                            pendingPickId = id
                            filePicker.launch()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderNode(
    node: PageNode,
    event: (String, String?) -> Unit,
    pickFile: (String) -> Unit,
) {
    when (node.type) {
        "column" -> Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            node.children.forEach { RenderNode(it, event, pickFile) }
        }

        "row" -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            node.children.forEach { RenderNode(it, event, pickFile) }
        }

        "text" -> Text(
            text = node.str("value", node.str("text")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        "button" -> Button(
            onClick = { event(node.str("id"), null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(node.str("label", Strings.manager.mod.pageDefaultButton))
        }

        "pickFile" -> Button(
            onClick = { pickFile(node.str("id")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(node.str("label", Strings.manager.mod.pageSelectFile))
        }

        "checkbox" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = node.bool("value"),
                onCheckedChange = { event(node.str("id"), it.toString()) },
            )
            Text(node.str("label"))
        }

        "slider" -> {
            val min = node.num("min", 0.0).toFloat()
            val max = node.num("max", 100.0).toFloat()
            val value = node.num("value", min.toDouble()).toFloat().coerceIn(min, max)
            Text("${node.str("label")}: ${value.toInt()}")
            androidx.compose.material3.Slider(
                value = value,
                valueRange = min..if (max == min) min + 1f else max,
                onValueChange = { event(node.str("id"), it.toString()) },
            )
        }

        "input" -> OutlinedTextField(
            value = node.str("value"),
            onValueChange = {},
            label = { Text(node.str("label")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        "separator" -> androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))

        "spacer" -> Spacer(Modifier.height(node.num("height", 12.0).dp))

        "progress" -> LinearProgressIndicator(
            progress = { node.num("value", 0.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        "collapsing" -> {
            Text(node.str("label"), style = MaterialTheme.typography.titleMedium)
            node.children.forEach { RenderNode(it, event, pickFile) }
        }
    }
}

private fun PageNode.str(key: String, fallback: String = ""): String =
    props[key]?.toString() ?: fallback

private fun PageNode.bool(key: String): Boolean = props[key] == true || props[key] == "true"

private fun PageNode.num(key: String, fallback: Double): Double {
    val value = props[key] ?: return fallback
    return (value as? Number)?.toDouble() ?: value.toString().toDoubleOrNull() ?: fallback
}
