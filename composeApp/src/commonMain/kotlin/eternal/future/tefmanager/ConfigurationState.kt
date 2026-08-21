package eternal.future.tefmanager

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import eternal.future.tefmanager.strings.StringsResource
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.AppLogger
import eternal.future.tefmanager.utils.ConfigManager
import kotlinx.serialization.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/*******************************************************************************
 * TEFManager - ConfigurationState
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
 * Created: 2026/2/23
 *******************************************************************************/

object ConfigurationState {
    @Serializable
    data class AppConfig(
        var initialized: Boolean = false,
        var kernelVersion: String = "1.0.0",
        var language: StringsResource.Language = StringsResource.Language.System,
        var themeMode: ThemeMode = ThemeMode.SYSTEM,
        var themeSeedColor: ULong = Color(0xFF2196F3).value,
        var fontSizeScale: Float = 1.0f,
        var autoUpdate: Boolean = true,
        var dynamicColor: Boolean = Platform.dynamicColor,
        var kernelLogEnabled: Boolean = true,
        var softwareLogEnabled: Boolean = true,
        var autoCleanLogs: Boolean = true,
        var autoCleanTime: Int = 60, // 分钟
        var maxAppLogFiles: Int = 10,
        var maxAppLogSizeMB: Int = 5,
        var modSupportEnabled: Boolean = true,
        var redirectSavesEnabled: Boolean = false
        ) {
        enum class ThemeMode {
            LIGHT, DARK, AUTO, SYSTEM;

            override fun toString(): String {
                return when(this) {
                    LIGHT -> Strings.settings.appearance.lightTheme
                    DARK -> Strings.settings.appearance.darkTheme
                    AUTO -> Strings.settings.appearance.autoTheme
                    SYSTEM -> Strings.settings.followSystem
                }
            }
        }
    }

    class AutoConfigDelegate<T>(
        private val propertyRef: KMutableProperty1<AppConfig, T>,
        private val onConfigUpdate: (T) -> Unit = {}
    ) : ReadWriteProperty<Any?, T> {

        private var _state: MutableState<T>? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (_state == null) {
                try {
                    val initialValue = propertyRef.get(ConfigManager.getInstance().getConfig())
                    _state = mutableStateOf(initialValue)
                    AppLogger.d("AutoConfig loaded: ${propertyRef.name} = $initialValue")
                } catch (e: Exception) {
                    AppLogger.w("AutoConfig load failed for ${propertyRef.name}", e)
                }
            }
            return _state!!.value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (_state == null) {
                _state = mutableStateOf(value)
            } else if (_state!!.value != value) {
                _state!!.value = value
                try {
                    ConfigManager.getInstance().updateConfig { config ->
                        propertyRef.set(config, value)
                    }
                    AppLogger.d("AutoConfig saved: ${propertyRef.name} = $value")
                    onConfigUpdate(value)
                } catch (e: Exception) {
                    AppLogger.e("AutoConfig save failed for ${propertyRef.name} = $value", e)
                }
            }
        }
    }

    private var _themeSeedColorULong by AutoConfigDelegate(
        AppConfig::themeSeedColor
    )

    var initialized by AutoConfigDelegate(AppConfig::initialized)
    var kernelVersion by AutoConfigDelegate(AppConfig::kernelVersion)
    var language by AutoConfigDelegate(AppConfig::language) {
        StringsResource.setLanguage(it)
    }
    var themeMode by AutoConfigDelegate(AppConfig::themeMode)
    var themeSeedColor: Color
        get() = Color(_themeSeedColorULong)
        set(value) {
            _themeSeedColorULong = value.value
        }
    var fontSizeScale by AutoConfigDelegate(AppConfig::fontSizeScale)
    var autoUpdate by AutoConfigDelegate(AppConfig::autoUpdate)
    var dynamicColor by AutoConfigDelegate(AppConfig::dynamicColor)
    var kernelLogEnabled by AutoConfigDelegate(AppConfig::kernelLogEnabled)
    var softwareLogEnabled by AutoConfigDelegate(AppConfig::softwareLogEnabled)
    var autoCleanLogs by AutoConfigDelegate(AppConfig::autoCleanLogs)
    var autoCleanTime by AutoConfigDelegate(AppConfig::autoCleanTime)
    var maxAppLogFiles by AutoConfigDelegate(AppConfig::maxAppLogFiles)
    var maxAppLogSizeMB by AutoConfigDelegate(AppConfig::maxAppLogSizeMB)
}