package com.dentacit.admin.data.remote

import com.dentacit.admin.BuildConfig
import com.dentacit.admin.data.local.SesionStore
import com.dentacit.admin.domain.model.ErrorConexionException
import com.dentacit.admin.domain.model.ErrorServidorException
import com.dentacit.admin.domain.model.NoAutorizadoException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Agrega el encabezado `Authorization: Bearer <token>` a cada petición
 * cuando el dentista tiene sesión iniciada.
 *
 * @param sesionStore almacén de la sesión.
 */
class AuthInterceptor(private val sesionStore: SesionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sesionStore.tokenEnMemoria
        val peticion = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(peticion)
    }
}

/**
 * Construye la instancia de [DentaCitApi] apuntando a [BuildConfig.BASE_URL].
 *
 * @param sesionStore almacén de la sesión, usado por el interceptor JWT.
 */
fun crearApi(sesionStore: SesionStore): DentaCitApi {
    val registro = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    val cliente = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sesionStore))
        .addInterceptor(registro)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DentaCitApi::class.java)
}

/**
 * Ejecuta una llamada al API y traduce los errores técnicos a excepciones de dominio.
 *
 * @param bloque llamada Retrofit a ejecutar.
 * @return el resultado de la llamada.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): T {
    try {
        return bloque()
    } catch (e: HttpException) {
        if (e.code() == 401 || e.code() == 403) throw NoAutorizadoException()
        throw ErrorServidorException(
            extraerMensaje(e) ?: "El servidor respondió con un error (${e.code()})."
        )
    } catch (e: IOException) {
        throw ErrorConexionException()
    }
}

/** Lanza [HttpException] si una respuesta sin cuerpo no fue exitosa. */
fun retrofit2.Response<Unit>.verificar() {
    if (!isSuccessful) throw HttpException(this)
}

/** Intenta leer el campo `msg` del JSON de error que devuelven los servicios. */
private fun extraerMensaje(e: HttpException): String? = try {
    val cuerpo = e.response()?.errorBody()?.string()
    if (cuerpo.isNullOrBlank()) null else JSONObject(cuerpo).optString("msg").ifBlank { null }
} catch (ignorado: Exception) {
    null
}
