package com.dentacit.admin.domain.repository

import com.dentacit.admin.domain.model.Servicio

/** Contrato de acceso al catálogo de servicios (implementado contra catalogo-service). */
interface ServicioRepository {

    /** Obtiene todos los servicios, activos e inactivos. */
    suspend fun obtenerServicios(): List<Servicio>

    /**
     * Crea un servicio nuevo.
     *
     * @param servicio servicio con `id` nulo.
     * @return el servicio guardado con su `id`.
     */
    suspend fun crearServicio(servicio: Servicio): Servicio

    /**
     * Actualiza un servicio existente.
     *
     * @param servicio servicio con `id` no nulo.
     * @return el servicio actualizado.
     */
    suspend fun actualizarServicio(servicio: Servicio): Servicio

    /**
     * Elimina (desactiva) un servicio.
     *
     * @param id identificador del servicio.
     */
    suspend fun eliminarServicio(id: Int)
}
