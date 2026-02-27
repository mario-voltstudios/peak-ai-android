package com.peakai.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peakai.fitness.domain.model.ReadinessScore
import com.peakai.fitness.ui.theme.AmberDark
import com.peakai.fitness.ui.theme.AmberLight
import com.peakai.fitness.ui.theme.AmberPrimary
import com.peakai.fitness.ui.theme.BackgroundDark
import com.peakai.fitness.ui.theme.ErrorRed
import com.peakai.fitness.ui.theme.OnSurface
import com.peakai.fitness.ui.theme.OnSurfaceVariant
import com.peakai.fitness.ui.theme.SuccessGreen
import com.peakai.fitness.ui.theme.SurfaceDark
import com.peakai.fitness.ui.theme.SurfaceVariantDark
import com.peakai.fitness.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "PEAK AI",
            style = MaterialTheme.typography.labelSmall.copy(
                color = AmberPrimary,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Daily Readiness",
            style = MaterialTheme.typography.headlineMedium.copy(color = OnSurface)
        )

        Spacer(Modifier.height(24.dp))

        // Readiness Score Ring
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmberPrimary)
                }
            }
            uiState.readiness != null -> {
                ReadinessRing(readiness = uiState.readiness!!)
            }
            else -> {
                PermissionPromptCard(onGrantClick = { viewModel.requestPermissions() })
            }
        }

        Spacer(Modifier.height(20.dp))

        // Biometric cards
        uiState.snapshot?.let { snap ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BiometricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Favorite,
                    label = "HRV",
                    value = snap.hrv?.let { "${String.format("%.0f", it.sdnn)}" } ?: "--",
                    unit = "ms",
                    iconTint = AmberPrimary
                )
                BiometricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Speed,
                    label = "Resting HR",
                    value = snap.restingHeartRate?.toString() ?: "--",
                    unit = "bpm",
                    iconTint = ErrorRed
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BiometricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Nightlight,
                    label = "Sleep",
                    value = snap.sleep?.let { String.format("%.1f", it.durationHours) } ?: "--",
                    unit = "hrs",
                    iconTint = Color(0xFF818CF8) // indigo-400
                )
                BiometricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.WaterDrop,
                    label = "SpO₂",
                    value = snap.spo2?.let { String.format("%.1f", it) } ?: "--",
                    unit = "%",
                    iconTint = Color(0xFF38BDF8) // sky-400
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Morning briefing card
        uiState.morningBriefing?.let { briefing ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Morning Briefing",
                        style = MaterialTheme.typography.labelLarge.copy(color = AmberPrimary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        briefing.content,
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface)
                    )
                    if (briefing.actionItems.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Action Items",
                            style = MaterialTheme.typography.labelMedium.copy(color = AmberDark)
                        )
                        briefing.actionItems.forEachIndexed { i, item ->
                            Text(
                                "${i + 1}. $item",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = OnSurfaceVariant
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Log summary
        uiState.dailySummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Today's Log",
                        style = MaterialTheme.typography.labelLarge.copy(color = AmberPrimary)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        LogStat("💧", "${summary.waterGlasses}", "glasses")
                        LogStat("☕", "${summary.caffeineMg}", "mg caffeine")
                        LogStat("💊", "${summary.supplements.size}", "supplements")
                    }
                    if (summary.caffeineOverLimit) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ Caffeine limit reached — switch to water",
                            style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // Bottom nav padding
    }
}

@Composable
private fun ReadinessRing(readiness: ReadinessScore) {
    val score = readiness.score
    val progress by animateFloatAsState(
        targetValue = score / 10f,
        animationSpec = tween(1200),
        label = "readiness_progress"
    )

    val ringColor = when (readiness.label) {
        ReadinessScore.ReadinessLabel.PEAK, ReadinessScore.ReadinessLabel.HIGH -> AmberPrimary
        ReadinessScore.ReadinessLabel.MODERATE -> Color(0xFFF97316) // orange-500
        ReadinessScore.ReadinessLabel.LOW -> ErrorRed
        ReadinessScore.ReadinessLabel.RECOVERY -> Color(0xFF8B5CF6) // violet-500
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(200.dp),
                color = ringColor,
                strokeWidth = 14.dp,
                trackColor = SurfaceVariantDark,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = ringColor,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = readiness.label.name.lowercase().replace('_', ' '),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = OnSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }

    // Score breakdown
    Spacer(Modifier.height(16.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScoreComponent("HRV", readiness.hrvScore)
        ScoreComponent("Sleep", readiness.sleepScore)
        ScoreComponent("RHR", readiness.rhrScore)
        ScoreComponent("Activity", readiness.activityScore)
    }
}

@Composable
private fun ScoreComponent(label: String, score: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            String.format("%.0f%%", score * 100),
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (score >= 0.7) AmberLight else OnSurfaceVariant
            )
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
        )
    }
}

@Composable
private fun BiometricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = OnSurface, fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LogStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(color = OnSurface, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant))
    }
}

@Composable
private fun PermissionPromptCard(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Connect Health Data", style = MaterialTheme.typography.titleMedium.copy(color = OnSurface))
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant Health Connect permissions to see your readiness score and biometrics.",
                style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onGrantClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Connect Health Connect", color = Color.Black)
            }
        }
    }
}

// Needed extension — not in Material3 yet
private val MaterialTheme.typography: androidx.compose.material3.Typography get() =
    androidx.compose.material3.MaterialTheme.typography
