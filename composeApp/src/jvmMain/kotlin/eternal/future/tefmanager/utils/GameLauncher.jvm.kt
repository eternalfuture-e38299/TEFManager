package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.GameItem
import okio.Path.Companion.toPath
import java.io.File

actual object GameLauncher {

    actual fun launch(item: GameItem?, isServerMode: Boolean) {
        if (item == null) {
            AppLogger.e("GameItem is null")
            return
        }

        val gameExecutable = File(item.filePath)
        val gameDir = gameExecutable.parentFile
        if (!gameExecutable.isFile || gameDir == null || !gameDir.isDirectory) {
            AppLogger.e("Game executable does not exist: ${item.filePath}")
            return
        }

        val loaderExecutable = resolveLoaderExecutable(item, gameDir)
        if (loaderExecutable == null) {
            AppLogger.e("TEFLoader executable not found for ${item.filePath}")
            return
        }

        val kernelLibrary = resolveKernelLibrary(item.architecture)
        if (kernelLibrary == null) {
            AppLogger.e(
                "TefKernel library not found for architecture '${item.architecture}' in ${Platform.getData("tefkernel")}"
            )
            return
        }

        // 服务端模式需要启动 TerrariaServer.exe，而不是客户端 Terraria.exe。
        val targetGameExecutable = if (isServerMode) {
            File(gameDir, "TerrariaServer.exe").takeIf { it.isFile } ?: gameExecutable
        } else {
            gameExecutable
        }

        val command = mutableListOf(
            loaderExecutable.absolutePath,
            "-k", kernelLibrary.absolutePath,
            "-w", Platform.getData(null).toString(),
            "-e", targetGameExecutable.absolutePath
        )

        if (isServerMode)
            command.add("-server")

        AppLogger.i("Launching: ${command.joinToString(" ")}")

        try {
            ProcessBuilder(command)
                // tefloader 默认在当前目录查找 Terraria.exe / Content 等资源，
                // 因此必须把子进程工作目录设置为游戏目录，而不是 TEFManager 的启动目录。
                .directory(gameDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            AppLogger.i("Game launched successfully: $command")
        } catch (e: Exception) {
            AppLogger.e("Failed to launch game: ${e.message}", e)
        }
    }

    /**
     * 解析实际要启动的 TEFLoader。
     *
     * 旧版本在 Windows 上会把 GamePatcher 返回的 null 写成字符串 "null"，
     * 这里对这类历史数据做兼容：优先使用有效路径，否则在游戏目录中查找
     * 已解压的 tefloader.exe，最后再尝试重新解压一次。
     */
    private fun resolveLoaderExecutable(item: GameItem, gameDir: File): File? {
        val configuredPath = item.tefloaderPath.takeIf {
            it.isNotBlank() && !it.equals("null", ignoreCase = true)
        }

        if (configuredPath != null) {
            val configuredFile = File(configuredPath)
            if (configuredFile.isFile) return configuredFile
            AppLogger.w("Configured TEFLoader does not exist: $configuredPath")
        }

        val knownNames = listOf("tefloader.exe", "TefLoader.exe", "tefloader")
        knownNames.forEach { name ->
            val candidate = File(gameDir, name)
            if (candidate.isFile) return candidate
        }

        return try {
            val architecture = item.architecture.ifBlank { Platform.getArchitecture() }.lowercase()
            GamePatcher.patchViaDotNetGrafting(item.filePath.toPath(), "", architecture)
                ?.toString()
                ?.let { File(it) }
                ?.takeIf { it.isFile }
        } catch (e: Exception) {
            AppLogger.e("Failed to extract TEFLoader: ${e.message}", e)
            null
        }
    }

    /**
     * 解析 TefKernel 动态库。
     *
     * TEFManager 内置压缩包中的文件名带有 "lib" 前缀（如 libtefkernel.windows.x86.dll），
     * 而 Platform.getDynamicLibraryName 在 Windows 上生成的名字不带 "lib" 前缀。
     * 两种命名都需要兼容，否则会找不到内核库。
     */
    private fun resolveKernelLibrary(architecture: String): File? {
        val arch = architecture.ifBlank { Platform.getArchitecture() }.lowercase()
        val os = Platform.osName.lowercase()
        val kernelDir = File(Platform.getData("tefkernel").toString())

        val candidateNames = listOf(
            Platform.getDynamicLibraryName("tefkernel.$os.$arch"),
            "libtefkernel.$os.$arch${Platform.dynamicLibrarySuffix}",
            "tefkernel.$os.$arch${Platform.dynamicLibrarySuffix}"
        ).distinct()

        candidateNames.forEach { name ->
            val candidate = File(kernelDir, name)
            if (candidate.isFile) return candidate
        }

        // 大小写或前缀不完全一致时，按扩展名 / 名称模式兜底查找。
        val suffix = Platform.dynamicLibrarySuffix.lowercase()
        return kernelDir.listFiles()?.firstOrNull { file ->
            val name = file.name.lowercase()
            file.isFile &&
                name.endsWith(suffix) &&
                name.contains("tefkernel") &&
                name.contains(os) &&
                name.contains(arch)
        }
    }
}
