package com.jqlqapa.appnotas.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqlqapa.appnotas.ui.viewmodel.AddEditNoteViewModel
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.ui.viewmodel.MediaItem
import com.jqlqapa.appnotas.ui.components.AudioRecorderDialog
import com.jqlqapa.appnotas.ui.components.MediaPlayerDialog // <--- IMPORTANTE
import com.jqlqapa.appnotas.utils.AndroidAudioRecorder // <--- IMPORTANTE
import com.jqlqapa.appnotas.utils.FILE_PROVIDER_AUTHORITY
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    navController: NavHostController,
    factory: AddEditViewModelFactory,
    noteId: Long
) {
    val viewModel: AddEditNoteViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Estados
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Estado para reproducción [NUEVO]
    var mediaToPlay by remember { mutableStateOf<MediaItem?>(null) }

    var tempDateMillis by remember { mutableStateOf<Long?>(null) }
    var currentMediaUri by remember { mutableStateOf<Uri?>(null) }

    // --- LAUNCHERS ---
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentMediaUri != null) viewModel.addMediaItem(MediaItem(uri = currentMediaUri.toString(), mediaType = "IMAGE", description = "Foto"))
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && currentMediaUri != null) viewModel.addMediaItem(MediaItem(uri = currentMediaUri.toString(), mediaType = "VIDEO", description = "Video"))
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val flag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            val type = context.contentResolver.getType(uri) ?: "image/jpeg"
            val mediaType = if (type.startsWith("video")) "VIDEO" else "IMAGE"
            viewModel.addMediaItem(MediaItem(uri = uri.toString(), mediaType = mediaType, description = "Galería"))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) showAudioRecorderDialog = true }

    // --- DATE PICKERS ---
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { tempDateMillis = it; showDatePicker = false; showTimePicker = true } }) { Text("Siguiente") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { if (tempDateMillis != null) { val calendar = Calendar.getInstance(); val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")); utcCalendar.timeInMillis = tempDateMillis!!; calendar.set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH), utcCalendar.get(Calendar.DAY_OF_MONTH), timePickerState.hour, timePickerState.minute, 0); viewModel.updateTaskDueDate(calendar.timeInMillis) }; showTimePicker = false }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Hora", style = MaterialTheme.typography.titleMedium); TimePicker(state = timePickerState) } }
        )
    }

    // --- REPRODUCTOR INTERNO ---
    if (mediaToPlay != null) {
        MediaPlayerDialog(
            uri = mediaToPlay!!.uri,
            mediaType = mediaToPlay!!.mediaType,
            onDismiss = { mediaToPlay = null }
        )
    }

    // --- DIÁLOGO ELIMINAR ---
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar Nota") },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }

    if (uiState.saveSuccessful) {
        LaunchedEffect(Unit) { viewModel.saveComplete(); navController.popBackStack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = { IconButton(onClick = { showDeleteConfirmation = true }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.onPrimary) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!uiState.isSaving && uiState.title.isNotBlank()) viewModel.saveNote() }, containerColor = MaterialTheme.colorScheme.secondary) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Actualizar", modifier = Modifier.padding(16.dp))
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !uiState.isTask, onClick = { viewModel.updateIsTask(false) }); Text("Nota")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = uiState.isTask, onClick = { viewModel.updateIsTask(true) }); Text("Tarea")
                }
            }
            item { OutlinedTextField(value = uiState.title, onValueChange = viewModel::updateTitle, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = uiState.description, onValueChange = viewModel::updateDescription, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()) }

            if (uiState.isTask) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Configuración de Tarea", fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Event, null); Spacer(modifier = Modifier.width(8.dp)); Text(uiState.taskDueDate?.let { dateFormatter.format(Date(it)) } ?: "Seleccionar Fecha y Hora") }
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = uiState.isCompleted, onCheckedChange = viewModel::toggleCompletion); Text("Completada") }
                        }
                    }
                }
            }

            // Botones Multimedia
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SmallFloatingActionButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val file = createTempMediaFile(context, "IMAGE"); currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file); takePictureLauncher.launch(currentMediaUri!!) } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.PhotoCamera, "Foto") }
                    SmallFloatingActionButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val file = createTempMediaFile(context, "VIDEO"); currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file); captureVideoLauncher.launch(currentMediaUri!!) } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.Videocam, "Video") }

                    // AUDIO REPARADO
                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) showAudioRecorderDialog = true
                        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Icon(Icons.Default.Mic, "Audio") }

                    SmallFloatingActionButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Icon(Icons.Default.Image, "Galería") }
                }
            }

            if (uiState.mediaFiles.isNotEmpty()) {
                items(uiState.mediaFiles) { media ->
                    AttachmentItemComponent(
                        mediaItem = media,
                        onDelete = { viewModel.deleteMedia(media) },
                        onClick = {
                            // Lógica inteligente: Imagen -> Galería, Audio/Video -> Reproductor Interno
                            if (media.mediaType == "IMAGE") {
                                openMediaFile(context, media)
                            } else {
                                mediaToPlay = media
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    if (showAudioRecorderDialog) {
        // AQUÍ ESTÁ EL CAMBIO CRÍTICO: Usamos el grabador real, no el dummy
        val recorder = remember { AndroidAudioRecorder(context) }

        AudioRecorderDialog(
            onDismiss = { showAudioRecorderDialog = false },
            onSave = { f ->
                viewModel.addMediaItem(MediaItem(uri = f.absolutePath, mediaType = "AUDIO", description = "Audio grabado"))
                showAudioRecorderDialog = false
            },
            audioRecorder = recorder
        )
    }
}

// Funciones auxiliares privadas
private fun createTempMediaFile(context: Context, type: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val (prefix, suffix, dir) = if (type == "IMAGE") Triple("IMG_", ".jpg", Environment.DIRECTORY_PICTURES) else Triple("VID_", ".mp4", Environment.DIRECTORY_MOVIES)
    return File.createTempFile("${prefix}${timeStamp}_", suffix, context.getExternalFilesDir(dir))
}

private fun openMediaFile(context: Context, mediaItem: MediaItem) {
    try {
        val uri = if (mediaItem.uri.startsWith("content://")) Uri.parse(mediaItem.uri) else FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, File(mediaItem.uri))
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) { }
}

@Composable
private fun AttachmentItemComponent(mediaItem: MediaItem, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(4.dp), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(when(mediaItem.mediaType) { "VIDEO" -> Icons.Default.Videocam; "AUDIO" -> Icons.Default.Mic; else -> Icons.Default.Image }, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(mediaItem.description.ifBlank { mediaItem.mediaType }, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, "Borrar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}