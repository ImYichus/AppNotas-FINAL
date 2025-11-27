package com.jqlqapa.appnotas.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// CORRECCIÓN: Debe coincidir con 'authorities' en tu AndroidManifest.xml
const val FILE_PROVIDER_AUTHORITY = "com.jqlqapa.appnotas.provider"

fun createMediaFile(context: Context, type: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    // Usamos Triple para devolver prefijo, sufijo y directorio
    val (prefix, suffix, directory) = when (type) {
        "IMAGE" -> Triple("IMG_", ".jpg", Environment.DIRECTORY_PICTURES)
        "VIDEO" -> Triple("VID_", ".mp4", Environment.DIRECTORY_MOVIES)
        "AUDIO" -> Triple("AUD_", ".mp4", Environment.DIRECTORY_MUSIC)
        else -> Triple("FILE_", ".dat", Environment.DIRECTORY_DOWNLOADS)
    }

    val storageDir = context.getExternalFilesDir(directory)
    return File.createTempFile("${prefix}${timeStamp}_", suffix, storageDir)
}

fun getMediaUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
}