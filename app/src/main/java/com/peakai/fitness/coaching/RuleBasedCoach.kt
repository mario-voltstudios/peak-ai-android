package com.peakai.fitness.coaching

import com.peakai.fitness.domain.model.BaselineData
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.CoachingMessage
import com.peakai.fitness.domain.model.DailyLogSummary
import com.peakai.fitness.domain.model.ReadinessScore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based coaching fallback — fires when Gemini Nano is unavailable.
 * Deterministic, private, offline-first.
 */
@Singleton
class RuleBasedCoach @Inject constructor() {

    fun generateMorningBriefing(
        readiness: ReadinessScore,
        snapshot: BiometricSnapshot,
        baseline: BaselineData,
        logSummary: DailyLogSummary
    ): CoachingMessage {
        val lines = mutableListOf<String>()
        val actions = mutableListOf<String>()

        // Headline
        val headline = when (readiness.label) {
            ReadinessScore.ReadinessLabel.PEAK ->
                "You're firing on all cylinders today. Score: ${readiness.score}/10. Push hard."
            ReadinessScore.ReadinessLabel.HIGH ->
                "Strong readiness. Score: ${readiness.score}/10. Good day to train."
            ReadinessScore.ReadinessLabel.MODERATE ->
                "Moderate readiness. Score: ${readiness.score}/10. Steady, intentional effort."
            ReadinessScore.ReadinessLabel.LOW ->
                "Low readiness. Score: ${readiness.score}/10. Manage energy carefully."
            ReadinessScore.ReadinessLabel.RECOVERY ->
                "Recovery day. Score: ${readiness.score}/10. Your body is asking for rest."
        }
        lines.add(headline)

        // HRV insight
        snapshot.hrv?.let { hrv ->
            if (baseline.avgHrv > 0) {
                val ratio = hrv.sdnn / baseline.avgHrv
                when {
                    ratio < 0.70 -> {
                        lines.add("HRV is ${String.format("%.0f", hrv.sdnn)}ms — ${String.format("%.0f", (1 - ratio) * 100)}% below your baseline.")
                        actions.add("Recovery day. Light movement only. Prioritize sleep tonight.")
                    }
                    ratio < 0.85 -> {
                        lines.add("HRV slightly suppressed at ${String.format("%.0f", hrv.sdnn)}ms.")
                        actions.add("Moderate intensity only. No new PRs today.")
                    }
                    else -> {
                        lines.add("HRV healthy at ${String.format("%.0f", hrv.sdnn)}ms.")
                    }
                }
            }
        }

        // Sleep insight
        snapshot.sleep?.let { sleep ->
            when {
                sleep.durationHours < 6.0 -> {
                    lines.add("Short sleep: ${String.format("%.1f", sleep.durationHours)}h logged.")
                    actions.add("Sleep deficit. Delay caffeine 90min after waking. Front-load protein.")
                }
                sleep.durationHours < 7.0 -> {
                    lines.add("Sleep: ${String.format("%.1f", sleep.durationHours)}h — slightly under target.")
                    actions.add("Add a 20-min nap if possible. Avoid screens after 9pm.")
                }
                else -> {
                    lines.add("Sleep: ${String.format("%.1f", sleep.durationHours)}h. Well rested.")
                }
            }
        } ?: run {
            lines.add("No sleep data found. Log your sleep in Health Connect for better insights.")
        }

        // RHR insight
        snapshot.restingHeartRate?.let { rhr ->
            if (baseline.avgRestingHr > 0) {
                val ratio = rhr / baseline.avgRestingHr
                if (ratio > 1.10) {
                    lines.add("Resting HR elevated: ${rhr}bpm vs baseline ${baseline.avgRestingHr.toInt()}bpm.")
                    actions.add("Elevated resting HR. Possible illness or stress. Monitor closely.")
                }
            }
        }

        // Caffeine check
        if (logSummary.caffeineMg > 300) {
            actions.add("Caffeine cap reached (${logSummary.caffeineMg}mg). Switch to water.")
        }

        // Pad to 3 action items
        while (actions.size < 3) {
            val fillers = listOf(
                "Hydrate first: 500ml water before coffee.",
                "5-minute breathing exercise to anchor your day.",
                "Set your top-3 priorities before checking messages.",
                "Get sunlight exposure in the first hour of your day.",
                "A short walk after lunch boosts afternoon focus."
            )
            val next = fillers.firstOrNull { it !in actions } ?: break
            actions.add(next)
        }

        return CoachingMessage(
            type = CoachingMessage.MessageType.MORNING_BRIEFING,
            content = lines.joinToString("\n\n"),
            actionItems = actions.take(3),
            source = CoachingMessage.MessageSource.RULE_BASED
        )
    }

