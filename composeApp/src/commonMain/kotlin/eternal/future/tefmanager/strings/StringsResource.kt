package eternal.future.tefmanager.strings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eternal.future.tefmanager.ConfigurationState
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.generated.*

/*******************************************************************************
 * TEFManager - StringsResource
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
 * Created: 2026/1/30
 *******************************************************************************/

object StringsResource {
    var Strings by mutableStateOf<LocaleStrings>(ZhHans)

    init {
        setLanguage(ConfigurationState.language)
    }

    fun setLanguage(code: Language) {
        ConfigurationState.language = code
        val code = if (code == Language.System) fromSystemLocale() else code
        Strings = when (code) {
            Language.ZhHans -> ZhHans
            Language.En -> En
            Language.Ru -> Ru
            else -> ZhHans
        }
    }

    enum class Language {
        System, ZhHans, En, Ru;  // Добавлен Ru

        override fun toString(): String {
            return when(this) {
                System -> Strings.settings.followSystem
                ZhHans -> "简体中文"
                En -> "English"
                Ru -> "Русский"
            }
        }
    }

    fun fromSystemLocale() : Language {
        val language = Platform.systemLanguage.lowercase()
        val region = Platform.systemRegion.lowercase()

        val simplifiedChineseRegions = setOf("hans", "cn", "sg")

        return when (language) {
            "zh" if (region in simplifiedChineseRegions) -> Language.ZhHans
            "en" -> Language.En
            "ru" -> Language.Ru
            else -> Language.En
        }
    }
}