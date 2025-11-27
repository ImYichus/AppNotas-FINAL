package com.jqlqapa.appnotas

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jqlqapa.appnotas.data.AppDataContainer

class AppNotasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppDataContainer.initialize(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANTE: Este ID "CHANNEL_ID_NOTAS" debe ser idéntico en el Receiver
            val channelId = "CHANNEL_ID_NOTAS"
            val channelName = "Recordatorios de Notas"
            val descriptionText = "Notificaciones para tareas programadas"
            val importance = NotificationManager.IMPORTANCE_HIGH // ALTA para que suene

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}