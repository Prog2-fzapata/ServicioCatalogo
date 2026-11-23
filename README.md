<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-78%2F100-green) ![Aprobar](https://img.shields.io/badge/Aprobar-s%C3%AD-brightgreen) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-1.8%25-orange) ![%POO](https://img.shields.io/badge/%25POO-64%25-yellow) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-4-red) ![D2](https://img.shields.io/badge/D2-100-green) ![D4](https://img.shields.io/badge/D4-90-green) ![D5](https://img.shields.io/badge/D5-100-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-100-green)

**Calidad (heurístico, 7 dimensiones):** índice **78/100** · build **OK** · aprobar **sí (8/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

# ServicioCatalogo

Servicio de catálogo y sincronización. Mantiene una copia local del catálogo de profesionales de la cátedra (categorías, profesionales y horarios semanales) y se la ofrece a la aplicación Android y al servicio de turnos, sin consultar a la cátedra en cada pedido. También registra a los usuarios y emite el JWT con el que se autentican los dos servicios.

## Documentos

| Documento | Qué explica |
| --- | --- |
| [Arquitectura](docs/arquitectura.md) | El sistema completo (aplicación Android, los dos servicios, sus bases y la cátedra) y la estructura interna del servicio |
| [Modelo de datos](docs/modelo-de-datos.md) | Las tablas, sus restricciones y la propiedad de los datos |
| [Contrato](docs/contrato.md) | Operaciones, DTO, autenticación y errores, y lo que turnos consume de este servicio |
| [Sincronización](docs/sincronizacion.md) | Cómo se mantiene la copia: snapshot, incremental y verificación de versión |
| [Idempotencia y recuperación](docs/idempotencia-y-recuperacion.md) | Qué pasa con los mensajes duplicados, perdidos y fuera de orden |
| [Seguridad](docs/seguridad.md) | Autenticación, autorización y protección de datos |
| [Decisiones de diseño](docs/decisiones/README.md) | Cada decisión técnica, con sus alternativas descartadas y consecuencias |

## Requisitos

- Docker y Docker Compose v2.
- Java 25, para ejecutar las pruebas automatizadas (`./mvnw verify`).

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

No son secretos ni dependen de la máquina, así que **tienen un valor por defecto** (en `application.properties`) y no hace falta definirlos. Si se define la variable de entorno, esa **gana**: se define en el shell o en el `.env`, y el compose la pasa al servicio solo si está definida. Los formatos de tiempo son `500ms`, `10s`, `5m`.

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

## Endpoints

Todos exigen `Authorization: Bearer <id_token>`, salvo los públicos. Los parámetros, los DTO y los errores de cada uno están en [docs/contrato.md](docs/contrato.md).

| Método y ruta | Acceso | Qué hace |
| --- | --- | --- |
| `POST /api/register` | público | Registra un usuario |
| `POST /api/authenticate` | público | Devuelve el JWT (`id_token`) |
| `GET /api/account` | JWT | Devuelve la cuenta del usuario autenticado |
| `POST /api/account` | JWT | Actualiza los datos editables de la cuenta, solo los que se envían |
| `GET /api/professional-categories` | JWT | Lista las categorías, con el filtro `enabled` y paginadas |
| `GET /api/professionals` | JWT | Busca profesionales por `categoryId`, `name`, `enabled` y `available`, paginados |
| `GET /api/professionals/{id}` | JWT | Devuelve la agenda vigente de un profesional, con todos sus horarios |
| `GET /api/sync/status` | JWT | Informa la versión local y el resultado de la última sincronización |
| `GET /actuator/health` | público | Estado del servicio |

Algunos ejemplos, con el `id_token` que devuelve `POST /api/authenticate`:

```bash
# Profesionales con «gomez» en el nombre y al menos un horario habilitado, de a 10, por apellido
curl "localhost:8081/api/professionals?name=gomez&available=true&size=10&sort=lastName,asc" \
  -H "Authorization: Bearer <id_token>"
# La agenda vigente de un profesional
curl localhost:8081/api/professionals/15 -H "Authorization: Bearer <id_token>"
# Las categorías habilitadas
curl "localhost:8081/api/professional-categories?enabled=true" -H "Authorization: Bearer <id_token>"
```

Las listas devuelven un arreglo JSON, con el total en la cabecera `X-Total-Count` y los enlaces a las otras páginas en `Link`.

## Sincronización del catálogo

Las búsquedas se responden solo con la copia local, y el servicio la mantiene al día por su cuenta:

- **Snapshot:** con la base vacía, o cuando ya no se puede seguir por incrementos, pide el catálogo completo a la API de la cátedra y lo aplica en una sola transacción.
- **Incremental:** lee de Redis qué cambió en cada versión y las aplica de a una, en orden.
- **Aviso de Kafka:** la cátedra publica `CatalogUpdated` cada vez que hay una versión nueva. El aviso solo dispara la sincronización, y un evento repetido se descarta por su `eventId`.
- **Verificación de versión:** unos segundos después de arrancar y cada 5 minutos, el servicio compara su versión con la de Redis. Así recupera lo que un aviso perdido deje atrasado, y con la base vacía se llena solo, sin esperar un evento.

Si la cátedra, Redis o Kafka no responden, el servicio sigue sano y sirve la copia local: ninguno forma parte de `/actuator/health`. El estado se consulta con `GET /api/sync/status`:

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

`localVersion` vale `0` en una base que nunca se sincronizó. `lastSyncType` es `FULL` o `INCREMENTAL` y `lastStatus` es `OK` o `ERROR`; los errores se guardan sin datos sensibles.

La estrategia completa está en [docs/sincronizacion.md](docs/sincronizacion.md), y qué pasa con los mensajes duplicados, perdidos y fuera de orden, en [docs/idempotencia-y-recuperacion.md](docs/idempotencia-y-recuperacion.md).

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
| Un evento de Kafka, con su resultado | `Evento de catálogo: aplicado` (`syncType`, `fromVersion`, `toVersion`, `snapshotReason` si hubo snapshot) y `Evento de catálogo: nada que aplicar. …` para un duplicado o una copia que ya estaba al día; ambos con `eventId` y `eventVersion` |
| La verificación de versión que aplicó algo | `Verificación de versión: aplicado`, con los mismos campos que el evento, sin `eventId` |
| La cátedra que retrocede de versión (se aplica igual: es la fuente de verdad) | `…: la cátedra retrocedió la versión del catálogo` (`WARN`, `fromVersion`, `toVersion`) |
| Una vuelta de verificación que falló | `La verificación de versión falló; se reintenta en la próxima vuelta` (`ERROR`, la excepción en `error`) |
| Un login o una contraseña que no coinciden | `Credenciales rechazadas` (`WARN`, `path`) |
| Una violación de integridad que no es de login ni de email repetidos | `Violación de integridad no prevista` (`ERROR`, `path`, `exception`, `sqlState`) |
| Los errores inesperados, con su traza | `Error inesperado` (`ERROR`, `path`, la excepción en `error`) |

La verificación de versión **sin cambios** no sale por defecto: corre cada `CATEDRA_SYNC_CHECK_INTERVAL` (5 minutos) y escribiría la misma línea una y otra vez, así que va a `DEBUG`. Un evento duplicado de Kafka sí queda en `INFO`, porque es la traza de la deduplicación. Los clientes de Kafka vuelcan unas cien líneas con su configuración cada vez que arrancan: `ConsumerConfig` y `AdminClientConfig` están en `WARN` (`logging.level.…` en `application.properties`), y el resto de lo que dice Kafka sigue visible.

**Nunca se escribe** la contraseña, el email ni el login de un usuario (tampoco en un intento fallido), el JWT del usuario, el JWT técnico de la cátedra, la clave de Redis, el secreto de firma ni la contraseña de la base. Lo comprueba `SecretsNotInLogsIntegrationTest`, que corre los flujos reales con PostgreSQL y Redis en contenedores y una cátedra simulada que repite el JWT técnico en sus errores. Cómo se garantiza está en [docs/seguridad.md](docs/seguridad.md).

## Tests

```bash
./mvnw verify
```

Corre las pruebas unitarias y de integración y genera el informe de cobertura (JaCoCo) en `target/site/jacoco/index.html`. Los tests de integración levantan su propio PostgreSQL 18 y, según el caso, un Kafka 4.0 o un Redis 7 en contenedores descartables (Testcontainers), así que solo requieren Docker en ejecución; no usan el `.env` ni la base del compose. La primera vez se descargan las imágenes.

Para correr una clase o un método:

```bash
./mvnw test -Dtest=SynchronizeCatalogIntegrationTest
./mvnw test -Dtest='AuthenticationIntegrationTest#healthAndAuthenticateStayPublic'
```

Qué clases cubren cada comportamiento que pide el enunciado:

| Comportamiento | Clases de test principales |
| --- | --- |
| Registro y autenticación | `AccountRegistrationIntegrationTest`, `AuthenticationIntegrationTest`, `RegisterUserUseCaseImplTest`, `AuthenticateUserUseCaseImplTest`, `Argon2PasswordHasherTest` |
| Autorización | `SecurityRejectionTest`, `JwtConfigurationTest`, `AuthenticationIntegrationTest` |
| Sincronización completa e incremental | `SynchronizeCatalogIntegrationTest`, `SynchronizeCatalogIncrementalIntegrationTest`, `SynchronizeCatalogUseCaseImplTest`, `CatalogVersionCheckSchedulerIntegrationTest`, `CatedraSyncSourceIntegrationTest` |
| Idempotencia | `CatalogEventListenerIntegrationTest` (evento repetido, reintentos y mensaje inválido), `SynchronizeCatalogIncrementalIntegrationTest` (evento ya procesado o viejo) |
| Búsqueda y detalle | `ProfessionalSearchIntegrationTest`, `CategorySearchIntegrationTest`, `ProfessionalDetailIntegrationTest` |
| Contrato de los endpoints (DTO, errores y paginación) | `ProfessionalControllerTest`, `ProfessionalCategoryControllerTest`, `AccountControllerTest`, `PaginationHeadersTest` |
| Logs sin secretos | `SecretsNotInLogsIntegrationTest`, `LogStyleTest` |

Las reservas son del servicio de turnos.
