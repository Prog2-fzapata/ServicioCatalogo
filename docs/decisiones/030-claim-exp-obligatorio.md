# 030. Exigir el claim exp en el JWT

**Estado:** aceptada

## Contexto

El validador por defecto de Spring Security (`JwtValidators.createDefault()`, que es un `JwtTimestampValidator` y es el que usa `NimbusJwtDecoder...build()`) acepta un token bien firmado que no trae `exp`, y ese token no vence nunca. Se comprobó con un test: un token firmado con la clave correcta y sin `exp` obtenía `200`. Hoy no era explotable (catálogo siempre escribe `exp` al emitir y fabricar un token exige conocer `JWT_SECRET`), pero un token que no vence no debería poder ser válido en ningún caso.

## Decisión

El decodificador usa un `JwtTimestampValidator` con `setAllowEmptyExpiryClaim(false)` en lugar del validador por defecto. Un token sin `exp` recibe el mismo `401` `application/problem+json` que uno vencido, sin revelar el motivo. `createDefault()` no agregaba otra validación, así que no se pierde ninguna. Es el mismo cambio que se hizo en turnos, que valida los tokens que emite este servicio.

## Alternativas descartadas

- Agregar un validador de claims obligatorios como el de turnos: catálogo no lo tiene y habría que crearlo para un solo claim; corregir el validador de vigencia es un cambio de una línea y el mismo en los dos servicios.
- Dejarlo y documentarlo: el riesgo es bajo, pero el arreglo cuesta una línea y un test.

## Consecuencias

Un token firmado con la clave compartida y sin `exp` ya no sirve en ninguno de los dos servicios. El emisor (`JwtTokenIssuer`) no cambia: siempre escribió `exp`.
