package com.codefm.aquameter.ui.screens.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.codefm.aquameter.R
import com.codefm.aquameter.databinding.ActivityLoginBinding
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.service.BiometricCredentialManager
import com.codefm.aquameter.ui.screens.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity de login con diseño elegante tipo card y autenticación biométrica
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var biometricCredentialManager: BiometricCredentialManager

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var lastSuccessfulUsername: String? = null
    private var lastSuccessfulPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay una sesión activa
        if (UserSession.isLoggedIn) {
            navigateToHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometric()
        setupViews()
        observeViewModel()
        checkBiometricAvailability()
    }

    private fun setupBiometric() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(this@LoginActivity,
                            getString(R.string.biometric_error) + ": $errString",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity,
                        R.string.biometric_failed,
                        Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setDescription(getString(R.string.biometric_description))
            .setNegativeButtonText(getString(R.string.biometric_negative_button))
            .build()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        // Mostrar botón de huella solo si está disponible y hay credenciales guardadas
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS &&
            biometricCredentialManager.hasBiometricCredentials()) {
            binding.biometricButton.visibility = View.VISIBLE
        } else {
            binding.biometricButton.visibility = View.GONE
        }
    }

    private fun handleBiometricSuccess() {
        val credentials = biometricCredentialManager.getCredentials()
        if (credentials != null) {
            val (username, password) = credentials
            Toast.makeText(this, R.string.biometric_success, Toast.LENGTH_SHORT).show()

            // Primero rellenar los campos de texto (esto disparará los TextWatcher que actualizarán el ViewModel)
            binding.usernameInput.setText(username)
            binding.passwordInput.setText(password)

            // Luego hacer login automáticamente
            viewModel.onLogin()
        } else {
            Toast.makeText(this, "Error al recuperar credenciales", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        // Listener para el campo de usuario
        binding.usernameInput.doAfterTextChanged { text ->
            viewModel.onUsernameChange(text?.toString() ?: "")
        }

        // Listener para el campo de contraseña
        binding.passwordInput.doAfterTextChanged { text ->
            viewModel.onPasswordChange(text?.toString() ?: "")
        }

        // Listener para el botón de login
        binding.loginButton.setOnClickListener {
            viewModel.onLogin()
        }

        // Listener para el botón de autenticación biométrica
        binding.biometricButton.setOnClickListener {
            authenticateWithBiometric()
        }

        // Acción del teclado en el campo de contraseña
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onLogin()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                // Estado de carga: deshabilitar inputs y mostrar spinner
                binding.loginButton.text = "Iniciando sesión..."
                binding.loginButton.isEnabled = false
                binding.usernameInput.isEnabled = false
                binding.passwordInput.isEnabled = false
                binding.progressIndicator.visibility = View.VISIBLE
            } else {
                // Estado normal: habilitar inputs y ocultar spinner
                binding.loginButton.text = getString(R.string.login_button)
                binding.loginButton.isEnabled = true
                binding.usernameInput.isEnabled = true
                binding.passwordInput.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
            }
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                binding.errorMessage.text = error
                binding.errorMessage.visibility = View.VISIBLE
            } else {
                binding.errorMessage.visibility = View.GONE
            }
        }

        // Observar login exitoso
        viewModel.isLoginSuccessful.observe(this) { isSuccessful ->
            if (isSuccessful) {
                // Guardar las credenciales exitosas
                lastSuccessfulUsername = binding.usernameInput.text.toString()
                lastSuccessfulPassword = binding.passwordInput.text.toString()

                // Verificar si debe ofrecer guardar huella
                offerBiometricSave()

                viewModel.resetLoginSuccess()
            }
        }
    }

    private fun authenticateWithBiometric() {
        biometricPrompt.authenticate(promptInfo)
    }

    private fun offerBiometricSave() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        // Solo ofrecer si la biometría está disponible
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            navigateToHome()
            return
        }

        val currentUsername = lastSuccessfulUsername ?: ""
        val currentPassword = lastSuccessfulPassword ?: ""

        // Verificar si ya hay credenciales guardadas
        val existingCredentials = biometricCredentialManager.getCredentials()

        if (existingCredentials != null) {
            val (savedUsername, savedPassword) = existingCredentials

            // Si las credenciales son diferentes, notificar y actualizar
            if (savedUsername != currentUsername || savedPassword != currentPassword) {
                AlertDialog.Builder(this)
                    .setTitle("Actualizar huella dactilar")
                    .setMessage(getString(R.string.biometric_credentials_changed))
                    .setPositiveButton("Actualizar") { _, _ ->
                        saveBiometricCredentials(currentUsername, currentPassword)
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        navigateToHome()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                // Las credenciales son las mismas, ir a home
                navigateToHome()
            }
        } else {
            // No hay credenciales guardadas, ofrecer guardar
            AlertDialog.Builder(this)
                .setTitle("Configurar huella dactilar")
                .setMessage(getString(R.string.biometric_save_prompt))
                .setPositiveButton("Sí") { _, _ ->
                    saveBiometricCredentials(currentUsername, currentPassword)
                }
                .setNegativeButton("No") { _, _ ->
                    navigateToHome()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun saveBiometricCredentials(username: String, password: String) {
        val success = biometricCredentialManager.saveCredentials(username, password)
        if (success) {
            Toast.makeText(this, R.string.biometric_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al guardar credenciales", Toast.LENGTH_SHORT).show()
        }
        navigateToHome()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

