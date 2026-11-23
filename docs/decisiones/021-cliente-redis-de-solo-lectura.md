# 021. Leer los cambios de la cátedra desde Redis, solo lectura

**Estado:** aceptada

## Contexto

Para sincronizar por incrementos y para saber si la copia local está al día, el servicio necesita leer lo que la cátedra publica en Redis (`catedra:sync:*`, anexo §14): las versiones, los cambios de cada versión y el estado vigente de las entidades. Un cambio de versión trae solo los **ids** afectados; el estado completo de cada entidad hay que leerlo aparte.

## Decisión

- El dominio define el puerto `out` `CatedraSyncSource` con cinco lecturas: `readMetadata()`, `readChanges(version)` y la lectura por ids de categorías, profesionales y horarios. Su adapter, `CatedraSyncSourceAdapter`, usa `StringRedisTemplate` de Spring Data Redis (Lettuce) y vive en `infrastructure/redis`.
- Las versiones se leen del hash `metadata` en **una sola operación**, para que la versión actual y la más antigua disponible sean consistentes entre sí. Las entidades se leen **por ids** (`HMGET`), que es lo que necesita la incremental; el estado completo viene del snapshot REST.
- `readChanges` devuelve vacío si la clave no existe: esa versión ya salió de la ventana que la cátedra retiene y hay que pedir un snapshot. Un id que se pide y la cátedra no publica es un fallo de contrato, igual que una clave de cambios que trae otra versión.
- Es **solo lectura**: la cuenta técnica puede escribir únicamente en su namespace `alumnos:*`, que este cliente no usa.
- La conexión se configura con las propiedades estándar de Spring Boot (`spring.data.redis.*`), alimentadas por `CATEDRA_REDIS_HOST`, `CATEDRA_REDIS_PORT`, `CATEDRA_REDIS_USERNAME` y `CATEDRA_REDIS_PASSWORD`, todas obligatorias. Los timeouts son de 3 s (conexión) y 10 s (comando), configurables. No hay clase de configuración propia.
- Los errores se traducen igual que en el cliente REST: un fallo de Redis (sin conexión, timeout o credenciales rechazadas) es `CatedraUnavailableException`; un JSON ilegible, un campo obligatorio ausente o un id faltante es `CatedraResponseException`. El cliente no reintenta.
- Los DTO del snapshot y su conversión al dominio se reutilizan: las entidades tienen la misma forma en REST y en Redis. La conversión se extrajo a `CatedraEntityMapper`, compartido por los dos adapters.
- El indicador de salud de Redis está **desactivado**: un Redis ajeno caído no debe volver «no saludable» al servicio, que puede seguir sirviendo el catálogo local.

## Alternativas descartadas

- Leer también las claves `current-version` y `oldest-available-version`: son dos lecturas que podrían ser inconsistentes entre sí; el hash `metadata` trae las dos versiones juntas.
- Leer los hashes completos: nadie los usa hoy. Se agrega si la sincronización incremental o la verificación periódica lo necesitan.
- Jedis o Lettuce sin Spring: más código y ningún beneficio.
- Una clase de configuración propia con las propiedades de Redis dentro de `CatedraProperties`: duplica lo que Spring Boot ya hace con `spring.data.redis.*`.
- Duplicar los DTO y la conversión en el paquete `redis`: dos copias de la misma lógica que habría que mantener iguales.
- Distinguir un fallo de credenciales de uno de red: exigiría leer el texto del error de Redis, y la regla del cliente REST es decidir por el tipo, nunca por el mensaje.

## Consecuencias

El paquete `redis` depende de `client` (DTO y mapper compartidos). Las lecturas por ids no son atómicas respecto de una publicación nueva de la cátedra; la incremental lo tolera porque solo avanza la versión local cuando aplicó todos los cambios de una versión. Los tests levantan un Redis real en Docker (Testcontainers), incluido uno con un usuario de solo lectura como el de la cátedra. El CI no puede probar contra el Redis real de la cátedra (está detrás de ZeroTier).
