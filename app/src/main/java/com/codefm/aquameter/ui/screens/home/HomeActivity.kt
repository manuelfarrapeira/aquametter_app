package com.codefm.aquameter.ui.screens.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.codefm.aquameter.R
import com.codefm.aquameter.databinding.ActivityHomeBinding
import com.codefm.aquameter.model.PendingMedicion
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.ui.adapters.ContadorAdapter
import com.codefm.aquameter.ui.screens.login.LoginActivity
import com.codefm.aquameter.ui.screens.medicion.MedicionActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity de Home que muestra la lista de contadores
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private var isFabMenuOpen = false
    private var isFilteringPending = false

    // Launcher para recibir resultado de MedicionActivity
    private val medicionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Recargar contadores después de agregar medición
            viewModel.loadContadores()
        }
    }

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

    override fun onResume() {
        super.onResume()
        // Verificar si la sesión ha expirado
        if (!UserSession.isLoggedIn) {
            // Sesión expirada, redirigir al login de forma silenciosa
            navigateToLogin()
            return
        }
        // Renovar el tiempo de actividad
        UserSession.updateActivity()

        // Actualizar estados de pendientes localmente (sin llamar a la API)
        // Esto es útil cuando volvemos de la pantalla de medición después de guardar en caché
        viewModel.refreshPendingStatesLocally()
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

        // Botón filtrar pendientes (toggle)
        binding.fabFilterPending.setOnClickListener {
            closeFabMenu()
            togglePendingFilter()
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

        // Mostrar botón de filtrar pendientes solo si hay pendientes
        val hasPendientes = viewModel.hasPendingMediciones.value == true
        if (hasPendientes) {
            binding.fabFilterPending.show()
            binding.fabFilterPendingLabel.visibility = View.VISIBLE
        }

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

        binding.fabFilterPending.hide()
        binding.fabFilterPendingLabel.visibility = View.GONE

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

    private fun navigateToLogin() {
        // Ir a LoginActivity sin limpiar explícitamente la sesión (ya expiró)
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
                val adapter = ContadorAdapter(
                    context = this,
                    contadores = contadores,
                    onDeleteClick = { contador ->
                        // Mostrar diálogo de confirmación antes de eliminar
                        showDeleteConfirmationDialog(contador.idLastLectura)
                    },
                    onItemClick = { contador ->
                        // Abrir formulario de medición
                        openMedicionActivity(contador)
                    },
                    onRetryClick = { contador ->
                        // Manejar reintento de envío
                        handleRetryMedicion(contador)
                    },
                    onClearCacheClick = { contador ->
                        // Mostrar confirmación antes de limpiar caché
                        showClearCacheConfirmationDialog(contador)
                    }
                )
                binding.contadoresListView.adapter = adapter
                binding.contadoresListView.visibility = View.VISIBLE
                binding.errorText.visibility = View.GONE
            }
        }

        // Observar si hay mediciones pendientes
        viewModel.hasPendingMediciones.observe(this) { hasPending ->
            // Si no hay pendientes y estamos filtrando, desactivar filtro automáticamente
            if (!hasPending && isFilteringPending) {
                isFilteringPending = false
                viewModel.showAllContadores()
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

        // Observar éxito al reintentar
        viewModel.retrySuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, R.string.retry_send_success, Toast.LENGTH_SHORT).show()
                viewModel.resetRetrySuccess()
            }
        }

        // Observar error al reintentar
        viewModel.retryError.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
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

    private fun openMedicionActivity(contador: com.codefm.aquameter.model.Contador) {
        // Verificar si hay datos pendientes en caché
        if (contador.hasPendingMedicion) {
            showPendingMedicionInfoDialog(contador)
            return
        }

        // Verificar si ya hay una lectura de hoy
        if (contador.isToday()) {
            // Calcular información de la lectura (entre última y penúltima)
            val lectura = contador.ultimaLectura
            val unidad = contador.getFormatedUnidad()
            val consumo = contador.getLastConsumo() // Consumo entre última y penúltima lectura
            val dias = contador.getDiasPenultima() // Días entre penúltima y última lectura

            // Construir mensaje detallado
            val mensaje = buildString {
                append("Lectura: $lectura $unidad\n\n")
                append("Consumo: $consumo\n\n")
                append("Días: $dias")
            }

            // Mostrar diálogo con información
            AlertDialog.Builder(this)
                .setTitle(R.string.lectura_ya_registrada)
                .setMessage(mensaje)
                .setPositiveButton(R.string.entendido, null)
                .show()
            return
        }

        // Si no hay lectura de hoy, abrir el formulario
        val intent = Intent(this, MedicionActivity::class.java)
        intent.putExtra("contador", Gson().toJson(contador))
        medicionLauncher.launch(intent)
    }

    private fun handleRetryMedicion(contador: com.codefm.aquameter.model.Contador) {
        // Obtener medición pendiente del repositorio
        val pendingMedicion = viewModel.getPendingMedicion(contador.id) ?: return

        val litros = pendingMedicion.litros.toDoubleOrNull() ?: 0.0

        // Calcular mensaje de confirmación (igual que en envío normal)
        val message = if (litros == 0.0) {
            "Reinicio de contador"
        } else {
            val exceso = contador.getExceso(litros)
            val consumo = contador.getConsumo(litros)
            val dias = contador.getDias()

            buildString {
                if (exceso.isEmpty()) {
                    append("Consumo máximo no superado\n\n")
                } else {
                    append("Exceso: $exceso\n\n")
                }
                append("Consumo: $consumo\n\n")
                append("Días: $dias")
            }
        }

        // Mostrar confirmación con BottomSheet (igual que en MedicionActivity)
        showRetryConfirmationBottomSheet(message, contador, pendingMedicion)
    }

    private fun showRetryConfirmationBottomSheet(
        message: String,
        contador: com.codefm.aquameter.model.Contador,
        pendingMedicion: PendingMedicion
    ) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null)

        val txtMessage = view.findViewById<TextView>(R.id.txtConfirmationMessage)
        val btnEnviar = view.findViewById<MaterialButton>(R.id.btnEnviar)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btnCancelarConfirm)

        txtMessage.text = message

        btnEnviar.setOnClickListener {
            bottomSheetDialog.dismiss()
            viewModel.retrySendMedicion(contador, pendingMedicion)
        }

        btnCancelar.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showClearCacheConfirmationDialog(contador: com.codefm.aquameter.model.Contador) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar medición pendiente")
            .setMessage(R.string.confirm_clear_cache)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.clearCachedMedicion(contador.id)
                Toast.makeText(this, R.string.clear_cache_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun togglePendingFilter() {
        if (isFilteringPending) {
            // Desactivar filtro
            isFilteringPending = false
            viewModel.showAllContadores()
            Toast.makeText(this, "Mostrando todos los contadores", Toast.LENGTH_SHORT).show()
        } else {
            // Activar filtro
            isFilteringPending = true
            viewModel.filterPendingOnly()
            Toast.makeText(this, "Mostrando solo pendientes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPendingMedicionInfoDialog(contador: com.codefm.aquameter.model.Contador) {
        // Obtener medición pendiente
        val pendingMedicion = viewModel.getPendingMedicion(contador.id) ?: return

        val litros = pendingMedicion.litros.toDoubleOrNull() ?: 0.0

        // Calcular consumo, días y exceso
        val consumo = contador.getConsumo(litros)
        val dias = contador.getDias()
        val exceso = contador.getExceso(litros)

        // Crear diálogo con BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_pending_medicion_info, null)

        val txtPendingMedicion = view.findViewById<TextView>(R.id.txtPendingMedicion)
        val txtPendingConsumo = view.findViewById<TextView>(R.id.txtPendingConsumo)
        val txtPendingDias = view.findViewById<TextView>(R.id.txtPendingDias)
        val txtPendingExceso = view.findViewById<TextView>(R.id.txtPendingExceso)
        val btnVerFoto = view.findViewById<MaterialButton>(R.id.btnVerFoto)
        val btnCerrarInfo = view.findViewById<MaterialButton>(R.id.btnCerrarInfo)

        // Configurar textos
        txtPendingMedicion.text = "Medición: $litros ${contador.getFormatedUnidad()}"
        txtPendingConsumo.text = "Consumo: $consumo"
        txtPendingDias.text = "Días: $dias"

        // Mostrar exceso si existe
        if (exceso.isNotEmpty()) {
            txtPendingExceso.text = "Exceso: $exceso"
            txtPendingExceso.visibility = View.VISIBLE
        } else {
            txtPendingExceso.text = "Consumo máximo no superado"
            txtPendingExceso.visibility = View.VISIBLE
        }

        // Mostrar botón de ver foto si hay foto
        if (pendingMedicion.foto.isNotEmpty()) {
            btnVerFoto.visibility = View.VISIBLE
            btnVerFoto.setOnClickListener {
                showPhotoDialog(pendingMedicion.foto)
            }
        }

        btnCerrarInfo.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showPhotoDialog(fotoBase64: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_view_photo, null)

        val imgFotoPreview = view.findViewById<android.widget.ImageView>(R.id.imgFotoPreview)
        val btnCerrarFoto = view.findViewById<MaterialButton>(R.id.btnCerrarFoto)

        // Decodificar base64 a Bitmap
        try {
            val imageBytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imgFotoPreview.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la foto", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        btnCerrarFoto.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
}

