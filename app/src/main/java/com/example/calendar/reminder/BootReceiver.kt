package com.example.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.calendar.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = DatabaseProvider.getDatabase(context).eventDao()
                val all = dao.getAllEvents()
                val now = System.currentTimeMillis()

                all.filter { e ->
                    e.time != null && e.time!! > now && !e.finished
                }.forEach { e ->
                    ReminderScheduler.schedule(context, e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
