# Música online, calidad y FLAC (versión 2.5.0)

## La decisión importante: de dónde sale la música online

No existe una API gratuita y legal que ofrezca el catálogo comercial (los éxitos de las grandes
discográficas). Ese catálogo sólo se puede reproducir pasando por el reproductor oficial de
YouTube o pagando licencias. Por eso la app hace dos cosas distintas:

1. **Módulo `youtube/` aislado.** Abre la canción, el artista o el álbum en la **app oficial** de
   YouTube o YouTube Music. No bloquea anuncios, no reproduce YouTube en segundo plano y no usa
   técnicas para saltarse nada: eso lo prohíben sus términos y depende de YouTube Premium.
2. **Módulo `online/` con proveedor intercambiable.** La búsqueda y la reproducción online usan la
   interfaz `OnlineProvider`, así que cambiar de fuente no toca el reproductor. La implementación
   incluida es **Internet Archive**: JSON público, sin API key, sin cuenta, y con licencias que
   permiten la distribución (Live Music Archive, netlabels, dominio público, conciertos).

Qué significa esto en la práctica: sí puedes buscar por canción, artista, álbum, concierto,
instrumental o versión, y escucharla en FLAC; **pero el catálogo es libre, no comercial**. Si
buscas un éxito actual de un gran sello, no va a aparecer.

## Cómo funciona la reproducción online

1. La búsqueda consulta `advancedsearch.php` y, para cada ítem, su `metadata` para conocer los
   archivos de audio.
2. Cada resultado se convierte en una pista real de la biblioteca (`source = "online"`) con su URL,
   portada y duración. Por eso funciona con la **misma cola, favoritos, playlists, historial y
   notificación** que tus archivos locales.
3. `QualityChooser` elige el archivo según la calidad pedida y la conexión: FLAC o el mejor
   disponible con Wi‑Fi, calidad media con datos móviles y la más baja en conexiones lentas.
4. Media3/ExoPlayer reproduce el stream con la misma MediaSession y el mismo servicio en primer
   plano: **pantalla apagada, bloqueo, Bluetooth y controles del sistema incluidos**.
5. Si la conexión se corta, el reproductor avisa y **reanuda solo** cuando vuelve el Internet.

Ajustes relacionados: **Ajustes → Datos y calidad** (usar datos móviles, solo Wi‑Fi, calidad
online) y el indicador de estado en la pantalla **Explorar** (conectado / lento / sin conexión).

## FLAC y formatos sin pérdida

- Se detectan y reproducen `.flac`, `.wav`, `.mp3`, `.m4a`, `.aac`, `.ogg` y `.opus`.
- El archivo se reproduce **tal cual**: nunca se convierte a MP3 ni se re-codifica.
- El reproductor lee el contenedor con `MediaExtractor` y muestra
  **códec · bit depth · sample rate · Lossless**, por ejemplo `FLAC · 24-bit / 96 kHz · Lossless`.
  La profundidad de bits se calcula a partir del bitrate cuando el contenedor no la expone.
- La música local funciona **sin Internet**, con la misma cola y las mismas listas.

## Lo que sí cumple y lo que no

| Pedido | Estado |
| --- | --- |
| Buscador online de canciones, artistas, álbumes, conciertos, versiones | Sí, sobre catálogo libre |
| Reproducción por Internet con portada, título, artista y progreso | Sí |
| Pantalla apagada, bloqueo, Bluetooth, notificación y segundo plano | Sí (Media3 + MediaSession + servicio en primer plano) |
| Sin anuncios propios en la app | Sí: la app no inserta anuncios en ninguna calidad |
| FLAC local sinconvertir, con datos técnicos | Sí |
| Calidad automática / baja / normal / alta según la conexión | Sí |
| Cola, playlists, favoritos, historial, mini reproductor | Sí, compartidos entre online y local |
| Catálogo comercial tipo YouTube Music (éxitos de grandes sellos) | **No**: ninguna API legal gratuita lo ofrece |
| Quitar los anuncios de YouTube o reproducir YouTube en segundo plano | **No**: lo prohíben sus términos |
| Doblaje de voces a otros idiomas | **No**: requiere modelos de voz que no hay en el teléfono |
