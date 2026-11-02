package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.SyncStatusResponse;

class SyncStatusDtoMapperTest {

    private final SyncStatusDtoMapper mapper = new SyncStatusDtoMapper();

    @Test
    void mapsEveryField() {
        Instant at = Instant.parse("2026-10-27T19:54:25Z");

        SyncStatusResponse response = mapper.toResponse(SyncState.builder().localVersion(5).lastSyncAt(at)
                .lastSyncType(SyncType.INCREMENTAL).lastStatus(SyncStatus.ERROR)
                .lastError("CatedraUnavailableException: sin conexión").build());

        assertThat(response.getLocalVersion()).isEqualTo(5);
        assertThat(response.getLastSyncAt()).isEqualTo(at);
        assertThat(response.getLastSyncType()).isEqualTo(SyncType.INCREMENTAL);
        assertThat(response.getLastStatus()).isEqualTo(SyncStatus.ERROR);
        assertThat(response.getLastError()).isEqualTo("CatedraUnavailableException: sin conexión");
    }

    @Test
    void keepsNullsForABaseThatWasNeverSynchronized() {
        SyncStatusResponse response = mapper.toResponse(SyncState.builder().build());

        assertThat(response.getLocalVersion()).isZero();
        assertThat(response.getLastSyncAt()).isNull();
        assertThat(response.getLastSyncType()).isNull();
        assertThat(response.getLastStatus()).isNull();
        assertThat(response.getLastError()).isNull();
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
