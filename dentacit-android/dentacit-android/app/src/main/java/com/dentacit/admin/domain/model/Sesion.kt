package com.dentacit.admin.domain.model

/**
 * Sesión autenticada del dentista.
 *
 * @property token JWT emitido por auth-service.
 * @property nombre nombre del dentista para mostrar en la app.
 */
data class Sesion(
    val token: String,
    val nombre: String
)
