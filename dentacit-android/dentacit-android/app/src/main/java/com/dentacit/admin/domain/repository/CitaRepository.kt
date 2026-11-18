package com.dentacit.admin.domain.repository

import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita

/** Contrato de acceso a las citas (implementado contra citas-service). */
interface CitaRepository {

    /**
     * Obtiene las citas, opcionalmente filtradas por estado.
     *
     * @param estatus estado a filtrar o `null` para todas.
     */
    suspend fun obtenerCitas(estatus: EstatusCita?): List<Cita>

    /**
     * Obtiene una cita por su identificador.
     *
     * @param id identificador de la cita.
     */
    suspend fun obtenerCita(id: Int): Cita

    /**
     * Cambia el estado de una cita en el servidor.
     *
     * @param id identificador de la cita.
     * @param estatus nuevo estado.
     */
    suspend fun actualizarEstatus(id: Int, estatus: EstatusCita)
}
