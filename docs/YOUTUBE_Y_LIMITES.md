# YouTube dentro de la app: qué se puede hacer y qué no

Fecha: 2026-09-26 · Versión: 2.8.0

## 1. Lo que se implementó

| Pieza | Cómo funciona | Por qué es legítimo |
| --- | --- | --- |
| **Búsqueda** | `YouTubeProvider` llama a la **YouTube Data API v3** (`/youtube/v3/search` con `type=video&videoCategoryId=10`, y `/youtube/v3/videos` para la duración). | Es la API oficial y pública de Google. La clave la pone el usuario en **Ajustes → YouTube**; la app no trae ninguna clave incrustada. |
| **Reproducción** | `YouTubePlayerScreen` muestra el **reproductor oficial de YouTube** (`youtube.com/embed/<id>` con la IFrame Player API) dentro de la app, en un `WebView`. **No abre otra ventana ni otra app.** | El reproductor incrustado es la forma prevista por Google para reproducir YouTube fuera de su web. |
| **Pantalla completa** | `WebChromeClient.onShowCustomView` monta la vista nativa de vídeo a pantalla completa. | Comportamiento estándar del reproductor oficial. |
| **YouTube Music** | Botón que abre el enlace oficial `music.youtube.com/watch?v=<id>`. | **No existe API pública de YouTube Music.** Lo que circula (InnerTube, `ytmusicapi`) son clientes no autorizados que se hacen pasar por la app oficial: no se usan. |
| **Sin conexión / segundo plano** | El reproductor se pausa al salir de la app o al irse a segundo plano (`WebView.onPause()` con el ciclo de vida). | Es lo que exige el reproductor oficial: el audio en segundo plano es función de YouTube Premium. |

Resultado: buscas «Alex Campos», y en la pestaña **ONLINE** ves los vídeos de YouTube junto a los resultados
de música libre; tocas uno y **suena dentro de la app** sin cambiar de ventana.

## 2. Lo que NO se implementó, y por qué

### Bloqueador de anuncios

* Los **Términos del Servicio de YouTube** prohíben bloquear, ocultar o saltar los anuncios del
  reproductor. Hacerlo desde dentro de una app es exactamente el mecanismo que persiguen
  judicialmente las plataformas y, en la práctica, Google lo rompe cada pocas semanas.
* En tus propias instrucciones escribiste: *«no implementar mecanismos destinados a evadir
  restricciones de acceso, DRM o controles de una plataforma»* y *«no bloquea anuncios ni reproduce
  YouTube en segundo plano; eso depende de YouTube Premium»*.
* Técnicamente existen tres variantes y las tres son inaceptables aquí:
  1. **Ocultar el contador de anuncios o pulsar “Saltar” automáticamente** en el WebView →
     manipulación del reproductor oficial (ToS).
  2. **Extraer el stream y reproducirlo sin anuncios** en ExoPlayer → evasión técnica, la más
     perseguida (es lo que hacen los clones de NewPipe/YouTube Vanced).
  3. **Bloqueo por DNS o hosts en el sistema** → mismo resultado, distinto sitio; la app no va a
     configurar el sistema del usuario para eso.

**La única vía legítima para escuchar YouTube sin anuncios y con la pantalla apagada es
YouTube Premium.** Si algún día quieres una app sin anuncios de verdad, el camino es usar fuentes
que nacen libres: la pestaña ONLINE ya incluye **Internet Archive** (música libre, netlabels y
dominio público), y se pueden añadir **Jamendo** o **Free Music Archive** (Creative Commons, con
clave de API gratuita) si te interesa.

### Descargas de YouTube

Descargar el audio para escucharlo sin conexión está prohibido salvo con Premium (o para el
creador del contenido). No se implementa, y tampoco se descarga nada automáticamente para
sustituir una canción local.

### «Selección de calidad» y «selección de pista de audio»

Con el reproductor incrustado, **la calidad la elige YouTube dentro de su propio reproductor**
(la API de IFrame expone `getAvailableQualityLevels()` pero fijar la calidad por debajo de lo que
decide el reproductor, o silenciar sus anuncios, es manipularlo). Para el resto de fuentes
(Internet Archive, tus FLAC) la calidad sí se elige en la app, porque ahí somos nosotros los que
reproducimos el archivo.

**Sí está disponible, y es legítimo, en la música que reproduce nuestra app**: velocidad,
ecualizador, fundidos, normalización LUFS, cola, playlists, favoritos y pantalla completa.

## 3. Qué pasa si no pones la clave

Nada se rompe: el proveedor de YouTube se queda en silencio y la pantalla de búsqueda lo dice
(«Para buscar en YouTube falta tu clave…»). El resto de fuentes sigue funcionando igual.

## 4. Cómo conseguir la clave

1. Entra en <https://console.cloud.google.com/> con tu cuenta de Google.
2. Crea un proyecto (o usa uno existente) → **APIs y servicios → Biblioteca** → habilita
   **YouTube Data API v3**.
3. **Credenciales → Crear credencial → Clave de API**. Restrínsela a «YouTube Data API v3» y,
   si quieres, a Android con el paquete `com.streamvault`.
4. Pega la clave en **Ajustes → YouTube → Clave de YouTube Data API v3** y pulsa **Guardar clave**.

La cuota gratuita son 10 000 unidades al día; una búsqueda gasta 100, así que hay margen de sobra
para uso personal. La clave se guarda en los ajustes del teléfono y se incluye en el respaldo de
configuración.

## 5. Resumen honesto

| Lo que pediste | Estado |
| --- | --- |
| YouTube dentro de la app, sin otra ventana | ✅ Hecho con la API oficial y el reproductor oficial |
| YouTube Music | ⚠️ Solo enlace oficial: **no existe API pública** |
| Bloqueador de anuncios | ❌ No se implementa: incumple los ToS y tus propias reglas. Alternativa legal: YouTube Premium o fuentes libres |
| Calidad / pista de audio en YouTube | ❌ Lo decide el reproductor oficial; ✅ sí en tu música y en Internet Archive |
| Segundo plano / PiP con YouTube | ❌ Requiere Premium; ✅ sí con tu música local |
