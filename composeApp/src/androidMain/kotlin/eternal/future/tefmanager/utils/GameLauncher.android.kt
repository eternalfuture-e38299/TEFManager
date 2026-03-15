package eternal.future.tefmanager.utils

import android.content.Intent
import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.ui.model.GameItem

actual object GameLauncher {
    actual fun launch(item: GameItem?) {
        if (item == null) {
            AppLogger.e("Application is empty")
            return
        }

        try {
            val context = MainActivity.context ?: return

            // 获取包管理器
            val packageManager = context.packageManager

            // 获取启动Intent
            val launchIntent = packageManager.getLaunchIntentForPackage(item.apkPackName)

            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(launchIntent)
                AppLogger.i("Start Game: ${item.apkPackName} (v${item.version})")

            }

        } catch (e: SecurityException) {
            AppLogger.e("Insufficient privileges to start: ${item.apkPackName}", e)

        } catch (e: Exception) {
            AppLogger.e("Failed to start the game: ${item.apkPackName}", e)
        }
    }
}