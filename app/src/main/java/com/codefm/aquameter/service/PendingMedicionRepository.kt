package com.codefm.aquameter.service

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.codefm.aquameter.model.PendingMedicion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar mediciones pendientes
 * Las fotos se guardan físicamente en el almacenamiento interno de la app
 */
@Singleton
class PendingMedicionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val photosDir: File = File(context.filesDir, "pending_photos").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private const val PREF_NAME = "pending_mediciones"
        private const val KEY_MEDICIONES = "mediciones"
    }

    /**
     * Guarda una medición pendiente con foto física
     * @param medicion Medición con fotoPath vacío
     * @param fotoBase64 Foto en Base64 (opcional)
     * @return Path de la foto guardada o cadena vacía
     */
    fun savePendingMedicion(medicion: PendingMedicion, fotoBase64: String = ""): String {
        val mediciones = getAllPendingMediciones().toMutableList()

        // Eliminar medición anterior del mismo contador si existe
        val previousMedicion = mediciones.find { it.idContador == medicion.idContador }
        if (previousMedicion != null) {
            // Eliminar foto física anterior si existe
            deletePendingPhoto(previousMedicion.fotoPath)
            mediciones.removeAll { it.idContador == medicion.idContador }
        }

        // Guardar foto física si existe
        val photoPath = if (fotoBase64.isNotEmpty()) {
            savePhotoToFile(medicion.idContador, fotoBase64)
        } else {
            ""
        }

        // Crear medición con la ruta de la foto
        val medicionWithPhoto = medicion.copy(fotoPath = photoPath)
        mediciones.add(medicionWithPhoto)

        // Guardar en SharedPreferences (solo metadatos, sin foto)
        val json = gson.toJson(mediciones)
        prefs.edit().putString(KEY_MEDICIONES, json).apply()

        return photoPath
    }

    /**
     * Guarda una foto en el almacenamiento interno de la app
     */
    private fun savePhotoToFile(idContador: String, fotoBase64: String): String {
        try {
            val photoFile = File(photosDir, "photo_$idContador.jpg")

            // Convertir Base64 a bytes
            val decodedBytes = Base64.decode(fotoBase64, Base64.DEFAULT)

            // Guardar en archivo
            photoFile.writeBytes(decodedBytes)

            return photoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Lee una foto del archivo y la convierte a Base64 para envío
     */
    fun getPhotoAsBase64(fotoPath: String): String {
        if (fotoPath.isEmpty()) return ""

        return try {
            val photoFile = File(fotoPath)
            if (!photoFile.exists()) return ""

            // Leer archivo
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

            // Convertir a Base64
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageBytes = baos.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Elimina una foto física
     */
    private fun deletePendingPhoto(fotoPath: String) {
        if (fotoPath.isEmpty()) return

        try {
            val photoFile = File(fotoPath)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Obtiene todas las mediciones pendientes
     */
    fun getAllPendingMediciones(): List<PendingMedicion> {
        val json = prefs.getString(KEY_MEDICIONES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingMedicion>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Obtiene una medición pendiente por ID de contador
     */
    fun getPendingMedicion(idContador: String): PendingMedicion? {
        return getAllPendingMediciones().find { it.idContador == idContador }
    }

    /**
     * Verifica si un contador tiene medición pendiente
     */
    fun hasPendingMedicion(idContador: String): Boolean {
        return getPendingMedicion(idContador) != null
    }

    /**
     * Elimina una medición pendiente y su foto
     */
    fun deletePendingMedicion(idContador: String) {
        val medicion = getPendingMedicion(idContador)
        if (medicion != null) {
            // Eliminar foto física
            deletePendingPhoto(medicion.fotoPath)
        }

        val mediciones = getAllPendingMediciones().toMutableList()
        mediciones.removeAll { it.idContador == idContador }

        val json = gson.toJson(mediciones)
        prefs.edit().putString(KEY_MEDICIONES, json).apply()
    }

    /**
     * Limpia todas las mediciones pendientes y fotos
     */
    fun clearAll() {
        // Eliminar todas las fotos
        getAllPendingMediciones().forEach {
            deletePendingPhoto(it.fotoPath)
        }

        prefs.edit().clear().apply()
    }
}

