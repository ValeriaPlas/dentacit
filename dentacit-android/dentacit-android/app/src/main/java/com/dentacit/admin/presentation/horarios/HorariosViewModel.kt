package com.dentacit.admin.presentation.horarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.usecase.CrearHorarioUseCase
import com.dentacit.admin.domain.usecase.EliminarHorarioUseCase
import com.dentacit.admin.domain.usecase.ObtenerHorariosUseCase
import com.dentacit.admin.presentation.common.mensajeUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de horarios. */
data class HorariosUiState(
    val horarios: List<Horario> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val editorAbierto: Boolean = false,
    val guardando: Boolean = false,
    val errorEditor: String? = null,
    val mensaje: String? = null
)

/**
 * ViewModel de la administración de bloques de horario.
 *
 * @param obtenerHorarios caso de uso de consulta.
 * @param crearHorario caso de uso de alta (valida empalmes con [com.dentacit.admin.domain.model.Agenda]).
 * @param eliminarHorario caso de uso de baja.
 */
class HorariosViewModel(
    private val obtenerHorarios: ObtenerHorariosUseCase,
    private val crearHorario: CrearHorarioUseCase,
    private val eliminarHorario: EliminarHorarioUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(HorariosUiState())

    /** Estado observable por la pantalla. */
    val estado: StateFlow<HorariosUiState> = _estado.asStateFlow()

    /** Carga los bloques de horario. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val horarios = obtenerHorarios()
                _estado.update { it.copy(horarios = horarios, cargando = false) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.mensajeUsuario()) }
            }
        }
    }

    /** Abre el formulario de nuevo bloque. */
    fun abrirEditor() = _estado.update { it.copy(editorAbierto = true, errorEditor = null) }

    /** Cierra el formulario. */
    fun cerrarEditor() = _estado.update { it.copy(editorAbierto = false, errorEditor = null) }

    /**
     * Crea un bloque de horario.
     *
     * @param fecha fecha en formato AAAA-MM-DD.
     * @param inicio hora de inicio HH:mm.
     * @param fin hora de fin HH:mm.
     */
    fun crear(fecha: String, inicio: String, fin: String) {
        if (_estado.value.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorEditor = null) }
            try {
                crearHorario(fecha, inicio, fin)
                _estado.update {
                    it.copy(guardando = false, editorAbierto = false, mensaje = "Horario agregado.")
                }
                cargar()
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, errorEditor = e.mensajeUsuario()) }
            }
        }
    }

    /**
     * Elimina un bloque de horario.
     *
     * @param horario bloque a eliminar.
     */
    fun eliminar(horario: Horario) {
        viewModelScope.launch {
            try {
                eliminarHorario(horario.id)
                _estado.update { it.copy(mensaje = "Horario eliminado.") }
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
                HorariosViewModel(
                    container.obtenerHorarios,
                    container.crearHorario,
                    container.eliminarHorario
                )
            }
        }
    }
}
