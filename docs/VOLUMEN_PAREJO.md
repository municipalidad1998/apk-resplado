# Volumen parejo · nivelación de volumen por canción

Problema que resuelve: hay canciones que suenan mucho más bajas que otras. La app mide el
volumen **real** de cada archivo y aplica una corrección al reproducirlo. El archivo original
nunca se modifica, no se re-codifica y no se generan copias.

## Cómo funciona

1. **Medición.** `LoudnessAnalyzer` decodifica el audio con `MediaCodec` (el mismo camino PCM que
   usa el detector de silencio) y calcula el nivel RMS en dBFS de una ventana de 90 segundos,
   empezando en el inicio detectado de la canción, para no medir el silencio inicial.
2. **Almacenamiento.** El resultado se guarda en `tracks.loudnessDb` (base de datos, migración
   2 → 3). Nada se escribe en el archivo de audio.
3. **Corrección al reproducir.** `LoudnessMath` calcula `ganancia = nivelObjetivo − medido`:
   - **Positiva** (canción baja): se amplifica con `LoudnessEnhancer`, el efecto de audio del
     sistema, hasta **+15 dB**.
   - **Negativa** (canción fuerte): se baja el volumen del reproductor hasta un mínimo de 0,05.
   - **Sin medición**: no se toca nada. Nunca se inventa una ganancia.

La corrección se combina con los fades y el crossfade, así que una transición suave se mantiene
suave: el volumen del reproductor siempre es `gananciaDeMezcla × atenuaciónDeNivelación`.

## Dónde se activa

- **Ajustes → Audio → Volumen parejo** (activado por defecto).
- **Ajustes → Audio → Nivel objetivo**: −22, −20, −18, −16 (predeterminado), −14 o −12 dBFS RMS.
  Un objetivo más alto suena más fuerte en general y deja menos margen.
- **Ajustes → Audio → Medir el volumen de tu biblioteca**: mide todas las canciones pendientes en
  segundo plano (también ocurre durante el análisis normal de silencios).
- **Menú de la canción → Medir volumen y nivelar**: mide una sola pista al instante.
- El reproductor muestra `Volumen parejo · +3.5 dB`, `−6.0 dB`, `nivelado` o `sin medir`.

## Límites honestos

- Es **RMS de una ventana**, no loudness LUFS con ponderación K como en radio o streaming. Dos
  canciones con el mismo RMS pueden percibirse algo distinto si su timbre difiere mucho.
- Solo se leen **90 segundos** desde el inicio real de la canción. Una pista con una introducción
  muy suave y un estribillo muy fuerte puede quedar sobre-amplificada en el estribillo.
- La amplificación depende de que el dispositivo traiga el efecto `LoudnessEnhancer`. La gran
  mayoría de teléfonos Android lo incluyen; si no está, la app solo puede **bajar** las canciones
  fuertes, no subir las bajas.
- El tope de +15 dB es deliberado: más allá de eso se amplifica también el ruido del archivo.
- No hay compresión de rango dinámico ni limitador: una grabación con picos extremos puede
  distorsionar si se le pide mucha ganancia.
