package com.dentacit.admin.domain.repository

import com.dentacit.admin.domain.model.Horario
import java.time.LocalDate
import java.time.LocalTime

/** Contrato de acceso a los bloques de horario (implementado contra citas-service). */
interface HorarioRepository {

    /** Obtiene todos los bloques de horario registrados. */
    suspend fun obtenerHorarios(): List<Horario>

    /**
     * Crea un bloque de horario.
     *
     * @param fecha día del bloque.
     * @param inicio hora de inicio.
     * @param fin hora de fin.
     * @return el bloque guardado.
     */
    suspend fun crearHorario(fecha: LocalDate, inicio: LocalTime, fin: LocalTime): Horario

    /**
     * Elimina un bloque de horario.
     *
     * @param id identificador del bloque.
     */
    suspend fun eliminarHorario(id: Int)
}
