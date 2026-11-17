# 001. Usar Maven, Java 25 y Spring Boot 4.1.1

**Estado:** aceptada

## Contexto

El enunciado exige Java y Spring Boot para los dos backends, pero no fija la herramienta de construcción ni las versiones.

## Decisión

Maven con wrapper (`./mvnw`), Java 25 (Temurin) y Spring Boot 4.1.1.

**Justificación:** Spring Boot no publica una etiqueta LTS; 4.1.1 es la última versión estable al momento de empezar (la 4.2 solo existía como milestone). Java 25 es la versión de soporte largo vigente. El wrapper permite compilar sin instalar Maven.
