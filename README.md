# ServicioCatalogo

Servicio de catálogo y sincronización.

## Requisitos

- Docker y Docker Compose v2.
- Java 25, solo si se ejecuta el servicio fuera de Docker.

## Configuración

```bash
cp .env.example .env
```

Completar la clave de la base, generar el secreto del JWT (`openssl rand -base64 64`) y cargar la URL y el JWT técnico de la cátedra en `.env`. El archivo no se versiona.

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

Por ahora solo la ejecutan los tests: los disparadores (arranque, chequeo periódico y notificación de Kafka) se agregan en los issues siguientes.

## Tests

```bash
./mvnw verify
```

Los tests de integración levantan su propio PostgreSQL 18 en un contenedor descartable (Testcontainers), así que solo requieren Docker en ejecución; no usan el `.env` ni la base del compose.

## Decisiones de diseño

El registro de decisiones técnicas, con alternativas descartadas y justificación, está en [docs/decisiones.md](docs/decisiones.md).
