package com.dentacit.admin.data.fake

import com.dentacit.admin.data.local.SesionStore
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.ErrorServidorException
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.NoAutorizadoException
import com.dentacit.admin.domain.model.Paciente
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.domain.model.Sesion
import com.dentacit.admin.domain.repository.AuthRepository
import com.dentacit.admin.domain.repository.CitaRepository
import com.dentacit.admin.domain.repository.HorarioRepository
import com.dentacit.admin.domain.repository.ServicioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Retardo simulado de red en el modo demostración. */
private const val RETARDO_MS = 400L

/**
 * Datos en memoria para el modo demostración ([com.dentacit.admin.BuildConfig.USE_FAKE_API]).
 * Permiten probar y presentar la app sin tener los microservicios levantados.
 */
object DatosDemo {
    const val CORREO = "dentista@dentacit.com"
    const val PASSWORD = "dentacit123"

    private val hoy = LocalDate.now()

    val servicios = mutableListOf(
        Servicio(1, "Limpieza dental", "Profilaxis y eliminación de sarro", 600.0),
        Servicio(2, "Resina", "Restauración estética de caries", 850.0),
        Servicio(3, "Extracción simple", "Extracción de pieza sin cirugía", 900.0),
        Servicio(4, "Blanqueamiento", "Blanqueamiento en consultorio", 3500.0),
        Servicio(5, "Valoración", "Revisión general y diagnóstico", 300.0)
    )

    val citas = mutableListOf(
        Cita(1, Paciente("Ana López García", "ana.lopez@correo.com", "477 123 4567"),
            "Limpieza dental", hoy.plusDays(1), LocalTime.of(10, 0),
            "Sangrado leve de encías al cepillarme.", EstatusCita.PENDIENTE),
        Cita(2, Paciente("Luis Hernández Ruiz", "luis.hr@correo.com", "477 987 6543"),
            "Extracción simple", hoy.plusDays(1), LocalTime.of(12, 0),
            "Dolor fuerte en muela del juicio inferior derecha.", EstatusCita.PENDIENTE),
        Cita(3, Paciente("María Torres Díaz", "maria.td@correo.com", "462 555 0101"),
            "Resina", hoy.plusDays(2), LocalTime.of(9, 0),
            "Sensibilidad al frío en un diente frontal.", EstatusCita.PENDIENTE),
        Cita(4, Paciente("Jorge Ramírez Soto", "jorge.rs@correo.com", "477 222 3344"),
            "Valoración", hoy.plusDays(3), LocalTime.of(16, 0),
            "Revisión general, hace dos años que no voy al dentista.", EstatusCita.ACEPTADA),
        Cita(5, Paciente("Sofía Mendoza Cruz", "sofia.mc@correo.com", "477 444 8899"),
            "Blanqueamiento", hoy.plusDays(4), LocalTime.of(11, 0),
            "Quiero mejorar el color de mis dientes.", EstatusCita.RECHAZADA)
    )

    val horarios = mutableListOf(
        Horario(1, hoy.plusDays(1), LocalTime.of(9, 0), LocalTime.of(14, 0)),
        Horario(2, hoy.plusDays(1), LocalTime.of(16, 0), LocalTime.of(19, 0)),
        Horario(3, hoy.plusDays(2), LocalTime.of(9, 0), LocalTime.of(13, 0))
    )

    private var siguienteId = 100

    /** Genera un identificador nuevo para registros creados en la demo. */
    fun nuevoId(): Int = siguienteId++
}

/** [AuthRepository] de demostración: acepta solo las credenciales de [DatosDemo]. */
class AuthRepositoryDemo(private val sesionStore: SesionStore) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, password: String): Sesion {
        delay(RETARDO_MS)
        if (correo != DatosDemo.CORREO || password != DatosDemo.PASSWORD) {
            throw NoAutorizadoException()
        }
        return Sesion("token-demo", "Dentista DentaCit").also { sesionStore.guardar(it) }
    }

    override suspend fun cerrarSesion() = sesionStore.borrar()

    override fun sesionActiva(): Flow<Sesion?> = sesionStore.sesion
}

/** [CitaRepository] de demostración sobre [DatosDemo.citas]. */
class CitaRepositoryDemo : CitaRepository {

    override suspend fun obtenerCitas(estatus: EstatusCita?): List<Cita> {
        delay(RETARDO_MS)
        return DatosDemo.citas.filter { estatus == null || it.estatus == estatus }
    }

    override suspend fun obtenerCita(id: Int): Cita {
        delay(RETARDO_MS)
        return DatosDemo.citas.firstOrNull { it.id == id }
            ?: throw ErrorServidorException("La cita $id no existe.")
    }

    override suspend fun actualizarEstatus(id: Int, estatus: EstatusCita) {
        delay(RETARDO_MS)
        val indice = DatosDemo.citas.indexOfFirst { it.id == id }
        if (indice < 0) throw ErrorServidorException("La cita $id no existe.")
        DatosDemo.citas[indice] = DatosDemo.citas[indice].copy(estatus = estatus)
    }
}

/** [ServicioRepository] de demostración sobre [DatosDemo.servicios]. */
class ServicioRepositoryDemo : ServicioRepository {

    override suspend fun obtenerServicios(): List<Servicio> {
        delay(RETARDO_MS)
        return DatosDemo.servicios.toList()
    }

    override suspend fun crearServicio(servicio: Servicio): Servicio {
        delay(RETARDO_MS)
        val nuevo = servicio.copy(id = DatosDemo.nuevoId())
        DatosDemo.servicios.add(nuevo)
        return nuevo
    }

    override suspend fun actualizarServicio(servicio: Servicio): Servicio {
        delay(RETARDO_MS)
        val indice = DatosDemo.servicios.indexOfFirst { it.id == servicio.id }
        if (indice < 0) throw ErrorServidorException("El servicio no existe.")
        DatosDemo.servicios[indice] = servicio
        return servicio
    }

    override suspend fun eliminarServicio(id: Int) {
        delay(RETARDO_MS)
        DatosDemo.servicios.removeAll { it.id == id }
    }
}

/** [HorarioRepository] de demostración sobre [DatosDemo.horarios]. */
class HorarioRepositoryDemo : HorarioRepository {

    override suspend fun obtenerHorarios(): List<Horario> {
        delay(RETARDO_MS)
        return DatosDemo.horarios.toList()
    }

    override suspend fun crearHorario(fecha: LocalDate, inicio: LocalTime, fin: LocalTime): Horario {
        delay(RETARDO_MS)
        val nuevo = Horario(DatosDemo.nuevoId(), fecha, inicio, fin)
        DatosDemo.horarios.add(nuevo)
        return nuevo
    }

    override suspend fun eliminarHorario(id: Int) {
        delay(RETARDO_MS)
        DatosDemo.horarios.removeAll { it.id == id }
    }
}
