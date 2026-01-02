# Plan: Sistema de caché para mediciones fallidas con confirmación de reenvío

Se implementará un sistema de almacenamiento local para mediciones que no puedan enviarse al servidor por errores de red o API. Las mediciones en caché se mostrarán en la lista de contadores con botones para reenviar o descartar. Al reenviar, se mostrará un popup de confirmación con consumo, exceso y días igual que en los envíos normales.

## Componentes a crear

### 1. Modelo PendingMedicion
**Archivo**: `app/src/main/java/com/codefm/aquameter/model/PendingMedicion.kt`

Modelo de datos para almacenar mediciones pendientes:
- `idContador: String` - ID del contador
- `litros: String` - Valor de la medición
- `nota: String` - Nota opcional
- `foto: String` - Foto en base64
- `fecha: String` - Fecha en formato yyyy-MM-dd
- `timestamp: Long` - Timestamp de cuando se guardó en caché

### 2. Repositorio PendingMedicionRepository
**Archivo**: `app/src/main/java/com/codefm/aquameter/service/PendingMedicionRepository.kt`

Gestiona el almacenamiento de mediciones pendientes usando SharedPreferences:
- Singleton inyectable con Hilt
- Usa Gson para serializar/deserializar lista de PendingMedicion
- Métodos:
  - `savePendingMedicion(medicion: PendingMedicion)` - Guarda o actualiza medición pendiente (elimina anterior del mismo contador)
  - `getAllPendingMediciones(): List<PendingMedicion>` - Obtiene todas las mediciones pendientes
  - `getPendingMedicion(idContador: String): PendingMedicion?` - Obtiene medición pendiente de un contador específico
  - `hasPendingMedicion(idContador: String): Boolean` - Verifica si un contador tiene medición pendiente
  - `deletePendingMedicion(idContador: String)` - Elimina medición pendiente de un contador
  - `clearAll()` - Limpia todas las mediciones pendientes

## Modificaciones en archivos existentes

### 3. Contador.kt
**Cambio**: Agregar propiedad transient para indicar si tiene medición pendiente

```kotlin
data class Contador(
    // ...campos existentes...
    
    @Transient
    var hasPendingMedicion: Boolean = false
)
```

### 4. MedicionViewModel.kt
**Cambios**:
- Inyectar `PendingMedicionRepository`
- Modificar `sendMedicion()` para que en caso de error guarde la medición en caché usando el repositorio
- Cambiar mensaje de error para notificar que se guardó localmente: "No se pudo enviar. La medición se guardó localmente para reenviar más tarde."

```kotlin
@HiltViewModel
class MedicionViewModel @Inject constructor(
    private val medicionService: MedicionService,
    private val pendingRepository: PendingMedicionRepository
) : ViewModel() {
    
    fun sendMedicion(idContador: String, litros: String, nota: String, foto: String) {
        viewModelScope.launch {
            try {
                // ...código existente de envío...
                
                if (success) {
                    // Si se envió correctamente, eliminar de caché si existía
                    pendingRepository.deletePendingMedicion(idContador)
                    _successMessage.value = message
                } else {
                    // Guardar en caché
                    val pending = PendingMedicion(idContador, litros, nota, foto, fecha)
                    pendingRepository.savePendingMedicion(pending)
                    _errorMessage.value = "No se pudo enviar. La medición se guardó localmente para reenviar más tarde."
                }
            } catch (e: Exception) {
                // Guardar en caché por error de red
                val pending = PendingMedicion(idContador, litros, nota, foto, fecha)
                pendingRepository.savePendingMedicion(pending)
                _errorMessage.value = "No se pudo enviar. La medición se guardó localmente para reenviar más tarde."
            }
        }
    }
}
```

