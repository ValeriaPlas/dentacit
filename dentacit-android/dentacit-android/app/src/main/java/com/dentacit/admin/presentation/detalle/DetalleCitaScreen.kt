package com.dentacit.admin.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.presentation.common.EstatusChip
import com.dentacit.admin.presentation.common.MensajeError
import com.dentacit.admin.presentation.common.PantallaCargando
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.common.formato
import com.dentacit.admin.presentation.common.formatoLargo
import com.dentacit.admin.presentation.theme.RojoRechazada
import com.dentacit.admin.presentation.theme.VerdeAceptada

/**
 * Detalle de una cita con las acciones de aceptar o rechazar.
 *
 * @param idCita identificador de la cita.
 * @param onRegresar vuelve a la bandeja de solicitudes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCitaScreen(idCita: Int, onRegresar: () -> Unit) {
    val viewModel: DetalleCitaViewModel =
        viewModel(factory = DetalleCitaViewModel.factory(appContainer(), idCita))
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarRechazo by remember { mutableStateOf(false) }

    LaunchedEffect(estado.mensaje) {
        val mensaje = estado.mensaje
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de la cita") },
                navigationIcon = {
                    IconButton(onClick = onRegresar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            val cita = estado.cita
            val error = estado.error
            when {
                estado.cargando -> PantallaCargando()
                cita == null -> MensajeError(error ?: "No se encontró la cita.", viewModel::cargar)
                else -> ContenidoDetalle(
                    cita = cita,
                    procesando = estado.procesando,
                    onAceptar = { viewModel.responder(EstatusCita.ACEPTADA) },
                    onRechazar = { confirmarRechazo = true }
                )
            }
        }
    }

    if (confirmarRechazo) {
        AlertDialog(
            onDismissRequest = { confirmarRechazo = false },
            title = { Text("¿Rechazar la cita?") },
            text = { Text("Se notificará al paciente por correo y la cita ya no podrá modificarse.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRechazo = false
                    viewModel.responder(EstatusCita.RECHAZADA)
                }) { Text("Rechazar", color = RojoRechazada) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRechazo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ContenidoDetalle(
    cita: Cita,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Seccion("Cita") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cita.servicio,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                EstatusChip(cita.estatus)
            }
            Campo("Fecha", cita.fecha.formatoLargo())
            Campo("Hora", cita.hora.formato())
        }
        Seccion("Paciente") {
            Campo("Nombre", cita.paciente.nombre)
            Campo("Correo", cita.paciente.correo)
            Campo("Teléfono", cita.paciente.telefono.ifBlank { "Sin teléfono" })
        }
        Seccion("Síntomas") {
            Text(cita.sintomas.ifBlank { "El paciente no describió síntomas." })
        }

        Spacer(Modifier.height(8.dp))
        if (cita.puedeResponderse) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRechazar,
                    enabled = !procesando,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RojoRechazada),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Rechazar") }
                Button(
                    onClick = onAceptar,
                    enabled = !procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeAceptada),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    if (procesando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Aceptar")
                    }
                }
            }
        } else {
            Text(
                "Esta cita ya fue respondida y no puede modificarse.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                titulo.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            contenido()
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String) {
    Column {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
