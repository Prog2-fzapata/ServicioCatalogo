# 020. Aplicar la sincronización completa por upsert

**Estado:** aceptada

## Contexto

CAT-08 reemplaza la copia local del catálogo con un snapshot de la cátedra. La copia y la versión deben cambiar juntas o no cambiar, y el servicio nunca debe quedar con una versión que no corresponde a sus datos. Los disparadores de la sincronización (arranque, chequeo periódico, Kafka) se agregan después, con CAT-11 y CAT-12.

## Decisión

- **Upsert, no borrar todo.** Dentro de una transacción se leen las tres tablas, se hace `save` de cada elemento del snapshot y se borran los ids que ya no llegan. Como las entities se leyeron antes, Hibernate sabe cuáles existen y emite `UPDATE` solo si algo cambió (un test lo comprueba con las estadísticas de Hibernate). Se escribe de padres a hijos (categorías, profesionales, horarios) y se borra de hijos a padres por las claves foráneas. La cátedra solo hace bajas lógicas ([decisión 018](018-esquema-de-la-copia-local.md)), así que el borrado casi nunca encuentra algo: queda como defensa por si la cátedra reinicia sus datos.
- **Tres casos de uso.** `SynchronizeCatalogUseCaseImpl` orquesta y no tiene transacción: pide el snapshot por HTTP (hasta 10 s) antes de abrirla. `ApplyCatalogSnapshotUseCaseImpl` es `@Transactional`: bloquea la fila de `sync_state`, reemplaza los datos y guarda la versión. `RecordSyncFailureUseCaseImpl` registra el fallo en una transacción propia.
- **Una sincronización a la vez.** El orquestador envuelve todo, pedido HTTP incluido, en un `ReentrantLock` con espera: si dos disparadores coinciden, la segunda pide su propio snapshot cuando la primera termina y nunca aplica uno viejo encima de uno nuevo. El `SELECT ... FOR UPDATE` sobre `sync_state` queda por si corren dos instancias.
- **Un snapshot con versión menor se aplica igual**: la cátedra es la fuente de verdad (por ejemplo, si reinicia sus datos). Los casos de uso no registran logs, como pide la arquitectura: al aplicar un snapshot se devuelve un `SyncResult` con la versión anterior y la nueva, y quien lo llame avisa del retroceso.
- **Registro del fallo.** Si la sincronización falla, se guarda `last_status = ERROR` y `last_error` (clase y mensaje de la excepción, cortados a 500 caracteres) sin tocar `local_version`. Si registrar el fallo también falla, se relanza la excepción **original** con la del registro como suprimida.
- **Persistencia con JPA.** Las entities guardan `categoryId` y `professionalId` como números, sin `@ManyToOne`, para coincidir con el modelo de dominio y evitar cargas perezosas.

## Alternativas descartadas

- Borrar todo y reinsertar: reescribe todas las filas y deja filas muertas en PostgreSQL aunque cambie una sola, y no se reutiliza en la sincronización incremental.
- `JdbcClient` con `INSERT ... ON CONFLICT DO UPDATE` en lotes: es más rápido y no carga las tablas en memoria, pero obliga a escribir SQL a mano, ata el código a PostgreSQL y se aparta de la arquitectura. Queda como salida si se mide que JPA es lento.
- Una sola transacción que incluya el pedido HTTP: retendría una conexión a la base mientras espera a la cátedra.
- Saltear la sincronización si ya hay una en curso: un evento que anuncia una versión nueva se perdería hasta el próximo chequeo periódico.
- Registrar el error en la misma transacción: el rollback lo borraría.

## Consecuencias

Cada tabla se carga en memoria al sincronizar (alcanza para miles de filas) y, para un id nuevo, `merge` hace un `SELECT` antes del `INSERT`. Si la base está caída, el error no puede registrarse y `sync_state` conserva el estado anterior hasta la próxima sincronización. El candado en memoria vale para una instancia; con varias, el bloqueo de la base evita que se pisen pero no ahorra pedidos al snapshot.
