package com.codefm.aquameter.ui.screens.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.codefm.aquameter.databinding.ActivityLoginBinding
import com.codefm.aquameter.ui.screens.home.HomeActivity

/**
 * Activity de login con diseño elegante tipo card
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
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
                binding.loginButton.text = getString(com.codefm.aquameter.R.string.login_button)
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
                navigateToHome()
                viewModel.resetLoginSuccess()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

