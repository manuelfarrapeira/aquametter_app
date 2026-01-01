package com.codefm.aquameter.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para un contador de agua
 */
data class Contador(
    @SerializedName("id")
    val id: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("codigo_contador")
    val codigoContador: String,

    @SerializedName("usuario_anterior")
    val usuarioAnterior: String,

    @SerializedName("litros")
    val litros: String,

    @SerializedName("unidad_familiar")
    val unidadFamiliar: String,

    @SerializedName("unidad")
    val unidad: String,

    @SerializedName("ultima_lectura")
    val ultimaLectura: String,

    @SerializedName("fecha_lectura")
    val fechaLectura: String,

    @SerializedName("id_last_lectura")
    val idLastLectura: String,

    @SerializedName("penultima_lectura")
    val penultimaLectura: String,

    @SerializedName("penultima_fecha_lectura")
    val penultimaFechaLectura: String
) {
    /**
     * Verifica si la fecha de lectura es hoy
     * Formato esperado: dd/MM/yyyy
     */
    fun isToday(): Boolean {
        return try {
            val parts = fechaLectura.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()

                val calendar = java.util.Calendar.getInstance()
                val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH es base 0
                val currentYear = calendar.get(java.util.Calendar.YEAR)

                day == currentDay && month == currentMonth && year == currentYear
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

