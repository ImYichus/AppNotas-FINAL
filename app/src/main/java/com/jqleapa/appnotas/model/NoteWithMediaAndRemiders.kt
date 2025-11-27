package com.jqlqapa.appnotas.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithMediaAndReminders(
    @Embedded val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val media: List<MediaEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val reminders: List<ReminderEntity>
)