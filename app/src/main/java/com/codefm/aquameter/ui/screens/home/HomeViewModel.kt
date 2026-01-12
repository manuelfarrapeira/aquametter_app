package com.codefm.aquameter.ui.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codefm.aquameter.model.Contador
import com.codefm.aquameter.model.PendingMedicion
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.service.ContadorService
import com.codefm.aquameter.service.LecturaService
import com.codefm.aquameter.service.MedicionService
import com.codefm.aquameter.service.PendingMedicionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Home
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contadorService: ContadorService,
    private val lecturaService: LecturaService,
    private val pendingRepository: PendingMedicionRepository,
    private val medicionService: MedicionService
) : ViewModel() {

    private var allContadores: List<Contador> = emptyList()
    private var showingPendingOnly = false

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

    private val _hasPendingMediciones = MutableLiveData<Boolean>()
    val hasPendingMediciones: LiveData<Boolean> = _hasPendingMediciones

    private val _retrySuccess = MutableLiveData<Boolean>()
    val retrySuccess: LiveData<Boolean> = _retrySuccess

    private val _retryError = MutableLiveData<String?>()
    val retryError: LiveData<String?> = _retryError

    private val _reloadError = MutableLiveData<String?>()
    val reloadError: LiveData<String?> = _reloadError

    // LiveData para envío masivo de pendientes
    private val _sendAllResult = MutableLiveData<Pair<Int, Int>?>() // (éxitos, fallos)
    val sendAllResult: LiveData<Pair<Int, Int>?> = _sendAllResult

    private val _isSendingAll = MutableLiveData<Boolean>()
    val isSendingAll: LiveData<Boolean> = _isSendingAll

    // LiveData para el progreso del envío masivo
    private val _sendAllProgress = MutableLiveData<Pair<Int, Int>>() // (enviadas, total)
    val sendAllProgress: LiveData<Pair<Int, Int>> = _sendAllProgress

    /**
     * Carga los contadores desde la API
     * @param isRefreshing indica si es una recarga (pull-to-refresh), en cuyo caso no se muestra el ProgressBar
     */
    fun loadContadores(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            try {
                // Solo mostrar ProgressBar si NO es un pull-to-refresh (carga inicial)
                if (!isRefreshing) {
                    _isLoading.value = true
                }
                _errorMessage.value = null
                _reloadError.value = null

                // Obtener el id_traida de la sesión
                val idTraida = UserSession.idTraida

                if (idTraida == null) {
                    // Error de carga inicial - sin datos previos
                    if (allContadores.isEmpty()) {
                        _errorMessage.value = "Error: No hay sesión activa"
                    } else {
                        // Error de recarga - tenemos datos previos
                        _reloadError.value = "No se pudo recargar la lista"
                    }
                    if (!isRefreshing) {
                        _isLoading.value = false
                    }
                    return@launch
                }

                // Llamar al servicio
                val result = contadorService.getContadores(idTraida)

                if (result != null) {
                    // Marcar contadores con mediciones pendientes
                    result.forEach { contador ->
                        contador.hasPendingMedicion = pendingRepository.hasPendingMedicion(contador.id)
                    }
                    allContadores = result
                    _contadores.value = if (showingPendingOnly) {
                        result.filter { it.hasPendingMedicion }
                    } else {
                        result
                    }

                    // Actualizar si hay pendientes
                    _hasPendingMediciones.value = result.any { it.hasPendingMedicion }

                    // Limpiar error de recarga si fue exitoso
                    _reloadError.value = null
                } else {
                    // Error al obtener contadores
                    if (allContadores.isEmpty()) {
                        // Error de carga inicial - sin datos previos
                        _errorMessage.value = "No se pudieron cargar los contadores"
                    } else {
                        // Error de recarga - tenemos datos previos
                        _reloadError.value = "No se pudo recargar la lista"
                    }
                }

                if (!isRefreshing) {
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                if (!isRefreshing) {
                    _isLoading.value = false
                }

                // Diferenciar entre error de carga inicial vs error de recarga
                if (allContadores.isEmpty()) {
                    // Error de carga inicial - sin datos previos
                    _errorMessage.value = "Error de conexión: ${e.message}"
                } else {
                    // Error de recarga - tenemos datos previos
                    _reloadError.value = "No se pudo recargar la lista"
                }

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
                _isLoading.value = true
                val success = lecturaService.deleteLectura(idLastLectura)

                if (success) {
                    _deleteSuccess.value = true
                    // Recargar la lista de contadores
                    loadContadores()
                } else {
                    _isLoading.value = false
                    _deleteError.value = "No se pudo eliminar la lectura"
                }

            } catch (e: Exception) {
                _isLoading.value = false
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
     * Filtra los contadores por nombre y código de contador
     * @param query texto de búsqueda
     */
    fun filterContadores(query: String) {
        if (query.isBlank()) {
            // Si no hay texto de búsqueda, mostrar todos
            _contadores.value = allContadores
        } else {
            // Filtrar por nombre o código de contador (case insensitive)
            val filtered = allContadores.filter { contador ->
                contador.nombre.contains(query, ignoreCase = true) ||
                contador.codigoContador.equals(query, ignoreCase = true)
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

    /**
     * Obtiene una medición pendiente por ID de contador
     */
    fun getPendingMedicion(idContador: String): PendingMedicion? {
        return pendingRepository.getPendingMedicion(idContador)
    }

    /**
     * Reintenta enviar una medición pendiente
     */
    fun retrySendMedicion(contador: Contador, pendingMedicion: PendingMedicion) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Convertir foto física a base64 para envío
                val fotoBase64 = if (pendingMedicion.fotoPath.isNotEmpty()) {
                    pendingRepository.getPhotoAsBase64(pendingMedicion.fotoPath)
                } else {
                    ""
                }

                val (success, message) = medicionService.addLectura(
                    fecha = pendingMedicion.fecha,
                    litros = pendingMedicion.litros,
                    idContador = pendingMedicion.idContador,
                    idUsuario = UserSession.idUsuario ?: "",
                    foto = fotoBase64,
                    nota = pendingMedicion.nota
                )

                if (success) {
                    // Eliminar de caché (esto también elimina la foto física)
                    pendingRepository.deletePendingMedicion(pendingMedicion.idContador)
                    _retrySuccess.value = true
                    loadContadores()
                } else {
                    _isLoading.value = false
                    _retryError.value = "No se pudo enviar: $message"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _retryError.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Elimina una medición de la caché
     */
    fun clearCachedMedicion(idContador: String) {
        pendingRepository.deletePendingMedicion(idContador)
        loadContadores()
    }

    /**
     * Filtra solo contadores con mediciones pendientes
     */
    fun filterPendingOnly() {
        showingPendingOnly = true
        val pending = allContadores.filter { it.hasPendingMedicion }
        _contadores.value = pending
    }

    /**
     * Muestra todos los contadores (quita filtro de pendientes)
     */
    fun showAllContadores() {
        showingPendingOnly = false
        _contadores.value = allContadores
    }

    /**
     * Actualiza el estado de pendientes de los contadores sin recargar desde la API
     * Útil cuando se vuelve de la pantalla de medición después de guardar en caché
     */
    fun refreshPendingStatesLocally() {
        // Actualizar estado de pendientes en todos los contadores
        allContadores.forEach { contador ->
            contador.hasPendingMedicion = pendingRepository.hasPendingMedicion(contador.id)
        }

        // Actualizar si hay pendientes
        _hasPendingMediciones.value = allContadores.any { it.hasPendingMedicion }

        // Refrescar la lista visible aplicando el filtro actual si corresponde
        if (showingPendingOnly) {
            _contadores.value = allContadores.filter { it.hasPendingMedicion }
        } else {
            _contadores.value = allContadores
        }
    }

    /**
     * Resetea el estado de éxito al reintentar
     */
    fun resetRetrySuccess() {
        _retrySuccess.value = false
    }

    /**
     * Obtiene la cantidad total de mediciones pendientes
     */
    fun getPendingCount(): Int {
        return allContadores.count { it.hasPendingMedicion }
    }

    /**
     * Envía todas las mediciones pendientes una por una
     */
    fun sendAllPendingMediciones() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isSendingAll.value = true

                // Obtener todas las mediciones pendientes
                val allPending = allContadores.filter { it.hasPendingMedicion }
                var successCount = 0
                var failCount = 0

                // Enviar una por una
                allPending.forEachIndexed { index, contador ->
                    val pendingMedicion = pendingRepository.getPendingMedicion(contador.id)

                    if (pendingMedicion != null) {
                        // Convertir foto a base64
                        val fotoBase64 = if (pendingMedicion.fotoPath.isNotEmpty()) {
                            pendingRepository.getPhotoAsBase64(pendingMedicion.fotoPath)
                        } else {
                            ""
                        }

                        // Intentar enviar
                        val (success, _) = medicionService.addLectura(
                            fecha = pendingMedicion.fecha,
                            litros = pendingMedicion.litros,
                            idContador = pendingMedicion.idContador,
                            idUsuario = UserSession.idUsuario ?: "",
                            foto = fotoBase64,
                            nota = pendingMedicion.nota
                        )

                        if (success) {
                            successCount++
                            // Eliminar de caché
                            pendingRepository.deletePendingMedicion(pendingMedicion.idContador)
                            // Actualizar contador para quitar pendiente
                            contador.hasPendingMedicion = false
                        } else {
                            failCount++
                        }
                    }

                    // Actualizar progreso (enviadas, total)
                    _sendAllProgress.value = Pair(successCount + failCount, allPending.size)
                }

                // Ocultar loading momentáneamente
                _isLoading.value = false
                _isSendingAll.value = false

                // Recargar la lista desde la API para obtener datos actualizados
                loadContadores()

                // Mostrar resultado
                _sendAllResult.value = Pair(successCount, failCount)

            } catch (e: Exception) {
                _isLoading.value = false
                _isSendingAll.value = false
                e.printStackTrace()
            }
        }
    }

    /**
     * Resetea el resultado de envío masivo
     */
    fun resetSendAllResult() {
        _sendAllResult.value = null
    }
}
