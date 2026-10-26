package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka.dto.CatalogUpdatedMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CatalogEventMapper {

    private final JsonMapper jsonMapper;

    public CatalogEventMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    private CatalogUpdatedMessage parse(String payload) {
        try {
            CatalogUpdatedMessage message = jsonMapper.readValue(payload, CatalogUpdatedMessage.class);
            if (message == null) {
                throw new InvalidCatalogEventException("El mensaje de CatalogUpdated está vacío");
            }
            return message;
        } catch (JacksonException e) {
            throw new InvalidCatalogEventException("El mensaje de CatalogUpdated no es un JSON válido", e);
        }
    }

    public CatalogUpdatedEvent toDomainModel(String payload) {
        CatalogUpdatedMessage message = parse(payload);
        if (message.eventId() == null || message.eventId().isBlank()) {
            throw new InvalidCatalogEventException("Falta el eventId en el mensaje de CatalogUpdated");
        }
        if (message.newVersion() == null || message.newVersion() < 0) {
            throw new InvalidCatalogEventException("newVersion falta o es negativa en el evento " + message.eventId());
        }
        return new CatalogUpdatedEvent(message.eventId(), message.eventType(), message.newVersion());
    }
}
