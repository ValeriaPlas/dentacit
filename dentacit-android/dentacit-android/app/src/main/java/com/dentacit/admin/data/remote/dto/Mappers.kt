package com.dentacit.admin.data.remote.dto

import com.dentacit.admin.domain.model.Cita
import com.dentacit.admin.domain.model.EstatusCita
import com.dentacit.admin.domain.model.Horario
import com.dentacit.admin.domain.model.Paciente
import com.dentacit.admin.domain.model.Servicio
import java.time.LocalDate
import java.time.LocalTime

/** Convierte el DTO de cita en la entidad de dominio. */
fun CitaDto.aDominio(): Cita = Cita(
    id = id,
    paciente = Paciente(pacienteNombre ?: "", pacienteCorreo ?: "", pacienteTelefono ?: ""),
    servicio = servicio ?: "",
    fecha = LocalDate.parse(fecha),
    hora = LocalTime.parse(hora),
    sintomas = sintomas ?: "",
    estatus = EstatusCita.desdeTexto(estatus)
)

/** Convierte el DTO de servicio en la entidad de dominio. */
fun ServicioDto.aDominio(): Servicio =
    Servicio(id, nombre, descripcion ?: "", costo, activo ?: true)

/** Convierte la entidad de servicio en el DTO que espera catalogo-service. */
fun Servicio.aDto(): ServicioDto =
    ServicioDto(id, nombre, descripcion, costo, activo)

/** Convierte el DTO de horario en la entidad de dominio. */
fun HorarioDto.aDominio(): Horario =
    Horario(id, LocalDate.parse(fecha), LocalTime.parse(horaInicio), LocalTime.parse(horaFin))
