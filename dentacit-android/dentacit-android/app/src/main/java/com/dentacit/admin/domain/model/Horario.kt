package com.dentacit.admin.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Bloque de horario en el que el dentista atiende y los pacientes pueden agendar.
 *
 * @property id identificador del bloque.
 * @property fecha día del bloque.
 * @property inicio hora de inicio.
 * @property fin hora de fin (exclusiva).
 */
data class Horario(
    val id: Int,
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime
) {
    /**
     * Indica si este bloque se empalma con otro intervalo.
     *
     * Dos bloques contiguos (por ejemplo 9:00–10:00 y 10:00–11:00) no se empalman.
     *
     * @param fecha día del otro intervalo.
     * @param inicio inicio del otro intervalo.
     * @param fin fin del otro intervalo.
     * @return `true` si ambos intervalos comparten al menos un minuto el mismo día.
     */
    fun seEmpalmaCon(fecha: LocalDate, inicio: LocalTime, fin: LocalTime): Boolean =
        this.fecha == fecha && inicio < this.fin && this.inicio < fin
}
