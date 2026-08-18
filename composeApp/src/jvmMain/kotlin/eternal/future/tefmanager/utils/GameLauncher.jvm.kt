package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem

actual object GameLauncher {

    actual fun launch(item: GameItem?, isServerMode: Boolean) {
        if (item == null) {
            AppLogger.e("GameItem is null")
            return
        }

        val command = mutableListOf(
            item.tefloaderPath,
            "-k", (Platform.getData("tefkernel") / Platform.getDynamicLibraryName("tefkernel.${Platform.osName.lowercase()}.${item.architecture.lowercase()}")).toString(),
            "-w", (Platform.getData(null)).toString()
        )

        if (isServerMode)
            command.add("-server")

        AppLogger.i("Launching: ${command.joinToString(" ")}")

        try {
            val processBuilder = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)

            // 启动进程但不等待
            processBuilder.start()

            // 分离进程，不调用 waitFor()
            // 在 JVM 退出时子进程可以继续运行
            processBuilder.redirectErrorStream()

            AppLogger.i("Game launched successfully: $command")
        } catch (e: Exception) {
            AppLogger.e("Failed to launch game: ${e.message}")
        }
    }
}