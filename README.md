<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# ServicioCatalogo

Servicio de catálogo y sincronización.

## Requisitos

- Docker y Docker Compose v2.
- Java 25, solo si se ejecuta el servicio fuera de Docker.

## Configuración

```bash
cp .env.example .env
```

Completar la clave de la base, generar el secreto del JWT (`openssl rand -base64 64`) y cargar la URL, el JWT técnico y los datos de Redis y de Kafka de la cátedra en `.env`. Las rutas y claves del contrato de la cátedra también viven ahí (ya vienen con el valor de la versión 1 en `.env.example`): si la cátedra cambia alguna, se edita el `.env` y se reinicia, sin tocar el código. El archivo no se versiona.

| Variable | Descripción | Por defecto |
| --- | --- | --- |
| `CATALOGO_DB_NAME` | Nombre de la base de datos | `catalogo_db` |
| `CATALOGO_DB_USER` | Usuario de la base | `catalogo_user` |
| `CATALOGO_DB_PASSWORD` | Clave del usuario | |
| `POSTGRES_PORT` | Puerto del host donde se publica PostgreSQL | `5432` |
| `CATALOGO_PORT` | Puerto del host donde se publica el servicio | `8081` |
| `JWT_SECRET` | Clave con la que se firman los tokens (HS512): Base64 de al menos 512 bits. Si falta o es más corta, el servicio no arranca. Turnos debe usar el mismo valor | |
| `CATEDRA_BASE_URL` | URL base de la API de la cátedra, sin `/` final. Obligatoria: sin ella el servicio no arranca | |
| `CATEDRA_JWT` | JWT técnico de la cuenta de la cátedra (el `id_token` de su `POST /api/authenticate`). Obligatorio; vence al año, y entonces hay que generar otro y reemplazarlo. No se escribe en logs | |
| `CATEDRA_REDIS_HOST` | Host del Redis de la cátedra, de solo lectura. Va la IP de ZeroTier: el host que devuelve la cátedra en `integration` es interno y no se alcanza. Obligatoria | |
| `CATEDRA_REDIS_PORT` | Puerto del Redis de la cátedra. Obligatoria | `6379` |
| `CATEDRA_REDIS_USERNAME` | Usuario de la cuenta técnica en Redis (`redisUsername` de `integration`). Obligatoria | |
| `CATEDRA_REDIS_PASSWORD` | Clave de esa cuenta (`redisPassword` de `integration`, solo se entrega con la cuenta en estado `PROVISIONED`). Obligatoria; no se escribe en logs | |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | Servidor de Kafka de la cátedra (`kafkaBootstrapServers` de `integration`), con su puerto. Va la IP de ZeroTier, igual que en Redis. El broker no pide autenticación. Obligatoria | |
| `CATEDRA_KAFKA_GROUP_ID` | Grupo de consumo de la cuenta técnica (`kafkaConsumerGroupId` de `integration`). Obligatoria | |
| `CATEDRA_KAFKA_CATALOG_TOPIC` | Topic con los avisos `CatalogUpdated` (`kafkaCatalogTopic` de `integration`). Obligatoria | |
| `CATEDRA_SNAPSHOT_PATH` | Ruta REST del snapshot de la cátedra; debe empezar con `/` o el servicio no arranca. Obligatoria; `.env.example` trae el valor del contrato v1 | |
| `CATEDRA_REDIS_KEY_METADATA` | Clave del hash con las versiones que publica la cátedra. Obligatoria; `.env.example` trae el valor del contrato v1 | |
| `CATEDRA_REDIS_KEY_CHANGES_PREFIX` | Prefijo de la clave de cambios de cada versión (el número de versión se agrega al final). Obligatoria | |
| `CATEDRA_REDIS_KEY_CATEGORIES` | Clave del hash con las categorías vigentes. Obligatoria | |
| `CATEDRA_REDIS_KEY_PROFESSIONALS` | Clave del hash con los profesionales vigentes. Obligatoria | |
| `CATEDRA_REDIS_KEY_WEEKLY_SCHEDULES` | Clave del hash con los horarios semanales vigentes. Obligatoria | |
| `PASSWORD_ARGON2_MEMORY_KIB` | Memoria por hash de contraseña (Argon2id), en KiB. Mínimo de OWASP: 19456 (19 MiB); el servicio no arranca con menos. `.env.example` trae ese valor | |
| `PASSWORD_ARGON2_ITERATIONS` | Pasadas del hash sobre esa memoria. Mínimo de OWASP: 2; el servicio no arranca con menos | |
| `PASSWORD_ARGON2_PARALLELISM` | Hilos por hash. Mínimo: 1 (con la librería usada, más hilos no aceleran nada) | |
| `SPRING_PROFILES_ACTIVE` | Entorno de ejecución: `dev` o `prod`. Con `prod` se desactiva la documentación interactiva de la API (Swagger UI y `/v3/api-docs`) | `dev` |

