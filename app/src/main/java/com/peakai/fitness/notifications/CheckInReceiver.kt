package com.peakai.fitness.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.peakai.fitness.notifications.workers.CheckInWorker
import com.peakai.fitness.notifications.workers.MorningBriefingWorker
import java.util.concurrent.TimeUnit

/**
 * Reschedules periodic workers after boot.
 */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("CheckInReceiver", "Boot completed — rescheduling workers")
            scheduleWorkers(context)
        }
    }

    companion object {
        fun scheduleWorkers(context: Context) {
            val wm = WorkManager.getInstance(context)

            // Morning briefing — daily at ~7am (WorkManager doesn't guarantee exact time, use alarm for precision)
            wm.enqueueUniquePeriodicWork(
                "morning_briefing",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<MorningBriefingWorker>(24, TimeUnit.HOURS)
                    .build()
            )

            // Check-ins — every 4 hours
            wm.enqueueUniquePeriodicWork(
                "check_in",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CheckInWorker>(4, TimeUnit.HOURS)
                    .build()
            )
        }
    }
}
