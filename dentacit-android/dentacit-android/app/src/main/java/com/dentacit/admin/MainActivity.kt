package com.dentacit.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dentacit.admin.presentation.common.PantallaCargando
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.navigation.DentaCitNavGraph
import com.dentacit.admin.presentation.navigation.Rutas
import com.dentacit.admin.presentation.theme.DentaCitTheme
import kotlinx.coroutines.flow.first

/** Única actividad de la app; decide la pantalla inicial según la sesión guardada. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DentaCitTheme {
                val container = appContainer()
                var destino by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val sesion = container.sesionStore.sesion.first()
                    destino = if (sesion != null) Rutas.HOME else Rutas.LOGIN
                }

                val destinoActual = destino
                if (destinoActual == null) {
                    PantallaCargando()
                } else {
                    DentaCitNavGraph(destinoInicial = destinoActual)
                }
            }
        }
    }
}
