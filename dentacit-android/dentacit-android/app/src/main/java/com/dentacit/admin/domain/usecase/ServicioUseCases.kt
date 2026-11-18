package com.dentacit.admin.domain.usecase

import com.dentacit.admin.domain.model.ReglaNegocioException
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.domain.repository.ServicioRepository

/**
 * Caso de uso: consultar el catálogo de servicios.
 *
 * @param repository repositorio de servicios.
 */
class ObtenerServiciosUseCase(private val repository: ServicioRepository) {

    /** @return los servicios ordenados alfabéticamente. */
    suspend operator fun invoke(): List<Servicio> =
        repository.obtenerServicios().sortedBy { it.nombre.lowercase() }
}

/**
 * Caso de uso: crear o editar un servicio del catálogo (RF-07).
 *
 * @param repository repositorio de servicios.
 */
class GuardarServicioUseCase(private val repository: ServicioRepository) {

    /**
     * @param id identificador del servicio o `null` si es nuevo.
     * @param nombre nombre del servicio.
     * @param descripcion descripción del servicio.
     * @param costo costo en pesos; `null` si el texto capturado no es un número.
     * @param activo si el servicio se muestra a los pacientes.
     * @return el servicio guardado.
     * @throws ReglaNegocioException si el nombre está vacío o el costo no es válido.
     */
    suspend operator fun invoke(
        id: Int?,
        nombre: String,
        descripcion: String,
        costo: Double?,
        activo: Boolean
    ): Servicio {
        if (nombre.isBlank()) {
            throw ReglaNegocioException("El nombre del servicio es obligatorio.")
        }
        if (costo == null || costo <= 0.0) {
            throw ReglaNegocioException("El costo debe ser un número mayor a cero.")
        }
        val servicio = Servicio(id, nombre.trim(), descripcion.trim(), costo, activo)
        return if (id == null) {
            repository.crearServicio(servicio)
        } else {
            repository.actualizarServicio(servicio)
        }
    }
}

/**
 * Caso de uso: eliminar un servicio del catálogo (RF-07).
 *
 * @param repository repositorio de servicios.
 */
class EliminarServicioUseCase(private val repository: ServicioRepository) {

    /** @param id identificador del servicio. */
    suspend operator fun invoke(id: Int) = repository.eliminarServicio(id)
}
