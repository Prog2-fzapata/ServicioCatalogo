package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SyncStateTest {

    private static final Instant BEFORE = Instant.parse("2026-11-17T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    private SyncState failedAt(long localVersion) {
        return SyncState.builder().localVersion(localVersion).lastSyncAt(BEFORE).lastSyncType(SyncType.INCREMENTAL)
                .lastStatus(SyncStatus.ERROR).lastError("CatedraUnavailableException: sin respuesta").build();
    }

    @Test
    void aSuccessMovesTheVersionAndLeavesNoErrorPending() {
        SyncState state = failedAt(5);

        state.recordSuccess(9, SyncType.FULL, NOW);

        assertThat(state.getLocalVersion()).isEqualTo(9);
        assertThat(state.getLastSyncAt()).isEqualTo(NOW);
        assertThat(state.getLastSyncType()).isEqualTo(SyncType.FULL);
        assertThat(state.getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(state.getLastError()).isNull();
    }

    @Test
    void aFailureKeepsTheVersionAndRecordsWhatHappened() {
        SyncState state = SyncState.builder().localVersion(5).lastSyncAt(BEFORE).lastSyncType(SyncType.FULL)
                .lastStatus(SyncStatus.OK).build();

        state.recordFailure(SyncType.INCREMENTAL, "CatedraResponseException: JSON inválido", NOW);

        assertThat(state.getLocalVersion()).isEqualTo(5);
        assertThat(state.getLastSyncAt()).isEqualTo(NOW);
        assertThat(state.getLastSyncType()).isEqualTo(SyncType.INCREMENTAL);
        assertThat(state.getLastStatus()).isEqualTo(SyncStatus.ERROR);
        assertThat(state.getLastError()).isEqualTo("CatedraResponseException: JSON inválido");
    }

    @Test
    void clearingTheErrorKeepsTheVersionAndTheTypeOfTheLastAttempt() {
        SyncState state = failedAt(5);

        state.clearError(NOW);

        assertThat(state.getLocalVersion()).isEqualTo(5);
        assertThat(state.getLastSyncType()).isEqualTo(SyncType.INCREMENTAL);
        assertThat(state.getLastSyncAt()).isEqualTo(NOW);
        assertThat(state.getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(state.getLastError()).isNull();
    }
}
