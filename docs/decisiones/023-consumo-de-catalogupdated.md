# 023. Consumir CatalogUpdated desde Kafka y deduplicar por eventId

**Estado:** aceptada. Ajustada por la [decisión 024](024-sincronizacion-incremental.md).

## Contexto

La cátedra avisa en Kafka cada versión nueva del catálogo (`CatalogUpdated`, anexo §15.3). La entrega es **al menos una vez**: el mismo evento puede llegar repetido, y puede llegar después de un fallo del servicio. El aviso no trae datos: hay que pedirlos. Cuando se implementó este consumidor todavía no existía la sincronización incremental, así que pedía siempre el snapshot completo; la [decisión 024](024-sincronizacion-incremental.md) la agrega y reemplaza ese paso.

## Decisión

- El adapter de entrada es `CatalogEventListener` (`infrastructure/kafka`, un paquete por tecnología como `client` y `redis`). Interpreta el JSON con `CatalogEventMapper`, llama a `CatalogService` y **confirma el offset al final**, con `ack-mode=manual`. No contiene lógica de negocio.
- `synchronizeForEvent` (en `SynchronizeCatalogUseCaseImpl`) primero descarta el evento si su `eventId` ya está en `processed_events` o si la versión local es **igual** a la del evento: ahorra el pedido HTTP. Una versión local mayor no se descarta (la cátedra pudo retroceder, [decisión 024](024-sincronizacion-incremental.md)). Si no, pide el snapshot y llama a `ApplyCatalogEventUseCase`.
- `ApplyCatalogEventUseCaseImpl` es transaccional y escribe **datos, versión y `eventId` en una sola transacción**: reutiliza `ApplyCatalogSnapshotUseCase`, que se une a la misma. Vuelve a comprobar el `eventId` dentro de la transacción y la clave primaria de `processed_events` rechaza el repetido si dos corren a la vez.
- `synchronize` y `synchronizeForEvent` comparten el candado en memoria y el registro de fallos: un evento y la verificación periódica no se cruzan, y un snapshot viejo nunca se aplica sobre uno nuevo.
- **Errores:** un fallo se reintenta con espera creciente (por defecto 3 reintentos desde 1 s, configurables). Agotados, el mensaje se descarta con un log de error y se sigue. Un mensaje que no se puede interpretar (JSON inválido, sin `eventId` o con versión inválida) se descarta sin reintentar.
- **Configuración:** el servidor y el grupo usan las propiedades estándar de Spring (`spring.kafka.*`), alimentadas por `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` y `CATEDRA_KAFKA_GROUP_ID`; el topic es `catedra.contract.kafka-catalog-topic` (`CATEDRA_KAFKA_CATALOG_TOPIC`) dentro de `CatedraContractProperties`, como los demás nombres del contrato ([decisión 022](022-rutas-y-claves-como-configuracion.md)). Las tres variables son obligatorias y sin valor por defecto. Los reintentos (`catedra.kafka.*`) salen de `CATEDRA_KAFKA_MAX_RETRIES` y `CATEDRA_KAFKA_INITIAL_BACKOFF`, con valor por defecto en `application.properties` y la variable de entorno ganando si está definida ([decisión 025](025-verificacion-de-version.md)).
- Un grupo sin offset guardado empieza por el principio del topic (`earliest`): los eventos viejos se descartan solos, porque la copia ya está al día o el `eventId` ya figura.
- El broker de la cátedra se alcanza sin autenticación ni cifrado (verificado con un cliente real): alcanza con el servidor.
- Kafka no entra en `/actuator/health`: un broker ajeno caído no debe volver «no saludable» al servicio, igual que Redis.

## Alternativas descartadas

- Pasar el `eventId` al caso de uso que aplica el snapshot: mezclaría en uno que hoy no conoce eventos dos responsabilidades.
- Sin deduplicar, confiando en que reaplicar un snapshot es idempotente: lo es en el resultado, pero repite un pedido HTTP y una escritura por cada duplicado.
- Confirmar el offset antes de procesar o con confirmación automática: un fallo después de confirmar perdería el evento sin remedio.
- Una cola de eventos muertos: más piezas (otro topic, otro consumidor) sin un beneficio claro; la verificación de versión recupera lo perdido.
- Reintentar sin límite o detener el consumo ante un fallo: un único mensaje que siempre falla bloquearía los siguientes.
- Un candado distinto para el consumidor: un evento y la verificación periódica podrían aplicar snapshots en orden inverso.

## Consecuencias

Si se agotan los reintentos la notificación se pierde y la copia queda atrasada hasta que la verificación de versión (arranque y chequeo periódico, [decisión 025](025-verificacion-de-version.md)) la recupere. Como el consumidor y la sincronización completa comparten un candado de una instancia, con varias instancias solo se evitan las colisiones por el bloqueo de la fila `sync_state`. Los tests usan un Kafka real en Docker (Testcontainers), con el listener desactivado en el resto de los tests; desactivan además la pausa de contextos de Spring (`spring.test.context.cache.pause=never`), que de otro modo reanuda el listener contra un broker ficticio. El CI no puede probar contra el broker real de la cátedra (está detrás de ZeroTier); la corrida manual está en [docs/evidencias.md](../evidencias.md).
