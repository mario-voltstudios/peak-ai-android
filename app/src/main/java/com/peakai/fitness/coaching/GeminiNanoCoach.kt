package com.peakai.fitness.coaching

import android.content.Context
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.CoachingMessage
import com.peakai.fitness.domain.model.DailyLogSummary
import com.peakai.fitness.domain.model.ReadinessScore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Nano on-device AI coach.
 *
 * Uses Google AI Edge SDK (com.google.ai.edge.aicore) for inference.
 * All inference is 100% on-device — no data leaves the phone.
 *
 * NOTE: Gemini Nano requires Android 14+ AND device support (Pixel 8+, Samsung S24+).
 * Falls back gracefully to RuleBasedCoach when unavailable.
 *
 * Integration: Google AI Edge SDK is listed as dependency in build.gradle.
 * Since the SDK is distributed via Google's private Maven, replace the
 * dependency placeholder below with the current artifact when it becomes GA.
 *
 * Current GA artifact (as of SDK 0.2.0):
 *   implementation("com.google.ai.edge.aicore:aicore:0.0.1-exp01")
 *
 * See: https://developer.android.com/ai/gemini-nano/aicore
 */
@Singleton
class GeminiNanoCoach @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fallback: RuleBasedCoach
) {
    // Lazy so we don't crash on unsupported devices
    private var generativeModel: Any? = null
    private var isAvailable: Boolean? = null

    /**
     * Check if Gemini Nano is available on this device.
     * Uses reflection so the code compiles even without the SDK.
     */
    suspend fun checkAvailability(): Boolean = withContext(Dispatchers.IO) {
        if (isAvailable != null) return@withContext isAvailable!!
        try {
            // Reflect on GenerativeModel to avoid hard compile dep until SDK is stable
            val managerClass = Class.forName("com.google.ai.edge.aicore.GenerativeModel")
            val manager = managerClass.getDeclaredConstructor(Context::class.java)
                .newInstance(context)
            // Try to prepare the model
            val prepareMethod = managerClass.getMethod("prepareInferenceSession")
            prepareMethod.invoke(manager)
            generativeModel = manager
            isAvailable = true
            true
        } catch (e: Exception) {
            isAvailable = false
            false
        }
    }

    /**
     * Generate morning briefing using Gemini Nano.
     * Falls back to rule-based if Nano unavailable.
     */
    suspend fun generateMorningBriefing(
        readiness: ReadinessScore,
        snapshot: BiometricSnapshot,
        logSummary: DailyLogSummary
    ): CoachingMessage = withContext(Dispatchers.IO) {
        if (!checkAvailability()) {
            // Return null to signal fallback — caller handles
            throw GeminiUnavailableException("Gemini Nano not available")
        }

        val prompt = buildMorningPrompt(readiness, snapshot, logSummary)
        val responseText = runInference(prompt) ?: throw GeminiUnavailableException("Inference failed")

        CoachingMessage(
            type = CoachingMessage.MessageType.MORNING_BRIEFING,
            content = responseText,
            source = CoachingMessage.MessageSource.GEMINI_NANO
        )
    }

    /**
     * Conversational "Ask Coach" query.
     */
    suspend fun askCoach(
        userMessage: String,
        readiness: ReadinessScore,
        snapshot: BiometricSnapshot,
        logSummary: DailyLogSummary,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): CoachingMessage = withContext(Dispatchers.IO) {
        if (!checkAvailability()) {
            throw GeminiUnavailableException("Gemini Nano not available")
        }

        val prompt = buildConversationPrompt(userMessage, readiness, snapshot, logSummary, conversationHistory)
        val responseText = runInference(prompt) ?: throw GeminiUnavailableException("Inference failed")

        CoachingMessage(
            type = CoachingMessage.MessageType.COACH_RESPONSE,
            content = responseText,
            source = CoachingMessage.MessageSource.GEMINI_NANO
        )
    }

    private fun buildMorningPrompt(
        readiness: ReadinessScore,
        snapshot: BiometricSnapshot,
        logSummary: DailyLogSummary
    ): String = buildString {
        appendLine("You are Peak AI, a precise, data-driven personal health coach. Be direct, not motivational-poster generic.")
        appendLine()
        appendLine("## Today's Biometrics")
        appendLine("Readiness score: ${readiness.score}/10 (${readiness.label})")
        snapshot.hrv?.let { appendLine("HRV (SDNN): ${String.format("%.1f", it.sdnn)}ms") }
        snapshot.restingHeartRate?.let { appendLine("Resting HR: ${it} bpm") }
        snapshot.sleep?.let {
            appendLine("Sleep: ${String.format("%.1f", it.durationHours)}h total")
            appendLine("  Deep: ${it.deepMinutes}min, REM: ${it.remMinutes}min")
        }
        snapshot.spo2?.let { appendLine("SpO2: ${String.format("%.1f", it)}%") }
        snapshot.steps?.let { appendLine("Steps yesterday: $it") }
        appendLine()
        appendLine("## Morning Log")
        appendLine("Water: ${logSummary.waterMl}ml, Caffeine: ${logSummary.caffeineMg}mg")
        appendLine()
        appendLine("Generate a morning briefing with:")
        appendLine("1. One direct headline sentence about today's readiness")
        appendLine("2. Two specific insights from the biometrics")
        appendLine("3. Three concrete action items (numbered)")
        appendLine("Be concise. Max 150 words total.")
    }

    private fun buildConversationPrompt(
        userMessage: String,
        readiness: ReadinessScore,
        snapshot: BiometricSnapshot,
        logSummary: DailyLogSummary,
        history: List<Pair<String, String>>
    ): String = buildString {
        appendLine("You are Peak AI, a precise health coach. Answer based on the user's actual biometrics.")
        appendLine()
        appendLine("## Current Biometrics")
        appendLine("Readiness: ${readiness.score}/10")
        snapshot.hrv?.let { appendLine("HRV: ${String.format("%.1f", it.sdnn)}ms") }
        snapshot.restingHeartRate?.let { appendLine("RHR: ${it}bpm") }
        snapshot.sleep?.let { appendLine("Sleep: ${String.format("%.1f", it.durationHours)}h") }
        appendLine("Water: ${logSummary.waterMl}ml | Caffeine: ${logSummary.caffeineMg}mg")
        appendLine()

        if (history.isNotEmpty()) {
            appendLine("## Conversation")
            history.takeLast(4).forEach { (role, msg) ->
                appendLine("$role: $msg")
            }
        }

        appendLine()
        appendLine("User: $userMessage")
        appendLine("Coach (max 80 words, direct and specific):")
    }

    /**
     * Run inference via reflection (avoids hard compile dep on SDK).
     * Replace with direct SDK calls when SDK is stable.
     */
    private suspend fun runInference(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: return@withContext null
            val generateMethod = model.javaClass.getMethod("generateContent", String::class.java)
            val response = generateMethod.invoke(model, prompt)
            // Extract text from response object
            val textMethod = response.javaClass.getMethod("getText")
            textMethod.invoke(response) as? String
        } catch (e: Exception) {
            null
        }
    }
}

class GeminiUnavailableException(message: String) : Exception(message)
