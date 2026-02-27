package com.peakai.fitness.data.repository

import com.peakai.fitness.coaching.GeminiNanoCoach
import com.peakai.fitness.coaching.GeminiUnavailableException
import com.peakai.fitness.coaching.RuleBasedCoach
import com.peakai.fitness.data.health.HealthConnectManager
import com.peakai.fitness.data.local.CoachingDao
import com.peakai.fitness.data.local.CoachingMessageEntity
import com.peakai.fitness.data.local.LogDao
import com.peakai.fitness.data.local.LogEntryEntity
import com.peakai.fitness.domain.model.BaselineData
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.CoachingMessage
import com.peakai.fitness.domain.model.DailyLogSummary
import com.peakai.fitness.domain.model.LogEntry
import com.peakai.fitness.domain.model.ReadinessScore
import com.peakai.fitness.domain.usecase.ComputeReadinessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    val healthConnect: HealthConnectManager,
    private val logDao: LogDao,
    private val coachingDao: CoachingDao,
    private val computeReadiness: ComputeReadinessUseCase,
    private val geminiCoach: GeminiNanoCoach,
    private val ruleCoach: RuleBasedCoach
) {

    // ── Health Connect ───────────────────────────────────────────────────────

    suspend fun getTodaySnapshot(): BiometricSnapshot = healthConnect.readTodaySnapshot()

    suspend fun getSevenDayBaseline(): BaselineData = healthConnect.readSevenDayBaseline()

    suspend fun getReadinessScore(
        snapshot: BiometricSnapshot = getTodaySnapshot(),
        baseline: BaselineData = getSevenDayBaseline()
    ): ReadinessScore = computeReadiness(snapshot, baseline)

    // ── Logging ──────────────────────────────────────────────────────────────

    suspend fun logWater(amountMl: Int) {
        logDao.insert(LogEntryEntity(type = "WATER", timestamp = Instant.now(), amountInt = amountMl))
    }

    suspend fun logCaffeine(amountMg: Int, source: String = "") {
        logDao.insert(LogEntryEntity(type = "CAFFEINE", timestamp = Instant.now(), amountInt = amountMg, extra = source))
    }

    suspend fun logSupplement(name: String, dosage: String = "") {
        logDao.insert(LogEntryEntity(type = "SUPPLEMENT", timestamp = Instant.now(), name = name, extra = dosage))
    }

    suspend fun logMedication(name: String, dosage: String = "") {
        logDao.insert(LogEntryEntity(type = "MEDICATION", timestamp = Instant.now(), name = name, extra = dosage))
    }

    suspend fun getDailySummary(date: LocalDate = LocalDate.now()): DailyLogSummary {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val waterMl = logDao.getTotalWaterMl(start, end) ?: 0
        val caffeineMg = logDao.getTotalCaffeineMg(start, end) ?: 0
        val entries = logDao.getRange(start, end)

        return DailyLogSummary(
            waterMl = waterMl,
            caffeineMg = caffeineMg,
            supplements = entries.filter { it.type == "SUPPLEMENT" }.map { it.name },
            medications = entries.filter { it.type == "MEDICATION" }.map { it.name }
        )
    }

    fun observeTodayLogs(): Flow<DailyLogSummary> {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return logDao.observeRange(start, end).map { entries ->
            val water = entries.filter { it.type == "WATER" }.sumOf { it.amountInt }
            val caffeine = entries.filter { it.type == "CAFFEINE" }.sumOf { it.amountInt }
            DailyLogSummary(
                waterMl = water,
                caffeineMg = caffeine,
                supplements = entries.filter { it.type == "SUPPLEMENT" }.map { it.name },
                medications = entries.filter { it.type == "MEDICATION" }.map { it.name }
            )
        }
    }

    // ── Coaching ─────────────────────────────────────────────────────────────

    suspend fun getMorningBriefing(): CoachingMessage {
        val snapshot = getTodaySnapshot()
        val baseline = getSevenDayBaseline()
        val readiness = computeReadiness(snapshot, baseline)
        val logSummary = getDailySummary()

        val message = try {
            geminiCoach.generateMorningBriefing(readiness, snapshot, logSummary)
        } catch (_: GeminiUnavailableException) {
            ruleCoach.generateMorningBriefing(readiness, snapshot, baseline, logSummary)
        }

        // Persist
        coachingDao.insert(message.toEntity())
        return message
    }

    suspend fun askCoach(
        query: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): CoachingMessage {
        val snapshot = getTodaySnapshot()
        val baseline = getSevenDayBaseline()
        val readiness = computeReadiness(snapshot, baseline)
        val logSummary = getDailySummary()

        return try {
            geminiCoach.askCoach(query, readiness, snapshot, logSummary, conversationHistory)
        } catch (_: GeminiUnavailableException) {
            ruleCoach.respondToQuery(query, snapshot, readiness, logSummary)
        }
    }

    fun observeRecentMessages(): Flow<List<CoachingMessageEntity>> =
        coachingDao.observeRecent(20)

    // ── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun pruneOldLogs(daysToKeep: Int = 30) {
        val cutoff = Instant.now().minusSeconds(daysToKeep * 24 * 3600L)
        logDao.deleteOlderThan(cutoff)
    }
}

private fun CoachingMessage.toEntity() = CoachingMessageEntity(
    type = type.name,
    content = content,
    actionItems = actionItems.joinToString("|"),
    timestamp = timestamp,
    source = source.name
)
