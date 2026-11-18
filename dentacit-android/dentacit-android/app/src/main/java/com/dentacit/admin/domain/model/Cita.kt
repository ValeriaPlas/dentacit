package com.dentacit.admin.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Entidad de dominio que representa una solicitud de cita.
 *
 * Concentra la regla de negocio "una vez aceptada o rechazada, la cita ya no se modifica",
 * para que ningún controlador ni pantalla pueda saltarla.
 *
 * @property id identificador de la cita.
 * @property paciente datos del paciente que la solicitó.
 * @property servicio nombre del servicio odontológico solicitado.
 * @property fecha día de la cita.
 * @property hora hora de inicio de la cita.
 * @property sintomas descripción de síntomas escrita por el paciente.
 * @property estatus estado actual de la cita.
 */
data class Cita(
    val id: Int,
    val paciente: Paciente,
    val servicio: String,
    val fecha: LocalDate,
    val hora: LocalTime,
    val sintomas: String,
    val estatus: EstatusCita
) {
    /** Indica si el dentista todavía puede aceptar o rechazar la cita. */
    val puedeResponderse: Boolean
        get() = estatus == EstatusCita.PENDIENTE

    /**
     * Aplica la respuesta del dentista a la cita.
     *
     * @param nuevoEstatus [EstatusCita.ACEPTADA] o [EstatusCita.RECHAZADA].
     * @return una copia de la cita con el nuevo estado.
     * @throws ReglaNegocioException si se intenta volver a Pendiente o la cita ya fue respondida.
     */
    fun responder(nuevoEstatus: EstatusCita): Cita {
        if (nuevoEstatus == EstatusCita.PENDIENTE) {
            throw ReglaNegocioException("Una cita solo puede aceptarse o rechazarse.")
        }
        if (!puedeResponderse) {
            throw ReglaNegocioException(
                "La cita ya fue ${estatus.etiqueta.lowercase()} y no puede modificarse."
            )
        }
        return copy(estatus = nuevoEstatus)
    }
}
