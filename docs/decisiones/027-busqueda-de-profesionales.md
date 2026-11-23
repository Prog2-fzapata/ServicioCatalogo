# 027. Buscar profesionales sobre la copia local con filtros y paginación

**Estado:** aceptada

## Contexto

El enunciado pide buscar y filtrar profesionales usando exclusivamente la copia local; la lista alimenta la app KMP. Hay que decidir qué filtros, qué significa «disponibilidad», cómo se pagina y se ordena, y cómo se evita una consulta por profesional.

## Decisión

- **Filtros** opcionales y combinables con AND: `categoryId`, `name`, `enabled` y `available`. Sin ninguno devuelve todos, también los deshabilitados (quien consume decide qué hacer con ellos).
- **«Disponibilidad» es local**: tiene al menos un horario semanal habilitado (`available=true`) o ninguno (`false`), con una subconsulta sobre `weekly_schedules`. La disponibilidad real, con turnos libres, exigiría las ocupaciones de la cátedra, que el enunciado prohíbe consultar en una búsqueda: es del servicio de turnos.
- **Nombre sin distinguir mayúsculas ni tildes.** Se busca sobre «nombre apellido» juntos, así que `ana gom` encuentra a «Ana Gómez». Para ignorar tildes se usa la extensión `unaccent` de PostgreSQL, que instala la migración `V4` (viene con PostgreSQL y es «confiable»: la puede crear el dueño de la base). Los comodines `%`, `_` y `\` del texto se escapan para que se busquen como texto, y el texto viaja como parámetro de la consulta (`like lower(unaccent(?)) escape '\'`) y nunca se escribe dentro de la sentencia SQL: `NameSearchBindingTest` registra cada sentencia que Hibernate manda a la base y comprueba, sobre PostgreSQL real, que el texto buscado no aparece en ninguna.
- **Paginación como JHipster:** `page`, `size` y `sort`; la respuesta es un arreglo y el total y los enlaces van en las cabeceras `X-Total-Count` y `Link` (`first`, `prev`, `next`, `last`), que conservan los filtros y el orden. El tamaño por defecto es 20 y el máximo sale de `CATALOGO_PAGE_MAX_SIZE` (100 por defecto: es un parámetro de ajuste, con valor por defecto y el entorno gana, como los de la [decisión 025](025-verificacion-de-version.md)); un `size` mayor se reduce al tope, no es un error. Se escribió una clase propia para las cabeceras (`PaginationHeaders`) que no codifica de nuevo la consulta, que ya llega codificada.
- **Orden:** solo por `id`, `firstName` o `lastName` (los nombres de la respuesta, no los de las columnas); un campo no permitido responde `400 VALIDATION_ERROR` con el campo y los permitidos. Por defecto, apellido y nombre. El `id` desempata siempre, para que dos páginas seguidas no repitan ni salteen filas.
- **Validación:** un `name` de más de 100 caracteres y un parámetro con el tipo equivocado responden `400`.
- **Capas.** El dominio no usa `Pageable` ni `Sort` de Spring: define `ProfessionalFilter`, `PageQuery`, `PagedResult` y `ProfessionalSortOrder`, y un `ProfessionalSummary` (profesional, nombre de su categoría y horarios habilitados). El caso de uso `SearchProfessionalsUseCase` usa tres puertos de salida y hace **tres consultas por página** sin importar cuántos profesionales traiga: la página (con una `Specification`), las categorías y los horarios de esa página. El controller traduce `Pageable` a los tipos del dominio.

## Alternativas descartadas

- Disponibilidad real consultando ocupaciones: viola «solo datos locales» y duplica lo que hace turnos.
- Una consulta por profesional para sus horarios (N+1): una página de 20 harían 22 consultas.
- Una sola consulta con `JOIN` y `GROUP BY`: mezcla tres tablas en un resultado plano difícil de paginar bien (la paginación cuenta filas, no profesionales).
- Tipos `Pageable` y `Page` de Spring en el caso de uso: el dominio no importa Spring.
- Normalizar tildes en Java y guardar una columna de búsqueda: más código, una migración de datos y otro dato que mantener sincronizado.
- Devolver `400` ante un `size` mayor que el tope: para el cliente es más útil recibir el máximo que un error.

## Consecuencias

La base necesita la extensión `unaccent`: con la imagen oficial de PostgreSQL se crea sola; en una base administrada podría requerir un permiso. La búsqueda por nombre no usa índice (`unaccent` no es inmutable); con el volumen del catálogo no hace falta. Una `Specification` más permite sumar criterios sin cambiar la firma del puerto. Los tests usan PostgreSQL real (Testcontainers), entre ellos que una página completa cueste a lo sumo cuatro consultas y que `%` en el nombre no sea un comodín; no se probó contra el catálogo real de la cátedra más allá del snapshot de la prueba manual.
