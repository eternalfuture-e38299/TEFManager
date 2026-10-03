package eternal.future.tefmanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    return MaterialTheme.colorScheme
}