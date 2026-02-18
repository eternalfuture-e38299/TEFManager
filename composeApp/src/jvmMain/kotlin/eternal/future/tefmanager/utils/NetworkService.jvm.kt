package eternal.future.tefmanager.utils

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String): Boolean {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            val uri = URI(url)
            Desktop.getDesktop().browse(uri)
            return true
        } else {
            return openUrlWithCommandLine(url)
        }
    } catch (e: Exception) {
        AppLogger.e("Failed to open URL: $url", e)
    }
    return false
}

private fun openUrlWithCommandLine(url: String): Boolean {
    return try {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
            os.contains("mac") -> arrayOf("open", url)
            else -> arrayOf("xdg-open", url) // Linux
        }

        Runtime.getRuntime().exec(command).waitFor() == 0
    } catch (e: Exception) {
        AppLogger.e("Unable to open URL via command line: $url", e)
        false
    }
}