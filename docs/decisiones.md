# Decisiones de diseño: servicio de catálogo

Registro de las decisiones técnicas de este servicio: contexto, decisión, alternativas descartadas y justificación. Cada entrada se agrega cuando la decisión queda implementada en el código.

## 1. Stack: Maven, Java 25 y Spring Boot 4.1.1

- **Contexto:** el enunciado no fija herramientas de construcción ni versiones.
- **Decisión:** Maven con wrapper (`./mvnw`), Java 25 (Temurin) y Spring Boot 4.1.1.
- **Justificación:** Spring Boot no publica una etiqueta LTS; 4.1.1 es la última versión estable al momento de empezar (la 4.2 solo existía como milestone). Java 25 es la versión de soporte largo vigente. El wrapper permite compilar sin instalar Maven.

## 2. Una base PostgreSQL propia por servicio

- **Contexto:** catálogo y turnos deben tener datos separados y delimitar su propiedad.
- **Decisión:** cada servicio tiene su propia instancia de PostgreSQL 18, con su base y su usuario. No hay claves foráneas entre servicios: las referencias son por valor.
- **Alternativa descartada:** una sola instancia con dos bases lógicas. Es más simple de operar, pero acopla el despliegue de ambos servicios a una misma máquina y a un mismo punto de falla.
- **Justificación:** cada servicio es dueño exclusivo de sus datos y puede desplegarse en un nodo distinto.

## 3. Esquema con Flyway y validación de Hibernate

- **Decisión:** el esquema lo crean solo las migraciones de Flyway (`src/main/resources/db/migration`) y Hibernate corre con `ddl-auto=validate`.
- **Alternativa descartada:** dejar que Hibernate genere el esquema (`update`/`create`). No es reproducible ni auditable, y puede modificar tablas sin dejar rastro.
- **Justificación:** el esquema queda versionado en git, se aplica igual en todos los entornos y `validate` detecta al arrancar cualquier diferencia entre las entidades y las tablas.

## 4. Docker: un compose por repositorio e imagen multi-stage

- **Decisión:** cada repositorio trae su `docker-compose.yml` (con su propia base) y un `Dockerfile` multi-stage: una etapa con JDK compila con Maven y la imagen final usa solo el JRE, corre con un usuario sin privilegios y contiene únicamente el JAR. Los puertos se publican solo en `127.0.0.1`.
- **Alternativa descartada:** un único compose compartido entre los repositorios. Obliga a clonar los demás repositorios para levantar uno solo.
- **Justificación:** cada servicio es autocontenido; con clonar un repositorio alcanza para ejecutarlo, y la imagen final es más chica y expone menos superficie.

## 5. Calidad: SonarCloud por GitHub Actions con cobertura de JaCoCo

- **Decisión:** el análisis corre en un workflow de GitHub Actions (no con el análisis automático de SonarCloud). JaCoCo mide la cobertura durante `./mvnw verify` y `sonar-maven-plugin` sube el análisis y el reporte.
- **Justificación:** el análisis automático no mide cobertura de tests. Con Actions, el mismo `verify` que ejecuta las pruebas alimenta a Sonar.

## 6. Pruebas de integración con Testcontainers

- **Contexto:** los tests de persistencia necesitan una base PostgreSQL real, porque dependen de restricciones únicas, claves foráneas y tipos propios del motor. El enunciado no exige una herramienta concreta.
- **Decisión:** cada ejecución de pruebas levanta un PostgreSQL 18 en un contenedor descartable (Testcontainers), conectado a la aplicación con `@ServiceConnection`. Es la misma imagen que usa el compose.
- **Alternativas descartadas:**
  - Usar la base del compose: los tests compartirían datos con el desarrollo manual (pueden chocar con registros cargados a mano) y consumirían ids de esa base.
  - Una base en memoria (H2): su SQL difiere de PostgreSQL, así que los tests podrían pasar con un comportamiento que en producción falla.
- **Consecuencias:** los tests solo requieren Docker en ejecución, no usan el `.env` y el workflow de CI no necesita un servicio de base de datos. Cada contexto de Spring distinto levanta su propio contenedor, lo que suma algunos segundos a la ejecución.

## 7. Modelo de usuarios compatible con JHipster

- **Contexto:** el enunciado exige que el modelo y el contrato de usuarios sean compatibles con el usuario de JHipster.
- **Decisión:** se usan solo los campos de JHipster (`login`, contraseña, `firstName`, `lastName`, `email`, `imageUrl`, `langKey`), sin agregar campos propios. Las tablas se llaman `users`, `authorities` y `user_authorities`; `login` y `email` son únicos con restricciones con nombre. `firstName` y `lastName` son opcionales, como en JHipster. Las identidades se relacionan por el `id` numérico, que no cambia nunca, por eso el email puede modificarse.
- **Justificación:** imitar el comportamiento observable de JHipster, como pidió el profesor. Las restricciones con nombre permiten traducir las violaciones de unicidad a errores de negocio precisos.

## 8. Hash de contraseñas con Argon2id

- **Contexto:** el enunciado exige almacenar las contraseñas con un mecanismo criptográfico adecuado y respetar las validaciones definidas por la cátedra, cuyo registro admite contraseñas de 4 a 100 caracteres. La primera versión usaba BCrypt precedido de un SHA-256 en Base64, porque BCrypt solo procesa 72 bytes y los caracteres como `ñ` pesan 2. El profesor indicó el 2026-10-24 que BCrypt quedó obsoleto y que se use Argon2id, que es lo que OWASP recomienda en primer lugar para guardar contraseñas.
- **Decisión:**
  - `Argon2PasswordHasher` implementa el puerto `PasswordHasher` con el `Argon2PasswordEncoder` de Spring Security, que usa las clases de Argon2 de BouncyCastle. La contraseña se hashea tal cual, sin paso previo: Argon2id no limita su largo.
  - Parámetros: los mínimos de OWASP, **19 MiB de memoria, 2 iteraciones y paralelismo 1**, con sal de 16 bytes y hash de 32. En la máquina de desarrollo cada hash tarda unos 32 ms (BCrypt con costo 10 tardaba unos 91 ms) y la memoria que pide Java coincide con el parámetro.
  - El costo se configura por entorno: `PASSWORD_ARGON2_MEMORY_KIB`, `PASSWORD_ARGON2_ITERATIONS` y `PASSWORD_ARGON2_PARALLELISM`, definidas en `.env`, sin valor por defecto en `application.properties`. `Argon2Properties` las valida al arrancar y el servicio no arranca si falta alguna o si está por debajo de los mínimos de OWASP, para que nadie debilite el hash por error. `Argon2Configuration` (en `shared/security`, como la del JWT) arma el encoder.
  - El hash guarda sus propios parámetros (`$argon2id$v=19$m=19456,t=2,p=1$...`): cambiar el costo más adelante no invalida los hashes ya guardados, que se verifican con el costo con el que se crearon.
  - La migración `V3` ensancha `users.password_hash` de `VARCHAR(60)` a `VARCHAR(255)`, porque un hash Argon2id mide 97 caracteres. La `V1` ya estaba aplicada y no se edita.
  - Los casos de uso no cambian: el dominio solo conoce `PasswordHasher`.
