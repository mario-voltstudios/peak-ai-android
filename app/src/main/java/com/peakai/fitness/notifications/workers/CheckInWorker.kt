package com.peakai.fitness.notifications.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peakai.fitness.coaching.RuleBasedCoach
import com.peakai.fitness.data.repository.HealthRepository
import com.peakai.fitness.notifications.PeakNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime

@HiltWorker
class CheckInWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: HealthRepository,
    private val ruleBasedCoach: RuleBasedCoach,
    private val notificationManager: PeakNotificationManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val hour = LocalTime.now().hour
        // Skip early morning (briefing covers that) and midnight
        if (hour < 10 || hour > 22) return Result.success()

        return try {
            val snapshot = repository.getTodaySnapshot()
            val logSummary = repository.getDailySummary()

            val checkIn = ruleBasedCoach.generateCheckIn(snapshot, logSummary, hour)

            // Hydration alert: no water logged by 10am
            if (hour == 10 && logSummary.waterMl == 0) {
                notificationManager.showHydrationReminder()
            }

            // Caffeine over limit
            if (logSummary.caffeineOverLimit) {
                notificationManager.showAlert("Caffeine cap reached (${logSummary.caffeineMg}mg). Switch to water.")
            }

            notificationManager.showCheckIn(checkIn.content)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
