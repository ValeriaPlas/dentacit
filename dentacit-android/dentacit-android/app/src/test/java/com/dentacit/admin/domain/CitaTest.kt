package com.dentacit.admin.domain

import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.model.Paciente
import com.dentacit.admin.domain.model.ReglaNegocioException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Pruebas unitarias de las reglas de negocio de la entidad [Cita]. */
class CitaTest {

    private fun cita(estatus: EstatusCita) = Cita(
        id = 1,
        paciente = Paciente("Ana", "ana@correo.com", "4771234567"),
        servicio = "Limpieza dental",
        fecha = LocalDate.of(2026, 12, 1),
        hora = LocalTime.of(10, 0),
        sintomas = "Sangrado de encías",
        estatus = estatus
    )

    @Test
    fun `una cita pendiente puede aceptarse`() {
        val resultado = cita(EstatusCita.PENDIENTE).responder(EstatusCita.ACEPTADA)
        assertEquals(EstatusCita.ACEPTADA, resultado.estatus)
    }

    @Test
    fun `una cita pendiente puede rechazarse`() {
        val resultado = cita(EstatusCita.PENDIENTE).responder(EstatusCita.RECHAZADA)
        assertEquals(EstatusCita.RECHAZADA, resultado.estatus)
    }

    @Test
    fun `una cita ya aceptada no puede modificarse`() {
        assertThrows(ReglaNegocioException::class.java) {
            cita(EstatusCita.ACEPTADA).responder(EstatusCita.RECHAZADA)
        }
    }

    @Test
    fun `una cita no puede regresar a pendiente`() {
        assertThrows(ReglaNegocioException::class.java) {
            cita(EstatusCita.PENDIENTE).responder(EstatusCita.PENDIENTE)
        }
    }

    @Test
    fun `el texto del API se convierte al estado correcto`() {
        assertEquals(EstatusCita.ACEPTADA, EstatusCita.desdeTexto("aceptada"))
        assertEquals(EstatusCita.RECHAZADA, EstatusCita.desdeTexto("Rechazada"))
    }
}
