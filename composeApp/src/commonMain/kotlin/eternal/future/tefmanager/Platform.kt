package eternal.future.tefmanager

import okio.Path

expect object Platform {

    val isAndroid: Boolean
    val isDesktop: Boolean
    val isIos: Boolean
    val isWeb: Boolean

    val dynamicColor : Boolean

    val isMacOS: Boolean
    val isLinux: Boolean
    val isWindows: Boolean

    val isMobile: Boolean
    val osName: String
    val osVersion: String?
    val deviceModel: String?
    val dynamicLibrarySuffix: String

    val systemLanguage: String
    val systemRegion: String
    val systemLocale: String

    fun getDynamicLibraryName(baseName: String): String
    fun getDirectory(type: String?): Path?
    fun getData(type: String?): Path
    fun getArchitecture() : String
}