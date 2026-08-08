package eternal.future.tefmanager.ui.screen.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eternal.future.tefmanager.Platform
import eternal.future.tefmanager.strings.StringsResource.Strings
import eternal.future.tefmanager.utils.addon.AddonManager
import kotlinx.coroutines.delay
import okio.FileSystem
import okio.SYSTEM
import kotlin.time.Duration.Companion.seconds

/*******************************************************************************
 * TEFManager - OnboardingComponents
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingContent(
    state: Onboarding.OnboardingState,
    onAgreementChange: (Boolean) -> Unit,
    onTipsConfirmed: (Boolean) -> Unit,
    onInstallComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            fadeIn(animationSpec = tween(300, delayMillis = 200)).togetherWith(
                fadeOut(animationSpec = tween(100))
            )
        },
        modifier = modifier
    ) { step ->
        when (step) {
            0 -> AgreementScreen(
                checked = state.agreedToTerms,
                onCheckedChange = onAgreementChange,
                countdown = state.countdown,
                isCountdownActive = state.isCountdownActive
            )
            1 -> TipScreen(
                onAllConfirmed = onTipsConfirmed
            )
            2 -> PlatformTipsScreen()
            3 -> ComponentInstallScreen(
                isInstalling = state.isInstalling,
                onInstallComplete = onInstallComplete
            )
        }
    }
}

// ==================== 协议页面 ====================

@Composable
fun AgreementScreen(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    countdown: Int,
    isCountdownActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Downloading,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Strings.onboarding.agreement.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Strings.onboarding.agreement.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        text = Onboarding.getAgreementText(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = !isCountdownActive,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.onboarding.agreement.check,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (checked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCountdownActive) {
                        Text(
                            text = Strings.onboarding.agreement.wait(countdown),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ==================== 使用须知页面 ====================

@Composable
fun TipScreen(
    onAllConfirmed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tips = Onboarding.getTips()
    var checkedStates by remember { mutableStateOf(tips.map { false }) }
    var allChecked by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(Onboarding.COUNTDOWN_SECONDS) }
    var canProceed by remember { mutableStateOf(false) }

    LaunchedEffect(checkedStates) {
        allChecked = checkedStates.all { it }
        if (allChecked) {
            canProceed = false
            remainingSeconds = Onboarding.COUNTDOWN_SECONDS
            for (i in Onboarding.COUNTDOWN_SECONDS downTo 1) {
                delay(1.seconds)
                remainingSeconds = i - 1
            }
            canProceed = true
            onAllConfirmed(true)
        } else {
            canProceed = false
            remainingSeconds = Onboarding.COUNTDOWN_SECONDS
            onAllConfirmed(false)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = Strings.onboarding.tips.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Strings.onboarding.tips.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tips) { index, tip ->
                TipConfirmationItem(
                    tip = tip,
                    checked = checkedStates[index],
                    onCheckedChange = { isChecked ->
                        checkedStates = checkedStates.toMutableList().apply {
                            set(index, isChecked)
                        }
                    },
                    index = index + 1
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.onboarding.tips.confirmed(checkedStates.count { it }, tips.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (allChecked && !canProceed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = Strings.onboarding.tips.unlocking(remainingSeconds),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (canProceed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = Strings.onboarding.tips.allConfirmed,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TipConfirmationItem(
    tip: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (checked) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = if (checked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// ==================== 平台提示页面 ====================

@Composable
fun PlatformTipsScreen(modifier: Modifier = Modifier) {
    val tips = if (Platform.isAndroid) {
        Onboarding.getAndroidTips()
    } else {
        Onboarding.getDesktopTips()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (Platform.isAndroid) Icons.Default.Android else Icons.Default.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column {
                Text(
                    text = Strings.onboarding.platform.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (Platform.isAndroid) Strings.onboarding.platform.android.subtitle else Strings.onboarding.platform.desktop.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tips) { tip ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 组件安装页面 ====================

@Composable
fun ComponentInstallScreen(
    isInstalling: Boolean,
    onInstallComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val components = Onboarding.getComponentsToInstall()
    var installProgress by remember { mutableStateOf(0) }
    var currentComponent by remember { mutableStateOf(0) }
    var installationComplete by remember { mutableStateOf(false) }

    LaunchedEffect(isInstalling) {
        if (isInstalling && !installationComplete) {
            for (i in components.indices) {
                currentComponent = i
                installProgress = (i + 1) * 100 / components.size

                when (i) {
                    0 -> {
                        val kernelTmpPath = releaseResourceToTmp("tefkernel.zip")
                        AddonManager.installKernel(kernelTmpPath)
                        FileSystem.SYSTEM.delete(kernelTmpPath)

                        val tefloaderTmpPath = releaseResourceToTmp("tefloader.zip")
                        FileSystem.SYSTEM.copy(tefloaderTmpPath, Platform.getData("tefkernel") / "tefloader.zip")
                        FileSystem.SYSTEM.delete(tefloaderTmpPath)
                    } // TEFKernel
                    1 -> {
                        val moduleTmpPath = releaseResourceToTmp("LanguagePackExtension.zip")
                        AddonManager.install(moduleTmpPath)
                        FileSystem.SYSTEM.delete(moduleTmpPath)
                    } // LanguagePackExtension
                    2 -> {
                        val moduleTmpPath = releaseResourceToTmp("TexturePackExtension.zip")
                        AddonManager.install(moduleTmpPath)
                        FileSystem.SYSTEM.delete(moduleTmpPath)
                    }    // TexturePackExtension
                    3 -> {
                        val moduleTmpPath = releaseResourceToTmp("FontPackExtension.zip")
                        AddonManager.install(moduleTmpPath)
                        FileSystem.SYSTEM.delete(moduleTmpPath)
                    }    // FontPackExtension
                    // 4 -> installAudioPack(resourcePath)   // AudioPackExtension
                }

                delay(1.seconds)
            }
            installationComplete = true
            onInstallComplete()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                installationComplete -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                isInstalling -> CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Icon(
                    imageVector = Icons.Default.Downloading,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (installationComplete) Strings.onboarding.install.completeTitle else Strings.onboarding.install.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (installationComplete) {
                Strings.onboarding.install.complete
            } else if (isInstalling) {
                Strings.onboarding.install.installing(components[currentComponent].name)
            } else {
                Strings.onboarding.install.start
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isInstalling || installationComplete) {
            LinearProgressIndicator(
                progress = { installProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$installProgress%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(components) { index, component ->
                ComponentItem(
                    name = component.name,
                    version = component.version,
                    isInstalled = installationComplete || (isInstalling && index <= currentComponent),
                    isInstalling = isInstalling && index == currentComponent && !installationComplete
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = Strings.onboarding.install.total,
                    value = "${components.size}",
                    icon = Icons.Default.Memory
                )
                StatItem(
                    label = Strings.onboarding.install.latest,
                    value = components.maxOfOrNull { it.version } ?: "N/A",
                    icon = Icons.Default.Update
                )
                StatItem(
                    label = Strings.onboarding.install.status,
                    value = if (installationComplete) Strings.onboarding.install.statusCompleted
                    else if (isInstalling) Strings.onboarding.install.statusInstalling
                    else Strings.onboarding.install.statusPending,
                    icon = if (installationComplete) Icons.Default.CheckCircle else Icons.Default.Downloading
                )
            }
        }
    }
}

@Composable
fun ComponentItem(
    name: String,
    version: String,
    isInstalled: Boolean = false,
    isInstalling: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isInstalled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isInstalling -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    isInstalled -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    isInstalling -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isInstalled -> MaterialTheme.colorScheme.onSurface
                        isInstalling -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun HorizontalProgressIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(Onboarding.TOTAL_STEPS) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = if (index <= currentStep)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}