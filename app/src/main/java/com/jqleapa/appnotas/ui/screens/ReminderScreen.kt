package com.jqleapa.appnotas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*

private val reminderDateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(navController: NavHostController) {

    var description by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf<Long?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var showDateTimePicker by remember { mutableStateOf(false) }

    val onSaveReminder: () -> Unit = {
        if (description.isBlank() || reminderDate == null) {
            // Mostrar error o Snackbar
            println("ERROR: Descripción y fecha/hora son obligatorios.")
        } else {
            isSaving = true
            println("Recordatorio Guardado: $description en ${reminderDateFormatter.format(Date(reminderDate!!))}")

            isSaving = false
            navController.popBackStack()
        }
    }

    if (showDateTimePicker) {
        SimulatedDateTimePickerDialog(
            onDismiss = { showDateTimePicker = false },
            onDateSelected = { time ->
                reminderDate = time
                showDateTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Programar Recordatorio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { showDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        reminderDate?.let { reminderDateFormatter.format(Date(it)) }
                            ?: "Elegir Fecha y Hora"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSaveReminder,
                    enabled = !isSaving && description.isNotBlank() && reminderDate != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Text("Guardar Recordatorio", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    )
}

@Composable
private fun SimulatedDateTimePickerDialog(onDismiss: () -> Unit, onDateSelected: (Long) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Fecha y Hora") },
        text = { Text("Simulación: La fecha se establecerá a mañana a las 10:00 AM.") },
        confirmButton = {
            Button(onClick = {
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 10)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                onDateSelected(tomorrow)
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}