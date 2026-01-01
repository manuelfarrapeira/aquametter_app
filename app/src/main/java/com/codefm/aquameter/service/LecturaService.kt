package com.codefm.aquameter.service

import com.codefm.aquameter.api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para gestionar lecturas
 */
@Singleton
class LecturaService @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Elimina una lectura
     * @param idLectura ID de la lectura a eliminar
     * @return true si se eliminó correctamente (HTTP 200 con respuesta "OK"), false en caso contrario
     */
    suspend fun deleteLectura(idLectura: String): Boolean {
        return try {
            val response = apiService.deleteLectura(idLectura)
            // Verificar que sea HTTP 200 y que el cuerpo de la respuesta sea "OK"
            if (response.isSuccessful && response.body() != null) {
                val responseText = response.body()?.string()?.trim() ?: ""
                responseText.equals("OK", ignoreCase = true)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

