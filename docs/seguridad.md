# Seguridad

Este documento reúne las decisiones de autenticación, autorización y protección de datos del servicio de catálogo. El contrato de los endpoints está en [contrato.md](contrato.md) y la justificación de cada decisión, en [decisiones/](decisiones/README.md).

## En resumen

- **Autenticación:** el servicio emite un JWT firmado con HS512 y turnos lo valida con el mismo secreto. Las contraseñas se guardan con Argon2id.
- **Autorización:** todo exige un token válido salvo el registro, el login, la salud del servicio y la documentación de la API. El usuario de cada pedido sale siempre del token.
- **Errores:** un rechazo no revela el motivo: un token inválido y uno vencido dan la misma respuesta, y un login fallido no dice si falló el usuario o la clave.
- **Secretos:** salen del entorno, no tienen valor por defecto y el servicio no arranca si faltan o son débiles. No aparecen en los logs, y un test lo comprueba.

## Contraseñas

- El hash es **Argon2id** con los mínimos de OWASP: 19 MiB de memoria, 2 iteraciones y paralelismo 1, con sal de 16 bytes ([decisión 008](decisiones/008-hash-de-contrasenas-argon2id.md)). El costo se configura por entorno (`PASSWORD_ARGON2_MEMORY_KIB`, `PASSWORD_ARGON2_ITERATIONS` y `PASSWORD_ARGON2_PARALLELISM`) y el servicio **no arranca** si una falta o está por debajo del mínimo, para que nadie debilite el hash por error.
- El hash guarda sus propios parámetros, así que cambiar el costo más adelante no invalida los ya guardados.
- La contraseña admite de 4 a 100 caracteres. El máximo evita hashear entradas enormes con un algoritmo costoso, y en el login se aplica el mismo tope.
- La contraseña en claro no se guarda ni se escribe en ningún log. Tampoco se devuelve: `GET /api/account` nunca incluye la contraseña ni su hash.

## Login

- Se entra con el login o con el email, sin distinguir mayúsculas. Un usuario inexistente, una clave incorrecta y una cuenta inactiva dan el mismo `401 INVALID_CREDENTIALS` ([decisión 014](decisiones/014-autenticacion-como-caso-de-uso.md)).
- Un login no puede ser el email de otro usuario, ni un email el login de otro: de lo contrario alguien podría registrar como login el email de una persona y bloquearle el ingreso. Si de todos modos hubiera dos cuentas para el mismo texto, se prueba la contraseña contra las dos y entra la que la acierte ([decisión 015](decisiones/015-login-y-email-que-se-confunden.md)).
- Un intento fallido deja un `WARN` en el log con la ruta y nada más: ni el login ni la contraseña que escribió el usuario. Alcanza para notar un abuso sin dejar un dato personal ([decisión 031](decisiones/031-logs-en-json-sin-secretos.md)).

## El token

- **Firma HS512** con un secreto compartido, `JWT_SECRET`: una clave en Base64 de al menos 512 bits (`openssl rand -base64 64`). Si falta, no es Base64 o es corta, el servicio no arranca. No hay valor por defecto: un despliegue olvidado firmaría con una clave pública ([decisión 012](decisiones/012-jwt-hs512-secreto-compartido.md)).
- **Contenido:** `sub` (login), `uid` (id numérico), `auth` (autoridades), `iat` y `exp`. Dura 24 horas, o 30 días con `rememberMe`. No lleva nombre ni apellido porque un claim queda fijo al emitirse ([decisión 013](decisiones/013-contenido-y-vigencia-del-token.md)).
- **Validación:** el decodificador solo acepta HS512, así que un token firmado con otro algoritmo se rechaza aunque use la clave correcta. Exige `exp`: el validador por defecto de Spring aceptaba un token bien firmado y sin `exp`, que no vencería nunca ([decisión 030](decisiones/030-claim-exp-obligatorio.md)).
- **Qué rechaza**, con un test para cada caso: un token firmado con otra clave, con HS256 y la clave correcta, vencido, sin firma, con el cuerpo alterado, mal formado y sin `exp`.
- **Respuesta al rechazo:** siempre `401 UNAUTHORIZED` en `application/problem+json`, con la cabecera `WWW-Authenticate: Bearer` pero sin `error_description`: no se distingue si el token está vencido, mal firmado o ausente. El cliente solo necesita saber que debe volver a iniciar sesión ([decisión 017](decisiones/017-respuestas-401-y-403.md)).

## Autorización

- **Rutas públicas:** `POST /api/register`, `POST /api/authenticate`, `GET /actuator/health` y la documentación de la API (`/v3/api-docs` y `/swagger-ui`). Todo lo demás exige un token válido.
- **Roles:** hoy existe un solo rol, `ROLE_USER`, y todo usuario autenticado puede consultar el catálogo y el estado de la sincronización. La seguridad por método (`@PreAuthorize`) está habilitada para pedir roles sin cambiar la configuración. Un usuario autenticado sin el rol necesario recibe `403 FORBIDDEN`; como ningún endpoint real pide un rol, un test lo ejercita con un controller de prueba.
- **Quién es el usuario:** sale siempre del claim `uid`, nunca del cuerpo ni de la URL. Por eso `GET` y `POST /api/account` solo pueden leer y modificar la cuenta de quien presenta el token ([decisión 016](decisiones/016-validacion-del-token-sin-sesion.md)).
- **Las credenciales no salen de este servicio:** turnos nunca ve una contraseña ni un hash, solo valida el token.

