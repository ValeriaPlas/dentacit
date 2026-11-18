package com.dentacit.admin.data.remote

import com.dentacit.admin.data.remote.dto.CitaDto
import com.dentacit.admin.data.remote.dto.EstatusRequest
import com.dentacit.admin.data.remote.dto.HorarioDto
import com.dentacit.admin.data.remote.dto.HorarioRequest
import com.dentacit.admin.data.remote.dto.LoginRequest
import com.dentacit.admin.data.remote.dto.LoginResponse
import com.dentacit.admin.data.remote.dto.ServicioDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Definición Retrofit de los endpoints del API Gateway que usa la app del dentista.
 * El contrato completo está documentado en el README del proyecto.
 */
interface DentaCitApi {

    // ---------- auth-service ----------
    @POST("auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): LoginResponse

    // ---------- citas-service ----------
    @GET("citas")
    suspend fun obtenerCitas(@Query("estatus") estatus: String?): List<CitaDto>

    @GET("citas/{id}")
    suspend fun obtenerCita(@Path("id") id: Int): CitaDto

    @PATCH("citas/{id}/estatus")
    suspend fun actualizarEstatus(@Path("id") id: Int, @Body cuerpo: EstatusRequest): Response<Unit>

    @GET("horarios")
    suspend fun obtenerHorarios(): List<HorarioDto>

    @POST("horarios")
    suspend fun crearHorario(@Body cuerpo: HorarioRequest): HorarioDto

    @DELETE("horarios/{id}")
    suspend fun eliminarHorario(@Path("id") id: Int): Response<Unit>

    // ---------- catalogo-service ----------
    @GET("servicios")
    suspend fun obtenerServicios(): List<ServicioDto>

    @POST("servicios")
    suspend fun crearServicio(@Body cuerpo: ServicioDto): ServicioDto

    @PUT("servicios/{id}")
    suspend fun actualizarServicio(@Path("id") id: Int, @Body cuerpo: ServicioDto): ServicioDto

    @DELETE("servicios/{id}")
    suspend fun eliminarServicio(@Path("id") id: Int): Response<Unit>
}
