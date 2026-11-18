package com.dentacit.admin.presentation.solicitudes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.usecase.ObtenerSolicitudesUseCase
import com.dentacit.admin.presentation.common.mensajeUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la bandeja de solicitudes. */
data class SolicitudesUiState(
    val filtro: EstatusCita = EstatusCita.PENDIENTE,
    val citas: List<Cita> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel de la bandeja de solicitudes de cita.
 *
 * @param obtenerSolicitudes caso de uso de consulta de citas.
 */
class SolicitudesViewModel(
    private val obtenerSolicitudes: ObtenerSolicitudesUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(SolicitudesUiState())

    /** Estado observable por la pantalla. */
    val estado: StateFlow<SolicitudesUiState> = _estado.asStateFlow()

    /** Carga las citas del filtro actual. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val citas = obtenerSolicitudes(_estado.value.filtro)
                _estado.update { it.copy(citas = citas, cargando = false) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.mensajeUsuario()) }
            }
        }
    }

    /**
     * Cambia la pestaña de estado y recarga.
     *
     * @param filtro estado seleccionado.
     */
    fun cambiarFiltro(filtro: EstatusCita) {
        if (filtro == _estado.value.filtro) return
        _estado.update { it.copy(filtro = filtro, citas = emptyList()) }
        cargar()
    }

    companion object {
        /** Fábrica que obtiene el caso de uso del [AppContainer]. */
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SolicitudesViewModel(container.obtenerSolicitudes) }
        }
    }
}
