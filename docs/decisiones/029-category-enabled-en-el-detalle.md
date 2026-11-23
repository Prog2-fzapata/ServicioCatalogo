# 029. Informar si la categoría del profesional está habilitada

**Estado:** aceptada

## Contexto

Turnos no ofrece horarios de un profesional cuya categoría está deshabilitada (cada profesional tiene una sola categoría). El detalle devolvía `categoryId` y `categoryName`, pero su `enabled` es solo el del profesional, así que turnos no podía saber si la categoría estaba apagada.

## Decisión

- **El detalle agrega `categoryEnabled`** (booleano). El `enabled` del profesional no cambia de significado. La lista de profesionales **no** lo expone: solo lo necesita el detalle, que es lo que consulta turnos.
- **Se calcula en los casos de uso desde la copia local**, sin llamar a la cátedra: `ProfessionalSummary` lleva el dato junto al nombre de la categoría, y tanto el detalle como la búsqueda lo completan con la misma categoría que ya cargaban para el nombre, así que no suma consultas.
- **Si la categoría faltara** (la clave foránea lo impide), se informa como deshabilitada: ante la duda, turnos no ofrece horarios.

## Alternativas descartadas

- Que turnos pida `GET /api/professional-categories` aparte: no hay endpoint por id, sería una llamada más por cada consulta y otro motivo de `503`.
- Que `enabled` del profesional pase a ser «profesional y categoría habilitados»: se pierde saber cuál de los dos se apagó y cambia el significado de un campo que ya está en `main`.
- Exponerlo también en la lista: aumenta el contrato sin que nadie lo use todavía.

## Consecuencias

Cambio compatible hacia atrás (un campo nuevo; turnos ignora lo desconocido, y los clientes que no lo conocen lo ignoran). Turnos debe actualizarse junto con este cambio: lee `categoryEnabled` y, si falta, lo trata como un desajuste entre servicios.
