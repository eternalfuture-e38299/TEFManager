package eternal.future.tefmanager.utils

import eternal.future.tefmanager.Platform
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import java.io.File

/*******************************************************************************
 * TEFManager - GamePatcher
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
 * Created: 2026/2/17
 *******************************************************************************/

object GamePatcher {
    private val files = FileSystem.SYSTEM;

    fun patchViaDotNetGrafting(filePath: Path, addLoader: Boolean = true, architecture: String = Platform.getArchitecture()) {
        val kernelDir = Platform.getData("tefkernel")
        val tefloader = files.openZip(kernelDir / "tefloader.zip")

        try {
            val targetDir = filePath.parent
            AppLogger.i("Starting .NET grafting patch process")
            AppLogger.d("Target directory: $targetDir")
            AppLogger.d("Add loader: $addLoader")
            AppLogger.d("Architecture: $architecture")

            if (addLoader) {
                val netRuntime = if (Platform.isWindows) "net452/" else "net472/"
                AppLogger.i("Extracting .NET runtime: $netRuntime")

                tefloader.list(netRuntime.toPath()).forEach { entry ->
                    try {
                        val entryNameWithoutPrefix = entry.name.removePrefix(netRuntime)
                        val targetFile = targetDir!!.resolve(entryNameWithoutPrefix)

                        files.sink(targetFile).buffer().use { sink ->
                            tefloader.source(entry).use { source ->
                                sink.writeAll(source)
                            }
                        }

                        // 对非Windows系统，为提取的文件设置可执行权限
                        if (!Platform.isWindows) {
                            try {
                                val file = File(targetFile.toString())
                                if (file.exists()) {
                                    val result = file.setExecutable(true)
                                    if (result) {
                                        AppLogger.d("Set executable permission for: ${targetFile.name}")
                                    } else {
                                        AppLogger.w("Failed to set executable permission for: ${targetFile.name}")
                                    }
                                }
                            } catch (e: SecurityException) {
                                AppLogger.e("Security exception when setting executable permission for ${targetFile.name}", e)
                            } catch (e: Exception) {
                                AppLogger.e("Failed to set executable permission for ${targetFile.name}", e)
                            }
                        }

                        AppLogger.d("Extracted: ${entry.name} -> $targetFile")
                    } catch (e: Exception) {
                        AppLogger.e("Failed to extract ${entry.name}", e)
                    }
                }
            }

            if (!Platform.isWindows) {
                AppLogger.i("Processing binary files for non-Windows platform")
                val binFiles = files.list(targetDir!!).filter { file ->
                    file.name.startsWith("Terraria.bin.")
                }

                AppLogger.d("Found ${binFiles.size} binary files to process")

                binFiles.forEach { sourceFile ->
                    try {
                        val sourceArchitecture = sourceFile.name.removePrefix("Terraria.bin.")
                        val targetFile = targetDir.resolve("tefloader.bin.$sourceArchitecture")

                        files.copy(sourceFile, targetFile)

                        try {
                            val copiedFile = File(targetFile.toString())
                            if (copiedFile.exists()) {
                                val result = copiedFile.setExecutable(true)
                                if (result) {
                                    AppLogger.d("Set executable permission for copied binary: ${targetFile.name}")
                                } else {
                                    AppLogger.w("Failed to set executable permission for copied binary: ${targetFile.name}")
                                }
                            }
                        } catch (e: SecurityException) {
                            AppLogger.e("Security exception when setting executable permission for copied binary ${targetFile.name}", e)
                        } catch (e: Exception) {
                            AppLogger.e("Failed to set executable permission for copied binary ${targetFile.name}", e)
                        }

                        AppLogger.d("Copied binary: ${sourceFile.name} -> ${targetFile.name}")
                    } catch (e: Exception) {
                        AppLogger.e("Failed to copy binary ${sourceFile.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error in patchViaDotNetGrafting", e)
            throw e
        } finally {
            try {
                tefloader.close()
                AppLogger.i("Zip file closed successfully")
            } catch (e: Exception) {
                AppLogger.e("Error closing zip file", e)
            }
        }
    }
}