### 5. HomeViewModel.kt
**Cambios**:
- Inyectar `PendingMedicionRepository` y `MedicionService`
- Al cargar contadores, marcar cuáles tienen mediciones pendientes
- Agregar LiveData para saber si hay pendientes: `_hasPendingMediciones`
- Agregar método `retrySendMedicion(contador: Contador, pendingMedicion: PendingMedicion)` - Reenvía medición guardada
- Agregar método `clearCachedMedicion(idContador: String)` - Elimina medición de caché
- Agregar método `filterPendingOnly()` - Filtra solo contadores con mediciones pendientes
- Agregar método `showAllContadores()` - Muestra todos los contadores (quita filtro de pendientes)

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contadorService: ContadorService,
    private val lecturaService: LecturaService,
    private val pendingRepository: PendingMedicionRepository,
    private val medicionService: MedicionService
) : ViewModel() {
    
    private val _hasPendingMediciones = MutableLiveData<Boolean>()
    val hasPendingMediciones: LiveData<Boolean> = _hasPendingMediciones
    
    private val _retrySuccess = MutableLiveData<Boolean>()
    val retrySuccess: LiveData<Boolean> = _retrySuccess
    
    private val _retryError = MutableLiveData<String?>()
    val retryError: LiveData<String?> = _retryError
    
    private var showingPendingOnly = false
    
    fun loadContadores() {
        viewModelScope.launch {
            // ...código existente...
            
            if (result != null) {
                // Marcar contadores con mediciones pendientes
                result.forEach { contador ->
                    contador.hasPendingMedicion = pendingRepository.hasPendingMedicion(contador.id)
                }
                allContadores = result
                _contadores.value = result
                
                // Actualizar si hay pendientes
                _hasPendingMediciones.value = result.any { it.hasPendingMedicion }
            }
        }
    }
    
    fun retrySendMedicion(contador: Contador, pendingMedicion: PendingMedicion) {
        viewModelScope.launch {
            try {
                val (success, message) = medicionService.addLectura(
                    fecha = pendingMedicion.fecha,
                    litros = pendingMedicion.litros,
                    idContador = pendingMedicion.idContador,
                    idUsuario = UserSession.idUsuario ?: "",
                    foto = pendingMedicion.foto,
                    nota = pendingMedicion.nota
                )
                
                if (success) {
                    // Eliminar de caché
                    pendingRepository.deletePendingMedicion(pendingMedicion.idContador)
                    _retrySuccess.value = true
                    loadContadores()
                } else {
                    _retryError.value = "No se pudo enviar: $message"
                }
            } catch (e: Exception) {
                _retryError.value = "Error de conexión: ${e.message}"
            }
        }
    }
    
    fun clearCachedMedicion(idContador: String) {
        pendingRepository.deletePendingMedicion(idContador)
        loadContadores()
    }
    
    fun filterPendingOnly() {
        showingPendingOnly = true
        val pending = allContadores.filter { it.hasPendingMedicion }
        _contadores.value = pending
    }
    
    fun showAllContadores() {
        showingPendingOnly = false
        _contadores.value = allContadores
    }
}
```

### 6. item_contador.xml
**Cambios**:
- Agregar dos ImageView para los botones de retry (flecha arriba) y clear (X)
- Estos botones reemplazarán al botón de papelera cuando `hasPendingMedicion` sea true
- Crear iconos necesarios: `ic_retry.xml` (flecha hacia arriba) y `ic_clear.xml` (X)

```xml
<!-- Dentro del RelativeLayout existente -->

<!-- Botón para reintentar envío (solo visible si hay medición pendiente) -->
<ImageView
    android:id="@+id/retryButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_alignParentEnd="true"
    android:layout_alignParentBottom="true"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/retry_send"
    android:padding="8dp"
    android:src="@drawable/ic_retry"
    android:visibility="gone"
    android:clickable="true"
    android:focusable="true" />

<!-- Botón para limpiar caché (solo visible si hay medición pendiente) -->
<ImageView
    android:id="@+id/clearCacheButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_toStartOf="@id/retryButton"
    android:layout_alignParentBottom="true"
    android:layout_marginEnd="8dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/clear_cache"
    android:padding="8dp"
    android:src="@drawable/ic_clear"
    android:visibility="gone"
    android:clickable="true"
    android:focusable="true" />
