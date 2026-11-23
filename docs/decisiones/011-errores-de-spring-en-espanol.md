# 011. Dar a los errores estándar de Spring el mismo formato y el texto en español

**Estado:** aceptada

## Contexto

Spring responde por su cuenta algunos errores (ruta inexistente, método no permitido, tipo de contenido no soportado) con texto en inglés y sin `code`.

## Decisión

El handler compartido intercepta también esas respuestas y les agrega el `code` (`NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `BAD_REQUEST`), el `path` y un `detail` en español, para que todos los errores de la API tengan la misma forma. El `title` conserva el nombre estándar del estado HTTP.

## Consecuencias

**Limitación conocida:** Spring agrega el campo `instance` (con el mismo valor que `path`) al escribir cualquier respuesta `ProblemDetail`, y no se puede omitir sin reemplazar esa clase por una propia. Es inofensivo para los clientes, que ignoran campos que no conocen.
