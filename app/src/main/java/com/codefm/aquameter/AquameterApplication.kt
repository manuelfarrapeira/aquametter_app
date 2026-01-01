package com.codefm.aquameter

import android.app.Application
import com.codefm.aquameter.model.UserSession
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application para inicializar Hilt y componentes globales
 */
@HiltAndroidApp
class AquameterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializar sesión de usuario
        UserSession.init(this)
    }
}

