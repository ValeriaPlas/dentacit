package com.dentacit.admin.data.repository

import com.dentacit.admin.data.local.SesionStore
import com.dentacit.admin.data.remote.DentaCitApi
import com.dentacit.admin.data.remote.dto.EstatusRequest
import com.dentacit.admin.data.remote.dto.HorarioRequest
import com.dentacit.admin.data.remote.dto.LoginRequest
import com.dentacit.admin.data.remote.dto.aDominio
import com.dentacit.admin.data.remote.dto.aDto
import com.dentacit.admin.data.remote.llamarApi
import com.dentacit.admin.data.remote.verificar
import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.Servicio
import com.dentacit.admin.domain.model.Sesion
import com.dentacit.admin.domain.repository.AuthRepository
import com.dentacit.admin.domain.repository.CitaRepository
import com.dentacit.admin.domain.repository.HorarioRepository
import com.dentacit.admin.domain.repository.ServicioRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Implementación de [AuthRepository] contra auth-service. */
class AuthRepositoryImpl(
    private val api: DentaCitApi,
    private val sesionStore: SesionStore
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, password: String): Sesion = llamarApi {
        val respuesta = api.login(LoginRequest(correo, password))
        Sesion(respuesta.token, respuesta.nombre ?: "Dentista").also { sesionStore.guardar(it) }
    }

    override suspend fun cerrarSesion() = sesionStore.borrar()

    override fun sesionActiva(): Flow<Sesion?> = sesionStore.sesion
}

/** Implementación de [CitaRepository] contra citas-service. */
class CitaRepositoryImpl(private val api: DentaCitApi) : CitaRepository {

    override suspend fun obtenerCitas(estatus: EstatusCita?): List<Cita> = llamarApi {
        api.obtenerCitas(estatus?.etiqueta).map { it.aDominio() }
    }

    override suspend fun obtenerCita(id: Int): Cita = llamarApi {
        api.obtenerCita(id).aDominio()
    }

    override suspend fun actualizarEstatus(id: Int, estatus: EstatusCita) = llamarApi {
        api.actualizarEstatus(id, EstatusRequest(estatus.etiqueta)).verificar()
    }
}

/** Implementación de [ServicioRepository] contra catalogo-service. */
class ServicioRepositoryImpl(private val api: DentaCitApi) : ServicioRepository {

    override suspend fun obtenerServicios(): List<Servicio> = llamarApi {
        api.obtenerServicios().map { it.aDominio() }
    }

    override suspend fun crearServicio(servicio: Servicio): Servicio = llamarApi {
        api.crearServicio(servicio.aDto()).aDominio()
    }

    override suspend fun actualizarServicio(servicio: Servicio): Servicio = llamarApi {
        api.actualizarServicio(requireNotNull(servicio.id), servicio.aDto()).aDominio()
    }

    override suspend fun eliminarServicio(id: Int) = llamarApi {
        api.eliminarServicio(id).verificar()
    }
}

/** Implementación de [HorarioRepository] contra citas-service. */
class HorarioRepositoryImpl(private val api: DentaCitApi) : HorarioRepository {

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun obtenerHorarios(): List<Horario> = llamarApi {
        api.obtenerHorarios().map { it.aDominio() }
    }

    override suspend fun crearHorario(fecha: LocalDate, inicio: LocalTime, fin: LocalTime): Horario =
        llamarApi {
            api.crearHorario(
                HorarioRequest(fecha.toString(), inicio.format(formatoHora), fin.format(formatoHora))
            ).aDominio()
        }

    override suspend fun eliminarHorario(id: Int) = llamarApi {
        api.eliminarHorario(id).verificar()
    }
}
