# Lúmina 2.1 — correcciones del reproductor

## Qué cambia

- **A continuación** es ahora un panel de altura acotada dentro de la misma ventana del reproductor, con lista desplazable, botones separados y una acción visible para volver. Se abre en la pista actual y conserva la reproducción.
- **Anterior** siempre cambia al canto anterior: ya no reinicia el mismo canto después de tres segundos. **Siguiente** avanza a otra pista y reanuda la reproducción. En los extremos vuelven al otro extremo de la cola. Con una sola pista se muestra una explicación.
- Reproducir desde el menú ya no reemplaza inadvertidamente la cola con una sola canción. Las playlists/contextos elegidos expresamente conservan su selección; reproducir desde el menú general crea una cola con la biblioteca disponible.
- El servicio **espera el análisis del inicio de la pista elegida antes de dejarla sonar**, consulta la información actual de Room, prepara el inicio de la siguiente pista y observa cambios de offsets/transiciones incluso con la cola cargada. No depende únicamente de que WorkManager ya haya terminado.
- Un error de decodificación no se guarda como un análisis exitoso de “0 segundos”. Se distingue en caché y se puede reintentar manualmente. El análisis pendiente se indica en el mini reproductor y en pantalla completa.
- **Inicio y crossfade**, accesible desde el reproductor y el menú de pista, añade final útil y duración de crossfade **por canción**, con persistencia y aplicación a la cola actual.
- Crossfade global: 0, 2, 5, 10, 15, 20, 30, 60, 90, 120 y 180 s. Por pista: cualquier entero de 0 a 180 s, o vacío para heredar el global. Ya no se limita arbitrariamente a la mitad de la canción: solo al audio reproducible disponible.
- Room migra de versión 1 a 2 sin borrar biblioteca, favoritos, nombres, offsets ni playlists.

## Ejemplo: canto que comienza en el segundo once

1. Abre **Inicio y crossfade → Detectar y usar inicio automático**. Este botón quita un inicio manual anterior y activa el inicio automático.
2. Para fijarlo exactamente, escribe **`00:11`** en inicio manual y pulsa **Guardar punto**.
3. `00:11` son once segundos. `0.11` son **110 milisegundos**. Se aceptan `mm:ss`, `hh:mm:ss` o segundos decimales.
4. En Ajustes, “Silencio mínimo: 0” permite detectar entradas de menos de un segundo. La sensibilidad depende del ruido de fondo: la detección RMS no sabe distinguir una introducción intencional, voz suave o ruido de una entrada musical deseada. El punto manual permanece disponible.

El archivo no se recorta. “Reproducir desde el inicio original” ignora el offset de entrada de esa reproducción.

## Ejemplo: archivo de 5:20 cuyo canto termina en 3:20

- **Final útil:** `03:20` (200 segundos).
- **Crossfade de esta pista:** `10`.
- La mezcla comienza aproximadamente en `03:10` y abandona la pista en `03:20`; no espera los dos minutos restantes.
- Si realmente quieres mezclar durante dos minutos, elige `120` y deja el final donde quieras terminar. La duración se acota si las pistas no tienen suficiente audio.
- Vaciar el final y restablecer la transición vuelve al final original y al crossfade global.

**El final útil es manual.** No se ha implementado un detector automático del fin musical: un pasaje suave o un silencio en medio de la canción no debe hacer que se corte arbitrariamente.

## Quitar voz: límite no resuelto con estos cambios

Esta actualización **no incluye un modelo de separación vocal** ni configura un servidor. Por eso no promete generar instrumentales ni reproducir el original con un nombre distinto. El menú y la pantalla ahora indican explícitamente que se necesita un motor instalado, y que escuchar el original no elimina la voz. Sigue disponible la arquitectura de separación, pero falta conectar y validar un modelo/API real. Ver `SOURCE_SEPARATION.md`.

## Actualización de una instalación existente

La migración de base de datos se aplica al actualizar con **la misma firma Android**. Los APK debug producidos en runners distintos pueden tener firmas diferentes. Si Android rechaza la actualización por firma, **no desinstales sin respaldar tus datos**. Los archivos originales externos no los borra Lúmina, pero desinstalar elimina sus datos privados (playlists, nombres personalizados, portadas, preferencias y derivados).

Para distribuir actualizaciones que preserven los datos se necesita conservar y utilizar la misma clave de firma. La entrega no contiene una clave de producción, y no se pide compartir claves/contraseñas en el chat.
