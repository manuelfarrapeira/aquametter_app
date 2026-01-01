# Plan: Integración de autenticación con API REST y gestión de sesión global

Se implementará autenticación contra la API REST externa (https://traidadeaugas.es/App/Authenticate/{username}/{password}), creando un modelo de respuesta, actualizando el servicio de autenticación con llamadas HTTP usando Retrofit, y estableciendo una clase de sesión global accesible desde cualquier punto de la aplicación para almacenar los datos del usuario autenticado (id_traida, nombre, id_usuario).

## Steps

1. Agregar dependencias de red (`Retrofit`, `Gson`, `OkHttp`) en `build.gradle.kts` y `libs.versions.toml`, incluyendo permiso de internet en `AndroidManifest.xml`

2. Crear clases de datos en nuevo paquete `model`: `AuthResponse` para el JSON de respuesta y `UserSession` como singleton/object para gestión global de sesión

3. Crear interfaz `ApiService` con Retrofit para definir el endpoint GET de autenticación, configurando la URL base de la API

4. Refactorizar `AuthService` para usar Retrofit, realizar llamada HTTP, parsear respuesta (JSON exitoso vs "null"), y almacenar datos en `UserSession`

5. Actualizar `LoginViewModel` para ejecutar la validación de forma asíncrona con manejo de excepciones de red y timeout

## Further Considerations

1. **Gestión de sesión persistente**: ¿Deseas que la sesión persista entre cierres de app usando SharedPreferences o DataStore, o solo mantenerla en memoria mientras la app está activa?

2. **Cierre de sesión**: ¿Necesitas un mecanismo de logout que limpie la sesión y redirija al login?

3. **Seguridad HTTPS**: La API usa HTTPS, ¿necesitas configuración especial de certificados o está correctamente firmada?

