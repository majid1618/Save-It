package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.AlarmClock
import android.util.Log
import android.widget.Toast
import com.example.data.SavedUrl
import java.util.TimeZone

object SystemIntegrationHelper {

    fun addToSystemCalendar(context: Context, savedUrl: SavedUrl): Boolean {
        try {
            val contentResolver = context.contentResolver
            
            // Get standard primary calendar ID (default is 1)
            val values = ContentValues().apply {
                val start = savedUrl.reminderTime ?: savedUrl.expiryTime ?: System.currentTimeMillis()
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, start + 30 * 60 * 1000) // 30 minutes duration
                put(CalendarContract.Events.TITLE, "Save It: Review ${savedUrl.title}")
                put(CalendarContract.Events.DESCRIPTION, "Bookmark: ${savedUrl.url}\nNotes: ${savedUrl.notes}")
                put(CalendarContract.Events.CALENDAR_ID, 1) // default primary calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                Log.d("SystemIntegration", "Successfully added event to system calendar: $uri")
                Toast.makeText(context, "Sync success! Added reminder to System Calendar.", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Log.e("SystemIntegration", "Failed to add to calendar", e)
            Toast.makeText(context, "Calendar Sync Failed: Enable Calendar permission in App Settings.", Toast.LENGTH_LONG).show()
        }
        return false
    }

    fun setSystemClockAlarm(context: Context, savedUrl: SavedUrl) {
        try {
            val time = savedUrl.reminderTime ?: return
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = time
            }
            
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            
            // Setting a clock alarm
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Review ${savedUrl.title}")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(context, "System Alarm set for ${String.format("%02d", hour)}:${String.format("%02d", minute)}!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SystemIntegration", "Failed to set Clock Alarm", e)
            Toast.makeText(context, "Could not set native Clock app alarm. Try setting a standard push notification.", Toast.LENGTH_LONG).show()
        }
    }
}
