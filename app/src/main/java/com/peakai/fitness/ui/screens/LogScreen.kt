package com.peakai.fitness.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peakai.fitness.ui.theme.AmberPrimary
import com.peakai.fitness.ui.theme.BackgroundDark
import com.peakai.fitness.ui.theme.ErrorRed
import com.peakai.fitness.ui.theme.OnSurface
import com.peakai.fitness.ui.theme.OnSurfaceVariant
import com.peakai.fitness.ui.theme.SuccessGreen
import com.peakai.fitness.ui.theme.SurfaceDark
import com.peakai.fitness.ui.theme.SurfaceVariantDark
import com.peakai.fitness.ui.viewmodel.LogViewModel

@Composable
fun LogScreen(viewModel: LogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "LOG",
            style = MaterialTheme.typography.labelSmall.copy(
                color = AmberPrimary,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            "Today's Intake",
            style = MaterialTheme.typography.headlineMedium.copy(color = OnSurface)
        )

        Spacer(Modifier.height(24.dp))

        // Water
        SectionLabel("💧 Water")
        WaterTiles(
            currentMl = uiState.waterMl,
            onLog = { viewModel.logWater(it) }
        )

        Spacer(Modifier.height(24.dp))

        // Caffeine
        SectionLabel("☕ Caffeine")
        val caffeineColor by animateColorAsState(
            targetValue = if (uiState.caffeineMg > 300) ErrorRed else AmberPrimary,
            animationSpec = tween(500),
            label = "caffeine_color"
        )
        Text(
            "${uiState.caffeineMg}mg today",
            style = MaterialTheme.typography.titleMedium.copy(color = caffeineColor)
        )
        if (uiState.caffeineMg > 300) {
            Text(
                "⚠️ Limit reached — switch to water",
                style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed)
            )
        }
        Spacer(Modifier.height(8.dp))
        CaffeineTiles(onLog = { viewModel.logCaffeine(it.first, it.second) })

        Spacer(Modifier.height(24.dp))

        // Supplements
        SectionLabel("💊 Supplements")
        SupplementTiles(
            logged = uiState.supplements,
            onLog = { viewModel.logSupplement(it) }
        )

        Spacer(Modifier.height(24.dp))

        // Medications
        SectionLabel("🩺 Medications")
        SupplementTiles(
            logged = uiState.medications,
            onLog = { viewModel.logMedication(it) },
            presets = listOf("Metformin", "Lisinopril", "Levothyroxine", "Aspirin", "Vitamin D")
        )

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium.copy(color = OnSurface, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun WaterTiles(currentMl: Int, onLog: (Int) -> Unit) {
    val options = listOf(
        Triple("🥃", "250ml", 250),
        Triple("🫙", "500ml", 500),
        Triple("🏋️", "750ml", 750),
        Triple("🔱", "1L", 1000)
    )

    Column {
        Text(
            "${currentMl}ml today (~${currentMl / 250} glasses)",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (currentMl >= 2000) SuccessGreen else OnSurfaceVariant
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (emoji, label, ml) ->
                TapTile(
                    modifier = Modifier.weight(1f),
                    emoji = emoji,
                    label = label,
                    onClick = { onLog(ml) }
                )
            }
        }
    }
}

@Composable
private fun CaffeineTiles(onLog: (Pair<Int, String>) -> Unit) {
    val options = listOf(
        Triple("☕", "Espresso\n65mg", Pair(65, "espresso")),
        Triple("🫖", "Coffee\n95mg", Pair(95, "drip coffee")),
        Triple("🍵", "Tea\n50mg", Pair(50, "tea")),
        Triple("⚡", "Pre-WO\n200mg", Pair(200, "pre-workout"))
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (emoji, label, data) ->
            TapTile(
                modifier = Modifier.weight(1f),
                emoji = emoji,
                label = label,
                onClick = { onLog(data) }
            )
        }
    }
}

@Composable
private fun SupplementTiles(
    logged: List<String>,
    onLog: (String) -> Unit,
    presets: List<String> = listOf("Creatine", "Magnesium", "Omega-3", "Vitamin D", "Zinc", "B12")
) {
    if (logged.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            logged.forEach { name ->
                Text(
                    "✅ $name",
                    style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.take(4).forEach { name ->
            val alreadyLogged = name in logged
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !alreadyLogged) { onLog(name) },
                colors = CardDefaults.cardColors(
                    containerColor = if (alreadyLogged) SurfaceVariantDark else SurfaceDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (alreadyLogged) OnSurfaceVariant else OnSurface
                    ),
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TapTile(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = OnSurfaceVariant
                ),
                maxLines = 2
            )
        }
    }
}
