package com.codefm.aquameter.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de respuesta de autenticación de la API
 * Representa el JSON: {"id_traida":"1","nombre":"Traida De Prueba","id":"1"}
 */
data class AuthResponse(
    @SerializedName("id_traida")
    val idTraida: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("id")
    val id: String
)

