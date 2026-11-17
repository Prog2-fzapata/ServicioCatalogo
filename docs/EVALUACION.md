<!-- evaluacion:inicio -->
# Evaluación — Prog2-fzapata/ServicioCatalogo

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-17.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **78/100** |
| Tipo de repositorio | `backend` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 11,816 |
| Archivos de código | 241 |
| Clases | 253 |
| Código dentro de clases (%POO) | 63% |
| Funciones fuera de clases | 1 |
| Duplicación (puerta) | 2.0% |
| Duplicación lógica | 2.2% |
| Duplicación de boilerplate | 15.8% |
| Cobertura | 99% |
| Tope duro aplicado a D1 | `POO_BAJO` → D1 ≤ 60 |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 7/11 |
| Destacar (`exigente`) | **no cumple** — 5/11 |

**Para aprobar, falta cumplir:** SOLID

**Para destacar, falta cumplir:** SOLID; POO; Duplicación

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Franco Zapata | 38 | 4,800 | 9,682 | 609 | 18 | 100% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 4 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | 100 |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | N/A |
| **D4** Integración y contrato (10%) | 10% | 90 |
| **D5** Seguridad y robustez (10%) | 10% | 100 |
| **D6** Pruebas y cobertura (20%) | 20% | 100 |
| **D7** Proceso y entrega (15%) | 15% | 100 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **4/100**
- **Para investigar la causa:**
    - ¿Se pensó el modelo de clases antes de codificar o se agrupó después?
    - ¿Qué clases concentran muchas responsabilidades a la vez?
    - ¿Los colaboradores llegan como parámetros o se crean por dentro?

### D2. Arquitectura hexagonal — backend (15%)

- **Qué mide:** Separación en capas (dominio, puertos de entrada/salida, adaptadores) y dirección de las dependencias del backend.
- **Por qué importa:** Aislar el dominio de la infraestructura permite cambiar la base de datos, el framework o el transporte sin tocar las reglas de negocio.
- **Tu resultado:** **100/100**

### D4. Integración y contrato (10%)

- **Qué mide:** Cumplimiento del contrato de integración de la cátedra (formato de errores, paginación, autenticación, endpoints esperados).
- **Por qué importa:** Los servicios deben poder integrarse entre sí y con la app sin sorpresas; el contrato es lo que hacen cumplir las pruebas de integración.
- **Tu resultado:** **90/100**

### D5. Seguridad y robustez (10%)

- **Qué mide:** Credenciales embebidas, consultas SQL por concatenación, y presencia de config de entorno; aislamiento de secretos.
- **Por qué importa:** Un secreto en el repositorio o una consulta construida por concatenación son riesgos concretos, no hipotéticos.
- **Tu resultado:** **100/100**

### D6. Pruebas y cobertura (20%)

- **Qué mide:** Cantidad y resultado de pruebas y cobertura de línea (JaCoCo/Kover, Surefire/Failsafe) cuando hay build.
- **Por qué importa:** Las pruebas son la red de seguridad para refactorizar e integrar; la cobertura acota las zonas sin verificar.
- **Tu resultado:** **100/100**

### D7. Proceso y entrega (15%)

- **Qué mide:** Artefactos verificables de proceso/entrega: `compose`, `.env.example`, CI (`.github/workflows/`), `CODEOWNERS` y wrapper de build (`mvnw`/`gradlew`). **No** puntúa documentación.
- **Por qué importa:** Un proyecto que compila, se levanta y se valida solo es más fácil de revisar y de integrar en equipo.
- **Tu resultado:** **100/100**

## 5. Señales detectadas

El análisis encontró las siguientes señales (archivo, sin número de línea a propósito):

