# 028. Devolver el detalle de un profesional con todos sus horarios y paginar las categorías

**Estado:** aceptada

## Contexto

El servicio de turnos necesita la agenda vigente de un profesional para generar los horarios libres, y la app KMP necesita las categorías para filtrar la búsqueda. Hay que decidir qué horarios trae el detalle, qué pasa con lo deshabilitado y si las categorías se paginan.

## Decisión

- **El detalle trae todos los horarios**, con el `id` y el `enabled` de cada uno, ordenados por día de la semana y hora de inicio. Quien consume (turnos) decide qué hacer con un horario deshabilitado: el catálogo informa lo que tiene, no interpreta.
- **Lo deshabilitado se devuelve igual.** Un profesional o una categoría con `enabled=false` no es un error ni un `404`; el `404 PROFESSIONAL_NOT_FOUND` es solo para un id que no existe. Un id que no es un número es `400`, lo resuelve Spring con el formato de error estándar.
- **Las categorías se paginan** como los profesionales: `page`, `size` y `sort`, arreglo con `X-Total-Count` y `Link`, tamaño 20 y el mismo tope `CATALOGO_PAGE_MAX_SIZE`. Hoy son pocas (8 en la cátedra), pero el catálogo puede crecer y el contrato no debería cambiar cuando crezca. Filtro opcional `enabled`; orden por `id` o `name` (otro campo, `400 VALIDATION_ERROR`), por defecto `name`, y el `id` desempata.
- **Capas.** `PageQuery` y `PagedResult` se reutilizan; las categorías tienen su propio `CategoryFilter` y `CategorySortOrder`, con sus campos. Casos de uso `GetProfessionalUseCase` (devuelve `Optional`; `CatalogService` lo traduce a `ProfessionalNotFoundException`) y `SearchProfessionalCategoriesUseCase`. Se reutiliza `ProfessionalSummary`, pero el DTO del detalle es propio porque lleva `id` y `enabled` de cada horario. El orden de los horarios (`WeeklySchedule.BY_DAY_AND_START_TIME`) se comparte con la búsqueda.
- **Autenticación.** JWT válido, como todo lo demás; turnos reenvía el del usuario.

## Alternativas descartadas

- Categorías sin paginar: obligaría a un cambio incompatible cuando el catálogo crezca.
- Un orden genérico compartido por profesionales y categorías: acopla dos conjuntos de campos que no tienen nada en común y complica el tipo.
- Detalle con solo los horarios habilitados: turnos no podría distinguir un horario deshabilitado de uno que nunca existió, ni mostrar por qué un turno ya reservado quedó fuera de la agenda.
- `404` para un profesional deshabilitado: ocultaría un dato que turnos necesita para decidir qué hacer con las reservas existentes.

## Consecuencias

El detalle hace tres consultas (el profesional, su categoría y sus horarios) y la lista de categorías, dos (la página y el total). Los tests de integración usan PostgreSQL real y comprueban los casos deshabilitados, el `404` y el desempate del orden.
