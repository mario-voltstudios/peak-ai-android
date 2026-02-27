package com.peakai.fitness.domain.model

import java.time.Instant

/**
 * User-logged wellness entries — water, caffeine, supplements, medications.
 */
sealed class LogEntry {
    abstract val id: Long
    abstract val timestamp: Instant

    data class Water(
        override val id: Long = 0,
        override val timestamp: Instant = Instant.now(),
        val amountMl: Int                        // e.g. 250 = one glass
    ) : LogEntry()

    data class Caffeine(
        override val id: Long = 0,
        override val timestamp: Instant = Instant.now(),
        val amountMg: Int,                       // mg — espresso ~65mg, drip coffee ~95mg
        val source: String = ""                  // "coffee", "tea", "pre-workout", etc.
    ) : LogEntry()

    data class Supplement(
        override val id: Long = 0,
        override val timestamp: Instant = Instant.now(),
        val name: String,
        val dosage: String = ""
    ) : LogEntry()

    data class Medication(
        override val id: Long = 0,
        override val timestamp: Instant = Instant.now(),
        val name: String,
        val dosage: String = ""
    ) : LogEntry()
}

/**
 * Daily totals computed from log entries.
 */
data class DailyLogSummary(
    val waterMl: Int,
    val caffeineMg: Int,
    val supplements: List<String>,
    val medications: List<String>
) {
    val waterGlasses: Int get() = waterMl / 250
    val caffeineOverLimit: Boolean get() = caffeineMg > 300
}
