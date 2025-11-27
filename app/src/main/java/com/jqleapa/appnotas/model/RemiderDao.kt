package com.jqlqapa.appnotas.data.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.jqlqapa.appnotas.data.model.ReminderEntity
@Dao
interface ReminderDao {
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long // Devuelve el ID para programar la alarma

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE noteId = :noteId")
    fun getRemindersForNote(noteId: Long): Flow<List<ReminderEntity>>

    // Útil para limpiar todos los recordatorios de una nota al eliminarla
    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteRemindersForNote(noteId: Long)

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<ReminderEntity>
}