# 026. Exponer el estado de la sincronización sin datos sensibles

**Estado:** aceptada

## Contexto

Hace falta poder consultar cómo está la copia local del catálogo: en qué versión está y cómo salió el último intento. El criterio del issue pide además que los errores registrados no traigan datos sensibles. El campo `last_error` de `sync_state` guardaba hasta ahora la clase y el mensaje de cualquier excepción, y el mensaje de Hibernate, del driver o de Redis puede incluir la sentencia SQL, nombres de columnas o valores. Mostrarlo en un endpoint lo habría expuesto.

## Decisión

- **Endpoint.** `GET /api/sync/status` devuelve `localVersion`, `lastSyncAt`, `lastSyncType`, `lastStatus` y `lastError`, con el mismo nombre en inglés que usan las demás respuestas. Una base que nunca se sincronizó devuelve `200` con `localVersion` en `0` y el resto en `null`: es un estado normal. Si falta la fila única de `sync_state` (la base fue alterada) responde `500` con el formato de error estándar y sin detalles: el motivo queda en el log.
- **Autenticación.** Requiere un JWT válido y cualquier usuario autenticado puede consultarlo, sin rol particular. No hizo falta configurar nada: `SecurityConfiguration` ya exige token a todo lo que no sea registro, login, salud y documentación. Los tests comprueban el `401` sin token.
- **Los errores se limpian al guardarlos, no al mostrarlos.** `RecordSyncFailureUseCaseImpl` guarda `Clase: mensaje` solo para las excepciones de la cátedra (`CatedraException`), cuyos mensajes escribimos nosotros; de cualquier otra guarda solo el nombre de la clase. Así lo sensible nunca llega a la base ni a ningún otro lector futuro de `sync_state`. El texto completo no se pierde: queda en el log, que ya registra la pila entera (el programador y el manejador de errores de Kafka). Se mantiene el recorte a 500 caracteres.
- **Estructura.** Caso de uso `GetSyncStatusUseCase` (lectura sin bloqueo con `SyncStateRepository.find()`, para no esperar a una sincronización en curso), método en `CatalogService` que traduce la ausencia de fila a `SyncStateMissingException`, y controller, DTO y mapper en `catalogo/infrastructure/web`.

## Alternativas descartadas

- Limpiar el error solo al mostrarlo y guardar todo: el dato sensible quedaría en la base y dependería de que ningún otro camino la lea.
- Guardar el mensaje de toda excepción y recortarlo: el recorte no quita el SQL ni los valores.
- Pedir un rol de administrador: el proyecto no tiene ese concepto.
- Una sola lista blanca de códigos de error en lugar de la clase: más piezas sin un beneficio claro para este tamaño.

## Consecuencias

En `sync_state` se pierde el mensaje de errores propios que no son de la cátedra (por ejemplo «Se esperaba la versión 5 y llegó la 6»); solo quedan en el log. Esos errores de la incremental, además, nunca llegan a `sync_state`: la incremental que falla cae al snapshot y el motivo se registra en el log del resultado. Las filas que ya estuvieran guardadas con el formato anterior no se limpian: no hay datos reales todavía.
