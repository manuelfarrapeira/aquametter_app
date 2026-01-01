package com.codefm.aquameter.model

/**
 * Clase singleton para gestionar la sesión del usuario en toda la aplicación.
 * Almacena los datos del usuario autenticado para uso en futuras peticiones.
 */
object UserSession {

    private var _idTraida: String? = null
    private var _nombre: String? = null
    private var _idUsuario: String? = null

    /**
     * ID de la traída del usuario autenticado
     */
    val idTraida: String?
        get() = _idTraida

    /**
     * Nombre de la traída del usuario autenticado
     */
    val nombre: String?
        get() = _nombre

    /**
     * ID del usuario autenticado
     */
    val idUsuario: String?
        get() = _idUsuario

    /**
     * Indica si hay una sesión activa
     */
    val isLoggedIn: Boolean
        get() = _idTraida != null && _idUsuario != null

    /**
     * Inicia sesión con los datos del usuario
     */
    fun login(authResponse: AuthResponse) {
        _idTraida = authResponse.idTraida
        _nombre = authResponse.nombre
        _idUsuario = authResponse.id
    }

    /**
     * Cierra la sesión y limpia todos los datos
     */
    fun logout() {
        _idTraida = null
        _nombre = null
        _idUsuario = null
    }

    /**
     * Obtiene una representación legible de la sesión actual
     */
    override fun toString(): String {
        return if (isLoggedIn) {
            "UserSession(idTraida=$_idTraida, nombre=$_nombre, idUsuario=$_idUsuario)"
        } else {
            "UserSession(not logged in)"
        }
    }
}