## Entradas

- Los cuerpos se validan con Bean Validation y los errores devuelven `400 VALIDATION_ERROR` con el campo y el motivo. Un parámetro de tipo equivocado devuelve `400` antes de llegar al controller.
- En las búsquedas, `sort` acepta solo una lista fija de campos (los nombres de la respuesta, no los de las columnas); el resto es un `400`. El `size` tiene un tope y un valor mayor se reduce.
- El texto de `name` se limita a 100 caracteres y se escapan los comodines de `LIKE` (`%`, `_` y `\`), de modo que se busca como texto y no como un patrón ([decisión 027](decisiones/027-busqueda-de-profesionales.md)).
- Las respuestas de error no exponen mensajes internos: el `500` lleva un detalle genérico, y el mensaje de una excepción de la base, que puede traer la sentencia SQL o los valores, solo llega al log.

## Secretos y datos sensibles

- Los secretos (`JWT_SECRET`, la clave de la base, `CATEDRA_JWT` y las credenciales de Redis) salen de variables de entorno, definidas en un `.env` que **nunca se versiona**. `.env.example` es la plantilla y trae `cambiar` en lugar de cada secreto. Ninguno tiene valor por defecto y, si falta uno, el servicio no arranca con un mensaje que nombra la propiedad.
- El JWT técnico de la cátedra llega solo por el entorno: `CatedraProperties` oculta su valor en `toString()`, y los mensajes de error de los clientes no incluyen el cuerpo de la respuesta ni cabeceras ([decisión 019](decisiones/019-cliente-rest-de-la-catedra.md)).
- **Los logs no llevan secretos, y está probado.** Todas las líneas se escriben con un mensaje fijo y los datos como campos, y `LogStyleTest` falla si alguien escribe una de otra forma. `SecretsNotInLogsIntegrationTest` corre los flujos reales (registro, login acertado y fallido, una violación de integridad y la sincronización contra una cátedra que repite el JWT técnico en sus errores) y captura mensaje, campos y la traza completa con todas sus causas: ningún valor sentinela aparece. Gracias a ese test se corrigió una fuga real: el mensaje del parser de JSON repetía el comienzo del cuerpo de la respuesta.
- `sync_state` guarda los errores sin datos sensibles: de una falla de la cátedra, la clase y el mensaje que escribimos nosotros; de cualquier otra excepción, solo la clase.

## Superficie expuesta

- **CSRF desactivado:** CSRF protege a las aplicaciones que identifican al usuario con una cookie de sesión. Esta API no usa sesión ni cookies: cada pedido trae su JWT en la cabecera `Authorization`, que un sitio ajeno no puede agregar. SonarCloud marca la línea como vulnerabilidad crítica sin mirar el resto de la configuración, por eso lleva un `NOSONAR` con el motivo ([decisión 016](decisiones/016-validacion-del-token-sin-sesion.md)).
- **CORS queda fuera:** lo aplican los navegadores y el único cliente es la aplicación Android, así que no hay ningún origen que permitir. Si se sumara un cliente web, se agregaría con una variable de entorno con la lista de orígenes, vacía por defecto.
- **Documentación de la API:** está activa por defecto y el perfil `prod` desactiva Swagger UI y `/v3/api-docs`, para no revelar la superficie de la API en un despliegue real ([decisión 010](decisiones/010-springdoc-desactivable-por-perfil.md)).
- **Contenedores:** la imagen final solo trae el JRE y el JAR, corre con un usuario sin privilegios y los puertos se publican solo en `127.0.0.1` ([decisión 004](decisiones/004-docker-compose-por-repositorio.md)).
- **La cátedra:** el JWT técnico nunca sale del backend. Del Redis de la cátedra el servicio solo lee `catedra:sync:*` y nunca escribe: la cuenta técnica puede escribir únicamente en su propio espacio `alumnos:*`, que este servicio no usa. El broker de Kafka de la cátedra no pide autenticación ni cifrado, así que no hay credenciales que configurar.

## Límites conocidos

- **Un token no se puede revocar** antes de que venza: no hay una lista de tokens. La vigencia de 24 horas lo acota. Quien conozca `JWT_SECRET` puede fabricar tokens, así que el secreto debe cuidarse igual en los dos servicios.
- **El tiempo de respuesta del login no es constante.** El código y el cuerpo de la respuesta son iguales para un usuario inexistente y para uno existente con la clave equivocada, pero con un usuario inexistente no se calcula el hash de Argon2id y la respuesta tarda mucho menos. En una medición local fue de unos 10 ms contra unos 100 ms.
- **No hay límite de intentos de login.** El `WARN` de credenciales rechazadas deja rastro, pero el servicio no bloquea ni frena a quien insiste.
- **Memoria bajo carga.** Cada registro o login ocupa unos 19 MiB durante unos 30 ms, así que muchos a la vez suman memoria temporal.
- **Sin HTTPS en local.** El servicio escucha en HTTP; en un entorno real tiene que publicarse detrás de HTTPS.
