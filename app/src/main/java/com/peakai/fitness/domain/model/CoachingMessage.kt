package com.peakai.fitness.domain.model

import java.time.Instant

data class CoachingMessage(
    val id: Long = 0,
    val type: MessageType,
    val content: String,
    val actionItems: List<String> = emptyList(),
    val timestamp: Instant = Instant.now(),
    val source: MessageSource = MessageSource.RULE_BASED
) {
    enum class MessageType {
        MORNING_BRIEFING,
        CHECK_IN,
        COACH_RESPONSE,
        NOTIFICATION,
        ALERT
    }

    enum class MessageSource {
        GEMINI_NANO,
        RULE_BASED
    }
}

data class CoachConversation(
    val messages: List<CoachMessage> = emptyList()
)

data class CoachMessage(
    val role: Role,
    val content: String,
    val timestamp: Instant = Instant.now()
) {
    enum class Role { USER, COACH }
}
