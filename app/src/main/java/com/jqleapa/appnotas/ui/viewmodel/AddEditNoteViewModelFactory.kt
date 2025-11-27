package com.jqlqapa.appnotas.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jqlqapa.appnotas.data.NoteRepository

class AddEditViewModelFactory(
    private val noteRepository: NoteRepository,
    private val context: Context,
    private val editId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditNoteViewModel::class.java)) {
            val viewModel = AddEditNoteViewModel(noteRepository, context)
            if (editId != null && editId != 0L) viewModel.loadNote(editId)
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}