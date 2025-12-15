package com.example.calendar

import com.example.calendar.data.LunarCalendar
import com.example.calendar.data.Zodiac

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.calendar.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    // Android 13+ 通知权限
    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted -> 不强制处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 进入就申请一次（Android 13+ 才需要）
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val db = DatabaseProvider.getDatabase(this)
        val dao = db.eventDao()

        setContent {
            MaterialTheme {

                var selectedDate by remember { mutableStateOf(getToday()) }
                var viewMode by remember { mutableStateOf(CalendarViewMode.DAY) }

                var eventsForDay by remember { mutableStateOf<List<Event>>(emptyList()) }
                var datesWithEvents by remember { mutableStateOf<Set<Long>>(emptySet()) }

                var searchQuery by remember { mutableStateOf("") }

                var showAddDialog by remember { mutableStateOf(false) }

                var viewingEvent by remember { mutableStateOf<Event?>(null) }
                var showViewDialog by remember { mutableStateOf(false) }

                var editingEvent by remember { mutableStateOf<Event?>(null) }
                var showEditDialog by remember { mutableStateOf(false) }

                LaunchedEffect(selectedDate) {
                    eventsForDay = dao.getEventsByDate(selectedDate)
                }

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
                            selectedDate = selectedDate,
                            onDismiss = { showAddDialog = false },
                            onSave = { title, desc, category, reminderMillis ->
                                lifecycleScope.launch {
                                    val newEvent = Event(
                                        title = title,
                                        description = desc,
                                        date = selectedDate,
                                        category = category,
                                        time = reminderMillis
                                    )

                                    // ✅ insert 拿到真实 id
                                    val newId = dao.insertEvent(newEvent).toInt()
                                    val eventWithId = newEvent.copy(id = newId)

                                    // ✅ 安排提醒（如果 time != null 且未来）
                                    ReminderScheduler.schedule(this@MainActivity, eventWithId)

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
                                        // ✅ 删除前取消提醒
                                        ReminderScheduler.cancel(this@MainActivity, toDelete)

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
                    // 目前你的编辑弹窗只改标题/描述，不改 time
                    // 所以这里暂时不做 schedule/cancel；后续你要支持改提醒时间，再加即可
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

                            TabRow(selectedTabIndex = viewMode.ordinal) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = viewMode.ordinal == index,
                                        onClick = { viewMode = CalendarViewMode.values()[index] },
                                        text = { Text(title) }
                                    )
                                }
                            }

                            when (viewMode) {

                                CalendarViewMode.MONTH -> {
                                    Column {
                                        AndroidView(
                                            factory = { context ->
                                                CalendarView(context).apply {
                                                    setOnDateChangeListener { _, year, month, day ->
                                                        val cal = Calendar.getInstance().apply {
                                                            set(year, month, day, 0, 0, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        selectedDate = cal.timeInMillis
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(Modifier.height(16.dp))

                                        Button(
                                            onClick = { viewMode = CalendarViewMode.DAY },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("查看当天日程")
                                        }
                                    }
                                }

                                CalendarViewMode.WEEK -> {
                                    WeekView(
                                        selectedDate = selectedDate,
                                        datesWithEvents = datesWithEvents,
                                        onDateSelected = { date -> selectedDate = date },
                                        onEnterDayView = { viewMode = CalendarViewMode.DAY },
                                        onPrevWeek = { selectedDate = addDays(selectedDate, -7) },
                                        onNextWeek = { selectedDate = addDays(selectedDate, 7) }
                                    )
                                }

                                CalendarViewMode.DAY -> {
                                    DayView(
                                        selectedDate = selectedDate,
                                        eventsForDay = eventsForDay,
                                        searchQuery = searchQuery,
                                        onSearchChange = { searchQuery = it },
                                        onEventClick = { event ->
                                            viewingEvent = event
                                            showViewDialog = true
                                        },
                                        onToggleFinished = { event ->
                                            lifecycleScope.launch {
                                                dao.updateEvent(event.copy(finished = !event.finished))
                                                eventsForDay = dao.getEventsByDate(selectedDate)

                                                // ✅ 完成后就取消提醒；取消完成则重新安排（如果有 time）
                                                val updated = event.copy(finished = !event.finished)
                                                ReminderScheduler.cancel(this@MainActivity, updated)
                                                ReminderScheduler.schedule(this@MainActivity, updated)
                                            }
                                        },
                                        onDeleteEvent = { event ->
                                            lifecycleScope.launch {
                                                // ✅ 删除前取消提醒
                                                ReminderScheduler.cancel(this@MainActivity, event)

                                                dao.deleteEvent(event)
                                                eventsForDay = dao.getEventsByDate(selectedDate)
                                                refreshDatesWithEvents()
                                            }
                                        },
                                        onPrevDay = { selectedDate = addDays(selectedDate, -1) },
                                        onNextDay = { selectedDate = addDays(selectedDate, 1) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

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
                                                val updated = event.copy(finished = !event.finished)
                                                dao.updateEvent(updated)
                                                eventsForDay = dao.getEventsByDate(selectedDate)

                                                ReminderScheduler.cancel(this@MainActivity, updated)
                                                ReminderScheduler.schedule(this@MainActivity, updated)
                                            }
                                        },
                                        onClick = {
                                            viewingEvent = event
                                            showViewDialog = true
                                        },
                                        onDelete = {
                                            lifecycleScope.launch {
                                                ReminderScheduler.cancel(this@MainActivity, event)

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

enum class CalendarViewMode { MONTH, WEEK, DAY }

@Composable
fun DayView(
    selectedDate: Long,
    eventsForDay: List<Event>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEventClick: (Event) -> Unit,
    onToggleFinished: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Column {
        val title = remember(selectedDate) { dayTitleText(selectedDate) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevDay) { Text("前一天") }
            Spacer(Modifier.weight(1f))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onNextDay) { Text("后一天") }
        }

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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    onDateSelected: (Long) -> Unit,
    onPrevWeek: () -> Unit,
    onEnterDayView: () -> Unit,
    onNextWeek: () -> Unit
) {
    val weekDates = remember(selectedDate) { getWeekDates(selectedDate) }
    val title = remember(selectedDate) { weekRangeText(selectedDate) }

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevWeek) { Text("上周") }
            Spacer(Modifier.weight(1f))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onNextWeek) { Text("下周") }
        }

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

        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(onClick = { onEnterDayView() }) {
                Text("查看该日的日程")
            }
        }
    }
}

fun startOfWeek(date: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = date
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
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

fun addDays(date: Long, days: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = date }
    cal.add(Calendar.DAY_OF_MONTH, days)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun weekRangeText(selectedDate: Long): String {
    val start = startOfWeek(selectedDate)
    val end = addDays(start, 6)
    val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
    val yearSdf = java.text.SimpleDateFormat("yyyy年MM月", java.util.Locale.getDefault())
    val title = yearSdf.format(java.util.Date(start))
    return "$title  ${sdf.format(java.util.Date(start))}–${sdf.format(java.util.Date(end))}"
}

fun dayTitleText(date: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val sdf = java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.getDefault())
    val weekDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "周一"
        Calendar.TUESDAY -> "周二"
        Calendar.WEDNESDAY -> "周三"
        Calendar.THURSDAY -> "周四"
        Calendar.FRIDAY -> "周五"
        Calendar.SATURDAY -> "周六"
        else -> "周日"
    }
    return "${sdf.format(java.util.Date(date))}  $weekDay"
}
