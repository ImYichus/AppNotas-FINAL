package com.jqlqapa.appnotas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyReceiverBootCompleted : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Filtramos para que solo actúe si es un reinicio
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AppNotasBoot", "⚡ RECIBIDO: Evento de arranque del sistema.")

            // 1. OBLIGATORIO: Inicializar la base de datos manualmente por seguridad
            // (A veces el Application no ha terminado de cargar al reiniciar)
            try {
                AppDataContainer.initialize(context)
                Log.d("AppNotasBoot", "Contenedor de datos inicializado.")
            } catch (e: Exception) {
                Log.e("AppNotasBoot", "Error inicializando datos: ${e.message}")
            }

            // 2. Pedimos tiempo extra al sistema (goAsync)
            val pendingResult = goAsync()
            val scheduler = AlarmScheduler(context)
            val repository = AppDataContainer.noteRepository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("AppNotasBoot", "🔄 Leyendo base de datos...")
                    val allNotes = repository.obtenerTodas().first()
                    val now = System.currentTimeMillis()
                    var count = 0

                    for (note in allNotes) {
                        if (note.isTask && !note.isCompleted) {
                            // Obtenemos detalles para ver fecha de recordatorio original
                            // (Nota: Esto podría optimizarse, pero para fines prácticos funciona)
                            val details = repository.getNoteDetails(note.id).first()

                            // Re-agendamos solo si la fecha es futura
                            if (note.taskDueDate != null && note.taskDueDate > now) {
                                // Calculamos 10 min antes (o usamos la lógica de tus recordatorios guardados)
                                // Aquí usaremos la lógica simple de reprogramar basado en la nota
                                val reminderTime = note.taskDueDate - 600000 // 10 min antes

                                if (reminderTime > now) {
                                    // Creamos un objeto recordatorio temporal para el scheduler
                                    // Ojo: Lo ideal es usar el ReminderEntity real de la BD si lo tienes
                                    val reminderEntity = details.reminders.firstOrNull()

                                    if (reminderEntity != null) {
                                        scheduler.schedule(reminderEntity, "Recordatorio: ${note.title}")
                                        Log.d("AppNotasBoot", "Reprogramada: ${note.title}")
                                        count++
                                    }
                                }
                            }
                        }
                    }
                    Log.d("AppNotasBoot", "Fin. Total reprogramadas: $count")

                } catch (e: Exception) {
                    Log.e("AppNotasBoot", "FALLO CRÍTICO: ${e.message}")
                    e.printStackTrace()
                } finally {
                    // 3. Liberamos el proceso
                    pendingResult.finish()
                }
            }
        }
    }
}