package eternal.future.tefmanager.utils

import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.Platform
import java.io.File

object ZygiskManager {

    const val MODULE_ID = "tefmanager"
    private const val MODULE_DIR = "/data/adb/modules/tefmanager"
    private const val MARKER_NAME = "zygisk_active"

    fun hasMarker(): Boolean {
        val dir = MainActivity.context?.getExternalFilesDir(null) ?: return false
        return File(dir, MARKER_NAME).isFile
    }

    fun verifyWithRoot(timeoutMs: Long = 3000L): Boolean {
        val command = "test -f " + MODULE_DIR + "/module.prop" +
                " && test -d " + MODULE_DIR + "/zygisk" +
                " && test ! -f " + MODULE_DIR + "/disable"
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    return process.exitValue() == 0
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(50)
                }
            }
            process.destroy()
            AppLogger.d("Zygisk module check timed out")
            false
        } catch (e: Exception) {
            AppLogger.d("Zygisk root check unavailable: " + e.message)
            false
        }
    }

    fun refreshActiveState(verify: Boolean = false): Boolean {
        val active = if (verify) {
            verifyWithRoot() || hasMarker()
        } else {
            hasMarker() || verifyWithRoot()
        }
        Platform.isAndroidModuleActive = active
        AppLogger.i("Zygisk module active: " + active)
        return active
    }
}
