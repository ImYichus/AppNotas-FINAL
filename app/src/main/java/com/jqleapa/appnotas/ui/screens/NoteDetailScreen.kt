package com.jqlqapa.appnotas.ui.screens
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqlqapa.appnotas.ui.viewmodel.AddEditNoteViewModel
import com.jqlqapa.appnotas.ui.viewmodel.AddEditUiState
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.ui.viewmodel.MediaItem
import com.jqlqapa.appnotas.ui.navigation.AppScreens
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.utils.FILE_PROVIDER_AUTHORITY
import com.jqlqapa.appnotas.ui.components.MediaPlayerDialog // <--- IMPORTANTE: Tu nuevo componente
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

// --------------------------------------------------------------------------------
// 1. PANTALLA PRINCIPAL (Wrapper con ViewModel para Navegación)
// --------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    navController: NavHostController
) {
    val context = LocalContext.current

    val viewModel: AddEditNoteViewModel = viewModel(
        factory = AddEditViewModelFactory(
            noteRepository = AppDataContainer.noteRepository,
            context = context,
            editId = noteId
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // Llamamos a la versión visual
    NoteDetailContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(AppScreens.EditNote.withArgs(noteId.toString())) }
    )
}

// --------------------------------------------------------------------------------
// 2. CONTENIDO VISUAL PURO (Interfaz Gráfica)
// --------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailContent(
    uiState: AddEditUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Estado para controlar qué medio se está reproduciendo internamente
    var mediaToPlay by remember { mutableStateOf<MediaItem?>(null) }

    // Si hay un medio seleccionado (Audio/Video), mostramos el diálogo flotante
    if (mediaToPlay != null) {
        MediaPlayerDialog(
            uri = mediaToPlay!!.uri,
            mediaType = mediaToPlay!!.mediaType,
            onDismiss = { mediaToPlay = null }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TÍTULO
            item {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // FECHA Y TIPO
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isTask) Icons.Default.TaskAlt else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isTask) "Tarea" else "Nota",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (uiState.isTask && uiState.taskDueDate != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormatter.format(Date(uiState.taskDueDate!!)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ESTADO (Solo si es tarea)
            if (uiState.isTask) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = uiState.isCompleted, onCheckedChange = null, enabled = false)
                            Text(
                                text = if (uiState.isCompleted) "Completada" else "Pendiente",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // DESCRIPCIÓN
            item {
                Text(
                    text = uiState.description.ifBlank { "Sin descripción" },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // MULTIMEDIA
            if (uiState.mediaFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Archivos Adjuntos:", fontWeight = FontWeight.Bold)
                }

                items(uiState.mediaFiles) { media ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // LÓGICA DE APERTURA OPTIMIZADA
                                if (media.mediaType == "IMAGE") {
                                    // Las imágenes siguen abriéndose con la galería (externo)
                                    openDetailMediaFile(context, media)
                                } else {
                                    // Audio y Video se abren INSTANTÁNEAMENTE en la app
                                    mediaToPlay = media
                                }
                            },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (media.mediaType) {
                                    "VIDEO" -> Icons.Default.Videocam
                                    "AUDIO" -> Icons.Default.Mic
                                    else -> Icons.Default.Image
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = media.description.ifBlank { "Archivo Multimedia" },
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (media.mediaType == "IMAGE") "Toca para ver en Galería" else "Toca para reproducir aquí",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --------------------------------------------------------------------------------
// 3. ADAPTADOR LÓGICO PARA TABLETS/DETAIL VIEW
// --------------------------------------------------------------------------------
@Composable
fun NoteDetailContent(
    noteId: Long,
    modifier: Modifier = Modifier,
    onEditClick: (Long) -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: AddEditNoteViewModel = viewModel(
        key = "detail_$noteId",
        factory = AddEditViewModelFactory(
            noteRepository = AppDataContainer.noteRepository,
            context = context,
            editId = noteId
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.saveSuccessful) {
        LaunchedEffect(Unit) {
            onDeleteConfirmed()
        }
    }

    NoteDetailContent(
        uiState = uiState,
        onBack = { /* En modo tablet no hay back action aquí */ },
        onEdit = { onEditClick(noteId) },
        modifier = modifier
    )
}

// --- FUNCIÓN AUXILIAR PARA ABRIR IMÁGENES (Legacy/Externo) ---
private fun openDetailMediaFile(context: Context, mediaItem: MediaItem) {
    try {
        val uriString = mediaItem.uri
        val uri: Uri

        if (uriString.startsWith("content://")) {
            uri = Uri.parse(uriString)
        } else {
            val file = File(uriString)
            uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                file
            )
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            val mimeType = "image/*" // Solo manejamos imágenes por aquí ahora
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Manejo de errores silencioso o log
        e.printStackTrace()
    }
}