### Parámetros de ajuste

No son secretos ni dependen de la máquina, así que **tienen un valor por defecto** (en `application.properties`) y no hace falta definirlos. Si se define la variable de entorno, esa **gana**: con Docker, desde el shell o desde el `.env` (el compose la pasa solo si está definida); fuera de Docker, exportándola. Los formatos de tiempo son `500ms`, `10s`, `5m`.

| Variable | Descripción | Por defecto |
| --- | --- | --- |
| `CATEDRA_SYNC_INITIAL_DELAY` | Espera desde que arranca el servicio hasta la primera verificación de versión. Si la base está vacía, esa vuelta trae el snapshot. Mínimo `0s` | `10s` |
| `CATEDRA_SYNC_CHECK_INTERVAL` | Espera entre el fin de una verificación de versión y el comienzo de la siguiente. Mayor que cero | `5m` |
| `CATEDRA_KAFKA_MAX_RETRIES` | Reintentos (después del primer intento) ante un fallo al procesar un evento de Kafka. `0` los desactiva | `3` |
| `CATEDRA_KAFKA_INITIAL_BACKOFF` | Espera antes del primer reintento; cada uno espera el doble del anterior | `1s` |
| `CATALOGO_PAGE_MAX_SIZE` | Tamaño máximo de página en las búsquedas: un `size` mayor se reduce a este valor | `100` |
| `LOG_FORMAT` | Formato de los logs: `ecs` (JSON, por defecto), `logstash` (otro JSON) o vacío para el texto de Spring Boot, que no muestra los campos de cada línea (ver «Logs») | `ecs` |

PostgreSQL y el servicio se publican solo en `127.0.0.1`.

## Levantar con Docker

```bash
docker compose up -d --build
```

Levanta una instancia de PostgreSQL propia del servicio y el servicio, que arranca cuando la base está lista. Estado: `http://localhost:8081/actuator/health`.

| Acción | Comando |
| --- | --- |
| Ver estado | `docker compose ps` |
| Ver logs | `docker compose logs -f catalogo` |
| Detener (conserva los datos) | `docker compose down` |
| Borrar todo, incluidos los datos | `docker compose down -v` |

## Ejecutar fuera de Docker

Levantar solo la base y correr el servicio con Maven, con las variables de `.env` cargadas:

```bash
docker compose up -d --wait postgres
set -a; . ./.env; set +a
./mvnw spring-boot:run
```

El servicio se conecta a `localhost` en el puerto `POSTGRES_PORT`.

## Documentación de la API

Con el servicio en ejecución, la documentación interactiva está en `http://localhost:8081/swagger-ui.html` y el documento OpenAPI en `http://localhost:8081/v3/api-docs`. Con `SPRING_PROFILES_ACTIVE=prod` ambos quedan desactivados.

Para probar los endpoints protegidos desde Swagger UI: llamar a `POST /api/authenticate`, copiar el `id_token` de la respuesta, pulsar el botón **Authorize** (candado, arriba a la derecha), pegarlo y confirmar. Desde ahí todos los pedidos de la pantalla lo envían.

## Autenticación

Los endpoints, salvo el registro, el login, `/actuator/health` y la documentación, exigen `Authorization: Bearer <jwt>`.

```bash
curl -X POST localhost:8081/api/register -H 'Content-Type: application/json' \
  -d '{"login":"maria","password":"clave123","email":"maria@mail.com"}'
curl -X POST localhost:8081/api/authenticate -H 'Content-Type: application/json' \
  -d '{"username":"maria","password":"clave123","rememberMe":false}'
curl localhost:8081/api/account -H "Authorization: Bearer <id_token>"
```

El token dura 24 horas (30 días con `rememberMe`). `username` acepta el login o el email.

