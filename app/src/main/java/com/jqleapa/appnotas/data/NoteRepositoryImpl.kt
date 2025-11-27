package com.jqlqapa.appnotas.data

import com.jqlqapa.appnotas.data.model.MediaDao
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.data.model.NoteDao
import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.data.model.NoteWithMediaAndReminders
import com.jqlqapa.appnotas.data.model.ReminderDao
import com.jqlqapa.appnotas.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Implementación concreta de NoteRepository que utiliza los DAOs de Room.
 * Implementa los métodos con nombres en español de la interfaz.
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val mediaDao: MediaDao,
    private val reminderDao: ReminderDao
) : NoteRepository {

    // --- IMPLEMENTACIÓN DE OPERACIONES CRUD ---

    override suspend fun insertar(nota: NoteEntity): Long {
        return noteDao.insertNote(nota)
    }

    override suspend fun actualizar(nota: NoteEntity) {
        noteDao.updateNote(nota)
    }

    override suspend fun eliminarPorId(id: Long) {
        // Lógica de limpieza: Eliminar recordatorios y luego la nota.
        reminderDao.deleteRemindersForNote(id)
        noteDao.deleteNoteById(id)
    }

    override suspend fun obtenerPorId(id: Long): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    // --- IMPLEMENTACIÓN DE CONSULTAS ---

    override fun obtenerTodas(): Flow<List<NoteEntity>> {
        // Combina el flujo de notas y tareas (solución del bug anterior)
        return combine(obtenerNotas(), obtenerTareas()) { notes, tasks ->
            notes + tasks
        }
    }

    override fun obtenerNotas(): Flow<List<NoteEntity>> = noteDao.getAllNotesByRegistrationDate()

    override fun obtenerTareas(): Flow<List<NoteEntity>> = noteDao.getAllTasksByDueDate()

    override fun obtenerCompletadas(): Flow<List<NoteEntity>> {
        // Filtra las tareas completadas del flujo de todas las tareas
        return obtenerTareas().combine(obtenerTareas()) { _, tasks ->
            tasks.filter { it.isCompleted }
        }
    }

    override fun getNoteDetails(id: Long): Flow<NoteWithMediaAndReminders> = noteDao.getNoteWithDetails(id)

    override fun searchNotesAndTasks(query: String): Flow<List<NoteEntity>> = noteDao.searchNotesAndTasks(query)

    // --- IMPLEMENTACIÓN MULTIMEDIA ---

    override fun getAllMedia(): Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    override suspend fun addMedia(media: MediaEntity) {
        mediaDao.insertMedia(media)
    }

    override suspend fun deleteMedia(media: MediaEntity) {
        mediaDao.deleteMedia(media)
    }

    // --- IMPLEMENTACIÓN RECORDATORIOS ---

    override suspend fun addReminder(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }
}