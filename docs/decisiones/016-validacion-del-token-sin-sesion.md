# 016. Validar el token en una cadena de filtros sin sesión ni CSRF

**Estado:** aceptada

## Contexto

`GET` y `POST /api/account` necesitan saber quién llama, así que la validación del JWT no puede esperar al issue de seguridad.

## Decisión

La cadena de filtros (`shared.security.SecurityConfiguration`) valida el token, no usa sesión ni CSRF (cada pedido trae su token) y protege todo salvo el registro, el login, `/actuator/health` y la documentación. El usuario de cada pedido sale siempre del claim `uid`, nunca del cuerpo ni de la URL.

**CSRF desactivado y SonarCloud:** CSRF protege a las aplicaciones que identifican al usuario con una cookie de sesión: otro sitio puede hacer que el navegador mande esa cookie sin que el usuario lo note. Esta API no usa sesión ni cookies; cada pedido trae su JWT en la cabecera `Authorization`, que un sitio ajeno no puede agregar. Por eso se desactiva (es lo que recomienda Spring Security para APIs con token), pero la regla `java:S4502` de Sonar lo marca como vulnerabilidad crítica sin mirar el resto de la configuración. Se dejó el `NOSONAR` en esa línea, con su motivo, en lugar de aceptar el aviso en silencio o activar CSRF sin sentido.

Los `401` y `403` del filtro y el esquema «Bearer JWT» de OpenAPI se resolvieron en la [decisión 017](017-respuestas-401-y-403.md).

## Alternativas descartadas

- Activar CSRF e ignorarlo para `/api/**`, que no protege nada (todas las rutas quedarían excluidas) y agrega configuración que no se usa.
