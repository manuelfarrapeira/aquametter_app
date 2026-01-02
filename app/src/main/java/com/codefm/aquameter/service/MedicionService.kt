package com.codefm.aquameter.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.codefm.aquameter.api.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para gestionar mediciones con foto y caché
 */
@Singleton
class MedicionService @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    /**
     * Comprime y codifica una imagen a base64
     * @param bitmap imagen a procesar
     * @return String en base64
     */
    fun compressAndEncodeImage(bitmap: Bitmap): String {
        return try {
            // Redimensionar manteniendo aspect ratio a 500px de altura
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newHeight = 500
            val newWidth = (newHeight * aspectRatio).toInt()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Comprimir a JPEG con calidad 100
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteArray = baos.toByteArray()

            // Codificar a base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Guarda una imagen en caché
     * @param base64 imagen en base64
     * @param contadorId ID del contador
     */
    fun saveImageToCache(base64: String, contadorId: String) {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "foto_contador_$contadorId.txt")
            file.writeText(base64)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Carga una imagen desde caché
     * @param contadorId ID del contador
     * @return String en base64 o null si no existe
     */
    fun loadImageFromCache(contadorId: String): String? {
        return try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "foto_contador_$contadorId.txt")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Elimina una imagen de caché
     * @param contadorId ID del contador
     */
    fun deleteImageFromCache(contadorId: String) {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "foto_contador_$contadorId.txt")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Envía una nueva lectura a la API
     * @param fecha fecha en formato yyyy-MM-dd
     * @param litros lectura del contador
     * @param idContador ID del contador
     * @param idUsuario ID del usuario
     * @param foto imagen en base64 (vacío si no hay foto)
     * @param nota nota opcional (vacío si no hay nota)
     * @return Pair con éxito (Boolean) y mensaje (String)
     */
    suspend fun addLectura(
        fecha: String,
        litros: String,
        idContador: String,
        idUsuario: String,
        foto: String,
        nota: String
    ): Pair<Boolean, String> {
        return try {
            // Intentar guardar foto en caché antes de enviar
            if (foto.isNotEmpty()) {
                saveImageToCache(foto, idContador)
            }

            val response = apiService.addLectura(fecha, litros, idContador, idUsuario, foto, nota)

            if (response.isSuccessful && response.body() != null) {
                // Leer la respuesta como texto plano
                val responseText = response.body()?.string()?.trim() ?: ""

                // Verificar si la respuesta es "OK"
                if (responseText.equals("Lectura insertada", ignoreCase = true)) {
                    // Éxito: eliminar foto de caché
                    if (foto.isNotEmpty()) {
                        deleteImageFromCache(idContador)
                    }
                    Pair(true, "Lectura registrada correctamente")
                } else {
                    // Respuesta inesperada: mantener foto en caché
                    Pair(false, "Error: respuesta inesperada del servidor")
                }
            } else {
                // Error: mantener foto en caché
                Pair(false, "Error al enviar la lectura")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Error de red: mantener foto en caché
            Pair(false, "Error de conexión: ${e.message}")
        }
    }

    /**
     * Limpia fotos antiguas de caché (más de 7 días)
     */
    fun cleanOldCachedImages() {
        try {
            val cacheDir = context.cacheDir
            val now = System.currentTimeMillis()
            val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("foto_contador_") && file.name.endsWith(".txt")) {
                    val fileAge = now - file.lastModified()
                    if (fileAge > sevenDaysInMillis) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

