package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;

@ExtendWith(MockitoExtension.class)
class SynchronizeCatalogUseCaseImplTest {

    @Mock
    private CatedraApiClient catedraApiClient;

    @Mock
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    @Mock
    private RecordSyncFailureUseCase recordSyncFailureUseCase;

    private SynchronizeCatalogUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SynchronizeCatalogUseCaseImpl(catedraApiClient, applyCatalogSnapshotUseCase,
                recordSyncFailureUseCase);
    }

    private CatalogSnapshot snapshotOfVersion(long version) {
        return CatalogSnapshot.builder().version(version).build();
    }

    private CatedraUnavailableException unavailable() {
        return new CatedraUnavailableException("sin conexión", null);
    }

    @Test
    void fetchesTheSnapshotAndAppliesIt() {
        CatalogSnapshot snapshot = snapshotOfVersion(7);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogSnapshotUseCase.applySnapshot(snapshot)).thenReturn(new SyncResult(3, 7));

        SyncResult result = useCase.synchronizeFull();

        assertThat(result).isEqualTo(new SyncResult(3, 7));
        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void recordsTheFailureAndRethrowsTheSameExceptionWhenTheCatedraFails() {
        CatedraUnavailableException failure = unavailable();
        when(catedraApiClient.fetchSnapshot()).thenThrow(failure);

        assertThatThrownBy(() -> useCase.synchronizeFull()).isSameAs(failure);

        verify(recordSyncFailureUseCase).recordFailure(SyncType.FULL, failure);
        verify(applyCatalogSnapshotUseCase, never()).applySnapshot(any());
    }

    @Test
    void recordsTheFailureAndRethrowsTheSameExceptionWhenApplyingFails() {
        CatalogSnapshot snapshot = snapshotOfVersion(7);
        IllegalStateException failure = new IllegalStateException("violación de clave foránea");
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogSnapshotUseCase.applySnapshot(snapshot)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.synchronizeFull()).isSameAs(failure);

        verify(recordSyncFailureUseCase).recordFailure(SyncType.FULL, failure);
    }

    @Test
    void keepsTheOriginalErrorAndAttachesTheRecordingFailureWhenRecordingFails() {
        CatedraUnavailableException original = unavailable();
        IllegalStateException recordingFailure = new IllegalStateException("base caída");
        when(catedraApiClient.fetchSnapshot()).thenThrow(original);
        doThrow(recordingFailure).when(recordSyncFailureUseCase).recordFailure(SyncType.FULL, original);

        assertThatThrownBy(() -> useCase.synchronizeFull())
                .isSameAs(original)
                .hasSuppressedException(recordingFailure);
    }

    @Test
    void secondSynchronizationWaitsForTheFirstAndThenFetchesItsOwnSnapshot() throws Exception {
        CountDownLatch firstIsFetching = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger fetches = new AtomicInteger();
        when(catedraApiClient.fetchSnapshot()).thenAnswer(invocation -> {
            int number = fetches.incrementAndGet();
            if (number == 1) {
                firstIsFetching.countDown();
                releaseFirst.await(5, TimeUnit.SECONDS);
            }
            return snapshotOfVersion(number);
        });
        when(applyCatalogSnapshotUseCase.applySnapshot(any())).thenReturn(new SyncResult(0, 1));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<SyncResult> first = pool.submit(useCase::synchronizeFull);
            assertThat(firstIsFetching.await(5, TimeUnit.SECONDS)).isTrue();
            Future<SyncResult> second = pool.submit(useCase::synchronizeFull);

            // Mientras la primera sigue pidiendo el snapshot, la segunda no puede terminar ni pedir el suyo
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(fetches.get()).isEqualTo(1);

            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertThat(fetches.get()).isEqualTo(2);
            verify(applyCatalogSnapshotUseCase, times(2)).applySnapshot(any());
        } finally {
            pool.shutdownNow();
        }
    }
}
