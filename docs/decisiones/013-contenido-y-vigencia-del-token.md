# 013. Definir el contenido y la vigencia del token

**Estado:** aceptada

## Contexto

El token debe identificar al usuario y sus autoridades, con el formato y los valores de JHipster.

## Decisión

El token lleva `sub` (login), `uid` (id numérico), `auth` (autoridades separadas por espacio), `iat` y `exp`. Dura 24 horas, o 30 días si el login pide `rememberMe`; ambos valores son propiedades (`jwt.token-validity`, `jwt.remember-me-validity`). Son los valores y el formato de JHipster.

## Alternativas descartadas

- Incluir nombre y apellido como claims. Un claim queda fijo al emitir el token: si el usuario carga su nombre después, el token viejo seguiría sin él. Turnos lo consulta en `GET /api/account` al reservar.

## Consecuencias

Un token no se puede revocar antes de que venza (no hay lista de tokens). Es una limitación conocida del esquema sin estado; la vigencia de 24 horas la acota.
