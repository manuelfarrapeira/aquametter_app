package com.codefm.aquameter.ui.screens.medicion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codefm.aquameter.R
import com.codefm.aquameter.databinding.DialogMedicionBinding
import com.codefm.aquameter.model.Contador
import com.codefm.aquameter.model.UserSession
import com.codefm.aquameter.ui.screens.login.LoginActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity para registrar mediciones con foto
 */
@AndroidEntryPoint
class MedicionActivity : AppCompatActivity() {

    private lateinit var binding: DialogMedicionBinding
    private val viewModel: MedicionViewModel by viewModels()
    private lateinit var contador: Contador

    private var capturedBitmap: Bitmap? = null
    private var photoBase64: String? = null

    // Launcher para solicitar permiso de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, abrir cámara
            launchCamera()
        } else {
            // Permiso denegado, mostrar mensaje
            MaterialAlertDialogBuilder(this)
                .setTitle("Permiso de cámara requerido")
                .setMessage("Para tomar fotos de las mediciones, necesitas otorgar el permiso de cámara en la configuración de la aplicación.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Launcher para la cámara nativa
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                // Guardar la imagen capturada
                capturedBitmap = bitmap
                photoBase64 = viewModel.compressAndEncodeImage(bitmap)
                binding.btnVerFoto.isEnabled = true
                showMessage("Foto capturada correctamente")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogMedicionBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Obtener contador desde Intent
        val contadorJson = intent.getStringExtra("contador")
        if (contadorJson == null) {
            finish()
            return
        }

        contador = Gson().fromJson(contadorJson, Contador::class.java)

        setupViews()
        observeViewModel()
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
    }

    private fun setupViews() {
        // Configurar título y última lectura
        binding.txtTitulo.text = "${contador.nombre} - ${contador.codigoContador}"
        binding.txtUltimaLectura.text = getString(
            R.string.ultima_lectura_con_fecha,
            contador.ultimaLectura,
            contador.getFormatedUnidad(),
            contador.fechaLectura
        )

        // Botón tomar foto
        binding.btnTomarFoto.setOnClickListener {
            openNativeCamera()
        }

        // Botón ver foto
        binding.btnVerFoto.setOnClickListener {
            showPhotoDialog()
        }

        // Botón cancelar
        binding.btnCancelar.setOnClickListener {
            finish()
        }

        // Botón aceptar
        binding.btnAceptar.setOnClickListener {
            validateAndShowConfirmation()
        }
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnAceptar.isEnabled = false
                binding.btnAceptar.text = "Enviando..."
                binding.btnCancelar.isEnabled = false
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnAceptar.isEnabled = true
                binding.btnAceptar.text = "Aceptar"
                binding.btnCancelar.isEnabled = true
            }
        }

        // Observar éxito
        viewModel.successMessage.observe(this) { message ->
            if (message != null) {
                showSuccessDialog(message)
            }
        }

        // Observar error
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                showMessage(error)
            }
        }
    }

    private fun openNativeCamera() {
        // Verificar si el permiso de cámara está concedido
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido, abrir cámara
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostrar explicación de por qué necesitamos el permiso
                MaterialAlertDialogBuilder(this)
                    .setTitle("Permiso de cámara necesario")
                    .setMessage("La aplicación necesita acceso a la cámara para tomar fotos de las mediciones del contador.")
                    .setPositiveButton("Otorgar permiso") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                // Solicitar permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        } else {
            showMessage("No se encontró una aplicación de cámara")
        }
    }

    private fun showPhotoDialog() {
        capturedBitmap?.let { bitmap ->
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_photo_preview, null)
            val imageView = dialogView.findViewById<ImageView>(R.id.imagePreview)
            imageView.setImageBitmap(bitmap)

            MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun validateAndShowConfirmation() {
        val medicion = binding.edtMedicion.text.toString().trim()

        // Validar campo obligatorio
        if (medicion.isEmpty() || medicion == ".") {
            binding.medicionInputLayout.error = "Campo obligatorio"
            return
        }

        binding.medicionInputLayout.error = null

        val litros = medicion.toDoubleOrNull() ?: 0.0
        val nota = binding.edtNota.text.toString().trim()

        // Calcular mensaje de confirmación
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

        // Mostrar confirmación con BottomSheet
        showConfirmationBottomSheet(message, medicion, nota)
    }

    private fun showConfirmationBottomSheet(message: String, medicion: String, nota: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null)

        val txtMessage = view.findViewById<android.widget.TextView>(R.id.txtConfirmationMessage)
        val btnEnviar = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEnviar)
        val btnCancelarConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelarConfirm)

        txtMessage.text = message

        btnEnviar.setOnClickListener {
            bottomSheetDialog.dismiss()
            sendMedicion(medicion, nota)
        }

        btnCancelarConfirm.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun sendMedicion(medicion: String, nota: String) {
        val foto = photoBase64 ?: ""
        viewModel.sendMedicion(contador.id, medicion, nota, foto)
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showMessage(message: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToLogin() {
        // Ir a LoginActivity cuando la sesión ha expirado
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

