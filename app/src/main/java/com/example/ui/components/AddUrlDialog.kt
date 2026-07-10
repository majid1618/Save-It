package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUrlDialog(
    initialUrl: String? = null,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (
        url: String,
        title: String,
        notes: String,
        folderId: Long,
        lifetimeOption: String,
        reminderBeforeOption: String,
        customSound: String,
        addToCalendar: Boolean,
        addClockAlarm: Boolean
    ) -> Unit,
    onCreateFolder: (name: String) -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Select first folder as default if available
    var selectedFolderId by remember { mutableStateOf(folders.firstOrNull()?.id ?: -1L) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    
    // Lifetime and reminder settings
    var lifetimeOption by remember { mutableStateOf("1 Week") } // Default 1 week
    var reminderBeforeOption by remember { mutableStateOf("1 day") } // Default 1 day before
    var customSound by remember { mutableStateOf("Default Sound") }
    
    // System integrations
    var addToCalendar by remember { mutableStateOf(false) }
    var addClockAlarm by remember { mutableStateOf(false) }
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val lifetimeOptions = listOf("1 Hour", "1 Day", "1 Week", "1 Month", "3 Months", "No Limit")
    val reminderOptions = listOf("None", "1 hour", "1 day", "1 week", "1 month")
    val soundOptions = listOf("Default Sound", "Alarm Tone", "Ringtone", "None")

    // Android Permission Launchers
    val calendarPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            addToCalendar = true
            Toast.makeText(context, "Calendar Access Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Calendar Access Denied. Cannot sync with Calendar.", Toast.LENGTH_LONG).show()
            addToCalendar = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notifications permission denied. Alarms won't show banners.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto request notification permission on mount for API 33+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save New Bookmark") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Link Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL / Link") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth().testTag("url_input"),
                    singleLine = true
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Android Tutorial") },
                    modifier = Modifier.fillMaxWidth().testTag("title_input"),
                    singleLine = true
                )

                // Folder Selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = folderMenuExpanded,
                            onExpandedChange = { folderMenuExpanded = !folderMenuExpanded }
                        ) {
                            val activeFolder = folders.find { it.id == selectedFolderId }
                            OutlinedTextField(
                                value = activeFolder?.name ?: "Select Folder",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category Folder") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = folderMenuExpanded,
                                onDismissRequest = { folderMenuExpanded = false }
                            ) {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            selectedFolderId = folder.id
                                            folderMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Create inline folder button
                    FilledIconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Folder")
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Add Notes") },
                    placeholder = { Text("Why are you saving this link?") },
                    modifier = Modifier.fillMaxWidth().height(90.dp).testTag("notes_input"),
                    maxLines = 3
                )

                Divider()

                // Lifetime limit
                Text("Lifetime Limit (Auto-delete):", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lifetimeOptions.take(3).forEach { option ->
                        FilterChip(
                            selected = lifetimeOption == option,
                            onClick = { 
                                lifetimeOption = option
                                // Automatically disable clock alarm / calendar if infinity
                                if (option == "No Limit") {
                                    addClockAlarm = false
                                }
                            },
                            label = { Text(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lifetimeOptions.drop(3).forEach { option ->
                        FilterChip(
                            selected = lifetimeOption == option,
                            onClick = { 
                                lifetimeOption = option
                                if (option == "No Limit") {
                                    addClockAlarm = false
                                    reminderBeforeOption = "None"
                                }
                            },
                            label = { Text(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (lifetimeOption != "No Limit") {
                    // Warning countdown before expiry
                    Text("Warning Countdown Reminder:", style = MaterialTheme.typography.titleSmall)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var reminderDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = reminderDropdownExpanded,
                            onExpandedChange = { reminderDropdownExpanded = !reminderDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = reminderBeforeOption,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Remind me before expiry") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = reminderDropdownExpanded,
                                onDismissRequest = { reminderDropdownExpanded = false }
                            ) {
                                reminderOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            reminderBeforeOption = option
                                            reminderDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Sound / Ringtone Selection
                Text("Notification Tone Preference:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    soundOptions.forEach { sound ->
                        FilterChip(
                            selected = customSound == sound,
                            onClick = { customSound = sound },
                            label = { Text(sound, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Divider()

                // System Integration options
                Text("System Integrations:", style = MaterialTheme.typography.titleSmall)
                
                // Calendar toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add to System Calendar", style = MaterialTheme.typography.bodyMedium)
                        Text("Saves a calendar event for this reminder", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = addToCalendar,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // Check/request permissions
                                val hasCalendar = calendarPermissions.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }
                                if (hasCalendar) {
                                    addToCalendar = true
                                } else {
                                    calendarPermissionLauncher.launch(calendarPermissions)
                                }
                            } else {
                                addToCalendar = false
                            }
                        }
                    )
                }

                // Native clock app alarm toggle
                if (lifetimeOption != "No Limit" && reminderBeforeOption != "None") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Set Clock App Alarm", style = MaterialTheme.typography.bodyMedium)
                            Text("Sets a native physical alarm in clock app", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = addClockAlarm,
                            onCheckedChange = { checked -> addClockAlarm = checked }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank() && selectedFolderId != -1L) {
                        val finalTitle = if (title.isBlank()) url else title
                        onConfirm(
                            url,
                            finalTitle,
                            notes,
                            selectedFolderId,
                            lifetimeOption,
                            reminderBeforeOption,
                            customSound,
                            addToCalendar,
                            addClockAlarm
                        )
                    }
                },
                enabled = url.isNotBlank() && selectedFolderId != -1L,
                modifier = Modifier.testTag("submit_url_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { fullName, _ ->
                onCreateFolder(fullName)
                showCreateFolderDialog = false
            }
        )
    }
}
