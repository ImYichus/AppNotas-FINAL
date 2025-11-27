package com.jqlqapa.appnotas.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jqlqapa.appnotas.data.model.MediaEntity
import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModel
import com.jqlqapa.appnotas.utils.FILE_PROVIDER_AUTHORITY // Usamos la constante segura
import java.io.File

// Función auxiliar local
fun createImageFile(context: Context): File {
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", storageDir)
}

@Composable
fun CameraCaptureScreen(viewModel: HomeViewModel, noteId: Long? = null) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // 1. Launcher para el resultado de la CÁMARA (Tomar la foto)
    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                viewModel.addMedia(
                    MediaEntity(
                        noteId = noteId ?: 0,
                        filePath = photoUri.toString(),
                        mediaType = "IMAGE",
                        description = "Foto tomada con cámara"
                    )
                )
                Toast.makeText(context, "Foto guardada", Toast.LENGTH_SHORT).show()
            }
        }

    // 2. Launcher para pedir el PERMISO de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Si el usuario acepta, procedemos a crear el archivo y lanzar la cámara
            val file = createImageFile(context)
            photoUri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY, // Usamos la constante corregida
                file
            )
            photoUri?.let { uri -> takePictureLauncher.launch(uri) }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // 3. Verificación antes de lanzar la acción
            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                // YA TIENE PERMISO: Lanza directo
                val file = createImageFile(context)
                photoUri = FileProvider.getUriForFile(
                    context,
                    FILE_PROVIDER_AUTHORITY, // Usamos la constante corregida
                    file
                )
                photoUri?.let { uri -> takePictureLauncher.launch(uri) }
            } else {
                // NO TIENE PERMISO: Solicítalo
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }) {
            Text("Tomar Foto")
        }
    }
}