# Decisiones de diseño: servicio de catálogo

Registro de las decisiones técnicas de este servicio, una por archivo (formato ADR): contexto, decisión, alternativas descartadas y consecuencias. Una sección que no aporta información se omite. Cada decisión se agrega cuando queda implementada en el código; si una se reemplaza no se borra: pasa a `reemplazada por NNN` y la nueva la referencia.

Estados posibles: `aceptada`, `reemplazada por NNN` y `obsoleta`.

| N.º | Decisión | Estado |
|---|---|---|
| 001 | [Usar Maven, Java 25 y Spring Boot 4.1.1](001-stack-maven-java-spring-boot.md) | aceptada |
| 002 | [Dar a cada servicio su propia base PostgreSQL](002-base-postgresql-propia-por-servicio.md) | aceptada |
| 003 | [Crear el esquema solo con Flyway y validarlo con Hibernate](003-esquema-con-flyway.md) | aceptada |
| 004 | [Empaquetar con un compose por repositorio y una imagen multi-stage](004-docker-compose-por-repositorio.md) | aceptada |
| 005 | [Analizar la calidad con SonarCloud desde GitHub Actions y JaCoCo](005-sonarcloud-con-github-actions.md) | aceptada |
| 006 | [Probar la integración con Testcontainers](006-pruebas-con-testcontainers.md) | aceptada |
| 007 | [Modelar los usuarios de forma compatible con JHipster](007-usuarios-compatibles-con-jhipster.md) | aceptada |
| 008 | [Guardar las contraseñas con Argon2id](008-hash-de-contrasenas-argon2id.md) | aceptada |
| 009 | [Traducir los errores a problem+json con un handler por slice y uno compartido](009-errores-problem-json.md) | aceptada |
| 010 | [Documentar la API con SpringDoc y apagarla en el perfil prod](010-springdoc-desactivable-por-perfil.md) | aceptada |
| 011 | [Dar a los errores estándar de Spring el mismo formato y el texto en español](011-errores-de-spring-en-espanol.md) | aceptada |
| 012 | [Firmar el JWT con HS512 y un secreto compartido](012-jwt-hs512-secreto-compartido.md) | aceptada |
| 013 | [Definir el contenido y la vigencia del token](013-contenido-y-vigencia-del-token.md) | aceptada |
| 014 | [Implementar la autenticación como caso de uso propio](014-autenticacion-como-caso-de-uso.md) | aceptada |
| 015 | [Impedir que el login de un usuario sea el email de otro](015-login-y-email-que-se-confunden.md) | aceptada |
| 016 | [Validar el token en una cadena de filtros sin sesión ni CSRF](016-validacion-del-token-sin-sesion.md) | aceptada |
| 017 | [Responder los 401 y 403 en problem+json sin revelar el motivo](017-respuestas-401-y-403.md) | aceptada |
| 018 | [Guardar la copia local del catálogo en cinco tablas](018-esquema-de-la-copia-local.md) | aceptada |
| 019 | [Pedir el snapshot a la cátedra con un cliente REST sin reintentos](019-cliente-rest-de-la-catedra.md) | aceptada |
| 020 | [Aplicar la sincronización completa por upsert](020-sincronizacion-completa-por-upsert.md) | aceptada |
| 021 | [Leer los cambios de la cátedra desde Redis, solo lectura](021-cliente-redis-de-solo-lectura.md) | aceptada |
| 022 | [Configurar por entorno las rutas y claves de la cátedra](022-rutas-y-claves-como-configuracion.md) | aceptada |
| 023 | [Consumir CatalogUpdated desde Kafka y deduplicar por eventId](023-consumo-de-catalogupdated.md) | aceptada |
| 024 | [Elegir entre sincronización incremental y completa según la versión](024-sincronizacion-incremental.md) | aceptada |
| 025 | [Verificar la versión al arrancar y cada cierto tiempo](025-verificacion-de-version.md) | aceptada |
| 026 | [Exponer el estado de la sincronización sin datos sensibles](026-estado-de-la-sincronizacion.md) | aceptada |
| 027 | [Buscar profesionales sobre la copia local con filtros y paginación](027-busqueda-de-profesionales.md) | aceptada |
| 028 | [Devolver el detalle de un profesional con todos sus horarios y paginar las categorías](028-detalle-de-profesional-y-categorias.md) | aceptada |
| 029 | [Informar si la categoría del profesional está habilitada](029-category-enabled-en-el-detalle.md) | aceptada |
| 030 | [Exigir el claim exp en el JWT](030-claim-exp-obligatorio.md) | aceptada |
| 031 | [Escribir los logs en JSON, con campos y sin secretos](031-logs-en-json-sin-secretos.md) | aceptada |
| 032 | [Actualizar la cuenta solo con los campos que llegan](032-actualizacion-parcial-de-la-cuenta.md) | aceptada |
