package eternal.future.tefmanager

import co.touchlab.kermit.Logger
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages
import platform.UIKit.UIDevice
import platform.darwin.*

actual object Platform {
    actual val isAndroid: Boolean = false
    actual val isDesktop: Boolean = false
    actual val isIos: Boolean = true
    actual val isWeb: Boolean = false
    actual val isMobile: Boolean = true

    actual val osName: String = "iOS"
    actual val osVersion: String?
    actual val deviceModel: String?

    init {
        val device = UIDevice.currentDevice
        osVersion = device.systemVersion
        deviceModel = device.model
    }

    actual val isMacOS: Boolean = false
    actual val isLinux: Boolean = false
    actual val isWindows: Boolean = false

    actual fun getDirectory(type: String?): Path {
        return try {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = NSDocumentDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )

            if (paths.isNotEmpty()) {
                val documentsPath = (paths.first() as String).toPath()
                when (type?.lowercase()) {
                    "documents" -> documentsPath
                    "library" -> documentsPath / "../Library"
                    "tmp" -> documentsPath / "../tmp"
                    else -> documentsPath / (type ?: "")
                }
            } else {
                "".toPath()
            }
        } catch (e: Exception) {
            Logger.e("getDirectory Failed ", e)
            "".toPath()
        }
    }

    actual fun getData(type: String?): Path {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )

        if (paths.isNotEmpty()) {
            val documentsPath = paths.first() as String
            val basePath = documentsPath.toPath()

            return when (type?.lowercase()) {
                null -> basePath
                else -> basePath / type
            }
        }

        return "".toPath()
    }

    actual val dynamicColor: Boolean = true // iOS 13+ 支持动态颜色

    actual fun getArchitecture(): String {
        return try {
            // 使用Darwin系统常量检测架构
            when {
                // 检测ARM64架构
                CPU_ARCH_ABI64 != 0 && CPU_ARCH_ABI64 != 0 -> "arm64"

                // 检测ARM架构
                CPU_TYPE_ARM != 0 -> "arm"

                // 检测x86_64架构
                CPU_TYPE_X86_64 != 0 -> "x86_64"

                // 检测x86架构
                CPU_TYPE_X86 != 0 -> "x86"

                // 其他情况
                else -> {
                    // 备用方案：通过常量组合检测
                    when {
                        (CPU_TYPE_ARM64 != 0) -> "arm64"
                        (CPU_TYPE_ARM != 0) -> "arm"
                        (CPU_TYPE_X86_64 != 0) -> "x86_64"
                        (CPU_TYPE_X86 != 0) -> "x86"
                        else -> "unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to detect iOS architecture using Darwin constants", e)
            "unknown"
        }
    }

    actual val dynamicLibrarySuffix: String = ".dylib"
    actual fun getDynamicLibraryName(baseName: String): String = "lib${baseName}.dylib"

    actual val systemLanguage: String
        get() {
            val languages = NSLocale.preferredLanguages
            return if (languages.isNotEmpty()) {
                val langCode = languages[0] as String
                // 处理格式如 "zh-Hans" -> 返回 "zh"
                langCode.substringBefore("-")
            } else {
                "en" // 默认值
            }
        }

    actual val systemRegion: String
        get() = NSLocale.currentLocale.countryCode ?: "US"

    actual val systemLocale: String
        get() = "$systemLanguage-$systemRegion"
}