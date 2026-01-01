package com.codefm.aquameter.api

import com.codefm.aquameter.model.AuthResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interfaz de API para comunicación con el servidor
 */
interface ApiService {

    /**
     * Autenticar usuario
     * @param username nombre de usuario
     * @param password contraseña
     * @return Response con AuthResponse si es exitoso, o cuerpo "null" si falla
     */
    @GET("Authenticate/{username}/{password}")
    suspend fun authenticate(
        @Path("username") username: String,
        @Path("password") password: String
    ): Response<AuthResponse>
}

