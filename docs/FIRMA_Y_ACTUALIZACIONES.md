# Firma, versiones y actualizaciones

Instalar una APK nueva **encima** de la anterior exige que se cumplan tres condiciones a la vez.
Si falla cualquiera de ellas, Android muestra «No se instaló la aplicación» o «La aplicación no se
instaló porque el paquete es incompatible».

## 1. El identificador de paquete no cambia nunca

```gradle
defaultConfig {
    applicationId 'com.streamvault'   // ← esto NO se toca, nunca
    versionCode 9
    versionName '2.7.0'
}
```

El `applicationId` es la identidad de la app para Android. Si cambiara (por ejemplo a
`com.denilson.musicdrive`), la nueva APK sería **otra aplicación distinta**: el sistema no la
aceptaría como actualización, habría que desinstalar la anterior y con ella se perderían la
biblioteca, las playlists, los favoritos y el historial.

Por eso esta app mantiene `com.streamvault` desde la primera versión y la prueba
`VersioningTest.applicationIdNeverChanges` lo vigila: si alguien lo cambia, la compilación falla.

> Si algún día se quisiera realmente otro identificador, hay que asumir una única migración con
> pérdida de datos: respaldar los ajustes (Ajustes → Respaldar), desinstalar, instalar con el id
> nuevo y restaurar. No hay forma de hacerlo manteniendo los datos.

## 2. La misma clave de firma en todas las versiones

Cada APK se firma con `keystore/lumina.jks`:

```
alias      : denilson
storepass  : lumina-denilson
keypass    : lumina-denilson
validez    : 10950 días (30 años)
```

* El archivo **está versionado** en el repositorio (`!keystore/lumina.jks` en `.gitignore`), de modo
  que cualquier máquina y cualquier ejecución de CI generan APKs con la misma firma.
* El flujo de CI (`.github/workflows/build.yml`) primero intenta usar el secreto
  `KEYSTORE_BASE64`; si no existe, reutiliza el keystore del repositorio y, si falta, lo genera y
  lo guarda. Así la clave no cambia entre ejecuciones.
* `app/build.gradle` aplica la firma tanto a `debug` como a `release` cuando el keystore existe.

**Consecuencia práctica:** si instalas una APK firmada con otra clave (por ejemplo, compilada en
otro sitio), Android la rechazará aunque el paquete sea el mismo. La app detecta ese caso y ofrece
desinstalar la versión anterior (Ajustes → Actualizaciones).

## 3. `versionCode` siempre creciente

| Versión | versionCode | Novedades relevantes |
| --- | --- | --- |
| 2.3.0 | 6 | Reproductor, cola y escaneo |
| 2.4.0 | 7 | Actualizaciones desde GitHub, ajustes respaldables |
| 2.5.0 | 8 | Telegram, separación de fuentes, FLAC |
| 2.6.0 | 8 → (corregido) | Loudness BS.1770, búsqueda por relevancia, metadatos |
| **2.7.0** | **9** | Motor de búsqueda propio, pestañas TELÉFONO/ONLINE/TODOS, playlists por tipo |

`versionCode` es un entero que Android compara: la nueva APK debe tener un número **mayor**.
`versionName` (2.7.0) es solo la etiqueta que ve el usuario.

## 4. Actualización desde la propia app

Ajustes → Actualizaciones:

1. **Buscar actualización** consulta
   `https://api.github.com/repos/municipalidad1998/apk-resplado/releases/latest`.
2. Muestra versión actual, versión nueva, novedades y tamaño del APK.
3. **Actualizar** descarga el APK y abre el instalador estándar de Android
   (`Intent.ACTION_VIEW` con `FileProvider`, o `PackageInstaller` en Android 10+).
4. Si el sistema aún no tiene permiso para instalar apps de esta fuente, se abre el ajuste
   «Instalar apps desconocidas» y la actualización queda pendiente.

La actualización **no borra nada**: la base de datos SQLite se mantiene y, cuando el esquema cambia,
Room ejecuta las migraciones (1→2, 2→3, 3→4, 4→5), todas idempotentes. La prueba instrumentada
`migrationKeepsLibraryAndPlaylistWhenUpdatingFromVersionOne` lo verifica desde la versión 1.

## 5. Comprobación rápida tras instalar

```bash
adb shell dumpsys package com.streamvault | grep -E "versionCode|versionName|lastUpdateTime"
```

El `versionCode` debe ser el de la versión recién instalada y `lastUpdateTime` reciente: eso
confirma que se actualizó **sobre** la anterior y no se reinstaló.
