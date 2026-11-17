# 025. Verificar la versión al arrancar y cada cierto tiempo

**Estado:** aceptada

## Contexto

Kafka es solo una notificación y su entrega puede perderse (se agotan los reintentos, el servicio estuvo caído y el grupo ya había avanzado, la cátedra publicó sin que llegara el aviso). Sin otra cosa, la copia quedaría atrasada hasta el próximo evento. Además, un servicio recién instalado, con la base vacía, no tenía forma de llenarse sin que Kafka avisara. El enunciado (§6.2) pide detectar el atraso y reconstruir el estado local.

## Decisión

- `SynchronizeCatalogUseCase.synchronize()` aplica **la misma decisión** de la [decisión 024](024-sincronizacion-incremental.md) (comparar la versión local con la de Redis: nada, incremental o snapshot, con caída al snapshot ante cualquier fallo) pero **sin evento** que la dispare, así que no consulta ni guarda ningún `eventId`. Comparte el candado en memoria con `synchronizeForEvent`. `CatalogService.synchronize()` la expone.
- Un único disparador, `CatalogVersionCheckScheduler` (`infrastructure/scheduling`, adaptador de entrada), registra una tarea de **espera fija** con `SchedulingConfigurer`: la primera vuelta ocurre `CATEDRA_SYNC_INITIAL_DELAY` después de arrancar (con la base vacía trae el snapshot) y después una cada `CATEDRA_SYNC_CHECK_INTERVAL`, contadas desde el fin de la anterior para que nunca se solapen.
- Un fallo de una vuelta (cátedra o Redis caídos) lo captura el propio programador y deja un error con contexto en el log; se reintenta en la siguiente y el servicio sigue sano, igual que con Redis. Si el snapshot falló, el error ya queda en `sync_state` (CAT-08).
- El log del resultado lo comparten el listener de Kafka y el programador (`SyncOutcomeLogger`): los casos de uso no loguean.
- **Recuperación del estado.** Si una vuelta falló y la siguiente encuentra la copia al día (la versión local igual a la actual de Redis), `ClearSyncErrorUseCase` pasa `sync_state` a `OK` y borra el error, con la fecha del momento; no cambia la versión ni el tipo del último intento. Solo escribe si había un error que limpiar, así una verificación al día no toca la base cada cinco minutos. Salió de la primera prueba contra la cátedra real: tras un corte de red, la vuelta siguiente se recuperaba pero el estado seguía en `ERROR`, y el endpoint de estado (CAT-15) habría mostrado un error viejo con la copia al día. Se descartó escribir la fecha de cada verificación (una escritura cada cinco minutos sin necesidad) y dejar el estado como estaba (informaría mal).
- **Parámetros:** son valores de ajuste que no son secretos ni de una máquina, así que **tienen un valor por defecto en `application.properties`** (`${CATEDRA_SYNC_INITIAL_DELAY:10s}`, `${CATEDRA_SYNC_CHECK_INTERVAL:5m}`, `${CATEDRA_KAFKA_MAX_RETRIES:3}` y `${CATEDRA_KAFKA_INITIAL_BACKOFF:1s}`) y, si la variable de entorno está definida, esa **gana**. En Docker, el compose lista las variables **sin valor** (`CATEDRA_SYNC_CHECK_INTERVAL:`): Compose las pasa al contenedor solo si están definidas en el shell o en el `.env`, y si no, no las pasa y rige el valor por defecto. Se validan al arrancar (`CatedraSyncProperties`: el intervalo mayor que cero y la espera inicial no negativa). El interruptor `catedra.sync.check-enabled` (activo si no se define) lo apagan los tests. *Primera versión (reemplazada):* los valores iban literales en el compose y sin valor por defecto en el código; se cambió para que el servicio arranque sin configurar nada, dentro o fuera de Docker, y para que el entorno pueda cambiarlos.

## Alternativas descartadas

- Valores literales en el compose y sin valor por defecto en el código (la primera versión): el servicio no arranca sin ellos fuera de Docker y hay que repetirlos en cada entorno.
- `${VARIABLE:-valor}` en el compose: dos lugares para el mismo valor por defecto.
- Un `ApplicationReadyEvent` para el arranque y un `@Scheduled` para lo periódico: dos disparadores para lo mismo; la espera inicial corta ya cubre el arranque.
- Verificar cada minuto: más lecturas a Redis sin beneficio, porque Kafka ya avisa de casi todo.
- Tarea de frecuencia fija en lugar de espera fija: si una vuelta tarda (un snapshot grande, una cátedra lenta), la siguiente se acumularía.

## Consecuencias

El atraso por una notificación perdida dura como máximo el intervalo (5 minutos) más lo que tarde en aplicarse. Cada vuelta cuesta una lectura a Redis cuando la copia está al día. Con varias instancias cada una verificaría por su cuenta; el bloqueo de la base evita que se pisen. Los tests de integración usan un intervalo de 200 ms y PostgreSQL real, con la cátedra como doble.
