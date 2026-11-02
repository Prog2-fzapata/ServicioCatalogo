package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka.dto;

// Forma del JSON de CatalogUpdated (anexo §15.3). Los campos que no se usan se ignoran, como pide el contrato
public record CatalogUpdatedMessage(String eventId, String eventType, Long newVersion) {
}
