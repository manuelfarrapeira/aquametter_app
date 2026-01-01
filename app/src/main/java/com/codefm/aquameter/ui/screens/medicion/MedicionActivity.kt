package com.codefm.aquameter.ui.screens.medicion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.codefm.aquameter.R
import com.codefm.aquameter.databinding.DialogMedicionBinding
import com.codefm.aquameter.model.Contador
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraDialog: AlertDialog? = null

    // Launcher para permisos de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraDialog()
        } else {
            showMessage("Permiso de cámara denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogMedicionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

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
            requestCameraPermission()
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
                binding.btnAceptar.isEnabled = false
                binding.btnAceptar.text = "Enviando..."
            } else {
                binding.btnAceptar.isEnabled = true
                binding.btnAceptar.text = "Aceptar"
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

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                showCameraDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_camera, null)
        val previewView = dialogView.findViewById<PreviewView>(R.id.previewView)
        val btnCapture = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCapture)
        val btnCloseCamera = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseCamera)

        cameraDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        startCamera(previewView)

        btnCapture.setOnClickListener {
            takePicture()
        }

        btnCloseCamera.setOnClickListener {
            cameraDialog?.dismiss()
        }

        cameraDialog?.show()
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage("Error al iniciar la cámara")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    capturedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Comprimir y codificar
                    capturedBitmap?.let { bitmap ->
                        photoBase64 = viewModel.compressAndEncodeImage(bitmap)
                        binding.btnVerFoto.isEnabled = true
                        showMessage("Foto capturada")
                    }

                    imageProxy.close()
                    cameraDialog?.dismiss()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    showMessage("Error al capturar foto")
                }
            }
        )
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