**DIP/concretos** (52)
- clase 'JwtTokenIssuer' muy acoplada (CBO=17) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/security/JwtTokenIssuer.java`
- clase 'AuthenticationController' muy acoplada (CBO=15) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AuthenticationController.java`
- clase 'AccountController' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AccountController.java`
- clase 'UserExceptionHandler' muy acoplada (CBO=18) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/handler/UserExceptionHandler.java`
- clase 'RegisterUserUseCaseImpl' muy acoplada (CBO=12) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/application/usecases/RegisterUserUseCaseImpl.java`
- clase 'AuthenticateUserUseCaseImpl' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/application/usecases/AuthenticateUserUseCaseImpl.java`
- clase 'UpdateAccountUseCaseImpl' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/application/usecases/UpdateAccountUseCaseImpl.java`
- clase 'Argon2Properties' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/Argon2Properties.java`
- clase 'JwtConfiguration' muy acoplada (CBO=17) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/JwtConfiguration.java`
- clase 'JwtConfiguration' instancia 4 concretos (CBO=17) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/JwtConfiguration.java`
- clase 'OpenApiConfiguration' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/OpenApiConfiguration.java`
- clase 'OpenApiConfiguration' instancia 3 concretos (CBO=14) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/OpenApiConfiguration.java`
- clase 'SecurityConfiguration' muy acoplada (CBO=21) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/SecurityConfiguration.java`
- clase 'ProblemDetailsSecurityHandler' muy acoplada (CBO=19) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/security/ProblemDetailsSecurityHandler.java`
- clase 'CatedraClientConfiguration' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/config/CatedraClientConfiguration.java`
- clase 'CatedraContractProperties' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/config/CatedraContractProperties.java`
- clase 'PaginationHeaders' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/PaginationHeaders.java`
- clase 'ProblemDetailsFactory' muy acoplada (CBO=22) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/ProblemDetailsFactory.java`
- clase 'ProblemDetailsFactory' instancia 7 concretos (CBO=22) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/ProblemDetailsFactory.java`
- clase 'GlobalExceptionHandler' muy acoplada (CBO=20) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/GlobalExceptionHandler.java`
- clase 'RedisSyncMapper' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/redis/RedisSyncMapper.java`
- clase 'CatedraSyncSourceAdapter' muy acoplada (CBO=29) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/redis/CatedraSyncSourceAdapter.java`
- clase 'CatalogEventListener' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/kafka/CatalogEventListener.java`
- clase 'CatalogEventMapper' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/kafka/CatalogEventMapper.java`
- clase 'CatalogEventMapper' instancia 5 concretos (CBO=8) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/kafka/CatalogEventMapper.java`
- clase 'CatalogKafkaErrorHandlerConfiguration' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/kafka/CatalogKafkaErrorHandlerConfiguration.java`
- clase 'CatalogVersionCheckScheduler' muy acoplada (CBO=12) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/scheduling/CatalogVersionCheckScheduler.java`
- clase 'ProfessionalSpecifications' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/specification/ProfessionalSpecifications.java`
- clase 'JpaCatalogRepositoryAdapter' muy acoplada (CBO=19) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/adapter/JpaCatalogRepositoryAdapter.java`
- clase 'JpaProfessionalRepositoryAdapter' muy acoplada (CBO=25) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/adapter/JpaProfessionalRepositoryAdapter.java`
- clase 'JpaProfessionalCategoryRepositoryAdapter' muy acoplada (CBO=23) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/adapter/JpaProfessionalCategoryRepositoryAdapter.java`
- clase 'CatedraApiClientAdapter' muy acoplada (CBO=19) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/client/CatedraApiClientAdapter.java`
- clase 'CatedraApiClientAdapter' instancia 5 concretos (CBO=19) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/client/CatedraApiClientAdapter.java`
- clase 'CatedraEntityMapper' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/client/CatedraEntityMapper.java`
- clase 'ProfessionalCategoryController' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalCategoryController.java`
- clase 'ProfessionalController' muy acoplada (CBO=15) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalController.java`
- clase 'CatalogExceptionHandler' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/handler/CatalogExceptionHandler.java`
- clase 'ProfessionalQueryMapper' muy acoplada (CBO=18) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/mapper/ProfessionalQueryMapper.java`
- clase 'ProfessionalQueryMapper' instancia 5 concretos (CBO=18) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/mapper/ProfessionalQueryMapper.java`
- clase 'CategoryQueryMapper' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/mapper/CategoryQueryMapper.java`
- clase 'CategoryQueryMapper' instancia 4 concretos (CBO=16) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/mapper/CategoryQueryMapper.java`
- clase 'CatalogService' muy acoplada (CBO=18) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/service/CatalogService.java`
- clase 'RecordSyncFailureUseCaseImpl' muy acoplada (CBO=18) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/RecordSyncFailureUseCaseImpl.java`
- clase 'ClearSyncErrorUseCaseImpl' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/ClearSyncErrorUseCaseImpl.java`
- clase 'GetProfessionalUseCaseImpl' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/GetProfessionalUseCaseImpl.java`
- clase 'ApplyCatalogIncrementUseCaseImpl' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/ApplyCatalogIncrementUseCaseImpl.java`
- clase 'SearchProfessionalsUseCaseImpl' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SearchProfessionalsUseCaseImpl.java`
- clase 'ApplyCatalogSnapshotUseCaseImpl' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/ApplyCatalogSnapshotUseCaseImpl.java`
- clase 'ApplyCatalogEventUseCaseImpl' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/ApplyCatalogEventUseCaseImpl.java`
- clase 'SynchronizeCatalogUseCaseImpl' muy acoplada (CBO=37) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- clase 'SynchronizeCatalogUseCaseImpl' instancia 3 concretos (CBO=37) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- clase 'ProfessionalCategoryRepository' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/ports/out/ProfessionalCategoryRepository.java`

