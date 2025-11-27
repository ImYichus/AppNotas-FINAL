package com.jqlqapa.appnotas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Requisitos: título, descripción, si es tarea (isTask), fecha de cumplimiento (taskDueDate)
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val registrationDate: Long = System.currentTimeMillis(), // Requisito: Ordenar por fecha de registro
    val isTask: Boolean, // Requisito: Clasificada como nota o tarea
    val taskDueDate: Long? = null, // Requisito: Fecha y hora en que se debe cumplir la tarea
    val isCompleted: Boolean = false // Requisito: Marcar como cumplida
    // NOTA: 'taskDueDate' debe ser un Long (timestamp) para Room
)