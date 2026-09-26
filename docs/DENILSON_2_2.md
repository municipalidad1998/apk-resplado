# Reproductor de música Denilson · versión 2.2.0

Guía de lo que cambió respecto a Lúmina 2.1 y de los límites reales de cada función.

## 1. Nombre de la app

El launcher muestra **Reproductor de música Denilson** (`@string/app_name`).

El identificador interno sigue siendo `com.streamvault` a propósito: si cambiara, Android lo
trataría como otra aplicación y no permitiría actualizar por encima de la versión instalada.

## 2. Actualizaciones automáticas

La app consulta la API de GitHub Releases de este repositorio
(`https://api.github.com/repos/municipalidad1998/apk-resplado/releases/latest`) y compara el
`tag_name` con la versión instalada.

- **Revisión automática:** al abrir la app, como máximo una vez cada 6 horas y solo si el
  ajuste «Buscar automáticamente» está activo. No interrumpe la reproducción: si hay versión
  nueva aparece un aviso abajo y nada más.
- **Revisión manual:** Ajustes → Actualizaciones → «Buscar actualizaciones».
- **Instalación:** al tocar «Descargar e instalar» se descarga el APK publicado y se abre el
  instalador oficial de Android. Android siempre muestra su propia confirmación; nada se
  instala en silencio.
- **Permiso previo:** Android 8+ exige autorizar una vez «Instalar apps desconocidas» para la
  app que hace la descarga. Si falta, la app abre la pantalla de ajustes correspondiente y
  luego basta con «Reintentar».
- **Datos conservados:** la biblioteca, las colas y los ajustes se guardan en la base local, así
  que actualizar no los borra.

### Firma estable (clave para poder actualizar)

Android solo deja instalar una actualización si está firmada con la misma clave que la versión
instalada. Para lograrlo el flujo de CI usa un keystore propio:

1. Si existe el secreto `KEYSTORE_BASE64` del repositorio, lo usa.
2. Si no, usa `keystore/lumina.jks`; si el archivo no existe lo genera y lo guarda en la rama.

**Aviso de seguridad:** mientras el keystore viva dentro del repositorio, cualquiera con acceso
de escritura al repositorio podría firmar apps con esa identidad. Si vas a publicar esto más
allá de tus propios teléfonos, genera un keystore propio, guárdalo como secreto
`KEYSTORE_BASE64` y borra `keystore/lumina.jks`.

## 3. Botón YouTube: qué hace y qué no

En el reproductor aparecen **YouTube** y **YouTube Music**, y en el menú de cada canción
**Buscar en YouTube**. Los tres construyen una búsqueda con título y artista y la abren en la
app oficial de YouTube (o en el navegador, si no está instalada).

Lo que **no** hace, y no se puede hacer desde esta app:

- No bloquea ni salta anuncios de YouTube o YouTube Music.
- No reproduce audio de YouTube dentro del reproductor.
- No reproduce YouTube con la pantalla apagada.

Las tres cosas dependen de la app oficial y de YouTube Premium; saltarse los anuncios o la
reproducción en segundo plano incumple los términos de servicio de YouTube. La reproducción en
segundo plano **sí** funciona, y sin interrupciones, con los archivos locales de tu teléfono.

## 4. Diseño

- Paleta dinámica de Android 12+ (se adapta al fondo de pantalla) con el morado/menta propio
  como alternativa; se puede activar en Ajustes → Apariencia → «Colores del sistema».
- Formas más redondeadas y tipografía con jerarquía propia.
- Mini reproductor con esquinas de 18 dp y la misma paleta del reproductor completo.

Se mantienen las correcciones de la versión 2.1: cola editable «A continuación», anterior y
siguiente reales, inicio inteligente con offsets manuales, final útil y crossfade por canción
de 0 a 180 segundos.

## 5. Limitaciones honestas

- La separación de voz sigue sin modelo instalado; la función aparece marcada como no
  disponible en lugar de simular un resultado.
- No hay detector automático del final musical: el final útil se elige a mano.
- No se probó en un teléfono físico ni con Bluetooth, tarjeta SD ni bibliotecas de 10 000
  canciones; la verificación fue en emulador con Android 15.
- Si tenías instalada una compilación firmada con otra clave (por ejemplo, la 2.1 generada
  antes de fijar la firma), Android pedirá desinstalarla primero: haz respaldo antes.
