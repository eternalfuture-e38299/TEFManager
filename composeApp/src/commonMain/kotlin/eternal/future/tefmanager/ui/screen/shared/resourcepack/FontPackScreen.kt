package eternal.future.tefmanager.ui.screen.shared.resourcepack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import eternal.future.tefmanager.ui.component.ResourcesPackCard
import eternal.future.tefmanager.utils.resourcepack.FontPackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.strings.StringsResource.Strings

/*******************************************************************************
 * TEFManager - FontPackScreen
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

object FontPackScreen : Screen {
    @Composable
    override fun Content() {
        var isLoading by remember { mutableStateOf(true) }
        var selectedPackName by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                FontPackManager.initialize()
                selectedPackName = FontPackManager.selectedPackFileName
                isLoading = false
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Strings.loading)
                    }
                }
                FontPackManager.fontPacks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${Strings.resource.empty(Strings.resource.font.title)}\n\n${Strings.resource.emptyAction(Strings.resource.font.title)}")
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 顶部提示卡片
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = Strings.resource.font.hint,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (selectedPackName != null) {
                                            FontPackManager.getSelectedFontPack()?.name.let {
                                                if (it != null)
                                                    Strings.resource.font.selected(it)
                                                else
                                                    Strings.resource.font.selectedUnknown
                                            }
                                        } else {
                                            Strings.resource.font.none
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedPackName != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = FontPackManager.fontPacks.size,
                                key = { index -> FontPackManager.fontPacks[index].fileName }
                            ) { index ->
                                val pack = FontPackManager.fontPacks[index]
                                val isSelected = FontPackManager.isFontPackSelected(pack.fileName)

                                ResourcesPackCard(
                                    pack = pack,
                                    index = index,
                                    totalItems = FontPackManager.fontPacks.size,
                                    isEnabled = isSelected,
                                    switchEnabled = isSelected || FontPackManager.selectedPackFileName == null,
                                    onEnableChange = { enabled ->
                                        if (enabled) {
                                            // 选中这个字体包（会自动取消其他）
                                            FontPackManager.selectFontPack(pack.fileName)
                                            selectedPackName = pack.fileName
                                        } else {
                                            // 如果是当前选中的，取消选中
                                            if (FontPackManager.isFontPackSelected(pack.fileName)) {
                                                FontPackManager.deselectFontPack()
                                                selectedPackName = null
                                            }
                                        }
                                    },
                                    onMoveUp = null,  // 字体包不需要排序
                                    onMoveDown = null,  // 字体包不需要排序
                                    onDelete = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            FontPackManager.deleteFontPack(pack.fileName)
                                            selectedPackName = FontPackManager.selectedPackFileName
                                        }
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
}