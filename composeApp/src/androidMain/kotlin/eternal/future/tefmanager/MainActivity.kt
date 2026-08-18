package eternal.future.tefmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.ui.theme.TEFManagerTheme
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import eternal.future.tefmanager.ui.screen.landscape.MainScreen as LandscapeMainScreen
import eternal.future.tefmanager.ui.screen.landscape.OnboardingScreen as LandscapeOnboardingScreen
import eternal.future.tefmanager.ui.screen.portrait.MainScreen as PortraitMainScreen
import eternal.future.tefmanager.ui.screen.portrait.OnboardingScreen as PortraitOnboardingScreen

class MainActivity : ComponentActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Activity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context = this

        AppLogger.initializeSync(enableFileLog = true, logDir = Platform.getData("logs/app").toString())
        ConfigManager.getInstance().initialize(Platform.getData(null).toString())

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            TEFManagerTheme {
                var showInvalidDialog by remember { mutableStateOf(false) }

                if (BuildConfig.IS_INLINE_GAME) {
                    LaunchedEffect(Unit) {
                        if (!checkEnvironment()) showInvalidDialog = true
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showInvalidDialog) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(StringsResource.Strings.validation.dialog.title) },
                            text = {
                                Column {
                                    Text(StringsResource.Strings.validation.dialog.text1)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = StringsResource.Strings.validation.dialog.text2,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = StringsResource.Strings.validation.dialog.text3,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        finishAffinity()
                                    }
                                ) {
                                    Text(StringsResource.Strings.validation.dialog.confirm)
                                }
                            }
                        )
                    }

                    ScreenSwitcher()
                }
            }
        }
    }

    @Composable
    private fun ScreenSwitcher() {
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (!ConfigurationState.initialized) LandscapeOnboardingScreen { ConfigurationState.initialized = true }
                else LandscapeMainScreen.Content()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (!ConfigurationState.initialized) PortraitOnboardingScreen { ConfigurationState.initialized = true }
                else PortraitMainScreen.Content()
            }
            else -> {
                if (!ConfigurationState.initialized) PortraitOnboardingScreen { ConfigurationState.initialized = true }
                else PortraitMainScreen.Content()
            }
        }
    }

    private fun checkEnvironment(): Boolean {
        return if (!isTerrariaInstalled()) {
            isRunningInZhuoyitong() || isHarmonyOS()
        } else {
            true
        }
    }

    @SuppressLint("PrivateApi")
    private fun isHarmonyOS(): Boolean {
        // 检查系统属性
        return try {
            val prop = Class.forName("android.os.SystemProperties")
            val method = prop.getMethod("get", String::class.java)
            val version = method.invoke(null, "hw_sc.build.platform.version") as? String
            version?.contains("HarmonyOS") == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isRunningInZhuoyitong(): Boolean {
        return try {
            val cgroupInfo = java.io.BufferedReader(java.io.FileReader("/proc/self/cgroup"))
                .use { reader -> reader.readText() }

            // 检查是否包含 iSulad 容器的特征标识
            val keywords = listOf("isulad", "lxc", "zhuoyi")
            keywords.any { cgroupInfo.contains(it, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isTerrariaInstalled(): Boolean {
        return try {
            this.packageManager.getPackageInfo("com.and.games505.TerrariaPaid", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}