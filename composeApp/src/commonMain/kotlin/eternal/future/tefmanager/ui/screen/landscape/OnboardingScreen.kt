package eternal.future.tefmanager.ui.screen.landscape

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.ui.screen.shared.HorizontalProgressIndicator
import eternal.future.tefmanager.ui.screen.shared.Onboarding
import eternal.future.tefmanager.ui.screen.shared.OnboardingContent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/*******************************************************************************
 * TEFManager - OnboardingScreen (Landscape)
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
    ) {
        // 左侧导航面板
        LandscapeNavigationPanel(
            currentStep = state.currentStep,
            onStepChange = { step ->
                if (step < state.currentStep) {
                    if (state.currentStep == 1) {
                        state = state.copy(confirmedTips = false)
                    }
                    state = state.copy(currentStep = step)
                }
            },
            modifier = Modifier
                .weight(0.32f)
                .fillMaxHeight()
        )

        // 右侧内容面板
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Onboarding.getStepTitle(state.currentStep),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.currentStep + 1} / ${Onboarding.TOTAL_STEPS}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Onboarding.getStepIcon(state.currentStep),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalProgressIndicator(
                currentStep = state.currentStep,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(20.dp))

            // 底部操作按钮
            LandscapeBottomActions(
                state = state,
                onStepChange = { step ->
                    state = state.copy(currentStep = step)
                },
                onConfirmedTipsChange = { confirmed ->
                    state = state.copy(confirmedTips = confirmed)
                },
                onInstallStart = {
                    state = state.copy(isInstalling = true)
                }
            )
        }
    }
}

// ============================================================
// 左侧导航面板
// ============================================================
@Composable
private fun LandscapeNavigationPanel(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            ),
                            radius = 88f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "TEFManager",
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TEFManager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "游戏模组加载与管理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 步骤列表
            repeat(Onboarding.TOTAL_STEPS) { index ->
                val isActive = index == currentStep
                val isCompleted = index < currentStep
                val isClickable = index < currentStep

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(
                            if (isClickable) {
                                Modifier.clickable { onStepChange(index) }
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        isCompleted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    },
                    tonalElevation = if (isActive) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Onboarding.getStepTitle(index),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isCompleted) {
                                Text(
                                    text = Strings.onboarding.navigation.stepCompleted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else if (isActive) {
                                Text(
                                    text = Strings.onboarding.navigation.stepActive,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isActive) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 横屏底部操作按钮
// ============================================================
@Composable
private fun LandscapeBottomActions(
    state: Onboarding.OnboardingState,
    onStepChange: (Int) -> Unit,
    onConfirmedTipsChange: (Boolean) -> Unit,
    onInstallStart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        if (state.currentStep in 1..<3) {
            OutlinedButton(
                onClick = {
                    if (state.currentStep == 1) {
                        onConfirmedTipsChange(false)
                    }
                    onStepChange(state.currentStep - 1)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(0.25f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    Strings.onboarding.tips.back,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(0.25f))
        }

        // 主按钮
        val isEnabled = Onboarding.canProceed(state)
        val buttonText = Onboarding.getPrimaryButtonText(state)

        Button(
            onClick = {
                when (state.currentStep) {
                    0 -> if (!state.isCountdownActive) onStepChange(state.currentStep + 1)
                    1 -> if (state.confirmedTips) onStepChange(state.currentStep + 1)
                    2 -> onStepChange(state.currentStep + 1)
                    3 -> onInstallStart()
                }
            },
            enabled = isEnabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
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
            ),
            modifier = Modifier.weight(0.5f)
        ) {
            when {
                state.isInstalling -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                state.currentStep == 3 -> {
                    Icon(
                        imageVector = Icons.Default.Downloading,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                state.currentStep < 2 -> {
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 占位保持平衡
        Spacer(modifier = Modifier.weight(0.25f))
    }
}