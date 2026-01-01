package com.codefm.aquameter.ui.screens.home

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

/**
 * Activity de Home que muestra la lista de contadores
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar nombre de la traída
        val traidaName = UserSession.nombre ?: "Aquameter"
        binding.traidaNameText.text = traidaName

        observeViewModel()
        viewModel.loadContadores()
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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

