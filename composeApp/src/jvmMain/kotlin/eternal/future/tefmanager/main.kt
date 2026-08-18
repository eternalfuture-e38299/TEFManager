package eternal.future.tefmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import io.github.vinceglb.filekit.FileKit
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.landscape.OnboardingScreen as LandscapeOnboardingScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen
import eternal.future.tefmanager.ui.screen.portrait.OnboardingScreen as PortraitOnboardingScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
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
        state = windowState,
        undecorated = true,
        transparent = false,
        resizable = true,
    ) {
        val isLandscape = windowState.size.width > windowState.size.height

        LaunchedEffect(isLandscape) {
            AppLogger.d("Window orientation changed: ${if (isLandscape) "Landscape" else "Portrait"}")
        }

        TEFManagerTheme(
            themeMode = ConfigurationState.themeMode,
        ) {
            Column {
                WindowDraggableArea {
                    // 自定义标题栏
                    CustomTitleBar(
                        windowState = windowState,
                        onClose = ::exitApplication
                    )
                }

                // 主内容区域
                Box(modifier = Modifier.fillMaxSize()) {
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
    }
}

@Composable
private fun CustomTitleBar(
    windowState: WindowState,
    onClose: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEFManager",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // 窗口控制按钮（这些按钮区域不可拖动）
                Row {
                    IconButton(
                        onClick = { windowState.isMinimized = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HorizontalRule,
                            contentDescription = "Minimize",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            windowState.placement =
                                if (windowState.placement == WindowPlacement.Maximized) {
                                    WindowPlacement.Floating
                                } else {
                                    WindowPlacement.Maximized
                                }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "Maximize/Restore",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

    }
}