package com.jqlqapa.appnotas.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
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
import com.jqlqapa.appnotas.ui.viewmodel.AddEditNoteViewModel
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.ui.viewmodel.MediaItem
import com.jqlqapa.appnotas.ui.viewmodel.ReminderItem
import com.jqlqapa.appnotas.ui.components.AudioRecorderDialog
import com.jqlqapa.appnotas.ui.components.MediaPlayerDialog
import com.jqlqapa.appnotas.utils.AndroidAudioRecorder
import com.jqlqapa.appnotas.utils.FILE_PROVIDER_AUTHORITY
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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

    // --- ESTADOS UI ---
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Gestión de Recordatorios
    var showReminderOptionsDialog by remember { mutableStateOf<ReminderItem?>(null) }
    var isEditingReminder by remember { mutableStateOf(false) }
    var reminderBeingEdited by remember { mutableStateOf<ReminderItem?>(null) }

    // Multimedia, Permisos y Eliminación
    var showAudioRecorderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var mediaToPlay by remember { mutableStateOf<MediaItem?>(null) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    var tempDateMillis by remember { mutableStateOf<Long?>(null) }
    var currentMediaUri by remember { mutableStateOf<Uri?>(null) }

    // --- LAUNCHERS ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) showPermissionSettingsDialog = true
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showAudioRecorderDialog = true
        else showPermissionSettingsDialog = true
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentMediaUri != null) viewModel.addMediaItem(MediaItem(uri = currentMediaUri.toString(), mediaType = "IMAGE", description = "Foto"))
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && currentMediaUri != null) viewModel.addMediaItem(MediaItem(uri = currentMediaUri.toString(), mediaType = "VIDEO", description = "Video"))
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            val type = context.contentResolver.getType(uri) ?: "image/jpeg"
            val mediaType = if (type.startsWith("video")) "VIDEO" else "IMAGE"
            viewModel.addMediaItem(MediaItem(uri = uri.toString(), mediaType = mediaType, description = "Galería"))
        }
    }

    // --- DIÁLOGOS DE FECHA Y HORA ---
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

                        if (isEditingReminder) {
                            reminderBeingEdited?.let { viewModel.updateReminder(it, calendar.timeInMillis) }
                        } else {
                            viewModel.addReminder(calendar.timeInMillis)
                        }
                    }
                    showTimePicker = false
                    isEditingReminder = false
                    reminderBeingEdited = null
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Selecciona la hora"); TimePicker(state = timePickerState) } }
        )
    }

    // Opciones de Recordatorio
    if (showReminderOptionsDialog != null) {
        AlertDialog(
            onDismissRequest = { showReminderOptionsDialog = null },
            title = { Text("Opciones") },
            text = { Text("¿Qué deseas hacer con esta alarma?") },
            confirmButton = {
                TextButton(onClick = {
                    isEditingReminder = true
                    reminderBeingEdited = showReminderOptionsDialog
                    showReminderOptionsDialog = null
                    showDatePicker = true
                }) { Text("Editar Hora") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReminderOptionsDialog?.let { viewModel.removeReminder(it) }
                    showReminderOptionsDialog = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    // Diálogo de Permisos
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permiso Necesario") },
            text = { Text("Ve a Ajustes para activar los permisos manualmente.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Ir a Ajustes") }
            },
            dismissButton = { TextButton(onClick = { showPermissionSettingsDialog = false }) { Text("Cancelar") } }
        )
    }

    // Reproductor Interno
    if (mediaToPlay != null) MediaPlayerDialog(uri = mediaToPlay!!.uri, mediaType = mediaToPlay!!.mediaType, onDismiss = { mediaToPlay = null })

    // Diálogo Eliminar Nota
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar Nota") },
            text = { Text("¿Estás seguro? No se puede deshacer.") },
            confirmButton = { Button(onClick = { viewModel.deleteNote(); showDeleteConfirmation = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }

    if (uiState.saveSuccessful) {
        LaunchedEffect(Unit) { viewModel.saveComplete(); navController.popBackStack() }
    }

    // --- UI PRINCIPAL ---
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
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:", fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(16.dp))
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

                            // SECCIÓN ALARMAS SIMPLIFICADA
                            Text("Recordatorios", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.reminders.isEmpty()) {
                                Text("Sin alarmas configuradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                uiState.reminders.forEach { reminder ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { showReminderOptionsDialog = reminder }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(dateFormatter.format(Date(reminder.timeInMillis)), modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            // Botón Agregar
                            OutlinedButton(
                                onClick = {
                                    isEditingReminder = false
                                    reminderBeingEdited = null
                                    showDatePicker = true
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                Icon(Icons.Default.AddAlarm, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Agregar Alarma")
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = uiState.isCompleted, onCheckedChange = viewModel::toggleCompletion); Text("Marcar como completada") }
                        }
                    }
                }
            }

            item {
                Text("Adjuntar Multimedia:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {

                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val file = createTempMediaFile(context, "IMAGE")
                            currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                            takePictureLauncher.launch(currentMediaUri!!)
                        } else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Icon(Icons.Default.PhotoCamera, "Foto") }

                    SmallFloatingActionButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val file = createTempMediaFile(context, "VIDEO")
                            currentMediaUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                            captureVideoLauncher.launch(currentMediaUri!!)
                        } else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Icon(Icons.Default.Videocam, "Video") }

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
                        onClick = { if (media.mediaType == "IMAGE") openMediaFile(context, media) else mediaToPlay = media }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAudioRecorderDialog) {
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

// --- UTILIDADES ---
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
    } catch (e: Exception) { }
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