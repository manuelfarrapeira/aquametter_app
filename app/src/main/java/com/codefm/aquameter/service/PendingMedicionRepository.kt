package com.codefm.aquameter.service

import android.content.Context
import android.content.SharedPreferences
import com.codefm.aquameter.model.PendingMedicion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar mediciones pendientes en SharedPreferences
 */
@Singleton
class PendingMedicionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "pending_mediciones"
        private const val KEY_MEDICIONES = "mediciones"
    }

    /**
     * Guarda una medición pendiente
     */
    fun savePendingMedicion(medicion: PendingMedicion) {
        val mediciones = getAllPendingMediciones().toMutableList()

        // Eliminar medición anterior del mismo contador si existe
        mediciones.removeAll { it.idContador == medicion.idContador }

        // Agregar nueva medición
        mediciones.add(medicion)

        // Guardar en SharedPreferences
        val json = gson.toJson(mediciones)
        prefs.edit().putString(KEY_MEDICIONES, json).apply()
    }

    /**
     * Obtiene todas las mediciones pendientes
     */
    fun getAllPendingMediciones(): List<PendingMedicion> {
        val json = prefs.getString(KEY_MEDICIONES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingMedicion>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Obtiene una medición pendiente por ID de contador
     */
    fun getPendingMedicion(idContador: String): PendingMedicion? {
        return getAllPendingMediciones().find { it.idContador == idContador }
    }

    /**
     * Verifica si un contador tiene medición pendiente
     */
    fun hasPendingMedicion(idContador: String): Boolean {
        return getPendingMedicion(idContador) != null
    }

    /**
     * Elimina una medición pendiente
     */
    fun deletePendingMedicion(idContador: String) {
        val mediciones = getAllPendingMediciones().toMutableList()
        mediciones.removeAll { it.idContador == idContador }

        val json = gson.toJson(mediciones)
        prefs.edit().putString(KEY_MEDICIONES, json).apply()
    }

    /**
     * Limpia todas las mediciones pendientes
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

