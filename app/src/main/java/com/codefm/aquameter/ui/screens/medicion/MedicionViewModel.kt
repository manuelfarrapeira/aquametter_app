package com.codefm.aquameter.ui.screens.medicion

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codefm.aquameter.model.PendingMedicion
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.service.MedicionService
import com.codefm.aquameter.service.PendingMedicionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * ViewModel para MedicionActivity
 */
@HiltViewModel
class MedicionViewModel @Inject constructor(
    private val medicionService: MedicionService,
    private val pendingRepository: PendingMedicionRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Comprime y codifica imagen a base64
     */
    fun compressAndEncodeImage(bitmap: Bitmap): String {
        return medicionService.compressAndEncodeImage(bitmap)
    }

    /**
     * Envía la medición a la API
     */
    fun sendMedicion(idContador: String, litros: String, nota: String, foto: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                // Obtener fecha actual en formato yyyy-MM-dd
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Madrid")
                }
                val fecha = dateFormat.format(Date())

                // Obtener ID de usuario desde sesión
                val idUsuario = UserSession.idUsuario ?: ""

                if (idUsuario.isEmpty()) {
                    _errorMessage.value = "Error: No hay sesión activa"
                    _isLoading.value = false
                    return@launch
                }

                // Enviar medición
                val (success, message) = medicionService.addLectura(
                    fecha = fecha,
                    litros = litros,
                    idContador = idContador,
                    idUsuario = idUsuario,
                    foto = foto,
                    nota = nota
                )

                _isLoading.value = false

                if (success) {
                    // Si se envió correctamente, eliminar de caché si existía
                    pendingRepository.deletePendingMedicion(idContador)
                    _successMessage.value = message
                } else {
                    // Guardar en caché (foto se guarda físicamente)
                    val pending = PendingMedicion(idContador, litros, nota, "", fecha)
                    pendingRepository.savePendingMedicion(pending, foto)
                    _errorMessage.value = "No se pudo enviar. La medición se guardó localmente para reenviar más tarde."
                }

            } catch (e: Exception) {
                _isLoading.value = false
                // Guardar en caché por error de red (foto se guarda físicamente)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Madrid")
                }
                val fecha = dateFormat.format(Date())
                val pending = PendingMedicion(idContador, litros, nota, "", fecha)
                pendingRepository.savePendingMedicion(pending, foto)
                _errorMessage.value = "No se pudo enviar. La medición se guardó localmente para reenviar más tarde."
                e.printStackTrace()
            }
        }
    }
}

