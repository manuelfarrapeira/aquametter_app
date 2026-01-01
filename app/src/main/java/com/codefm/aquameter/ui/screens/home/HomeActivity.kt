package com.codefm.aquameter.ui.screens.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.codefm.aquameter.R
import com.codefm.aquameter.databinding.ActivityHomeBinding
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.ui.adapters.ContadorAdapter
import com.codefm.aquameter.ui.screens.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity de Home que muestra la lista de contadores
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar nombre de la traída
        val traidaName = UserSession.nombre ?: "Aquameter"
        binding.traidaNameText.text = traidaName

        setupSearchFunctionality()
        setupSwipeRefresh()
        setupFabMenu()
        observeViewModel()
        viewModel.loadContadores()
    }

    private fun setupSearchFunctionality() {
        // Toggle del campo de búsqueda al hacer clic en el icono
        binding.searchIcon.setOnClickListener {
            if (binding.searchInputLayout.visibility == View.GONE) {
                binding.searchInputLayout.visibility = View.VISIBLE
                binding.searchInput.requestFocus()
                // Mostrar el teclado
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                binding.searchInputLayout.visibility = View.GONE
                binding.searchInput.text?.clear()
                viewModel.filterContadores("")
                // Ocultar el teclado
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
            }
        }

        // Filtrar mientras el usuario escribe
        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterContadores(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadContadores()
        }
        // Configurar colores del indicador de refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            android.R.color.holo_blue_dark,
            android.R.color.holo_green_dark
        )
    }

    private fun setupFabMenu() {
        // Click en el FAB principal para abrir/cerrar el menú
        binding.fabMenu.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        // Botón ordenar por nombre
        binding.fabSortByName.setOnClickListener {
            viewModel.sortByName()
            closeFabMenu()
            Toast.makeText(this, getString(R.string.sort_by_name), Toast.LENGTH_SHORT).show()
        }

        // Botón ordenar por código
        binding.fabSortByCode.setOnClickListener {
            viewModel.sortByCode()
            closeFabMenu()
            Toast.makeText(this, getString(R.string.sort_by_code), Toast.LENGTH_SHORT).show()
        }

        // Botón ordenar por usuario
        binding.fabSortByUser.setOnClickListener {
            viewModel.sortByUser()
            closeFabMenu()
            Toast.makeText(this, getString(R.string.sort_by_user), Toast.LENGTH_SHORT).show()
        }

        // Botón cerrar sesión
        binding.fabLogout.setOnClickListener {
            closeFabMenu()
            showLogoutConfirmationDialog()
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true

        // Rotar el FAB principal
        binding.fabMenu.animate().rotation(45f).setDuration(300).start()

        // Mostrar botones con animación
        binding.fabSortByName.show()
        binding.fabSortByNameLabel.visibility = View.VISIBLE

        binding.fabSortByCode.show()
        binding.fabSortByCodeLabel.visibility = View.VISIBLE

        binding.fabSortByUser.show()
        binding.fabSortByUserLabel.visibility = View.VISIBLE

        binding.fabLogout.show()
        binding.fabLogoutLabel.visibility = View.VISIBLE
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false

        // Restaurar rotación del FAB principal
        binding.fabMenu.animate().rotation(0f).setDuration(300).start()

        // Ocultar botones
        binding.fabSortByName.hide()
        binding.fabSortByNameLabel.visibility = View.GONE

        binding.fabSortByCode.hide()
        binding.fabSortByCodeLabel.visibility = View.GONE

        binding.fabSortByUser.hide()
        binding.fabSortByUserLabel.visibility = View.GONE

        binding.fabLogout.hide()
        binding.fabLogoutLabel.visibility = View.GONE
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.confirm_logout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                logout()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun logout() {
        // Limpiar sesión
        UserSession.logout()

        // Ir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Detener el refresh cuando termine la carga
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observar errores
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
                binding.contadoresListView.visibility = View.GONE
            } else {
                binding.errorText.visibility = View.GONE
            }
        }

        // Observar lista de contadores
        viewModel.contadores.observe(this) { contadores ->
            if (contadores.isNotEmpty()) {
                val adapter = ContadorAdapter(this, contadores) { contador ->
                    // Mostrar diálogo de confirmación antes de eliminar
                    showDeleteConfirmationDialog(contador.idLastLectura)
                }
                binding.contadoresListView.adapter = adapter
                binding.contadoresListView.visibility = View.VISIBLE
                binding.errorText.visibility = View.GONE
            }
        }

        // Observar éxito al eliminar
        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                viewModel.resetDeleteSuccess()
            }
        }

        // Observar error al eliminar
        viewModel.deleteError.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, getString(R.string.delete_error) + ": $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(idLastLectura: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_lectura)
            .setMessage(R.string.confirm_delete_lectura)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteLectura(idLastLectura)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

