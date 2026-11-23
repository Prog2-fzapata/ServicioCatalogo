# Contrato del servicio de catálogo

Lo consumen la **aplicación Android** (todos los endpoints) y el **servicio de turnos** (solo dos, ver [más abajo](#lo-que-turnos-consume-de-catálogo)). Con el servicio en ejecución, `/swagger-ui.html` es la interfaz interactiva para probar los endpoints y `/v3/api-docs` es el documento OpenAPI en JSON del que sale; ambos se generan a partir del código. Este documento explica el contrato en lenguaje llano.

## Convenciones

- Todas las rutas empiezan con `/api`. El JSON usa `camelCase`, salvo `id_token`, que conserva el nombre de JHipster: es la misma convención que usa el contrato de la cátedra.
- Fechas `yyyy-MM-dd`, instantes ISO-8601 en UTC y horas `HH:mm:ss`.
- Las listas se paginan al estilo de JHipster: `page` (desde 0), `size` y `sort` (`campo,asc` o `campo,desc`). La respuesta es un **arreglo JSON** y el total y los enlaces van en las cabeceras `X-Total-Count` y `Link` (`first`, `prev`, `next`, `last`, con los mismos filtros y orden). `size` vale 20 por defecto y 100 como máximo: un valor mayor se reduce al tope, no es un error.
- Los mensajes de `detail` están en español. El cliente decide por `status` y `code`, nunca por el texto.
- En local el servicio escucha en `http://localhost:8081` y sin HTTPS; en un entorno real habría que publicarlo detrás de HTTPS.

## Autenticación

Todo exige `Authorization: Bearer <jwt>` salvo `POST /api/register`, `POST /api/authenticate`, `GET /actuator/health` y la documentación de la API.

**El servicio emite el JWT y turnos lo valida.** La firma es HMAC **HS512** con un secreto compartido (`JWT_SECRET`, en Base64 y de al menos 512 bits) que ambos servicios leen de su entorno ([decisión 012](decisiones/012-jwt-hs512-secreto-compartido.md)). El token lleva:

| Claim | Contenido |
|---|---|
| `sub` | el `login` del usuario |
| `uid` | el `id` numérico del usuario |
| `auth` | las autoridades separadas por espacio, por ejemplo `ROLE_USER` |
| `iat` y `exp` | emisión y vencimiento (obligatorio: un token sin `exp` no se acepta, [decisión 030](decisiones/030-claim-exp-obligatorio.md)) |

Dura 24 horas, o 30 días si el login pide `rememberMe` ([decisión 013](decisiones/013-contenido-y-vigencia-del-token.md)). No lleva nombre ni apellido: un claim queda fijo al emitirse, así que quien los necesita los consulta en `GET /api/account`. El usuario de cada pedido sale **siempre** del claim `uid`, nunca del cuerpo ni de la URL.

Un token ausente, mal formado, vencido, sin `exp` o firmado con otra clave recibe el mismo `401 UNAUTHORIZED`: no se distingue el motivo ([decisión 017](decisiones/017-respuestas-401-y-403.md)). Los detalles de seguridad están en [seguridad.md](seguridad.md).

## Operaciones

### Usuarios

#### `POST /api/register` (público)

Registra un usuario nuevo, que queda activo de inmediato. El modelo es compatible con el usuario de JHipster.

```json
{
  "login": "juan.perez",
  "password": "una-clave",
  "firstName": "Juan",
  "lastName": "Perez",
  "email": "juan@example.com",
  "imageUrl": null,
  "langKey": "es"
}
```

| Campo | Obligatorio | Restricciones |
|---|---|---|
| `login` | sí | de 3 a 50 caracteres, con el patrón de login de JHipster; único sin distinguir mayúsculas (se guarda en minúsculas) |
| `password` | sí | de 4 a 100 caracteres |
| `firstName`, `lastName` | no | hasta 50 caracteres cada uno |
| `email` | sí | un email válido de 5 a 254 caracteres; único sin distinguir mayúsculas (se guarda en minúsculas) |
| `imageUrl` | no | hasta 256 caracteres |
| `langKey` | no | de 2 a 10 caracteres; `es` por defecto |

Responde `201 Created` sin cuerpo. `id`, `activated` y las autoridades los asigna el servicio; si llegan en el cuerpo se ignoran. `langKey` se guarda y se devuelve en `GET /api/account` por compatibilidad con JHipster, pero no cambia el comportamiento del servicio: los mensajes están siempre en español. Errores: `400 VALIDATION_ERROR`, `400 LOGIN_ALREADY_USED` y `400 EMAIL_ALREADY_USED`. Un login no puede ser el email de otro usuario, ni un email el login de otro ([decisión 015](decisiones/015-login-y-email-que-se-confunden.md)).

#### `POST /api/authenticate` (público)

```json
{ "username": "juan.perez", "password": "una-clave", "rememberMe": false }
```

`username` es el login o el email, sin distinguir mayúsculas. `password` admite hasta 100 caracteres (sin mínimo: una clave corta es una credencial incorrecta). `rememberMe` es opcional y vale `false` si falta.

Responde `200 OK` con `{ "id_token": "<jwt>" }` y el mismo token en la cabecera `Authorization: Bearer <jwt>`. Errores: `401 INVALID_CREDENTIALS` (usuario inexistente, clave incorrecta o cuenta inactiva: no se distingue cuál) y `400 VALIDATION_ERROR`.

#### `GET /api/account`

Devuelve la cuenta del usuario autenticado. Nunca incluye la contraseña ni su hash.

```json
{
  "id": 42,
  "login": "juan.perez",
  "firstName": "Juan",
  "lastName": "Perez",
  "email": "juan@example.com",
  "imageUrl": null,
  "langKey": "es",
  "authorities": ["ROLE_USER"]
}
```

Errores: `401` sin token válido y `404 USER_NOT_FOUND` si el usuario del token ya no existe.

#### `POST /api/account`

Actualiza los datos editables del usuario autenticado, y solo los que se envían: todos los campos son opcionales y los que faltan (o van en `null`) conservan su valor ([decisión 032](decisiones/032-actualizacion-parcial-de-la-cuenta.md)). Un cuerpo vacío, `{}`, es válido y no cambia nada.

```json
{ "firstName": "Juan", "lastName": "Perez" }
```

```json
{ "email": "juan@example.com" }
```

Para borrar el nombre, el apellido o la imagen se envía un texto vacío (`""`): el dato queda como si nunca se hubiera cargado. Tiene las mismas restricciones de formato y largo que el registro; un `email` que llega debe ser válido (uno vacío es un `400`). `login`, contraseña, autoridades e `id` no se cambian por acá. Responde `200 OK` sin cuerpo. Errores: `401`, `404 USER_NOT_FOUND`, `400 VALIDATION_ERROR` y `400 EMAIL_ALREADY_USED` (solo si el email que llega es el email o el login de otro usuario; conservar el propio es válido).

### Catálogo

Se resuelve siempre sobre la copia local: ningún endpoint llama a la cátedra. No hay endpoints de escritura, la única fuente de datos es la [sincronización](sincronizacion.md).

#### `GET /api/professional-categories`

| Parámetro | Descripción |
|---|---|
| `enabled` | `true` o `false`; sin valor devuelve habilitadas y deshabilitadas |
| `page`, `size`, `sort` | paginación; `sort` solo por `id` o `name` (otro campo es `400 VALIDATION_ERROR`); por defecto, por `name` |

```json
[
  { "id": 1, "name": "Clinica medica", "description": "Atencion clinica general", "enabled": true }
]
```

#### `GET /api/professionals`

| Parámetro | Descripción |
|---|---|
| `categoryId` | solo los de esa categoría |
| `name` | texto parcial sobre nombre y apellido juntos, sin distinguir mayúsculas ni tildes (hasta 100 caracteres) |
| `enabled` | `true` o `false`; sin valor devuelve habilitados y deshabilitados |
| `available` | `true`: tiene al menos un horario semanal habilitado; `false`: no tiene ninguno. Es una disponibilidad **local**: los turnos libres de verdad los resuelve turnos con las ocupaciones de la cátedra |
| `page`, `size`, `sort` | paginación; `sort` solo por `id`, `firstName` o `lastName` (otro campo es `400 VALIDATION_ERROR`); por defecto, apellido y nombre |

Los filtros se combinan con AND. Cada elemento trae solo los horarios **habilitados**, lo que alcanza para el resumen de la lista:

```json
[
  {
    "id": 15,
    "categoryId": 1,
    "categoryName": "Clinica medica",
    "firstName": "Ana",
    "lastName": "Gomez",
    "enabled": true,
    "weeklySchedules": [
      { "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "12:00:00", "slotDurationMinutes": 30 }
    ]
  }
]
```

#### `GET /api/professionals/{id}`

Es la **agenda vigente** de un profesional. Trae **todos** sus horarios, también los deshabilitados, cada uno con su `id` y su `enabled`, ordenados por día de la semana y hora de inicio, y además `categoryEnabled`, que dice si su categoría está habilitada.

```json
{
  "id": 15,
  "categoryId": 1,
  "categoryName": "Clinica medica",
  "categoryEnabled": true,
  "firstName": "Ana",
  "lastName": "Gomez",
  "enabled": true,
  "weeklySchedules": [
    {
      "id": 21,
      "dayOfWeek": "MONDAY",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "slotDurationMinutes": 30,
      "enabled": true
    }
  ]
}
```

Un profesional o una categoría deshabilitados se devuelven igual, con `enabled = false`: el catálogo informa lo que tiene y quien consume decide qué hacer ([decisión 028](decisiones/028-detalle-de-profesional-y-categorias.md)). Error: `404 PROFESSIONAL_NOT_FOUND`, solo para un id que no existe.

### Sincronización

#### `GET /api/sync/status`

Cualquier usuario autenticado puede consultarlo. Una base que nunca se sincronizó responde `200` con `localVersion` en `0` y el resto en `null`.

```json
{
  "localVersion": 7,
  "lastSyncAt": "2026-10-07T13:44:59Z",
  "lastSyncType": "INCREMENTAL",
  "lastStatus": "OK",
  "lastError": null
}
```

`lastSyncType` es `FULL` o `INCREMENTAL` y `lastStatus` es `OK` o `ERROR`. `lastError` es un mensaje corto que nunca trae datos sensibles ([decisión 026](decisiones/026-estado-de-la-sincronizacion.md)).

## Errores

Todos los errores salen como `application/problem+json`, con el mismo formato que usa la cátedra:

```json
{
  "detail": "No existe el profesional con id 99",
  "instance": "/api/professionals/99",
  "status": 404,
  "title": "Not Found",
  "path": "/api/professionals/99",
  "code": "PROFESSIONAL_NOT_FOUND"
}
```

`instance` lo agrega Spring con el mismo valor que `path`; los clientes pueden ignorarlo ([decisión 011](decisiones/011-errores-de-spring-en-espanol.md)). Los `401` y `403` los escribe el filtro de seguridad, antes de llegar a Spring MVC, y no lo traen. Los errores de validación suman `fieldErrors`:

```json
{
  "detail": "Los datos enviados no son válidos",
  "instance": "/api/register",
  "status": 400,
  "title": "Bad Request",
  "path": "/api/register",
  "code": "VALIDATION_ERROR",
  "fieldErrors": [
    { "objectName": "userRegistrationRequest", "field": "email", "message": "El email no es válido" }
  ]
}
```

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | el cuerpo no es un JSON válido o no cumple las restricciones, o los parámetros de búsqueda no son válidos (un campo de `sort` no permitido, un `name` de más de 100 caracteres); trae `fieldErrors` salvo con un JSON mal formado |
| 400 | `BAD_REQUEST` | una solicitud que Spring rechaza antes del controller, como un parámetro de tipo equivocado (un `id` que no es un número) |
| 400 | `LOGIN_ALREADY_USED`, `EMAIL_ALREADY_USED` | el login o el email ya están en uso |
| 401 | `UNAUTHORIZED` | falta el token, o está vencido, mal formado, sin `exp` o firmado con otra clave |
| 401 | `INVALID_CREDENTIALS` | login con usuario inexistente, clave incorrecta o cuenta inactiva |
| 403 | `FORBIDDEN` | autenticado, pero sin permiso para la operación |
| 404 | `USER_NOT_FOUND`, `PROFESSIONAL_NOT_FOUND` | el recurso no existe |
| 404 | `NOT_FOUND` | la ruta no existe |
| 405 | `METHOD_NOT_ALLOWED` | el método HTTP no está permitido para la ruta |
| 406 | `NOT_ACCEPTABLE` | el formato de respuesta pedido no está disponible |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | el `Content-Type` no está soportado |
| 500 | `INTERNAL_ERROR` | un error inesperado; el detalle es genérico a propósito, sin mensajes internos |

## Lo que turnos consume de catálogo

Turnos llama a catálogo con el **JWT del usuario**, el mismo que recibió, y solo usa dos operaciones:

- `GET /api/professionals/{id}`: la agenda vigente. Lee `id`, `categoryId`, `categoryName`, `categoryEnabled`, `firstName`, `lastName` y `enabled` del profesional y, de cada horario, `id`, `dayOfWeek`, `startTime`, `endTime`, `slotDurationMinutes` y `enabled`. Con eso genera los turnos libres y no ofrece horarios de un profesional ni de una categoría deshabilitados.
- `GET /api/account`: lee `firstName` y `lastName` del paciente al iniciar una reserva.

Las reglas del acuerdo entre los dos servicios:

- **Compatibilidad.** Catálogo puede agregar campos a una respuesta sin avisar: turnos ignora los que no conoce. Lo que no puede hacer es quitar un campo, cambiarle el nombre, el tipo o el significado. Por eso `categoryEnabled` se agregó como un campo nuevo y el `enabled` del profesional no cambió de significado ([decisión 029](decisiones/029-category-enabled-en-el-detalle.md)). Si turnos no recibe `categoryEnabled` lo trata como un desajuste entre los servicios.
- **Errores.** Un `404 PROFESSIONAL_NOT_FOUND` es un profesional que no existe. Un `5xx`, un timeout o una conexión caída hacen que turnos responda `503 CATALOG_UNAVAILABLE` y no inicie operaciones nuevas; solo ese caso se reintenta.
- **Identidad.** Turnos valida el token con el mismo secreto y exige que traiga `exp`, `uid` y `auth`. Si falta alguno lo rechaza con `401`.
