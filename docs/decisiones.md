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

## 8. Hash de contraseñas: SHA-256 previo a BCrypt

- **Contexto:** el enunciado exige almacenar las contraseñas con un mecanismo criptográfico adecuado y respetar las validaciones definidas por la cátedra, cuyo registro admite contraseñas de 4 a 100 caracteres. BCrypt solo procesa hasta 72 bytes: Spring Security 7.1.1 rechaza con una excepción lo que supere ese tamaño, y versiones anteriores lo truncaban en silencio. Los caracteres como `ñ` o las tildes pesan 2 bytes en UTF-8, por lo que ni siquiera 72 caracteres están garantizados.
- **Decisión:** `BCryptPasswordHasher` calcula el SHA-256 de la contraseña, lo codifica en Base64 (siempre 44 caracteres) y aplica BCrypt a ese resultado. `hash` y `matches` pasan por el mismo paso. El hash BCrypt se guarda en `users.password_hash`.
- **Alternativas descartadas:**
  - Limitar la contraseña a 72 bytes en el registro: es más simple, pero se aparta del rango de 4 a 100 caracteres y podría rechazar una clave que se espera válida.
  - Argon2: no tiene este límite, pero exige una librería adicional (BouncyCastle) y se aparta del algoritmo por defecto de JHipster.
  - Dejar que BCrypt truncara: ignoraría los caracteres posteriores al byte 72 sin avisar.
- **Consecuencias:** cualquier contraseña de hasta 100 caracteres entra completa y todos sus caracteres cuentan (hay un test que lo comprueba). Los hashes guardados solo se verifican con este mismo hasher, no con BCrypt a secas.

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
- **Limitación conocida:** los `401` y `403` que genera el filtro todavía salen sin cuerpo `problem+json`; se completan junto con CORS en el issue de seguridad siguiente.
- **Limitación conocida:** el documento OpenAPI no declara el esquema «Bearer JWT», por lo que Swagger UI no ofrece el botón «Authorize» y no puede probar los endpoints protegidos. Está pendiente de resolver.
