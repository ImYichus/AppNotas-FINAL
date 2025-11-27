package com.jqleapa.appnotas.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.jqlqapa.appnotas.data.model.MediaEntity // Asegúrate de que esta importación sea correcta
//import com.jqleapa.appnotas.ui.viewmodel.HomeViewModel
// Importaciones de estado de Compose
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jqlqapa.appnotas.ui.viewmodel.HomeViewModel


@Composable
fun GalleryScreen(viewModel: HomeViewModel) {
    val configuration = LocalConfiguration.current
    // Adaptabilidad: si la pantalla es ancha (> 600dp), usa 3 columnas, sino 2.
    val isTablet = configuration.screenWidthDp > 600

    // Observa el StateFlow del ViewModel y lo convierte en State de Compose
    val mediaList: List<MediaEntity> by viewModel.allMedia.collectAsState(initial = emptyList())

    // Filtra la lista para obtener solo las imágenes (IMAGE)
    val imagesList = mediaList.filter { it.mediaType == "IMAGE" }

    if (imagesList.isEmpty()) { // Ahora verifica la lista filtrada
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay fotos aún")
        }
    } else {
        val columns = if (isTablet) 3 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CORRECCIÓN CLAVE: Pasamos la lista filtrada y usamos 'media' para iterar.
            items(
                items = imagesList, // La lista de imágenes filtrada
                key = { media -> media.id }
            ) { media -> // 'media' es ahora el elemento MediaEntity

                // Usamos las propiedades del objeto 'media' (media.filePath, media.description)
                val painter = rememberAsyncImagePainter(model = media.filePath)

                Image(
                    painter = painter,
                    contentDescription = media.description ?: "Imagen",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { /* abrir detalle o eliminar */ },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}