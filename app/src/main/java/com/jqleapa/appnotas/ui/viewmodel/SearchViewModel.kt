package com.jqleapa.appnotas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.data.model.NoteEntity // ✅ Asegurar la importación
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// --- 1. DATA CLASS PARA EL ESTADO ---

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = false, // true solo para el estado inicial si la base de datos es lenta
    val isSearching: Boolean = false
)

// --- 2. IMPLEMENTACIÓN DEL VIEW MODEL ---

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: NoteRepository) : ViewModel() {

    // Estado interno para el texto de búsqueda ingresado por el usuario
    private val _searchQuery = MutableStateFlow("")

    // Estado que la UI observará
    val uiState: StateFlow<SearchUiState> = _searchQuery
        // Aplica debounce (300ms) para evitar consultas excesivas a la base de datos
        .debounce(300L)
        // flatMapLatest asegura que solo la última búsqueda activa es procesada
        .flatMapLatest { query ->
            // Si el query está vacío o solo tiene espacios, retorna un flujo vacío para no buscar
            if (query.isBlank()) {
                MutableStateFlow(emptyList())
            } else {
                // Llama al método del repositorio que busca en título y descripción
                repository.searchNotesAndTasks(query)
            }
        }
        // Combina el flujo de resultados con el query (del _searchQuery sin debounce)
        .combine(_searchQuery) { results, query ->
            SearchUiState(
                searchQuery = query,
                searchResults = results,
                isLoading = false,
                // isSearching es true si hay texto, incluso si está en debounce
                isSearching = query.isNotBlank()
            )
        }
        .stateIn(
            scope = viewModelScope,
            // Detiene la emisión después de 5 segundos de que no haya observadores
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState(isLoading = false) // Estado inicial sin carga
        )

    // Función llamada desde la UI cuando el usuario escribe
    fun updateSearchQuery(newQuery: String) {
        _searchQuery.update { newQuery }
    }
}