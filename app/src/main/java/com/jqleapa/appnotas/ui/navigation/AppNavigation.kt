package com.jqlqapa.appnotas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jqleapa.appnotas.ui.screens.AddNoteScreen
import com.jqleapa.appnotas.ui.screens.GalleryScreen
import com.jqleapa.appnotas.ui.screens.ReminderScreen
import com.jqleapa.appnotas.ui.screens.SearchScreen

// --- IMPORTS CORREGIDOS (Todo unificado a 'jqlqapa') ---
import com.jqlqapa.appnotas.ui.screens.HomeScreen // <--- IMPORTANTE: La pantalla real
import com.jqlqapa.appnotas.ui.screens.CameraCaptureScreen
import com.jqlqapa.appnotas.ui.screens.EditNoteScreen
import com.jqlqapa.appnotas.ui.screens.NoteDetailScreen

import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModel
import com.jqlqapa.appnotas.data.NoteRepository
import com.jqlqapa.appnotas.ui.viewmodel.AddEditViewModelFactory
import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModelFactory

const val NOTE_ID_ARG = "noteId"

sealed class AppScreens(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Home : AppScreens("home", "Notas", Icons.Filled.Description)
    object Reminder : AppScreens("reminder", "Pendientes", Icons.Filled.Checklist)
    object Search : AppScreens("search", "Buscar", Icons.Filled.Search)
    object Camera : AppScreens("camera", "Cámara", Icons.Filled.CameraAlt)
    object Gallery : AppScreens("gallery", "Galería", Icons.Filled.PhotoLibrary)
    object AddNote : AppScreens("add_note")
    object NoteDetail : AppScreens("note_detail/{$NOTE_ID_ARG}")
    object EditNote : AppScreens("edit_note/{$NOTE_ID_ARG}")

    fun withArgs(vararg args: String): String {
        var finalRoute = this.route
        if (args.isNotEmpty()) {
            if (finalRoute.contains("{$NOTE_ID_ARG}")) {
                finalRoute = finalRoute.replace("{$NOTE_ID_ARG}", args[0])
            }
            if (args.size > 1) {
                val additionalArgs = args.drop(1).joinToString("/")
                finalRoute += "/$additionalArgs"
            }
        }
        return finalRoute
    }
}

val bottomNavItems = listOf(AppScreens.Home, AppScreens.Reminder)

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('/')

        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon!!, contentDescription = screen.label) },
                label = { Text(screen.label!!) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    noteRepository: NoteRepository
) {
    val context = LocalContext.current

    val homeViewModelFactory = remember { HomeViewModelFactory(noteRepository = noteRepository) }
    val addEditViewModelFactory = remember { AddEditViewModelFactory(noteRepository = noteRepository, context = context) }

    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreens.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreens.Home.route) { HomeScreen(navController) }
            composable(AppScreens.Reminder.route) { ReminderScreen(navController = navController) }
            composable(AppScreens.Search.route) { SearchScreen(navController = navController) }

            composable(AppScreens.Camera.route) {
                CameraCaptureScreen(viewModel = homeViewModelFactory.create(HomeViewModel::class.java))
            }
            composable(AppScreens.Gallery.route) {
                GalleryScreen(viewModel = homeViewModelFactory.create(HomeViewModel::class.java))
            }
            composable(AppScreens.AddNote.route) {
                AddNoteScreen(navController = navController, factory = addEditViewModelFactory)
            }
            composable(
                route = AppScreens.NoteDetail.route,
                arguments = listOf(navArgument(NOTE_ID_ARG) { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong(NOTE_ID_ARG)
                requireNotNull(noteId)
                NoteDetailScreen(noteId = noteId, navController = navController)
            }
            composable(
                route = AppScreens.EditNote.route,
                arguments = listOf(navArgument(NOTE_ID_ARG) { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong(NOTE_ID_ARG)
                requireNotNull(noteId)
                val editFactory = AddEditViewModelFactory(noteRepository, context, noteId)
                EditNoteScreen(navController = navController, factory = editFactory, noteId = noteId)
            }
        }
    }
}