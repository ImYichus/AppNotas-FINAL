package com.jqlqapa.appnotas.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.data.model.ReminderEntity
import com.jqlqapa.appnotas.utils.AlarmScheduler

// Estados de la UI
data class AddEditUiState(
    val noteId: Long? = null,
    val title: String = "",
    val description: String = "",
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val taskDueDate: Long? = null,
    val mediaFiles: List<MediaItem> = emptyList(),
    // Lista de recordatorios en memoria (lo que ves en pantalla antes de guardar)
    val reminders: List<ReminderItem> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccessful: Boolean = false,
    val error: String? = null
)

// Modelos para la UI
data class ReminderItem(val id: Long = 0L, val timeInMillis: Long, val description: String = "")
data class MediaItem(val id: Long = 0L, val uri: String, val description: String = "", val mediaType: String)

class AddEditNoteViewModel(
    private val repository: NoteRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private val alarmScheduler = AlarmScheduler(context)
    private var loadJob: Job? = null

    // ----------------------------------------------------------------
    // 1. GESTIÓN DE RECORDATORIOS (EN PANTALLA)
    // ----------------------------------------------------------------

    fun addReminder(timeInMillis: Long) {
        val newReminder = ReminderItem(id = 0, timeInMillis = timeInMillis, description = "Recordatorio")
        _uiState.update { it.copy(reminders = it.reminders + newReminder) }
    }

    fun removeReminder(reminder: ReminderItem) {
        _uiState.update { it.copy(reminders = it.reminders - reminder) }
    }

    fun updateReminder(oldReminder: ReminderItem, newTime: Long) {
        _uiState.update { state ->
            val updatedList = state.reminders.map {
                if (it == oldReminder) it.copy(timeInMillis = newTime) else it
            }
            state.copy(reminders = updatedList)
        }
    }

    // ----------------------------------------------------------------
    // 2. CARGA DE DATOS (LECTURA EN VIVO)
    // ----------------------------------------------------------------

    fun loadNote(id: Long) {
        // Cancelamos trabajos anteriores para evitar duplicados
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            try {
                // Usamos collect() para mantener la pantalla actualizada si la BD cambia
                repository.getNoteDetails(id).collect { noteDetails ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            noteId = id,
                            title = noteDetails.note.title,
                            description = noteDetails.note.description,
                            isTask = noteDetails.note.isTask,
                            isCompleted = noteDetails.note.isCompleted,
                            taskDueDate = noteDetails.note.taskDueDate,
                            // Mapeamos los archivos guardados
                            mediaFiles = noteDetails.media.map {
                                MediaItem(it.id, it.filePath, it.description ?: "", it.mediaType ?: "UNKNOWN")
                            },
                            // Mapeamos los recordatorios guardados
                            reminders = noteDetails.reminders.map {
                                ReminderItem(it.id, it.reminderDateTime, "Recordatorio")
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar: ${e.message}") }
            }
        }
    }

    // ----------------------------------------------------------------
    // 3. GUARDADO (LA PARTE CRÍTICA CORREGIDA)
    // ----------------------------------------------------------------

    fun saveNote() {
        _uiState.update { it.copy(isSaving = true) }
        val current = _uiState.value

        viewModelScope.launch {
            try {
                // A) Guardar o Actualizar la Nota Principal
                val noteEntity = NoteEntity(
                    id = current.noteId ?: 0L,
                    title = current.title,
                    description = current.description,
                    isTask = current.isTask,
                    isCompleted = current.isCompleted,
                    taskDueDate = current.taskDueDate,
                    // Si es edición, intentamos mantener la fecha original, si no, actual
                    registrationDate = System.currentTimeMillis()
                )

                val resultingId: Long
                if (current.noteId != null && current.noteId != 0L) {
                    repository.actualizar(noteEntity)
                    resultingId = current.noteId
                } else {
                    resultingId = repository.insertar(noteEntity)
                }

                // B) Gestión de Recordatorios (Limpieza e Inserción)

                // 1. Borrar viejos (Para evitar duplicados o basura)
                if (current.noteId != null && current.noteId != 0L) {
                    val oldDetails = repository.getNoteDetails(resultingId).first()
                    oldDetails.reminders.forEach { oldRem ->
                        alarmScheduler.cancel(oldRem)
                        repository.deleteReminder(oldRem)
                    }
                }

                // 2. Insertar los nuevos de la lista actual
                current.reminders.forEach { uiRem ->

                    // Creamos la entidad
                    val newEntity = ReminderEntity(0, resultingId, uiRem.timeInMillis)

                    // [CORRECCIÓN] Guardamos en BD SIEMPRE (aunque la hora haya pasado)
                    val newId = repository.addReminder(newEntity)

                    // [CORRECCIÓN] Programamos la alarma SOLO si es futuro
                    if (uiRem.timeInMillis > System.currentTimeMillis()) {
                        // Usamos el ID real de la BD para poder cancelarla luego
                        alarmScheduler.schedule(newEntity.copy(id = newId), "Recordatorio: ${current.title}")
                    }
                }

                // C) Guardar Multimedia (Solo los nuevos que tienen ID 0)
                current.mediaFiles.filter { it.id == 0L }.forEach { media ->
                    repository.addMedia(MediaEntity(0, resultingId, media.uri, media.mediaType, media.description))
                }

                _uiState.update { it.copy(isSaving = false, saveSuccessful = true) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // ----------------------------------------------------------------
    // 4. BORRADO Y AUXILIARES
    // ----------------------------------------------------------------

    fun deleteNote() {
        val noteId = _uiState.value.noteId
        if (noteId != null && noteId != 0L) {
            viewModelScope.launch {
                try {
                    // Cancelar alarmas del sistema antes de borrar de BD
                    _uiState.value.reminders.forEach {
                        alarmScheduler.cancel(ReminderEntity(it.id, noteId, it.timeInMillis))
                    }
                    repository.eliminarPorId(noteId)
                    _uiState.update { AddEditUiState(saveSuccessful = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
                }
            }
        }
    }

    fun addMediaItem(item: MediaItem) {
        _uiState.update { it.copy(mediaFiles = it.mediaFiles + item) }
    }

    fun deleteMedia(item: MediaItem) {
        _uiState.update { it.copy(mediaFiles = it.mediaFiles - item) }
        // Si ya estaba guardado en BD (ID != 0), lo borramos de la BD
        if (item.id != 0L) {
            viewModelScope.launch {
                repository.deleteMedia(MediaEntity(item.id, _uiState.value.noteId ?: 0, item.uri, item.mediaType, item.description))
            }
        }
    }

    // Setters simples para actualizar la UI mientras escribes
    fun updateTitle(s: String) { _uiState.update { it.copy(title = s) } }
    fun updateDescription(s: String) { _uiState.update { it.copy(description = s) } }
    fun updateIsTask(b: Boolean) { _uiState.update { it.copy(isTask = b) } }
    fun toggleCompletion(b: Boolean) { _uiState.update { it.copy(isCompleted = b) } }
    fun updateTaskDueDate(l: Long?) { _uiState.update { it.copy(taskDueDate = l) } }
    fun saveComplete() { _uiState.update { it.copy(saveSuccessful = false) } }
}