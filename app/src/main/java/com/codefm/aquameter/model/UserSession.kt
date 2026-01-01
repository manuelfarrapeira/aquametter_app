package com.codefm.aquameter.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Clase singleton para gestionar la sesión del usuario en toda la aplicación.
 * Almacena los datos del usuario autenticado para uso en futuras peticiones.
 * Persiste los datos en SharedPreferences para mantenerlos entre reinicios de la app.
 */
object UserSession {

    private const val PREF_NAME = "aquameter_session"
    private const val KEY_ID_TRAIDA = "id_traida"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_ID_USUARIO = "id_usuario"

    private var prefs: SharedPreferences? = null

    private var _idTraida: String? = null
    private var _nombre: String? = null
    private var _idUsuario: String? = null

    /**
     * Inicializa el UserSession con el contexto de la aplicación
     * Debe llamarse al inicio de la aplicación
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadFromPreferences()
    }

    /**
     * Carga los datos desde SharedPreferences
     */
    private fun loadFromPreferences() {
        prefs?.let {
            _idTraida = it.getString(KEY_ID_TRAIDA, null)
            _nombre = it.getString(KEY_NOMBRE, null)
            _idUsuario = it.getString(KEY_ID_USUARIO, null)
        }
    }

    /**
     * Guarda los datos en SharedPreferences
     */
    private fun saveToPreferences() {
        prefs?.edit {
            putString(KEY_ID_TRAIDA, _idTraida)
            putString(KEY_NOMBRE, _nombre)
            putString(KEY_ID_USUARIO, _idUsuario)
        }
    }

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
        saveToPreferences()
    }

    /**
     * Cierra la sesión y limpia todos los datos
     */
    fun logout() {
        _idTraida = null
        _nombre = null
        _idUsuario = null
        prefs?.edit { clear() }
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

