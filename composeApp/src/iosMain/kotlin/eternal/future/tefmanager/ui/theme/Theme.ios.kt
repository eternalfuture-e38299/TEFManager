package eternal.future.tefmanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import io.ktor.client.request.invoke
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.systemBackgroundColor

@OptIn(ExperimentalForeignApi::class)
private fun UIColor.toComposeColor(): Color {
    val red = this.CIColor.red.toFloat()
    val green = this.CIColor.green.toFloat()
    val blue = this.CIColor.blue.toFloat()
    val alpha = this.CIColor.alpha.toFloat()

    return Color(
        red = red,
        green = green,
        blue = blue,
        alpha = alpha
    )
}

@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    val traitCollection = if (darkTheme) {
        UITraitCollection.traitCollectionWithUserInterfaceStyle(UIUserInterfaceStyle.UIUserInterfaceStyleDark)
    } else {
        UITraitCollection.traitCollectionWithUserInterfaceStyle(UIUserInterfaceStyle.UIUserInterfaceStyleLight)
    }
    val themeColor = UIColor.systemBackgroundColor().resolvedColorWithTraitCollection(traitCollection).toComposeColor()
    return rememberDynamicColorScheme(themeColor, true)
}