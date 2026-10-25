package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import tools.jackson.databind.json.JsonMapper;

class CatalogEventMapperTest {

    private final CatalogEventMapper mapper = new CatalogEventMapper(JsonMapper.builder().build());

    @Test
    void readsTheEventAndIgnoresTheFieldsItDoesNotUse() {
        String payload = """
                {"eventId":"catalog-event-7-4f3b","eventType":"CatalogUpdated","newVersion":7,
                 "occurredAt":"2026-09-10T13:44:59Z","source":"MANUAL","schemaVersion":1,"campoNuevo":"x"}""";

        assertThat(mapper.toDomainModel(payload))
                .isEqualTo(new CatalogUpdatedEvent("catalog-event-7-4f3b", "CatalogUpdated", 7));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "no es json",
            "",
            "null",
            "{\"eventType\":\"CatalogUpdated\",\"newVersion\":7}",
            "{\"eventId\":\" \",\"newVersion\":7}",
            "{\"eventId\":\"e1\"}",
            "{\"eventId\":\"e1\",\"newVersion\":-1}",
            "{\"eventId\":\"e1\",\"newVersion\":\"siete\"}"})
    void rejectsAMessageItCannotInterpret(String payload) {
        assertThatThrownBy(() -> mapper.toDomainModel(payload)).isInstanceOf(InvalidCatalogEventException.class);
    }
}
