# 009. Traducir los errores a problem+json con un handler por slice y uno compartido

**Estado:** aceptada

## Contexto

Todos los errores de la API deben tener el mismo formato (`application/problem+json` con `status`, `detail`, `path` y un `code` estable, más `fieldErrors` en las validaciones) y las excepciones de negocio no deben contener lógica HTTP.

## Decisión

Las excepciones de negocio son *unchecked* y se traducen en dos niveles con `@RestControllerAdvice`:

- Cada slice tiene su handler para sus excepciones (`UserExceptionHandler` convierte `LoginAlreadyUsedException` y `EmailAlreadyUsedException` en `400` con `LOGIN_ALREADY_USED` y `EMAIL_ALREADY_USED`). Se consulta primero (`@Order(HIGHEST_PRECEDENCE)`).
- El paquete `shared.web` tiene el handler de lo que no pertenece a ningún slice: validación de datos (`VALIDATION_ERROR` con `fieldErrors`), JSON mal formado y errores inesperados (`500` con un detalle genérico, sin exponer mensajes internos), más un helper que arma el cuerpo `problem+json`. No contiene lógica de negocio ni importa de ningún slice.
- Dos registros simultáneos con el mismo login o email pasan la verificación previa del caso de uso; la restricción única de la base frena al segundo, y el handler del slice traduce esa violación, reconocida por el nombre de la restricción (`uk_users_login`, `uk_users_email`), al mismo código que el chequeo previo.

## Alternativas descartadas

- `try/catch` en cada método del controller (como en la plantilla de la cátedra): repite código y deja fuera los errores que Spring lanza antes de llegar al controller.
- Un único handler global con las excepciones de todos los slices: acopla los slices entre sí.
- Un handler de validación en cada slice: Spring elegiría uno según el orden de los beans, lo que es difícil de predecir.

## Consecuencias

Un slice nuevo agrega su handler sin tocar los demás. El orden entre handlers es explícito y está protegido por tests (sin `@Order` fallan cuatro). El paquete `shared` queda restringido a infraestructura web y no tiene capas propias.
