package com.jqlqapa.appnotas.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jqlqapa.appnotas.data.model.MediaDao
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.data.model.NoteDao
import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.data.model.ReminderDao
import com.jqlqapa.appnotas.data.model.ReminderEntity

@Database(
    entities = [NoteEntity::class, MediaEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Declaramos los DAOs que Room debe implementar
    abstract fun noteDao(): NoteDao
    abstract fun mediaDao(): MediaDao
    abstract fun reminderDao(): ReminderDao
}