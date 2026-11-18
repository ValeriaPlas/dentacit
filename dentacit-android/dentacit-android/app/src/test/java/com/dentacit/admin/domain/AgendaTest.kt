package com.dentacit.admin.domain

import com.dentacit.admin.domain.model.Agenda
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.ReglaNegocioException
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Pruebas unitarias del agregado [Agenda] (RNF-05: sin empalmes). */
class AgendaTest {

    private val hoy = LocalDate.of(2026, 11, 18)
    private val manana = hoy.plusDays(1)
    private val agenda = Agenda(
        listOf(Horario(1, manana, LocalTime.of(9, 0), LocalTime.of(13, 0)))
    )

    @Test
    fun `un bloque que se empalma es rechazado`() {
        assertThrows(ReglaNegocioException::class.java) {
            agenda.validarNuevoBloque(manana, LocalTime.of(12, 0), LocalTime.of(14, 0), hoy)
        }
    }

    @Test
    fun `un bloque contiguo es valido`() {
        agenda.validarNuevoBloque(manana, LocalTime.of(13, 0), LocalTime.of(15, 0), hoy)
    }

    @Test
    fun `el mismo horario en otro dia es valido`() {
        agenda.validarNuevoBloque(manana.plusDays(1), LocalTime.of(9, 0), LocalTime.of(13, 0), hoy)
    }

    @Test
    fun `la hora de fin debe ser posterior al inicio`() {
        assertThrows(ReglaNegocioException::class.java) {
            agenda.validarNuevoBloque(manana, LocalTime.of(16, 0), LocalTime.of(15, 0), hoy)
        }
    }

    @Test
    fun `no se permiten fechas pasadas`() {
        assertThrows(ReglaNegocioException::class.java) {
            agenda.validarNuevoBloque(hoy.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), hoy)
        }
    }
}
