package eternal.future.tefmanager

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen

class MainActivity : ComponentActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context = this

        "".format()

        AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
        ConfigManager.getInstance().initialize(Platform.getData(null).toString())

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            TEFManagerTheme {
                ScreenSwitcher()
            }
        }
    }

    @Composable
    private fun ScreenSwitcher() {
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeMainScreen.Content()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                PortraitMainScreen.Content()
            }
            else -> {
                PortraitMainScreen.Content()
            }
        }
    }
}