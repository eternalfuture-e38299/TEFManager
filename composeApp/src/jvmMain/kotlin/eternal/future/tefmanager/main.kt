package eternal.future.tefmanager

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import eternal.future.tefmanager.ui.component.ModuleItemCard
import eternal.future.tefmanager.ui.model.ModuleItem
import eternal.future.tefmanager.ui.screen.landscape.MainScreen
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.FileKit.appId
import okio.Path.Companion.toPath

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun main() = application {
    FileKit.init("tefmanager")
    AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
    ConfigManager.getInstance().initialize(Platform.getData(null).toString())

    Window(
        onCloseRequest = ::exitApplication,
        title = "TEFManager",
        state = rememberWindowState(
            placement = WindowPlacement.Maximized
        )
    ) {
        TEFManagerTheme(
            themeMode = ConfigurationState.themeMode,
        ) {
            Navigator(MainScreen)
        }
    }
}