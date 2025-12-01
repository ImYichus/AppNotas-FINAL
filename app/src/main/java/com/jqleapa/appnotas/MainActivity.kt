package com.jqlqapa.appnotas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jqleapa.appnotas.ui.theme.AppNotasTheme
import com.jqlqapa.appnotas.data.AppDataContainer
import com.jqlqapa.appnotas.ui.navigation.AppNavigation
// Si AppNotasTheme está en otro lugar, ajusta el path. El path común es ui.theme.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppDataContainer.initialize(this)

        val noteIdFromNotif = intent.getLongExtra("nav_to_note_id", -1L)

        setContent {
            AppNotasTheme { // El error 'theme' es ahora 'AppNotasTheme'
                val repository = AppDataContainer.noteRepository

                AppNavigation(
                    noteRepository = repository,
                    startDestinationId = if (noteIdFromNotif != -1L) noteIdFromNotif else null
                )
            }
        }
    }
}