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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.calendar.data.DatabaseProvider
import com.example.calendar.data.Event
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = DatabaseProvider.getDatabase(this)
        val dao = db.eventDao()

        setContent {
            MaterialTheme {

                var selectedDate by remember { mutableStateOf(getToday()) }
                //日式图
                var viewMode by remember { mutableStateOf(CalendarViewMode.DAY) }


                var eventsForDay by remember { mutableStateOf<List<Event>>(emptyList()) }
                var datesWithEvents by remember { mutableStateOf<Set<Long>>(emptySet()) }

                var searchQuery by remember { mutableStateOf("") }

                // 添加
                var showAddDialog by remember { mutableStateOf(false) }

                // 查看
                var viewingEvent by remember { mutableStateOf<Event?>(null) }
                var showViewDialog by remember { mutableStateOf(false) }

                // 编辑
                var editingEvent by remember { mutableStateOf<Event?>(null) }
                var showEditDialog by remember { mutableStateOf(false) }

                // 选中日期 -> 刷新当天事件
                LaunchedEffect(selectedDate) {
                    eventsForDay = dao.getEventsByDate(selectedDate)
                }

                // 启动时加载“哪些日期有事件”
                LaunchedEffect(Unit) {
                    datesWithEvents = dao.getAllEvents().map { it.date }.toSet()
                }

                fun refreshDatesWithEvents() {
                    lifecycleScope.launch {
                        datesWithEvents = dao.getAllEvents().map { it.date }.toSet()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // ---------- 新增弹窗 ----------
                    if (showAddDialog) {
                        AddEventDialog(
                            onDismiss = { showAddDialog = false },
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
                                    refreshDatesWithEvents()
                                }
                                showAddDialog = false
                                Toast.makeText(this, "保存成功：$title", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // ---------- 查看弹窗 ----------
                    if (showViewDialog && viewingEvent != null) {
                        ViewEventDialog(
                            event = viewingEvent!!,
                            onDismiss = {
                                showViewDialog = false
                                viewingEvent = null
                            },
                            onEdit = {
                                editingEvent = viewingEvent
                                showViewDialog = false
                                showEditDialog = true
                            },
                            onDelete = {
                                val toDelete = viewingEvent
                                if (toDelete != null) {
                                    lifecycleScope.launch {
                                        dao.deleteEvent(toDelete)
                                        eventsForDay = dao.getEventsByDate(selectedDate)
                                        refreshDatesWithEvents()
                                    }
                                }
                                showViewDialog = false
                                viewingEvent = null
                            }
                        )
                    }

                    // ---------- 编辑弹窗 ----------
                    if (showEditDialog && editingEvent != null) {
                        EditEventDialog(
                            event = editingEvent!!,
                            onDismiss = {
                                showEditDialog = false
                                editingEvent = null
                            },
                            onSave = { newTitle, newDesc ->
                                lifecycleScope.launch {
                                    dao.updateEvent(
                                        editingEvent!!.copy(
                                            title = newTitle,
                                            description = newDesc
                                        )
                                    )
                                    eventsForDay = dao.getEventsByDate(selectedDate)
                                    refreshDatesWithEvents()
                                }
                                showEditDialog = false
                                editingEvent = null
                            }
                        )
                    }

                    // ---------- 主界面 ----------
                    Box(modifier = Modifier.fillMaxSize()) {

                        Column(modifier = Modifier.fillMaxSize()) {
                            val tabs = listOf("月视图", "周视图", "日视图")

                            TabRow(
                                selectedTabIndex = viewMode.ordinal
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = viewMode.ordinal == index,
                                        onClick = {
                                            viewMode = CalendarViewMode.values()[index]
                                        },
                                        text = { Text(title) }
                                    )
                                }
                            }

                            // 系统 CalendarView
                            when (viewMode) {

                                CalendarViewMode.MONTH -> {
                                    // 月视图（仍然使用 CalendarView）
                                    AndroidView(
                                        factory = { context ->
                                            CalendarView(context).apply {
                                                setOnDateChangeListener { _, year, month, day ->
                                                    val cal = Calendar.getInstance().apply {
                                                        set(year, month, day, 0, 0, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    selectedDate = cal.timeInMillis
                                                    viewMode = CalendarViewMode.DAY // 👈 点月 → 进日视图
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                CalendarViewMode.WEEK -> {
                                    WeekView(
                                        selectedDate = selectedDate,
                                        datesWithEvents = datesWithEvents,
                                        onDateSelected = { date ->
                                            selectedDate = date
                                            viewMode = CalendarViewMode.DAY
                                        }
                                    )
                                }


                                CalendarViewMode.DAY -> {
                                    DayView(
                                        selectedDate = selectedDate,
                                        eventsForDay = eventsForDay,
                                        searchQuery = searchQuery,
                                        onSearchChange = { searchQuery = it },
                                        onEventClick = {
                                            viewingEvent = it
                                            showViewDialog = true
                                        },
                                        onToggleFinished = {
                                            lifecycleScope.launch {
                                                dao.updateEvent(it.copy(finished = !it.finished))
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                            }
                                        },
                                        onDeleteEvent = {
                                            lifecycleScope.launch {
                                                dao.deleteEvent(it)
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                                refreshDatesWithEvents()
                                            }
                                        }
                                    )
                                }
                            }


                            Spacer(Modifier.height(8.dp))

                            // ✅ 修复：LunarCalendar / Zodiac 都是接收 Calendar 参数
                            val cal = remember(selectedDate) {
                                Calendar.getInstance().apply { timeInMillis = selectedDate }
                            }
                            val lunar = remember(selectedDate) { LunarCalendar.getLunarDate(cal) }
                            val zodiac = remember(selectedDate) { Zodiac.getZodiac(cal) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("农历：$lunar", modifier = Modifier.weight(1f))
                                Text("星座：$zodiac")
                            }

                            Spacer(Modifier.height(8.dp))

                            // 搜索框
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                label = { Text("搜索标题/描述") },
                                singleLine = true
                            )

                            Spacer(Modifier.height(8.dp))

                            val filteredEvents = eventsForDay.filter {
                                if (searchQuery.isBlank()) true
                                else it.title.contains(searchQuery, true) ||
                                        it.description.contains(searchQuery, true)
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
                                                // finished 不影响 datesWithEvents，所以不必刷新
                                            }
                                        },
                                        onClick = {
                                            viewingEvent = event
                                            showViewDialog = true
                                        },
                                        onDelete = {
                                            lifecycleScope.launch {
                                                dao.deleteEvent(event)
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                                refreshDatesWithEvents()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { showAddDialog = true },
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

enum class CalendarViewMode {
    MONTH,
    WEEK,
    DAY
}
@Composable
fun DayView(
    selectedDate: Long,
    eventsForDay: List<Event>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEventClick: (Event) -> Unit,
    onToggleFinished: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit
) {
    Column {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text("搜索标题/描述") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(eventsForDay.filter {
                searchQuery.isBlank() ||
                        it.title.contains(searchQuery, true) ||
                        it.description.contains(searchQuery, true)
            }) { event ->
                EventItem(
                    event = event,
                    onToggleFinished = { onToggleFinished(event) },
                    onClick = { onEventClick(event) },
                    onDelete = { onDeleteEvent(event) }
                )
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
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(42.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(CategoryColor.colorFor(event.category))
            )

            Spacer(Modifier.width(10.dp))

            Checkbox(
                checked = event.finished,
                onCheckedChange = { onToggleFinished() }
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (event.finished) TextDecoration.LineThrough else null,
                    color = if (event.finished) Color.Gray else LocalContentColor.current
                )
                if (event.description.isNotEmpty()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (event.finished) TextDecoration.LineThrough else null,
                        color = if (event.finished) Color.Gray else LocalContentColor.current
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun ViewEventDialog(
    event: Event,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(event.date) { formatDate(event.date) }
    val timeText = remember(event.time) { event.time?.let { formatTime(it) } ?: "未设置" }
    val categoryText = remember(event.category) { categoryName(event.category) }
    val finishedText = if (event.finished) "已完成" else "未完成"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日程详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("标题：${event.title}")
                if (event.description.isNotBlank()) Text("描述：${event.description}")
                Text("日期：$dateText")
                Text("提醒时间：$timeText")
                Text("类别：$categoryText")
                Text("状态：$finishedText")
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("编辑") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
fun EditEventDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, desc) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

fun categoryName(category: Int): String = when (category) {
    0 -> "工作"
    1 -> "学习"
    2 -> "生活"
    3 -> "提醒"
    4 -> "其他"
    else -> "其他"
}

fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}

fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
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
@Composable
fun WeekView(
    selectedDate: Long,
    datesWithEvents: Set<Long>,
    onDateSelected: (Long) -> Unit
) {
    val weekDates = remember(selectedDate) { getWeekDates(selectedDate) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDates.forEach { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val isSelected = date == selectedDate
            val hasEvent = date in datesWithEvents

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDateSelected(date) }
                    .padding(6.dp)
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        hasEvent -> MaterialTheme.colorScheme.secondary
                        else -> LocalContentColor.current
                    }
                )

                if (hasEvent) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}


fun startOfWeek(date: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = date
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // 周一开始
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun getWeekDates(selectedDate: Long): List<Long> {
    val start = startOfWeek(selectedDate)
    val cal = Calendar.getInstance()
    return (0..6).map { offset ->
        cal.timeInMillis = start
        cal.add(Calendar.DAY_OF_MONTH, offset)
        cal.timeInMillis
    }
}
