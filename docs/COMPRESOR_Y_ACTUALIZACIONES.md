# Compresor, instalación de actualizaciones y respaldo

Guía de la versión 2.4.0. Las tres cosas que se pidieron: un solo volumen estándar con
compresor, que la actualización se pueda instalar desde la propia app, y que la configuración
sobreviva a una reinstalación.

## 1. Compresor: un volumen estándar, no el volumen del teléfono

La app ya nivelaba el volumen entre canciones (versión 2.3.0). Lo que faltaba era equilibrar
**dentro** de una misma canción, como el *Efecto → Compresor* de Audacity.

- Se aplica con `DynamicsProcessing`, el efecto de audio oficial de Android (Android 9 / API 28+).
- Un **limitador a −1 dB** hace el papel de *Normalizar a −1 dB*: permite subir el volumen sin
  que los picos se recorten.
- Se procesa **al reproducir**, en tiempo real. El archivo original nunca se modifica.

### Ajustes

**Ajustes → Audio → Compresor**

| Opción | Uso |
| --- | --- |
| Desactivado | Solo la nivelación entre canciones de la 2.3.0 |
| Suave | 2,5:1 desde −24 dB. Controla picos sin cambiar el carácter |
| Equilibrado | 4:1 desde −28 dB. Punto de partida recomendado |
| Fuerte | 8:1 desde −32 dB. **Cantos religiosos y grabaciones en vivo** |
| Voz y predicación | 12:1 desde −34 dB. Voces grabadas con eco o ruido de sala |

La ganancia final es la del preset más lo que la canción necesite para llegar al **nivel
objetivo** (−16 dBFS por defecto), con un tope de +24 dB. Cuando el compresor ya está subiendo
la señal, el amplificador anterior se apaga para no aplicar la misma ganancia dos veces.

### Límites

- Requiere **Android 9 o superior**. En versiones anteriores solo se atenúa.
- No es un compresor multibanda ni un limitador de masterización: busca que se entienda la voz y
  que los picos no molesten, no un sonido de estudio.
- Un preset muy fuerte sobre una grabación con mucho ruido de sala también sube ese ruido en los
  silencios. Si notas eso, baja a *Equilibrado*.

## 2. Instalar la actualización desde la app sin errores

### Qué hacía fallar la instalación

Android muestra *«ya existe una aplicación similar»* o *«no se instaló»* por tres motivos
distintos, y cada uno tiene una solución diferente:

| Causa | Qué hace la app ahora |
| --- | --- |
| **Firma distinta** a la versión instalada | Lo detecta **antes** de abrir el instalador comparando los certificados y explica que hay que desinstalar una vez. Desde la 2.2 todas las versiones comparten la misma firma, así que solo pasa con la 2.1 o anteriores. |
| **Origen desconocido** no autorizado | Abre directamente el ajuste *Instalar apps desconocidas* para que lo autorices una vez. |
| **Play Protect** bloquea el APK | Mensaje del sistema; hay que tocar *Instalar de todos modos*. Al no venir de Google Play, Android avisa siempre. |

### Cómo se instala ahora

La descarga se entrega al **PackageInstaller** de Android (la misma API que usa Play), no a un
`Intent` genérico. La ventaja es que Android devuelve un motivo concreto: si responde
`STATUS_FAILURE_CONFLICT`, la app lo traduce a un mensaje en español y ofrece desinstalar la
versión anterior en lugar de dejarte con un error sin explicación.

Pasos recomendados la primera vez:

1. **Ajustes → Actualizaciones → Buscar actualizaciones.**
2. Toca **Descargar e instalar**.
3. Si Android pide autorizar *Instalar apps desconocidas*, actívalo para esta app y vuelve.
4. Android mostrará su propia confirmación. Confirma y listo: la biblioteca, las colas y los
   ajustes se conservan.

Si vienes de la **2.1 o anterior**, la firma cambió: guarda tus ajustes (sección 3), desinstala y
vuelve a instalar. A partir de esa instalación, las siguientes se actualizan solas.

## 3. Que la configuración sobreviva

Tres mecanismos, del más automático al más manual:

1. **Actualizar sin desinstalar** conserva todo: los datos viven en la base interna de la app.
2. **Respaldo de Android:** el APK declara `allowBackup="true"` con reglas que incluyen la base de
   datos `lumina-library.db` y las preferencias. Al reinstalar con la misma cuenta, Android puede
   restaurarlos. Depende de la copia de seguridad del sistema.
3. **Respaldo manual:** **Ajustes → Respaldo → Guardar mis ajustes** escribe un JSON con toda la
   configuración (tema, crossfade, silencios, compresor, nivel objetivo, actualizaciones, carpetas
   excluidas…) donde tú elijas. **Restaurar mis ajustes** lo vuelve a leer. Es lo único que te
   sirve si tienes que desinstalar por un cambio de firma.

El respaldo no incluye los archivos de audio ni las portadas importadas: los audios son tuyos y
nunca se copian.
