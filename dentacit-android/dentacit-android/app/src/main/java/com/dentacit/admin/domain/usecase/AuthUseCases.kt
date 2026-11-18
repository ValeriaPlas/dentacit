package com.dentacit.admin.domain.usecase

import com.dentacit.admin.domain.model.ReglaNegocioException
import com.dentacit.admin.domain.model.Sesion
import com.dentacit.admin.domain.repository.AuthRepository

/**
 * Caso de uso: el dentista inicia sesión (RF-01).
 *
 * @param repository repositorio de autenticación.
 */
class IniciarSesionUseCase(private val repository: AuthRepository) {

    /**
     * Valida los datos capturados e inicia sesión.
     *
     * @param correo correo del dentista.
     * @param password contraseña.
     * @return la sesión creada.
     * @throws ReglaNegocioException si faltan datos o el correo no es válido.
     */
    suspend operator fun invoke(correo: String, password: String): Sesion {
        val correoLimpio = correo.trim()
        if (correoLimpio.isBlank() || password.isBlank()) {
            throw ReglaNegocioException("Ingresa tu correo y tu contraseña.")
        }
        if (!correoLimpio.contains("@")) {
            throw ReglaNegocioException("El correo no tiene un formato válido.")
        }
        return repository.iniciarSesion(correoLimpio, password)
    }
}

/**
 * Caso de uso: el dentista cierra sesión.
 *
 * @param repository repositorio de autenticación.
 */
class CerrarSesionUseCase(private val repository: AuthRepository) {

    /** Borra la sesión guardada. */
    suspend operator fun invoke() = repository.cerrarSesion()
}