    fun generateCheckIn(
        snapshot: BiometricSnapshot,
        logSummary: DailyLogSummary,
        hourOfDay: Int
    ): CoachingMessage {
        val message = buildString {
            when {
                hourOfDay in 11..13 -> {
                    appendLine("Midday check-in.")
                    if (logSummary.waterMl < 500) appendLine("Hydration low — drink a glass now.")
                    if (logSummary.caffeineMg > 200) appendLine("Limit additional caffeine — you're at ${logSummary.caffeineMg}mg.")
                    appendLine("Step away from screens for 5 minutes. Your focus will thank you.")
                }
                hourOfDay in 14..17 -> {
                    appendLine("Afternoon check-in.")
                    appendLine("Energy dip is normal here — try movement, not another coffee.")
                    if (logSummary.waterMl < 1000) appendLine("Water target: aim for ${2000 - logSummary.waterMl}ml more today.")
                }
                hourOfDay >= 20 -> {
                    appendLine("Evening wind-down.")
                    appendLine("Dim lights, cut screens at least 1h before sleep.")
                    if (logSummary.caffeineMg > 100) appendLine("No more caffeine — it's in your system for 5–6h.")
                    appendLine("Tomorrow's readiness starts with tonight's sleep.")
                }
                else -> {
                    appendLine("You're doing great. Keep moving forward.")
                }
            }
        }

        return CoachingMessage(
            type = CoachingMessage.MessageType.CHECK_IN,
            content = message.trim(),
            source = CoachingMessage.MessageSource.RULE_BASED
        )
    }

    fun respondToQuery(
        query: String,
        snapshot: BiometricSnapshot,
        readiness: ReadinessScore,
        logSummary: DailyLogSummary
    ): CoachingMessage {
        val lower = query.lowercase()
        val response = when {
            "hrv" in lower ->
                buildHrvResponse(snapshot, readiness)
            "sleep" in lower ->
                buildSleepResponse(snapshot)
            "caffeine" in lower || "coffee" in lower ->
                "You've had ${logSummary.caffeineMg}mg caffeine today. " +
                if (logSummary.caffeineOverLimit) "You're over the 300mg threshold. Stop here."
                else "You have ${300 - logSummary.caffeineMg}mg headroom."
            "water" in lower || "hydrat" in lower ->
                "You've logged ${logSummary.waterMl}ml today (~${logSummary.waterGlasses} glasses). " +
                "Target is 2,000–2,500ml."
            "workout" in lower || "train" in lower || "exercise" in lower ->
                buildWorkoutRecommendation(readiness)
            "recover" in lower || "rest" in lower ->
                "Readiness is ${readiness.score}/10. " +
                if (readiness.score <= 4) "Yes — take it easy today. Active recovery only."
                else "You've got enough in the tank for a moderate session."
            "score" in lower || "readiness" in lower ->
                "Your readiness today is ${readiness.score}/10 (${readiness.label.name.lowercase().replace('_', ' ')}). " +
                "HRV: ${String.format("%.0f", readiness.hrvScore * 100)}% | " +
                "Sleep: ${String.format("%.0f", readiness.sleepScore * 100)}% | " +
                "RHR: ${String.format("%.0f", readiness.rhrScore * 100)}%"
            else ->
                "Based on your ${readiness.score}/10 readiness, ${
                    when {
                        readiness.score >= 7 -> "you're in good shape. Stay hydrated and stay consistent."
                        readiness.score >= 5 -> "manage your energy. Don't overcommit today."
                        else                 -> "recovery is the priority. Rest, hydrate, sleep well tonight."
                    }
                }"
        }

        return CoachingMessage(
            type = CoachingMessage.MessageType.COACH_RESPONSE,
            content = response,
            source = CoachingMessage.MessageSource.RULE_BASED
        )
    }

    private fun buildHrvResponse(snapshot: BiometricSnapshot, readiness: ReadinessScore): String {
        val hrv = snapshot.hrv ?: return "No HRV data logged yet. Wear your device overnight for accurate HRV."
        return "Your HRV is ${String.format("%.0f", hrv.sdnn)}ms. " +
            when (readiness.label) {
                ReadinessScore.ReadinessLabel.PEAK,
                ReadinessScore.ReadinessLabel.HIGH -> "Above your baseline — your nervous system is well recovered."
                ReadinessScore.ReadinessLabel.MODERATE -> "Near baseline. Normal day."
                ReadinessScore.ReadinessLabel.LOW,
                ReadinessScore.ReadinessLabel.RECOVERY -> "Below baseline — your body wants rest more than training."
            }
    }

    private fun buildSleepResponse(snapshot: BiometricSnapshot): String {
        val sleep = snapshot.sleep ?: return "No sleep data found. Ensure Health Connect has sleep tracking enabled."
        return "Last night: ${String.format("%.1f", sleep.durationHours)}h total. " +
            "Deep: ${sleep.deepMinutes}min, REM: ${sleep.remMinutes}min. " +
            when {
                sleep.durationHours >= 8.0 -> "Excellent. You're fully charged."
                sleep.durationHours >= 7.0 -> "Solid night. Good foundation for the day."
                sleep.durationHours >= 6.0 -> "Slightly under. Make tonight count."
                else                       -> "Sleep deficit detected. Prioritize recovery."
            }
    }

    private fun buildWorkoutRecommendation(readiness: ReadinessScore): String = when {
        readiness.score >= 9 -> "Peak day. Go hard — heavy compound lifts, intense cardio, new PRs. You're ready."
        readiness.score >= 7 -> "Good training day. 75–85% intensity. Focus on form and progression."
        readiness.score >= 5 -> "Moderate session. Aerobic work, skill practice, or hypertrophy volume."
        readiness.score >= 3 -> "Low-intensity only. Walk, yoga, mobility work. No heavy loading."
        else                 -> "Rest day. Seriously — movement only if it feels restorative."
    }
}
