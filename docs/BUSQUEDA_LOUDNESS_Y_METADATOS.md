# Búsqueda inteligente, loudness profesional y metadatos (versión 2.6.0)

## 1. El buscador ya no devuelve cualquier cosa

El objetivo es que `Alex Campos` muestre a Alex Campos, y que `Tu poeta Alex Campos` ponga esa
canción primero aunque esté escrita sin guion.

Cómo se ordena (`search/SmartSearch.kt`):

| Prioridad | Puntuación | Caso |
| --- | --- | --- |
| 1 | 100 | Artista **y** título exactos, aunque la consulta no traiga separador |
| 2 | 95 / 94 | Título exacto, o artista exacto y título que empieza igual |
| 3 | 90 | Artista exacto |
| 4 | 80 | Artista y título contenidos en la consulta |
| 5 | 70 | Álbum exacto |
| 6 | 60 / 52 | Empieza por, o contiene la frase completa como palabra |
| 7 | 58 | Colaboraciones (`feat.` / `ft.`) donde aparece el artista buscado |
| 8 | 46…20 | Todas las palabras en orden, o parte de ellas |
| 9 | 38…30 | Coincidencia con errores de escritura (distancia de Levenshtein) |

Reglas que hacen falta para que eso funcione:

- **Normalización:** minúsculas, sin acentos y sin puntuación, así que `Tú Poeta`, `tu poeta` y
  `TU POETA` son lo mismo.
- **Separadores:** `-`, `–`, `:`, `|`, ` de `, `del `, ` by `, `feat` y `ft` separan artista de
  canción. Si no hay ninguno, se prueban todos los cortes posibles entre palabras.
- **Ruido fuera:** si algún resultado es fuerte (60 o más), los resultados débiles dejan de
  mostrarse. Por eso una canción que sólo comparte una palabra no aparece por encima.

**Local antes que Internet:** el buscador consulta primero la biblioteca del teléfono y sólo
después el proveedor online, y ambos se ordenan con las mismas reglas. En **Explorar** verás una
sección *En tu teléfono* y otra *También en Internet*.

Lo que no puede hacer: si la canción no está en tu teléfono ni en el catálogo libre integrado, no
aparece. El catálogo comercial (éxitos de grandes sellos) sólo existe en el reproductor oficial de
YouTube o con licencias de pago.

## 2. Normalización de loudness de verdad

Antes se medía RMS. Ahora se mide **LUFS con ITU-R BS.1770-4** (`analysis/LoudnessMeter.kt`):

- **Curva K** con los dos filtros del estándar (estante agudo + paso alto RLB). Los coeficientes se
  derivan del prototipo analógico con una transformada bilineal, así que la curva es correcta a
  cualquier frecuencia de muestreo: en 48 kHz reproduce exactamente los coeficientes publicados.
- **Bloques de 400 ms con 75 % de solape** y las dos compuertas: absoluta en −70 LUFS y relativa
  10 LU por debajo del promedio. Por eso los silencios largos no bajan la medición.
- **Pico verdadero** estimado con sobremuestreo ×4, en dBTP.
- **Objetivo configurable:** −20, −18, −16, −14 (predeterminado) o −12 LUFS en
  *Ajustes → Audio → Loudness objetivo*.
- **Limitación por pico:** si una canción necesita +6 dB pero su pico está a −3 dBTP, la ganancia
  se queda en lo que quepa por debajo de **−1 dBTP**. Nunca se recorta.
- **El archivo no se toca:** la ganancia se aplica al reproducir (`LoudnessEnhancer` para subir,
  volumen lineal para bajar, y el compresor con limitador cuando está activo).

En el reproductor verás: `Original −20.1 LUFS → −14 LUFS · +6.0 dB` y
`Pico real −0.6 dBTP · tope −1.0 dBTP`.

Referencia para comprobarlo: un seno de 1 kHz a escala completa mide **−3,00 LUFS** (media
cuadrática 0,5 → −3,01 dBFS, más 0,70 dB de la curva K y el offset −0,691 del estándar). Ese valor
está fijado en las pruebas unitarias.

## 3. FLAC y metadatos

- FLAC, WAV, ALAC, MP3, AAC, OGG y Opus se reproducen **sin convertir**; el FLAC suena como es.
- El reproductor lee el contenedor y muestra `FLAC · 24-bit / 96 kHz · Lossless`, con bitrate,
  canales, sample rate y duración. La profundidad de bits se calcula desde el bitrate cuando el
  contenedor no la expone.
- **Editar información** ahora incluye título, artista, álbum, género, **año, número de pista y
  disco**, además del nombre personalizado, etiquetas y notas. Todo se guarda en la base de datos:
  el archivo original permanece intacto.
- El escáner lee las etiquetas del archivo (incluidos pista y disco) y respeta lo que edites a mano
  en escaneos posteriores.

## 4. Música de Telegram

*Ajustes → Telegram → Importar música de Telegram* lee el respaldo que exporta Telegram
(`result.json`) y guarda en SQLite, por cada audio:

- id del mensaje, id de fichero y hash
- nombre, artista y álbum
- duración, tamaño y fecha

Las canciones que siguen en el teléfono se enlazan con su copia local y se reproducen como el resto
de la biblioteca. Las que ya no estén conservan sus datos para cuando vuelvas a tener el archivo.

**Límite honesto:** reproducir directamente desde los servidores de Telegram requiere un cliente
MTProto con las credenciales de API del propio usuario (api_id y api_hash, más el inicio de sesión
con tu número). Sin eso ninguna app puede transmitir esos archivos. La importación sí deja todo
listo y la música registrada.
