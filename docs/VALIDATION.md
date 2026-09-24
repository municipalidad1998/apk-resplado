# Verificación

## Estado final de esta entrega

**24 de septiembre de 2026 — CI aprobado.**

Run: [35942450216](https://github.com/municipalidad1998/apk-resplado/actions/runs/35942450216)
Código probado: `dd86f97c049215bfceca757e64bd14f61b49cf22`
Los cambios posteriores a ese commit en esta entrega son únicamente documentación de resultados.

| Comprobación | Resultado |
|---|---|
| `testDebugUnitTest` | **20 aprobadas; 0 fallos, 0 errores, 0 omitidas** |
| `lintDebug` | **Aprobado**, sin errores bloqueantes |
| `assembleDebug` | **APK generado** |
| `assembleDebugAndroidTest` | **APK de tests generado** |
| Instrumentación Android 15 / API 35 | **5 pruebas aprobadas** |
| XML de manifiesto/recursos y archivo Gradle Wrapper | Validación estructural local aprobada |
| `git diff --check` | Sin errores de whitespace |

**[APK debug descargable](https://github.com/municipalidad1998/apk-resplado/actions/runs/35942450216/artifacts/10785677058)**. Los artefactos `lumina-build-reports` y `lumina-device-reports` del mismo run contienen resultados y logcat del emulador. El job de dispositivo instala los mismos APK que produce el job de compilación; no recompila una variante diferente.

### Alcance y correcciones durante la verificación

El entorno de edición carece de Java, SDK/emulador Android y acceso directo a Google Maven, así que la compilación y los tests Android se ejecutaron realmente en CI. El primer intento de preparación del SDK falló antes de compilar; se utilizó el SDK preinstalado del runner. Durante las pruebas se corrigieron dos problemas de fixtures/selectores: la diferencia entre URI alias `external` y volumen canónico `external_primary`, y un selector de texto que coincidía tanto con una fila de ajustes como con una opción de diálogo. La suite se repitió completa después de las correcciones y terminó en verde.

**No se ha validado físicamente** la escucha, Bluetooth, SD real ni el rendimiento con 10.000 archivos. Las cinco pruebas no certifican todos los teléfonos, códecs, permisos de fabricantes o situaciones de batería. Tampoco validan separación vocal real: no hay un modelo instalado. Queda la matriz manual siguiente antes de una distribución de producción.

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
