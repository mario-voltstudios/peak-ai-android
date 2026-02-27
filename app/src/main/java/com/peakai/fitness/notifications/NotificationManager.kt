package com.peakai.fitness.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peakai.fitness.MainActivity
import com.peakai.fitness.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeakNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_COACHING = "peak_coaching"
        const val CHANNEL_HYDRATION = "peak_hydration"
        const val CHANNEL_CHECKIN = "peak_checkin"

        const val NOTIF_MORNING_BRIEFING = 1001
        const val NOTIF_HYDRATION = 1002
        const val NOTIF_CHECKIN = 1003
        const val NOTIF_ALERT = 1004
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_COACHING,
                    "Peak AI Coach",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Morning briefings and coaching insights"
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_HYDRATION,
                    "Hydration Reminders",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Hydration reminders throughout the day"
                },
                NotificationChannel(
                    CHANNEL_CHECKIN,
                    "Daily Check-Ins",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Midday and evening check-in reminders"
                }
            )
        )
    }

    fun showMorningBriefing(title: String, body: String) {
        notify(
            id = NOTIF_MORNING_BRIEFING,
            channel = CHANNEL_COACHING,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showHydrationReminder() {
        notify(
            id = NOTIF_HYDRATION,
            channel = CHANNEL_HYDRATION,
            title = "💧 Hydration check",
            body = "No water logged yet this morning. Drink a glass — your body will thank you.",
            priority = NotificationCompat.PRIORITY_LOW
        )
    }

    fun showCheckIn(message: String) {
        notify(
            id = NOTIF_CHECKIN,
            channel = CHANNEL_CHECKIN,
            title = "Peak AI Check-In",
            body = message,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showAlert(message: String) {
        notify(
            id = NOTIF_ALERT,
            channel = CHANNEL_COACHING,
            title = "⚠️ Peak AI Alert",
            body = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    private fun notify(id: Int, channel: String, title: String, body: String, priority: Int) {
        if (!hasPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_peak_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
