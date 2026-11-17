# 017. Responder los 401 y 403 en problem+json sin revelar el motivo

**Estado:** aceptada

## Contexto

Los filtros de seguridad cortan el pedido antes de llegar a los controllers, así que los `@RestControllerAdvice` no los ven: el `401` salía sin cuerpo y con una cabecera `WWW-Authenticate` que incluye el motivo exacto del rechazo (por ejemplo `Jwt expired at ...` o `Invalid signature`). Además, Swagger UI no podía probar los endpoints protegidos.

## Decisión

- `ProblemDetailsSecurityHandler` (un solo componente que hace de `AuthenticationEntryPoint` y de `AccessDeniedHandler`) escribe el `401` y el `403` en `application/problem+json`, con el mismo formato que el resto de los errores (`status`, `title`, `detail`, `path`, `code`). Los textos viven en `ProblemDetailsFactory`.
- `code`: `UNAUTHORIZED` y `FORBIDDEN`. No se distingue token vencido, mal firmado o ausente, ni en el cuerpo ni en la cabecera (`WWW-Authenticate: Bearer`, sin `error_description`): el cliente solo necesita saber que debe volver a iniciar sesión.
- Seguridad por método (`@EnableMethodSecurity`) para poder pedir roles con `@PreAuthorize` en los casos de uso futuros. Como esa excepción se lanza dentro del controller, `GlobalExceptionHandler` la traduce a `403 FORBIDDEN`; sin ese handler el genérico devolvía `500`.
- `OpenApiConfiguration` declara el esquema Bearer JWT: Swagger UI muestra el botón «Authorize», se pega el `id_token` una vez y los pedidos de la pantalla lo mandan. El registro y el login quedan marcados sin requisito de seguridad.

**CORS:** Queda fuera por ahora. CORS lo aplican los navegadores y el único cliente es la app Android, así que no hay ningún origen que permitir. Si se suma un cliente web, se agrega una variable de entorno con la lista de orígenes (vacía por defecto).

## Alternativas descartadas

- Delegar en un `HandlerExceptionResolver` para que los `@RestControllerAdvice` armen el `401`: reutiliza el handler global, pero agrega una capa de indirección que no hace falta para dos respuestas fijas.
- Dejar la cabecera `WWW-Authenticate` por defecto de Spring (RFC 6750): informa el motivo del rechazo, que no necesita un cliente y facilita probar tokens.
- Un `code` distinto por motivo (`TOKEN_EXPIRED`, `INVALID_TOKEN`): el contrato dice «sin garantía» para estos códigos y la app actúa igual en todos los casos.

## Consecuencias

**Pruebas:** `SecurityRejectionTest` ejercita el `401` (sin token, mal formado, vencido, otra clave) y el `403` con un controller de prueba que pide `ROLE_ADMIN`, porque ningún endpoint real exige un rol todavía. Se comprobó que fallan si se quita el handler del `403` (da `500`) o el entry point del filtro (aparece el motivo en la cabecera).
