package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import eternal.future.tefmanager.BuildConfig
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.model.Component
import eternal.future.tefmanager.strings.StringsResource.Strings
import okio.Path

/*******************************************************************************
 * TEFManager - Onboarding
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
 * Created: 2026/8/7
 *******************************************************************************/

object Onboarding {
    const val TOTAL_STEPS = 4
    const val COUNTDOWN_SECONDS = 5

    fun getStepTitle(step: Int): String {
        return when (step) {
            0 -> Strings.onboarding.step.agreement
            1 -> Strings.onboarding.step.tips
            2 -> Strings.onboarding.step.platform
            3 -> Strings.onboarding.step.install
            else -> ""
        }
    }

    fun getStepIcon(step: Int): ImageVector {
        return when (step) {
            0 -> Icons.Default.Description
            1 -> Icons.Default.Psychology
            2 -> Icons.Default.Warning
            3 -> Icons.Default.Downloading
            else -> Icons.Default.Info
        }
    }

    fun getAgreementText(): String {
        return Strings.onboarding.agreement.text
    }

    fun getAndroidTips(): List<String> {
        return listOf(
            Strings.onboarding.android.tips1,
            Strings.onboarding.android.tips2,
            Strings.onboarding.android.tips3,
            Strings.onboarding.android.tips4
        )
    }

    fun getDesktopTips(): List<String> {
        return listOf(
            Strings.onboarding.desktop.tips1,
            Strings.onboarding.desktop.tips2,
            Strings.onboarding.desktop.tips3
        )
    }

    fun getComponentsToInstall(): List<Component> {
        return mutableListOf(
            Component("TEFKernel", BuildConfig.KERNEL_VERSION),
            Component(Strings.update.language, BuildConfig.getModuleVersion("LanguagePackExtension"))
        ).also {
            if (Platform.isAndroid) {
                it.add(Component(Strings.update.texture, BuildConfig.getModuleVersion("TexturePackExtension")))
                it.add(Component(Strings.update.font, BuildConfig.getModuleVersion("FontPackExtension")))
                // it.add(Component(Strings.update.music, BuildConfig.getModuleVersion("AudioPackExtension")))
            }
        }
    }

    // ==================== 共享状态管理 ====================

    data class OnboardingState(
        val currentStep: Int = 0,
        val agreedToTerms: Boolean = false,
        val confirmedTips: Boolean = false,
        val isInstalling: Boolean = false,
        val countdown: Int = COUNTDOWN_SECONDS,
        val isCountdownActive: Boolean = false
    )

    fun canProceed(state: OnboardingState): Boolean {
        return when (state.currentStep) {
            0 -> state.agreedToTerms && !state.isCountdownActive
            1 -> state.confirmedTips
            2 -> true
            3 -> !state.isInstalling
            else -> true
        }
    }

    fun getPrimaryButtonText(state: OnboardingState): String {
        return when (state.currentStep) {
            0 -> if (state.isCountdownActive) Strings.onboarding.agreement.reading else Strings.onboarding.agreement.button
            1 -> if (state.confirmedTips) Strings.onboarding.tips.button else Strings.onboarding.tips.confirmRequired
            2 -> Strings.onboarding.platform.button
            3 -> if (state.isInstalling) Strings.onboarding.install.installingButton else Strings.onboarding.install.button
            else -> Strings.confirm
        }
    }

    fun getTips(): List<String> {
        return listOf(
            Strings.onboarding.tips.item1,
            Strings.onboarding.tips.item2,
            Strings.onboarding.tips.item3,
            Strings.onboarding.tips.item4
        )
    }
}

expect fun releaseResourceToTmp(name : String) : Path