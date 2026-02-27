package com.peakai.fitness.notifications.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peakai.fitness.data.repository.HealthRepository
import com.peakai.fitness.notifications.PeakNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MorningBriefingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: HealthRepository,
    private val notificationManager: PeakNotificationManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val briefing = repository.getMorningBriefing()
            val logSummary = repository.getDailySummary()

            // Hydration check
            if (logSummary.waterMl == 0) {
                notificationManager.showHydrationReminder()
            }

            notificationManager.showMorningBriefing(
                title = "Good morning — Readiness briefing",
                body = briefing.content
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
