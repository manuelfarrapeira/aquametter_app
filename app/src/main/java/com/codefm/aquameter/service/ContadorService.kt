package com.codefm.aquameter.service

import com.codefm.aquameter.api.ApiService
import com.codefm.aquameter.model.Contador
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para obtener contadores desde la API
 */
@Singleton
class ContadorService @Inject constructor(private val apiService: ApiService) {

    /**
     * Obtiene la lista de contadores para una traida
     * @param idTraida ID de la traída
     * @return Lista de contadores o null si hay error
     */
    suspend fun getContadores(idTraida: String): List<Contador>? {
        return try {
            val response = apiService.getContadores(idTraida)

            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

