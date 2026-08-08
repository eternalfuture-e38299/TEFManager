package eternal.future.tefmanager

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import io.github.vinceglb.filekit.FileKit
import eternal.future.tefmanager.ui.screen.portrait.OnboardingScreen as PortraitOnboardingScreen
import eternal.future.tefmanager.ui.screen.landscape.OnboardingScreen as LandscapeOnboardingScreen
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun main() = application {
    FileKit.init("tefmanager")
    AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
    ConfigManager.getInstance().initialize(Platform.getData(null).toString())

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "TEFManager",
        state = windowState
    ) {
        // 使用 windowState.size 检测窗口尺寸变化
        val isLandscape = windowState.size.width > windowState.size.height

        // 记录日志便于调试
        LaunchedEffect(isLandscape) {
            AppLogger.d("Window orientation changed: ${if (isLandscape) "Landscape" else "Portrait"}")
        }

        TEFManagerTheme(
            themeMode = ConfigurationState.themeMode,
        ) {
            // 根据方向切换屏幕，使用 key 确保完全重组
            key(isLandscape) {
                if (isLandscape) {
                    if (!ConfigurationState.initialized) LandscapeOnboardingScreen { ConfigurationState.initialized = true }
                    else LandscapeMainScreen.Content()
                } else {
                    if (!ConfigurationState.initialized) PortraitOnboardingScreen { ConfigurationState.initialized = true }
                    else PortraitMainScreen.Content()
                }
            }
        }
    }
}