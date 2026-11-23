# 005. Analizar la calidad con SonarCloud desde GitHub Actions y JaCoCo

**Estado:** aceptada

## Decisión

El análisis corre en un workflow de GitHub Actions (no con el análisis automático de SonarCloud). JaCoCo mide la cobertura durante `./mvnw verify` y `sonar-maven-plugin` sube el análisis y el reporte.

**Justificación:** El análisis automático no mide cobertura de tests. Con Actions, el mismo `verify` que ejecuta las pruebas alimenta a Sonar.
