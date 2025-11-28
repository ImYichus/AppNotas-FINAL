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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AppNotasBoot", "⚡ RECIBIDO: Evento de arranque del sistema.")

            // 1. Inicializar DB por seguridad
            try {
                AppDataContainer.initialize(context)
            } catch (e: Exception) {
                // Ya estaba inicializada
            }

            val pendingResult = goAsync()
            val scheduler = AlarmScheduler(context)
            val repository = AppDataContainer.noteRepository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("AppNotasBoot", "Buscando recordatorios pendientes...")

                    // Obtenemos todas las notas
                    val allNotes = repository.obtenerTodas().first()
                    val now = System.currentTimeMillis()
                    var count = 0

                    for (note in allNotes) {
                        // Solo procesamos tareas no completadas
                        if (note.isTask && !note.isCompleted) {

                            // Obtenemos los detalles (donde están los recordatorios)
                            val details = repository.getNoteDetails(note.id).first()

                            // --- AQUÍ ESTÁ LA CORRECCIÓN ---
                            // Iteramos sobre la lista REAL de recordatorios guardados
                            for (reminder in details.reminders) {

                                // Verificamos si la hora exacta del recordatorio es en el FUTURO
                                if (reminder.reminderDateTime > now) {

                                    scheduler.schedule(reminder, "Recordatorio: ${note.title}")
                                    Log.d("AppNotasBoot", " Reprogramada (ID=${reminder.id}): ${note.title}")
                                    count++

                                } else {
                                    Log.d("AppNotasBoot", "gnores (Vencida): ${note.title}")
                                }
                            }
                        }
                    }
                    Log.d("AppNotasBoot", "Fin. Total reprogramadas: $count")

                } catch (e: Exception) {
                    Log.e("AppNotasBoot", " Error: ${e.message}")
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}