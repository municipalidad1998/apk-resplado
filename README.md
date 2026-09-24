# Lúmina · Tu música, en tu universo

**Actualización 2.1:** [correcciones de cola, inicio, navegación y crossfade por pista](docs/PLAYER_FIXES_2_1.md).

Reproductor **Android nativo**, local y sin cuenta. Kotlin + Jetpack Compose + Room + Media3. Este proyecto reemplaza la interfaz IPTV anterior de StreamVault; conserva `applicationId = com.streamvault`, pero utiliza una nueva base `lumina-library.db` y **no importa las antiguas listas IPTV**.

No es una web empaquetada, no contiene canciones de demostración y no sube archivos. Al abrirlo por primera vez, la biblioteca estará vacía hasta que se autorice el acceso a música o una carpeta.

## APK anterior verificado (2.0; ver actualización 2.1 arriba)

**[Descargar Lúmina debug (ZIP con APK)](https://github.com/municipalidad1998/apk-resplado/actions/runs/35942450216/artifacts/10785677058)** · [Resultado de CI y reportes](https://github.com/municipalidad1998/apk-resplado/actions/runs/35942450216)

Build del código `dd86f97`: **20/20 pruebas JVM, Android Lint, compilación y 5/5 pruebas instrumentadas en Android 15 aprobadas**. El artefacto es un APK de depuración instalable, no una versión firmada para distribución pública. Extrae `app-debug.apk` del ZIP; GitHub puede pedir iniciar sesión para descargar artefactos. Los artefactos tienen caducidad: si ya no está disponible, recompila con las instrucciones siguientes.

## Compilar e instalar

Requisitos:

- Android Studio Ladybug o posterior, **JDK 17**.
- Android SDK Platform **35**, Build Tools 35.0.0, Platform Tools.
- Android **8.0 / API 26** o posterior en el teléfono.
- Acceso a Google Maven, Maven Central y la distribución de Gradle durante la primera compilación.

```bash
# En la raíz del proyecto; no hace falta instalar Gradle globalmente.
chmod +x gradlew
# Configura ANDROID_HOME o escribe sdk.dir=/ruta/al/sdk en local.properties.
./gradlew testDebugUnitTest lintDebug assembleDebug

# APK instalable, firmado únicamente con la clave de depuración:
# app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En Windows, abre el proyecto en Android Studio y utiliza **Build → Build APK(s)**, o ejecuta `bash ./gradlew assembleDebug` desde Git Bash. `local.properties`, cachés y claves privadas no se versionan.

### APK de distribución

Android Studio → **Build → Generate Signed App Bundle / APK → APK → release**. Crea o selecciona **tu propia** clave de firma y guárdala fuera del repositorio. No se incluye una clave de producción ni una contraseña predeterminada. `./gradlew assembleRelease` genera un APK **sin firmar** si no se configura la firma. Para actualizar una instalación anterior se necesita la misma firma; el APK debug no puede actualizar uno de producción firmado con otra clave.

### Verificación automática

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Con un emulador/dispositivo conectado:
./gradlew connectedDebugAndroidTest
```

El workflow `.github/workflows/build.yml` compila, ejecuta tests JVM y lint, publica el APK debug y ejecuta pruebas instrumentadas en un emulador API 35. Consulta **[el estado real de las pruebas](docs/VALIDATION.md)**: la existencia de los tests no implica que hayan pasado.

## Primer uso

1. Pulsa **Encontrar mi música** y acepta el permiso de audio. Si prefieres no concederlo, usa **Seleccionar carpeta**.
2. Para WhatsApp, una SD o archivos no indexados, ve a **Ajustes → Agregar carpeta** y autoriza la carpeta con el selector de Android. Android puede impedir seleccionar la raíz del almacenamiento, `Android/data`, `Android/obb` o Descargas completa; selecciona una subcarpeta permitida. No se intenta saltar esa restricción.
3. El escaneo crea la biblioteca y obtiene metadatos y portadas. El primer escaneo calcula hashes completos; puede tardar si hay muchos archivos. Es cancelable desde la notificación.
4. Toca una pista para reproducirla. Toca el mini reproductor para abrir el reproductor completo, y **A continuación** o desliza la portada hacia arriba para abrir la cola sin cerrar el reproductor.
5. En `⋮` puedes editar título, nombre personalizado, artista, álbum, género, notas, etiquetas, portada, favoritos y punto de inicio.
6. **Actualizar biblioteca** detecta cambios. La información personalizada, la cola y las playlists se conservan al cerrar la app. La cola se restaura **en pausa**, sin reproducir inesperadamente al abrir.

## Qué hace esta versión

- **MediaStore en todos los volúmenes externos visibles** + carpetas SAF con permisos persistentes. Incluye audios que Android no marca como música: grabaciones, notas de voz y descargas.
- Escaneo incremental por URI, tamaño y modificación. SHA-256 de archivos nuevos/cambiados para deduplicar copias exactas, incluso entre MediaStore y SAF. Las ubicaciones se guardan separadas de la identidad de la pista.
- Metadatos con `MediaMetadataRetriever`, portada embebida, `cover.jpg`, `folder.jpg`, `album.jpg` y PNG en carpetas **SAF**, imagen manual copiada al almacenamiento privado y portadas gráficas deterministas de respaldo.
- Detección de WhatsApp por carpeta y nombres `AUD-/PTT-AAAAMMDD-WA…`; fecha extraída del nombre cuando es válida. Los nombres personalizados solo cambian en Room.
- Reproducción real con **Media3 ExoPlayer**, `MediaSessionService`, notificación multimedia, controles externos, audio focus, desconexión de auriculares y wake lock durante reproducción.
- **Crossfade real con dos ExoPlayers simultáneos** y ganancias seno/coseno de potencia constante. Duraciones globales 0/2/5/10/15/20/30/60/90/120/180 s y ajuste por pista de 0–180 s, con final útil manual. El servicio cambia el reproductor de la sesión al canal entrante al terminar la mezcla: no reinicia la canción siguiente ni crea un archivo mezclado.
- Salto configurable 5/10/15/30 s, anterior/siguiente, seek, volumen del sistema, aleatorio, repetir una/toda la cola, favoritos, entrada/salida suave sin crossfade.
- Análisis PCM de silencio en segundo plano. Ventanas RMS de 20 ms, tres ventanas consecutivas de señal, umbral configurable, mínimo de silencio y 80 ms de pre-roll. Offset automático y manual persistente, reproducción original y forma de onda **del fragmento inicial**.
- Búsqueda incremental por título, nombre personalizado, archivo, artista, álbum, género, carpeta y etiquetas. Biblioteca paginada con 60 filas/página y caché acotada. Portadas bajo demanda con Coil.
- Playlists con descripción, portada, agregar/quitar canciones y orden mediante botones accesibles. Cola reordenable, agregar canciones, playlists y álbumes.
- Inicio con contenido real, carruseles, cinco destinos, mini reproductor, temas claro/oscuro/sistema, adaptación a tablet y reproductor horizontal.
- Eliminación **solo de la biblioteca**, restaurable en Ajustes. No existe una acción de borrado físico de archivos originales.

## Límites explícitos (no funciones simuladas)

- **Quitar voz:** se incluye el contrato `SourceSeparator`, registro de proveedor, worker cancelable, progreso real, validación, importación de voz/instrumental, escucha y exportación. **No se incluye un modelo ML ni una API activa**. La interfaz indica que falta el modelo; no presenta una copia del original como “instrumental”. Ver [integración del motor](docs/SOURCE_SEPARATION.md).
- **Formatos:** MP3, AAC/M4A, WAV, FLAC, OGG/OPUS, AMR y otros se indexan. Reproducir y analizar depende del extractor/decodificador de Android y Media3. **WMA no está garantizado**; no se incluye un decodificador FFmpeg. Un formato no compatible genera un mensaje, no se borra de la colección.
- La búsqueda es SQLite `LIKE` (insensible a mayúsculas ASCII, no elimina diacríticos); no es un buscador fonético. Se prueba la lógica, no se ha certificado una latencia determinada con 10.000 pistas.
- La cola creada desde la biblioteca incluye el conjunto filtrado completo mediante una consulta de metadatos ligeros, no solo la página visible. Una playlist o álbum agregado a cola incluye su colección. No se cargan las 10.000 portadas ni las formas de onda en el reproductor.
- La deduplicación compara **bytes exactos**, no huellas acústicas. Dos codificaciones o archivos con etiquetas embebidas diferentes pueden ser pistas distintas.
- El análisis busca el comienzo en los **primeros 120 segundos**. Si no hay sonido sostenido, conserva cero. Si el decodificador falla, la tarea automática conserva cero para no bloquear la biblioteca; se puede reintentar manualmente y ver el error. Los cambios de sensibilidad invalidan la caché de análisis. Los offsets se consultan antes de reproducir y se actualizan en la cola cargada; un inicio original explícito se respeta.
- Crossfade se limita al fragmento reproducible disponible de cada pista, descontando un pequeño margen de seguridad. Se desactiva en repetir-una, reproducción automática desactivada o duración desconocida. Busca continuidad de ganancia, **no sincroniza BPM** ni evita por sí solo clipping en dos señales correlacionadas a máximo nivel.
- La normalización de volumen/LUFS no está implementada y se indica en Ajustes. El ecualizador abre el panel del fabricante si existe. Calidad = archivo original, no una selección de bitrate ficticia.
- No hay descarga de carátulas por Internet. No se solicita `INTERNET` ni `MANAGE_EXTERNAL_STORAGE`.
- El escaneo periódico lo programa WorkManager; Android puede aplazarlo por batería/Doze. El observador de cambios funciona mientras la actividad está visible. Un “Forzar detención” de Android detiene también el servicio: no se intenta eludirlo.
- Permisos SAF revocados, SD retirada o archivos inaccesibles requieren volver a autorizar/restaurar el almacenamiento. Android no permite acceder libremente a `Android/data`.

## Estructura

```text
app/src/main/java/com/streamvault/
├── LuminaApp.kt            # Dependencias compartidas: Room, preferencias, portadas
├── data/                  # Entidades, DAO, consultas paginadas y transacciones
├── scanner/               # MediaStore, SAF, hashes, clasificación, WorkManager
├── analysis/              # Decodificación PCM, RMS, caché y tareas por lotes
├── artwork/               # Miniaturas persistentes y procesamiento de imágenes
├── playback/              # MediaItems, MediaSessionService y curvas de mezcla
├── processing/            # Contrato ML/API y worker de separación desacoplado
├── settings/              # Preferencias persistentes observables
└── ui/                    # Compose, navegación, reproductor y ViewModel
app/src/test/              # Tests JVM de offsets, RMS, ganancias, clasificación
app/src/androidTest/       # Room, MediaStore, PCM real, sesión y crossfade
app/src/main/AndroidManifest.xml
.github/workflows/build.yml
docs/                      # Arquitectura, permisos, validación y extensión ML
```

Las versiones de todas las dependencias están fijadas en `app/build.gradle` y `build.gradle`; Gradle Wrapper 8.9. No hacen falta claves de API ni servicios de pago para las funciones locales.
