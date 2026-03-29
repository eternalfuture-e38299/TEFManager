package eternal.future.tefmanager

import android.os.Build
import android.os.Environment
import co.touchlab.kermit.Logger
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import java.util.Locale

actual object Platform {
    actual val isAndroid: Boolean = true
    actual val isDesktop: Boolean = false
    actual val isIos: Boolean = false
    actual val isWeb: Boolean = false
    actual val isMobile: Boolean = true

    actual val osName: String = "Android"
    actual val osVersion: String? = Build.VERSION.RELEASE
    actual val deviceModel: String? = Build.MODEL

    val apiLevel: Int = Build.VERSION.SDK_INT
    val manufacturer: String = Build.MANUFACTURER
    val deviceBrand: String = Build.BRAND
    val deviceDisplay: String = Build.DISPLAY

    actual val isMacOS: Boolean = false
    actual val isLinux: Boolean = false
    actual val isWindows: Boolean = false

    actual fun getDirectory(type: String?): Path {
        return try {
            when (type?.lowercase()) {
                "documents" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path.toPath()
                "data" -> Environment.getDataDirectory().path.toPath()
                "tmp" -> MainActivity.context?.externalCacheDir?.absolutePath?.toPath()?.let { it / "tmp" } ?:
                MainActivity.context?.cacheDir?.let { (it.absolutePath + "/tmp").toPath() } ?:
                (File.createTempFile("temp", "").parent ?: "/tmp").toPath()
                else -> (Environment.getDataDirectory().path + "/" + (type ?: "")).toPath()
            }
        } catch (e: Exception) {
            Logger.e("getDirectory Failed ", e)
            "".toPath()
        }
    }

    actual fun getData(type: String?): Path {
        return MainActivity.context?.getExternalFilesDir(type)?.absolutePath?.toPath() ?:
        MainActivity.context?.dataDir?.let { (it.absolutePath + "/" + (type ?: "")).toPath() } ?: "".toPath()
    }

    actual val dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S


    actual fun getArchitecture(): String {
        return try {
            val abis = Build.SUPPORTED_ABIS
            val arch = abis[0]
            if (abis.isNotEmpty()) {
                when {
                    arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
                    arch.contains("x86") || arch.contains("i386") || arch.contains("i686") -> "x86"
                    arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
                    arch.contains("arm") -> "arm"
                    else -> "unknown"
                }
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            Logger.e("Failed to detect Android architecture", e)
            "unknown"
        }
    }

    actual val dynamicLibrarySuffix: String = ".so"
    actual fun getDynamicLibraryName(baseName: String): String = "lib${baseName}.so"

    actual val systemLanguage: String
        get() = Locale.getDefault().language

    actual val systemRegion: String
        get() = Locale.getDefault().country

    actual val systemLocale: String
        get() = "${Locale.getDefault().language}-${Locale.getDefault().country}"
}