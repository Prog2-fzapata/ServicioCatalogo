package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatalogRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class ApplyCatalogSnapshotUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private SyncStateRepository syncStateRepository;

    private ApplyCatalogSnapshotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        useCase = new ApplyCatalogSnapshotUseCaseImpl(catalogRepository, syncStateRepository, clock);
    }

    private CatalogSnapshot snapshotOfVersion(long version) {
        return CatalogSnapshot.builder().version(version).build();
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
    void replacesCatalogAndSavesNewVersionAsSuccessfulFullSync() {
        givenState(SyncState.builder().localVersion(3).build());
        CatalogSnapshot snapshot = snapshotOfVersion(7);

        SyncResult result = useCase.applySnapshot(snapshot);

        verify(catalogRepository).replaceAll(snapshot);
        SyncState saved = savedState();
        assertThat(saved.getLocalVersion()).isEqualTo(7);
        assertThat(saved.getLastSyncAt()).isEqualTo(NOW);
        assertThat(saved.getLastSyncType()).isEqualTo(SyncType.FULL);
        assertThat(saved.getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(saved.getLastError()).isNull();
        assertThat(result).isEqualTo(new SyncResult(3, 7));
    }

    @Test
    void locksSyncStateBeforeTouchingTheCatalogAndSavesItAfter() {
        givenState(SyncState.builder().build());

        useCase.applySnapshot(snapshotOfVersion(1));

        InOrder order = inOrder(syncStateRepository, catalogRepository);
        order.verify(syncStateRepository).findForUpdate();
        order.verify(catalogRepository).replaceAll(any());
        order.verify(syncStateRepository).save(any());
    }

    @Test
    void appliesSnapshotWithLowerVersionAndReportsTheRegression() {
        givenState(SyncState.builder().localVersion(9).build());

        SyncResult result = useCase.applySnapshot(snapshotOfVersion(4));

        assertThat(savedState().getLocalVersion()).isEqualTo(4);
        assertThat(result).isEqualTo(new SyncResult(9, 4));
    }

    @Test
    void clearsTheErrorOfAPreviousFailedAttempt() {
        givenState(SyncState.builder().localVersion(3).lastStatus(SyncStatus.ERROR).lastError("boom").build());

        useCase.applySnapshot(snapshotOfVersion(4));

        SyncState saved = savedState();
        assertThat(saved.getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    void keepsTheStateUntouchedWhenReplacingTheCatalogFails() {
        givenState(SyncState.builder().localVersion(3).build());
        CatalogSnapshot snapshot = snapshotOfVersion(4);
        IllegalArgumentException failure = new IllegalArgumentException("dato inválido");
        doThrow(failure).when(catalogRepository).replaceAll(snapshot);

        assertThatThrownBy(() -> useCase.applySnapshot(snapshot)).isSameAs(failure);

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.empty());
        CatalogSnapshot snapshot = snapshotOfVersion(1);

        assertThatThrownBy(() -> useCase.applySnapshot(snapshot)).isInstanceOf(IllegalStateException.class);

        verify(catalogRepository, never()).replaceAll(any());
    }
}
