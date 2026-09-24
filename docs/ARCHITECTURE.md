# Arquitectura y decisiones

## Capas y ciclo de vida

- `LuminaApp` es el contenedor de dependencias; una sola base Room y un mutex de escaneo por proceso.
- Compose observa `StateFlow`, `Flow` de Room y `PagingData`; `LibraryViewModel` orquesta operaciones, no decodifica audio.
- `MediaController` se conecta con un `SessionToken` al servicio. El servicio es el propietario de los reproductores. Cerrar una pantalla no libera ExoPlayer.
- WorkManager procesa escaneo, análisis por lotes y separación independientemente de la actividad. El escaneo y separación prolongados tienen notificación cancelable. Los trabajos de análisis son lotes de 25 pistas con batería no baja.
- No existe backend, cuenta ni tráfico de red en la versión local.

## Room

`tracks`: identidad SHA-256, URI preferida, metadatos, información personalizada, fecha, origen, duración, tamaño, portada privada, favoritos, contador/última reproducción, offsets, firma del análisis, forma de onda inicial y visibilidad/disponibilidad.

`locations`: URI única, pista, raíz de escaneo, tamaño, modificación y generación vista. Una pista puede tener varias ubicaciones. El escaneo no utiliza `REPLACE` en `tracks`: no destruye ediciones/favoritos. Los registros no vistos se podan **solo si terminó de consultarse la raíz**. Errores de permiso no se tratan como borrados. `reconcile()` elige una URI disponible y conserva metadatos de archivos ausentes.

`playlists` + `playlist_entries`: claves foráneas e índice de posición. Agregar/reordenar usa transacciones. Una canción aparece como máximo una vez en la misma playlist.

`queue`: orden de pistas, permite duplicados. Posición actual y ajustes pequeños se guardan en SharedPreferences. Cada tres segundos y al pausar/destruir se guarda la posición. No se fuerza reproducción al restaurar.

Versión inicial de la nueva biblioteca: 1. Las futuras modificaciones deben agregar una `Migration` y tests; **no usar migración destructiva**. Room exporta esquemas a `app/schemas` durante la compilación. La DB IPTV anterior no se reutiliza.

## Lectura y límites de memoria

El scanner no filtra `IS_MUSIC = 1`: de hacerlo perdería muchas notas de voz. Consulta audio por volumen para incluir SD cuando Android la expone. SAF recorre documentos mediante `DocumentsContract`, buscando audio por MIME/extensión y portadas hermanas. El tamaño y modificación permiten evitar repetir hashing/metadatos de archivos intactos; proveedores sin fecha confiable se vuelven a verificar.

Hash en bloques de 128 KiB y cancelación cooperativa. No se mantiene el archivo completo en RAM. Los bitmaps se decodifican muestreados, de hasta ~768 px, se guardan en almacenamiento privado y Coil obtiene miniaturas según el tamaño de cada composable. La lista principal usa Paging 3 con máximo 240 filas cargadas; carruseles limitados a 20. Las colecciones de metadatos de una playlist y la cola sí se cargan enteras para reordenarlas; las imágenes no. La cola desde la biblioteca/restaurada usa una proyección `QueueRecord` sin notas ni formas de onda y conserva el conjunto filtrado completo.

## Silencio

`SilenceGate` es independiente de Android. `SilenceAnalyzer` extrae una pista de audio, decodifica con MediaCodec y procesa PCM16 o float. Usa marcas de tiempo de salida y número de muestras; no confunde bytes comprimidos con amplitud. La sensibilidad se expresa en dBFS RMS; requiere 60 ms de sonido continuo y aplica 80 ms de pre-roll.

El resultado nunca cambia el archivo. `manualOffsetMs` es nullable: null utiliza el automático si está habilitado; 0 fuerza el inicio original. La firma `rms-v2:threshold:minimum` evita análisis repetidos y cambia al modificar ajustes. La forma de onda es una vista inicial, no de la canción completa.

## Crossfade

1. El ExoPlayer activo conserva toda la cola de la sesión.
2. Un segundo ExoPlayer se prepara 2,5 s antes del solapamiento, en la siguiente pista y su offset.
3. Cuando está `READY` y comienza el tramo de transición, reproduce simultáneamente con volumen cero.
4. Cada 40 ms se actualizan ganancias `cos(p·π/2)` / `sin(p·π/2)`, con progreso basado en posición multimedia (no en tiempo de pared).
5. Al terminar, `MediaSession.setPlayer()` adopta el reproductor entrante, que ya está reproduciendo. Se libera la cola del anterior y se reutiliza para la siguiente mezcla.

No se transcodifica, mezcla a disco ni se altera el original. Pausa, seek, cambios de cola, foco perdido o error cancelan la transición. La cola no queda con dos canales reproduciendo al pausar. El control de volumen visible ajusta el stream Android; las ganancias de mezcla son internas y no lo sustituyen.

Es una transición de potencia constante, no un limitador de picos, un DJ con beat matching o normalización perceptual. Latencia y continuidad audible dependen de dispositivos/códecs: además de los tests, hacen falta pruebas de escucha reales.

## UI

Paleta original lavanda/carbón/verde, arte geométrico generado, tarjetas y Material 3. La portada real muestreada tiñe el fondo del reproductor. Temas claros/oscuros/sistema y navegación adaptable a rail en tablet. Los carruseles están ocultos si aún no contienen datos: no se muestran estadísticas ni reproducciones ficticias.

El menú ofrece nombre personalizado, edición, favoritas, playlists, cola/álbum, portada, offset, compartir el archivo original con permiso temporal, ubicación, información y ocultación reversible. Los botones de orden son accesibles; no dependen de drag-and-drop. La cola se abre como hoja encima del reproductor, también al deslizar la portada hacia arriba.
