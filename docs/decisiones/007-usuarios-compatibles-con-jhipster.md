# 007. Modelar los usuarios de forma compatible con JHipster

**Estado:** aceptada

## Contexto

El enunciado exige que el modelo y el contrato de usuarios sean compatibles con el usuario de JHipster.

## Decisión

Se usan solo los campos de JHipster (`login`, contraseña, `firstName`, `lastName`, `email`, `imageUrl`, `langKey`), sin agregar campos propios. Las tablas se llaman `users`, `authorities` y `user_authorities`; `login` y `email` son únicos con restricciones con nombre. `firstName` y `lastName` son opcionales, como en JHipster. Las identidades se relacionan por el `id` numérico, que no cambia nunca, por eso el email puede modificarse.

**Justificación:** Imitar el comportamiento observable de JHipster. Las restricciones con nombre permiten traducir las violaciones de unicidad a errores de negocio precisos.
