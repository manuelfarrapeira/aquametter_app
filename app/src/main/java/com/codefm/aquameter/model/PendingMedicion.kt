package com.codefm.aquameter.model

/**
 * Modelo de datos para mediciones pendientes de enviar
 */
data class PendingMedicion(
    val idContador: String,
    val litros: String,
    val nota: String,
    val foto: String,
    val fecha: String,
    val timestamp: Long = System.currentTimeMillis()
)

