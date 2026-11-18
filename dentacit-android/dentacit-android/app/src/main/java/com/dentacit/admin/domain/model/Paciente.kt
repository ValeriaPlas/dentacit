package com.dentacit.admin.domain.model

/**
 * Paciente que solicita una cita desde el cliente web.
 *
 * @property nombre nombre completo.
 * @property correo correo donde recibe las notificaciones.
 * @property telefono teléfono de contacto.
 */
data class Paciente(
    val nombre: String,
    val correo: String,
    val telefono: String
)
