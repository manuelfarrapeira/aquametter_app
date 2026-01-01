# Plan: Pantalla Home con lista de contadores

Implementar la pantalla Home que cargue y muestre los contadores del usuario autenticado desde la API GET https://traidadeaugas.es/App/getContadores/id_traida, mostrando el nombre y código de contador en un ListView.

## Steps

1. Crear modelo de datos `Contador` con todos los campos del JSON de respuesta (id, nombre, codigo_contador, usuario_anterior, litros, unidad_familiar, unidad, ultima_lectura, fecha_lectura, id_last_lectura, penultima_lectura, penultima_fecha_lectura)

2. Actualizar `ApiService` para agregar el método `getContadores(id_traida)` que retorne `Response<List<Contador>>`

3. Crear `ContadorService` para encapsular la lógica de llamada a la API y manejo de respuestas/errores

4. Crear layout `item_contador.xml` para cada elemento del ListView mostrando nombre en negrita y "Contador: {codigo_contador}" debajo

5. Crear `ContadorAdapter` (ArrayAdapter) para gestionar el binding de datos del modelo Contador al layout item_contador

6. Crear `HomeViewModel` con LiveData para contadores, isLoading y errorMessage, que use `UserSession.idTraida` para hacer la llamada

7. Actualizar `activity_home.xml` con título, ProgressBar (loading), TextView (error) y ListView (contadores), usando ConstraintLayout con estados visibles/ocultos

8. Actualizar `HomeActivity` para usar ViewBinding, observar el ViewModel, configurar el adapter del ListView y cargar contadores en onCreate

## Further Considerations

1. **Manejo de lista vacía**: ¿Mostrar mensaje "No hay contadores disponibles" cuando la API retorna array vacío?

2. **Refresh de datos**: ¿Agregar SwipeRefreshLayout o botón para recargar la lista de contadores?

3. **Click en items**: ¿Implementar navegación a detalle del contador al hacer click en un item de la lista?

4. **Persistencia**: ¿Cachear la lista de contadores en local para mostrarla sin conexión?

5. **Menú de logout**: ¿Agregar opción en toolbar para cerrar sesión y volver al login?