**SRP/responsabilidad** (30)
- clase 'UserEntity' grande (LOC=23, métodos=0, campos=12) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/entity/UserEntity.java`
- clase 'UserRegistrationRequest' grande (LOC=24, métodos=0, campos=8) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/dto/UserRegistrationRequest.java`
- clase 'AccountResponse' grande (LOC=10, métodos=0, campos=8) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/dto/AccountResponse.java`
- clase 'UserService' con baja cohesión (20%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/application/service/UserService.java`
- clase 'User' grande (LOC=15, métodos=0, campos=12) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/domain/model/User.java`
- clase 'User' sin métodos (12 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/usuario/domain/model/User.java`
- método 'ProblemDetailsFactory.standardFor' con complejidad 7 — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/ProblemDetailsFactory.java`
- clase 'CatedraSyncSourceAdapter' grande (LOC=69, métodos=10, campos=5) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/redis/CatedraSyncSourceAdapter.java`
- clase 'CatedraSyncSourceAdapter' con baja cohesión (29%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/redis/CatedraSyncSourceAdapter.java`
- método 'CatedraSyncSourceAdapter.readByIds' con 5 parámetros — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/redis/CatedraSyncSourceAdapter.java`
- clase 'CatalogMapper' grande (LOC=76, métodos=11, campos=0) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/mapper/CatalogMapper.java`
- clase 'JpaCatalogRepositoryAdapter' con baja cohesión (0%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/adapter/JpaCatalogRepositoryAdapter.java`
- clase 'WeeklyScheduleEntity' grande (LOC=13, métodos=0, campos=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/persistence/entity/WeeklyScheduleEntity.java`
- clase 'CatedraApiClientAdapter' con baja cohesión (20%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/client/CatedraApiClientAdapter.java`
- clase 'ProfessionalCategoryController' sin métodos (3 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalCategoryController.java`
- clase 'CatalogService' con baja cohesión (17%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/service/CatalogService.java`
- clase 'GetProfessionalUseCaseImpl' con baja cohesión (33%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/GetProfessionalUseCaseImpl.java`
- clase 'SynchronizeCatalogUseCaseImpl' grande (LOC=143, métodos=15, campos=9) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- clase 'SynchronizeCatalogUseCaseImpl' con baja cohesión (8%) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- método 'SynchronizeCatalogUseCaseImpl.isPresent' de 35 líneas — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- método 'SynchronizeCatalogUseCaseImpl.isPresent' con complejidad 7 — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- método 'SynchronizeCatalogUseCaseImpl.synchronizeFor' con complejidad 7 — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/SynchronizeCatalogUseCaseImpl.java`
- clase 'SyncState' sin métodos (5 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/SyncState.java`
- clase 'CatalogSnapshot' sin métodos (5 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/CatalogSnapshot.java`
- clase 'WeeklySchedule' grande (LOC=15, métodos=0, campos=10) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/WeeklySchedule.java`
- clase 'WeeklySchedule' sin métodos (10 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/WeeklySchedule.java`
- clase 'ProcessedEvent' sin métodos (3 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/ProcessedEvent.java`
- clase 'CatalogIncrement' sin métodos (4 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/CatalogIncrement.java`
- clase 'ProfessionalCategory' sin métodos (6 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/ProfessionalCategory.java`
- clase 'Professional' sin métodos (7 campos): solo datos — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/domain/model/Professional.java`

**OCP/type-switch** (2)
- clase 'GlobalExceptionHandler' con 2 type-checks — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/shared/web/GlobalExceptionHandler.java`
- clase 'RecordSyncFailureUseCaseImpl' con 1 type-checks — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/usecases/RecordSyncFailureUseCaseImpl.java`

**?** (2)
- clase 'CatedraUnavailableException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/exception/CatedraUnavailableException.java`
- clase 'CatedraResponseException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/exception/CatedraResponseException.java`

**POO** (1)
- función 'CatedraAuthenticationException' fuera de una clase — `src/main/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/application/exception/CatedraAuthenticationException.java`

**duplicación** (6)
- duplicación lógica: 2.0% · boilerplate: 15.8%
- bloque de 8 líneas repetido 2 veces en src/test/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalCategoryControllerTest.java:108, src/test/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalControllerTest.java:136 — `src/test/java/ar/edu/um/prog2/fzapata/catalogo/catalogo/infrastructure/web/controller/ProfessionalCategoryControllerTest.java`
- bloque de 8 líneas repetido 2 veces en src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/adapter/JpaUserRepositoryAdapterTest.java:66, src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AccountManagementControllerTest.java:71 — `src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/adapter/JpaUserRepositoryAdapterTest.java`
- bloque de 8 líneas repetido 2 veces en src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java:22, src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AccountManagementControllerTest.java:68 — `src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java`
- bloque de 8 líneas repetido 2 veces en src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java:23, src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AccountManagementControllerTest.java:69 — `src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java`
- bloque de 8 líneas repetido 2 veces en src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java:24, src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/web/controller/AccountManagementControllerTest.java:70 — `src/test/java/ar/edu/um/prog2/fzapata/catalogo/usuario/infrastructure/persistence/mapper/UserMapperTest.java`

## 6. Evolución por commit

Métricas calculadas sobre el estado de `main` en **cada commit** (4 commits con trabajo), del primero al último. La calidad usa el perfil `exigente`; el aporte de código lo mide RepoSense. El índice puede **bajar** al crecer el proyecto: se acumulan señales.

```mermaid
xychart-beta
    title "Índice de calidad a lo largo de los commits"
    x-axis ["d85388a", "89cf6ad", "e4697cc", "4676bc8"]
    y-axis "Índice" 0 --> 100
    line [0, 15, 15, 66]
```

| # | Fecha | Commit | Autor | LOC | Índice | %POO | Duplic. | Sueltas |
|---|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2026-09-07 | `d85388a` | Franco Zapata | 0 | 0 | 0% | 0.0% | 0 |
| 2 | 2026-09-10 | `89cf6ad` | Ing. Daniel Quinteros | 0 | 15 | 0% | 0.0% | 0 |
| 3 | 2026-10-13 | `e4697cc` | Franco Zapata | 0 | 15 | 0% | 0.0% | 0 |
| 4 | 2026-11-02 | `4676bc8` | Franco Zapata | 11,816 | 66 | 63% | 2.0% | 1 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
