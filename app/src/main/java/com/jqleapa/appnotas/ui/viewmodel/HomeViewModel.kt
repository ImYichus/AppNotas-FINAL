package com.jqlqapa.appnotas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.data.model.NoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// --- 1. ENUM Y DATA CLASSES PARA EL ESTADO ---

enum class NoteTab {
    NOTES, TASKS
}

data class HomeUiState(
    val currentList: List<NoteEntity> = emptyList(),
    val selectedTab: NoteTab = NoteTab.NOTES,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedNoteId: Long? = null // Para el diseño Maestro-Detalle
)

// --- 2. VIEW MODEL IMPLEMENTACIÓN ---

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _selectedTab = MutableStateFlow(NoteTab.NOTES)
    private val _selectedNoteId = MutableStateFlow<Long?>(null)

    // CORRECCIÓN CLAVE: Usamos los métodos en español del repositorio
    private val allNotesFlow = repository.obtenerNotas() // ⬅️ Antes: getAllNotes()
    private val allTasksFlow = repository.obtenerTareas() // ⬅️ Antes: getAllTasks()

    // Flujo que combina ambas listas para tener todos los ítems (Notas + Tareas)
    private val allItemsFlow = combine(allNotesFlow, allTasksFlow) { notes, tasks ->
        notes + tasks // Une la lista de notas y la lista de tareas
    }

    // 3. Estado Combinado que la UI va a observar
    val uiState: StateFlow<HomeUiState> = allItemsFlow
        .combine(_selectedTab) { allItems, selectedTab ->

            // Lógica de filtrado: solo se muestran los ítems según la pestaña seleccionada
            val currentList = when (selectedTab) {
                // Se hace el filtro explícito para resolver las ambigüedades
                NoteTab.NOTES -> allItems.filter { note -> !note.isTask } // Filtra donde isTask es FALSE (Notas)
                NoteTab.TASKS -> allItems.filter { note -> note.isTask }  // Filtra donde isTask es TRUE (Tareas)
            }

            HomeUiState(
                currentList = currentList,
                selectedTab = selectedTab,
                selectedNoteId = _selectedNoteId.value,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true) // Estado inicial de carga
        )

    // Función para cambiar la pestaña
    fun selectTab(tab: NoteTab) {
        _selectedTab.value = tab
        clearSelection() // Opcional: limpiar la selección al cambiar de pestaña
    }

    // Función para seleccionar una nota (llamada desde HomeScreen)
    fun selectNote(noteId: Long) {
        _selectedNoteId.value = noteId
    }

    // Función para cerrar el panel de detalle (llamada desde HomeScreen)
    fun clearSelection() {
        _selectedNoteId.value = null
    }

    // Función para marcar una tarea como cumplida
    fun toggleTaskCompletion(task: NoteEntity) {
        if (task.isTask) {
            viewModelScope.launch {
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                // CORRECCIÓN CLAVE: Usamos 'actualizar' para la tarea existente
                repository.actualizar(updatedTask) // ⬅️ Antes: saveNote()
            }
        }
    }

    // Función para eliminar una nota/tarea
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            // CORRECCIÓN CLAVE: Usamos 'eliminarPorId' en lugar de deleteNote(note)
            repository.eliminarPorId(note.id) // ⬅️ Antes: deleteNote(note)
            if (_selectedNoteId.value == note.id) {
                clearSelection() // Limpiar selección si se elimina la nota de detalle
            }
        }
    }

    // Flujo de todos los archivos multimedia (se mantiene)
    val allMedia = repository.getAllMedia()

    fun addMedia(media: MediaEntity) {
        viewModelScope.launch {
            repository.addMedia(media)
        }
    }
}