Sin token, o con uno vencido, sin `exp` (no vencería nunca, así que no se acepta), mal formado o firmado con otra clave, la respuesta es `401` en `application/problem+json` con `code` `UNAUTHORIZED` y la cabecera `WWW-Authenticate: Bearer`; no se informa el motivo. Un usuario autenticado sin el rol necesario recibe `403` con `code` `FORBIDDEN`.

## Sincronización del catálogo

El servicio mantiene una copia local de las categorías, los profesionales y los horarios de la cátedra. La sincronización completa pide el snapshot (`GET /api/synchronization/snapshot`), y en una sola transacción crea o actualiza lo que llega, borra lo que ya no llega y guarda la versión. Si algo falla no cambia nada: ni los datos ni la versión. El resultado del último intento queda en la tabla `sync_state`.

Para saber qué cambió, el servicio también lee de solo lectura el Redis de la cátedra (`catedra:sync:*`: metadata con las versiones, cambios por versión y entidades vigentes por id). Si ese Redis no responde, el servicio sigue sano y sirve el catálogo local: su estado no forma parte de `/actuator/health`.

La cátedra avisa cada versión nueva con un evento `CatalogUpdated` en Kafka. El servicio lo consume así:

1. Si ya procesó ese `eventId`, o la copia local tiene justo la versión que anuncia el evento, lo ignora sin pedir nada.
2. Si no, lee de Redis las versiones que publica la cátedra y decide cómo sincronizar, comparando con la versión local:

   | Versión local | Qué hace |
   | --- | --- |
   | Igual a la actual de Redis | Nada (un evento viejo que llega tarde no cambia nada) |
   | 0 (base vacía) | Snapshot completo |
   | Mayor que la actual de Redis | Snapshot completo: la cátedra retrocedió y es la fuente de verdad. Se avisa con un log |
   | Menor que la versión más antigua disponible | Snapshot completo: ya no están todos los cambios |
   | Cualquier otra menor | **Incremental**: aplica de a una las versiones desde la local + 1 hasta la actual |

3. En la incremental, cada versión es **una transacción**: se leen de Redis las entidades afectadas, se crean o actualizan (las bajas de la cátedra llegan como `enabled = false`, no se borra nada) y avanza la versión. El `eventId` se guarda en la transacción de la **última** versión. Si algo falla a mitad, lo ya aplicado queda.
4. Si la incremental falla (falta el historial de una versión, un dato inválido, la base lo rechaza) o Redis no responde, cae al snapshot completo, que en **una sola transacción** aplica los datos, avanza la versión y guarda el `eventId`. El error de la incremental solo se registra si también falla el snapshot. El tipo del último intento queda en `sync_state`.
5. Recién después confirma el offset en Kafka. Si el servicio cae antes, Kafka reentrega el evento y el `eventId` evita repetir el efecto.

Un fallo al procesar se reintenta `CATEDRA_KAFKA_MAX_RETRIES` veces (3 por defecto), esperando 1, 2 y 4 segundos (`CATEDRA_KAFKA_INITIAL_BACKOFF`, que se duplica en cada reintento). Si se agotan los reintentos, o el mensaje no se puede interpretar, se registra un error y se pasa al siguiente mensaje: no hay cola de eventos muertos. Lo que una notificación perdida deja atrasado lo recupera la verificación de versión, que se explica a continuación.

### Verificación de versión

Kafka solo avisa: si una notificación se pierde, la copia queda atrasada. Para eso el servicio **compara su versión con la de Redis por su cuenta**, sin esperar un evento: unos segundos después de arrancar (`CATEDRA_SYNC_INITIAL_DELAY`) y después cada `CATEDRA_SYNC_CHECK_INTERVAL` (5 minutos por defecto).

Aplica la misma decisión de la tabla de arriba, sin evento que registrar: si la copia está al día no hace nada (una sola lectura a Redis), si está atrasada aplica las versiones que faltan y, **con la base vacía, trae el snapshot**; así un servicio recién instalado se llena solo, sin esperar a un evento. Comparte el candado con el consumidor de Kafka: una verificación y un evento no se cruzan.

Si la cátedra o Redis no responden, la vuelta falla, se registra el error y se reintenta en la próxima; el servicio sigue sano y sirviendo la copia local. Si falló el snapshot, el error queda en la tabla `sync_state`. Cuando una vuelta posterior encuentra la copia al día, el estado vuelve a `OK` y el error se borra: `sync_state` describe cómo está la copia ahora, no el último tropiezo.

