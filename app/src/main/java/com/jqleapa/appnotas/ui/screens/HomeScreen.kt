package com.jqlqapa.appnotas.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jqlqapa.appnotas.ui.navigation.AppScreens
import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModel
import com.jqlqapa.appnotas.ui.viewmodel.NoteTab
import com.jqlqapa.appnotas.data.model.NoteWithMediaAndReminders // <--- CAMBIO IMPORTANTE
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.ui.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

// Definición de colores para adaptabilidad (TUS COLORES ORIGINALES)
val phonePrimary = Color(0xFF4CAF50) // Verde
val tabletPrimary = Color(0xFF9C27B0) // Morado

// ----------------------------------------------------
// LÓGICA DE ADAPTACIÓN DE PANTALLA
// ----------------------------------------------------

enum class WindowType { Phone, Tablet }

@Composable
fun rememberWindowType(): WindowType {
    val configuration = LocalConfiguration.current
    return if (configuration.screenWidthDp >= 600) WindowType.Tablet else WindowType.Phone
}

@Composable
fun getAdaptiveColorScheme(windowType: WindowType): ColorScheme {
    val baseColorScheme = MaterialTheme.colorScheme
    return remember(windowType) {
        when (windowType) {
            WindowType.Phone -> baseColorScheme.copy(primary = phonePrimary, primaryContainer = phonePrimary.copy(alpha = 0.8f))
            WindowType.Tablet -> baseColorScheme.copy(primary = tabletPrimary, primaryContainer = tabletPrimary.copy(alpha = 0.8f))
        }
    }
}

