package eternal.future.tefmanager.ui.screen.shared

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
import eternal.future.tefmanager.ui.component.TexturePackCard
import eternal.future.tefmanager.utils.resourcepack.TexturePackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TexturePackScreen : Screen {
    @Composable
    override fun Content() {
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                TexturePackManager.initialize()
                isLoading = false
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("正在加载材质包...")
                }
            } else {
                if (TexturePackManager.texturePacks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无材质包\n\n请点击右上角按钮安装材质包")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = TexturePackManager.texturePacks.size,
                            key = { index -> TexturePackManager.texturePacks[index].fileName }
                        ) { index ->
                            val pack = TexturePackManager.texturePacks[index]
                            var isEnabled by remember(pack.fileName) {
                                mutableStateOf(TexturePackManager.isPackEnabled(pack.fileName))
                            }

                            TexturePackCard(
                                pack = pack,
                                index = index,
                                totalItems = TexturePackManager.texturePacks.size,
                                isEnabled = isEnabled,
                                onEnableChange = { enabled ->
                                    isEnabled = enabled
                                    TexturePackManager.setPackEnabled(pack.fileName, enabled)
                                },
                                onMoveUp = {
                                    TexturePackManager.movePackPriority(pack.fileName, moveUp = true)
                                },
                                onMoveDown = {
                                    TexturePackManager.movePackPriority(pack.fileName, moveUp = false)
                                },
                                onDelete = {
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        TexturePackManager.deleteTexturePack(pack.fileName)
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