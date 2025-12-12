package com.example.calendar.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Event::class],
    version = 2,          // ← 删除旧数据库后提升版本即可
    exportSchema = false
)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
