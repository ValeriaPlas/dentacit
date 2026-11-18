package com.dentacit.admin.domain.usecase

import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.repository.CitaRepository

/**
 * Caso de uso: consultar las solicitudes de cita (RF-05).
 *
 * @param repository repositorio de citas.
 */
class ObtenerSolicitudesUseCase(private val repository: CitaRepository) {

    /**
     * @param estatus estado a filtrar o `null` para todas.
     * @return las citas ordenadas de la más próxima a la más lejana.
     */
    suspend operator fun invoke(estatus: EstatusCita?): List<Cita> =
        repository.obtenerCitas(estatus).sortedWith(compareBy({ it.fecha }, { it.hora }))
}

/**
 * Caso de uso: consultar el detalle de una cita.
 *
 * @param repository repositorio de citas.
 */
class ObtenerCitaUseCase(private val repository: CitaRepository) {

    /** @param id identificador de la cita. */
    suspend operator fun invoke(id: Int): Cita = repository.obtenerCita(id)
}

/**
 * Caso de uso: el dentista acepta o rechaza una cita (RF-05).
 *
 * La regla de negocio vive en [Cita.responder]; este caso de uso solo la coordina
 * con el repositorio. El correo al paciente (RF-06) lo dispara citas-service en el servidor.
 *
 * @param repository repositorio de citas.
 */
class ResponderCitaUseCase(private val repository: CitaRepository) {

    /**
     * @param id identificador de la cita.
     * @param estatus [EstatusCita.ACEPTADA] o [EstatusCita.RECHAZADA].
     * @return la cita con su nuevo estado.
     */
    suspend operator fun invoke(id: Int, estatus: EstatusCita): Cita {
        val cita = repository.obtenerCita(id)
        val actualizada = cita.responder(estatus)
        repository.actualizarEstatus(id, estatus)
        return actualizada
    }
}
