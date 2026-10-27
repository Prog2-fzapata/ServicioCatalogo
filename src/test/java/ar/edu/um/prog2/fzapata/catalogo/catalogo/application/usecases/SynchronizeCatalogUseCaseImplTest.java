package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogEventUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogIncrementUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraSyncSource;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class SynchronizeCatalogUseCaseImplTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-26T12:00:00Z");

    @Mock
    private CatedraApiClient catedraApiClient;

    @Mock
    private CatedraSyncSource catedraSyncSource;

    @Mock
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    @Mock
    private ApplyCatalogIncrementUseCase applyCatalogIncrementUseCase;

    @Mock
    private ApplyCatalogEventUseCase applyCatalogEventUseCase;

    @Mock
    private RecordSyncFailureUseCase recordSyncFailureUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private SyncStateRepository syncStateRepository;

    private SynchronizeCatalogUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SynchronizeCatalogUseCaseImpl(catedraApiClient, catedraSyncSource, applyCatalogSnapshotUseCase,
                applyCatalogIncrementUseCase, applyCatalogEventUseCase, recordSyncFailureUseCase,
                processedEventRepository, syncStateRepository);
    }

    private CatalogSnapshot snapshotOfVersion(long version) {
        return CatalogSnapshot.builder().version(version).build();
    }

    private CatedraUnavailableException unavailable() {
        return new CatedraUnavailableException("sin conexión", null);
    }

    private CatalogUpdatedEvent eventOfVersion(long version) {
        return new CatalogUpdatedEvent("evento-" + version, "CatalogUpdated", version);
    }

    private void givenLocalVersion(long version) {
        when(syncStateRepository.find()).thenReturn(Optional.of(SyncState.builder().localVersion(version).build()));
    }

    private void givenRedisVersions(long current, long oldest) {
        when(catedraSyncSource.readMetadata()).thenReturn(new SyncMetadata(current, oldest, PUBLISHED_AT));
    }

    // Cada versión afecta a una categoría con el mismo número que la versión: alcanza para distinguirlas
    private void givenChangesOf(long version) {
        when(catedraSyncSource.readChanges(version))
                .thenReturn(Optional.of(new CatalogChanges(version, List.of(version), List.of(), List.of())));
        when(catedraSyncSource.readCategories(List.of(version))).thenReturn(List.of());
        when(catedraSyncSource.readProfessionals(List.of())).thenReturn(List.of());
        when(catedraSyncSource.readWeeklySchedules(List.of())).thenReturn(List.of());
    }

    private CatalogIncrement incrementOfVersion(long version) {
        return argThat(increment -> increment.getVersion() == version);
    }

    private void givenSnapshotApplies(CatalogUpdatedEvent event, long version, SyncResult result) {
        CatalogSnapshot snapshot = snapshotOfVersion(version);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogEventUseCase.applyEvent(event, snapshot)).thenReturn(Optional.of(result));
    }

    private CatalogSnapshot givenSnapshotAppliesWithoutEvent(long version, SyncResult result) {
        CatalogSnapshot snapshot = snapshotOfVersion(version);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogSnapshotUseCase.applySnapshot(snapshot)).thenReturn(result);
        return snapshot;
    }

    @Test
    void fetchesTheSnapshotAndAppliesIt() {
        CatalogSnapshot snapshot = snapshotOfVersion(7);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogSnapshotUseCase.applySnapshot(snapshot)).thenReturn(new SyncResult(3, 7, SyncType.FULL));

        SyncResult result = useCase.synchronizeFull();

        assertThat(result).isEqualTo(new SyncResult(3, 7, SyncType.FULL));
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
    void ignoresAnAlreadyProcessedEventWithoutReadingRedisNorAskingTheCatedra() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(3);
        when(processedEventRepository.existsById("evento-7")).thenReturn(true);

        assertThat(useCase.synchronizeForEvent(event)).isEmpty();

        verify(catedraSyncSource, never()).readMetadata();
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void ignoresAnEventWhoseVersionIsExactlyTheLocalOne() {
        givenLocalVersion(7);

        assertThat(useCase.synchronizeForEvent(eventOfVersion(7))).isEmpty();

        verify(catedraSyncSource, never()).readMetadata();
    }

    // Un evento viejo que llega tarde se resuelve contra lo que publica Redis hoy, no contra su propia versión
    @Test
    void doesNothingWhenTheLocalVersionIsAlreadyTheCurrentOneInRedis() {
        givenLocalVersion(9);
        givenRedisVersions(9, 4);

        assertThat(useCase.synchronizeForEvent(eventOfVersion(7))).isEmpty();

        verify(catedraApiClient, never()).fetchSnapshot();
        verify(applyCatalogIncrementUseCase, never()).applyIncrement(any());
    }

    @Test
    void appliesTheSnapshotWhenTheDatabaseIsEmpty() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(0);
        givenRedisVersions(7, 0);
        givenSnapshotApplies(event, 7, new SyncResult(0, 7, SyncType.FULL));

        assertThat(useCase.synchronizeForEvent(event))
                .contains(new SyncResult(0, 7, SyncType.FULL, "la base está vacía"));

        verify(applyCatalogIncrementUseCase, never()).applyIncrement(any());
    }

    // La cátedra es la fuente de verdad: una versión local mayor que la suya es un retroceso y se aplica igual
    @Test
    void appliesTheSnapshotWhenTheCatedraWentBackwards() {
        CatalogUpdatedEvent event = eventOfVersion(5);
        givenLocalVersion(9);
        givenRedisVersions(5, 2);
        givenSnapshotApplies(event, 5, new SyncResult(9, 5, SyncType.FULL));

        Optional<SyncResult> result = useCase.synchronizeForEvent(event);

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.newVersion()).isEqualTo(5);
            assertThat(applied.snapshotReason()).contains("retrocedió", "9", "5");
        });
    }

    @Test
    void appliesTheSnapshotWhenTheLocalVersionIsOlderThanTheOldestAvailable() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(3);
        givenRedisVersions(7, 4);
        givenSnapshotApplies(event, 7, new SyncResult(3, 7, SyncType.FULL));

        Optional<SyncResult> result = useCase.synchronizeForEvent(event);

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).contains("anterior a la más antigua", "4");
        });
        verify(catedraSyncSource, never()).readChanges(4);
    }

    // Justo en el límite: con versión local 3 y la más antigua disponible 3 todavía se puede ir por incrementos
    @Test
    void usesIncrementsWhenTheLocalVersionIsExactlyTheOldestAvailable() {
        CatalogUpdatedEvent event = eventOfVersion(5);
        givenLocalVersion(3);
        givenRedisVersions(5, 3);
        givenChangesOf(4);
        givenChangesOf(5);
        when(applyCatalogEventUseCase.applyEvent(any(CatalogUpdatedEvent.class), any(CatalogIncrement.class)))
                .thenReturn(Optional.of(new SyncResult(4, 5, SyncType.INCREMENTAL)));

        assertThat(useCase.synchronizeForEvent(event)).contains(new SyncResult(3, 5, SyncType.INCREMENTAL));

        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void appliesEachVersionInOrderAndRecordsTheEventOnlyWithTheLastOne() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(4);
        givenRedisVersions(7, 2);
        givenChangesOf(5);
        givenChangesOf(6);
        givenChangesOf(7);
        when(applyCatalogEventUseCase.applyEvent(eq(event), incrementOfVersion(7)))
                .thenReturn(Optional.of(new SyncResult(6, 7, SyncType.INCREMENTAL)));

        assertThat(useCase.synchronizeForEvent(event)).contains(new SyncResult(4, 7, SyncType.INCREMENTAL));

        InOrder order = inOrder(applyCatalogIncrementUseCase, applyCatalogEventUseCase);
        order.verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(5));
        order.verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(6));
        order.verify(applyCatalogEventUseCase).applyEvent(eq(event), incrementOfVersion(7));
        verify(applyCatalogIncrementUseCase, times(2)).applyIncrement(any());
        verify(catedraApiClient, never()).fetchSnapshot();
        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void appliesASingleNewVersionTogetherWithTheEvent() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(6);
        givenRedisVersions(7, 1);
        givenChangesOf(7);
        when(applyCatalogEventUseCase.applyEvent(eq(event), incrementOfVersion(7)))
                .thenReturn(Optional.of(new SyncResult(6, 7, SyncType.INCREMENTAL)));

        assertThat(useCase.synchronizeForEvent(event)).contains(new SyncResult(6, 7, SyncType.INCREMENTAL));

        verify(applyCatalogIncrementUseCase, never()).applyIncrement(any());
    }

    @Test
    void returnsEmptyWhenTheEventTurnsOutToBeAlreadyProcessedWhileApplyingTheLastVersion() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(6);
        givenRedisVersions(7, 1);
        givenChangesOf(7);
        when(applyCatalogEventUseCase.applyEvent(eq(event), incrementOfVersion(7))).thenReturn(Optional.empty());

        assertThat(useCase.synchronizeForEvent(event)).isEmpty();

        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void fallsBackToTheSnapshotWhenAVersionHistoryIsMissingAndKeepsWhatWasAlreadyApplied() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(4);
        givenRedisVersions(7, 2);
        givenChangesOf(5);
        when(catedraSyncSource.readChanges(6)).thenReturn(Optional.empty());
        givenSnapshotApplies(event, 7, new SyncResult(5, 7, SyncType.FULL));

        Optional<SyncResult> result = useCase.synchronizeForEvent(event);

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).contains("falló la incremental", "versión 6");
        });
        verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(5));
        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void fallsBackToTheSnapshotWhenApplyingAVersionFails() {
        CatalogUpdatedEvent event = eventOfVersion(6);
        givenLocalVersion(4);
        givenRedisVersions(6, 2);
        givenChangesOf(5);
        doThrow(new IllegalStateException("violación de clave foránea"))
                .when(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(5));
        givenSnapshotApplies(event, 6, new SyncResult(4, 6, SyncType.FULL));

        Optional<SyncResult> result = useCase.synchronizeForEvent(event);

        assertThat(result).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("falló la incremental", "violación de clave foránea"));
        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void fallsBackToTheSnapshotWhenRedisDoesNotAnswer() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(4);
        when(catedraSyncSource.readMetadata()).thenThrow(unavailable());
        givenSnapshotApplies(event, 7, new SyncResult(4, 7, SyncType.FULL));

        Optional<SyncResult> result = useCase.synchronizeForEvent(event);

        assertThat(result).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("no se pudo leer Redis", "sin conexión"));
        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void explainsTheFallbackWithOnlyTheClassNameWhenTheFailureHasNoMessage() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        givenLocalVersion(4);
        when(catedraSyncSource.readMetadata()).thenThrow(new IllegalStateException());
        givenSnapshotApplies(event, 7, new SyncResult(4, 7, SyncType.FULL));

        assertThat(useCase.synchronizeForEvent(event)).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).isEqualTo("no se pudo leer Redis: IllegalStateException"));
    }

    @Test
    void recordsTheSnapshotFailureAndAttachesTheIncrementalOneWhenBothFail() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        CatedraUnavailableException incrementalFailure = unavailable();
        CatedraUnavailableException snapshotFailure = unavailable();
        givenLocalVersion(4);
        when(catedraSyncSource.readMetadata()).thenThrow(incrementalFailure);
        when(catedraApiClient.fetchSnapshot()).thenThrow(snapshotFailure);

        assertThatThrownBy(() -> useCase.synchronizeForEvent(event))
                .isSameAs(snapshotFailure)
                .hasSuppressedException(incrementalFailure);

        verify(recordSyncFailureUseCase).recordFailure(SyncType.FULL, snapshotFailure);
    }

    @Test
    void recordsTheFailureAndRethrowsTheSameExceptionWhenApplyingTheEventSnapshotFails() {
        CatalogUpdatedEvent event = eventOfVersion(7);
        CatalogSnapshot snapshot = snapshotOfVersion(7);
        IllegalStateException failure = new IllegalStateException("violación de clave foránea");
        givenLocalVersion(0);
        givenRedisVersions(7, 1);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(applyCatalogEventUseCase.applyEvent(event, snapshot)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.synchronizeForEvent(event)).isSameAs(failure);

        verify(recordSyncFailureUseCase).recordFailure(SyncType.FULL, failure);
    }

    @Test
    void failsWhenTheSyncStateRowIsMissingAndDoesNotRecordItAsASyncFailure() {
        when(syncStateRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.synchronizeForEvent(eventOfVersion(7)))
                .isInstanceOf(SyncStateMissingException.class)
                .hasMessage("La tabla sync_state está vacía, la base fue alterada");

        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void theVersionCheckAppliesTheSnapshotWhenTheDatabaseIsEmptyAndRecordsNoEvent() {
        givenLocalVersion(0);
        givenRedisVersions(7, 0);
        givenSnapshotAppliesWithoutEvent(7, new SyncResult(0, 7, SyncType.FULL));

        assertThat(useCase.synchronize()).contains(new SyncResult(0, 7, SyncType.FULL, "la base está vacía"));

        verify(applyCatalogEventUseCase, never()).applyEvent(any(CatalogUpdatedEvent.class),
                any(CatalogSnapshot.class));
        verify(processedEventRepository, never()).existsById(any());
    }

    @Test
    void theVersionCheckDoesNothingWhenTheCopyIsAlreadyCurrent() {
        givenLocalVersion(7);
        givenRedisVersions(7, 1);

        assertThat(useCase.synchronize()).isEmpty();

        verify(catedraApiClient, never()).fetchSnapshot();
        verify(applyCatalogIncrementUseCase, never()).applyIncrement(any());
    }

    // Una notificación perdida: Kafka nunca avisó de las versiones 5 a 7, pero Redis sí las tiene
    @Test
    void theVersionCheckRecoversALostNotificationWithIncrementsAndNoEvent() {
        givenLocalVersion(4);
        givenRedisVersions(7, 2);
        givenChangesOf(5);
        givenChangesOf(6);
        givenChangesOf(7);
        when(applyCatalogIncrementUseCase.applyIncrement(any())).thenReturn(new SyncResult(0, 0, SyncType.INCREMENTAL));

        assertThat(useCase.synchronize()).contains(new SyncResult(4, 7, SyncType.INCREMENTAL));

        InOrder order = inOrder(applyCatalogIncrementUseCase);
        order.verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(5));
        order.verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(6));
        order.verify(applyCatalogIncrementUseCase).applyIncrement(incrementOfVersion(7));
        verify(applyCatalogEventUseCase, never()).applyEvent(any(CatalogUpdatedEvent.class),
                any(CatalogIncrement.class));
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void theVersionCheckAppliesTheSnapshotWhenTheCatedraWentBackwards() {
        givenLocalVersion(9);
        givenRedisVersions(5, 2);
        givenSnapshotAppliesWithoutEvent(5, new SyncResult(9, 5, SyncType.FULL));

        assertThat(useCase.synchronize()).hasValueSatisfying(applied -> {
            assertThat(applied.newVersion()).isEqualTo(5);
            assertThat(applied.snapshotReason()).contains("retrocedió");
        });
    }

    @Test
    void theVersionCheckAppliesTheSnapshotWhenTheLocalVersionIsOlderThanTheOldestAvailable() {
        givenLocalVersion(3);
        givenRedisVersions(7, 4);
        givenSnapshotAppliesWithoutEvent(7, new SyncResult(3, 7, SyncType.FULL));

        assertThat(useCase.synchronize()).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("anterior a la más antigua"));
    }

    @Test
    void theVersionCheckFallsBackToTheSnapshotWhenAnIncrementFails() {
        givenLocalVersion(4);
        givenRedisVersions(6, 2);
        givenChangesOf(5);
        when(catedraSyncSource.readChanges(6)).thenReturn(Optional.empty());
        givenSnapshotAppliesWithoutEvent(6, new SyncResult(5, 6, SyncType.FULL));

        assertThat(useCase.synchronize()).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("falló la incremental", "versión 6"));

        verify(recordSyncFailureUseCase, never()).recordFailure(any(), any());
    }

    @Test
    void theVersionCheckFallsBackToTheSnapshotWhenRedisDoesNotAnswer() {
        givenLocalVersion(4);
        when(catedraSyncSource.readMetadata()).thenThrow(unavailable());
        givenSnapshotAppliesWithoutEvent(7, new SyncResult(4, 7, SyncType.FULL));

        assertThat(useCase.synchronize()).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("no se pudo leer Redis"));
    }

    @Test
    void theVersionCheckRecordsTheFailureAndRethrowsWhenEverythingFails() {
        CatedraUnavailableException redisFailure = unavailable();
        CatedraUnavailableException snapshotFailure = unavailable();
        givenLocalVersion(4);
        when(catedraSyncSource.readMetadata()).thenThrow(redisFailure);
        when(catedraApiClient.fetchSnapshot()).thenThrow(snapshotFailure);

        assertThatThrownBy(() -> useCase.synchronize())
                .isSameAs(snapshotFailure)
                .hasSuppressedException(redisFailure);

        verify(recordSyncFailureUseCase).recordFailure(SyncType.FULL, snapshotFailure);
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
        when(applyCatalogSnapshotUseCase.applySnapshot(any())).thenReturn(new SyncResult(0, 1, SyncType.FULL));
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
