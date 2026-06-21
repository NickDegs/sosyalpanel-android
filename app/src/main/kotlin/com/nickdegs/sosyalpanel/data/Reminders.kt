package com.nickdegs.sosyalpanel.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

// Planlanmış paylaşımlar için yerel hatırlatma (AlarmManager + bildirim).
object ReminderScheduler {
    private const val CHANNEL = "post_reminders"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Paylaşım Hatırlatmaları", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun intent(context: Context, id: Long, title: String, body: String): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", id); putExtra("title", title); putExtra("body", body)
        }
        return PendingIntent.getBroadcast(
            context, id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, id: Long, platform: String, note: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        ensureChannel(context)
        val title = "Paylaşım zamanı · $platform"
        val body = note.ifBlank { "Planladığın içeriği paylaşmayı unutma." }
        val am = context.getSystemService(AlarmManager::class.java)
        // İnexact alarm → SCHEDULE_EXACT_ALARM izni gerekmez (Play güvenli).
        am.set(AlarmManager.RTC_WAKEUP, atMillis, intent(context, id, title, body))
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(intent(context, id, "", ""))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.ensureChannel(context)
        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title") ?: "Paylaşım zamanı"
        val body = intent.getStringExtra("body") ?: ""
        val notif = NotificationCompat.Builder(context, "post_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            context.getSystemService(NotificationManager::class.java).notify(id.toInt(), notif)
        }
    }
}
