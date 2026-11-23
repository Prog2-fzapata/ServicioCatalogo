# 004. Empaquetar con un compose por repositorio y una imagen multi-stage

**Estado:** aceptada

## Contexto

Cada repositorio debe poder levantarse por separado, sin clonar los demás.

## Decisión

Cada repositorio trae su `docker-compose.yml` (con su propia base) y un `Dockerfile` multi-stage: una etapa con JDK compila con Maven y la imagen final usa solo el JRE, corre con un usuario sin privilegios y contiene únicamente el JAR. Los puertos se publican solo en `127.0.0.1`.

**Justificación:** Cada servicio es autocontenido; con clonar un repositorio alcanza para ejecutarlo, y la imagen final es más chica y expone menos superficie.

## Alternativas descartadas

- Un único compose compartido entre los repositorios. Obliga a clonar los demás repositorios para levantar uno solo.
