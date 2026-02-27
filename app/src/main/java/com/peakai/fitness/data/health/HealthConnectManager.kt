package com.peakai.fitness.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.peakai.fitness.domain.model.BaselineData
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.HeartRateSample
import com.peakai.fitness.domain.model.HrvData
import com.peakai.fitness.domain.model.SleepData
import com.peakai.fitness.domain.model.SleepStage
import com.peakai.fitness.domain.model.SleepStageType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )

    /**
     * Check which permissions are currently granted.
     */
    suspend fun checkPermissions(): Set<String> = withContext(Dispatchers.IO) {
        try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = checkPermissions()
        return permissions.all { it in granted }
    }

    /**
     * Read today's biometric snapshot from Health Connect.
     */
    suspend fun readTodaySnapshot(): BiometricSnapshot = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

        val hrv = readLatestHrv(startOfDay, now)
        val rhr = readRestingHeartRate(startOfDay, now)
        val sleep = readLastNightSleep(now)
        val spo2 = readLatestSpo2(startOfDay, now)
        val steps = readSteps(startOfDay, now)
        val heartRate = readHeartRateSamples(startOfDay, now)

        BiometricSnapshot(
            timestamp = now,
            hrv = hrv,
            restingHeartRate = rhr,
            sleep = sleep,
            spo2 = spo2,
            steps = steps,
            heartRateSamples = heartRate
        )
    }

    /**
     * Read 7-day rolling baseline (excludes today).
     */
    suspend fun readSevenDayBaseline(): BaselineData = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val sevenDaysAgo = now.minusSeconds(7 * 24 * 3600)

        val hrvList = mutableListOf<Double>()
        val rhrList = mutableListOf<Double>()
        val sleepList = mutableListOf<Double>()
        val stepsList = mutableListOf<Long>()

        // HRV over 7 days
        try {
            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sevenDaysAgo, now)
                )
            )
            records.records.forEach { hr -> hrvList.add(hr.heartRateVariabilityMillis) }
        } catch (_: Exception) {}

        // RHR over 7 days
        try {
            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sevenDaysAgo, now)
                )
            )
            records.records.forEach { r -> rhrList.add(r.beatsPerMinute.toDouble()) }
        } catch (_: Exception) {}

        // Sleep — compute per-day duration
        for (dayOffset in 1..7) {
            val dayEnd = now.minusSeconds((dayOffset - 1) * 24 * 3600L)
            val dayStart = now.minusSeconds(dayOffset * 24 * 3600L)
            try {
                val records = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                    )
                )
                val totalMin = records.records.sumOf {
                    (it.endTime.epochSecond - it.startTime.epochSecond) / 60
                }
                if (totalMin > 0) sleepList.add(totalMin / 60.0)
            } catch (_: Exception) {}
        }

        // Steps over 7 days
        try {
            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sevenDaysAgo, now)
                )
            )
            // Group by day
            val byDay = records.records.groupBy {
                LocalDate.ofInstant(it.startTime, ZoneId.systemDefault())
            }
            byDay.values.forEach { daySteps ->
                stepsList.add(daySteps.sumOf { it.count })
            }
        } catch (_: Exception) {}

        BaselineData(
            avgHrv = if (hrvList.isNotEmpty()) hrvList.average() else 0.0,
            avgRestingHr = if (rhrList.isNotEmpty()) rhrList.average() else 0.0,
            avgSleepHours = if (sleepList.isNotEmpty()) sleepList.average() else 0.0,
            avgDailySteps = if (stepsList.isNotEmpty()) stepsList.average().toLong() else 0L,
            sampleDays = minOf(hrvList.size, 7)
        )
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private suspend fun readLatestHrv(start: Instant, end: Instant): HrvData? = try {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.maxByOrNull { it.time }?.let { rec ->
            // RMSSD is a proxy for SDNN in Wear OS — report as sdnn for display
            HrvData(
                sdnn = rec.heartRateVariabilityMillis,
                rmssd = rec.heartRateVariabilityMillis,
                timestamp = rec.time
            )
        }
    } catch (e: Exception) { null }

    private suspend fun readRestingHeartRate(start: Instant, end: Instant): Int? = try {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
    } catch (e: Exception) { null }

    private suspend fun readLastNightSleep(end: Instant): SleepData? = try {
        // Look back 18h for last sleep session
        val start = end.minusSeconds(18 * 3600L)
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.maxByOrNull { it.endTime }?.let { session ->
            val durationMin = (session.endTime.epochSecond - session.startTime.epochSecond) / 60
            val stages = session.stages.map { stage ->
                SleepStage(
                    type = mapSleepStage(stage.stage),
                    durationMinutes = (stage.endTime.epochSecond - stage.startTime.epochSecond) / 60
                )
            }
            SleepData(
                durationMinutes = durationMin,
                stages = stages,
                startTime = session.startTime,
                endTime = session.endTime
            )
        }
    } catch (e: Exception) { null }

    private fun mapSleepStage(stage: Int): SleepStageType = when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
        SleepSessionRecord.STAGE_TYPE_DEEP  -> SleepStageType.DEEP
        SleepSessionRecord.STAGE_TYPE_REM   -> SleepStageType.REM
        else                                -> SleepStageType.UNKNOWN
    }

    private suspend fun readLatestSpo2(start: Instant, end: Instant): Double? = try {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.maxByOrNull { it.time }?.percentage?.value
    } catch (e: Exception) { null }

    private suspend fun readSteps(start: Instant, end: Instant): Long? = try {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.sumOf { it.count }.takeIf { it > 0 }
    } catch (e: Exception) { null }

    private suspend fun readHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> = try {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        records.records.flatMap { record ->
            record.samples.map { sample ->
                HeartRateSample(bpm = sample.beatsPerMinute.toInt(), timestamp = sample.time)
            }
        }
    } catch (e: Exception) { emptyList() }
}
