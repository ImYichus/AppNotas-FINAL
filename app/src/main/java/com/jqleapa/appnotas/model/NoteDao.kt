package com.jqlqapa.appnotas.data.model
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // En tu archivo NoteDao.kt, agrega esta línea
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
    // CRUD Básico
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long // Devuelve el ID de la nota creada/actualizada

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Consultas Específicas
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    // Requisito: Las notas (no tareas) deben estar ordenadas por fecha de registro.
    @Query("SELECT * FROM notes WHERE isTask = 0 ORDER BY registrationDate DESC")
    fun getAllNotesByRegistrationDate(): Flow<List<NoteEntity>>

    // Requisito: Las tareas deben estar ordenadas por fecha de en qué deben realizarse.
    @Query("SELECT * FROM notes WHERE isTask = 1 ORDER BY taskDueDate ASC")
    fun getAllTasksByDueDate(): Flow<List<NoteEntity>>

    // Requisito: Búsqueda sobre los campos descripción y nombre (título) de la nota.
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchNotesAndTasks(query: String): Flow<List<NoteEntity>>

    // Transacción para obtener Nota con todos sus detalles (medios y recordatorios)
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteWithDetails(id: Long): Flow<NoteWithMediaAndReminders>
}