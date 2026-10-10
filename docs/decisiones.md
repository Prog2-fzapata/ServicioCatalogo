# Decisiones de diseño: servicio de catálogo

Registro de las decisiones técnicas de este servicio: contexto, decisión, alternativas descartadas y justificación. Cada entrada se agrega cuando la decisión queda implementada en el código.

## 1. Stack: Maven, Java 25 y Spring Boot 4.1.1

- **Contexto:** el enunciado no fija herramientas de construcción ni versiones.
- **Decisión:** Maven con wrapper (`./mvnw`), Java 25 (Temurin) y Spring Boot 4.1.1.
- **Justificación:** Spring Boot no publica una etiqueta LTS; 4.1.1 es la última versión estable al momento de empezar (la 4.2 solo existía como milestone). Java 25 es la versión de soporte largo vigente. El wrapper permite compilar sin instalar Maven.

## 2. Una base PostgreSQL propia por servicio

- **Contexto:** catálogo y turnos deben tener datos separados y delimitar su propiedad.
- **Decisión:** cada servicio tiene su propia instancia de PostgreSQL 18, con su base y su usuario. No hay claves foráneas entre servicios: las referencias son por valor.
- **Alternativa descartada:** una sola instancia con dos bases lógicas. Es más simple de operar, pero acopla el despliegue de ambos servicios a una misma máquina y a un mismo punto de falla.
- **Justificación:** cada servicio es dueño exclusivo de sus datos y puede desplegarse en un nodo distinto.

## 3. Esquema con Flyway y validación de Hibernate

- **Decisión:** el esquema lo crean solo las migraciones de Flyway (`src/main/resources/db/migration`) y Hibernate corre con `ddl-auto=validate`.
- **Alternativa descartada:** dejar que Hibernate genere el esquema (`update`/`create`). No es reproducible ni auditable, y puede modificar tablas sin dejar rastro.
- **Justificación:** el esquema queda versionado en git, se aplica igual en todos los entornos y `validate` detecta al arrancar cualquier diferencia entre las entidades y las tablas.

## 4. Docker: un compose por repositorio e imagen multi-stage

- **Decisión:** cada repositorio trae su `docker-compose.yml` (con su propia base) y un `Dockerfile` multi-stage: una etapa con JDK compila con Maven y la imagen final usa solo el JRE, corre con un usuario sin privilegios y contiene únicamente el JAR. Los puertos se publican solo en `127.0.0.1`.
- **Alternativa descartada:** un único compose compartido entre los repositorios. Obliga a clonar los demás repositorios para levantar uno solo.
- **Justificación:** cada servicio es autocontenido; con clonar un repositorio alcanza para ejecutarlo, y la imagen final es más chica y expone menos superficie.

## 5. Calidad: SonarCloud por GitHub Actions con cobertura de JaCoCo

- **Decisión:** el análisis corre en un workflow de GitHub Actions (no con el análisis automático de SonarCloud). JaCoCo mide la cobertura durante `./mvnw verify` y `sonar-maven-plugin` sube el análisis y el reporte.
- **Justificación:** el análisis automático no mide cobertura de tests. Con Actions, el mismo `verify` que ejecuta las pruebas alimenta a Sonar.

## 6. Pruebas de integración con Testcontainers

- **Contexto:** los tests de persistencia necesitan una base PostgreSQL real, porque dependen de restricciones únicas, claves foráneas y tipos propios del motor. El enunciado no exige una herramienta concreta.
- **Decisión:** cada ejecución de pruebas levanta un PostgreSQL 18 en un contenedor descartable (Testcontainers), conectado a la aplicación con `@ServiceConnection`. Es la misma imagen que usa el compose.
- **Alternativas descartadas:**
  - Usar la base del compose: los tests compartirían datos con el desarrollo manual (pueden chocar con registros cargados a mano) y consumirían ids de esa base.
  - Una base en memoria (H2): su SQL difiere de PostgreSQL, así que los tests podrían pasar con un comportamiento que en producción falla.
- **Consecuencias:** los tests solo requieren Docker en ejecución, no usan el `.env` y el workflow de CI no necesita un servicio de base de datos. Cada contexto de Spring distinto levanta su propio contenedor, lo que suma algunos segundos a la ejecución.

## 7. Modelo de usuarios compatible con JHipster

- **Contexto:** el enunciado exige que el modelo y el contrato de usuarios sean compatibles con el usuario de JHipster.
- **Decisión:** se usan solo los campos de JHipster (`login`, contraseña, `firstName`, `lastName`, `email`, `imageUrl`, `langKey`), sin agregar campos propios. Las tablas se llaman `users`, `authorities` y `user_authorities`; `login` y `email` son únicos con restricciones con nombre. `firstName` y `lastName` son opcionales, como en JHipster. Las identidades se relacionan por el `id` numérico, que no cambia nunca, por eso el email puede modificarse.
- **Justificación:** imitar el comportamiento observable de JHipster, como pidió el profesor. Las restricciones con nombre permiten traducir las violaciones de unicidad a errores de negocio precisos.
