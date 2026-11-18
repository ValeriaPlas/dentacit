package com.dentacit.admin

import android.app.Application
import com.dentacit.admin.di.AppContainer

/** Clase Application: crea el contenedor de dependencias una sola vez. */
class DentaCitApp : Application() {

    /** Contenedor de dependencias compartido por toda la app. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
