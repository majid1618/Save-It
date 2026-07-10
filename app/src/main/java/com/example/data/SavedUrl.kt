package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_urls")
data class SavedUrl(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val notes: String = "",
    val folderId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val expiryTime: Long? = null,
    val lifetimeDurationMs: Long? = null,
    val reminderBeforeOption: String = "None", // "1 month", "1 week", "1 day", "1 hour", "None"
    val reminderTime: Long? = null, // Specific timestamp for push notification
    val reminderInterval: String = "Once", // "Once", "Daily", "Weekly", "Monthly"
    val customSound: String = "Default", // "Default", "Bell", "Chime", "Digital", "Synth"
    val snoozeCount: Int = 0,
    val isSnoozed: Boolean = false
) {
    fun getFormattedCountdown(currentTime: Long = System.currentTimeMillis()): String {
        val expiry = expiryTime ?: return "No Limit"
        val diff = expiry - currentTime
        if (diff <= 0) return "Expired"
        
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h remaining"
            hours > 0 -> "${hours}h ${(diff / (1000 * 60)) % 60}m remaining"
            else -> "${(diff / (1000 * 60)) % 60}m ${(diff / 1000) % 60}s remaining"
        }
    }
}
