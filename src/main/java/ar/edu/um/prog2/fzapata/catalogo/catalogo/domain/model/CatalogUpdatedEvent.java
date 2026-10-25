package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// Aviso de la cátedra de que hay una versión nueva del catálogo. No trae los datos: se piden por separado
public record CatalogUpdatedEvent(String eventId, String eventType, long newVersion) {
}
