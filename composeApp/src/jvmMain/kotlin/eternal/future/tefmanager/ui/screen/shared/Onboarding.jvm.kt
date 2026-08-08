package eternal.future.tefmanager.ui.screen.shared

import eternal.future.tefmanager.Platform
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.source

actual fun releaseResourceToTmp(name: String): Path {
    val resource = object {}.javaClass.getResourceAsStream("/$name")
    val targetPath = Platform.getDirectory("tmp") / name
    FileSystem.SYSTEM.createDirectories(targetPath.parent!!)
    if (resource != null) {
        FileSystem.SYSTEM.sink(targetPath).buffer().use {
            it.writeAll(resource.source())
        }
    }
    return targetPath
}