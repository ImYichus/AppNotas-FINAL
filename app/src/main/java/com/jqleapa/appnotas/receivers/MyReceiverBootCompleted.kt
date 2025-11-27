package com.jqlqapa.appnotas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MyReceiverBootCompleted : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 1. Inicializar dependencias si es necesario (AppDataContainer ya es object)
            val repository = AppDataContainer.noteRepository
            val scheduler = AlarmScheduler(context)

            // 2. Lanzar corrutina para leer BD
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Obtenemos TODAS las notas/tareas
                    val allNotes = repository.obtenerTodas().first()

                    // Filtramos las que son tareas futuras
                    val now = System.currentTimeMillis()

                    for (note in allNotes) {
                        if (note.isTask && !note.isCompleted) {
                            // Obtenemos los recordatorios de esa nota
                            val details = repository.getNoteDetails(note.id).first()
                            for (reminder in details.reminders) {
                                if (reminder.reminderDateTime > now) {
                                    // REPROGRAMAR
                                    scheduler.schedule(reminder, "Recordatorio: ${note.title}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}