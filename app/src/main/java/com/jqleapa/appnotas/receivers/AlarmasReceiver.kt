package com.jqlqapa.appnotas.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jqlqapa.appnotas.R

class AlarmasReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tienes una tarea pendiente"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Usamos el MISMO ID que definimos en la Application: "CHANNEL_ID_NOTAS"
        val notification = NotificationCompat.Builder(context, "CHANNEL_ID_NOTAS")
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de que este icono existe
            .setContentTitle("Recordatorio de Tarea")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}