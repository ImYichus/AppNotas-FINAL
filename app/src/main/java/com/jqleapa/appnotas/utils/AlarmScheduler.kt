package com.jqlqapa.appnotas.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.jqlqapa.appnotas.data.model.ReminderEntity
import com.jqlqapa.appnotas.receivers.AlarmasReceiver
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity, message: String) {
        val intent = Intent(context, AlarmasReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Debug visual
        Toast.makeText(context, "Alarma programada para las ${Date(reminder.reminderDateTime)}", Toast.LENGTH_SHORT).show()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+)
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.reminderDateTime,
                        pendingIntent
                    )
                } else {
                    // Si no tenemos permiso de exactitud, usamos alarma estándar (puede variar unos segundos)
                    // Esto evita el crash y garantiza que la notificación llegue.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.reminderDateTime,
                        pendingIntent
                    )
                    Toast.makeText(context, "Aviso: Permiso de alarma exacta no concedido. Puede haber ligero retraso.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Android 11 o inferior
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.reminderDateTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(context, "Error de seguridad: No se pudo programar la alarma", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error desconocido al programar", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancel(reminder: ReminderEntity) {
        try {
            val intent = Intent(context, AlarmasReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // Ignorar si no existía
        }
    }
}