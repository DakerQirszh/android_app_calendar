package com.example.calendar.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.calendar.data.Event

object ReminderScheduler {

    fun schedule(context: Context, event: Event) {
        val triggerAt = event.time ?: return
        if (event.finished) return

        val now = System.currentTimeMillis()
        if (triggerAt <= now) return  // 过去时间不安排

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, event)

        // 闹钟
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pi
        )
    }

    fun cancel(context: Context, event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, event)
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(context: Context, event: Event): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, event.title)
            putExtra(AlarmReceiver.EXTRA_DESC, event.description)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            flags
        )
    }
}
