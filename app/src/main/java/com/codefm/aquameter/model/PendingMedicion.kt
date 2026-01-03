package com.codefm.aquameter.model

/**
 * Modelo de datos para mediciones pendientes de enviar
 * La foto ahora es una ruta al archivo físico en el dispositivo
 */
data class PendingMedicion(
    val idContador: String,
    val litros: String,
    val nota: String,
    val fotoPath: String, // Ruta al archivo físico
    val fecha: String,
    val timestamp: Long = System.currentTimeMillis()
)

