package com.peakai.fitness.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type") val type: String,         // WATER, CAFFEINE, SUPPLEMENT, MEDICATION
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "amount_int") val amountInt: Int = 0,  // ml for water, mg for caffeine
    @ColumnInfo(name = "name") val name: String = "",    // for supplements/medications
    @ColumnInfo(name = "extra") val extra: String = ""   // JSON string for misc fields
)

@Entity(tableName = "coaching_messages")
data class CoachingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "action_items") val actionItems: String = "", // pipe-separated
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "source") val source: String
)

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    @Query("SELECT * FROM log_entries WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    fun observeRange(startMs: Instant, endMs: Instant): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    suspend fun getRange(startMs: Instant, endMs: Instant): List<LogEntryEntity>

    @Query("SELECT SUM(amount_int) FROM log_entries WHERE type = 'WATER' AND timestamp >= :start AND timestamp < :end")
    suspend fun getTotalWaterMl(start: Instant, end: Instant): Int?

    @Query("SELECT SUM(amount_int) FROM log_entries WHERE type = 'CAFFEINE' AND timestamp >= :start AND timestamp < :end")
    suspend fun getTotalCaffeineMg(start: Instant, end: Instant): Int?

    @Query("DELETE FROM log_entries WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant)
}

@Dao
interface CoachingDao {
    @Insert
    suspend fun insert(message: CoachingMessageEntity): Long

    @Query("SELECT * FROM coaching_messages ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<CoachingMessageEntity>>

    @Query("SELECT * FROM coaching_messages WHERE type = 'MORNING_BRIEFING' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBriefing(): CoachingMessageEntity?
}
