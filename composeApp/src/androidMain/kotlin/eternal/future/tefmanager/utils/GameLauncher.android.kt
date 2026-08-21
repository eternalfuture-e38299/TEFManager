package eternal.future.tefmanager.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import eternal.future.tefmanager.FileProviderForegroundService
import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem

actual object GameLauncher {
    actual fun launch(item: GameItem?, isServerMode: Boolean) {
        if (item == null) {
            AppLogger.e("GameItem object is null, cannot launch application")
            return
        }

        AppLogger.d("Attempting to launch game: ${item.apkPackName} version ${item.version}")

        try {
            val context = MainActivity.context!!

/*
            if (BuildConfig.IS_INLINE_GAME) {
                AppLogger.d("Starting inline game launch process")

                val soFile = File(context.getExternalFilesDir(null), "tefkernel/libtefkernel.android.${Build.SUPPORTED_ABIS.first()}.so")
                val target = File(context.dataDir, "libtefkernel.so")

                val configFile = File(context.filesDir, "tefkernel_working_dir")
                if (!configFile.exists()) configFile.writeText(context.getExternalFilesDir(null)!!.absolutePath)

                // 复制文件
                soFile.copyTo(target, overwrite = true)

                // 设置 target 的权限（不是 soFile！）
                target.setExecutable(true)
                target.setWritable(false)  // 关键：设置为不可写

                // 现在可以安全加载
                System.load(target.absolutePath)

                // 加载完成后可以恢复权限（如果需要）
                // target.setWritable(true)

                val intent = Intent(context, UnityPlayerActivity::class.java)
                context.startActivity(intent)

                configFile.delete()
                return
            }
*/

            if (!Platform.isAndroidModuleActive) {
                AppLogger.d("Android module is not active, starting FileProviderForegroundService")

                // First stop the service if it's running
                try {
                    val stopIntent = Intent(context, FileProviderForegroundService::class.java)
                    context.stopService(stopIntent)
                    AppLogger.d("Successfully sent stop request to FileProviderForegroundService")
                } catch (e: Exception) {
                    AppLogger.w("Failed to stop FileProviderForegroundService: ${e.message}")
                    e.printStackTrace()
                    // Continue execution even if stop fails
                }

                // Delay to ensure service stops completely
                AppLogger.d("Waiting 200ms for service to stop completely")
                Thread.sleep(200)

                // Start the service
                val startIntent = Intent(context, FileProviderForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AppLogger.d("Android version >= Oreo (API 26), using startForegroundService")
                    context.startForegroundService(startIntent)
                } else {
                    AppLogger.d("Android version < Oreo, using startService")
                    context.startService(startIntent)
                }
                AppLogger.d("FileProviderForegroundService started successfully")
            } else {
                if (!PermissionManager.hasRootPermission()) {
                    AppLogger.w("Root permission not granted, showing dialog")
                    MainActivity.showNeedRootDialog.value = true
                    return
                }

                AppLogger.d("Android module is active, granting permissions to package: ${item.apkPackName}")
                PermissionManager.grantReadWriteAccess(context, item.apkPackName)
                AppLogger.d("Permissions granted successfully to package: ${item.apkPackName}")
            }

            // Get package manager
            val packageManager = context.packageManager
            AppLogger.d("Retrieved package manager, looking for launch intent")

            // Get launch Intent
            val launchIntent = packageManager.getLaunchIntentForPackage(item.apkPackName)

            if (launchIntent != null) {
                AppLogger.d("Launch intent found for package: ${item.apkPackName}")

                // Set intent flags
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                AppLogger.d("Intent flags set: FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP")

                // Start the activity
                context.startActivity(launchIntent)
                AppLogger.i("Game launched successfully: Package=${item.apkPackName}, Version=${item.version}")

            } else {
                AppLogger.e("No launchable activity found for package: ${item.apkPackName}. " +
                        "The package may not be installed or may not have a MAIN/LAUNCHER activity.")
            }

        } catch (e: SecurityException) {
            AppLogger.e("Security exception occurred while launching ${item.apkPackName}: " +
                    "Insufficient permissions to start activity. " +
                    "Error details: ${e.message}", e)

        } catch (e: android.content.ActivityNotFoundException) {
            AppLogger.e("Activity not found for package: ${item.apkPackName}. " +
                    "The application may be installed but the launch activity is missing or corrupted.", e)

        } catch (e: PackageManager.NameNotFoundException) {
            AppLogger.e("Package not found: ${item.apkPackName}. " +
                    "The application is not installed on this device.", e)

        } catch (e: IllegalStateException) {
            AppLogger.e("Illegal state exception while launching ${item.apkPackName}: " +
                    "The application may be in an invalid state. " +
                    "Error details: ${e.message}", e)

        } catch (e: InterruptedException) {
            AppLogger.e("Thread interrupted while waiting for service to stop: ${e.message}", e)
            Thread.currentThread().interrupt() // Restore interrupt flag

        } catch (e: Exception) {
            AppLogger.e("Unexpected error occurred while launching game: ${item.apkPackName}. " +
                    "Error type: ${e.javaClass.simpleName}, Message: ${e.message}", e)
        }
    }
}