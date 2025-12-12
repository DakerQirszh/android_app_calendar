package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

@Dao
interface EventDao {

    @Insert
    suspend fun insertEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE date = :date")
    suspend fun getEventsByDate(date: Long): List<Event>

    @Query("SELECT * FROM events ORDER BY date ASC")
    suspend fun getAllEvents(): List<Event>
}
