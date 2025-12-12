package com.example.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event

@Composable
fun EventList(
    events: List<Event>,
    onDelete: (Event) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events) { event ->
            EventItem(event = event, onDelete = onDelete)
        }
    }
}

@Composable
fun EventItem(
    event: Event,
    onDelete: (Event) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                if (event.description.isNotEmpty())
                    Text(event.description, style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = { onDelete(event) }) {
                Icon(Icons.Default.Delete, contentDescription = "delete")
            }
        }
    }
}
