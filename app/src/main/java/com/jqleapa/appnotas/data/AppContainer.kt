package com.jqlqapa.appnotas.data // Paquete correcto


import android.content.Context
import androidx.room.Room
import com.jqlqapa.appnotas.data.model.MediaDao
import com.jqlqapa.appnotas.data.model.NoteDao
import com.jqlqapa.appnotas.data.model.ReminderDao
import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModelFactory


/**
 * Contenedor de dependencias simple (Service Locator) para toda la aplicación.
 * Provee la instancia de la base de datos, DAOs y Repositorios.
 */
interface AppContainer {
    val noteRepository: NoteRepository
    val homeViewModelFactory: HomeViewModelFactory
}

// *** MODIFICACIÓN CLAVE: Se mantiene como un objeto Singleton/Service Locator ***
object AppDataContainer : AppContainer {

    private lateinit var internalNoteRepository: NoteRepository
    private lateinit var internalHomeViewModelFactory: HomeViewModelFactory
    private var isInitialized = false

    // Propiedades de la interfaz. Esto lanza el error si no se llama a initialize.
    override val noteRepository: NoteRepository
        get() = if (isInitialized) internalNoteRepository else throw IllegalStateException("AppContainer not initialized. Call initialize(context) first.")

    override val homeViewModelFactory: HomeViewModelFactory
        get() = if (isInitialized) internalHomeViewModelFactory else throw IllegalStateException("AppContainer not initialized. Call initialize(context) first.")

    // Función de inicialización
    fun initialize(context: Context) {
        if (isInitialized) return

        val applicationContext = context.applicationContext

        // 1. Base de datos
        val database: AppDatabase = Room.databaseBuilder(
            context = applicationContext,
            klass = AppDatabase::class.java,
            name = "notes_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        // 2. Provisión de DAOs
        val noteDao: NoteDao = database.noteDao()
        val mediaDao: MediaDao = database.mediaDao()
        val reminderDao: ReminderDao = database.reminderDao()

        // 3. Provisión de Repositorio (¡CORRECCIÓN CLAVE AQUÍ!)
        // Ahora se instancia la clase de IMPLEMENTACIÓN (NoteRepositoryImpl)
        internalNoteRepository = NoteRepositoryImpl(noteDao, mediaDao, reminderDao)

        // 4. Provisión de la Factory de ViewModel
        internalHomeViewModelFactory = HomeViewModelFactory(noteRepository = internalNoteRepository)

        isInitialized = true
    }
}