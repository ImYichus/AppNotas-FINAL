package com.jqlqapa.appnotas.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.jqlqapa.appnotas.data.model.ReminderEntity
import com.jqlqapa.appnotas.receivers.AlarmasReceiver
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity, message: String) {
        // 1. HACER EL INTENT ÚNICO
        // Agregamos 'data' con el ID. Esto diferencia los Intents para Android.
        val intent = Intent(context, AlarmasReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            // ESTA LÍNEA ES LA MAGIA: Hace que cada Intent sea único
            data = Uri.parse("content://reminders/${reminder.id}")
        }

        // 2. CREAR EL PENDING INTENT
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(), // El RequestCode también debe ser único
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val date = Date(reminder.reminderDateTime)
        Log.d("AppNotasAlarm", "Intentando programar ID:${reminder.id} para: $date")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.reminderDateTime,
                        pendingIntent
                    )
                    Log.d("AppNotasAlarm", "Alarma EXACTA programada (ID: ${reminder.id})")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.reminderDateTime,
                        pendingIntent
                    )
                    Log.w("AppNotasAlarm", "Alarma INEXACTA programada (Falta permiso)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.reminderDateTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AppNotasAlarm", "ERROR SEGURIDAD: ${e.message}")
        } catch (e: Exception) {
            Log.e("AppNotasAlarm", "ERROR: ${e.message}")
        }
    }

    fun cancel(reminder: ReminderEntity) {
        try {
            // Para cancelar, el Intent debe ser IDÉNTICO al que creamos (incluyendo el data)
            val intent = Intent(context, AlarmasReceiver::class.java).apply {
                data = Uri.parse("content://reminders/${reminder.id}")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("AppNotasAlarm", "Alarma cancelada: ID ${reminder.id}")
        } catch (e: Exception) {
            Log.e("AppNotasAlarm", "Error cancelando: ${e.message}")
        }
    }
}