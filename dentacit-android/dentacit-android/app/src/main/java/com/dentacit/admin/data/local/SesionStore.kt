package com.dentacit.admin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dentacit.admin.domain.model.Sesion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.dataStore by preferencesDataStore(name = "sesion_dentacit")

/**
 * Guarda la sesión del dentista en el dispositivo con DataStore.
 *
 * Mantiene además una copia del token en memoria para que el interceptor HTTP
 * pueda leerlo sin bloquear el hilo.
 *
 * @param context contexto de la aplicación.
 */
class SesionStore(private val context: Context) {

    /** Token actual en memoria; `null` si no hay sesión. */
    @Volatile
    var tokenEnMemoria: String? = null
        private set

    /** Flujo con la sesión guardada o `null`. */
    val sesion: Flow<Sesion?> = context.dataStore.data
        .map { preferencias ->
            val token = preferencias[CLAVE_TOKEN]
            if (token == null) null else Sesion(token, preferencias[CLAVE_NOMBRE] ?: "")
        }
        .onEach { tokenEnMemoria = it?.token }

    /**
     * Guarda una sesión nueva.
     *
     * @param sesion sesión devuelta por auth-service.
     */
    suspend fun guardar(sesion: Sesion) {
        tokenEnMemoria = sesion.token
        context.dataStore.edit { preferencias ->
            preferencias[CLAVE_TOKEN] = sesion.token
            preferencias[CLAVE_NOMBRE] = sesion.nombre
        }
    }

    /** Borra la sesión guardada. */
    suspend fun borrar() {
        tokenEnMemoria = null
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val CLAVE_TOKEN = stringPreferencesKey("token")
        val CLAVE_NOMBRE = stringPreferencesKey("nombre")
    }
}
