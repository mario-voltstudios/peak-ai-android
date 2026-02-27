package com.peakai.fitness.domain.model

import java.time.Instant

/**
 * Core biometric snapshot pulled from Health Connect.
 * All timestamps are UTC Instant — no timezone conversion until display layer.
 */
data class BiometricSnapshot(
    val timestamp: Instant = Instant.now(),
    val hrv: HrvData? = null,
    val restingHeartRate: Int? = null,          // bpm
    val sleep: SleepData? = null,
    val spo2: Double? = null,                   // percentage 0–100
    val steps: Long? = null,
    val heartRateSamples: List<HeartRateSample> = emptyList()
)

data class HrvData(
    val sdnn: Double,                            // ms — standard dev of NN intervals
    val rmssd: Double? = null,                  // ms — root mean square of successive differences
    val timestamp: Instant
)

data class SleepData(
    val durationMinutes: Long,
    val stages: List<SleepStage> = emptyList(),
    val startTime: Instant,
    val endTime: Instant
) {
    val durationHours: Double get() = durationMinutes / 60.0

    val deepMinutes: Long get() =
        stages.filter { it.type == SleepStageType.DEEP }.sumOf { it.durationMinutes }

    val remMinutes: Long get() =
        stages.filter { it.type == SleepStageType.REM }.sumOf { it.durationMinutes }
}

data class SleepStage(
    val type: SleepStageType,
    val durationMinutes: Long
)

enum class SleepStageType {
    AWAKE, LIGHT, DEEP, REM, UNKNOWN
}

data class HeartRateSample(
    val bpm: Int,
    val timestamp: Instant
)

/**
 * 7-day rolling baseline for readiness algorithm.
 */
data class BaselineData(
    val avgHrv: Double,                          // ms
    val avgRestingHr: Double,                    // bpm
    val avgSleepHours: Double,
    val avgDailySteps: Long,
    val sampleDays: Int                          // how many days of data we have
)

/**
 * Readiness score — 1-10 composite index.
 */
data class ReadinessScore(
    val score: Int,                              // 1–10
    val label: ReadinessLabel,
    val hrvScore: Double,                        // 0-1 component
    val sleepScore: Double,                      // 0-1 component
    val rhrScore: Double,                        // 0-1 component
    val activityScore: Double,                   // 0-1 component
    val timestamp: Instant = Instant.now()
) {
    enum class ReadinessLabel {
        PEAK,           // 9-10
        HIGH,           // 7-8
        MODERATE,       // 5-6
        LOW,            // 3-4
        RECOVERY        // 1-2
    }
}
