package com.jqleapa.appnotas.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment // <--- ESTE IMPORT FALTABA
import android.widget.Toast
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.ui.components.AudioRecorderDialog
import com.jqlqapa.appnotas.ui.viewmodel.AddEditNoteViewModel
import com.jqlqapa.appnotas.ui.viewmodel.MediaItem
import com.jqlqapa.appnotas.utils.AndroidAudioRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    navController: NavHostController,
    factory: AddEditViewModelFactory
) {
    val viewModel: AddEditNoteViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Estados
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }

    var tempDateMillis by remember { mutableStateOf<Long?>(null) }
    var currentMediaUri by remember { mutableStateOf<Uri?>(null) }

    // --- PERMISO NOTIFICACIONES (Android 13+) ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- LAUNCHERS MULTIMEDIA ---
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
            viewModel.addMediaItem(
                MediaItem(
                    uri = uri.toString(),
                    mediaType = mediaType,
                    description = "Galería"
                )
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) showAudioRecorderDialog = true }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { granted -> granted }) galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    // --- DATE & TIME PICKERS ---
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
            confirmButton = {
                TextButton(onClick = {
                    if (tempDateMillis != null) {
                        val calendar = Calendar.getInstance()
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalendar.timeInMillis = tempDateMillis!!
                        calendar.set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH), utcCalendar.get(Calendar.DAY_OF_MONTH), timePickerState.hour, timePickerState.minute, 0)

                        val tenMinFromNow = System.currentTimeMillis() + (10 * 60 * 1000)
                        if (calendar.timeInMillis < tenMinFromNow) {
                            Toast.makeText(context, "Selecciona una hora al menos 11 minutos en el futuro", Toast.LENGTH_LONG).show()
                        }
                        viewModel.updateTaskDueDate(calendar.timeInMillis)
                    }
                    showTimePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Selecciona la hora", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp)); TimePicker(state = timePickerState) } }
        )
    }

    if (uiState.saveSuccessful) {
        LaunchedEffect(Unit) { viewModel.saveComplete(); navController.popBackStack() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Crear Nota/Tarea", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!uiState.isSaving && uiState.title.isNotBlank()) viewModel.saveNote() },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Guardar", modifier = Modifier.padding(16.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !uiState.isTask, onClick = { viewModel.updateIsTask(false) })
                    Text("Nota")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = uiState.isTask, onClick = { viewModel.updateIsTask(true) })
                    Text("Tarea")
                }
            }

            item { OutlinedTextField(value = uiState.title, onValueChange = viewModel::updateTitle, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = uiState.description, onValueChange = viewModel::updateDescription, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp)) }

            if (uiState.isTask) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Configuración de Tarea", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Event, null); Spacer(modifier = Modifier.width(8.dp))
                                Text(text = uiState.taskDueDate?.let { dateFormatter.format(Date(it)) } ?: "Seleccionar Fecha y Hora")
                            }

                            if (uiState.taskDueDate != null) {
                                Text("🔔 Se notificará 10 min antes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = uiState.isCompleted, onCheckedChange = viewModel::toggleCompletion); Text("Marcar como completada") }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Adjuntar Multimedia:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SmallFloatingActionButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val file = createTempMediaFile(context, "IMAGE"); currentMediaUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); takePictureLauncher.launch(currentMediaUri!!) } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.PhotoCamera, "Foto") }
                    SmallFloatingActionButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val file = createTempMediaFile(context, "VIDEO"); currentMediaUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); captureVideoLauncher.launch(currentMediaUri!!) } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.Videocam, "Video") }
                    SmallFloatingActionButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) showAudioRecorderDialog = true else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Icon(Icons.Default.Mic, "Audio") }
                    SmallFloatingActionButton(onClick = { val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE); if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) else galleryPermissionLauncher.launch(perms) }) { Icon(Icons.Default.Image, "Galería") }
                }
            }

            if (uiState.mediaFiles.isNotEmpty()) {
                item { Text("Archivos Agregados:", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                items(uiState.mediaFiles) { media ->
                    AttachmentItemComponent(mediaItem = media, onDelete = { viewModel.deleteMedia(media) }, onClick = { openMediaFile(context, media) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAudioRecorderDialog) {
        val recorder = remember { AndroidAudioRecorder(context) }
        AudioRecorderDialog(onDismiss = { showAudioRecorderDialog = false }, onSave = { f -> viewModel.addMediaItem(MediaItem(uri = f.absolutePath, mediaType = "AUDIO", description = "Audio")); showAudioRecorderDialog = false }, audioRecorder = recorder)
    }
}

// --- FUNCIONES AUXILIARES ---

private fun createTempMediaFile(context: Context, type: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    // Corrección: Usamos if/else simple en lugar de Triple para evitar error de compilación
    val directory = if (type == "IMAGE") Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
    val prefix = if (type == "IMAGE") "IMG_" else "VID_"
    val suffix = if (type == "IMAGE") ".jpg" else ".mp4"

    return File.createTempFile("${prefix}${timeStamp}_", suffix, context.getExternalFilesDir(directory))
}

private fun openMediaFile(context: Context, mediaItem: MediaItem) {
    try {
        val uriString = mediaItem.uri
        val uri: Uri = if (uriString.startsWith("content://")) Uri.parse(uriString) else FileProvider.getUriForFile(context, "${context.packageName}.provider", File(uriString))
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            // Corrección: Usamos mediaItem en lugar de 'media'
            val mimeType = when (mediaItem.mediaType) {
                "IMAGE" -> "image/*"
                "VIDEO" -> "video/*"
                "AUDIO" -> "audio/*"
                else -> "*/*"
            }
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al abrir: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AttachmentItemComponent(mediaItem: MediaItem, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = when(mediaItem.mediaType) { "VIDEO" -> Icons.Default.Videocam; "AUDIO" -> Icons.Default.Mic; else -> Icons.Default.Image }, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mediaItem.description.ifBlank { mediaItem.mediaType }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = "Toca para ver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}