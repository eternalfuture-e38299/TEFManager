package eternal.future.tefmanager.ui.screen.shared.resourcepack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.ResourcesPackItem
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.component.ResourcesPackCard
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.resourcepack.BasePackManager
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/*******************************************************************************
 * TEFManager - BasePackScreen
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
 * Created: 2026/6/21
 *******************************************************************************/

abstract class BasePackScreen(
    protected val manager: BasePackManager,
    protected val title: () -> String
) : Screen {

    @Composable
    override fun Content() {
        val loadingMessage: String = Strings.loading
        val emptyActionText: String = Strings.resource.emptyAction(title())

        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                manager.initialize()
                isLoading = false
            }
        }

        var exportPack: ResourcesPackItem? = null
        val packExporter = rememberFileSaverLauncher(
            FileKitDialogSettings.createDefault()
        ) { uri ->
            exportPack?.let { pack ->
                uri?.let { targetFile ->
                    try {
                        val sourcePath = Path(
                            (Platform.getData("module") / "private" / manager.config.packName / manager.config.packSubDir / pack.fileName).toString()
                        )

                        if (SystemFileSystem.exists(sourcePath)) {
                            targetFile.sink().use { outputStream ->
                                SystemFileSystem.source(sourcePath).buffered().use { input ->
                                    outputStream.buffered().use { bufferedOutput ->
                                        input.transferTo(bufferedOutput)
                                    }
                                }
                            }
                        } else {
                            AppLogger.e("Source file does not exist: ${pack.fileName}")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(loadingMessage)
                    }
                }
                manager.packs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${Strings.resource.empty(title())}\n\n$emptyActionText")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = manager.packs.size,
                            key = { index -> manager.packs[index].fileName }
                        ) { index ->
                            val pack = manager.packs[index]
                            var isEnabled by remember(pack.fileName) {
                                mutableStateOf(manager.isPackEnabled(pack.fileName))
                            }

                            ResourcesPackCard(
                                pack = pack,
                                index = index,
                                totalItems = manager.packs.size,
                                isEnabled = isEnabled,
                                onEnableChange = { enabled ->
                                    isEnabled = enabled
                                    manager.setPackEnabled(pack.fileName, enabled)
                                },
                                onMoveUp = {
                                    manager.movePackPriority(pack.fileName, moveUp = true)
                                },
                                onMoveDown = {
                                    manager.movePackPriority(pack.fileName, moveUp = false)
                                },
                                onDelete = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        manager.deletePack(pack.fileName)
                                    }
                                },
                                onExport = {
                                    exportPack = it
                                    packExporter.launch(pack.fileName.removeSuffix(".zip"),
                                        defaultExtension = "zip")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}