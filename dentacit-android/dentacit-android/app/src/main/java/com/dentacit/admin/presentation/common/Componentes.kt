package com.dentacit.admin.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.presentation.theme.AmbarPendiente
import com.dentacit.admin.presentation.theme.RojoRechazada
import com.dentacit.admin.presentation.theme.VerdeAceptada

/** Indicador de carga centrado. */
@Composable
fun PantallaCargando() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Mensaje de error con botón para reintentar.
 *
 * @param mensaje texto del error.
 * @param onReintentar acción del botón.
 */
@Composable
fun MensajeError(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(mensaje, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

/**
 * Texto centrado para listas vacías.
 *
 * @param mensaje texto a mostrar.
 */
@Composable
fun EstadoVacio(mensaje: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            mensaje,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Color semántico asociado a cada estado de cita. */
fun EstatusCita.color() = when (this) {
    EstatusCita.PENDIENTE -> AmbarPendiente
    EstatusCita.ACEPTADA -> VerdeAceptada
    EstatusCita.RECHAZADA -> RojoRechazada
}

/**
 * Etiqueta de color con el estado de la cita.
 *
 * @param estatus estado a mostrar.
 */
@Composable
fun EstatusChip(estatus: EstatusCita) {
    Surface(
        color = estatus.color().copy(alpha = 0.14f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = estatus.etiqueta,
            color = estatus.color(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
