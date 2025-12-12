package com.example.calendar.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var instance: EventDatabase? = null

    fun getDatabase(context: Context): EventDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                EventDatabase::class.java,
                "event_database"
            ).build()
        }
        return instance!!
    }
}
