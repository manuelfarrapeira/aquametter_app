package com.codefm.aquameter.ui.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codefm.aquameter.model.Contador
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.service.ContadorService
import com.codefm.aquameter.service.LecturaService
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Home
 */
class HomeViewModel : ViewModel() {
    private val contadorService = ContadorService()
    private val lecturaService = LecturaService()

    private var allContadores: List<Contador> = emptyList()

    private val _contadores = MutableLiveData<List<Contador>>()
    val contadores: LiveData<List<Contador>> = _contadores

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val _deleteError = MutableLiveData<String?>()
    val deleteError: LiveData<String?> = _deleteError

    /**
     * Carga los contadores desde la API
     */
    fun loadContadores() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Obtener el id_traida de la sesión
                val idTraida = UserSession.idTraida

                if (idTraida == null) {
                    _errorMessage.value = "Error: No hay sesión activa"
                    _isLoading.value = false
                    return@launch
                }

                // Llamar al servicio
                val result = contadorService.getContadores(idTraida)

                if (result != null) {
                    allContadores = result
                    _contadores.value = result
                } else {
                    _errorMessage.value = "No se pudieron cargar los contadores"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Elimina una lectura
     */
    fun deleteLectura(idLastLectura: String) {
        viewModelScope.launch {
            try {
                val success = lecturaService.deleteLectura(idLastLectura)

                if (success) {
                    _deleteSuccess.value = true
                    // Recargar la lista de contadores
                    loadContadores()
                } else {
                    _deleteError.value = "No se pudo eliminar la lectura"
                }

            } catch (e: Exception) {
                _deleteError.value = "Error al eliminar: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Resetea el estado de eliminación exitosa
     */
    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }

    /**
     * Filtra los contadores por nombre
     * @param query texto de búsqueda
     */
    fun filterContadores(query: String) {
        if (query.isBlank()) {
            // Si no hay texto de búsqueda, mostrar todos
            _contadores.value = allContadores
        } else {
            // Filtrar por nombre (case insensitive)
            val filtered = allContadores.filter { contador ->
                contador.nombre.contains(query, ignoreCase = true)
            }
            _contadores.value = filtered
        }
    }

    /**
     * Ordena los contadores por nombre
     */
    fun sortByName() {
        val currentList = _contadores.value ?: return
        _contadores.value = currentList.sortedBy { it.nombre.lowercase() }
    }

    /**
     * Ordena los contadores por código de contador
     */
    fun sortByCode() {
        val currentList = _contadores.value ?: return
        _contadores.value = currentList.sortedBy { it.codigoContador.toIntOrNull() ?: 0 }
    }

    /**
     * Ordena los contadores por usuario anterior
     */
    fun sortByUser() {
        val currentList = _contadores.value ?: return
        _contadores.value = currentList.sortedBy { it.usuarioAnterior.toIntOrNull() ?: 0 }
    }
}

