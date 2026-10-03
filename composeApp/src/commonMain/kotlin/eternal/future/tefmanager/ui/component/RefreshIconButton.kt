package eternal.future.tefmanager.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*******************************************************************************
 * TEFManager - RefreshIconButton
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
 * Created: 2026/2/28
 *******************************************************************************/

@Composable
fun RefreshIconButton(
    refreshAction: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            if (!isRefreshing) {
                isRefreshing = true
                refreshAction()

                MainScope().launch {
                    delay(1000)
                    isRefreshing = false
                }
            }
        },
        enabled = !isRefreshing
    ) {
        if (isRefreshing) {
            // 刷新中，显示旋转图标
            androidx.compose.animation.core.animateFloatAsState(
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 1000,
                    easing = androidx.compose.animation.core.LinearEasing
                ),
                label = "refresh_rotation"
            ).let { rotationState ->
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refreshing",
                    modifier = Modifier.rotate(rotationState.value),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // 正常状态
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}