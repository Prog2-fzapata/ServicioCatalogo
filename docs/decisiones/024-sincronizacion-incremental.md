# 024. Elegir entre sincronización incremental y completa según la versión

**Estado:** aceptada

## Contexto

La cátedra publica en Redis qué cambió en cada versión (anexo §14.5): solo los ids afectados, y el estado vigente de cada entidad se lee aparte. Aplicar solo lo que cambió evita descargar el catálogo entero por cada aviso. Hay que decidir cuándo conviene la incremental, cuándo el snapshot, y qué hacer si algo falla a mitad.

## Decisión

- **Cuándo.** `synchronizeForEvent` compara la versión local con las que publica Redis (`readMetadata`, una sola lectura): igual a la actual, no hace nada; base vacía (versión 0), snapshot; **mayor** que la actual, snapshot (la cátedra retrocedió y es la fuente de verdad, [decisión 020](020-sincronizacion-completa-por-upsert.md)); menor que `oldestAvailableVersion`, snapshot (no están todos los cambios necesarios, anexo §14.2 y §18.2); si no, incremental desde `local + 1` hasta la actual. El evento solo es el disparador: la versión que manda es la de Redis, así que un evento viejo que llega tarde no hace nada.
- **Una transacción por versión.** `ApplyCatalogIncrementUseCaseImpl` bloquea `sync_state`, comprueba que la versión sea exactamente `local + 1`, hace el upsert (`CatalogRepository.upsert`, de padres a hijos y sin borrar nada: la cátedra solo hace bajas lógicas con `enabled = false`) y avanza la versión. Si la cátedra falla a mitad, lo ya aplicado queda.
- **El `eventId` en la última versión.** `ApplyCatalogEventUseCase` tiene una sobrecarga para incrementos: en la transacción de la última versión se guarda también el `eventId`, así nunca figura como procesado un evento que no se terminó de aplicar. La deduplicación es la misma de la [decisión 023](023-consumo-de-catalogupdated.md).
- **Caída al snapshot.** Cualquier fallo de la incremental (falta `changes:N`, una entidad ausente en Redis, un dato inválido, la base lo rechaza) o una lectura de Redis que falla (sin conexión, sin metadata) cae al snapshot completo, que ya aplica el evento. El fallo de la incremental no se registra en `sync_state`; solo si también falla el snapshot se registra este y el de la incremental viaja como excepción suprimida.
- `SyncResult` suma el tipo aplicado (`FULL` o `INCREMENTAL`) y el motivo por el que se usó un snapshot (`snapshotReason`). Los casos de uso no loguean (decisión de CAT-08): el listener escribe el aviso de retroceso y el motivo.
- **Corrección a la [decisión 023](023-consumo-de-catalogupdated.md).** El filtro previo ignoraba el evento si la versión local era igual **o mayor**; ahora solo si es **igual**, porque un retroceso anunciado por Kafka no se habría aplicado.
- Comparte el candado en memoria con la sincronización completa: una incremental y un snapshot no se cruzan.

## Alternativas descartadas

- Ignorar el evento si la versión local es igual o mayor que la de Redis: no aplicaría un retroceso de la cátedra, que es la fuente de verdad.
- Una sola transacción para todas las versiones: un fallo en la última deshace lo ya traído y obliga a empezar de nuevo.
- Borrar en la incremental lo que no aparece: la incremental solo ve lo que cambió, no el catálogo entero; las bajas llegan como `enabled = false` y el snapshot limpia lo que ya no exista.
- No caer al snapshot si Redis no responde: el catálogo quedaría atrasado aunque la API REST sí responda.
- Un candado propio para la incremental: podría aplicar versiones mientras un snapshot reemplaza la copia.

## Consecuencias

Si falta la fila única de `sync_state` (la crea la migración V2, con la versión local en 0), se lanza `SyncStateMissingException`: no es una base sin sincronizar, es una base alterada. Una versión con una entidad que Redis ya no tiene, o con una referencia a una categoría que no existe, no se puede aplicar de a una y se resuelve con el snapshot. Mientras Redis esté caído, cada evento descarga el snapshot completo. La verificación al arrancar y periódica ([decisión 025](025-verificacion-de-version.md)) reutiliza esta misma decisión. Los tests de integración usan un Redis con un usuario de solo lectura, como el de la cátedra, y PostgreSQL reales; el CI no puede probar contra el Redis real de la cátedra (está detrás de ZeroTier); la corrida manual está en [docs/evidencias.md](../evidencias.md).
