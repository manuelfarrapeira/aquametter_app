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

    /**
     * Calcula los días entre la penúltima y última lectura
     */
    private fun getDiasPenultima(): Int {
        return try {
            val ultimaDate = parseDateFromString(fechaLectura)
            val penultimaDate = parseDateFromString(penultimaFechaLectura)

            if (ultimaDate != null && penultimaDate != null) {
                val diffMillis = ultimaDate.time - penultimaDate.time
                val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                if (days > 0) days else 1
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Parsea fecha en formato dd/MM/yyyy
     */
    private fun parseDateFromString(dateStr: String): java.util.Date? {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1 // Calendar.MONTH es base 0
                val year = parts[2].toInt()

                val calendar = java.util.Calendar.getInstance()
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.time
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Redondea un número a n decimales
     */
    private fun round(value: Double, decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(value * multiplier) / multiplier
    }

    /**
     * Obtiene los litros por día
     */
    private fun getLitrosDia(): Double {
        return litros.toDoubleOrNull() ?: 0.0
    }

    /**
     * Calcula el exceso de la última lectura
     * @return exceso > 0: consumo excedido (rojo)
     *         exceso < 0: lectura negativa o consumo bajo (azul)
     *         exceso = 0: consumo exacto (amarillo)
     */
    fun getLastExceso(): Double {
        val dias = getDiasPenultima()
        val litrosDia = getLitrosDia()
        val unidadFam = unidadFamiliar.toIntOrNull() ?: 0

        val maxConsumo = if (unidadFam > 0) {
            dias * litrosDia * unidadFam
        } else {
            dias * litrosDia
        }

        val ultimaLect = ultimaLectura.toDoubleOrNull() ?: 0.0
        val penultimaLect = penultimaLectura.toDoubleOrNull() ?: 0.0

        var diff = round(ultimaLect - penultimaLect, 3)

        if (unidad == "m3l") {
            diff *= 1000
        }

        val exceso = round(diff - maxConsumo, 3)

        // Si la diferencia es negativa, retornar la diferencia
        if (diff < 0) {
            return diff
        }

        // Si hay exceso positivo
        if (exceso > 0) {
            return if (unidad == "l") {
                round(exceso, 0)
            } else {
                exceso
            }
        }

        return 0.0
    }

    /**
     * Obtiene el color del borde según el exceso
     * Rojo: exceso > 0
     * Azul: exceso < 0
     * Amarillo: exceso == 0
     */
    fun getBorderColor(): String {
        val exceso = getLastExceso()
        return when {
            exceso > 0 -> "#F44336" // Rojo
            exceso < 0 -> "#2196F3" // Azul
            else -> "#FFC107" // Amarillo
        }
    }

    /**
     * Calcula los días desde la última lectura hasta hoy
     */
    fun getDias(): Int {
        return try {
            val ultimaDate = parseDateFromString(fechaLectura)
            val hoy = java.util.Calendar.getInstance().time

            if (ultimaDate != null) {
                val diffMillis = hoy.time - ultimaDate.time
                val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                if (days > 0) days else 1
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Obtiene la unidad formateada
     */
    fun getFormatedUnidad(): String {
        return when (unidad) {
            "m3l" -> "m³"
            "m3" -> "m³"
            "l" -> "l"
            else -> unidad
        }
    }

    /**
     * Calcula el consumo dado una nueva lectura
     * @param nuevaLectura nueva lectura del contador
     * @return String con el consumo formateado
     */
    fun getConsumo(nuevaLectura: Double): String {
        val ultimaLect = ultimaLectura.toDoubleOrNull() ?: 0.0
        var diff = round(nuevaLectura - ultimaLect, 3)

        if (unidad == "m3l") {
            diff *= 1000
        }

        return if (unidad == "m3") {
            "$diff ${getFormatedUnidad()}"
        } else {
            "${diff.toLong()} ${getFormatedUnidad()}"
        }
    }

    /**
     * Calcula el exceso dado una nueva lectura
     * @param nuevaLectura nueva lectura del contador
     * @return String con el exceso formateado, o vacío si no hay exceso
     */
    fun getExceso(nuevaLectura: Double): String {
        val dias = getDias()
        val litrosDia = getLitrosDia()
        val unidadFam = unidadFamiliar.toIntOrNull() ?: 0

        val maxConsumo = if (unidadFam > 0) {
            dias * litrosDia * unidadFam
        } else {
            dias * litrosDia
        }

        val ultimaLect = ultimaLectura.toDoubleOrNull() ?: 0.0
        var diff = round(nuevaLectura - ultimaLect, 3)

        if (unidad == "m3l") {
            diff *= 1000
        }

        val exceso = round(diff - maxConsumo, 3)

        if (exceso > 0) {
            return if (unidad == "m3") {
                "$exceso ${getFormatedUnidad()}"
            } else {
                "${exceso.toLong()} ${getFormatedUnidad()}"
            }
        }

        return ""
    }
}

