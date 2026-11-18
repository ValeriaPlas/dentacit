package com.dentacit.admin.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.usecase.ObtenerCitaUseCase
import com.dentacit.admin.domain.usecase.ResponderCitaUseCase
import com.dentacit.admin.presentation.common.mensajeUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del detalle de una cita. */
data class DetalleCitaUiState(
    val cita: Cita? = null,
    val cargando: Boolean = true,
    val procesando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

/**
 * ViewModel del detalle de cita: consulta y respuesta (aceptar/rechazar).
 *
 * @param idCita identificador de la cita.
 * @param obtenerCita caso de uso de consulta.
 * @param responderCita caso de uso de respuesta.
 */
class DetalleCitaViewModel(
    private val idCita: Int,
    private val obtenerCita: ObtenerCitaUseCase,
    private val responderCita: ResponderCitaUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleCitaUiState())

    /** Estado observable por la pantalla. */
    val estado: StateFlow<DetalleCitaUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    /** Carga la cita desde el repositorio. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val cita = obtenerCita(idCita)
                _estado.update { it.copy(cita = cita, cargando = false) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.mensajeUsuario()) }
            }
        }
    }

    /**
     * Acepta o rechaza la cita.
     *
     * @param estatus [EstatusCita.ACEPTADA] o [EstatusCita.RECHAZADA].
     */
    fun responder(estatus: EstatusCita) {
        if (_estado.value.procesando) return
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true) }
            try {
                val actualizada = responderCita(idCita, estatus)
                val mensaje = if (estatus == EstatusCita.ACEPTADA) {
                    "Cita aceptada. Se enviará la confirmación por correo al paciente."
                } else {
                    "Cita rechazada. Se notificará al paciente por correo."
                }
                _estado.update { it.copy(cita = actualizada, procesando = false, mensaje = mensaje) }
            } catch (e: Exception) {
                _estado.update { it.copy(procesando = false, mensaje = e.mensajeUsuario()) }
            }
        }
    }

    /** Indica que el mensaje ya se mostró en el Snackbar. */
    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    companion object {
        /** Fábrica que recibe el id de la cita desde la navegación. */
        fun factory(container: AppContainer, idCita: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DetalleCitaViewModel(idCita, container.obtenerCita, container.responderCita)
                }
            }
    }
}
