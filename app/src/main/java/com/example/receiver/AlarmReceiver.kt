package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.SavedUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val urlId = intent.getLongExtra("saved_url_id", -1L)
        val alarmType = intent.getStringExtra("alarm_type") ?: "REMINDER"

        Log.d("AlarmReceiver", "Received alarm! id=$urlId, type=$alarmType")

        if (urlId == -1L) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val urlDao = db.savedUrlDao()
                val savedUrl = urlDao.getSavedUrlById(urlId)

                if (savedUrl == null) {
                    Log.d("AlarmReceiver", "URL not found in DB: $urlId")
                    return@launch
                }

                when (alarmType) {
                    "EXPIRATION" -> {
                        // Automatically delete when lifetime is finished
                        urlDao.deleteSavedUrl(savedUrl)
                        showExpirationNotification(context, savedUrl)
                    }
                    "REMINDER" -> {
                        // Play custom sound and show notification before expiry or custom reminder
                        showReminderNotification(context, savedUrl)
                        
                        // Handle recurring reminder interval (Daily, Weekly, Monthly)
                        rescheduleRecurringReminder(context, savedUrl)
                    }
                    "SNOOZE" -> {
                        // User clicked snooze button from notification
                        snoozeReminder(context, savedUrl, urlDao)
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error in onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, savedUrl: SavedUrl) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "link_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Link Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to review saved links"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app when notification clicked
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("saved_url_id", savedUrl.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            savedUrl.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action button
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("saved_url_id", savedUrl.id)
            putExtra("alarm_type", "SNOOZE")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            savedUrl.id.toInt() + 100000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundName = savedUrl.customSound
        playSound(context, soundName)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Save It: Time to Review")
            .setContentText(savedUrl.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Link: ${savedUrl.url}\nNotes: ${savedUrl.notes}\n\nReview this saved bookmark."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (10m)", snoozePendingIntent)

        notificationManager.notify(savedUrl.id.toInt(), builder.build())
    }

    private fun showExpirationNotification(context: Context, savedUrl: SavedUrl) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "link_expiration_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Link Expirations",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("Save It: Link Expired & Deleted")
            .setContentText(savedUrl.title)
            .setSubText("Lifetime reached")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(savedUrl.id.toInt() + 200000, builder.build())
    }

    private fun snoozeReminder(context: Context, savedUrl: SavedUrl, urlDao: com.example.data.SavedUrlDao) {
        val newReminderTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10) // Snooze for 10 minutes
        val updatedUrl = savedUrl.copy(
            reminderTime = newReminderTime,
            snoozeCount = savedUrl.snoozeCount + 1,
            isSnoozed = true
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            urlDao.updateSavedUrl(updatedUrl)
            // Schedule the new alarm
            scheduleAlarm(context, updatedUrl)
        }

        // Cancel previous notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(savedUrl.id.toInt())
    }

    private fun rescheduleRecurringReminder(context: Context, savedUrl: SavedUrl) {
        val intervalMs = when (savedUrl.reminderInterval) {
            "Daily" -> TimeUnit.DAYS.toMillis(1)
            "Weekly" -> TimeUnit.DAYS.toMillis(7)
            "Monthly" -> TimeUnit.DAYS.toMillis(30)
            else -> 0L
        }

        if (intervalMs > 0L && savedUrl.reminderTime != null) {
            val db = AppDatabase.getDatabase(context)
            val updatedUrl = savedUrl.copy(
                reminderTime = savedUrl.reminderTime + intervalMs,
                isSnoozed = false
            )
            CoroutineScope(Dispatchers.IO).launch {
                db.savedUrlDao().updateSavedUrl(updatedUrl)
                scheduleAlarm(context, updatedUrl)
            }
        }
    }

    private fun playSound(context: Context, soundName: String) {
        try {
            val soundUri: Uri = when (soundName) {
                "Alarm Tone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "Ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                "None" -> return
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to play sound: $soundName", e)
        }
    }

    companion object {
        fun scheduleAlarm(context: Context, savedUrl: SavedUrl) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // 1. Schedule reminder if set
            savedUrl.reminderTime?.let { time ->
                if (time > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("saved_url_id", savedUrl.id)
                        putExtra("alarm_type", "REMINDER")
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        savedUrl.id.toInt() + 300000,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            time,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            time,
                            pendingIntent
                        )
                    }
                    Log.d("AlarmReceiver", "Scheduled REMINDER alarm for URL ${savedUrl.id} at $time")
                }
            }

            // 2. Schedule expiration deletion if set
            savedUrl.expiryTime?.let { expiry ->
                if (expiry > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("saved_url_id", savedUrl.id)
                        putExtra("alarm_type", "EXPIRATION")
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        savedUrl.id.toInt() + 400000,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            expiry,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            expiry,
                            pendingIntent
                        )
                    }
                    Log.d("AlarmReceiver", "Scheduled EXPIRATION alarm for URL ${savedUrl.id} at $expiry")
                }
            }
        }

        fun cancelAlarms(context: Context, savedUrl: SavedUrl) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Cancel reminder
            val reminderIntent = Intent(context, AlarmReceiver::class.java)
            val reminderPending = PendingIntent.getBroadcast(
                context,
                savedUrl.id.toInt() + 300000,
                reminderIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (reminderPending != null) {
                alarmManager.cancel(reminderPending)
                reminderPending.cancel()
            }

            // Cancel expiration
            val expiryIntent = Intent(context, AlarmReceiver::class.java)
            val expiryPending = PendingIntent.getBroadcast(
                context,
                savedUrl.id.toInt() + 400000,
                expiryIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (expiryPending != null) {
                alarmManager.cancel(expiryPending)
                expiryPending.cancel()
            }
        }
    }
}
