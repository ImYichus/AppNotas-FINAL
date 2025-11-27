package com.jqleapa.appnotas.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.data.NoteRepository
//import com.jqleapa.appnotas.ui.navigation.AppScreens
import com.jqleapa.appnotas.ui.viewmodel.SearchViewModel
import com.jqlqapa.appnotas.data.model.NoteEntity
import com.jqlqapa.appnotas.ui.navigation.AppScreens
import com.jqlqapa.appnotas.ui.screens.NoteCard

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
    navController: NavHostController,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(AppDataContainer.noteRepository)
    )
) {
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { padding ->
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
                    label = { Text("Escribe para buscar notas o tareas...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (uiState.isLoading) {
                    // Muestra un indicador de carga si el estado lo requiere
                    CircularProgressIndicator(Modifier.padding(top = 32.dp))
                } else if (uiState.isSearching && uiState.searchResults.isEmpty()) {
                    // Muestra mensaje si está buscando pero no hay resultados
                    Text(
                        "No se encontraron resultados para \"${uiState.searchQuery}\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 32.dp)
                    )
                } else if (!uiState.isSearching && uiState.searchResults.isEmpty()) {
                    // Muestra mensaje si aún no ha buscado
                    Text(
                        "Comienza a escribir para buscar en el título y la descripción de tus notas.",
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
                            NoteCard(
                                note = note,
                                onClick = {
                                    navController.navigate(AppScreens.EditNote.route.replace("{noteId}", note.id.toString()))
                                },
                                onDelete = {

                                },
                                onToggleCompletion = {

                                }
                            )
                        }
                    }
                }
            }
        }
    )
}