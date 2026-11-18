package com.dentacit.admin.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.usecase.IniciarSesionUseCase
import com.dentacit.admin.presentation.common.mensajeUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de inicio de sesión. */
data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

/**
 * ViewModel del inicio de sesión del dentista.
 *
 * @param iniciarSesion caso de uso de autenticación.
 */
class LoginViewModel(private val iniciarSesion: IniciarSesionUseCase) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())

    /** Estado observable por la pantalla. */
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** Actualiza el correo capturado. */
    fun onCorreoChange(valor: String) = _estado.update { it.copy(correo = valor, error = null) }

    /** Actualiza la contraseña capturada. */
    fun onPasswordChange(valor: String) = _estado.update { it.copy(password = valor, error = null) }

    /** Intenta iniciar sesión con los datos capturados. */
    fun entrar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                iniciarSesion(_estado.value.correo, _estado.value.password)
                _estado.update { it.copy(cargando = false, exito = true) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.mensajeUsuario()) }
            }
        }
    }

    companion object {
        /** Fábrica que obtiene el caso de uso del [AppContainer]. */
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(container.iniciarSesion) }
        }
    }
}
