package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("📥") }
    
    val iconOptions = listOf("📥", "📖", "💡", "💼", "🛒", "🎓", "🏡", "✈️", "🎵", "👾", "❤️", "🍔")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input"),
                    singleLine = true
                )

                Text("Select Emoji Icon:", style = MaterialTheme.typography.labelMedium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    iconOptions.take(6).forEach { emoji ->
                        IconButton(
                            onClick = { selectedIcon = emoji },
                            colors = if (selectedIcon == emoji) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                IconButtonDefaults.iconButtonColors()
                            }
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    iconOptions.drop(6).forEach { emoji ->
                        IconButton(
                            onClick = { selectedIcon = emoji },
                            colors = if (selectedIcon == emoji) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                IconButtonDefaults.iconButtonColors()
                            }
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm("$selectedIcon $folderName", selectedIcon)
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("folder_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
