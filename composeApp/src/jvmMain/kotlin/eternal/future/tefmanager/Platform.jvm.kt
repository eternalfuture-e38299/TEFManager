package eternal.future.tefmanager

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath
import java.lang.System.getProperty
import java.util.Locale

actual object Platform {
    private val osNameRaw = getProperty("os.name", "Unknown")

    actual val isAndroid: Boolean = false
    actual val isDesktop: Boolean = true
    actual val isIos: Boolean = false
    actual val isWeb: Boolean = false

    actual val isMacOS: Boolean = osNameRaw.contains("mac", ignoreCase = true)
    actual val isLinux: Boolean = osNameRaw.contains("linux", ignoreCase = true)
    actual val isWindows: Boolean = osNameRaw.contains("win", ignoreCase = true)

    actual val isMobile: Boolean = false
    actual val osName: String = when {
        isMacOS -> "macOS"
        isLinux -> detectLinuxDistribution() ?: "Linux"
        isWindows -> "Windows"
        else -> osNameRaw
    }
    actual val osVersion: String? = getProperty("os.version")
    actual val deviceModel: String? = getProperty("user.name")?.let { "$it's PC" }

    private fun detectLinuxDistribution(): String? {
        return try {
            val osRelease = java.io.File("/etc/os-release")
            if (osRelease.exists()) {
                osRelease.useLines { lines ->
                    lines.find { it.startsWith("PRETTY_NAME=") }
                        ?.substringAfter("=")
                        ?.trim('"')
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    actual fun getDirectory(type: String?): Path? {
        return try {
            when (type?.lowercase()) {
                "documents" -> getDocumentsDirectory()
                "data" -> getDataDirectory()
                "tmp" -> getTempDirectory()
                else -> getCustomDirectory(type ?: "")
            }
        } catch (e: Exception) {
            Logger.e("getDirectory Failed ", e)
            null
        }
    }

    private fun getDocumentsDirectory(): Path? {
        return when {
            isWindows -> (System.getenv("USERPROFILE") ?: "C:").toPath() / "Documents"
            isMacOS -> (getProperty("user.home") ?: "~").toPath() / "Documents"
            isLinux -> (getProperty("user.home") ?: "~").toPath() / "Documents"
            else -> (getProperty("user.home") ?: "~").toPath()
        }
    }

    private fun getDataDirectory(): Path? {
        return when {
            isWindows -> (System.getenv("APPDATA") ?: ((System.getenv("USERPROFILE")
                ?: "C:") + "\\AppData\\Roaming")).toPath()
            isMacOS -> (getProperty("user.home") ?: "~").toPath() / "Library" / "Application Support"
            isLinux -> (getProperty("user.home") ?: "~").toPath() / ".local" / "share"
            else -> (getProperty("user.home") ?: "~").toPath()
        }
    }

    private fun getTempDirectory(): Path {
        return (getProperty("java.io.tmpdir") ?: "/tmp").toPath()
    }

    private fun getCustomDirectory(type: String): Path {
        return (getProperty("user.home") ?: "~").toPath() / type
    }

    actual fun getData(type: String?): Path {
        val p = FileKit.filesDir.path.toPath()
        type?.let {
            return p / type
        }
        return p
    }

    actual val dynamicColor: Boolean = false

    actual fun getArchitecture(): String {
        return try {
            val arch = getProperty("os.arch", "").lowercase()

            when {
                arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
                arch.contains("x86") || arch.contains("i386") || arch.contains("i686") -> "x86"
                arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
                arch.contains("arm") -> "arm"
                else -> "unknown"
            }
        } catch (e: Exception) {
            Logger.e("Failed to detect architecture", e)
            "unknown"
        }
    }

    actual val dynamicLibrarySuffix: String = when {
        isWindows -> ".dll"
        isMacOS -> ".dylib"
        isLinux -> ".so"
        else -> ".so"
    }

    actual fun getDynamicLibraryName(baseName: String): String = when {
        isWindows -> "$baseName.dll"
        else -> "lib$baseName${dynamicLibrarySuffix}"
    }

    actual val systemLanguage: String
        get() = Locale.getDefault().language

    actual val systemRegion: String
        get() = Locale.getDefault().country

    actual val systemLocale: String
        get() = "${Locale.getDefault().language}-${Locale.getDefault().country}"
}