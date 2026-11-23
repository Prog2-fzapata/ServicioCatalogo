# 014. Implementar la autenticación como caso de uso propio

**Estado:** aceptada

## Contexto

La autenticación debe resolverse sin que el dominio conozca el framework de seguridad.

## Decisión

`AuthenticateUserUseCase` busca al usuario por login y por email (sin distinguir mayúsculas), exige que esté activo, compara la contraseña con el puerto `PasswordHasher` y pide el token al puerto `TokenIssuer`, que implementa `JwtTokenIssuer` en infraestructura. No usa el `AuthenticationManager` de Spring Security.

## Alternativas descartadas

- El flujo estándar de Spring (`UserDetailsService` + `AuthenticationManager`): resuelve lo mismo, pero obliga a que las clases de Spring Security aparezcan en el dominio o a duplicar al usuario; con el caso de uso, el dominio sigue sin conocer el framework.

## Consecuencias

Usuario inexistente, contraseña incorrecta y cuenta inactiva dan el mismo `401 INVALID_CREDENTIALS`, para no revelar cuál falló.
