package eternal.future.tefmanager.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String): Boolean {
    try {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        val application = UIApplication.sharedApplication

        if (application.canOpenURL(nsUrl)) {
            application.openURL(nsUrl, emptyMap<Any?, Any>()) { success -> }
            return true
        }
    } catch (e: Exception) {
        AppLogger.e("Failed to open URL: $url", e)
    }

    return false
}