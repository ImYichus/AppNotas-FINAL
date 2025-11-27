package com.jqlqapa.appnotas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Requisitos: Archivo (URI/Ruta), descripción, miniatura
@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long, // FK a NoteEntity
    val filePath: String, // URI o ruta del archivo
    val mediaType: String, // e.g., "IMAGE", "VIDEO", "AUDIO"
    val description: String?, // Requisito: descripción por archivo adjunto
    val thumbnailPath: String? = null // Requisito: miniatura que lo represente
)