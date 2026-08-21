package eternal.future.tefmanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/*******************************************************************************
 * TEFManager - Type
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

val Typography = Typography()

fun Typography.getScaled(scale: Float): Typography {
    if (scale == 1.0f) return this

    fun scaleTextStyle(style: TextStyle): TextStyle {
        return style.copy(fontSize = (style.fontSize.value * scale).sp)
    }

    return Typography(
        displayLarge = scaleTextStyle(displayLarge),
        displayMedium = scaleTextStyle(displayMedium),
        displaySmall = scaleTextStyle(displaySmall),
        headlineLarge = scaleTextStyle(headlineLarge),
        headlineMedium = scaleTextStyle(headlineMedium),
        headlineSmall = scaleTextStyle(headlineSmall),
        titleLarge = scaleTextStyle(titleLarge),
        titleMedium = scaleTextStyle(titleMedium),
        titleSmall = scaleTextStyle(titleSmall),
        bodyLarge = scaleTextStyle(bodyLarge),
        bodyMedium = scaleTextStyle(bodyMedium),
        bodySmall = scaleTextStyle(bodySmall),
        labelLarge = scaleTextStyle(labelLarge),
        labelMedium = scaleTextStyle(labelMedium),
        labelSmall = scaleTextStyle(labelSmall)
    )
}