# Permisos Android y seguridad de archivos

| Android | Permiso / API | Uso |
|---|---|---|
| 8–12L (26–32) | `READ_EXTERNAL_STORAGE`, limitado con `maxSdkVersion=32` | Consulta de audio compartido con MediaStore |
| 13+ | `READ_MEDIA_AUDIO` | Acceso solo a audio, no permiso global para imágenes/vídeo |
| 13+ | `POST_NOTIFICATIONS` | Solicitud contextual al autorizar música; denegarlo no equivale a denegar audio |
| 8+ | `ACTION_OPEN_DOCUMENT_TREE` y `takePersistableUriPermission(READ)` | Carpetas seleccionadas conscientemente por el usuario |
| 8+ | `ACTION_OPEN_DOCUMENT` | Imagen manual; se copia una miniatura privada, no se necesita permiso general de fotos |
| 9+ | `FOREGROUND_SERVICE` | Servicios visibles de reproducción/procesamiento |
| 14+ | `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC` | Tipos de servicio explícitos |
| Todas soportadas | `WAKE_LOCK` | Reproducción/WorkManager durante trabajo activo |

No se usan permisos de escritura global, `MANAGE_EXTERNAL_STORAGE`, micrófono, contactos, ubicación ni Internet.

## Negación y revocación

El permiso se pide al pulsar la acción de encontrar música, no en un bucle al abrir. Si se rechaza, se explica la alternativa de seleccionar carpetas. El scanner comprueba permiso antes de consultar MediaStore y reporta excepciones. Las carpetas persisten como permisos URI; al revocarlas o cambiar de SD hay que seleccionarlas de nuevo. El botón para quitar una carpeta libera el permiso persistente y reconcilia las ubicaciones; no borra archivos.

## Directorios especiales

- `Music`, `Download`, grabaciones y `Android/media` se descubren si MediaStore ya los indexó y los permisos lo permiten.
- Audios de WhatsApp ocultos con `.nomedia` pueden no estar en MediaStore. Autorizar su carpeta con SAF permite buscarlos **si el selector del dispositivo concede acceso**.
- Android 11+ restringe árboles como raíz del almacenamiento, `Android/data`, `Android/obb` y la carpeta raíz Descargas. La aplicación no promete saltar estas restricciones ni usa rutas de filesystem privilegiadas.
- SD: MediaStore enumera volúmenes montados, y SAF permite autorizar documentos externos. Un almacenamiento desmontado no se convierte en un archivo borrado por la app.

## Originales y datos privados

Las ediciones, nombres y offsets residen en Room, y las portadas/derivados en almacenamiento privado. “Eliminar de biblioteca” oculta, nunca llama a un borrado del archivo fuente. “Compartir” entrega explícitamente el audio elegido mediante URI y permiso temporal de lectura. Las exportaciones procesadas usan el selector de destino del usuario. El backup automático de la app está deshabilitado para no copiar metadatos privados ni restaurar permisos URI inválidos en otro dispositivo.
