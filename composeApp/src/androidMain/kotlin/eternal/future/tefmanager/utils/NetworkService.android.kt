package eternal.future.tefmanager.utils

import android.content.Intent
import android.os.Bundle
import eternal.future.tefmanager.MainActivity
import androidx.core.net.toUri

actual fun openUrl(url: String): Boolean {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        MainActivity.context?.startActivity(browserIntent, Bundle())
        return true
    } catch (e: Exception) {
        AppLogger.e("Unable to open link: $url, error: ", e)
    }
    return false
}