```

### 7. ContadorAdapter.kt
**Cambios**:
- Agregar callbacks `onRetryClick` y `onClearCacheClick` al constructor
- Modificar lógica para mostrar botones según `hasPendingMedicion`:
  - Si `hasPendingMedicion` es true: mostrar `retryButton` y `clearCacheButton`, ocultar `deleteButton`
  - Si `hasPendingMedicion` es false y es hoy: mostrar `deleteButton`, ocultar los otros
  - Si no es hoy: ocultar todos los botones

```kotlin
class ContadorAdapter(
    context: Context,
    private val contadores: List<Contador>,
    private val onDeleteClick: (Contador) -> Unit,
    private val onItemClick: (Contador) -> Unit,
    private val onRetryClick: (Contador) -> Unit,
    private val onClearCacheClick: (Contador) -> Unit
) : ArrayAdapter<Contador>(context, R.layout.item_contador, contadores) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ...código existente...
        
        val retryButton = view.findViewById<ImageView>(R.id.retryButton)
        val clearCacheButton = view.findViewById<ImageView>(R.id.clearCacheButton)
        
        if (contador.hasPendingMedicion) {
            // Mostrar botones de pendiente
            deleteButton.visibility = View.GONE
            retryButton.visibility = View.VISIBLE
            clearCacheButton.visibility = View.VISIBLE
            
            retryButton.setOnClickListener { onRetryClick(contador) }
            clearCacheButton.setOnClickListener { onClearCacheClick(contador) }
        } else if (isToday) {
            // Mostrar botón de eliminar
            deleteButton.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
            clearCacheButton.visibility = View.GONE
        } else {
            // Ocultar todos
            deleteButton.visibility = View.GONE
            retryButton.visibility = View.GONE
            clearCacheButton.visibility = View.GONE
        }
        
        return view
    }
}
```

### 8. HomeActivity.kt
**Cambios**:
- Actualizar adapter con nuevos callbacks
- Implementar `onRetryClick`: Mostrar popup de confirmación con consumo/exceso/días antes de reenviar
- Implementar `onClearCacheClick`: Mostrar confirmación antes de eliminar de caché
- Observar `hasPendingMediciones` para mostrar/ocultar FAB de filtro pendientes
- Agregar lógica del FAB de filtro pendientes en el menú flotante

```kotlin
private fun observeViewModel() {
    // ...código existente...
    
    // Observar si hay pendientes
    viewModel.hasPendingMediciones.observe(this) { hasPending ->
        // Mostrar u ocultar el botón de filtrar pendientes
        if (hasPending) {
            binding.fabFilterPending.visibility = View.VISIBLE
        } else {
            binding.fabFilterPending.visibility = View.GONE
        }
    }
    
    // Observar lista de contadores
    viewModel.contadores.observe(this) { contadores ->
        if (contadores.isNotEmpty()) {
            val adapter = ContadorAdapter(
                context = this,
                contadores = contadores,
                onDeleteClick = { contador ->
                    showDeleteConfirmationDialog(contador.idLastLectura)
                },
                onItemClick = { contador ->
                    openMedicionActivity(contador)
                },
                onRetryClick = { contador ->
                    handleRetryMedicion(contador)
                },
                onClearCacheClick = { contador ->
                    showClearCacheConfirmationDialog(contador)
                }
            )
            // ...resto del código...
        }
    }
    
    // Observar éxito al reintentar
    viewModel.retrySuccess.observe(this) { success ->
        if (success) {
            Toast.makeText(this, "Medición enviada correctamente", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Observar error al reintentar
    viewModel.retryError.observe(this) { error ->
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }
}

private fun handleRetryMedicion(contador: Contador) {
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
    contador: Contador, 
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

private fun showClearCacheConfirmationDialog(contador: Contador) {
    AlertDialog.Builder(this)
        .setTitle("Eliminar medición pendiente")
        .setMessage("¿Estás seguro de que deseas eliminar esta medición pendiente? Esta acción no se puede deshacer.")
        .setPositiveButton(android.R.string.ok) { _, _ ->
            viewModel.clearCachedMedicion(contador.id)
            Toast.makeText(this, "Medición eliminada de la caché", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
```

### 9. activity_home.xml
**Cambios**:
- Agregar FAB para filtrar pendientes entre los botones del menú flotante
- Solo se mostrará si `hasPendingMediciones` es true

```xml
<!-- Agregar después de fabLogout y antes de fabMenu -->

<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fabFilterPending"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="16dp"
    android:layout_marginBottom="16dp"
    android:contentDescription="@string/filter_pending"
    android:src="@drawable/ic_pending"
    android:visibility="gone"
    app:backgroundTint="#FFA726"
    app:fabSize="mini"
    app:layout_constraintBottom_toTopOf="@id/fabLogout"
    app:layout_constraintEnd_toEndOf="parent" />

<TextView
    android:id="@+id/fabFilterPendingLabel"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="8dp"
    android:background="@drawable/bg_fab_label"
    android:elevation="6dp"
    android:paddingStart="12dp"
    android:paddingEnd="12dp"
    android:paddingTop="8dp"
    android:paddingBottom="8dp"
    android:text="@string/filter_pending"
    android:textColor="@android:color/black"
    android:textSize="12sp"
    android:visibility="gone"
    app:layout_constraintBottom_toBottomOf="@id/fabFilterPending"
    app:layout_constraintEnd_toStartOf="@id/fabFilterPending"
    app:layout_constraintTop_toTopOf="@id/fabFilterPending" />
```

### 10. strings.xml
**Nuevos strings a agregar**:

```xml
<string name="retry_send">Reintentar envío</string>
<string name="clear_cache">Eliminar de caché</string>
<string name="filter_pending">Filtrar pendientes</string>
<string name="confirm_clear_cache">¿Estás seguro de que deseas eliminar esta medición pendiente?</string>
<string name="clear_cache_success">Medición eliminada de la caché</string>
<string name="retry_send_success">Medición enviada correctamente</string>
<string name="medicion_saved_locally">No se pudo enviar. La medición se guardó localmente para reenviar más tarde.</string>
```

### 11. Iconos a crear

**ic_retry.xml** (flecha hacia arriba):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#4CAF50"
        android:pathData="M12,4l-1.41,1.41L15.17,10H4v2h11.17l-4.58,4.59L12,18l7,-7z"
        android:rotation="270"
        android:pivotX="12"
        android:pivotY="12"/>
</vector>
```

**ic_clear.xml** (X):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#F44336"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/>
</vector>
```

**ic_pending.xml** (reloj/pendiente):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"/>
</vector>
```

## Flujo completo del usuario

1. **Envío fallido**: Usuario intenta enviar medición → Error de red/API → Se guarda en caché local → Se muestra mensaje informando que se guardó localmente

2. **Visualización de pendientes**: En la lista de contadores, los que tienen medición pendiente muestran dos botones:
   - Botón X (rojo): Eliminar medición de caché
   - Botón flecha arriba (verde): Reintentar envío

3. **Reenvío con confirmación**: Usuario hace clic en flecha arriba → Se muestra BottomSheet con información de consumo/exceso/días → Usuario confirma → Se envía → Si éxito: se elimina de caché y se recarga lista

4. **Filtro de pendientes**: Si hay al menos una medición pendiente → Aparece FAB naranja en el menú flotante → Al hacer clic filtra solo contadores con pendientes → Se puede desactivar volviendo a hacer clic

5. **Limpieza de caché**: Usuario hace clic en X → Confirmación → Se elimina de caché → Se recarga lista

## Consideraciones técnicas

- Las fotos en base64 no se comprimirán más (ya están comprimidas al capturarlas)
- El reenvío es completamente manual, no automático
- SharedPreferences es suficiente para almacenar las mediciones pendientes
- Al reenviar exitosamente, la medición se elimina automáticamente de la caché
- El filtro de pendientes solo aparece si hay al menos una medición pendiente
- Al cerrar sesión, se mantienen las mediciones en caché (no se limpian)

## Orden de implementación

1. Crear PendingMedicion.kt
2. Crear PendingMedicionRepository.kt
3. Modificar Contador.kt (agregar hasPendingMedicion)
4. Crear iconos (ic_retry, ic_clear, ic_pending)
5. Agregar strings en strings.xml
6. Modificar item_contador.xml
7. Modificar ContadorAdapter.kt
8. Modificar MedicionViewModel.kt
9. Modificar HomeViewModel.kt
10. Modificar activity_home.xml
11. Modificar HomeActivity.kt
12. Probar flujo completo

