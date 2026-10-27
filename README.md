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

Sin token, o con uno vencido, mal formado o firmado con otra clave, la respuesta es `401` en `application/problem+json` con `code` `UNAUTHORIZED` y la cabecera `WWW-Authenticate: Bearer`; no se informa el motivo. Un usuario autenticado sin el rol necesario recibe `403` con `code` `FORBIDDEN`.

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

Un fallo al procesar se reintenta 3 veces, esperando 1, 2 y 4 segundos (`catedra.kafka.max-retries` y `catedra.kafka.initial-backoff`). Si se agotan los reintentos, o el mensaje no se puede interpretar, se registra un error y se pasa al siguiente mensaje: no hay cola de eventos muertos. Una notificación perdida la recupera la verificación de versión al arrancar y cada cierto tiempo, que se agrega en un issue siguiente: hasta entonces, la copia queda atrasada hasta el próximo evento.

Kafka no forma parte de `/actuator/health`, igual que Redis.

## Tests

```bash
./mvnw verify
```

Los tests de integración levantan su propio PostgreSQL 18 y, los del consumidor, un Kafka 4.0 en contenedores descartables (Testcontainers), así que solo requieren Docker en ejecución; no usan el `.env` ni la base del compose.

## Decisiones de diseño

El registro de decisiones técnicas, con alternativas descartadas y justificación, está en [docs/decisiones.md](docs/decisiones.md).
