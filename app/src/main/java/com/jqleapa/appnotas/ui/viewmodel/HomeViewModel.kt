package com.jqlqapa.appnotas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.data.model.NoteWithMediaAndReminders // Importante
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteTab { NOTES, TASKS }

data class HomeUiState(
    // CAMBIO: Ahora la lista es de objetos complejos, no simples notas
    val currentList: List<NoteWithMediaAndReminders> = emptyList(),
    val selectedTab: NoteTab = NoteTab.NOTES,
    val isLoading: Boolean = false,
    val selectedNoteId: Long? = null

)

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {
    val allMedia = repository.getAllMedia()
    private val _selectedTab = MutableStateFlow(NoteTab.NOTES)
    private val _selectedNoteId = MutableStateFlow<Long?>(null)

    // Usamos los nuevos métodos del repositorio que traen multimedias y recordatorios
    private val allNotesFlow = repository.obtenerNotasConDetalles()
    private val allTasksFlow = repository.obtenerTareasConDetalles()

    private val allItemsFlow = combine(allNotesFlow, allTasksFlow) { notes, tasks ->
        notes + tasks
    }

    val uiState: StateFlow<HomeUiState> = allItemsFlow
        .combine(_selectedTab) { allItems, selectedTab ->
            val currentList = when (selectedTab) {
                NoteTab.NOTES -> allItems.filter { !it.note.isTask }
                NoteTab.TASKS -> allItems.filter { it.note.isTask }
            }
            HomeUiState(currentList = currentList, selectedTab = selectedTab, selectedNoteId = _selectedNoteId.value)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun selectTab(tab: NoteTab) { _selectedTab.value = tab; clearSelection() }
    fun selectNote(noteId: Long) { _selectedNoteId.value = noteId }
    fun clearSelection() { _selectedNoteId.value = null }

    fun toggleTaskCompletion(item: NoteWithMediaAndReminders) {
        viewModelScope.launch {
            repository.actualizar(item.note.copy(isCompleted = !item.note.isCompleted))
        }
    }

    fun deleteNote(item: NoteWithMediaAndReminders) {
        viewModelScope.launch {
            repository.eliminarPorId(item.note.id)
            if (_selectedNoteId.value == item.note.id) clearSelection()
        }
    }
}