package com.dentacit.admin.domain

import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.Paciente
import com.dentacit.admin.domain.model.ReglaNegocioException
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.domain.repository.CitaRepository
import com.dentacit.admin.domain.repository.HorarioRepository
import com.dentacit.admin.domain.repository.ServicioRepository
import com.dentacit.admin.domain.usecase.CrearHorarioUseCase
import com.dentacit.admin.domain.usecase.GuardarServicioUseCase
import com.dentacit.admin.domain.usecase.ResponderCitaUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Pruebas unitarias de los casos de uso con repositorios en memoria. */
class CasosDeUsoTest {

    // ---------- Repositorios falsos para las pruebas ----------

    private class CitasEnMemoria(var cita: Cita) : CitaRepository {
        var actualizaciones = 0
        override suspend fun obtenerCitas(estatus: EstatusCita?) = listOf(cita)
        override suspend fun obtenerCita(id: Int) = cita
        override suspend fun actualizarEstatus(id: Int, estatus: EstatusCita) {
            actualizaciones++
            cita = cita.copy(estatus = estatus)
        }
    }

    private class ServiciosEnMemoria : ServicioRepository {
        val servicios = mutableListOf<Servicio>()
        override suspend fun obtenerServicios() = servicios.toList()
        override suspend fun crearServicio(servicio: Servicio) =
            servicio.copy(id = servicios.size + 1).also { servicios.add(it) }
        override suspend fun actualizarServicio(servicio: Servicio) = servicio
        override suspend fun eliminarServicio(id: Int) {
            servicios.removeAll { it.id == id }
        }
    }

    private class HorariosEnMemoria : HorarioRepository {
        val horarios = mutableListOf<Horario>()
        override suspend fun obtenerHorarios() = horarios.toList()
        override suspend fun crearHorario(fecha: LocalDate, inicio: LocalTime, fin: LocalTime) =
            Horario(horarios.size + 1, fecha, inicio, fin).also { horarios.add(it) }
        override suspend fun eliminarHorario(id: Int) {
            horarios.removeAll { it.id == id }
        }
    }

    private val citaPendiente = Cita(
        1, Paciente("Luis", "luis@correo.com", ""), "Resina",
        LocalDate.of(2026, 12, 1), LocalTime.of(9, 0), "", EstatusCita.PENDIENTE
    )

    // ---------- ResponderCitaUseCase ----------

    @Test
    fun `aceptar una cita pendiente la actualiza en el repositorio`() = runTest {
        val repo = CitasEnMemoria(citaPendiente)
        val resultado = ResponderCitaUseCase(repo)(1, EstatusCita.ACEPTADA)
        assertEquals(EstatusCita.ACEPTADA, resultado.estatus)
        assertEquals(1, repo.actualizaciones)
    }

    @Test
    fun `responder una cita ya respondida no llama al repositorio`() = runTest {
        val repo = CitasEnMemoria(citaPendiente.copy(estatus = EstatusCita.RECHAZADA))
        val error = runCatching { ResponderCitaUseCase(repo)(1, EstatusCita.ACEPTADA) }.exceptionOrNull()
        assertTrue(error is ReglaNegocioException)
        assertEquals(0, repo.actualizaciones)
    }

    // ---------- GuardarServicioUseCase ----------

    @Test
    fun `un servicio valido se crea con id`() = runTest {
        val repo = ServiciosEnMemoria()
        val creado = GuardarServicioUseCase(repo)(null, "Limpieza", "Profilaxis", 600.0, true)
        assertNotNull(creado.id)
        assertEquals(1, repo.servicios.size)
    }

    @Test
    fun `un servicio sin nombre es rechazado`() = runTest {
        val error = runCatching {
            GuardarServicioUseCase(ServiciosEnMemoria())(null, "  ", "", 600.0, true)
        }.exceptionOrNull()
        assertTrue(error is ReglaNegocioException)
    }

    @Test
    fun `un servicio con costo invalido es rechazado`() = runTest {
        val repo = ServiciosEnMemoria()
        val sinCosto = runCatching { GuardarServicioUseCase(repo)(null, "Resina", "", null, true) }
        val costoCero = runCatching { GuardarServicioUseCase(repo)(null, "Resina", "", 0.0, true) }
        assertTrue(sinCosto.exceptionOrNull() is ReglaNegocioException)
        assertTrue(costoCero.exceptionOrNull() is ReglaNegocioException)
    }

    // ---------- CrearHorarioUseCase ----------

    private val hoy = LocalDate.of(2026, 11, 18)

    @Test
    fun `un horario valido se guarda`() = runTest {
        val repo = HorariosEnMemoria()
        val crear = CrearHorarioUseCase(repo, hoy = { hoy })
        crear("2026-11-20", "9:00", "13:00")
        assertEquals(1, repo.horarios.size)
    }

    @Test
    fun `un horario con formato incorrecto es rechazado`() = runTest {
        val error = runCatching {
            val crear = CrearHorarioUseCase(HorariosEnMemoria(), hoy = { hoy })
            crear("20/11/2026", "09:00", "13:00")
        }.exceptionOrNull()
        assertTrue(error is ReglaNegocioException)
    }

    @Test
    fun `un horario empalmado no se guarda`() = runTest {
        val repo = HorariosEnMemoria()
        val crear = CrearHorarioUseCase(repo, hoy = { hoy })
        crear("2026-11-20", "09:00", "13:00")
        val error = runCatching { crear("2026-11-20", "12:00", "14:00") }.exceptionOrNull()
        assertTrue(error is ReglaNegocioException)
        assertEquals(1, repo.horarios.size)
    }
}
