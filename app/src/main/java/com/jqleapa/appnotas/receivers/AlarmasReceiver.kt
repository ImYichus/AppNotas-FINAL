package com.jqlqapa.appnotas.receivers

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jqlqapa.appnotas.MainActivity // Asegúrate de importar tu MainActivity

class AlarmasReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AppNotasAlarm", "🚨 RECEIVER ACTIVADO")

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tarea pendiente"
        // [NUEVO] Recuperamos el ID de la nota
        val noteId = intent.getLongExtra("EXTRA_NOTE_ID", -1L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // --- CREAR ACCIÓN AL TOCAR (TAP) ---
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            // Estos flags limpian la pila para abrir la nota fresca y evitar errores de navegación
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("nav_to_note_id", noteId) // Pasamos el ID a la Activity
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(), // RequestCode único
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // -----------------------------------

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel("CHANNEL_ID_NOTAS") == null) return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        }

        val notification = NotificationCompat.Builder(context, "CHANNEL_ID_NOTAS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio AppNotas")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // <--- AQUÍ VINCULAMOS EL TAP
            .setAutoCancel(true) // Se borra al tocarla
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}