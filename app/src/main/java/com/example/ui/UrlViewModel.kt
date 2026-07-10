package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Folder
import com.example.data.SavedUrl
import com.example.data.UrlRepository
import com.example.receiver.AlarmReceiver
import com.example.util.SystemIntegrationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class UrlViewModel(private val repository: UrlRepository) : ViewModel() {

    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedUrls: StateFlow<List<SavedUrl>> = repository.allSavedUrls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sharedUrlToSave = MutableStateFlow<String?>(null)
    val sharedUrlToSave: StateFlow<String?> = _sharedUrlToSave.asStateFlow()

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSharedUrl(url: String?) {
        _sharedUrlToSave.value = url
    }

    fun insertFolder(name: String, iconName: String = "Folder") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertFolder(Folder(name = name, iconName = iconName))
            }
        }
    }

    fun deleteFolder(folder: Folder, context: Context) {
        viewModelScope.launch {
            // Cancel all alarms of URLs in this folder
            repository.getSavedUrlsByFolder(folder.id).first().forEach { savedUrl ->
                AlarmReceiver.cancelAlarms(context, savedUrl)
            }
            repository.deleteFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    fun insertSavedUrl(
        url: String,
        title: String,
        notes: String,
        folderId: Long,
        lifetimeOption: String, // "1 Hour", "1 Day", "1 Week", "1 Month", "3 Months", "No Limit"
        reminderBeforeOption: String, // "1 hour", "1 day", "1 week", "1 month", "None"
        customSound: String, // "Default", "Alarm Tone", "Ringtone", "None"
        addToCalendar: Boolean,
        addClockAlarm: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            
            // 1. Calculate Expiry Time
            val lifetimeMs = when (lifetimeOption) {
                "1 Hour" -> TimeUnit.HOURS.toMillis(1)
                "1 Day" -> TimeUnit.DAYS.toMillis(1)
                "1 Week" -> TimeUnit.DAYS.toMillis(7)
                "1 Month" -> TimeUnit.DAYS.toMillis(30)
                "3 Months" -> TimeUnit.DAYS.toMillis(90)
                else -> null
            }
            val expiryTime = if (lifetimeMs != null) currentTime + lifetimeMs else null

            // 2. Calculate Reminder Time (countdown alert before expiry)
            val beforeMs = when (reminderBeforeOption) {
                "1 hour" -> TimeUnit.HOURS.toMillis(1)
                "1 day" -> TimeUnit.DAYS.toMillis(1)
                "1 week" -> TimeUnit.DAYS.toMillis(7)
                "1 month" -> TimeUnit.DAYS.toMillis(30)
                else -> 0L
            }
            
            // Reminder triggers prior to expiry or immediately if short lifetime
            var reminderTime: Long? = null
            if (expiryTime != null && beforeMs > 0L) {
                val calculatedReminder = expiryTime - beforeMs
                if (calculatedReminder > currentTime) {
                    reminderTime = calculatedReminder
                } else {
                    // If beforeMs is longer than total lifetime, trigger midway
                    reminderTime = currentTime + (expiryTime - currentTime) / 2
                }
            }

            val savedUrl = SavedUrl(
                url = url,
                title = title,
                notes = notes,
                folderId = folderId,
                createdAt = currentTime,
                expiryTime = expiryTime,
                lifetimeDurationMs = lifetimeMs,
                reminderBeforeOption = reminderBeforeOption,
                reminderTime = reminderTime,
                customSound = customSound
            )

            val newId = repository.insertSavedUrl(savedUrl)
            val finalSavedUrl = savedUrl.copy(id = newId)

            // 3. Schedule alarms using AlarmManager
            AlarmReceiver.scheduleAlarm(context, finalSavedUrl)

            // 4. Perform calendar integration if requested
            if (addToCalendar) {
                SystemIntegrationHelper.addToSystemCalendar(context, finalSavedUrl)
            }

            // 5. Set system Clock Alarm if requested
            if (addClockAlarm && reminderTime != null) {
                SystemIntegrationHelper.setSystemClockAlarm(context, finalSavedUrl)
            }
        }
    }

    fun deleteSavedUrl(savedUrl: SavedUrl, context: Context) {
        viewModelScope.launch {
            AlarmReceiver.cancelAlarms(context, savedUrl)
            repository.deleteSavedUrl(savedUrl)
        }
    }

    fun snoozeUrl(savedUrl: SavedUrl, context: Context) {
        viewModelScope.launch {
            // Extend lifetime by 1 day and snooze reminder
            val extraTime = TimeUnit.DAYS.toMillis(1)
            val newExpiry = if (savedUrl.expiryTime != null) savedUrl.expiryTime + extraTime else null
            val newReminder = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10) // Alarm again in 10 mins

            val snoozedUrl = savedUrl.copy(
                expiryTime = newExpiry,
                reminderTime = newReminder,
                snoozeCount = savedUrl.snoozeCount + 1,
                isSnoozed = true
            )

            repository.updateSavedUrl(snoozedUrl)
            AlarmReceiver.scheduleAlarm(context, snoozedUrl)
        }
    }

    fun cleanExpiredUrls(context: Context) {
        viewModelScope.launch {
            val expired = repository.getExpiredUrls(System.currentTimeMillis())
            expired.forEach { savedUrl ->
                repository.deleteSavedUrl(savedUrl)
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UrlViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val repository = UrlRepository(db.folderDao(), db.savedUrlDao())
                @Suppress("UNCHECKED_CAST")
                return UrlViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
