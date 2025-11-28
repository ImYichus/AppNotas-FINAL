package com.jqlqapa.appnotas.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
// import com.jqlqapa.appnotas.R // Comentado temporalmente para usar icono de sistema seguro

class AlarmasReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. PRUEBA DE FUEGO: Si ves este Toast, el sistema de alarmas FUNCIONA.
        Toast.makeText(context, "¡ALARMA RECIBIDA!", Toast.LENGTH_LONG).show()

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tienes una tarea pendiente"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Usamos un icono del sistema (android.R.drawable...) para descartar errores de recursos propios
        val notification = NotificationCompat.Builder(context, "CHANNEL_ID_NOTAS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de Tarea")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido y vibración por defecto
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al mostrar notificación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}