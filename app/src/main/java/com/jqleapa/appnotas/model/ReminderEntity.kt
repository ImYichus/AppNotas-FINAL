package com.jqlqapa.appnotas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Requisito: cantidad de recordatorios que el cliente requiera
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long, // FK a NoteEntity
    val reminderDateTime: Long // Fecha y hora del recordatorio
)