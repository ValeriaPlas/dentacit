package com.dentacit.admin.domain.model

/**
 * Estados posibles de una cita dentro de su ciclo de vida.
 *
 * Transiciones válidas: PENDIENTE → ACEPTADA y PENDIENTE → RECHAZADA.
 *
 * @property etiqueta texto con el que el estado se muestra y se envía al API.
 */
enum class EstatusCita(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada");

    companion object {
        /**
         * Convierte el texto recibido del API en un [EstatusCita].
         *
         * @param texto valor del campo `estatus` (sin distinguir mayúsculas).
         * @return el estado correspondiente o [PENDIENTE] si el texto no se reconoce.
         */
        fun desdeTexto(texto: String): EstatusCita =
            entries.firstOrNull { it.etiqueta.equals(texto, ignoreCase = true) } ?: PENDIENTE
    }
}
