# 031. Escribir los logs en JSON, con campos y sin secretos

**Estado:** aceptada

## Contexto

Catálogo tenía 6 líneas de log con los datos escritos dentro de la frase, no distinguía una verificación de versión que no cambia nada de una que aplica un snapshot (la primera se repetía cada vuelta), un intento de autenticación fallido no dejaba rastro, y nada probaba el criterio del issue (sin contraseñas ni secretos en los logs). La prueba real contra la cátedra mostró además que los clientes de Kafka vuelcan unas cien líneas de configuración cada vez que arrancan. Turnos ya había resuelto lo mismo.

## Decisión

- **JSON (ECS) por defecto, con `LOG_FORMAT`** como parámetro de ajuste (el entorno gana; `logstash` es otro JSON y vacío vuelve al texto de Spring Boot, que no muestra los campos). Los datos van como campos de SLF4J (`eventId`, `eventVersion`, `syncType`, `fromVersion`, `toVersion`, `snapshotReason`, `path`, `exception`, `sqlState`) y el mensaje es fijo. **Todas las líneas se escriben igual**, y `LogStyleTest` falla si alguien escribe una llamada clásica en `src/main`.
- **Dos orígenes de sincronización** en `SyncOutcomeLogger` (`logEvent` y `logCheck`), en lugar de un texto libre con el origen. Un evento sin cambios (duplicado o copia al día) queda en `INFO`: es la traza de la deduplicación. La verificación periódica sin cambios va a `DEBUG`: corre cada 5 minutos y escribiría 288 líneas iguales por día.
- **Los volcados de configuración de Kafka** (`ConsumerConfig` y `AdminClientConfig`) pasan a `WARN`; no se baja todo `org.apache.kafka`, así que la asignación de particiones y los avisos siguen visibles.
- **Credenciales rechazadas** deja un `WARN` con la ruta y nada más. El caso de uso devuelve `Optional.empty` sin distinguir si falló el login, la contraseña o la cuenta inactiva (a propósito, para no permitir enumerar usuarios), así que el único «motivo» que el log puede dar es el genérico; no se cambia el tipo de retorno del caso de uso para eso.
- **La violación de integridad inesperada** registra la clase de la causa y el `SQLSTATE`, no el mensaje de PostgreSQL: ese mensaje repite la fila que chocó (login, email y hash).
- **El error inesperado** conserva la traza completa (es lo que hay que diagnosticar), con la ruta como campo.
- **El cliente REST de la cátedra no pasa la excepción de Spring como causa** cuando una respuesta correcta no se puede leer como JSON, y el mensaje nombra la clase del problema (por ejemplo `StreamReadException`). Hallazgo del test de secretos: el mensaje del parser de Jackson repite el comienzo del cuerpo (`Unrecognized token 'eyJ…'`), y si la cátedra devolviera el JWT técnico en su respuesta, la traza del planificador lo escribiría.
- **`SecretsNotInLogsIntegrationTest`** corre los flujos reales (registro, autenticación acertada y fallida, una violación de integridad inesperada y la verificación de versión contra una cátedra que responde `500`, `403`, un cuerpo que no es JSON, una conexión cortada o un snapshot válido, con Redis real rechazando la clave) y captura mensaje, campos, MDC y traza con todas sus causas. Los errores simulados de la cátedra repiten a propósito el JWT técnico, y el sentinela busca también su primer tramo (lo que un mensaje de error de JSON repite). Antes de buscar los valores comprueba que el flujo viajó por donde debía.

## Alternativas descartadas

- Dejar el texto libre con el origen dentro de la frase: no se puede filtrar por versión ni por evento sin depender de la redacción.
- `DEBUG` también para el evento sin cambios: un duplicado de Kafka es una señal útil de que la deduplicación funciona, y ocurre pocas veces.
- Bajar todo `org.apache.kafka` a `WARN`: oculta también la asignación de particiones del consumidor, que sirve para saber si el servicio está escuchando.
- Registrar el login (o el email) en un intento fallido: sirve para ubicar a quien se equivoca, pero deja un dato personal en el log y facilita enumerar cuentas si alguien lee los logs; la ruta y el hecho de que falló alcanzan para detectar abuso.
- Cambiar el caso de uso de autenticación para devolver el motivo: se rompe una interfaz y un test que hoy prueban justamente que no distingue.
- Registrar siempre la excepción entera de una violación de integridad: es exactamente lo que filtra la fila.

## Consecuencias

El log de catálogo se filtra por campo igual que el de turnos. El `lastError` de `GET /api/sync/status` de una respuesta ilegible ahora termina en `(StreamReadException)` y ya no se pierde la clase del problema aunque no viaje la causa. Quien necesite el detalle del parser no lo tiene en el log; si hiciera falta, se reproduce con la respuesta guardada. Las líneas de lectura de Redis (`parse` en `CatedraSyncSourceAdapter`) conservan su causa: el valor de una clave de Redis es dato del catálogo y Redis nunca repite la clave de acceso en sus errores (lo comprueba el sentinela con `WRONGPASS`).
