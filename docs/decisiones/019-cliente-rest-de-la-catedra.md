# 019. Pedir el snapshot a la cátedra con un cliente REST sin reintentos

**Estado:** aceptada

## Contexto

El servicio necesita pedir a la cátedra el catálogo completo (`GET /api/synchronization/snapshot`, con el JWT técnico) para la sincronización completa. Es la primera comunicación saliente del servicio.

## Decisión

- El dominio define el puerto `out` `CatedraApiClient` (`fetchSnapshot()`), que devuelve un `CatalogSnapshot` del dominio. Su adapter, `CatedraApiClientAdapter`, usa `RestClient` de Spring sobre el cliente HTTP de Java (HTTP/1.1), sin dependencias nuevas. Los DTO de la respuesta y su mapper al dominio viven con el adapter.
- La configuración de conexión (`CatedraProperties` y el bean `RestClient`) está en `shared/config`, fuera de los slices, porque la usarán varios (REST hoy; Redis y Kafka después). El adapter vive en `infrastructure/client`.
- `CATEDRA_BASE_URL` y `CATEDRA_JWT` son obligatorias: sin ellas el servicio no arranca, con un mensaje que nombra la propiedad. Los timeouts de conexión y de lectura son propiedades, con 3 s y 10 s por defecto.
- El JWT técnico llega solo por el entorno. `CatedraProperties` sobrescribe `toString()` para no mostrarlo, y los mensajes de error no incluyen el cuerpo de la respuesta ni cabeceras.
- Los errores se traducen por `status`, nunca por el texto del detalle: sin conexión, timeout o `5xx` es `CatedraUnavailableException`; `401` o `403` es `CatedraAuthenticationException`; cualquier otro `4xx`, un cuerpo ilegible o un snapshot con datos obligatorios ausentes es `CatedraResponseException`. Un snapshot incompleto se rechaza entero: no se aplica a medias.
- El cliente no reintenta: hace un intento y lanza la excepción. Quien lo llame decide qué hacer.

## Alternativas descartadas

- `WebClient` o OpenFeign: agregan dependencias (WebFlux u otra librería) para una aplicación bloqueante.
- Reintentos dentro del cliente: el snapshot es una lectura inocua y la verificación periódica de la versión ya repite el intento más tarde; reintentar acá duplicaría ese mecanismo y esconde fallos a quien llama.
- Configuración opcional con la sincronización desactivada: el servicio no tiene sentido sin la cátedra, y un error claro al arrancar es mejor que uno tardío.
- Probar contra un servidor HTTP falso o WireMock: se eligió `MockRestServiceServer`, que trae Spring y sustituye la capa HTTP, para probar la traducción de errores y la lectura de la respuesta sin dependencias ni puertos.

## Consecuencias

Los tests automáticos no ejercitan los timeouts reales ni la conexión de red, porque `MockRestServiceServer` no usa sockets. Se comprobaron una vez, a mano, con un servidor local que acepta la conexión y no responde (el timeout de lectura de 1 s cortó a los 1059 ms) y con un puerto cerrado: ambos dan `CatedraUnavailableException`. Contra la cátedra real el CI no puede probar (está detrás de ZeroTier). Spring ignora por defecto los campos desconocidos de la respuesta, como pide el contrato, y lo cubre un test. El JWT técnico vence al año: habrá que generar otro y reemplazarlo en `.env`.
