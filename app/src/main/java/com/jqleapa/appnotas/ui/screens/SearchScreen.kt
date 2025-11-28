package com.jqlqapa.appnotas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqleapa.appnotas.ui.viewmodel.SearchViewModel
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.ui.navigation.AppScreens
import com.jqlqapa.appnotas.data.model.NoteWithMediaAndReminders

// Factory para crear el SearchViewModel
class SearchViewModelFactory(
    private val noteRepository: NoteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(noteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController
) {
    // Instanciamos el ViewModel usando la Factory
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(AppDataContainer.noteRepository)
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campo de Búsqueda
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Escribe para buscar...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Contenido
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.padding(top = 32.dp))
            } else if (uiState.isSearching && uiState.searchResults.isEmpty()) {
                Text(
                    "No se encontraron resultados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else if (!uiState.isSearching && uiState.searchResults.isEmpty()) {
                Text(
                    "Busca en tus notas y tareas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                // Lista de Resultados
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.searchResults, key = { it.id }) { note ->

                        // Adaptador: Creamos un objeto completo falso (sin media ni recordatorios)
                        // para que NoteCard lo acepte.
                        val fakeItem = NoteWithMediaAndReminders(
                            note = note,
                            media = emptyList(),
                            reminders = emptyList()
                        )

                        NoteCard(
                            item = fakeItem,
                            onClick = {
                                navController.navigate(AppScreens.NoteDetail.withArgs(note.id.toString()))
                            },
                            onDelete = { /* La búsqueda es solo lectura por ahora */ },
                            onToggleCompletion = { /* La búsqueda es solo lectura por ahora */ } // <--- CORREGIDO AQUÍ
                        )
                    }
                }
            }
        }
    }
}