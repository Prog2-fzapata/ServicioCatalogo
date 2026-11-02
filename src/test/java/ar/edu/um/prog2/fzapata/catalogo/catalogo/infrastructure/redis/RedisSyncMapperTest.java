package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.CatedraEntityMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis.dto.CatedraChangesResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis.dto.CatedraChangesResponse.Changes;

class RedisSyncMapperTest {

    private final RedisSyncMapper mapper = new RedisSyncMapper(new CatedraEntityMapper());

    private Map<Object, Object> metadata(String current, String oldest, String publishedAt) {
        return Map.of("currentVersion", current, "oldestAvailableVersion", oldest, "publishedAt", publishedAt);
    }

    @Test
    void convertsTheTextualMetadataToNumbersAndInstant() {
        SyncMetadata result = mapper.toMetadata(metadata("7", "4", "2026-09-10T13:44:59Z"));

        assertThat(result).isEqualTo(new SyncMetadata(7, 4, Instant.parse("2026-09-10T13:44:59Z")));
    }

    @Test
    void rejectsMetadataWithAMissingField() {
        Map<Object, Object> incomplete = Map.of("currentVersion", "7", "publishedAt", "2026-09-10T13:44:59Z");

        assertThatThrownBy(() -> mapper.toMetadata(incomplete))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("oldestAvailableVersion");
    }

    @Test
    void rejectsAnEmptyMetadataHash() {
        assertThatThrownBy(() -> mapper.toMetadata(Map.of())).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void rejectsAVersionThatIsNotANumber() {
        Map<Object, Object> invalid = metadata("siete", "4", "2026-09-10T13:44:59Z");

        assertThatThrownBy(() -> mapper.toMetadata(invalid))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("currentVersion");
    }

    @Test
    void rejectsAPublicationDateThatIsNotADate() {
        Map<Object, Object> invalid = metadata("7", "4", "ayer");

        assertThatThrownBy(() -> mapper.toMetadata(invalid))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("publishedAt");
    }

    @Test
    void convertsTheChangesOfAVersion() {
        CatedraChangesResponse response = new CatedraChangesResponse(7L,
                new Changes(List.of(1L), List.of(15L, 16L), List.of()));

        CatalogChanges result = mapper.toChanges(response);

        assertThat(result).isEqualTo(new CatalogChanges(7, List.of(1L), List.of(15L, 16L), List.of()));
    }

    @Test
    void rejectsChangesWithoutTheChangesObject() {
        CatedraChangesResponse response = new CatedraChangesResponse(7L, null);

        assertThatThrownBy(() -> mapper.toChanges(response))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("changes");
    }

    @Test
    void rejectsChangesWithAMissingArray() {
        CatedraChangesResponse response = new CatedraChangesResponse(7L, new Changes(List.of(), null, List.of()));

        assertThatThrownBy(() -> mapper.toChanges(response))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("changes.professionals");
    }

    @Test
    void rejectsChangesWithoutVersion() {
        CatedraChangesResponse response = new CatedraChangesResponse(null,
                new Changes(List.of(), List.of(), List.of()));

        assertThatThrownBy(() -> mapper.toChanges(response))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("version");
    }
}
