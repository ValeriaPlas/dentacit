package com.dentacit.admin.domain.usecase

import com.dentacit.admin.domain.model.Agenda
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.ReglaNegocioException
import com.dentacit.admin.domain.repository.HorarioRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Caso de uso: consultar los bloques de horario.
 *
 * @param repository repositorio de horarios.
 */
class ObtenerHorariosUseCase(private val repository: HorarioRepository) {

    /** @return los bloques ordenados por fecha y hora de inicio. */
    suspend operator fun invoke(): List<Horario> =
        repository.obtenerHorarios().sortedWith(compareBy({ it.fecha }, { it.inicio }))
}

/**
 * Caso de uso: el dentista define un nuevo bloque de horario disponible.
 *
 * Usa el agregado [Agenda] para impedir bloques inválidos o empalmados.
 *
 * @param repository repositorio de horarios.
 * @param hoy proveedor de la fecha actual (inyectable para pruebas).
 */
class CrearHorarioUseCase(
    private val repository: HorarioRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) {
    private val formatoHora = DateTimeFormatter.ofPattern("H:mm")

    /**
     * @param fechaTexto fecha en formato AAAA-MM-DD.
     * @param inicioTexto hora de inicio en formato HH:mm.
     * @param finTexto hora de fin en formato HH:mm.
     * @return el bloque creado.
     * @throws ReglaNegocioException si el formato es incorrecto o el bloque viola la agenda.
     */
    suspend operator fun invoke(fechaTexto: String, inicioTexto: String, finTexto: String): Horario {
        val fecha = try {
            LocalDate.parse(fechaTexto.trim())
        } catch (e: DateTimeParseException) {
            throw ReglaNegocioException("La fecha debe tener el formato AAAA-MM-DD.")
        }
        val inicio = leerHora(inicioTexto, "inicio")
        val fin = leerHora(finTexto, "fin")

        Agenda(repository.obtenerHorarios()).validarNuevoBloque(fecha, inicio, fin, hoy())
        return repository.crearHorario(fecha, inicio, fin)
    }

    private fun leerHora(texto: String, campo: String): LocalTime = try {
        LocalTime.parse(texto.trim(), formatoHora)
    } catch (e: DateTimeParseException) {
        throw ReglaNegocioException("La hora de $campo debe tener el formato HH:mm (por ejemplo 09:30).")
    }
}

/**
 * Caso de uso: eliminar un bloque de horario.
 *
 * @param repository repositorio de horarios.
 */
class EliminarHorarioUseCase(private val repository: HorarioRepository) {

    /** @param id identificador del bloque. */
    suspend operator fun invoke(id: Int) = repository.eliminarHorario(id)
}
