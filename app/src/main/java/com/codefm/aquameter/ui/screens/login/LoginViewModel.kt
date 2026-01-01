package com.codefm.aquameter.ui.screens.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codefm.aquameter.service.AuthService
import kotlinx.coroutines.launch

/**
 * Estado de la UI de login
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

/**
 * ViewModel para la pantalla de login
 */
class LoginViewModel : ViewModel() {
    private val authService = AuthService()

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    // LiveData individuales para facilitar el binding en XML
    val username: LiveData<String> = MutableLiveData("")
    val password: LiveData<String> = MutableLiveData("")
    val isLoading: LiveData<Boolean> = MutableLiveData(false)
    val errorMessage: LiveData<String?> = MutableLiveData(null)
    val isLoginSuccessful: LiveData<Boolean> = MutableLiveData(false)

    /**
     * Actualiza el nombre de usuario
     */
    fun onUsernameChange(username: String) {
        val currentState = _uiState.value ?: LoginUiState()
        _uiState.value = currentState.copy(username = username, errorMessage = null)
        (this.username as MutableLiveData).value = username
        (errorMessage as MutableLiveData).value = null
    }

    /**
     * Actualiza la contraseña
     */
    fun onPasswordChange(password: String) {
        val currentState = _uiState.value ?: LoginUiState()
        _uiState.value = currentState.copy(password = password, errorMessage = null)
        (this.password as MutableLiveData).value = password
        (errorMessage as MutableLiveData).value = null
    }

    /**
     * Intenta hacer login con las credenciales actuales
     */
    fun onLogin() {
        viewModelScope.launch {
            val currentState = _uiState.value ?: LoginUiState()

            // Validar que los campos no estén vacíos
            if (currentState.username.isBlank() || currentState.password.isBlank()) {
                _uiState.value = currentState.copy(errorMessage = "Por favor, completa todos los campos")
                (errorMessage as MutableLiveData).value = "Por favor, completa todos los campos"
                return@launch
            }

            // Mostrar loading
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            (isLoading as MutableLiveData).value = true
            (errorMessage as MutableLiveData).value = null

            try {
                // Validar credenciales contra la API
                val isValid = authService.validateLogin(
                    currentState.username,
                    currentState.password
                )

                if (isValid) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isLoginSuccessful = true,
                        errorMessage = null
                    )
                    (isLoading as MutableLiveData).value = false
                    (isLoginSuccessful as MutableLiveData).value = true
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "Usuario o contraseña incorrectos"
                    )
                    (isLoading as MutableLiveData).value = false
                    (errorMessage as MutableLiveData).value = "Usuario o contraseña incorrectos"
                }
            } catch (e: Exception) {
                // Manejo de errores de red u otros errores
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "Error de conexión. Verifica tu internet e intenta de nuevo."
                )
                (isLoading as MutableLiveData).value = false
                (errorMessage as MutableLiveData).value = "Error de conexión. Verifica tu internet e intenta de nuevo."
                e.printStackTrace()
            }
        }
    }

    /**
     * Resetea el estado de login exitoso
     */
    fun resetLoginSuccess() {
        val currentState = _uiState.value ?: LoginUiState()
        _uiState.value = currentState.copy(isLoginSuccessful = false)
        (isLoginSuccessful as MutableLiveData).value = false
    }
}

