# Análisis del repositorio de referencia y del buscador

Fecha: 2026-09-26 · Versión: 2.7.0

## 1. Qué es realmente `shreybaviskar/YouTube-Premium-IPA`

Es un **fork de [YTLite](https://github.com/dayanch96/YTLite) ("YouTube Plus")**, un *tweak* de
jailbreak para iOS. No es una app: es un conjunto de parches en Objective‑C/Logos que se inyectan
**dentro de la app oficial de YouTube ya instalada**.

Estructura real del repositorio:

| Archivo | Qué hace |
| --- | --- |
| `Makefile` | Compila con THEOS (`TWEAK_NAME = YTLite`, `TARGET := iphone:clang:16.5:13.0`). Se enlaza contra UIKit y se instala como tweak de Cydia/Sileo. |
| `YTLite.h` | Cabecera con las clases privadas de YouTube que se van a parchear (`YTSettingsSectionItemManager`, `YTPivotBarViewController`, `YTColdConfig`, `YTMainAppControlsOverlayView`…). |
| `YTLite.x` | ~1.400 líneas de `%hook` sobre esas clases privadas. |
| `Settings.x` | Construye el panel de ajustes dentro de los Ajustes de YouTube. |
| `YTNativeShare.x`, `Sideloading.x` | Detalles de integración con el instalador. |
| `Resources/`, `layout/…/YTLite.bundle` | Imágenes y traducciones. |

Mecanismos que usa, agrupados por tipo:

1. **Eliminación de anuncios y de secciones** (`YTIElementRenderer.elementData`,
   `YTAdsInnerTubeContextDecorator`, `YTSectionListViewController`): devuelve `nil` o un `NSData`
   vacío cuando el renderer corresponde a `brand_promo`, `text_search_ad`, `feed_ad_metadata`, etc.
2. **Falsificación de estado Premium** (`YTIPlayabilityStatus.isPlayableInBackground`,
   `MLVideo.playableInBackground`, `YTIFormattedString` del logo Premium): hace creer a la app que
   la cuenta tiene las funciones de pago.
3. **Valores de configuración interna** (`YTColdConfig`): activa switches experimentales
   (barra persistente, ocultar Shorts, zoom, gestos).
4. **Reproductor** (`YTVarispeedSwitchController` para velocidades extra,
   `YTVideoQualitySwitchControllerFactory` para calidad clásica, overlay y minibar).
5. **Descargas** (`YTNativeShare.x`, botón de descarga en el menú del reproductor).

### 1.1 Por qué **no** se puede reutilizar como está

* Es **Objective‑C/Logos para iOS**, inyectado en un binario de terceros. En Android no existe
  equivalente legal: parchear la app de YouTube viola sus condiciones de servicio y, en la
  práctica, equivale a evadir los controles de acceso de la plataforma.
* Sus funciones centrales (quitar anuncios, reproducir en segundo plano, descargar) son
  exactamente las que solo corresponden a **YouTube Premium**.
* El usuario lo pidió expresamente así: *"no copies código propietario ni dependas de modificar la
  aplicación oficial de YouTube"* y *"no implementar mecanismos destinados a evadir restricciones de
  acceso, DRM o controles de una plataforma"*.

### 1.2 Qué **sí** se toma como referencia (ideas, nunca código)

| Idea del tweak | Implementación propia en esta app |
| --- | --- |
| Panel de ajustes organizado por secciones (General, Navbar, Overlay, Player) | `SettingsScreen` con secciones: apariencia, reproducción, loudness, crossfade, biblioteca, Telegram, actualizaciones. |
| Velocidades extra y calidad elegible por el usuario | Velocidad de reproducción + `Quality` (Automática/Baja/Normal/Alta) en la pantalla de búsqueda; la calidad se aplica al elegir el stream. |
| Recordar y restaurar ajustes (respaldo) | `SettingsBackup` (exportar/importar JSON) ya disponible en Ajustes. |
| Reproductor con minibar, cola y pantalla completa | `MiniPlayer` + `FullPlayer`, cola editable, "A continuación". |
| Gestos y controles avanzados | Pendiente para la siguiente iteración (gestos sobre la carátula y el deslizador). |
| Firma estable para poder actualizar | `keystore/lumina.jks` versionado + flujo de CI que reutiliza la misma clave (ver `FIRMA_Y_ACTUALIZACIONES.md`). |

## 2. Por qué el buscador devolvía resultados incorrectos

Diagnóstico sobre el código anterior (versión 2.6.0 y anteriores):

1. **La preselección SQL era demasiado permisiva.** La consulta unía los términos con `OR` sobre
   seis columnas (`title`, `customName`, `artist`, `album`, `genre`, `fileName`). Una sola palabra
   coincidente en el género o en el nombre del archivo bastaba para meter la canción en la lista de
   candidatos.
2. **La puntuación premiaba una sola palabra.** El tramo "cobertura" daba
   `20 + 14 × palabras_coincidentes / total`. Con la consulta «Alex Campos» (2 palabras), una
   canción que solo contiene «Alex» obtenía **34 puntos** y superaba el suelo de 25, así que se
   mostraba.
3. **El prefijo del artista era demasiado generoso.** `artist.startsWith(query)` daba 60 puntos a
   «IBI Town» quando se buscaba «La IBI»: con eso el resultado parecía una coincidencia fuerte.
4. **No había una regla de exclusión por artista.** Aunque existiera una coincidencia exacta, las
   coincidencias débiles se mantenían en la lista, mezcladas y solo ordenadas por puntos.
5. **Las fuentes se mezclaban en la misma pantalla.** Los resultados online se guardaban en la
   misma tabla `tracks` y aparecían en la biblioteca «local», sin distintivo de origen.

## 3. Cómo se corrigió (2.7.0)

### 3.1 Puntuación (`search/SmartSearch.kt`)

| Nivel | Puntos | Ejemplo |
| --- | --- | --- |
| Artista **y** título exactos | 100 | «Tu poeta Alex Campos» → *Tu poeta* de Alex Campos |
| Título exacto | 95 | «Al taller del maestro» |
| Artista exacto (con o sin artículo) | 92 | «La IBI» o «IBI» |
| Artista exacto + título que empieza igual | 94 | versión en vivo del mismo tema |
| Artista acreditado entre varios | 84 | «Alex Campos, Marcos Witt» |
| Otra versión del mismo título | 88 | «Al taller del maestro (en vivo)» |
| Artista + título por partes | 80 | |
| Álbum exacto | 70 | |
| Artista que empieza por la consulta | 60 | |
| Colaboración «feat» | 58 | |
| Palabra completa en título o artista | 52 | |
| Palabras en orden / cobertura | 34–46 | |
| Erratas (Levenshtein) | 30–38 | «alex campo» |

### 3.2 Reglas de exclusión nuevas

* **Regla del artista exacto.** Si algún candidato coincide exactamente con el artista buscado,
  solo se muestran canciones de **ese** artista. «Alexander Acha», «Campos Verdes» o «Alex» dejan
  de aparecer al buscar «Alex Campos».
* **Regla del título exacto.** Si una canción coincide exactamente, solo se muestran ella y sus
  versiones (títulos que empiezan igual). «Al taller del maestro» ya no arrastra canciones que
  solo comparten una palabra.
* **Una sola palabra ya no basta.** En consultas de dos o más palabras, el tramo de cobertura
  exige al menos dos coincidencias de palabra completa.
* **Artículo opcional.** «La IBI» y «IBI» son el mismo artista a efectos de búsqueda; «IBI Town» o
  «IBIS» no.
* **Artista acreditado.** «Alex Campos, Marcos Witt» cuenta como Alex Campos, pero por debajo de la
  coincidencia exacta.

### 3.3 Separación real de fuentes (`provider/`)

```
MusicSearchEngine
 ├── LocalMusicProvider  → Room (solo archivos que existen, source != 'online'), sin red
 └── OnlineMusicProvider → OnlineRepository → ArchiveProvider, validado y solo con conexión
```

* Cada resultado es un `SearchHit` con `source = LOCAL | ONLINE`: la UI no puede confundirlos
  porque el origen viaja **dentro del dato**, no solo en la pantalla.
* La biblioteca del teléfono y las listas de facetas (artistas, álbumes, carpetas, géneros)
  excluyen `source = 'online'`: una canción de Internet nunca se cuela en «Música del teléfono».
* Sin conexión, el motor responde igual con la parte local y avisa en la parte online
  («Sin conexión a Internet»). La música local, las playlists locales y los favoritos siguen
  funcionando.

### 3.4 Pantalla de búsqueda (`ui/SearchScreen.kt`)

Pestañas **`[TELÉFONO] [ONLINE] [TODOS]`**:

* **TELÉFONO**: solo archivos del dispositivo, con distintivo 📱 y carpeta de origen.
* **ONLINE**: solo resultados de Internet, con distintivo 🌐 y proveedor.
* **TODOS**: las dos listas, una debajo de la otra, con su propia cabecera y su contador. Nunca se
  fusionan en una sola fila.

### 3.5 Validación de metadatos online (`online/ResultValidator.kt`)

Ningún resultado se muestra sin pasar por el validador: se descartan los que no traen título ni
stream reproducible, y se rellenan artista, álbum, duración e identificador. Cada fila muestra
título, artista, álbum, duración, carátula y origen.

### 3.6 Playlists separadas (base de datos v5)

La columna `playlists.kind` se calcula a partir de su contenido: `local`, `online` o `mixed`.
La pantalla de playlists permite filtrar por tipo y muestra el distintivo en cada lista. Una
canción online añadida a una playlist local **no se descarga**: suena por Internet y la lista pasa a
ser mixta.

## 4. Verificación

Pruebas nuevas en `app/src/test/java/com/streamvault/`:

* `search/SmartSearchRelevanceTest` — los cuatro casos del usuario («Alex Campos», «La IBI»,
  «Tu poeta Alex Campos», «Al taller del maestro») más alias, erratas, artista acreditado y la
  regla «una sola palabra no basta».
* `search/MusicSearchEngineTest` — las fuentes nunca se mezclan, sin conexión solo responde el
  teléfono, un fallo online no borra los resultados locales.
* `online/ResultValidatorTest` — la validación de metadatos.
* `VersioningTest` — el `applicationId` no cambia y el `versionCode` crece.

Comportamiento pendiente de comprobar en un teléfono real (no reproducible en el emulador de CI):
búsqueda sobre una biblioteca grande con metadatos incompletos y reproducción online con datos
móviles.
