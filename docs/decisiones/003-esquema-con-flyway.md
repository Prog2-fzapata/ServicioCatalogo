# 003. Crear el esquema solo con Flyway y validarlo con Hibernate

**Estado:** aceptada

## Contexto

Hace falta un esquema reproducible y auditable en todos los entornos.

## Decisión

El esquema lo crean solo las migraciones de Flyway (`src/main/resources/db/migration`) y Hibernate corre con `ddl-auto=validate`.

**Justificación:** El esquema queda versionado en git, se aplica igual en todos los entornos y `validate` detecta al arrancar cualquier diferencia entre las entidades y las tablas.

## Alternativas descartadas

- Dejar que Hibernate genere el esquema (`update`/`create`). No es reproducible ni auditable, y puede modificar tablas sin dejar rastro.