Kafka no forma parte de `/actuator/health`, igual que Redis.

### Estado de la sincronización

`GET /api/sync/status` informa cómo está la copia local. **Requiere un JWT válido** (cualquier usuario autenticado, sin rol particular); sin token responde `401`.

```bash
curl localhost:8081/api/sync/status -H "Authorization: Bearer <id_token>"
```

```json
{
  "localVersion": 7,
  "lastSyncAt": "2026-10-27T19:49:12Z",
  "lastSyncType": "INCREMENTAL",
  "lastStatus": "OK",
  "lastError": null
}
```

| Campo | Significado |
| --- | --- |
| `localVersion` | Versión del catálogo que tiene la copia. `0` si todavía no se sincronizó nunca |
| `lastSyncAt` | Momento del último intento (o de la última recuperación de un error) |
| `lastSyncType` | `FULL` (snapshot) o `INCREMENTAL` |
| `lastStatus` | `OK` o `ERROR` |
| `lastError` | Causa del último error, o `null` si el último intento salió bien |

Una base que nunca se sincronizó devuelve `localVersion` en `0` y el resto en `null`: es un estado normal, no un error. Si una sincronización falla, `lastStatus` queda en `ERROR` hasta que una verificación posterior encuentra la copia al día.

**Los errores se guardan sin datos sensibles.** De una falla al hablar con la cátedra se guarda la clase y el mensaje (por ejemplo `CatedraUnavailableException: No se pudo conectar con la cátedra`), que escribimos nosotros. De cualquier otra excepción se guarda solo el nombre de la clase: su mensaje (el de la base de datos, por ejemplo) puede traer la sentencia SQL, nombres de columnas o valores. El detalle completo queda en el log del servicio.

### Búsqueda de profesionales

`GET /api/professionals` busca sobre la copia local del catálogo; nunca consulta a la cátedra. **Requiere un JWT válido** (cualquier usuario autenticado).

| Parámetro | Descripción |
| --- | --- |
| `categoryId` | Solo los de esa categoría |
| `name` | Texto parcial sobre «nombre apellido» juntos, **sin distinguir mayúsculas ni tildes**: `ana gom` o `GOMEZ` encuentran a «Ana Gómez». Hasta 100 caracteres. Un `%` o un `_` se busca como texto, no como comodín |
| `enabled` | `true` o `false`; sin valor devuelve habilitados y deshabilitados |
| `available` | `true`: tiene al menos un horario semanal habilitado; `false`: no tiene ninguno. Se mira la agenda local, no las ocupaciones reales de la cátedra |
| `page`, `size` | Página (desde 0) y tamaño. Por defecto `size=20`; un valor mayor que `CATALOGO_PAGE_MAX_SIZE` se reduce a ese tope |
| `sort` | `id`, `firstName` o `lastName`, con `,asc` o `,desc` (se puede repetir). Por defecto, apellido y nombre. Cualquier otro campo responde `400` |

```bash
curl "localhost:8081/api/professionals?name=gomez&available=true&size=10&sort=lastName,asc" \
  -H "Authorization: Bearer <id_token>"
```

La respuesta es un arreglo y el total y los enlaces van en las cabeceras, como en JHipster: `X-Total-Count` con el total de profesionales que cumplen el criterio, y `Link` con las páginas `first`, `prev`, `next` y `last`, que conservan los mismos filtros y el mismo orden.

```json
[
  {
    "id": 15,
    "categoryId": 1,
    "categoryName": "Cardiologia",
    "firstName": "Ana",
    "lastName": "Gómez",
    "enabled": true,
    "weeklySchedules": [
      { "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "12:00:00", "slotDurationMinutes": 30 }
    ]
  }
]
```

`weeklySchedules` trae solo los horarios habilitados, ordenados por día de la semana y hora de inicio. Una página cuesta una cantidad fija de consultas (la página, el total, las categorías y los horarios), sin importar cuántos profesionales traiga.

### Detalle de un profesional

`GET /api/professionals/{id}` devuelve la **agenda vigente** de un profesional: es lo que consulta el servicio de turnos. Requiere un JWT válido.

```bash
curl "localhost:8081/api/professionals/15" -H "Authorization: Bearer <id_token>"
```

