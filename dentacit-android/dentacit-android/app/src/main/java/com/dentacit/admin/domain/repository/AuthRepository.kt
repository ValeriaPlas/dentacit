package com.dentacit.admin.domain.repository

import com.dentacit.admin.domain.model.Sesion
import kotlinx.coroutines.flow.Flow

/** Contrato de autenticación del dentista (implementado contra auth-service). */
interface AuthRepository {

    /**
     * Inicia sesión y guarda el token localmente.
     *
     * @param correo correo del dentista.
     * @param password contraseña en texto plano (viaja por HTTPS).
     * @return la sesión creada.
     */
    suspend fun iniciarSesion(correo: String, password: String): Sesion

    /** Elimina la sesión guardada en el dispositivo. */
    suspend fun cerrarSesion()

    /** Flujo con la sesión actual, o `null` si no hay sesión iniciada. */
    fun sesionActiva(): Flow<Sesion?>
}
