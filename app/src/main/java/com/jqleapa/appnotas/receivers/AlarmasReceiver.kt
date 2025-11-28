package com.jqlqapa.appnotas.receivers

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AlarmasReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AppNotasAlarm", "🚨 RECEIVER ACTIVADO: El sistema despertó a la app.")

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tienes una tarea pendiente"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Verificar si el Canal de Notificaciones existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel("CHANNEL_ID_NOTAS")
            if (channel == null) {
                Log.e("AppNotasAlarm", "ERROR FATAL: El canal 'CHANNEL_ID_NOTAS' no existe. Revisa AppNotasApplication.")
                return
            } else {
                Log.d("AppNotasAlarm", "Canal encontrado. Importancia: ${channel.importance}")
                if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    Log.e("AppNotasAlarm", "El usuario tiene el canal SILENCIADO/BLOQUEADO en Ajustes.")
                    return
                }
            }
        }

        // 2. Verificar Permiso de Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("AppNotasAlarm", "PERMISO DENEGADO: No tienes permiso POST_NOTIFICATIONS.")
                return
            }
        }

        // 3. Construir y Lanzar
        val notification = NotificationCompat.Builder(context, "CHANNEL_ID_NOTAS")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono seguro del sistema
            .setContentTitle("Recordatorio AppNotas")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d("AppNotasAlarm", "NOTIFICACIÓN ENVIADA AL SISTEMA VISUALMENTE")
        } catch (e: Exception) {
            Log.e("AppNotasAlarm", "Excepción al notificar: ${e.message}")
        }
    }
}