package com.dentacit.admin.presentation.solicitudes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.presentation.common.EstadoVacio
import com.dentacit.admin.presentation.common.EstatusChip
import com.dentacit.admin.presentation.common.MensajeError
import com.dentacit.admin.presentation.common.PantallaCargando
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.common.formato
import com.dentacit.admin.presentation.common.formatoCorto

/**
 * Bandeja de solicitudes de cita filtrada por estado (RF-05).
 *
 * @param onAbrirCita abre el detalle de la cita seleccionada.
 */
@Composable
fun SolicitudesScreen(onAbrirCita: (Int) -> Unit) {
    val viewModel: SolicitudesViewModel =
        viewModel(factory = SolicitudesViewModel.factory(appContainer()))
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Se recarga cada vez que la pantalla vuelve a mostrarse (por ejemplo, al regresar del detalle).
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = EstatusCita.entries.indexOf(estado.filtro)) {
            EstatusCita.entries.forEach { estatus ->
                Tab(
                    selected = estado.filtro == estatus,
                    onClick = { viewModel.cambiarFiltro(estatus) },
                    text = { Text(estatus.etiqueta) }
                )
            }
        }
        if (estado.cargando && estado.citas.isNotEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        val error = estado.error
        when {
            estado.cargando && estado.citas.isEmpty() -> PantallaCargando()
            error != null -> MensajeError(error, onReintentar = viewModel::cargar)
            estado.citas.isEmpty() ->
                EstadoVacio("No hay solicitudes ${estado.filtro.etiqueta.lowercase()}s.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.citas, key = { it.id }) { cita ->
                    TarjetaCita(cita = cita, onClick = { onAbrirCita(cita.id) })
                }
            }
        }
    }
}

/**
 * Tarjeta resumida de una cita.
 *
 * @param cita cita a mostrar.
 * @param onClick acción al tocar la tarjeta.
 */
@Composable
private fun TarjetaCita(cita: Cita, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cita.paciente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                EstatusChip(cita.estatus)
            }
            Spacer(Modifier.height(4.dp))
            Text(cita.servicio, color = MaterialTheme.colorScheme.primary)
            Text(
                "${cita.fecha.formatoCorto()} · ${cita.hora.formato()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
