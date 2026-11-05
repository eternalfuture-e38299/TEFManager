package eternal.future.tefmanager.utils

import android.content.Context
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

    @Suppress("unused")
    fun grantReadWriteAccess(context: Context, targetPackage: String) {
        val appInfo = context.packageManager.getApplicationInfo(targetPackage, 0)

        // 只给游戏访问它真正需要的运行目录，绝不再递归授权整个 TEFManager 外部目录。
        // - tefkernel：保持 App 私有（root companion 读取），游戏/模组不可改
        // - logs/tefkernel：内核日志
        // - modloader：模组目录
        // - module：模块私有目录
        val extRoot = context.getExternalFilesDir(null) ?: return
        val parent = extRoot.parentFile ?: return
        val appUid = context.applicationInfo.uid
        val targetUid = appInfo.uid

        File(extRoot, "logs/tefkernel").mkdirs()
        File(extRoot, "modloader").mkdirs()
        File(extRoot, "module").mkdirs()
        File(extRoot, "mods").mkdirs()
        File(extRoot, "plugin").mkdirs()

        // 顶层文件（config.json、下载包等）只允许 TEFManager 自己读写。
        extRoot.listFiles()?.forEach { entry ->
            if (entry.isFile) {
                entry.setReadable(false, false)
                entry.setReadable(true, true)
                entry.setWritable(false, false)
                entry.setWritable(true, true)
                entry.setExecutable(false, false)
            }
        }

        val root = extRoot.absolutePath
        val shell = buildString {
            // 父目录和 files 的 group 必须切到游戏 UID，否则游戏连 files 都进不去。
            append("chown ").append(appUid).append(':').append(targetUid)
            append(" \"").append(parent.absolutePath).append("\" \"").append(root).append("\"; ")
            // Android FUSE 下，files 需要 group 的读+执行权限才能正常解析子目录；
            // group 仍然没有写权限，不能改目录内容。
            append("chmod 0750 \"").append(parent.absolutePath).append("\"; ")
            append("chmod 0750 \"").append(root).append("\"; ")

            // 盘符/日志/模组目录按需授予游戏 UID
            append("mkdir -p \"").append(root).append("/logs/tefkernel\" ")
            append("\"").append(root).append("/modloader\" ")
            append("\"").append(root).append("/module\" ")
            append("\"").append(root).append("/mods\" ")
            append("\"").append(root).append("/plugin\"; ")

            append("chown -R :").append(targetUid).append(" ")
            append("\"").append(root).append("/logs\" ")
            append("\"").append(root).append("/modloader\" ")
            append("\"").append(root).append("/module\" ")
            append("\"").append(root).append("/mods\" ")
            append("\"").append(root).append("/plugin\"; ")
            append("chmod -R 2770 ")
            append("\"").append(root).append("/logs\" ")
            append("\"").append(root).append("/modloader\" ")
            append("\"").append(root).append("/module\" ")
            append("\"").append(root).append("/mods\" ")
            append("\"").append(root).append("/plugin\"; ")

            // App 日志不暴露给游戏
            if (File(extRoot, "logs/app").exists()) {
                append("chmod -R 0700 \"").append(root).append("/logs/app\"; ")
            }
        }
        executeRootCommand(shell)

        // 内核文件始终由 App 独占，root companion 以 root 读取。
        val kernelDir = File(extRoot, "tefkernel")
        if (kernelDir.exists()) {
            executeRootCommand(
                "chmod 0700 \"${kernelDir.absolutePath}\" && " +
                        "chmod -R 0700 \"${kernelDir.absolutePath}\""
            )
        }

        AppLogger.i(
            "Granted scoped storage access to ${appInfo.packageName} (uid $targetUid); " +
                    "kernel dir remains app-private"
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
