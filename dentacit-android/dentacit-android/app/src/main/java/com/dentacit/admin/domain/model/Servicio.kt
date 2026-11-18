package com.dentacit.admin.domain.model

/**
 * Servicio odontológico del catálogo del consultorio.
 *
 * @property id identificador; es `null` cuando el servicio aún no se ha guardado.
 * @property nombre nombre visible para el paciente.
 * @property descripcion descripción breve del tratamiento.
 * @property costo precio en pesos mexicanos.
 * @property activo solo los servicios activos aparecen en el catálogo del paciente.
 */
data class Servicio(
    val id: Int?,
    val nombre: String,
    val descripcion: String,
    val costo: Double,
    val activo: Boolean = true
)
