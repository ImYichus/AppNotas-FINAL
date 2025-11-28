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

data class AddEditUiState(
    val noteId: Long? = null,
    val title: String = "",
    val description: String = "",
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val taskDueDate: Long? = null,
    val mediaFiles: List<MediaItem> = emptyList(),
    // Lista de recordatorios en memoria (antes de guardar)
    val reminders: List<ReminderItem> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccessful: Boolean = false,
    val error: String? = null
)

// Usamos un ID temporal negativo para los nuevos recordatorios que aún no van a BD
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

    // --- GESTIÓN DE RECORDATORIOS EN UI ---

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

    // --- CARGA DE DATOS ---

    fun loadNote(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                repository.getNoteDetails(id).collect { noteDetails ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            noteId = id,
                            title = noteDetails.note.title,
                            description = noteDetails.note.description,
                            isTask = noteDetails.note.isTask,
                            isCompleted = noteDetails.note.isCompleted,
                            taskDueDate = noteDetails.note.taskDueDate,
                            mediaFiles = noteDetails.media.map {
                                MediaItem(it.id, it.filePath, it.description ?: "", it.mediaType ?: "UNKNOWN")
                            },
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

    // --- GUARDADO ROBUSTO (Elimina viejos -> Crea nuevos) ---

    fun saveNote() {
        _uiState.update { it.copy(isSaving = true) }
        val current = _uiState.value
        viewModelScope.launch {
            try {
                // 1. Guardar/Actualizar Nota
                val noteEntity = NoteEntity(
                    id = current.noteId ?: 0L,
                    title = current.title,
                    description = current.description,
                    isTask = current.isTask,
                    isCompleted = current.isCompleted,
                    taskDueDate = current.taskDueDate
                )

                val resultingId: Long
                if (current.noteId != null && current.noteId != 0L) {
                    repository.actualizar(noteEntity)
                    resultingId = current.noteId
                } else {
                    resultingId = repository.insertar(noteEntity)
                }

                // 2. GESTIÓN DE RECORDATORIOS (Sincronización Total)

                // A) Si la nota ya existía, obtenemos los recordatorios VIEJOS de la BD
                if (current.noteId != null && current.noteId != 0L) {
                    val oldDetails = repository.getNoteDetails(resultingId).first()
                    // B) Los borramos TODOS de la BD y cancelamos sus alarmas
                    // Esto es más seguro que intentar adivinar cuál cambió. Borrón y cuenta nueva.
                    oldDetails.reminders.forEach { oldRem ->
                        alarmScheduler.cancel(oldRem)
                        repository.deleteReminder(oldRem)
                    }
                }

                // C) Insertamos los recordatorios que están AHORA en la lista de la UI
                current.reminders.forEach { uiRem ->
                    // Solo programamos si es futuro
                    if (uiRem.timeInMillis > System.currentTimeMillis()) {
                        val newEntity = ReminderEntity(0, resultingId, uiRem.timeInMillis)
                        val newId = repository.addReminder(newEntity) // Insertamos y obtenemos ID real

                        // Programamos la alarma con el ID real
                        alarmScheduler.schedule(newEntity.copy(id = newId), "Recordatorio: ${current.title}")
                    }
                }

                // 3. Guardar Multimedia (Solo nuevos)
                current.mediaFiles.filter { it.id == 0L }.forEach { media ->
                    repository.addMedia(MediaEntity(0, resultingId, media.uri, media.mediaType, media.description))
                }

                _uiState.update { it.copy(isSaving = false, saveSuccessful = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // --- OTRAS ---
    fun deleteNote() {
        val noteId = _uiState.value.noteId
        if (noteId != null && noteId != 0L) {
            viewModelScope.launch {
                // Cancelamos alarmas antes de borrar
                _uiState.value.reminders.forEach {
                    alarmScheduler.cancel(ReminderEntity(it.id, noteId, it.timeInMillis))
                }
                repository.eliminarPorId(noteId)
                _uiState.update { AddEditUiState(saveSuccessful = true) }
            }
        }
    }

    fun addMediaItem(item: MediaItem) { _uiState.update { it.copy(mediaFiles = it.mediaFiles + item) } }

    fun deleteMedia(item: MediaItem) {
        _uiState.update { it.copy(mediaFiles = it.mediaFiles - item) }
        if (item.id != 0L) viewModelScope.launch {
            repository.deleteMedia(MediaEntity(item.id, _uiState.value.noteId ?: 0, item.uri, item.mediaType, item.description))
        }
    }

    fun updateTitle(s: String) { _uiState.update { it.copy(title = s) } }
    fun updateDescription(s: String) { _uiState.update { it.copy(description = s) } }
    fun updateIsTask(b: Boolean) { _uiState.update { it.copy(isTask = b) } }
    fun toggleCompletion(b: Boolean) { _uiState.update { it.copy(isCompleted = b) } }
    fun updateTaskDueDate(l: Long?) { _uiState.update { it.copy(taskDueDate = l) } }
    fun saveComplete() { _uiState.update { it.copy(saveSuccessful = false) } }
}