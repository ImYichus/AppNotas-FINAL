package com.jqlqapa.appnotas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jqleapa.appnotas.ui.theme.AppNotasTheme
import com.jqlqapa.appnotas.data.AppDataContainer // Singleton
import com.jqlqapa.appnotas.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CRÍTICO: Inicializar la Base de Datos antes de usarla
        // Si no pones esto, la app se cierra sola al abrir.
        AppDataContainer.initialize(this)

        setContent {
            AppNotasTheme {
                // 2. Ahora sí es seguro pedir el repositorio
                val repository = AppDataContainer.noteRepository

                AppNavigation(noteRepository = repository)
            }
        }
    }
}