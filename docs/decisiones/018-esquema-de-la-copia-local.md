# 018. Guardar la copia local del catálogo en cinco tablas

**Estado:** aceptada

## Contexto

El servicio guarda una copia del catálogo de la cátedra (categorías, profesionales y horarios) y el estado de la sincronización. Las clases Java de esas tablas llegan con los casos de uso que las usan (sincronización y búsqueda); esta migración fija el esquema antes.

## Decisión

- Tablas `professional_categories`, `professionals`, `weekly_schedules`, `sync_state` y `processed_events`. Los nombres van en plural, como pide la convención de la cátedra para las entities y como `users`; `sync_state` queda en singular porque es una sola fila.
- Los ids de categoría, profesional y horario son los de la cátedra y no se generan, para poder aplicar cambios por id. Las bajas son lógicas (`enabled = false`): no se borran filas.
- Claves foráneas normales (profesional a categoría, horario a profesional) con un índice en cada una, porque PostgreSQL no los crea solo.
- `day_of_week` es `VARCHAR` con `CHECK` de los siete valores. Los horarios exigen `end_time > start_time` y `slot_duration_minutes > 0`.
- `sync_state` tiene una sola fila (`CHECK (id = 1)`) creada por la propia migración, con `local_version = 0` (significa «base vacía, hace falta un snapshot») y sin datos de última sincronización. `last_sync_type` admite `FULL` o `INCREMENTAL` y `last_status`, `OK` o `ERROR`.
- `processed_events` usa el `eventId` como clave primaria: un evento repetido se rechaza.

## Alternativas descartadas

- Tipo `enum` de PostgreSQL para el día: es incómodo de modificar y no aporta nada frente a `VARCHAR` con `CHECK` con `@Enumerated(STRING)`.
- Sin claves foráneas, o diferibles (se comprueban al final de la transacción): la integridad queda a cargo del código. Con claves normales alcanza con aplicar siempre categorías, profesionales y horarios en ese orden. Si la sincronización lo complicara, pasar a diferibles es un cambio chico.
- Crear la fila de `sync_state` en la primera sincronización: cada lector tendría que manejar también el caso «no hay fila».
- Índices de búsqueda por nombre y categoría: dependen de las consultas reales, que se miden al implementar la búsqueda.

## Consecuencias

La cátedra no documenta los largos de nombres y descripciones, así que se usaron límites amplios (255 y `TEXT`); si un dato real no entra, se amplía con otra migración. Los `VARCHAR` de `day_of_week`, `last_sync_type` y `last_status` miden lo justo para sus valores: agregar un valor nuevo exige ampliar la columna. El esquema se prueba directamente en SQL, con `CatalogSchemaTest` sobre un PostgreSQL real: se comprobó que cada restricción hace fallar al test que la cubre al quitarla de la migración.
