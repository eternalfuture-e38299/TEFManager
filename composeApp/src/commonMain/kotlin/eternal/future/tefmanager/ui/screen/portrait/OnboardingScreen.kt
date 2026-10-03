package eternal.future.tefmanager.ui.screen.portrait

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.screen.shared.Onboarding
import eternal.future.tefmanager.ui.screen.shared.OnboardingContent
import eternal.future.tefmanager.ui.screen.shared.HorizontalProgressIndicator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/*******************************************************************************
 * TEFManager - OnboardingScreen (Portrait)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onInstallComplete: () -> Unit) {
    var state by remember {
        mutableStateOf(Onboarding.OnboardingState())
    }

    // 处理协议页面倒计时
    LaunchedEffect(state.currentStep) {
        if (state.currentStep == 0) {
            state = state.copy(
                countdown = Onboarding.COUNTDOWN_SECONDS,
                isCountdownActive = true
            )
            while (state.countdown > 0 && state.isCountdownActive) {
                delay(1.seconds)
                state = state.copy(countdown = state.countdown - 1)
            }
            state = state.copy(isCountdownActive = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
    ) {
        // 顶部进度指示器
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Onboarding.getStepTitle(state.currentStep),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.currentStep + 1}/${Onboarding.TOTAL_STEPS}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalProgressIndicator(
                    currentStep = state.currentStep,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            OnboardingContent(
                state = state,
                onAgreementChange = { agreed ->
                    state = state.copy(agreedToTerms = agreed)
                },
                onTipsConfirmed = { confirmed ->
                    state = state.copy(confirmedTips = confirmed)
                },
                onInstallComplete = {
                    state = state.copy(isInstalling = false)
                    onInstallComplete()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部按钮区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // 主要操作按钮
                val isPrimaryEnabled = Onboarding.canProceed(state)
                val primaryButtonText = Onboarding.getPrimaryButtonText(state)

                FilledTonalButton(
                    onClick = {
                        when (state.currentStep) {
                            0 -> if (!state.isCountdownActive) {
                                state = state.copy(currentStep = state.currentStep + 1)
                            }
                            1 -> if (state.confirmedTips) {
                                state = state.copy(currentStep = state.currentStep + 1)
                            }
                            2 -> state = state.copy(currentStep = state.currentStep + 1)
                            3 -> {
                                state = state.copy(isInstalling = true)
                            }
                        }
                    },
                    enabled = isPrimaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (state.currentStep == 3 && !state.isInstalling) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (state.currentStep == 3 && !state.isInstalling) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    when {
                        state.isInstalling -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        state.currentStep == 3 -> {
                            Icon(
                                imageVector = Icons.Default.Downloading,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        state.currentStep < 2 -> {
                            Text(
                                primaryButtonText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            return@FilledTonalButton
                        }
                    }
                    Text(
                        primaryButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 辅助操作行
                if (state.currentStep in 1..<3) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (state.currentStep == 1) {
                                    state = state.copy(confirmedTips = false)
                                }
                                state = state.copy(currentStep = state.currentStep - 1)
                            },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                Strings.onboarding.tips.back,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (state.currentStep == 1) {
                            Text(
                                text = if (state.confirmedTips) Strings.onboarding.tips.allConfirmed else Strings.onboarding.tips.confirming,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.confirmedTips) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 步骤指示器小点
                if (state.currentStep == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(Onboarding.TOTAL_STEPS) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == state.currentStep) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = when {
                                            index == state.currentStep -> MaterialTheme.colorScheme.primary
                                            index < state.currentStep -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        }
                                    )
                            )
                            if (index < Onboarding.TOTAL_STEPS - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}