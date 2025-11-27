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
    val reminders: List<ReminderItem> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccessful: Boolean = false,
    val error: String? = null
)

data class ReminderItem(val id: Long = 0L, val timeInMillis: Long, val description: String = "")
data class MediaItem(val id: Long = 0L, val uri: String, val description: String = "", val mediaType: String)

class AddEditNoteViewModel(
    private val repository: NoteRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private val alarmScheduler = AlarmScheduler(context)

    // Controlamos el trabajo de carga para no duplicar suscripciones
    private var loadJob: Job? = null

    // --- CORRECCIÓN DEL BUG DE ACTUALIZACIÓN ---
    fun loadNote(id: Long) {
        // Si ya estamos cargando este ID, cancelamos el anterior para reiniciar la escucha limpia
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            try {
                // CAMBIO CLAVE: Usamos .collect() en lugar de .first()
                // Esto mantiene el canal abierto. Si la BD cambia (al editar), esto se ejecuta de nuevo automáticamente.
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
                            // Mapeamos recordatorios si existen
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

    fun saveNote() {
        _uiState.update { it.copy(isSaving = true) }
        val current = _uiState.value
        viewModelScope.launch {
            try {
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

                // Programar alarma si es tarea futura
                if (current.isTask && current.taskDueDate != null && current.taskDueDate > System.currentTimeMillis()) {
                    // Limpieza básica de alarmas anteriores (simplificado)
                    current.reminders.forEach {
                        if (it.id != 0L) alarmScheduler.cancel(ReminderEntity(it.id, resultingId, it.timeInMillis))
                    }

                    val reminder = ReminderEntity(0, resultingId, current.taskDueDate - 600000) // 10 min antes
                    val rid = repository.addReminder(reminder)
                    alarmScheduler.schedule(reminder.copy(id = rid), "Recordatorio: ${current.title}")
                }

                // Guardar multimedia nueva
                current.mediaFiles.filter { it.id == 0L }.forEach { media ->
                    repository.addMedia(MediaEntity(0, resultingId, media.uri, media.mediaType, media.description))
                }

                _uiState.update { it.copy(isSaving = false, saveSuccessful = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun deleteNote() {
        val noteId = _uiState.value.noteId
        if (noteId != null && noteId != 0L) {
            viewModelScope.launch {
                try {
                    repository.eliminarPorId(noteId)
                    _uiState.update { AddEditUiState(saveSuccessful = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
                }
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