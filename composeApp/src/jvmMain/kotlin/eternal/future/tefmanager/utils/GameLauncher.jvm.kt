package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem

actual object GameLauncher {

    actual fun launch(item: GameItem?, isServerMode: Boolean) {
        if (item == null) {
            AppLogger.e("GameItem is null")
            return
        }

        val executablePath = buildExecutablePath(item.tefloaderPath, item.architecture)

        val command = mutableListOf(
            executablePath,
            "-k", (Platform.getData("tefkernel") / Platform.getDynamicLibraryName("tefkernel.${Platform.osName.lowercase()}.${item.architecture.lowercase()}")).toString(),
            "-w", (Platform.getData(null)).toString()
        )

        if (isServerMode)
            command.add("-server")

        AppLogger.i("Launching: ${command.joinToString(" ")}")

        try {
            ProcessBuilder(command)
                .inheritIO()
                .start()
            AppLogger.i("Game launched successfully: $command")
        } catch (e: Exception) {
            AppLogger.e("Failed to launch game: ${e.message}")
        }
    }

    private fun buildExecutablePath(basePath: String, architecture: String): String {
        val withoutExe = if (basePath.endsWith(".exe")) {
            basePath.substring(0, basePath.length - 4)
        } else {
            basePath
        }

        val arch = if (architecture.isNotEmpty()) {
            when (architecture.lowercase()) {
                "arm64-v8a", "arm64" -> "arm64"
                "armeabi-v7a", "armv7a" -> "armv7"
                "x86_64", "amd64" -> "x86_64"
                "x86" -> "x86"
                else -> architecture
            }
        } else {
            Platform.getArchitecture()
        }

        return if (Platform.isWindows) {
            "$withoutExe.exe"
        } else {
            "$withoutExe.bin.$arch"
        }
    }
}