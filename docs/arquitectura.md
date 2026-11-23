# Arquitectura

Este documento muestra el sistema completo (la aplicación Android, los dos servicios backend, sus bases de datos y las integraciones con la cátedra) y, después, cómo está organizado por dentro el servicio de catálogo. El diagrama general es el mismo en el repositorio de turnos.

## El sistema

El servicio de catálogo hace dos cosas: sincroniza el catálogo con la cátedra y atiende a los usuarios (registro, login y emisión del JWT).

```mermaid
flowchart LR
    subgraph Cliente
        KMP["App Android (KMP)"]
    end

    subgraph Alumno["Backends propios (Docker Compose)"]
        CAT["Servicio de catálogo y sincronización<br/>(incluye usuarios y autenticación)"]
        TUR["Servicio de turnos<br/>y reservas"]
        DBC[("BD catálogo<br/>PostgreSQL")]
        DBT[("BD turnos<br/>PostgreSQL")]
    end

    subgraph Catedra["Servicio de la cátedra (externo)"]
        API["API REST"]
        RED[("Redis")]
        KAF{{"Kafka"}}
    end

    KMP -- "REST: registro, login y catálogo<br/>(emite el JWT de usuario)" --> CAT
    KMP -- "REST + JWT de usuario" --> TUR
    TUR -- "REST + JWT de usuario<br/>(agenda de un profesional, nombre del paciente)" --> CAT

    CAT --- DBC
    TUR --- DBT

    CAT -- "REST: snapshot (JWT técnico)" --> API
    CAT -- "lee catedra:sync:*" --> RED
    KAF -- "catedra.catalog.{groupId}: CatalogUpdated" --> CAT

    TUR -- "REST: ocupaciones, holds, confirmación,<br/>listado y cancelación (JWT técnico)" --> API
    KAF -- "alumnos.turnos.acciones.{groupId}: eventos de reserva" --> TUR
    TUR -- "catedra.turnos.telefono.{groupId}:<br/>AdditionalInformationSubmitted" --> KAF
```

### Reglas que respeta

- La aplicación Android nunca habla con la cátedra ni conoce el JWT técnico: todo pasa por los dos servicios.
- Cada servicio es dueño exclusivo de su base, con su propia instancia de PostgreSQL y sus propias migraciones ([decisión 002](decisiones/002-base-postgresql-propia-por-servicio.md)). No hay claves foráneas ni accesos cruzados entre las dos bases; ver [modelo-de-datos.md](modelo-de-datos.md).
- Turnos no replica el catálogo: le pide a catálogo la agenda de un profesional cuando la necesita ([contrato.md](contrato.md#lo-que-turnos-consume-de-catálogo)).
- Catálogo es el único que consume `CatalogUpdated` y el único que lee las claves `catedra:sync:*` de Redis.
- Turnos es el único que consume los eventos de reserva y el que publica el teléfono del paciente.
- Los dos servicios usan la misma cuenta técnica de la cátedra, cargada por la variable de entorno `CATEDRA_JWT`.

### Dos identidades, dos JWT

```mermaid
flowchart TB
    U["Usuario final"] -- "registro y login" --> CAT["Servicio de catálogo"]
    CAT -- "emite el JWT de usuario" --> U
    U -- "JWT de usuario" --> TUR["Servicio de turnos"]
    CAT -- "JWT técnico<br/>(nunca sale del backend)" --> API["API de la cátedra"]
    TUR -- "JWT técnico<br/>(nunca sale del backend)" --> API
```

El JWT de usuario lo emite catálogo y lo valida turnos con el mismo secreto ([seguridad.md](seguridad.md)). El JWT técnico identifica a los dos servicios ante la cátedra y no viaja nunca a la aplicación.

## El servicio de catálogo por dentro

El servicio sigue la arquitectura hexagonal (puertos y adaptadores). Tiene dos slices, cada uno con sus capas `domain`, `application` e `infrastructure`:

- `catalogo`: la copia local del catálogo, la sincronización con la cátedra, la búsqueda y el estado de la sincronización.
- `usuario`: el registro, el login y la cuenta, y la emisión del JWT.

Además, `shared` agrupa lo que no pertenece a un slice: la configuración, la seguridad y el manejo de errores web.

```mermaid
flowchart LR
    subgraph Entrada["Adaptadores de entrada"]
        WEB["Controllers REST"]
        KL["Listener de Kafka"]
        SCH["Verificación periódica"]
    end

    subgraph App["Aplicación"]
        SVC["CatalogService y UserService"]
        UC["Casos de uso"]
    end

    subgraph Dom["Dominio"]
        MOD["Modelos y puertos"]
    end

    subgraph Salida["Adaptadores de salida"]
        JPA["Repositorios JPA"]
        REST["Cliente REST de la cátedra"]
        RDS["Lectura de Redis"]
    end

    WEB --> SVC
    KL --> SVC
    SCH --> SVC
    SVC --> UC
    UC --> MOD
    JPA -. implementa .-> MOD
    REST -. implementa .-> MOD
    RDS -. implementa .-> MOD
```

- **Dominio** (`domain`): los modelos y los puertos de entrada (un caso de uso por interfaz) y de salida. No importa Spring.
- **Aplicación** (`application`): los casos de uso y un servicio por slice (`CatalogService`, `UserService`) que los expone a los adaptadores de entrada.
- **Adaptadores de entrada** (`infrastructure`): los controllers (`web`), `CatalogEventListener` (`kafka`) y `CatalogVersionCheckScheduler` (`scheduling`). Todos llaman a un servicio de la capa de aplicación (`CatalogService` o `UserService`) y ninguno contiene lógica de negocio.
- **Adaptadores de salida** (`infrastructure`): la persistencia JPA (`persistence`), `CatedraApiClientAdapter` (`client`) y `CatedraSyncSourceAdapter` (`redis`).

Las decisiones de cada pieza están en [decisiones/](decisiones/README.md): la autenticación como caso de uso propio ([014](decisiones/014-autenticacion-como-caso-de-uso.md)), el cliente REST ([019](decisiones/019-cliente-rest-de-la-catedra.md)), el cliente Redis ([021](decisiones/021-cliente-redis-de-solo-lectura.md)), el consumidor de Kafka ([023](decisiones/023-consumo-de-catalogupdated.md)) y la verificación de versión ([025](decisiones/025-verificacion-de-version.md)).

## Cómo se ejecuta

Cada repositorio trae su `docker-compose.yml` con su propia base y se levanta por separado ([decisión 004](decisiones/004-docker-compose-por-repositorio.md)). Los pasos están en el [README](../README.md).
