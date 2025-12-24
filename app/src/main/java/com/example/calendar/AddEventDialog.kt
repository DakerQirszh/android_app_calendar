package com.example.calendar
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
fun AddEventDialog(
    selectedDate: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?) -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(0) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("工作", "学习", "生活", "提醒", "其他")
    var selectedCategory by remember { mutableStateOf(0) }

    var reminderTimeMillis by remember { mutableStateOf<Long?>(null) }
    var reminderText by remember { mutableStateOf("未设置") }

    fun setReminderFromHourMinute(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        reminderTimeMillis = cal.timeInMillis
        reminderText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

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

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("工作", "学习", "生活", "提醒", "其他").forEachIndexed { index, text ->
                        FilterChip(
                            selected = category == index,
                            onClick = { category = index },
                            label = { Text(text) }
                        )
                    }
                }
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    categories.forEachIndexed { index, text ->
//                        AssistChip(
//                            onClick = { selectedCategory = index },
//                            label = { Text(text) },
//                            colors = AssistChipDefaults.assistChipColors(
//                                containerColor = if (selectedCategory == index)
//                                    MaterialTheme.colorScheme.primaryContainer
//                                else
//                                    MaterialTheme.colorScheme.surfaceVariant
//                            )
//                        )
//                    }
//                }

                Spacer(Modifier.height(16.dp))
                Text("提醒时间：", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    setReminderFromHourMinute(hour, minute)
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    ) {
                        Text(if (reminderTimeMillis == null) "设置提醒" else "修改：$reminderText")
                    }

                    if (reminderTimeMillis != null) {
                        OutlinedButton(
                            onClick = {
                                reminderTimeMillis = null
                                reminderText = "未设置"
                            }
                        ) { Text("清除") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, desc, selectedCategory, reminderTimeMillis)
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
