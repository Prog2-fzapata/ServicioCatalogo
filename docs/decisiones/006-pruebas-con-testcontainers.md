# 006. Probar la integración con Testcontainers

**Estado:** aceptada

## Contexto

Los tests de persistencia necesitan una base PostgreSQL real, porque dependen de restricciones únicas, claves foráneas y tipos propios del motor. El enunciado no exige una herramienta concreta.

## Decisión

Cada ejecución de pruebas levanta un PostgreSQL 18 en un contenedor descartable (Testcontainers), conectado a la aplicación con `@ServiceConnection`. Es la misma imagen que usa el compose.

## Alternativas descartadas

- Usar la base del compose: los tests compartirían datos con el desarrollo manual (pueden chocar con registros cargados a mano) y consumirían ids de esa base.
- Una base en memoria (H2): su SQL difiere de PostgreSQL, así que los tests podrían pasar con un comportamiento que en producción falla.

## Consecuencias

Los tests solo requieren Docker en ejecución, no usan el `.env` y el workflow de CI no necesita un servicio de base de datos. Cada contexto de Spring distinto levanta su propio contenedor, lo que suma algunos segundos a la ejecución.
