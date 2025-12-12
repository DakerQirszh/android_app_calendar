package com.example.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val description: String = "",

    // 当天日期（毫秒，0 点时间戳）
    val date: Long,

    // 提醒时间（毫秒，可为空）
    val time: Long? = null,

    // C：事件类别（0=工作，1=学习，2=生活，3=提醒，4=其他）
    val category: Int = 0,

    // D：是否完成
    val finished: Boolean = false
)
