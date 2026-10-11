# ServicioCatalogo

Servicio de catálogo y sincronización.

## Requisitos

- Docker y Docker Compose v2.
- Java 25, solo si se ejecuta el servicio fuera de Docker.

## Configuración

```bash
cp .env.example .env
```

Completar la clave de la base y generar el secreto del JWT (`openssl rand -base64 64`) en `.env`. El archivo no se versiona.

| Variable | Descripción | Por defecto |
| --- | --- | --- |
| `CATALOGO_DB_NAME` | Nombre de la base de datos | `catalogo_db` |
| `CATALOGO_DB_USER` | Usuario de la base | `catalogo_user` |
| `CATALOGO_DB_PASSWORD` | Clave del usuario | |
| `POSTGRES_PORT` | Puerto del host donde se publica PostgreSQL | `5432` |
| `CATALOGO_PORT` | Puerto del host donde se publica el servicio | `8081` |
| `JWT_SECRET` | Clave con la que se firman los tokens (HS512): Base64 de al menos 512 bits. Si falta o es más corta, el servicio no arranca. Turnos debe usar el mismo valor | |
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

**Limitación actual:** Swagger UI todavía no tiene el botón «Authorize», así que desde esa pantalla se pueden probar el registro y el login pero no los endpoints protegidos (`GET` y `POST /api/account`), que responden `401`. Para esos se copia el `id_token` de `/api/authenticate` y se usa con `curl` (ver la sección Autenticación).

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

## Tests

```bash
./mvnw verify
```

Los tests de integración levantan su propio PostgreSQL 18 en un contenedor descartable (Testcontainers), así que solo requieren Docker en ejecución; no usan el `.env` ni la base del compose.

## Decisiones de diseño

El registro de decisiones técnicas, con alternativas descartadas y justificación, está en [docs/decisiones.md](docs/decisiones.md).
