package com.jqlqapa.appnotas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// IMPORTANTE: Importamos la lógica desde la carpeta 'utils'
// Si estas líneas se ponen en rojo, asegúrate de que el paquete en utils sea 'com.jqlqapa.appnotas.utils'
import com.jqlqapa.appnotas.utils.AudioRecorder
import com.jqlqapa.appnotas.utils.createMediaFile
import java.io.File

@Composable
fun AudioRecorderDialog(
    onDismiss: () -> Unit,
    onSave: (File) -> Unit,
    audioRecorder: AudioRecorder // Usa la interfaz importada de utils
) {
    var isRecording by remember { mutableStateOf(false) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grabar Audio") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (isRecording) {
                    Text("Grabando...", color = MaterialTheme.colorScheme.error)
                    LinearProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text(if (outputFile == null) "Presiona Iniciar para grabar" else "Audio grabado listo para guardar")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isRecording) {
                        // Detener y Guardar
                        audioRecorder.stop()
                        isRecording = false
                        outputFile?.let { onSave(it) }
                        // No cerramos el diálogo aquí automáticamente para asegurar que se guarde,
                        // la lógica de onSave en la pantalla padre debería cerrarlo.
                        // Pero por seguridad visual:
                        // onDismiss() <- Opcional, dependiendo de tu flujo
                    } else {
                        // Iniciar
                        val file = createMediaFile(context, "AUDIO")
                        outputFile = file
                        audioRecorder.start(file)
                        isRecording = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "Detener y Guardar" else "Iniciar Grabación")
            }
        },
        dismissButton = {
            if (!isRecording) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}