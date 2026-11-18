package com.dentacit.admin.presentation.horarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentacit.admin.presentation.common.EstadoVacio
import com.dentacit.admin.presentation.common.MensajeError
import com.dentacit.admin.presentation.common.PantallaCargando
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.common.formato
import com.dentacit.admin.presentation.common.formatoLargo
import java.time.LocalDate

/** Administración de los bloques de horario en los que los pacientes pueden agendar. */
@Composable
fun HorariosScreen() {
    val viewModel: HorariosViewModel = viewModel(factory = HorariosViewModel.factory(appContainer()))
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje) {
        val mensaje = estado.mensaje
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            viewModel.mensajeMostrado()
        }
    }

    Box(Modifier.fillMaxSize()) {
        val error = estado.error
        when {
            estado.cargando && estado.horarios.isEmpty() -> PantallaCargando()
            error != null && estado.horarios.isEmpty() -> MensajeError(error, viewModel::cargar)
            estado.horarios.isEmpty() -> EstadoVacio("No hay horarios definidos. Agrega uno con el botón +.")
            else -> {
                val porDia = estado.horarios.groupBy { it.fecha }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    porDia.forEach { (fecha, bloques) ->
                        item(key = "dia-$fecha") {
                            Text(
                                fecha.formatoLargo().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(bloques, key = { it.id }) { horario ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${horario.inicio.formato()} – ${horario.fin.formato()}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.eliminar(horario) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar horario")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::abrirEditor,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar horario")
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (estado.editorAbierto) {
        EditorHorario(
            guardando = estado.guardando,
            error = estado.errorEditor,
            onGuardar = viewModel::crear,
            onCancelar = viewModel::cerrarEditor
        )
    }
}

@Composable
private fun EditorHorario(
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var fecha by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var inicio by remember { mutableStateOf("09:00") }
    var fin by remember { mutableStateOf("13:00") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo horario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = inicio,
                    onValueChange = { inicio = it },
                    label = { Text("Hora de inicio (HH:mm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                OutlinedTextField(
                    value = fin,
                    onValueChange = { fin = it },
                    label = { Text("Hora de fin (HH:mm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(fecha, inicio, fin) }, enabled = !guardando) {
                Text(if (guardando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
