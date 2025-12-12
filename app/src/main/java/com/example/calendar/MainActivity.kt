package com.example.calendar
import com.example.calendar.data.LunarCalendar
import com.example.calendar.data.Zodiac

import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.calendar.data.DatabaseProvider
import com.example.calendar.data.Event
import com.example.calendar.ui.theme.日历Theme
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = DatabaseProvider.getDatabase(this)
        val dao = db.eventDao()

        setContent {
            日历Theme {

                // 状态变量
                var editingEvent by remember { mutableStateOf<Event?>(null) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showDialog by remember { mutableStateOf(false) }

                var selectedDate by remember { mutableStateOf(getToday()) }

                var eventsForDay by remember { mutableStateOf<List<Event>>(emptyList()) }

                // 搜索文本
                var searchQuery by remember { mutableStateOf("") }

                // 所有有事件的日期
                var datesWithEvents by remember { mutableStateOf<Set<Long>>(emptySet()) }

                // 每次选中日期刷新事件列表
                LaunchedEffect(selectedDate) {
                    eventsForDay = dao.getEventsByDate(selectedDate)
                }

                // 启动时读取所有事件，用于日期高亮
                LaunchedEffect(Unit) {
                    val all = dao.getAllEvents()
                    datesWithEvents = all.map { it.date }.toSet()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // 添加事件弹窗
                    if (showDialog) {
                        AddEventDialog(
                            onDismiss = { showDialog = false },
                            onSave = { title, desc, category ->
                                lifecycleScope.launch {
                                    dao.insertEvent(
                                        Event(
                                            title = title,
                                            description = desc,
                                            date = selectedDate,
                                            category = category
                                        )
                                    )
                                    eventsForDay = dao.getEventsByDate(selectedDate)

                                    val all = dao.getAllEvents()
                                    datesWithEvents = all.map { it.date }.toSet()
                                }
                                showDialog = false
                                Toast.makeText(this, "保存成功：$title", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // 编辑事件弹窗
                    if (showEditDialog && editingEvent != null) {
                        EditEventDialog(
                            event = editingEvent!!,
                            onDismiss = { showEditDialog = false },
                            onSave = { newTitle, newDesc ->
                                lifecycleScope.launch {
                                    dao.updateEvent(
                                        editingEvent!!.copy(
                                            title = newTitle,
                                            description = newDesc
                                        )
                                    )
                                    eventsForDay = dao.getEventsByDate(selectedDate)
                                }
                                showEditDialog = false
                            }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {

                        Column(modifier = Modifier.fillMaxSize()) {

                            // 日历视图
                            AndroidView(
                                factory = { context ->
                                    CalendarView(context).apply {
                                        setOnDateChangeListener { _, year, month, day ->
                                            val cal = Calendar.getInstance().apply {
                                                set(year, month, day, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val newDate = cal.timeInMillis
                                            selectedDate = newDate

                                            val hasEvent = newDate in datesWithEvents
                                            val msg = if (hasEvent) {
                                                "这一天有事件：$year-${month + 1}-$day"
                                            } else {
                                                "选中日期：$year-${month + 1}-$day"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            // 显示农历 + 星座
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            val lunar = LunarCalendar.getLunarDate(cal)
                            val zodiac = Zodiac.getZodiac(cal)

                            Text(
                                text = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)+1}-${cal.get(Calendar.DAY_OF_MONTH)}  ·  农历 $lunar  ·  $zodiac",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )

                            // 搜索框
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("搜索事件…") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = "当天事件：",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )

                            // 搜索过滤
                            val filteredEvents = eventsForDay.filter {
                                if (searchQuery.isBlank()) true
                                else {
                                    it.title.contains(searchQuery, true) ||
                                            it.description.contains(searchQuery, true)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(filteredEvents) { event ->
                                    EventItem(
                                        event = event,
                                        onToggleFinished = {
                                            lifecycleScope.launch {
                                                dao.updateEvent(event.copy(finished = !event.finished))
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                            }
                                        },
                                        onClick = {
                                            editingEvent = event
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            lifecycleScope.launch {
                                                dao.deleteEvent(event)
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Event")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(
    event: Event,
    onToggleFinished: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 类别颜色条
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(40.dp)
                    .background(CategoryColor.colorFor(event.category))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (event.finished) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (event.finished) Color.Gray else Color.Unspecified
                )
                if (event.description.isNotEmpty()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (event.finished) Color.Gray else Color.Unspecified
                    )
                }
            }

            Checkbox(
                checked = event.finished,
                onCheckedChange = { onToggleFinished() }
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun EditEventDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit     // 新标题 & 新描述
) {
    var title by remember { mutableStateOf(event.title) }
    var desc by remember { mutableStateOf(event.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑事件") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, desc) }) {
                Text("保存修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


fun getToday(): Long {
    val c = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}
