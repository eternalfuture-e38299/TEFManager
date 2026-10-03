package eternal.future.tefmanager

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/*******************************************************************************
 * TEFManager - TEFManagerHook
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
 * Created: 2026/4/25
 *******************************************************************************/

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "TEFManagerHook"

        // 目标应用包名
        private val targetPackages = setOf(
            "com.and.games505.Terraria",
            "com.and.games505.TerrariaPaid"
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "eternal.future.tefmanager") {
            val platformClass = XposedHelpers.findClass(
                "eternal.future.tefmanager.Platform",
                lpparam.classLoader
            )

            // 修改静态字段
            XposedHelpers.setStaticBooleanField(platformClass, "isAndroidModuleActive", true)
            Log.i(TAG, "Loaded Module: ${lpparam.packageName}")
            return
        }


        if (!targetPackages.contains(lpparam.packageName)) {
            Log.d(TAG, "Skip application: ${lpparam.packageName}")
            return
        }

        Log.i(TAG, "Start hooking target app: ${lpparam.packageName}")

        try {
            hookApplicationStartup(lpparam)
        } catch (e: Throwable) {
            Log.e(TAG, "Initialization hook failed", e)
        }
    }

    /**
     * Hook应用启动
     */
    private fun hookApplicationStartup(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 先hook Tefloader.initTefKernel，优先级排到前面
            hookTefloaderInit(lpparam)

            // 然后再hook Application.onCreate
            hookApplicationOnCreate(lpparam)

        } catch (e: Throwable) {
            Log.w(TAG, "Hook Application.Startup Failed: ${e.message}")
        }
    }

    private fun hookTefloaderInit(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val tefloaderClass = XposedHelpers.findClass(
                "eternal.future.tefkernel.Tefloader",
                lpparam.classLoader
            )

            // 使用更早的hook优先级
            XposedHelpers.findAndHookMethod(
                tefloaderClass,
                "initTefKernel",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as Context
                        val packageName = context.packageName

                        Log.i(TAG, "Intercepting Tefloader.initTefKernel call - Package: $packageName")
                        Log.i(TAG, "Prevent Tefloader from initializing")

                        // 阻止原始方法执行
                        param.result = null
                    }
                }
            )

            Log.i(TAG, "Tefloader.initTefKernel hook succeeded - initialization blocked")

        } catch (e: Throwable) {
            Log.w(TAG, "Hook Tefloader.initTefKernel Failed: ${e.message}")
        }
    }

    private fun hookApplicationOnCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val applicationClass = XposedHelpers.findClass(
                "android.app.Application",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                applicationClass,
                "onCreate",
                object : XC_MethodHook() {
                    @SuppressLint("UnsafeDynamicallyLoadedCode")
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as Context
                        val packageName = context.packageName

                        Log.i(TAG, "Application.onCreate: $packageName")
                        val files = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "eternal.future.tefmanager/files")
                        val soFile = File(files, "tefkernel/libtefkernel.android.${detectCurrentProcessArchitecture()}.so")
                        val target = File(context.dataDir, "libtefkernel.so")

                        val configFile = File(context.filesDir, "tefkernel_working_dir")
                        if (!configFile.exists()) configFile.writeText(files.absolutePath)

                        soFile.copyTo(target, overwrite = true)
                        System.load(target.absolutePath)

                        configFile.delete()
                    }
                }
            )

            Log.i(TAG, "Application.onCreate hook succeeded")

        } catch (e: Throwable) {
            Log.w(TAG, "Hook Application.onCreate Failed: ${e.message}")
        }
    }

    fun detectCurrentProcessArchitecture(): String {
        var architecture = "arm64-v8a" // 默认值

        try {
            // 方法1: 读取当前进程的内存映射文件
            val process = Runtime.getRuntime().exec("cat /proc/self/maps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                if (line!!.contains("arm64-v8a")) {
                    architecture = "arm64-v8a"
                    break
                }

                if (line.contains("armeabi-v7a")) {
                    architecture = "armeabi-v7a"
                    break
                }
            }

            reader.close()
            process.waitFor()
        } catch (_: Exception) {
            // 方法2: 读取系统架构属性
            val osArch = System.getProperty("os.arch", "unknown")
            if (osArch != null) {
                if (!osArch.contains("aarch64") && !osArch.contains("arm64")) {
                    if (osArch.contains("arm")) {
                        architecture = "armeabi-v7a"
                    }
                } else {
                    architecture = "arm64-v8a"
                }
            }
        }

        return architecture
    }
}