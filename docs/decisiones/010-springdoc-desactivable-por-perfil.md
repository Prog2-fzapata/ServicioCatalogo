# 010. Documentar la API con SpringDoc y apagarla en el perfil prod

**Estado:** aceptada

## Contexto

La documentación de endpoints y DTO se genera a partir del código con SpringDoc (OpenAPI y Swagger UI), por lo que siempre coincide con la solución. Dejarla expuesta en un despliegue real revela la superficie de la API.

## Decisión

La documentación está activa por defecto, porque forma parte de lo que se entrega y quien evalúe el proyecto necesita verla al levantarlo con `docker compose up`. El entorno se declara en el `.env` con `SPRING_PROFILES_ACTIVE` (`dev` por defecto, valor que fija `.env.example`); el perfil `prod` (`application-prod.properties`) desactiva Swagger UI y `/v3/api-docs`.

## Alternativas descartadas

- Desactivarla siempre y no dejar ninguna forma de consultarla en ejecución, lo que le quita utilidad como documentación entregable.

## Consecuencias

Dos tests lo verifican: sin perfil (o con `dev`) el documento OpenAPI responde `200`, con el perfil `prod` ambos endpoints responden `404`.
