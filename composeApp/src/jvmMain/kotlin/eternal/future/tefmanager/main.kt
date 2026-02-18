package eternal.future.tefmanager

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import eternal.future.tefmanager.ui.dialogs.UpdateDialogExample
import eternal.future.tefmanager.ui.screen.landscape.MainScreen
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun main() = application {

    AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())

    Window(
        onCloseRequest = ::exitApplication,
        title = "TEFManager"
    ) {
        TEFManagerTheme(
            darkTheme = true
        ) {
            Navigator(MainScreen)
        }
    }
}