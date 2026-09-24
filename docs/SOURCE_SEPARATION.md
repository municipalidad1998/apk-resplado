# Conectar un modelo de separación de fuentes

La versión entregada **no contiene pesos de ML ni un endpoint de separación**, por lo que “Quitar voz” informa de esta dependencia y no inventa resultados. No se aplica cancelación de canal central como sustituto engañoso de un modelo de fuentes.

## Contrato

Implementa `SourceSeparator` en `processing/SourceSeparation.kt`:

- `availability(context)`: verifica modelo/versionado/licencia, runtime (por ejemplo ONNX Runtime o TensorFlow Lite), RAM, capacidad de CPU/GPU y espacio de disco. Devuelve `Unavailable(reason)` si falta cualquier requisito.
- `separate(context, sourceUri, outputDirectory)`: `Flow<SeparationEvent>` cancelable. Abre **solo lectura**, convierte a PCM según el contrato del modelo, procesa bloques solapados y reconstruye señal con overlap-add.
- Emite `Progress(percent, stage)` **basado en trabajo real**, no un temporizador. Al finalizar, emite un único `Complete(vocals, instrumental)` con archivos decodificables en el directorio temporal proporcionado.
- Respeta cancelación en lectura, inferencia y red. Cierra descriptores y no borres/modifiques el original. No devuelvas el mismo archivo como voz e instrumental.

Registra el proveedor en `LuminaApp.onCreate()` con `separation.register(MiProveedor(...))`, para que también esté disponible cuando WorkManager inicia un proceso nuevo sin actividad.

## Pipeline incluido

`SeparationWorker` valida disponibilidad, crea carpeta temporal privada, ejecuta el proveedor con notificación cancelable, persiste porcentaje/etapa y comprueba que los resultados existen y están dentro de esa carpeta. Valida duración con Android, SHA-256 diferente del original, extensión soportada y rutas diferentes entre stems. Copia los derivados a `files/exports`, crea pistas y ubicaciones nuevas en Room y devuelve sus IDs. El original permanece intacto. No se renombra un WAV como MP3; la extensión conserva el contenedor real del proveedor.

La pantalla observa WorkManager: muestra porcentaje/etapa, permite cancelar y, cuando el worker realmente tiene resultados, escuchar original/instrumental/voz y exportar el instrumental con `ACTION_CREATE_DOCUMENT`.

## Proveedor remoto opcional

No hay tráfico de red en esta entrega. Para añadir un servidor:

1. Implementa el mismo contrato, añade `INTERNET` en esa variante y configura HTTPS.
2. Solicita consentimiento explícito **por audio** antes de subirlo; explica almacenamiento/retención y posibilidad de cancelación.
3. Guarda credenciales de forma segura (no en Git, preferencias sin cifrar o URLs registradas).
4. Usa subida en stream, IDs de trabajo, consulta de progreso real, timeout, reintentos limitados y endpoint de borrado/cancelación.
5. Comprueba firma/longitud/MIME de descargas, cuotas y espacio libre. Nunca sobreescribas el original.
6. Incluye pruebas de salida corrupta, proveedor ausente, cancelación, falta de disco, reanudación y detección de que el servidor devuelve el original.

La interfaz no incluye una caja de URL/API ficticia porque todavía no existe un protocolo de servidor implementado.
