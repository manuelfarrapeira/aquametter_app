# Plan: Implementar pantalla de login con logo centrado tipo card y ViewModel

Crear una pantalla de login elegante con diseño tipo card centrado, icono de Material Icons (WaterDrop), validación mediante servicio dedicado (admin/admin), gestión de estado con ViewModel, y navegación a la pantalla de bienvenida tras autenticación exitosa.

## Estado de Implementación: ✅ COMPLETADO

### Componentes Implementados:

#### 1. ✅ Dependencias (build.gradle.kts)
- `androidx-lifecycle-viewmodel-compose` - Gestión de ViewModel
- `androidx-navigation-compose` - Navegación entre pantallas
- `androidx-compose-material-icons-extended` - Iconos de Material Design

#### 2. ✅ AuthService (com.codefm.aquameter.service.AuthService)
- Servicio de autenticación con validación hardcoded
- Método `validateLogin(username: String, password: String): Boolean`
- Validación: usuario="admin", contraseña="admin"

#### 3. ✅ LoginViewModel (com.codefm.aquameter.ui.screens.login.LoginViewModel)
- Hereda de `ViewModel`
- Gestión de estado con `StateFlow<LoginUiState>`
- Data class `LoginUiState` con:
  - `username: String`
  - `password: String`
  - `isLoading: Boolean`
  - `errorMessage: String?`
  - `isLoginSuccessful: Boolean`
- Funciones públicas:
  - `onUsernameChange(username: String)`
  - `onPasswordChange(password: String)`
  - `onLogin()` - Valida credenciales con AuthService
  - `resetLoginSuccess()` - Resetea el estado tras navegación exitosa

#### 4. ✅ LoginScreen (com.codefm.aquameter.ui.screens.login.LoginScreen)
- Diseño elegante tipo card centrado con elevación
- Componentes UI:
  - Icono `WaterDrop` de 80dp centrado (logo)
  - Título "Aquameter" con tipografía headline
  - Campo de usuario con icono `Person`
  - Campo de contraseña con icono `Lock` y toggle de visibilidad
  - Mensaje de error con `AnimatedVisibility`
  - Botón de login con `CircularProgressIndicator` en estado loading
- Características:
  - Manejo de foco con `FocusManager`
  - Navegación por teclado (Next/Done)
  - Validación de campos vacíos
  - Feedback visual de errores

## Compilación: ✅ EXITOSA

El proyecto compiló exitosamente sin errores después de:
- Crear `LoginViewModel.kt` completo con todas las funcionalidades
- Sincronización de Gradle
- Build exitoso: `BUILD SUCCESSFUL in 39s`

## Further Considerations

1. **Persistencia de sesión**: Actualmente el login no persiste entre reinicios - se podría agregar DataStore/SharedPreferences para mantener la sesión activa

2. **Animaciones de transición**: Las transiciones de navegación usarán las animaciones predeterminadas de Navigation Compose

3. **Seguridad**: La validación hardcoded (admin/admin) es temporal - en producción se debe integrar con un backend real

4. **Validaciones adicionales**: Se podrían agregar:
   - Requisitos mínimos de contraseña
   - Límite de intentos de login
   - Recuperación de contraseña
   - Registro de usuarios

## Detalles de Implementación

### Estructura de archivos a crear:
```
app/src/main/java/com/codefm/aquameter/
├── service/
│   └── AuthService.kt
├── ui/
│   ├── screens/
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   └── home/
│   │       └── HomeScreen.kt
│   └── navigation/
│       └── Navigation.kt
└── MainActivity.kt (modificar)
```

### Dependencias a agregar:

**libs.versions.toml:**
```toml
[versions]
navigationCompose = "2.8.5"
lifecycleViewmodelCompose = "2.8.7"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

**build.gradle.kts:**
```kotlin
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.compose.material.icons.extended)
```

### Validación de credenciales:
- Usuario: "admin"
- Contraseña: "admin"
- Cualquier otra combinación mostrará error: "Usuario o contraseña incorrectos"

### Diseño visual:
- Card centrado con elevación y esquinas redondeadas
- Icono WaterDrop de Material Icons en la parte superior del card
- Título "Aquameter" debajo del icono
- Campos de texto con estilo OutlinedTextField
- Campo de contraseña con transformación visual (ocultar caracteres)
- Botón de login con efecto de carga (CircularProgressIndicator)
- Mensaje de error en color rojo con animación de aparición/desaparición

