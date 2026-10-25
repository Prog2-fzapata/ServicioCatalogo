package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class RecordSyncFailureUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    private SyncStateRepository syncStateRepository;

    private RecordSyncFailureUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordSyncFailureUseCaseImpl(syncStateRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenState(SyncState state) {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.of(state));
    }

    private SyncState savedState() {
        ArgumentCaptor<SyncState> captor = ArgumentCaptor.forClass(SyncState.class);
        verify(syncStateRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordsTheErrorAndKeepsTheLocalVersion() {
        givenState(SyncState.builder().localVersion(5).lastStatus(SyncStatus.OK).build());

        useCase.recordFailure(SyncType.FULL, new CatedraUnavailableException("sin conexión", null));

        SyncState saved = savedState();
        assertThat(saved.getLocalVersion()).isEqualTo(5);
        assertThat(saved.getLastSyncAt()).isEqualTo(NOW);
        assertThat(saved.getLastSyncType()).isEqualTo(SyncType.FULL);
        assertThat(saved.getLastStatus()).isEqualTo(SyncStatus.ERROR);
        assertThat(saved.getLastError()).isEqualTo("CatedraUnavailableException: sin conexión");
    }

    @Test
    void recordsOnlyTheClassNameWhenTheErrorHasNoMessage() {
        givenState(SyncState.builder().build());

        useCase.recordFailure(SyncType.FULL, new IllegalStateException());

        assertThat(savedState().getLastError()).isEqualTo("IllegalStateException");
    }

    @Test
    void cutsAVeryLongErrorMessage() {
        givenState(SyncState.builder().build());

        useCase.recordFailure(SyncType.FULL, new IllegalStateException("x".repeat(2000)));

        assertThat(savedState().getLastError()).hasSize(500).startsWith("IllegalStateException: xxx");
    }

    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.empty());
        IllegalStateException cause = new IllegalStateException("x");

        assertThatThrownBy(() -> useCase.recordFailure(SyncType.FULL, cause))
                .isInstanceOf(IllegalStateException.class);
    }
}
