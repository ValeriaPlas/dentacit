package com.dentacit.admin.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dentacit.admin.presentation.catalogo.CatalogoScreen
import com.dentacit.admin.presentation.horarios.HorariosScreen
import com.dentacit.admin.presentation.solicitudes.SolicitudesScreen

/** Pestañas de la pantalla principal. */
private enum class Pestana(val titulo: String, val icono: ImageVector) {
    SOLICITUDES("Solicitudes", Icons.Filled.Notifications),
    HORARIOS("Horarios", Icons.Filled.DateRange),
    CATALOGO("Catálogo", Icons.Filled.Build)
}

/**
 * Pantalla principal con barra de navegación inferior.
 *
 * @param onAbrirCita abre el detalle de la cita con el id indicado.
 * @param onCerrarSesion cierra la sesión del dentista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAbrirCita: (Int) -> Unit, onCerrarSesion: () -> Unit) {
    var seleccionada by rememberSaveable { mutableIntStateOf(0) }
    val pestana = Pestana.entries[seleccionada]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pestana.titulo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Pestana.entries.forEachIndexed { indice, item ->
                    NavigationBarItem(
                        selected = seleccionada == indice,
                        onClick = { seleccionada = indice },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(item.titulo) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when (pestana) {
                Pestana.SOLICITUDES -> SolicitudesScreen(onAbrirCita = onAbrirCita)
                Pestana.HORARIOS -> HorariosScreen()
                Pestana.CATALOGO -> CatalogoScreen()
            }
        }
    }
}
