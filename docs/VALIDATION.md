# Verificación

## Estado de esta entrega

Actualizado: 24 de septiembre de 2026. Los resultados de CI y las limitaciones del entorno se documentan explícitamente; “implementado” no significa “certificado en todos los dispositivos”.

- El entorno de edición no tiene Java, Android SDK ni emulador; las descargas directas de Google/Maven están restringidas.
- Se ejecutó la compilación en un runner de CI con JDK 17 y Android SDK.
- Run [35940833713](https://github.com/municipalidad1998/apk-resplado/actions/runs/35940833713): el job **build pasó** (`testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`). Se generaron APK debug y APK de pruebas y se publicaron como artefactos del run.
- En ese run pasaron 3/4 pruebas instrumentadas (Room, análisis PCM, sesión/crossfade/background). La de MediaStore falló al comparar el alias URI `external` usado al insertar con el URI canónico `external_primary` guardado por el escáner. Se corrigió el fixture para insertar en el volumen canónico. La reejecución [35941801977](https://github.com/municipalidad1998/apk-resplado/actions/runs/35941801977) confirmó las 4 pruebas funcionales y 20/20 tests JVM. La prueba de navegación encontró dos elementos con el texto “10 segundos” (fila de Ajustes y opción de diálogo); se restringió el selector al diálogo. La última reejecución de navegación sigue pendiente.
- No se ha hecho validación de escucha en un teléfono físico, auriculares Bluetooth, tarjeta SD real o una biblioteca de 10.000 archivos.

Los cambios posteriores a ese run deben volver a pasar la misma verificación. Consulta el run más reciente de la rama de esta entrega; un run anterior no certifica archivos modificados después.

## Tests JVM

- Ocho segundos de silencio, inicio inmediato, clicks aislados, mínimo configurable, sensibilidad, todo silencioso y NaN.
- Ganancias de crossfade: extremos, monotonía, límites, suma de cuadrados unitaria, duración acotada por pistas cortas.
- Seek limitado a archivo, offset manual (incluido cero), automático desactivado y límites de duración.
- Caracteres `%`, `_` y `\` del usuario escapados en búsqueda.
- Clasificación WhatsApp/recordings y fechas válidas/inválidas extraídas del nombre.

## Tests instrumentados

`DeviceTests` genera archivos WAV de prueba en tiempo de ejecución (no hay canciones añadidas al repositorio):

1. Room en memoria: favoritos, edición preservada, deduplicación, playlists, cola con duplicados y reconciliación de ubicaciones.
2. WAV con ocho segundos iniciales de silencio: decodificación real con MediaCodec, offset aproximado y comprobación de que los bytes del original permanecen idénticos.
3. Inserta dos copias en MediaStore: mismo hash/una pista, dos ubicaciones, segunda pasada preserva el nombre personalizado. Elimina los fixtures al terminar.
4. MediaController/servicio real con dos WAV: reproduce, detecta mezcla, pasa la actividad a segundo plano, comprueba continuidad y transición, pausa y seek.

`NavigationTests`: los cinco destinos, búsqueda, cambio de crossfade, creación/apertura/eliminación de una playlist y regreso a Inicio.

## Matriz manual antes de distribuir producción

Ejecutar en al menos Android 8, 12, 13, 14, 15 y una versión posterior disponible:

- [ ] Primera apertura sin permiso: biblioteca vacía, solicitud contextual, negar una vez/siempre, usar SAF sin permiso global.
- [ ] Autorizar música/denegar notificaciones: reproducir, pantalla apagada, abrir otra app y volver.
- [ ] SAF de WhatsApp, `.nomedia`, SD; revocar permiso desde Android; extraer/reinsertar SD.
- [ ] Mil/5.000/10.000 archivos: tiempo primer escaneo/incremental, memoria, scroll y búsqueda. No afirmar un benchmark sin medirlo.
- [ ] Nuevos archivos, cambio de archivo, borrado externo y duplicados en MediaStore+SAF.
- [ ] Casos MP3/VBR, AAC/M4A, FLAC 16/24 bits, WAV, OGG, OPUS, AMR; WMA/no compatible debe dar error comprensible.
- [ ] Portada embebida grande, imagen manual, cubierta SAF, archivo sin portada y error de imagen.
- [ ] Modificar metadatos, favorito, ocultar/restaurar, cerrar proceso y abrir; no perder información.
- [ ] Playlists: agregar repetido, eliminar, ordenar, cambiar nombre/portada y agregar a cola.
- [ ] Cola: cambiar/quitar actual, mover próxima durante crossfade, vaciar, agregar álbum/playlist.
- [ ] Crossfade 0/2/5/10/15/20/30 s; música correlacionada a máximo volumen, pistas cortas, offsets largos, pistas distintas en duración, repeat one/all y shuffle.
- [ ] Pausa/seek/skip/pérdida de foco/interrupción por llamada durante mezcla; no deben quedar dos canales activos.
- [ ] Bluetooth: play/pause/next, desconexión y reconexión; comparar con auriculares cableados.
- [ ] Análisis: silencio 0/1/8/120+ s, ruido de fondo, voz suave, click antes de música, manual cero y automático desactivado.
- [ ] Temas sistema/claro/oscuro, fuente grande, TalkBack, teléfono/tablet, horizontal, gestos y recortes de pantalla.
- [ ] Modelo ausente: mensaje honesto sin progreso inventado. Al integrar un proveedor: cancelación, error de red/disco, corrupto, salida idéntica al original y exportación segura.

Los fabricantes pueden suspender procesos en segundo plano con políticas de batería propias. No se puede prometer continuidad tras “Forzar detención” o apagar el teléfono.
