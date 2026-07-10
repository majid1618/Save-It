package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddUrlDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.SavedUrlCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SaveItApp(
    viewModel: UrlViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val savedUrls by viewModel.savedUrls.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sharedUrlToSave by viewModel.sharedUrlToSave.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<com.example.data.Folder?>(null) }

    // Periodically clean up expired links
    LaunchedEffect(Unit) {
        viewModel.cleanExpiredUrls(context)
    }

    // Automatically trigger Save Link dialog if a shared URL is intercepted!
    LaunchedEffect(sharedUrlToSave) {
        if (sharedUrlToSave != null) {
            showAddDialog = true
        }
    }

    // Dynamic counts
    val folderCountMap = remember(savedUrls) {
        savedUrls.groupBy { it.folderId }.mapValues { it.value.size }
    }

    // Filtered Saved URLs
    val filteredUrls = remember(savedUrls, selectedFolderId, searchQuery) {
        savedUrls.filter { url ->
            val matchesFolder = selectedFolderId == null || url.folderId == selectedFolderId
            val matchesSearch = searchQuery.isBlank() || 
                    url.title.contains(searchQuery, ignoreCase = true) ||
                    url.url.contains(searchQuery, ignoreCase = true) ||
                    url.notes.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Save It", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Secure & categorize shared links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = "App Icon Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(28.dp)
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_url_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Save New Link")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar_input"),
                placeholder = { Text("Search bookmarks, folders, notes...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // Dashboard Metrics Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${savedUrls.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val limitCount = savedUrls.count { it.expiryTime != null }
                        Text(
                            text = "$limitCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Expiring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val snoozeCount = savedUrls.count { it.snoozeCount > 0 }
                        Text(
                            text = "$snoozeCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Snoozed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Folders Horizontal Row list
            Text(
                text = "📁 Folders (Hold to delete custom)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All Bookmarks" filter chip
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { viewModel.selectFolder(null) },
                        label = { Text("🌐 All (${savedUrls.size})") },
                        modifier = Modifier.testTag("all_bookmarks_chip")
                    )
                }

                items(folders) { folder ->
                    val count = folderCountMap[folder.id] ?: 0
                    val isSelected = selectedFolderId == folder.id
                    
                    Surface(
                        shape = FilterChipDefaults.shape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { viewModel.selectFolder(folder.id) },
                                onLongClick = {
                                    // Deleting custom folders (prevent deleting Inbox or Read Later for safety)
                                    if (!folder.name.contains("Inbox") && !folder.name.contains("Read Later")) {
                                        folderToDelete = folder
                                    } else {
                                        Toast.makeText(context, "Cannot delete system default folders.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            .testTag("folder_chip_${folder.id}")
                    ) {
                        Text(
                            text = "${folder.name} ($count)",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Inline quick add folder button in the horizontal row
                item {
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Saved URLs List
            if (filteredUrls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "No Saved URLs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No search matches" else "No saved links in this folder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Share URLs directly from other apps to save, or click the '+' button below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("saved_urls_list")
                ) {
                    items(filteredUrls, key = { it.id }) { savedUrl ->
                        val folderOfUrl = folders.find { it.id == savedUrl.folderId }
                        SavedUrlCard(
                            savedUrl = savedUrl,
                            folder = folderOfUrl,
                            onDelete = { viewModel.deleteSavedUrl(savedUrl, context) },
                            onSnooze = { viewModel.snoozeUrl(savedUrl, context) }
                        )
                    }
                }
            }
        }
    }

    // Add Saved URL Dialog Sheet
    if (showAddDialog) {
        AddUrlDialog(
            initialUrl = sharedUrlToSave,
            folders = folders,
            onDismiss = {
                showAddDialog = false
                viewModel.setSharedUrl(null) // clear shared URL
            },
            onConfirm = { url, title, notes, folderId, lifetime, warningBefore, sound, syncCalendar, syncClockAlarm ->
                viewModel.insertSavedUrl(
                    url = url,
                    title = title,
                    notes = notes,
                    folderId = folderId,
                    lifetimeOption = lifetime,
                    reminderBeforeOption = warningBefore,
                    customSound = sound,
                    addToCalendar = syncCalendar,
                    addClockAlarm = syncClockAlarm,
                    context = context
                )
                showAddDialog = false
                viewModel.setSharedUrl(null) // clear shared URL after saving
                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onCreateFolder = { name ->
                viewModel.insertFolder(name)
            }
        )
    }

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { fullName, _ ->
                viewModel.insertFolder(fullName)
                showCreateFolderDialog = false
                Toast.makeText(context, "Folder created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Folder Confirmation Dialog
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder?") },
            text = { Text("This will permanently delete '${folderToDelete?.name}' and all saved URLs and reminders inside it.") },
            confirmButton = {
                Button(
                    onClick = {
                        folderToDelete?.let { viewModel.deleteFolder(it, context) }
                        folderToDelete = null
                        Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