// COMPONENTE PRINCIPAL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
) {
    val factory = AppDataContainer.homeViewModelFactory
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    val windowType = rememberWindowType()
    val adaptiveColorScheme = getAdaptiveColorScheme(windowType)

    MaterialTheme(colorScheme = adaptiveColorScheme) {
        Scaffold(
            topBar = { HomeTopBar(onSearchClick = { navController.navigate(AppScreens.Search.route) }) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (windowType == WindowType.Tablet && uiState.selectedNoteId != null) {
                            viewModel.clearSelection()
                        } else {
                            navController.navigate(AppScreens.AddNote.route)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val icon = if (windowType == WindowType.Tablet && uiState.selectedNoteId != null)
                        Icons.Default.Close else Icons.Default.Add
                    Icon(icon, contentDescription = "Acción", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { padding ->
            if (windowType == WindowType.Tablet) {
                TabletLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNoteClick = { id -> viewModel.selectNote(id) },
                    onToggleCompletion = viewModel::toggleTaskCompletion,
                    onDelete = viewModel::deleteNote,
                    onEditClick = { id -> navController.navigate(AppScreens.EditNote.withArgs(id.toString())) },
                    modifier = Modifier.padding(padding)
                )
            } else {
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    HomeTabRow(selectedTab = uiState.selectedTab, onTabSelected = viewModel::selectTab)
                    when {
                        uiState.isLoading -> LoadingScreen()
                        uiState.currentList.isEmpty() -> EmptyState(uiState.selectedTab)
                        else -> NoteTaskList(
                            notes = uiState.currentList,
                            onNoteClick = { id -> navController.navigate(AppScreens.NoteDetail.withArgs(id.toString())) },
                            onToggleCompletion = viewModel::toggleTaskCompletion,
                            onDelete = viewModel::deleteNote
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DISEÑO TABLET
// ----------------------------------------------------
@Composable
fun TabletLayout(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onNoteClick: (Long) -> Unit,
    onToggleCompletion: (NoteWithMediaAndReminders) -> Unit, // CAMBIO DE TIPO
    onDelete: (NoteWithMediaAndReminders) -> Unit, // CAMBIO DE TIPO
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Panel Izquierdo (Lista)
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
            HomeTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> viewModel.selectTab(tab); viewModel.clearSelection() }
            )
            when {
                uiState.isLoading -> LoadingScreen()
                uiState.currentList.isEmpty() -> EmptyState(uiState.selectedTab)
                else -> NoteTaskList(
                    notes = uiState.currentList,
                    onNoteClick = onNoteClick,
                    onToggleCompletion = onToggleCompletion,
                    onDelete = onDelete,
                    selectedNoteId = uiState.selectedNoteId
                )
            }
        }

        // Separador
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxHeight().width(1.dp)
        )

        // Panel Derecho (Detalle)
        Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
            val selectedNoteId = uiState.selectedNoteId
            if (selectedNoteId != null) {
                // Usamos el componente compartido
                com.jqlqapa.appnotas.ui.screens.NoteDetailContent(
                    noteId = selectedNoteId,
                    modifier = Modifier.fillMaxSize(),
                    onEditClick = { onEditClick(selectedNoteId) },
                    onDeleteConfirmed = { viewModel.clearSelection() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Selecciona un elemento para ver detalles.", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTES LISTA
// ----------------------------------------------------
@Composable
fun NoteTaskList(
    notes: List<NoteWithMediaAndReminders>, // CAMBIO DE TIPO: Recibe el objeto completo
    onNoteClick: (Long) -> Unit,
    onToggleCompletion: (NoteWithMediaAndReminders) -> Unit,
    onDelete: (NoteWithMediaAndReminders) -> Unit,
    selectedNoteId: Long? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.note.id }) { item ->
            NoteCard(
                item = item, // Pasamos el ITEM completo
                onClick = { onNoteClick(item.note.id) },
                onToggleCompletion = { onToggleCompletion(item) },
                onDelete = { onDelete(item) },
                isSelected = item.note.id == selectedNoteId
            )
        }
    }
}

@Composable
fun NoteCard(
    item: NoteWithMediaAndReminders, // CAMBIO: Recibimos el objeto complejo
    onClick: () -> Unit,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false
) {
    // Desestructuramos para facilitar el uso
    val note = item.note
    val media = item.media
    val reminders = item.reminders

    // Calculamos contadores
    val imgCount = media.count { it.mediaType == "IMAGE" }
    val vidCount = media.count { it.mediaType == "VIDEO" }
    val audCount = media.count { it.mediaType == "AUDIO" }
    val remCount = reminders.size

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila Superior (Título y Acciones)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val titleStyle = if (note.isTask && note.isCompleted) TextStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray) else LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)

                Text(
                    text = note.title,
                    style = titleStyle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isTask) Checkbox(checked = note.isCompleted, onCheckedChange = { onToggleCompletion() })
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Descripción
            Text(text = note.description.take(50) + if (note.description.length > 50) "..." else "", color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)

            // --- NUEVA SECCIÓN DE INFORMACIÓN (ICONOS) ---
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Multimedia
                if (imgCount > 0) MiniInfoChip(Icons.Default.Image, "$imgCount")
                if (vidCount > 0) MiniInfoChip(Icons.Default.Videocam, "$vidCount")
                if (audCount > 0) MiniInfoChip(Icons.Default.Mic, "$audCount")

                // Tareas
                if (note.isTask) {
                    if (remCount > 0) MiniInfoChip(Icons.Default.Alarm, "$remCount", MaterialTheme.colorScheme.primary)

                    // Fecha alineada a la derecha si hay espacio, o seguida
                    if (note.taskDueDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(Date(note.taskDueDate)),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Componente auxiliar para los iconos pequeños (Mantiene tu estilo)
@Composable
fun MiniInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, fontSize = 12.sp, color = tint)
    }
}

// ----------------------------------------------------
// COMPONENTES UI (Barra Superior y Tabs - Intactos)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onSearchClick: () -> Unit) {
    TopAppBar(
        title = { Text("Mis Notas y Tareas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
        actions = { IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onPrimary) } }
    )
}

@Composable
fun HomeTabRow(selectedTab: NoteTab, onTabSelected: (NoteTab) -> Unit) {
    val tabs = listOf(Triple(NoteTab.NOTES, "Notas", Icons.Filled.Description), Triple(NoteTab.TASKS, "Tareas", Icons.Filled.Checklist))
    TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }) {
        tabs.forEach { (tab, title, icon) ->
            Tab(selected = tab == selectedTab, onClick = { onTabSelected(tab) }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurface, icon = { Icon(icon, title) }, text = { Text(title, fontWeight = FontWeight.Bold) })
        }
    }
}

@Composable
fun EmptyState(tab: NoteTab) {
    val message = if (tab == NoteTab.NOTES) "¡No tienes notas! Toca '+'." else "¡No tienes tareas! Toca '+'."
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LoadingScreen() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }