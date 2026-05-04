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

    /**
     * 授予指定包名对 TEFManager 目录的读写权限
     *
     * @param context Android 上下文
     * @param targetPackage 目标应用包名
     * @throws Exception 任意一步失败直接抛异常
     */
    fun grantReadWriteAccess(context: Context, targetPackage: String) {
        val dirToShare = getTefManagerDirectory(context)

        if (!dirToShare.exists() && !dirToShare.mkdirs()) {
            throw Exception("Failed to create TEFManager directory")
        }

        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(targetPackage, PackageManager.GET_META_DATA)
        val targetUid = appInfo.uid
        val targetAppName = pm.getApplicationLabel(appInfo).toString()

        grantAccessWithRoot(dirToShare.absolutePath, targetUid)

        AppLogger.i(
            "Granted RW access to $targetAppName ($targetPackage), UID=$targetUid"
        )
    }

    private fun getTefManagerDirectory(context: Context): File {
        return File(
            context.getExternalFilesDir(null)
                ?.parentFile
                ?.parentFile,
            "eternal.future.tefmanager"
        )
    }

    /**
     * 使用 root + ACL 授权，不修改 owner
     */
    private fun grantAccessWithRoot(dirPath: String, targetUid: Int) {
        // 确保目录存在
        executeRootCommand("mkdir -p \"$dirPath\"")

        // 使用 ACL 精确授权目标 UID
        executeRootCommand("setfacl -m u:$targetUid:rwx \"$dirPath\"")
        executeRootCommand("find \"$dirPath\" -exec setfacl -m u:$targetUid:rwx {} \\;")
    }

    private fun executeRootCommand(cmd: String) {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw Exception("Root command failed (code=$exitCode): $cmd")
        }
    }
}