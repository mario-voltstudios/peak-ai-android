package com.peakai.fitness.domain.usecase

import com.peakai.fitness.domain.model.BaselineData
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.ReadinessScore
import com.peakai.fitness.domain.model.ReadinessScore.ReadinessLabel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Readiness scoring algorithm (1–10):
 *
 * HRV vs 7-day baseline        → weight 40%
 * Sleep duration + quality     → weight 30%
 * Resting HR vs baseline       → weight 20%
 * Activity / recovery balance  → weight 10%
 */
class ComputeReadinessUseCase @Inject constructor() {

    operator fun invoke(
        snapshot: BiometricSnapshot,
        baseline: BaselineData,
        targetSleepHours: Double = 7.5
    ): ReadinessScore {
        val hrvScore = computeHrvScore(snapshot, baseline)
        val sleepScore = computeSleepScore(snapshot, targetSleepHours)
        val rhrScore = computeRhrScore(snapshot, baseline)
        val activityScore = computeActivityScore(snapshot, baseline)

        val composite = (
            hrvScore * 0.40 +
            sleepScore * 0.30 +
            rhrScore * 0.20 +
            activityScore * 0.10
        )

        val score = (composite * 9 + 1).toInt().coerceIn(1, 10)

        return ReadinessScore(
            score = score,
            label = scoreToLabel(score),
            hrvScore = hrvScore,
            sleepScore = sleepScore,
            rhrScore = rhrScore,
            activityScore = activityScore
        )
    }

    /**
     * HRV score: how today's HRV compares to 7-day rolling baseline.
     * ≥ 100% baseline → 1.0
     * 70% baseline    → 0.5 (recovery threshold)
     * < 50% baseline  → 0.0
     */
    private fun computeHrvScore(snapshot: BiometricSnapshot, baseline: BaselineData): Double {
        val todayHrv = snapshot.hrv?.sdnn ?: return 0.5 // no data → neutral
        if (baseline.avgHrv <= 0) return 0.5
        val ratio = todayHrv / baseline.avgHrv
        return when {
            ratio >= 1.0  -> 1.0
            ratio >= 0.85 -> 0.7 + (ratio - 0.85) / 0.15 * 0.3
            ratio >= 0.70 -> 0.4 + (ratio - 0.70) / 0.15 * 0.3
            ratio >= 0.50 -> (ratio - 0.50) / 0.20 * 0.4
            else          -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    /**
     * Sleep score: duration + quality bonus for deep/REM.
     * ≥ target hours = max score (plus quality can add/subtract)
     */
    private fun computeSleepScore(snapshot: BiometricSnapshot, targetHours: Double): Double {
        val sleep = snapshot.sleep ?: return 0.4 // missing = low
        val durationScore = min(1.0, sleep.durationHours / targetHours)

        // Quality bonus: deep + REM should be ≥ 40% of total sleep
        val totalMinutes = sleep.durationMinutes.toDouble()
        val qualityMinutes = (sleep.deepMinutes + sleep.remMinutes).toDouble()
        val qualityRatio = if (totalMinutes > 0) qualityMinutes / totalMinutes else 0.0
        val qualityBonus = when {
            qualityRatio >= 0.40 -> 0.2
            qualityRatio >= 0.25 -> 0.1
            else                 -> 0.0
        }

        return min(1.0, durationScore * 0.8 + qualityBonus)
    }

    /**
     * RHR score: lower vs baseline is better. ≥110% baseline → 0.0
     */
    private fun computeRhrScore(snapshot: BiometricSnapshot, baseline: BaselineData): Double {
        val rhr = snapshot.restingHeartRate?.toDouble() ?: return 0.5
        if (baseline.avgRestingHr <= 0) return 0.5
        val ratio = rhr / baseline.avgRestingHr
        return when {
            ratio <= 0.90 -> 1.0
            ratio <= 1.00 -> 1.0 - (ratio - 0.90) / 0.10 * 0.3
            ratio <= 1.10 -> 0.7 - (ratio - 1.00) / 0.10 * 0.7
            else          -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    /**
     * Activity/recovery score: steps today vs baseline.
     * Too high (>150% baseline) on a low-HRV day penalizes score.
     */
    private fun computeActivityScore(snapshot: BiometricSnapshot, baseline: BaselineData): Double {
        val steps = snapshot.steps ?: return 0.5
        if (baseline.avgDailySteps <= 0) return 0.5
        val ratio = steps.toDouble() / baseline.avgDailySteps
        return when {
            ratio in 0.8..1.2 -> 1.0   // on target
            ratio < 0.5       -> 0.5   // very sedentary
            ratio > 1.5       -> 0.7   // overactive (might be fine, partial credit)
            else              -> 0.75
        }
    }

    private fun scoreToLabel(score: Int): ReadinessLabel = when (score) {
        9, 10 -> ReadinessLabel.PEAK
        7, 8  -> ReadinessLabel.HIGH
        5, 6  -> ReadinessLabel.MODERATE
        3, 4  -> ReadinessLabel.LOW
        else  -> ReadinessLabel.RECOVERY
    }
}