Es el mismo objeto que trae la lista, pero `weeklySchedules` incluye **todos** los horarios del profesional, también los deshabilitados, y cada uno con su `id` y su `enabled`, ordenados por día de la semana y hora de inicio. El detalle agrega `categoryEnabled`, que dice si la categoría del profesional está habilitada (la lista no lo trae). Un profesional deshabilitado, o de una categoría deshabilitada, se devuelve igual: `enabled` es el del profesional y `categoryEnabled` el de su categoría, y lo decide quien consume (turnos no ofrece horarios si cualquiera de los dos está en `false`).

```json
{
  "id": 15,
  "categoryId": 1,
  "categoryName": "Cardiologia",
  "categoryEnabled": true,
  "firstName": "Ana",
  "lastName": "Gómez",
  "enabled": true,
  "weeklySchedules": [
    { "id": 101, "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "12:00:00", "slotDurationMinutes": 30, "enabled": true },
    { "id": 102, "dayOfWeek": "TUESDAY", "startTime": "14:00:00", "endTime": "18:00:00", "slotDurationMinutes": 20, "enabled": false }
  ]
}
```

Un id que no existe responde `404` con el código `PROFESSIONAL_NOT_FOUND`; un id que no es un número, `400`.

### Categorías

`GET /api/professional-categories` lista las categorías de la copia local. Requiere un JWT válido.

| Parámetro | Descripción |
| --- | --- |
| `enabled` | `true` o `false`; sin valor devuelve habilitadas y deshabilitadas |
| `page`, `size` | Igual que en profesionales: `size=20` por defecto y tope `CATALOGO_PAGE_MAX_SIZE` |
| `sort` | `id` o `name`, con `,asc` o `,desc`. Por defecto, `name`. Cualquier otro campo responde `400` |

La respuesta es un arreglo con `X-Total-Count` y `Link`, como la búsqueda de profesionales:

```json
[ { "id": 1, "name": "Cardiologia", "description": "Corazón y circulación", "enabled": true } ]
```

## Logs

El servicio escribe en la salida estándar (`docker compose logs -f catalogo`) **una línea de JSON por evento** (formato ECS de Spring Boot), a nivel `INFO`. Cada línea tiene un mensaje corto y estable, y sus datos como **campos**, así se filtra por campo sin depender de la redacción:

```json
{"@timestamp":"2026-11-07T16:54:32Z","log":{"level":"INFO","logger":"…SyncOutcomeLogger"},"service":{"name":"catalogo"},
 "message":"Evento de catálogo: aplicado","eventId":"e1","eventVersion":55,"syncType":"INCREMENTAL","fromVersion":54,"toVersion":55}
```

| Campo | Qué es |
| --- | --- |
| `eventId`, `eventVersion` | El aviso `CatalogUpdated` de la cátedra que disparó la sincronización: su id y la versión que anuncia |
| `syncType` | `INCREMENTAL` (cambios leídos de Redis) o `FULL` (snapshot por REST) |
| `fromVersion`, `toVersion` | La versión de la copia local antes y después de aplicar |
| `snapshotReason` | Por qué se trajo un snapshot y no la incremental (la base vacía, Redis sin responder, un salto de versiones...) |
| `path`, `exception`, `sqlState` | En un error propio: la ruta del pedido, el nombre de la clase de la excepción y, si es de la base, su `SQLSTATE` |

Para filtrar: `docker compose logs --no-log-prefix catalogo | jq -c 'select(.toVersion == 55)'`. Para leerlas a ojo, este filtro deja una línea corta por evento (salta lo que no es JSON):

```bash
docker compose logs -f --no-log-prefix catalogo | jq -R -r 'fromjson? | [.["@timestamp"][11:19], .log.level, .message,
  (del(.["@timestamp"], .log, .process, .service, .ecs, .message, .tags, .error) | to_entries | map("\(.key)=\(.value)") | join(" ")),
  (if .error then "(\(.error.type): \(.error.message))" else "" end)] | map(select(. != "")) | join("  ")'
```

Todas las líneas de log se escriben igual (mensaje fijo y datos como campos), y `LogStyleTest` falla si alguien escribe una de otra forma. `LOG_FORMAT` cambia el formato: `logstash` es otro JSON, y vacío vuelve al texto de Spring Boot, que **no muestra los campos** (solo el mensaje).

