package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class ClearSyncErrorUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-27T20:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-10-27T19:54:25Z");

    @Mock
    private SyncStateRepository syncStateRepository;

    private ClearSyncErrorUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ClearSyncErrorUseCaseImpl(syncStateRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenState(SyncState state) {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.of(state));
    }

    @Test
    void clearsTheErrorAndKeepsTheVersionAndTheTypeOfTheLastAttempt() {
        givenState(SyncState.builder().localVersion(5).lastSyncAt(BEFORE).lastSyncType(SyncType.FULL)
                .lastStatus(SyncStatus.ERROR).lastError("CatedraUnavailableException: sin conexión").build());

        useCase.clearError();

        ArgumentCaptor<SyncState> saved = ArgumentCaptor.forClass(SyncState.class);
        verify(syncStateRepository).save(saved.capture());
        assertThat(saved.getValue().getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(saved.getValue().getLastError()).isNull();
        assertThat(saved.getValue().getLastSyncAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getLocalVersion()).isEqualTo(5);
        assertThat(saved.getValue().getLastSyncType()).isEqualTo(SyncType.FULL);
    }

    // Una verificación que encuentra todo al día cada cinco minutos no debe escribir en la base
    @Test
    void writesNothingWhenThereIsNoErrorToClear() {
        givenState(SyncState.builder().localVersion(5).lastSyncAt(BEFORE).lastStatus(SyncStatus.OK).build());

        useCase.clearError();

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void writesNothingWhenNothingWasEverSynchronized() {
        givenState(SyncState.builder().build());

        useCase.clearError();

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.clearError()).isInstanceOf(SyncStateMissingException.class)
                .hasMessage("La tabla sync_state está vacía, la base fue alterada");
    }
}
