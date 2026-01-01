package com.codefm.aquameter.service

import com.codefm.aquameter.api.RetrofitClient
import com.codefm.aquameter.model.UserSession

/**
 * Servicio de autenticación para validar credenciales de usuario
 */
class AuthService {

    private val apiService = RetrofitClient.apiService

    /**
     * Valida las credenciales de login contra la API
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si las credenciales son válidas y guarda la sesión, false en caso contrario
     */
    suspend fun validateLogin(username: String, password: String): Boolean {
        return try {
            val response = apiService.authenticate(username, password)

            if (response.isSuccessful && response.body() != null) {
                // Autenticación exitosa - guardar sesión
                val authResponse = response.body()!!
                UserSession.login(authResponse)
                true
            } else {
                // Autenticación fallida (HTTP 200 con body "null" o error)
                UserSession.logout()
                false
            }
        } catch (e: Exception) {
            // Error de red u otro error
            e.printStackTrace()
            UserSession.logout()
            false
        }
    }
}

