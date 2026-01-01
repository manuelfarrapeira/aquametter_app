package com.codefm.aquameter.api

import com.codefm.aquameter.model.AuthResponse
import com.codefm.aquameter.model.Contador
import okhttp3.ResponseBody
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

    /**
     * Obtener contadores de una traída
     * @param idTraida ID de la traída
     * @return Lista de contadores
     */
    @GET("getContadores/{id_traida}")
    suspend fun getContadores(
        @Path("id_traida") idTraida: String
    ): Response<List<Contador>>

    /**
     * Eliminar una lectura
     * @param idLectura ID de la lectura a eliminar
     * @return Response con texto plano (esperado: "OK")
     */
    @GET("deleteLectura/{id_lectura}")
    suspend fun deleteLectura(
        @Path("id_lectura") idLectura: String
    ): Response<ResponseBody>
}

