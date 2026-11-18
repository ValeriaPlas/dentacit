package com.dentacit.admin.di

import android.content.Context
import com.dentacit.admin.BuildConfig
import com.dentacit.admin.data.fake.AuthRepositoryDemo
import com.dentacit.admin.data.fake.CitaRepositoryDemo
import com.dentacit.admin.data.fake.HorarioRepositoryDemo
import com.dentacit.admin.data.fake.ServicioRepositoryDemo
import com.dentacit.admin.data.local.SesionStore
import com.dentacit.admin.data.remote.DentaCitApi
import com.dentacit.admin.data.remote.crearApi
import com.dentacit.admin.data.repository.AuthRepositoryImpl
import com.dentacit.admin.data.repository.CitaRepositoryImpl
import com.dentacit.admin.data.repository.HorarioRepositoryImpl
import com.dentacit.admin.data.repository.ServicioRepositoryImpl
import com.dentacit.admin.domain.repository.AuthRepository
import com.dentacit.admin.domain.repository.CitaRepository
import com.dentacit.admin.domain.repository.HorarioRepository
import com.dentacit.admin.domain.repository.ServicioRepository
import com.dentacit.admin.domain.usecase.CerrarSesionUseCase
import com.dentacit.admin.domain.usecase.CrearHorarioUseCase
import com.dentacit.admin.domain.usecase.EliminarHorarioUseCase
import com.dentacit.admin.domain.usecase.EliminarServicioUseCase
import com.dentacit.admin.domain.usecase.GuardarServicioUseCase
import com.dentacit.admin.domain.usecase.IniciarSesionUseCase
import com.dentacit.admin.domain.usecase.ObtenerCitaUseCase
import com.dentacit.admin.domain.usecase.ObtenerHorariosUseCase
import com.dentacit.admin.domain.usecase.ObtenerServiciosUseCase
import com.dentacit.admin.domain.usecase.ObtenerSolicitudesUseCase
import com.dentacit.admin.domain.usecase.ResponderCitaUseCase

/**
 * Contenedor de inyección de dependencias manual.
 *
 * Decide si la app usa los repositorios remotos (API REST) o los de demostración
 * según [BuildConfig.USE_FAKE_API], y expone los casos de uso a los ViewModels.
 *
 * @param context contexto de la aplicación.
 */
class AppContainer(context: Context) {

    /** Almacén de la sesión del dentista. */
    val sesionStore = SesionStore(context.applicationContext)

    private val api: DentaCitApi by lazy { crearApi(sesionStore) }

    private val authRepository: AuthRepository =
        if (BuildConfig.USE_FAKE_API) AuthRepositoryDemo(sesionStore) else AuthRepositoryImpl(api, sesionStore)

    private val citaRepository: CitaRepository =
        if (BuildConfig.USE_FAKE_API) CitaRepositoryDemo() else CitaRepositoryImpl(api)

    private val servicioRepository: ServicioRepository =
        if (BuildConfig.USE_FAKE_API) ServicioRepositoryDemo() else ServicioRepositoryImpl(api)

    private val horarioRepository: HorarioRepository =
        if (BuildConfig.USE_FAKE_API) HorarioRepositoryDemo() else HorarioRepositoryImpl(api)

    val iniciarSesion = IniciarSesionUseCase(authRepository)
    val cerrarSesion = CerrarSesionUseCase(authRepository)
    val obtenerSolicitudes = ObtenerSolicitudesUseCase(citaRepository)
    val obtenerCita = ObtenerCitaUseCase(citaRepository)
    val responderCita = ResponderCitaUseCase(citaRepository)
    val obtenerServicios = ObtenerServiciosUseCase(servicioRepository)
    val guardarServicio = GuardarServicioUseCase(servicioRepository)
    val eliminarServicio = EliminarServicioUseCase(servicioRepository)
    val obtenerHorarios = ObtenerHorariosUseCase(horarioRepository)
    val crearHorario = CrearHorarioUseCase(horarioRepository)
    val eliminarHorario = EliminarHorarioUseCase(horarioRepository)
}