- **Alternativas descartadas:**
  - Seguir con BCrypt y SHA-256: contradice la indicación del profesor, y BCrypt es la generación anterior (OWASP lo deja para sistemas heredados).
  - Los valores por defecto de Spring (16 MiB): quedan algo por debajo del mínimo de OWASP.
  - Costos fijos en el código: cambiarlos exigiría recompilar.
  - Un hasher que reconozca BCrypt y Argon2id y vuelva a hashear al iniciar sesión: solo serviría con usuarios ya registrados, y todavía no hay.
  - `argon2-jvm`: es una biblioteca nativa, más difícil de desplegar.
- **Consecuencias:** cada registro o login ocupa unos 19 MiB durante unos 30 ms, así que 50 simultáneos serían alrededor de 1 GB de memoria temporal, y BouncyCastle suma unos 7 MB a la imagen. Un hash de otro formato (por ejemplo, de una base anterior con BCrypt) no se verifica: `matches` devuelve `false` sin lanzar excepción y el login responde como credenciales inválidas; un test lo comprueba. En una base de desarrollo con usuarios viejos hay que reiniciarla o registrarse de nuevo. El rango de 4 a 100 caracteres y el máximo de 100 en el login se mantienen: este último evita hashear entradas enormes con un algoritmo costoso.

## 9. Manejo de errores: problem+json con un handler por slice y uno compartido

- **Contexto:** todos los errores de la API deben tener el mismo formato (`application/problem+json` con `status`, `detail`, `path` y un `code` estable, más `fieldErrors` en las validaciones) y las excepciones de negocio no deben contener lógica HTTP.
- **Decisión:** las excepciones de negocio son *unchecked* y se traducen en dos niveles con `@RestControllerAdvice`:
  - Cada slice tiene su handler para sus excepciones (`UserExceptionHandler` convierte `LoginAlreadyUsedException` y `EmailAlreadyUsedException` en `400` con `LOGIN_ALREADY_USED` y `EMAIL_ALREADY_USED`). Se consulta primero (`@Order(HIGHEST_PRECEDENCE)`).
  - El paquete `shared.web` tiene el handler de lo que no pertenece a ningún slice: validación de datos (`VALIDATION_ERROR` con `fieldErrors`), JSON mal formado y errores inesperados (`500` con un detalle genérico, sin exponer mensajes internos), más un helper que arma el cuerpo `problem+json`. No contiene lógica de negocio ni importa de ningún slice.
  - Dos registros simultáneos con el mismo login o email pasan la verificación previa del caso de uso; la restricción única de la base frena al segundo, y el handler del slice traduce esa violación, reconocida por el nombre de la restricción (`uk_users_login`, `uk_users_email`), al mismo código que el chequeo previo.
- **Alternativas descartadas:**
  - `try/catch` en cada método del controller (como en la plantilla de la cátedra): repite código y deja fuera los errores que Spring lanza antes de llegar al controller.
  - Un único handler global con las excepciones de todos los slices: acopla los slices entre sí.
  - Un handler de validación en cada slice: Spring elegiría uno según el orden de los beans, lo que es difícil de predecir.
- **Consecuencias:** un slice nuevo agrega su handler sin tocar los demás. El orden entre handlers es explícito y está protegido por tests (sin `@Order` fallan cuatro). El paquete `shared` queda restringido a infraestructura web y no tiene capas propias.

## 10. Documentación de la API con SpringDoc, desactivable por perfil

- **Contexto:** la documentación de endpoints y DTO se genera a partir del código con SpringDoc (OpenAPI y Swagger UI), por lo que siempre coincide con la solución. Dejarla expuesta en un despliegue real revela la superficie de la API.
- **Decisión:** la documentación está activa por defecto, porque forma parte de lo que se entrega y quien evalúe el proyecto necesita verla al levantarlo con `docker compose up`. El entorno se declara en el `.env` con `SPRING_PROFILES_ACTIVE` (`dev` por defecto, valor que fija `.env.example`); el perfil `prod` (`application-prod.properties`) desactiva Swagger UI y `/v3/api-docs`.
- **Alternativa descartada:** desactivarla siempre y no dejar ninguna forma de consultarla en ejecución, lo que le quita utilidad como documentación entregable.
- **Consecuencias:** dos tests lo verifican: sin perfil (o con `dev`) el documento OpenAPI responde `200`, con el perfil `prod` ambos endpoints responden `404`.

## 11. Errores estándar de Spring en español y con el mismo formato

