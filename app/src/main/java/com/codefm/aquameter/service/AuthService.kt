package com.codefm.aquameter.service

/**
 * Servicio de autenticación para validar credenciales de usuario
 */
class AuthService {

    /**
     * Valida las credenciales de login
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si las credenciales son válidas (admin/admin), false en caso contrario
     */
    fun validateLogin(username: String, password: String): Boolean {
        return username == "admin" && password == "admin"
    }
}

