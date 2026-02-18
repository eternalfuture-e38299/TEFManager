package eternal.future.tefmanager

import androidx.compose.ui.window.ComposeUIViewController
import eternal.future.tefmanager.ui.screen.portrait.MainScreen


fun MainViewController() = ComposeUIViewController {
    MainScreen.Content()
}