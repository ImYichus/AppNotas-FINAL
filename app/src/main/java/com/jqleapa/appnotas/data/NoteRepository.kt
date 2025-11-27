package com.jqlqapa.appnotas.data

import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.data.model.ReminderEntity
import com.jqlqapa.appnotas.data.model.NoteWithMediaAndReminders
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repositorio de Notas.
 * Utiliza los métodos de acceso a datos en español solicitados
 * y mantiene las funcionalidades de multimedia/recordatorios requeridas.
 */
interface NoteRepository {

    // --- OPERACIONES CRUD BÁSICAS (Adaptadas a tu solicitud) ---
    // Devuelve el ID Long de la nota creada/actualizada
    suspend fun insertar(nota: NoteEntity): Long
    suspend fun actualizar(nota: NoteEntity)
    suspend fun eliminarPorId(id: Long)

    suspend fun obtenerPorId(id: Long): NoteEntity?

    // --- CONSULTAS (Adaptadas a tu solicitud) ---

    // Combina todas las notas y tareas
    fun obtenerTodas(): Flow<List<NoteEntity>>

    // Solo notas (isTask = false)
    fun obtenerNotas(): Flow<List<NoteEntity>>

    // Solo tareas (isTask = true)
    fun obtenerTareas(): Flow<List<NoteEntity>>

    // Tareas completadas
    fun obtenerCompletadas(): Flow<List<NoteEntity>>

    // Detalles (requerido para NoteDetailScreen)
    fun getNoteDetails(id: Long): Flow<NoteWithMediaAndReminders>

    // Búsqueda (requerido para SearchViewModel)
    fun searchNotesAndTasks(query: String): Flow<List<NoteEntity>>

    // --- MULTIMEDIA (Requerido por Add/Edit y Home ViewModels) ---
    fun getAllMedia(): Flow<List<MediaEntity>>
    suspend fun addMedia(media: MediaEntity)
    suspend fun deleteMedia(media: MediaEntity)

    // --- RECORDATORIOS (Requerido por Add/Edit ViewModels) ---
    suspend fun addReminder(reminder: ReminderEntity): Long
    suspend fun deleteReminder(reminder: ReminderEntity)
}