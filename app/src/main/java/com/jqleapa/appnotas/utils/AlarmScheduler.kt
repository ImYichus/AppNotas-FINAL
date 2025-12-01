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
        val intent = Intent(context, AlarmasReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            // [NUEVO] Pasamos el ID de la nota para poder abrirla luego
            putExtra("EXTRA_NOTE_ID", reminder.noteId)
            data = Uri.parse("content://reminders/${reminder.id}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.reminderDateTime, pendingIntent)
                    Log.d("AppNotasAlarm", "Alarma EXACTA: ID ${reminder.id}")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.reminderDateTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.reminderDateTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel(reminder: ReminderEntity) {
        try {
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
        } catch (e: Exception) { }
    }
}