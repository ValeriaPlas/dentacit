package com.dentacit.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Cuerpo de `POST auth/login`. */
data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

/** Respuesta de `POST auth/login`. */
data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("nombre") val nombre: String?
)

/** Cita tal como la devuelve citas-service. */
data class CitaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("paciente_nombre") val pacienteNombre: String?,
    @SerializedName("paciente_correo") val pacienteCorreo: String?,
    @SerializedName("paciente_telefono") val pacienteTelefono: String?,
    @SerializedName("servicio") val servicio: String?,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora") val hora: String,
    @SerializedName("sintomas") val sintomas: String?,
    @SerializedName("estatus") val estatus: String
)

/** Cuerpo de `PATCH citas/{id}/estatus`. */
data class EstatusRequest(
    @SerializedName("estatus") val estatus: String
)

/** Servicio tal como lo maneja catalogo-service. */
data class ServicioDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("costo") val costo: Double,
    @SerializedName("activo") val activo: Boolean?
)

/** Bloque de horario tal como lo devuelve citas-service. */
data class HorarioDto(
    @SerializedName("id") val id: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String
)

/** Cuerpo de `POST horarios`. */
data class HorarioRequest(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String
)
