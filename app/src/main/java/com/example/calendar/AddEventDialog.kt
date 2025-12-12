package com.example.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    // C：类别选择
    val categories = listOf("工作", "学习", "生活", "提醒", "其他")
    var selectedCategory by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加日程") },
        text = {
            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") }
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述") }
                )

                Spacer(Modifier.height(16.dp))
                Text("类别：", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    categories.forEachIndexed { index, text ->
                        AssistChip(
                            onClick = { selectedCategory = index },
                            label = { Text(text) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategory == index)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, desc, selectedCategory)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
