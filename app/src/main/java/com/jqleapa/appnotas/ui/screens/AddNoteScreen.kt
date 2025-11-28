package com.jqlqapa.appnotas.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.ui.components.AudioRecorderDialog
import com.jqlqapa.appnotas.ui.components.MediaPlayerDialog
import com.jqlqapa.appnotas.ui.viewmodel.AddEditNoteViewModel
import com.jqlqapa.appnotas.ui.viewmodel.MediaItem
import com.jqlqapa.appnotas.utils.AndroidAudioRecorder
import com.jqlqapa.appnotas.utils.FILE_PROVIDER_AUTHORITY
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(navController: NavHostController, factory: AddEditViewModelFactory) {
    val viewModel: AddEditNoteViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Estados UI
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }
    var mediaToPlay by remember { mutableStateOf<MediaItem?>(null) }

    // --- NUEVO: Estado para el diálogo de "Ir a Ajustes" ---
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    var tempDateMillis by remember { mutableStateOf<Long?>(null) }
    var currentMediaUri by remember { mutableStateOf<Uri?>(null) }

    // --- LAUNCHERS DE PERMISOS INTELIGENTES ---

    // Launcher genérico que detecta si se rechazó el permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso concedido. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
        } else {
            // Si se rechaza (o ya se gastaron los 2 tokens), mostramos el botón de ir a ajustes
            showPermissionSettingsDialog = true
        }
    }

    // Launcher específico para audio (usa la misma lógica)
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showAudioRecorderDialog = true
        else showPermissionSettingsDialog = true
    }

    // --- LAUNCHERS DE CONTENIDO ---
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

    // Permiso Notificaciones (Android 13+)
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- LÓGICA DE DIÁLOGOS ---

    // 1. Diálogo de Permisos Rechazados (Ir a Ajustes)
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permiso Necesario") },
            text = { Text("Para usar esta función (Cámara/Microfono), necesitas activar los permisos manualmente en los Ajustes de la aplicación.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionSettingsDialog = false
                    // INTENT MÁGICO: Manda al usuario directo a la pantalla de permisos de TU app
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Ir a Ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // 2. Reproductor Interno
    if (mediaToPlay != null) {
        MediaPlayerDialog(uri = mediaToPlay!!.uri, mediaType = mediaToPlay!!.mediaType, onDismiss = { mediaToPlay = null })
    }

    // 3. Date/Time Pickers
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
                        viewModel.updateTaskDueDate(calendar.timeInMillis)
                    }
                    showTimePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Selecciona la hora"); TimePicker(state = timePickerState) } }
        )
    }

    if (uiState.saveSuccessful) {
        LaunchedEffect(Unit) { viewModel.saveComplete(); navController.popBackStack() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Crear Nota/Tarea", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!uiState.isSaving && uiState.title.isNotBlank()) viewModel.saveNote() }, containerColor = MaterialTheme.colorScheme.secondary) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Guardar", modifier = Modifier.padding(16.dp))
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !uiState.isTask, onClick = { viewModel.updateIsTask(false) }); Text("Nota")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = uiState.isTask, onClick = { viewModel.updateIsTask(true) }); Text("Tarea")
                }
            }

            item { OutlinedTextField(value = uiState.title, onValueChange = viewModel::updateTitle, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = uiState.description, onValueChange = viewModel::updateDescription, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp)) }

            if (uiState.isTask) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Configuración de Tarea", fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Event, null); Spacer(modifier = Modifier.width(8.dp)); Text(uiState.taskDueDate?.let { dateFormatter.format(Date(it)) } ?: "Seleccionar Fecha y Hora") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = uiState.isCompleted, onCheckedChange = viewModel::toggleCompletion); Text("Marcar como completada") }
                        }
                    }
                }
            }

            // BOTONES MULTIMEDIA (CON LÓGICA DE PERMISOS ACTUALIZADA)
            item {
                Text("Adjuntar Multimedia:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {

                    // FOTO
                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val file = createTempMediaFile(context, "IMAGE")
                            currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                            takePictureLauncher.launch(currentMediaUri!!)
                        } else {
                            // Si no tiene permiso, lo pide. Si ya fue denegado 2 veces, el launcher abrirá el diálogo
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) { Icon(Icons.Default.PhotoCamera, "Foto") }

                    // VIDEO
                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val file = createTempMediaFile(context, "VIDEO")
                            currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                            captureVideoLauncher.launch(currentMediaUri!!)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) { Icon(Icons.Default.Videocam, "Video") }

                    // AUDIO
                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            showAudioRecorderDialog = true
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) { Icon(Icons.Default.Mic, "Audio") }

                    // GALERÍA (No suele necesitar "Ir a ajustes" en versiones nuevas, pero usa el launcher estándar)
                    SmallFloatingActionButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Icon(Icons.Default.Image, "Galería") }
                }
            }

            if (uiState.mediaFiles.isNotEmpty()) {
                items(uiState.mediaFiles) { media ->
                    AttachmentItemComponent(media, { viewModel.deleteMedia(media) }, { if (media.mediaType == "IMAGE") openMediaFile(context, media) else mediaToPlay = media })
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
    val dir = if (type == "IMAGE") Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
    val suffix = if (type == "IMAGE") ".jpg" else ".mp4"
    return File.createTempFile("${if(type=="IMAGE")"IMG_" else "VID_"}${timeStamp}_", suffix, context.getExternalFilesDir(dir))
}

private fun openMediaFile(context: Context, mediaItem: MediaItem) {
    try {
        val uri = if (mediaItem.uri.startsWith("content://")) Uri.parse(mediaItem.uri) else FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, File(mediaItem.uri))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) { Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
}

@Composable
private fun AttachmentItemComponent(mediaItem: MediaItem, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(4.dp), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(when(mediaItem.mediaType) { "VIDEO" -> Icons.Default.Videocam; "AUDIO" -> Icons.Default.Mic; else -> Icons.Default.Image }, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(mediaItem.description.ifBlank { mediaItem.mediaType }, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}