- **Contexto:** Spring responde por su cuenta algunos errores (ruta inexistente, método no permitido, tipo de contenido no soportado) con texto en inglés y sin `code`.
- **Decisión:** el handler compartido intercepta también esas respuestas y les agrega el `code` (`NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `BAD_REQUEST`), el `path` y un `detail` en español, para que todos los errores de la API tengan la misma forma. El `title` conserva el nombre estándar del estado HTTP.
- **Limitación conocida:** Spring agrega el campo `instance` (con el mismo valor que `path`) al escribir cualquier respuesta `ProblemDetail`, y no se puede omitir sin reemplazar esa clase por una propia. Es inofensivo para los clientes, que ignoran campos que no conocen.

## 12. JWT firmado con HS512 y secreto compartido

- **Contexto:** el servicio emite el JWT de usuario y turnos lo valida; ambos deben coincidir en la clave sin que la clave viaje por la red ni se versione.
- **Decisión:** firma HMAC con **HS512**. La clave es una variable de entorno (`JWT_SECRET`, Base64, al menos 512 bits, generada con `openssl rand -base64 64`) y turnos usa el mismo valor. Si la clave falta, no es Base64 o es corta, la aplicación no arranca. El decoder solo acepta HS512. Librería: `oauth2-resource-server` de Spring Security (Nimbus), la misma que usa JHipster.
- **Alternativas descartadas:**
  - HS256: alcanza en seguridad, pero se aparta del algoritmo por defecto de JHipster (se había elegido por error creyendo lo contrario; se corrigió antes de emitir ningún token).
  - RSA con par de claves: turnos solo tendría la clave pública y no podría fabricar tokens, pero hay que generar y distribuir archivos PEM; con dos servicios propios en el mismo entorno ese riesgo es mínimo.
  - Una clave con valor por defecto en la configuración: un despliegue olvidado quedaría firmando con una clave pública. Por eso no hay valor por defecto.
- **Consecuencias:** quien conozca la clave puede fabricar tokens, así que los dos `.env` deben cuidarse igual. Hay tests que rechazan tokens con otra clave, con HS256 y la clave correcta, vencidos, sin firma y con el cuerpo alterado.

## 13. Contenido y vigencia del token

- **Decisión:** el token lleva `sub` (login), `uid` (id numérico), `auth` (autoridades separadas por espacio), `iat` y `exp`. Dura 24 horas, o 30 días si el login pide `rememberMe`; ambos valores son propiedades (`jwt.token-validity`, `jwt.remember-me-validity`). Son los valores y el formato de JHipster.
- **Alternativa descartada:** incluir nombre y apellido como claims. Un claim queda fijo al emitir el token: si el usuario carga su nombre después, el token viejo seguiría sin él. Turnos lo consulta en `GET /api/account` al reservar.
- **Consecuencias:** un token no se puede revocar antes de que venza (no hay lista de tokens). Es una limitación conocida del esquema sin estado; la vigencia de 24 horas la acota.

## 14. Autenticación como caso de uso propio

- **Decisión:** `AuthenticateUserUseCase` busca al usuario por login y por email (sin distinguir mayúsculas), exige que esté activo, compara la contraseña con el puerto `PasswordHasher` y pide el token al puerto `TokenIssuer`, que implementa `JwtTokenIssuer` en infraestructura. No usa el `AuthenticationManager` de Spring Security.
- **Alternativa descartada:** el flujo estándar de Spring (`UserDetailsService` + `AuthenticationManager`): resuelve lo mismo, pero obliga a que las clases de Spring Security aparezcan en el dominio o a duplicar al usuario; con el caso de uso, el dominio sigue sin conocer el framework.
- **Consecuencias:** usuario inexistente, contraseña incorrecta y cuenta inactiva dan el mismo `401 INVALID_CREDENTIALS`, para no revelar cuál falló.

## 15. Login y email que se confunden

- **Contexto:** el patrón de login de JHipster admite logins con forma de email, así que un mismo texto podía ser el login de un usuario y el email de otro. Alguien podía registrar como login el email de otra persona y bloquearle el ingreso por email; y si elegía la misma contraseña que ella usa, hacerla entrar a su cuenta sin que lo note.
- **Decisión:** dos niveles.
  - **Prevención (al escribir):** el registro y `POST /api/account` rechazan un login igual al email de otro usuario y un email igual al login de otro (`LOGIN_ALREADY_USED` y `EMAIL_ALREADY_USED`, los mismos códigos de siempre). El propio usuario sí puede usar el mismo texto como login y como email.
  - **Respaldo (al ingresar):** si de todos modos hubiera dos cuentas para el mismo texto (dos registros simultáneos, datos cargados por otra vía), se prueba la contraseña contra las dos y entra la que la acierte; si acertaran ambas, gana el login. Un mismo usuario hallado por login y por email se verifica una sola vez, porque el hash es costoso.
- **Alternativas descartadas:**
  - Resolverlo solo al ingresar, sin prevenir al escribir: deja abierto el caso de la cuenta equivocada.
  - Prohibir `@` en el login: es lo más simple, pero se aparta del patrón de login de JHipster.
- **Consecuencias:** la base no puede garantizar con una restricción única que un valor no se repita entre dos columnas distintas, así que dos registros opuestos en el mismo instante podrían colarse; para ese caso queda el respaldo del ingreso.

## 16. Alcance de la seguridad: validación del token desde CAT-04

- **Contexto:** `GET` y `POST /api/account` necesitan saber quién llama, así que la validación del JWT no puede esperar al issue de seguridad.
- **Decisión:** la cadena de filtros (`shared.security.SecurityConfiguration`) valida el token, no usa sesión ni CSRF (cada pedido trae su token) y protege todo salvo el registro, el login, `/actuator/health` y la documentación. El usuario de cada pedido sale siempre del claim `uid`, nunca del cuerpo ni de la URL.
- **CSRF desactivado y SonarCloud:** CSRF protege a las aplicaciones que identifican al usuario con una cookie de sesión: otro sitio puede hacer que el navegador mande esa cookie sin que el usuario lo note. Esta API no usa sesión ni cookies; cada pedido trae su JWT en la cabecera `Authorization`, que un sitio ajeno no puede agregar. Por eso se desactiva (es lo que recomienda Spring Security para APIs con token), pero la regla `java:S4502` de Sonar lo marca como vulnerabilidad crítica sin mirar el resto de la configuración. Se dejó el `NOSONAR` en esa línea, con su motivo, en lugar de aceptar el aviso en silencio o activar CSRF sin sentido. **Alternativa descartada:** activar CSRF e ignorarlo para `/api/**`, que no protege nada (todas las rutas quedarían excluidas) y agrega configuración que no se usa. Si el profesor prefiere otro criterio, el cambio está en una línea de `SecurityConfiguration`.
- Los `401` y `403` del filtro y el esquema «Bearer JWT» de OpenAPI se resolvieron en la entrada 17.

## 17. Respuestas 401 y 403, seguridad por método y botón «Authorize»

- **Contexto:** los filtros de seguridad cortan el pedido antes de llegar a los controllers, así que los `@RestControllerAdvice` no los ven: el `401` salía sin cuerpo y con una cabecera `WWW-Authenticate` que incluye el motivo exacto del rechazo (por ejemplo `Jwt expired at ...` o `Invalid signature`). Además, Swagger UI no podía probar los endpoints protegidos.
- **Decisión:**
  - `ProblemDetailsSecurityHandler` (un solo componente que hace de `AuthenticationEntryPoint` y de `AccessDeniedHandler`) escribe el `401` y el `403` en `application/problem+json`, con el mismo formato que el resto de los errores (`status`, `title`, `detail`, `path`, `code`). Los textos viven en `ProblemDetailsFactory`.
  - `code`: `UNAUTHORIZED` y `FORBIDDEN`. No se distingue token vencido, mal firmado o ausente, ni en el cuerpo ni en la cabecera (`WWW-Authenticate: Bearer`, sin `error_description`): el cliente solo necesita saber que debe volver a iniciar sesión.
  - Seguridad por método (`@EnableMethodSecurity`) para poder pedir roles con `@PreAuthorize` en los casos de uso futuros. Como esa excepción se lanza dentro del controller, `GlobalExceptionHandler` la traduce a `403 FORBIDDEN`; sin ese handler el genérico devolvía `500`.
  - `OpenApiConfiguration` declara el esquema Bearer JWT: Swagger UI muestra el botón «Authorize», se pega el `id_token` una vez y los pedidos de la pantalla lo mandan. El registro y el login quedan marcados sin requisito de seguridad.
- **Alternativas descartadas:**
  - Delegar en un `HandlerExceptionResolver` para que los `@RestControllerAdvice` armen el `401`: reutiliza el handler global, pero agrega una capa de indirección que no hace falta para dos respuestas fijas.
  - Dejar la cabecera `WWW-Authenticate` por defecto de Spring (RFC 6750): informa el motivo del rechazo, que no necesita un cliente y facilita probar tokens.
  - Un `code` distinto por motivo (`TOKEN_EXPIRED`, `INVALID_TOKEN`): el contrato dice «sin garantía» para estos códigos y la app actúa igual en todos los casos.
- **CORS:** queda fuera por ahora. CORS lo aplican los navegadores y el único cliente es la app Android, así que no hay ningún origen que permitir. Si se suma un cliente web, se agrega una variable de entorno con la lista de orígenes (vacía por defecto).
- **Pruebas:** `SecurityRejectionTest` ejercita el `401` (sin token, mal formado, vencido, otra clave) y el `403` con un controller de prueba que pide `ROLE_ADMIN`, porque ningún endpoint real exige un rol todavía. Se comprobó que fallan si se quita el handler del `403` (da `500`) o el entry point del filtro (aparece el motivo en la cabecera).

## 18. Esquema de la copia local del catálogo (migración V2)

- **Contexto:** el servicio guarda una copia del catálogo de la cátedra (categorías, profesionales y horarios) y el estado de la sincronización. Las clases Java de esas tablas llegan con los casos de uso que las usan (sincronización y búsqueda); esta migración fija el esquema antes.
- **Decisión:**
  - Tablas `professional_categories`, `professionals`, `weekly_schedules`, `sync_state` y `processed_events`. Los nombres van en plural, como pide la convención de la cátedra para las entities y como `users`; `sync_state` queda en singular porque es una sola fila.
  - Los ids de categoría, profesional y horario son los de la cátedra y no se generan, para poder aplicar cambios por id. Las bajas son lógicas (`enabled = false`): no se borran filas.
  - Claves foráneas normales (profesional a categoría, horario a profesional) con un índice en cada una, porque PostgreSQL no los crea solo.
  - `day_of_week` es `VARCHAR` con `CHECK` de los siete valores. Los horarios exigen `end_time > start_time` y `slot_duration_minutes > 0`.
  - `sync_state` tiene una sola fila (`CHECK (id = 1)`) creada por la propia migración, con `local_version = 0` (significa «base vacía, hace falta un snapshot») y sin datos de última sincronización. `last_sync_type` admite `FULL` o `INCREMENTAL` y `last_status`, `OK` o `ERROR`.
  - `processed_events` usa el `eventId` como clave primaria: un evento repetido se rechaza.
- **Alternativas descartadas:**
  - Tipo `enum` de PostgreSQL para el día: es incómodo de modificar y no aporta nada frente a `VARCHAR` con `CHECK` con `@Enumerated(STRING)`.
  - Sin claves foráneas, o diferibles (se comprueban al final de la transacción): la integridad queda a cargo del código. Con claves normales alcanza con aplicar siempre categorías, profesionales y horarios en ese orden. Si la sincronización lo complicara, pasar a diferibles es un cambio chico.
  - Crear la fila de `sync_state` en la primera sincronización: cada lector tendría que manejar también el caso «no hay fila».
  - Índices de búsqueda por nombre y categoría: dependen de las consultas reales, que se miden al implementar la búsqueda.
- **Consecuencias:** la cátedra no documenta los largos de nombres y descripciones, así que se usaron límites amplios (255 y `TEXT`); si un dato real no entra, se amplía con otra migración. Los `VARCHAR` de `day_of_week`, `last_sync_type` y `last_status` miden lo justo para sus valores: agregar un valor nuevo exige ampliar la columna. El esquema se prueba directamente en SQL, con `CatalogSchemaTest` sobre un PostgreSQL real: se comprobó que cada restricción hace fallar al test que la cubre al quitarla de la migración.

## 19. Cliente REST de la cátedra para el snapshot del catálogo

- **Contexto:** el servicio necesita pedir a la cátedra el catálogo completo (`GET /api/synchronization/snapshot`, con el JWT técnico) para la sincronización completa. Es la primera comunicación saliente del servicio.
- **Decisión:**
  - El dominio define el puerto `out` `CatedraApiClient` (`fetchSnapshot()`), que devuelve un `CatalogSnapshot` del dominio. Su adapter, `CatedraApiClientAdapter`, usa `RestClient` de Spring sobre el cliente HTTP de Java (HTTP/1.1), sin dependencias nuevas. Los DTO de la respuesta y su mapper al dominio viven con el adapter.
  - La configuración de conexión (`CatedraProperties` y el bean `RestClient`) está en `shared/config`, fuera de los slices, porque la usarán varios (REST hoy; Redis y Kafka después). Así lo indicó el profesor para la configuración de la cátedra; la carpeta del adapter dentro de `infrastructure` está pendiente de confirmar.
  - `CATEDRA_BASE_URL` y `CATEDRA_JWT` son obligatorias: sin ellas el servicio no arranca, con un mensaje que nombra la propiedad. Los timeouts de conexión y de lectura son propiedades, con 3 s y 10 s por defecto.
  - El JWT técnico llega solo por el entorno. `CatedraProperties` sobrescribe `toString()` para no mostrarlo, y los mensajes de error no incluyen el cuerpo de la respuesta ni cabeceras.
  - Los errores se traducen por `status`, nunca por el texto del detalle: sin conexión, timeout o `5xx` es `CatedraUnavailableException`; `401` o `403` es `CatedraAuthenticationException`; cualquier otro `4xx`, un cuerpo ilegible o un snapshot con datos obligatorios ausentes es `CatedraResponseException`. Un snapshot incompleto se rechaza entero: no se aplica a medias.
  - El cliente no reintenta: hace un intento y lanza la excepción. Quien lo llame decide qué hacer.
- **Alternativas descartadas:**
  - `WebClient` o OpenFeign: agregan dependencias (WebFlux u otra librería) para una aplicación bloqueante.
  - Reintentos dentro del cliente: el snapshot es una lectura inocua y la verificación periódica de la versión ya repite el intento más tarde; reintentar acá duplicaría ese mecanismo y esconde fallos a quien llama.
  - Configuración opcional con la sincronización desactivada: el servicio no tiene sentido sin la cátedra, y un error claro al arrancar es mejor que uno tardío.
  - Probar contra un servidor HTTP falso o WireMock: se eligió `MockRestServiceServer`, que trae Spring y sustituye la capa HTTP, para probar la traducción de errores y la lectura de la respuesta sin dependencias ni puertos.
- **Consecuencias:** los tests automáticos no ejercitan los timeouts reales ni la conexión de red, porque `MockRestServiceServer` no usa sockets. Se comprobaron una vez, a mano, con un servidor local que acepta la conexión y no responde (el timeout de lectura de 1 s cortó a los 1059 ms) y con un puerto cerrado: ambos dan `CatedraUnavailableException`. Contra la cátedra real el CI no puede probar (está detrás de ZeroTier). Spring ignora por defecto los campos desconocidos de la respuesta, como pide el contrato, y lo cubre un test. El JWT técnico vence al año: habrá que generar otro y reemplazarlo en `.env`.

## 20. Sincronización completa del catálogo por upsert

- **Contexto:** CAT-08 reemplaza la copia local del catálogo con un snapshot de la cátedra. La copia y la versión deben cambiar juntas o no cambiar, y el servicio nunca debe quedar con una versión que no corresponde a sus datos. Nada dispara todavía la sincronización: los disparadores (arranque, chequeo periódico, Kafka) llegan con CAT-11 y CAT-12.
- **Decisión:**
  - **Upsert, no borrar todo.** Dentro de una transacción se leen las tres tablas, se hace `save` de cada elemento del snapshot y se borran los ids que ya no llegan. Como las entities se leyeron antes, Hibernate sabe cuáles existen y emite `UPDATE` solo si algo cambió (un test lo comprueba con las estadísticas de Hibernate). Se escribe de padres a hijos (categorías, profesionales, horarios) y se borra de hijos a padres por las claves foráneas. La cátedra solo hace bajas lógicas (decisión 18), así que el borrado casi nunca encuentra algo: queda como defensa por si la cátedra reinicia sus datos.
  - **Tres casos de uso.** `SynchronizeCatalogUseCaseImpl` orquesta y no tiene transacción: pide el snapshot por HTTP (hasta 10 s) antes de abrirla. `ApplyCatalogSnapshotUseCaseImpl` es `@Transactional`: bloquea la fila de `sync_state`, reemplaza los datos y guarda la versión. `RecordSyncFailureUseCaseImpl` registra el fallo en una transacción propia.
  - **Una sincronización a la vez.** El orquestador envuelve todo, pedido HTTP incluido, en un `ReentrantLock` con espera: si dos disparadores coinciden, la segunda pide su propio snapshot cuando la primera termina y nunca aplica uno viejo encima de uno nuevo. El `SELECT ... FOR UPDATE` sobre `sync_state` queda por si corren dos instancias.
  - **Un snapshot con versión menor se aplica igual**: la cátedra es la fuente de verdad (por ejemplo, si reinicia sus datos). Los casos de uso no registran logs, como pide la arquitectura: `synchronizeFull()` devuelve un `SyncResult` con la versión anterior y la nueva, y quien lo llame avisa del retroceso.
  - **Registro del fallo.** Si la sincronización falla, se guarda `last_status = ERROR` y `last_error` (clase y mensaje de la excepción, cortados a 500 caracteres) sin tocar `local_version`. Si registrar el fallo también falla, se relanza la excepción **original** con la del registro como suprimida.
  - **Persistencia con JPA.** Las entities guardan `categoryId` y `professionalId` como números, sin `@ManyToOne`, para coincidir con el modelo de dominio y evitar cargas perezosas.
- **Alternativas descartadas:**
  - Borrar todo y reinsertar: reescribe todas las filas y deja filas muertas en PostgreSQL aunque cambie una sola, y no se reutiliza en la sincronización incremental.
  - `JdbcClient` con `INSERT ... ON CONFLICT DO UPDATE` en lotes: es más rápido y no carga las tablas en memoria, pero obliga a escribir SQL a mano, ata el código a PostgreSQL y se aparta de la arquitectura. Queda como salida si se mide que JPA es lento.
  - Una sola transacción que incluya el pedido HTTP: retendría una conexión a la base mientras espera a la cátedra.
  - Saltear la sincronización si ya hay una en curso: un evento que anuncia una versión nueva se perdería hasta el próximo chequeo periódico.
  - Registrar el error en la misma transacción: el rollback lo borraría.
- **Consecuencias:** cada tabla se carga en memoria al sincronizar (alcanza para miles de filas) y, para un id nuevo, `merge` hace un `SELECT` antes del `INSERT`. Si la base está caída, el error no puede registrarse y `sync_state` conserva el estado anterior hasta la próxima sincronización. El candado en memoria vale para una instancia; con varias, el bloqueo de la base evita que se pisen pero no ahorra pedidos al snapshot.

## 21. Cliente Redis de la cátedra, de solo lectura

- **Contexto:** para sincronizar por incrementos y para saber si la copia local está al día, el servicio necesita leer lo que la cátedra publica en Redis (`catedra:sync:*`, anexo §14): las versiones, los cambios de cada versión y el estado vigente de las entidades. Un cambio de versión trae solo los **ids** afectados; el estado completo de cada entidad hay que leerlo aparte.
- **Decisión:**
  - El dominio define el puerto `out` `CatedraSyncSource` con cinco lecturas: `readMetadata()`, `readChanges(version)` y la lectura por ids de categorías, profesionales y horarios. Su adapter, `CatedraSyncSourceAdapter`, usa `StringRedisTemplate` de Spring Data Redis (Lettuce) y vive en `infrastructure/redis`.
  - Las versiones se leen del hash `metadata` en **una sola operación**, para que la versión actual y la más antigua disponible sean consistentes entre sí. Las entidades se leen **por ids** (`HMGET`), que es lo que necesita la incremental; el estado completo viene del snapshot REST.
  - `readChanges` devuelve vacío si la clave no existe: esa versión ya salió de la ventana que la cátedra retiene y hay que pedir un snapshot. Un id que se pide y la cátedra no publica es un fallo de contrato, igual que una clave de cambios que trae otra versión.
  - Es **solo lectura**: la cuenta técnica puede escribir únicamente en su namespace `alumnos:*`, que este cliente no usa.
  - La conexión se configura con las propiedades estándar de Spring Boot (`spring.data.redis.*`), alimentadas por `CATEDRA_REDIS_HOST`, `CATEDRA_REDIS_PORT`, `CATEDRA_REDIS_USERNAME` y `CATEDRA_REDIS_PASSWORD`, todas obligatorias. Los timeouts son de 3 s (conexión) y 10 s (comando), configurables. No hay clase de configuración propia.
  - Los errores se traducen igual que en el cliente REST: un fallo de Redis (sin conexión, timeout o credenciales rechazadas) es `CatedraUnavailableException`; un JSON ilegible, un campo obligatorio ausente o un id faltante es `CatedraResponseException`. El cliente no reintenta.
  - Los DTO del snapshot y su conversión al dominio se reutilizan: las entidades tienen la misma forma en REST y en Redis. La conversión se extrajo a `CatedraEntityMapper`, compartido por los dos adapters.
  - El indicador de salud de Redis está **desactivado**: un Redis ajeno caído no debe volver «no saludable» al servicio, que puede seguir sirviendo el catálogo local.
- **Alternativas descartadas:**
  - Leer también las claves `current-version` y `oldest-available-version`: son dos lecturas que podrían ser inconsistentes entre sí; el hash `metadata` trae las dos versiones juntas.
  - Leer los hashes completos: nadie los usa hoy. Se agrega si la sincronización incremental o la verificación periódica lo necesitan.
  - Jedis o Lettuce sin Spring: más código y ningún beneficio.
  - Una clase de configuración propia con las propiedades de Redis dentro de `CatedraProperties`: duplica lo que Spring Boot ya hace con `spring.data.redis.*`.
  - Duplicar los DTO y la conversión en el paquete `redis`: dos copias de la misma lógica que habría que mantener iguales.
  - Distinguir un fallo de credenciales de uno de red: exigiría leer el texto del error de Redis, y la regla del cliente REST es decidir por el tipo, nunca por el mensaje.
- **Consecuencias:** el paquete `redis` depende de `client` (DTO y mapper compartidos). Las lecturas por ids no son atómicas respecto de una publicación nueva de la cátedra; la incremental lo tolera porque solo avanza la versión local cuando aplicó todos los cambios de una versión. Los tests levantan un Redis real en Docker (Testcontainers), incluido uno con un usuario de solo lectura como el de la cátedra. No se probó contra el Redis real de la cátedra (se hará en la prueba manual al final).

## 22. Rutas y claves de la cátedra como configuración

- **Contexto:** el cliente REST (decisión 19) y el cliente Redis (decisión 21) necesitan nombres que define la cátedra: la ruta del snapshot y las cinco claves de lectura de Redis. En CAT-07 la ruta quedó como constante del código. Si la cátedra renombra una de estas cosas, el servicio dejaría de funcionar hasta que alguien modifique, recompile y vuelva a desplegar.
- **Decisión:**
  - La ruta del snapshot y las cinco claves (`metadata`, el prefijo de `changes:`, categorías, profesionales y horarios) son propiedades `catedra.contract.*`, agrupadas en `CatedraContractProperties` (`shared/config`). Los adapters las reciben por el constructor y no tienen constantes con esos nombres.
  - Salen de variables de entorno (`CATEDRA_SNAPSHOT_PATH` y `CATEDRA_REDIS_KEY_*`) que se definen en `.env` y se pasan en el `environment` de `docker-compose.yml`. Son obligatorias y no tienen valor por defecto en `application.properties`: la única fuente de verdad es el `.env`, y `.env.example` ya trae los valores del contrato v1. Si la cátedra cambia uno, se edita el `.env` y se reinicia el servicio.
  - Si falta una o está en blanco, el servicio no arranca, igual que con la URL y el JWT de la cátedra. La ruta del snapshot además debe empezar con `/` (sin ella el pedido saldría mal formado); a las claves de Redis no se les impone ningún formato, para no volver a fijar una convención que es de la cátedra.
  - Los tests comprueban que los adapters siguen la configuración (con nombres distintos a los del contrato v1) y que cada propiedad es obligatoria.
- **Alternativas descartadas:**
  - Constantes en el código: es lo más simple, pero cualquier cambio de la cátedra exige tocar el código y desplegar de nuevo.
  - Valores por defecto en `application.properties` además del `.env`: serían dos fuentes que habría que mantener iguales.
  - Ampliar `CatedraProperties`: mezcla la conexión (URL, JWT, timeouts) con los nombres del contrato y obliga a cambiar los tests que la construyen.
- **Consecuencias:** quien actualice el servicio debe agregar las variables nuevas a su `.env` antes de arrancar. Si la cátedra cambiara la *forma* de un dato y no solo su nombre, igual hace falta tocar el código: esta decisión cubre los nombres, no el formato. El mismo criterio se aplica a los topics de Kafka cuando se implemente el consumidor.

## 23. Consumo de `CatalogUpdated` desde Kafka con deduplicación

- **Contexto:** la cátedra avisa en Kafka cada versión nueva del catálogo (`CatalogUpdated`, anexo §15.3). La entrega es **al menos una vez**: el mismo evento puede llegar repetido, y puede llegar después de un fallo del servicio. El aviso no trae datos: hay que pedirlos. Cuando se implementó este consumidor todavía no existía la sincronización incremental, así que pedía siempre el snapshot completo; la decisión 24 la agrega y reemplaza ese paso.
- **Decisión:**
  - El adapter de entrada es `CatalogEventListener` (`infrastructure/kafka`, un paquete por tecnología como `client` y `redis`). Interpreta el JSON con `CatalogEventMapper`, llama a `CatalogService` y **confirma el offset al final**, con `ack-mode=manual`. No contiene lógica de negocio.
  - `synchronizeForEvent` (en `SynchronizeCatalogUseCaseImpl`) primero descarta el evento si su `eventId` ya está en `processed_events` o si la versión local es **igual** a la del evento: ahorra el pedido HTTP. Una versión local mayor no se descarta (la cátedra pudo retroceder, decisión 24). Si no, pide el snapshot y llama a `ApplyCatalogEventUseCase`.
  - `ApplyCatalogEventUseCaseImpl` es transaccional y escribe **datos, versión y `eventId` en una sola transacción**: reutiliza `ApplyCatalogSnapshotUseCase`, que se une a la misma. Vuelve a comprobar el `eventId` dentro de la transacción y la clave primaria de `processed_events` rechaza el repetido si dos corren a la vez.
  - `synchronizeFull` y `synchronizeForEvent` comparten el candado en memoria y el registro de fallos: un evento y una sincronización completa no se cruzan, y un snapshot viejo nunca se aplica sobre uno nuevo.
  - **Errores:** un fallo se reintenta con espera creciente (por defecto 3 reintentos desde 1 s, configurables). Agotados, el mensaje se descarta con un log de error y se sigue. Un mensaje que no se puede interpretar (JSON inválido, sin `eventId` o con versión inválida) se descarta sin reintentar.
  - **Configuración:** el servidor y el grupo usan las propiedades estándar de Spring (`spring.kafka.*`), alimentadas por `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` y `CATEDRA_KAFKA_GROUP_ID`; el topic es `catedra.contract.kafka-catalog-topic` (`CATEDRA_KAFKA_CATALOG_TOPIC`) dentro de `CatedraContractProperties`, como los demás nombres del contrato (decisión 22). Las tres variables son obligatorias y sin valor por defecto. Los reintentos (`catedra.kafka.*`) salen de `CATEDRA_KAFKA_MAX_RETRIES` y `CATEDRA_KAFKA_INITIAL_BACKOFF`, escritas en el `docker-compose.yml` y sin valor por defecto en el código (decisión 25).
  - Un grupo sin offset guardado empieza por el principio del topic (`earliest`): los eventos viejos se descartan solos, porque la copia ya está al día o el `eventId` ya figura.
  - El broker de la cátedra se alcanza sin autenticación ni cifrado (verificado con un cliente real): alcanza con el servidor.
  - Kafka no entra en `/actuator/health`: un broker ajeno caído no debe volver «no saludable» al servicio, igual que Redis.
- **Alternativas descartadas:**
  - Pasar el `eventId` al caso de uso que aplica el snapshot: mezclaría en uno que hoy no conoce eventos dos responsabilidades.
  - Sin deduplicar, confiando en que reaplicar un snapshot es idempotente: lo es en el resultado, pero repite un pedido HTTP y una escritura por cada duplicado.
  - Confirmar el offset antes de procesar o con confirmación automática: un fallo después de confirmar perdería el evento sin remedio.
  - Una cola de eventos muertos: más piezas (otro topic, otro consumidor) sin un beneficio claro; la verificación de versión recupera lo perdido.
  - Reintentar sin límite o detener el consumo ante un fallo: un único mensaje que siempre falla bloquearía los siguientes.
  - Un candado distinto para el consumidor: un evento y la verificación periódica podrían aplicar snapshots en orden inverso.
- **Consecuencias:** si se agotan los reintentos la notificación se pierde y la copia queda atrasada hasta que se agregue la verificación de versión (arranque y chequeo periódico). Como el consumidor y la sincronización completa comparten un candado de una instancia, con varias instancias solo se evitan las colisiones por el bloqueo de la fila `sync_state`. Los tests usan un Kafka real en Docker (Testcontainers), con el listener desactivado en el resto de los tests; desactivan además la pausa de contextos de Spring (`spring.test.context.cache.pause=never`), que de otro modo reanuda el listener contra un broker ficticio. No se probó contra el broker real de la cátedra, cuyo topic todavía no existe: se hará en la prueba manual al final.

## 24. Sincronización incremental y decisión entre incremental y completa

- **Contexto:** la cátedra publica en Redis qué cambió en cada versión (anexo §14.5): solo los ids afectados, y el estado vigente de cada entidad se lee aparte. Aplicar solo lo que cambió evita descargar el catálogo entero por cada aviso. Hay que decidir cuándo conviene la incremental, cuándo el snapshot, y qué hacer si algo falla a mitad.
- **Decisión:**
  - **Cuándo.** `synchronizeForEvent` compara la versión local con las que publica Redis (`readMetadata`, una sola lectura): igual a la actual, no hace nada; base vacía (versión 0), snapshot; **mayor** que la actual, snapshot (la cátedra retrocedió y es la fuente de verdad, decisión 20); menor que `oldestAvailableVersion`, snapshot (no están todos los cambios necesarios, anexo §14.2 y §18.2); si no, incremental desde `local + 1` hasta la actual. El evento solo es el disparador: la versión que manda es la de Redis, así que un evento viejo que llega tarde no hace nada.
  - **Una transacción por versión.** `ApplyCatalogIncrementUseCaseImpl` bloquea `sync_state`, comprueba que la versión sea exactamente `local + 1`, hace el upsert (`CatalogRepository.upsert`, de padres a hijos y sin borrar nada: la cátedra solo hace bajas lógicas con `enabled = false`) y avanza la versión. Si la cátedra falla a mitad, lo ya aplicado queda.
  - **El `eventId` en la última versión.** `ApplyCatalogEventUseCase` tiene una sobrecarga para incrementos: en la transacción de la última versión se guarda también el `eventId`, así nunca figura como procesado un evento que no se terminó de aplicar. La deduplicación es la misma de la decisión 23.
  - **Caída al snapshot.** Cualquier fallo de la incremental (falta `changes:N`, una entidad ausente en Redis, un dato inválido, la base lo rechaza) o una lectura de Redis que falla (sin conexión, sin metadata) cae al snapshot completo, que ya aplica el evento. El fallo de la incremental no se registra en `sync_state`; solo si también falla el snapshot se registra este y el de la incremental viaja como excepción suprimida.
  - `SyncResult` suma el tipo aplicado (`FULL` o `INCREMENTAL`) y el motivo por el que se usó un snapshot (`snapshotReason`). Los casos de uso no loguean (decisión de CAT-08): el listener escribe el aviso de retroceso y el motivo.
  - **Corrección a la decisión 23.** El filtro previo ignoraba el evento si la versión local era igual **o mayor**; ahora solo si es **igual**, porque un retroceso anunciado por Kafka no se habría aplicado.
  - Comparte el candado en memoria con la sincronización completa: una incremental y un snapshot no se cruzan.
- **Alternativas descartadas:**
  - Ignorar el evento si la versión local es igual o mayor que la de Redis: no aplicaría un retroceso de la cátedra, que es la fuente de verdad.
  - Una sola transacción para todas las versiones: un fallo en la última deshace lo ya traído y obliga a empezar de nuevo.
  - Borrar en la incremental lo que no aparece: la incremental solo ve lo que cambió, no el catálogo entero; las bajas llegan como `enabled = false` y el snapshot limpia lo que ya no exista.
  - No caer al snapshot si Redis no responde: el catálogo quedaría atrasado aunque la API REST sí responda.
  - Un candado propio para la incremental: podría aplicar versiones mientras un snapshot reemplaza la copia.
- **Consecuencias:** si falta la fila única de `sync_state` (la crea la migración V2, con la versión local en 0), se lanza `SyncStateMissingException`: no es una base sin sincronizar, es una base alterada. Una versión con una entidad que Redis ya no tiene, o con una referencia a una categoría que no existe, no se puede aplicar de a una y se resuelve con el snapshot. Mientras Redis esté caído, cada evento descarga el snapshot completo. La verificación al arrancar y periódica (issue siguiente) reutilizará esta misma decisión. Los tests de integración usan un Redis con un usuario de solo lectura, como el de la cátedra, y PostgreSQL reales; no se probó contra el Redis real de la cátedra (se hará en la prueba manual al final).

## 25. Verificación de versión al arrancar y periódica

- **Contexto:** Kafka es solo una notificación y su entrega puede perderse (se agotan los reintentos, el servicio estuvo caído y el grupo ya había avanzado, la cátedra publicó sin que llegara el aviso). Sin otra cosa, la copia quedaría atrasada hasta el próximo evento. Además, un servicio recién instalado, con la base vacía, no tenía forma de llenarse sin que Kafka avisara. El enunciado (§6.2) pide detectar el atraso y reconstruir el estado local.
- **Decisión:**
  - `SynchronizeCatalogUseCase.synchronize()` aplica **la misma decisión** de la entrada 24 (comparar la versión local con la de Redis: nada, incremental o snapshot, con caída al snapshot ante cualquier fallo) pero **sin evento** que la dispare, así que no consulta ni guarda ningún `eventId`. Comparte el candado en memoria con `synchronizeForEvent` y `synchronizeFull`. `CatalogService.synchronize()` la expone.
  - Un único disparador, `CatalogVersionCheckScheduler` (`infrastructure/scheduling`, adaptador de entrada), registra una tarea de **espera fija** con `SchedulingConfigurer`: la primera vuelta ocurre `CATEDRA_SYNC_INITIAL_DELAY` después de arrancar (con la base vacía trae el snapshot) y después una cada `CATEDRA_SYNC_CHECK_INTERVAL`, contadas desde el fin de la anterior para que nunca se solapen.
  - Un fallo de una vuelta (cátedra o Redis caídos) lo captura el propio programador y deja un error con contexto en el log; se reintenta en la siguiente y el servicio sigue sano, igual que con Redis. Si el snapshot falló, el error ya queda en `sync_state` (CAT-08).
  - El log del resultado lo comparten el listener de Kafka y el programador (`SyncOutcomeLogger`): los casos de uso no loguean.
  - **Recuperación del estado.** Si una vuelta falló y la siguiente encuentra la copia al día (la versión local igual a la actual de Redis), `ClearSyncErrorUseCase` pasa `sync_state` a `OK` y borra el error, con la fecha del momento; no cambia la versión ni el tipo del último intento. Solo escribe si había un error que limpiar, así una verificación al día no toca la base cada cinco minutos. Salió de la primera prueba contra la cátedra real: tras un corte de red, la vuelta siguiente se recuperaba pero el estado seguía en `ERROR`, y el endpoint de estado (CAT-15) habría mostrado un error viejo con la copia al día. Se descartó escribir la fecha de cada verificación (una escritura cada cinco minutos sin necesidad) y dejar el estado como estaba (informaría mal).
  - **Parámetros:** son valores de ajuste que no son secretos ni de una máquina, así que se escriben **literales en el `environment:` del `docker-compose.yml`** (`CATEDRA_SYNC_INITIAL_DELAY` 10 s, `CATEDRA_SYNC_CHECK_INTERVAL` 5 m, `CATEDRA_KAFKA_MAX_RETRIES` 3 y `CATEDRA_KAFKA_INITIAL_BACKOFF` 1 s) y no en `.env`; `application.properties` solo los lee de la variable, sin valor por defecto. Se validan al arrancar (`CatedraSyncProperties`: el intervalo mayor que cero y la espera inicial no negativa). El interruptor `catedra.sync.check-enabled` (activo si no se define) lo apagan los tests.
- **Alternativas descartadas:**
  - Valores por defecto en `application.properties`: obligan a recompilar para afinar un tiempo, y quedan dos lugares para el mismo dato.
  - `${VARIABLE:-valor}` en el compose para poder pisarlos desde `.env`: otra vez dos lugares.
  - Un `ApplicationReadyEvent` para el arranque y un `@Scheduled` para lo periódico: dos disparadores para lo mismo; la espera inicial corta ya cubre el arranque.
  - Verificar cada minuto: más lecturas a Redis sin beneficio, porque Kafka ya avisa de casi todo.
  - Tarea de frecuencia fija en lugar de espera fija: si una vuelta tarda (un snapshot grande, una cátedra lenta), la siguiente se acumularía.
- **Consecuencias:** el atraso por una notificación perdida dura como máximo el intervalo (5 minutos) más lo que tarde en aplicarse. Cada vuelta cuesta una lectura a Redis cuando la copia está al día. Fuera de Docker hay que exportar los cuatro parámetros (nota en el README). Con varias instancias cada una verificaría por su cuenta; el bloqueo de la base evita que se pisen. Los tests de integración usan un intervalo de 200 ms y PostgreSQL real, con la cátedra como doble.

## 26. Estado de la sincronización (`GET /api/sync/status`) y errores sin datos sensibles

- **Contexto:** hace falta poder consultar cómo está la copia local del catálogo: en qué versión está y cómo salió el último intento. El criterio del issue pide además que los errores registrados no traigan datos sensibles. El campo `last_error` de `sync_state` guardaba hasta ahora la clase y el mensaje de cualquier excepción, y el mensaje de Hibernate, del driver o de Redis puede incluir la sentencia SQL, nombres de columnas o valores. Mostrarlo en un endpoint lo habría expuesto.
- **Decisión:**
  - **Endpoint.** `GET /api/sync/status` devuelve `localVersion`, `lastSyncAt`, `lastSyncType`, `lastStatus` y `lastError`, con el mismo nombre en inglés que usan las demás respuestas. Una base que nunca se sincronizó devuelve `200` con `localVersion` en `0` y el resto en `null`: es un estado normal. Si falta la fila única de `sync_state` (la base fue alterada) responde `500` con el formato de error estándar y sin detalles: el motivo queda en el log.
  - **Autenticación.** Requiere un JWT válido y cualquier usuario autenticado puede consultarlo, sin rol particular (decisión del usuario). No hizo falta configurar nada: `SecurityConfiguration` ya exige token a todo lo que no sea registro, login, salud y documentación. Los tests comprueban el `401` sin token.
  - **Los errores se limpian al guardarlos, no al mostrarlos.** `RecordSyncFailureUseCaseImpl` guarda `Clase: mensaje` solo para las excepciones de la cátedra (`CatedraException`), cuyos mensajes escribimos nosotros; de cualquier otra guarda solo el nombre de la clase. Así lo sensible nunca llega a la base ni a ningún otro lector futuro de `sync_state`. El texto completo no se pierde: queda en el log, que ya registra la pila entera (el programador y el manejador de errores de Kafka). Se mantiene el recorte a 500 caracteres.
  - **Estructura.** Caso de uso `GetSyncStatusUseCase` (lectura sin bloqueo con `SyncStateRepository.find()`, para no esperar a una sincronización en curso), método en `CatalogService` que traduce la ausencia de fila a `SyncStateMissingException`, y controller, DTO y mapper en `catalogo/infrastructure/web`.
- **Alternativas descartadas:**
  - Limpiar el error solo al mostrarlo y guardar todo: el dato sensible quedaría en la base y dependería de que ningún otro camino la lea.
  - Guardar el mensaje de toda excepción y recortarlo: el recorte no quita el SQL ni los valores.
  - Pedir un rol de administrador: el proyecto no tiene ese concepto y el usuario pidió solo autenticación.
  - Una sola lista blanca de códigos de error en lugar de la clase: más piezas sin un beneficio claro para este tamaño.
- **Consecuencias:** en `sync_state` se pierde el mensaje de errores propios que no son de la cátedra (por ejemplo «Se esperaba la versión 5 y llegó la 6»); solo quedan en el log. Esos errores de la incremental, además, nunca llegan a `sync_state`: la incremental que falla cae al snapshot y el motivo se registra en el log del resultado. Las filas que ya estuvieran guardadas con el formato anterior no se limpian: no hay datos reales todavía.
