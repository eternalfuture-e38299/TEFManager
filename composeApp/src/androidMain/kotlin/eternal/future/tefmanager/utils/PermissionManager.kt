package eternal.future.tefmanager.utils

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/*******************************************************************************
 * TEFManager - PermissionManager
 * Copyright (C) 2026 eternalfuture-e38299
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Author: eternalfuture-e38299
 * GitHub: https://github.com/eternalfuture-e38299
 * Created: 2026/4/26
 *******************************************************************************/


object PermissionManager {

    fun grantReadWriteAccess(context: Context, targetPackage: String) {
        val dirToShare = getTefManagerDirectory(context)

        if (!dirToShare.exists()) {
            dirToShare.mkdirs()
        }

        // 获取目标应用信息
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(targetPackage, 0)
        val targetUid = appInfo.uid
        val targetAppName = pm.getApplicationLabel(appInfo).toString()

        // 执行root权限命令修改权限
        grantAccessWithRoot(dirToShare, targetUid)

        AppLogger.i("Successfully granted permissions to the application: $targetAppName ($targetPackage) UID: $targetUid")
    }

    private fun getTefManagerDirectory(context: Context): File {
        return File(
            context.getExternalFilesDir(null)?.parentFile?.parentFile,
            "eternal.future.tefmanager"
        )
    }

    private fun executeRootCommand(cmd: String): Boolean {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw Exception("Root command execution failed (exit code: $exitCode): $cmd")
        }
        return true
    }

    private fun grantAccessWithRoot(dir: File, targetUid: Int) {
        val dirPath = dir.absolutePath

        executeRootCommand("chown -R :$targetUid \"$dirPath\"")
        executeRootCommand("chmod -R 2770 \"$dirPath\"")  // owner:rwx group:rwx other:---
    }

    /**
     * 检查是否有 Root 权限
     */
    fun hasRootPermission(): Boolean {
        return try {
            // 尝试执行 su 命令
            val process = Runtime.getRuntime().exec("su -c echo test")
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AppLogger.d("Root permission check: granted")
                return true
            }

            AppLogger.d("Root permission check: denied (exit code: $exitCode)")
            false
        } catch (e: Exception) {
            AppLogger.d("Root permission check: failed - ${e.message}")
            false
        }
    }
}
