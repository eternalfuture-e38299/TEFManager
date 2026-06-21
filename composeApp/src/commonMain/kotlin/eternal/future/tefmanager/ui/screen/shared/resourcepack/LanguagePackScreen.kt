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
import eternal.future.tefmanager.utils.resourcepack.LanguagePackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*******************************************************************************
 * TEFManager - LanguagePackScreen
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

object LanguagePackScreen : Screen {
    @Composable
    override fun Content() {
        var isLoading by remember { mutableStateOf(true) }
        var enabledCount by remember { mutableStateOf(0) }
        val maxCount = remember { LanguagePackManager.maxEnabledCount }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                LanguagePackManager.initialize()
                enabledCount = LanguagePackManager.getEnabledCount()
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
                        Text("正在加载语言包...")
                    }
                }
                LanguagePackManager.languagePacks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无语言包\n\n请点击右上角按钮安装语言包")
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 顶部统计信息
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
                                        text = "语言包管理",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "已启用 $enabledCount / $maxCount 个",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (enabledCount >= maxCount)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (enabledCount >= maxCount) {
                                    Text(
                                        text = "已达到上限",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
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
                                count = LanguagePackManager.languagePacks.size,
                                key = { index -> LanguagePackManager.languagePacks[index].fileName }
                            ) { index ->
                                val pack = LanguagePackManager.languagePacks[index]
                                var isEnabled by remember(pack.fileName) {
                                    mutableStateOf(LanguagePackManager.isPackEnabled(pack.fileName))
                                }

                                LaunchedEffect(LanguagePackManager.enabledPacks) {
                                    isEnabled = LanguagePackManager.isPackEnabled(pack.fileName)
                                }

                                ResourcesPackCard(
                                    pack = pack,
                                    index = index,
                                    totalItems = LanguagePackManager.languagePacks.size,
                                    isEnabled = isEnabled,
                                    switchEnabled = LanguagePackManager.canEnableMore() || isEnabled,  // 达到上限时禁用开关
                                    onEnableChange = { enabled ->
                                        if (enabled && !LanguagePackManager.canEnableMore() && !isEnabled) {
                                            // 已达到上限，无法启用更多
                                            return@ResourcesPackCard
                                        }
                                        val success = LanguagePackManager.setPackEnabled(pack.fileName, enabled)
                                        if (success) {
                                            isEnabled = enabled
                                            enabledCount = LanguagePackManager.getEnabledCount()
                                        }
                                    },
                                    onMoveUp = {
                                        LanguagePackManager.movePackPriority(pack.fileName, moveUp = true)
                                    },
                                    onMoveDown = {
                                        LanguagePackManager.movePackPriority(pack.fileName, moveUp = false)
                                    },
                                    onDelete = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            LanguagePackManager.deleteLanguagePack(pack.fileName)
                                            enabledCount = LanguagePackManager.getEnabledCount()
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