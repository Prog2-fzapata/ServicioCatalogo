package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProcessedEventEntity;

class ProcessedEventMapperTest {

    private final ProcessedEventMapper mapper = new ProcessedEventMapper();

    @Test
    void mapsEveryFieldToTheEntity() {
        Instant at = Instant.parse("2026-10-25T15:00:00Z");

        ProcessedEventEntity entity = mapper.toEntity(
                ProcessedEvent.builder().eventId("evento-1").eventType("CatalogUpdated").processedAt(at).build());

        assertThat(entity.getEventId()).isEqualTo("evento-1");
        assertThat(entity.getEventType()).isEqualTo("CatalogUpdated");
        assertThat(entity.getProcessedAt()).isEqualTo(at);
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
