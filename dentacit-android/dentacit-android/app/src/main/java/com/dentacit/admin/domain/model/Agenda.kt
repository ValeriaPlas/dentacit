package com.dentacit.admin.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Agregado de dominio que agrupa los horarios del dentista y protege la integridad
 * de la agenda (RNF-05: evitar el solapamiento de bloques).
 *
 * @param horarios bloques de horario existentes.
 */
class Agenda(private val horarios: List<Horario>) {

    /**
     * Verifica que un nuevo bloque pueda agregarse a la agenda.
     *
     * @param fecha día del nuevo bloque.
     * @param inicio hora de inicio.
     * @param fin hora de fin.
     * @param hoy fecha actual (parámetro para poder probar la regla).
     * @throws ReglaNegocioException si el bloque es inválido, está en el pasado o se empalma.
     */
    fun validarNuevoBloque(
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        hoy: LocalDate = LocalDate.now()
    ) {
        if (!fin.isAfter(inicio)) {
            throw ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio.")
        }
        if (fecha.isBefore(hoy)) {
            throw ReglaNegocioException("No se pueden crear horarios en fechas pasadas.")
        }
        val empalme = horarios.firstOrNull { it.seEmpalmaCon(fecha, inicio, fin) }
        if (empalme != null) {
            throw ReglaNegocioException(
                "El bloque se empalma con el horario de ${empalme.inicio} a ${empalme.fin}."
            )
        }
    }

    /**
     * Devuelve los bloques de un día ordenados por hora de inicio.
     *
     * @param fecha día a consultar.
     */
    fun horariosDe(fecha: LocalDate): List<Horario> =
        horarios.filter { it.fecha == fecha }.sortedBy { it.inicio }
}
