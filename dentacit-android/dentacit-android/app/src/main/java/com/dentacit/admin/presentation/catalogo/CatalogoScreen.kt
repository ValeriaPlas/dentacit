package com.dentacit.admin.presentation.catalogo

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.presentation.common.EstadoVacio
import com.dentacit.admin.presentation.common.MensajeError
import com.dentacit.admin.presentation.common.PantallaCargando
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.common.formatoMoneda
import com.dentacit.admin.presentation.theme.RojoRechazada

/** Catálogo de servicios con alta, edición y baja (RF-07). */
@Composable
fun CatalogoScreen() {
    val viewModel: CatalogoViewModel = viewModel(factory = CatalogoViewModel.factory(appContainer()))
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var porEliminar by remember { mutableStateOf<Servicio?>(null) }

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
            estado.cargando && estado.servicios.isEmpty() -> PantallaCargando()
            error != null && estado.servicios.isEmpty() -> MensajeError(error, viewModel::cargar)
            estado.servicios.isEmpty() -> EstadoVacio("Aún no hay servicios. Agrega el primero con el botón +.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.servicios, key = { it.id ?: it.hashCode() }) { servicio ->
                    TarjetaServicio(
                        servicio = servicio,
                        onEditar = { viewModel.abrirEditor(servicio) },
                        onEliminar = { porEliminar = servicio }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.abrirEditor(null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar servicio")
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (estado.editorAbierto) {
        EditorServicio(
            servicio = estado.servicioEnEdicion,
            guardando = estado.guardando,
            error = estado.errorEditor,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarEditor
        )
    }

    val servicioAEliminar = porEliminar
    if (servicioAEliminar != null) {
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("¿Eliminar servicio?") },
            text = { Text("\"${servicioAEliminar.nombre}\" dejará de aparecer en el catálogo.") },
            confirmButton = {
                TextButton(onClick = {
                    porEliminar = null
                    viewModel.eliminar(servicioAEliminar)
                }) { Text("Eliminar", color = RojoRechazada) }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaServicio(servicio: Servicio, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    servicio.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (servicio.descripcion.isNotBlank()) {
                    Text(
                        servicio.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    servicio.costo.formatoMoneda(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!servicio.activo) {
                    Text("Inactivo", style = MaterialTheme.typography.labelSmall, color = RojoRechazada)
                }
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar ${servicio.nombre}")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${servicio.nombre}")
            }
        }
    }
}

@Composable
private fun EditorServicio(
    servicio: Servicio?,
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, Boolean) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(servicio) { mutableStateOf(servicio?.nombre ?: "") }
    var descripcion by remember(servicio) { mutableStateOf(servicio?.descripcion ?: "") }
    var costo by remember(servicio) {
        mutableStateOf(
            servicio?.costo?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var activo by remember(servicio) { mutableStateOf(servicio?.activo ?: true) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (servicio == null) "Nuevo servicio" else "Editar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") }
                )
                OutlinedTextField(
                    value = costo,
                    onValueChange = { costo = it },
                    label = { Text("Costo (MXN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible para pacientes", modifier = Modifier.weight(1f))
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre, descripcion, costo, activo) },
                enabled = !guardando
            ) { Text(if (guardando) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
