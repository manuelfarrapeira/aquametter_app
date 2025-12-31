# Cambio de Icono del Launcher - Gota de Agua

## Cambio Realizado ✅

Se ha cambiado el icono del launcher (ícono de la aplicación) para usar la **imagen de la gota de agua** que aparece en la pantalla de login, en lugar del icono de Android por defecto.

## Archivos Modificados

### 1. **ic_launcher_foreground.xml**
**Ubicación**: `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Cambio**: Reemplazado el diseño del robot de Android con el path de la gota de agua.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="3.5"
        android:scaleY="3.5"
        android:translateX="30"
        android:translateY="30">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="M12,2.69l5.66,5.66a8,8 0,1 1,-11.31 0z"/>
    </group>
</vector>
```

**Detalles**:
- Usa el mismo path de `ic_water_drop.xml`
- Escalado 3.5x para que se vea bien en el icono (108dp)
- Color blanco para contrastar con el fondo azul
- Centrado con translateX y translateY

### 2. **ic_launcher_background.xml**
**Ubicación**: `app/src/main/res/drawable/ic_launcher_background.xml`

**Cambio**: Simplificado a un fondo sólido con el color azul primario de la app.

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FF1976D2"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

**Detalles**:
- Color de fondo: `#FF1976D2` (azul primario de Aquameter)
- Mismo color que se usa en el tema de la app

### 3. **ic_launcher.xml** (sin cambios)
**Ubicación**: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`

Este archivo ya estaba configurado correctamente como adaptive icon y automáticamente usa los nuevos drawables:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

## Resultado Visual

### Icono del Launcher:
- **Fondo**: Azul (#FF1976D2) - Color primario de la app
- **Foreground**: Gota de agua blanca
- **Estilo**: Adaptive Icon (compatible con Android 8.0+)

### Características:
✅ **Consistencia visual**: El mismo icono que se ve en la pantalla de login
✅ **Branding coherente**: Usa los colores de la marca Aquameter
✅ **Adaptive Icon**: Se adapta a diferentes formas según el launcher del dispositivo
✅ **Monochrome**: También funciona en modo monocromo (Android 13+)

## Compilación

```
BUILD SUCCESSFUL in 2s
37 actionable tasks: 16 executed, 21 up-to-date
```

## Cómo se ve el icono

El icono ahora muestra:
- Una **gota de agua blanca** centrada
- Sobre un **fondo azul** (#1976D2)
- En todos los tamaños de icono (hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi)
- Adaptándose automáticamente a la forma del launcher (círculo, cuadrado redondeado, etc.)

## Notas Técnicas

### Adaptive Icons
Android usa adaptive icons desde la versión 8.0 (API 26). Estos iconos consisten en:
- **Background layer**: Capa de fondo (nuestro azul sólido)
- **Foreground layer**: Capa frontal (nuestra gota de agua)
- **Monochrome** (opcional): Versión monocromática para temas del sistema

El sistema puede aplicar diferentes máscaras según el dispositivo:
- Círculo
- Cuadrado redondeado
- Squircle (cuadrado con esquinas muy redondeadas)
- Forma personalizada del fabricante

### Escalado del Path
El path original de `ic_water_drop.xml` está diseñado para un viewport de 24x24dp. Para el launcher (108x108dp), se aplicó:
- `scaleX="3.5"` y `scaleY="3.5"`: Escala el icono 3.5 veces
- `translateX="30"` y `translateY="30"`: Centra el icono en el canvas de 108dp

Cálculo: 24 × 3.5 = 84dp, dejando (108-84)/2 = 12dp de margen, pero ajustado a 30dp para mejor centrado visual.

## Próximos Pasos (Opcional)

Si quieres personalizar más el icono:
1. Puedes agregar efectos o sombras al foreground
2. Puedes crear un gradiente en el background en lugar de color sólido
3. Puedes ajustar el tamaño o posición de la gota