| Se registra | Mensaje y campos |
| --- | --- |
| Un evento de Kafka, con su resultado | `Evento de catálogo: aplicado` (`syncType`, `fromVersion`, `toVersion`, `snapshotReason` si hubo snapshot) y `Evento de catálogo: nada que aplicar (…)` para un duplicado o una copia que ya estaba al día; ambos con `eventId` y `eventVersion` |
| La verificación de versión que aplicó algo | `Verificación de versión: aplicado`, con los mismos campos que el evento, sin `eventId` |
| La cátedra que retrocede de versión (se aplica igual: es la fuente de verdad) | `…: la cátedra retrocedió la versión del catálogo` (`WARN`, `fromVersion`, `toVersion`) |
| Una vuelta de verificación que falló | `La verificación de versión falló; se reintenta en la próxima vuelta` (`ERROR`, la excepción en `error`) |
| Un login o una contraseña que no coinciden | `Credenciales rechazadas` (`WARN`, `path`) |
| Una violación de integridad que no es de login ni de email repetidos | `Violación de integridad no prevista` (`ERROR`, `path`, `exception`, `sqlState`) |
| Los errores inesperados, con su traza | `Error inesperado` (`ERROR`, `path`, la excepción en `error`) |

La verificación de versión **sin cambios** no sale por defecto: corre cada `CATEDRA_SYNC_CHECK_INTERVAL` (5 minutos) y escribiría la misma línea una y otra vez, así que va a `DEBUG`. Un evento duplicado de Kafka sí queda en `INFO`, porque es la traza de la deduplicación. Los clientes de Kafka vuelcan unas cien líneas con su configuración cada vez que arrancan: `ConsumerConfig` y `AdminClientConfig` están en `WARN` (`logging.level.…` en `application.properties`), y el resto de lo que dice Kafka sigue visible.

**Nunca se escribe:** la contraseña ni el email ni el login de un usuario (tampoco en un intento fallido), el JWT del usuario, el JWT técnico de la cátedra (`CATEDRA_JWT`), la clave de Redis, el secreto de firma (`JWT_SECRET`) ni la contraseña de la base. Cómo se garantiza:

- Una credencial rechazada registra solo la ruta: el caso de uso no distingue si falló el login, la contraseña o la cuenta, y el log tampoco.
- La violación de integridad inesperada registra la clase y el `SQLSTATE`, no el mensaje de PostgreSQL: ese mensaje repite la fila que chocó (login, email y hash).
- El cliente REST de la cátedra solo mira el status de un error, nunca su cuerpo; y si una respuesta correcta no se puede leer como JSON, **no pasa la excepción de Spring como causa** de la propia: su mensaje repite el comienzo del cuerpo (por ejemplo un JWT que la cátedra devolvió) y saldría en la traza. Se informa solo el nombre de la clase del problema.
- Los errores inesperados sí registran la excepción entera (mensaje y traza), porque son los que hay que diagnosticar. El mensaje de una excepción no lo controla el servicio, y por eso el test del criterio compara también las trazas.

Lo comprueba `SecretsNotInLogsIntegrationTest`: con PostgreSQL y Redis reales en contenedores y la cátedra como servidor HTTP local, corre el registro, la autenticación (acertada y fallida), una violación de integridad inesperada y la verificación de versión contra una cátedra que responde `500`, `403`, un cuerpo que no es JSON, una conexión cortada o un snapshot válido, con Redis rechazando la clave. Los errores simulados de la cátedra **repiten a propósito el JWT técnico**. Captura todo lo que se escribe (mensaje, campos, contexto y traza completa con sus causas) y verifica que ningún valor sentinela aparece; antes comprueba que el flujo viajó por donde debía (la respuesta, la fila en la base o la línea esperada), para que no pase en vacío.

## Tests

```bash
./mvnw verify
```

Los tests de integración levantan su propio PostgreSQL 18 y, según el caso, un Kafka 4.0 (el consumidor) o un Redis 7 (la lectura de la cátedra y el test de secretos) en contenedores descartables (Testcontainers), así que solo requieren Docker en ejecución; no usan el `.env` ni la base del compose.

## Decisiones de diseño

El registro de decisiones técnicas, con alternativas descartadas y justificación, está en [docs/decisiones.md](docs/decisiones.md).
