-- La búsqueda de profesionales por nombre ignora mayúsculas y tildes: «gomez» encuentra «Gómez». unaccent viene con
-- PostgreSQL y está marcada como confiable, así que la puede crear el dueño de la base sin ser superusuario
CREATE EXTENSION IF NOT EXISTS unaccent;
