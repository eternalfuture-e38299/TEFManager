package eternal.future.tefmanager.ui.screen.shared

import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.Platform
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.source

actual fun releaseResourceToTmp(name: String): Path {
    val context = MainActivity.context!!
    val resource = context.assets.open(name)
    val targetPath = Platform.getDirectory("tmp") / name
    FileSystem.SYSTEM.createDirectories(targetPath.parent!!)
    FileSystem.SYSTEM.sink(targetPath).buffer().use {
        it.writeAll(resource.source())
    }
    return targetPath
}