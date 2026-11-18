package com.dentacit.admin.presentation.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.domain.usecase.EliminarServicioUseCase
import com.dentacit.admin.domain.usecase.GuardarServicioUseCase
import com.dentacit.admin.domain.usecase.ObtenerServiciosUseCase
import com.dentacit.admin.presentation.common.mensajeUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de catálogo. */
data class CatalogoUiState(
    val servicios: List<Servicio> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val editorAbierto: Boolean = false,
    val servicioEnEdicion: Servicio? = null,
    val guardando: Boolean = false,
    val errorEditor: String? = null,
    val mensaje: String? = null
)

/**
 * ViewModel del CRUD del catálogo de servicios (RF-07).
 *
 * @param obtenerServicios caso de uso de consulta.
 * @param guardarServicio caso de uso de alta/edición.
 * @param eliminarServicio caso de uso de baja.
 */
class CatalogoViewModel(
    private val obtenerServicios: ObtenerServiciosUseCase,
    private val guardarServicio: GuardarServicioUseCase,
    private val eliminarServicio: EliminarServicioUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(CatalogoUiState())

    /** Estado observable por la pantalla. */
    val estado: StateFlow<CatalogoUiState> = _estado.asStateFlow()

    /** Carga los servicios. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val servicios = obtenerServicios()
                _estado.update { it.copy(servicios = servicios, cargando = false) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.mensajeUsuario()) }
            }
        }
    }

    /**
     * Abre el formulario de servicio.
     *
     * @param servicio servicio a editar, o `null` para crear uno nuevo.
     */
    fun abrirEditor(servicio: Servicio?) = _estado.update {
        it.copy(editorAbierto = true, servicioEnEdicion = servicio, errorEditor = null)
    }

    /** Cierra el formulario sin guardar. */
    fun cerrarEditor() = _estado.update {
        it.copy(editorAbierto = false, servicioEnEdicion = null, errorEditor = null)
    }

    /**
     * Guarda el servicio capturado en el formulario.
     *
     * @param nombre nombre capturado.
     * @param descripcion descripción capturada.
     * @param costoTexto costo capturado como texto.
     * @param activo si el servicio estará visible para pacientes.
     */
    fun guardar(nombre: String, descripcion: String, costoTexto: String, activo: Boolean) {
        if (_estado.value.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorEditor = null) }
            try {
                guardarServicio(
                    id = _estado.value.servicioEnEdicion?.id,
                    nombre = nombre,
                    descripcion = descripcion,
                    costo = costoTexto.replace(",", ".").trim().toDoubleOrNull(),
                    activo = activo
                )
                _estado.update {
                    it.copy(
                        guardando = false,
                        editorAbierto = false,
                        servicioEnEdicion = null,
                        mensaje = "Servicio guardado."
                    )
                }
                cargar()
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, errorEditor = e.mensajeUsuario()) }
            }
        }
    }

    /**
     * Elimina un servicio del catálogo.
     *
     * @param servicio servicio a eliminar.
     */
    fun eliminar(servicio: Servicio) {
        val id = servicio.id ?: return
        viewModelScope.launch {
            try {
                eliminarServicio(id)
                _estado.update { it.copy(mensaje = "Servicio \"${servicio.nombre}\" eliminado.") }
                cargar()
            } catch (e: Exception) {
                _estado.update { it.copy(mensaje = e.mensajeUsuario()) }
            }
        }
    }

    /** Indica que el mensaje ya se mostró. */
    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    companion object {
        /** Fábrica que obtiene los casos de uso del [AppContainer]. */
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CatalogoViewModel(
                    container.obtenerServicios,
                    container.guardarServicio,
                    container.eliminarServicio
                )
            }
        }
    }
}
