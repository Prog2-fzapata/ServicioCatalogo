# 002. Dar a cada servicio su propia base PostgreSQL

**Estado:** aceptada

## Contexto

Catálogo y turnos deben tener datos separados y delimitar su propiedad.

## Decisión

Cada servicio tiene su propia instancia de PostgreSQL 18, con su base y su usuario. No hay claves foráneas entre servicios: las referencias son por valor.

**Justificación:** Cada servicio es dueño exclusivo de sus datos y puede desplegarse en un nodo distinto.

## Alternativas descartadas

- Una sola instancia con dos bases lógicas. Es más simple de operar, pero acopla el despliegue de ambos servicios a una misma máquina y a un mismo punto de falla.
