package eternal.future.tefmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.ConfigurationState.AppConfig.ThemeMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/*******************************************************************************
 * TEFManager - Theme
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
 * Created: 2026/2/5
 *******************************************************************************/

@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme

@Composable
fun TEFManagerTheme(
    themeMode: ThemeMode = ConfigurationState.themeMode,
    dynamicColor: Boolean = ConfigurationState.dynamicColor,
    seedColor: Color = ConfigurationState.themeSeedColor,
    fontSizeScale: Float = ConfigurationState.fontSizeScale,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.AUTO -> isAutoDarkTheme()
    }

    val colorScheme = when {
        dynamicColor -> dynamicColorScheme(darkTheme)
        else -> rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme)
    }

    // 应用字体缩放
    val scaledTypography = Typography.getScaled(fontSizeScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}

private fun isAutoDarkTheme(): Boolean {
    val currentTime = Clock.System.now()
    val localTime = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localTime.hour

    return hour !in 6..<18
}