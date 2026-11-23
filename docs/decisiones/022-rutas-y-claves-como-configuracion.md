# 022. Configurar por entorno las rutas y claves de la cátedra

**Estado:** aceptada

## Contexto

El cliente REST ([decisión 019](019-cliente-rest-de-la-catedra.md)) y el cliente Redis ([decisión 021](021-cliente-redis-de-solo-lectura.md)) necesitan nombres que define la cátedra: la ruta del snapshot y las cinco claves de lectura de Redis. En CAT-07 la ruta quedó como constante del código. Si la cátedra renombra una de estas cosas, el servicio dejaría de funcionar hasta que alguien modifique, recompile y vuelva a desplegar.

## Decisión

- La ruta del snapshot y las cinco claves (`metadata`, el prefijo de `changes:`, categorías, profesionales y horarios) son propiedades `catedra.contract.*`, agrupadas en `CatedraContractProperties` (`shared/config`). Los adapters las reciben por el constructor y no tienen constantes con esos nombres.
- Salen de variables de entorno (`CATEDRA_SNAPSHOT_PATH` y `CATEDRA_REDIS_KEY_*`) que se definen en `.env` y se pasan en el `environment` de `docker-compose.yml`. Son obligatorias y no tienen valor por defecto en `application.properties`: la única fuente de verdad es el `.env`, y `.env.example` ya trae los valores del contrato v1. Si la cátedra cambia uno, se edita el `.env` y se reinicia el servicio.
- Si falta una o está en blanco, el servicio no arranca, igual que con la URL y el JWT de la cátedra. La ruta del snapshot además debe empezar con `/` (sin ella el pedido saldría mal formado); a las claves de Redis no se les impone ningún formato, para no volver a fijar una convención que es de la cátedra.
- Los tests comprueban que los adapters siguen la configuración (con nombres distintos a los del contrato v1) y que cada propiedad es obligatoria.

## Alternativas descartadas

- Constantes en el código: es lo más simple, pero cualquier cambio de la cátedra exige tocar el código y desplegar de nuevo.
- Valores por defecto en `application.properties` además del `.env`: serían dos fuentes que habría que mantener iguales.
- Ampliar `CatedraProperties`: mezcla la conexión (URL, JWT, timeouts) con los nombres del contrato y obliga a cambiar los tests que la construyen.

## Consecuencias

Quien actualice el servicio debe agregar las variables nuevas a su `.env` antes de arrancar. Si la cátedra cambiara la *forma* de un dato y no solo su nombre, igual hace falta tocar el código: esta decisión cubre los nombres, no el formato. El mismo criterio se aplica al topic de Kafka ([decisión 023](023-consumo-de-catalogupdated.md)).
