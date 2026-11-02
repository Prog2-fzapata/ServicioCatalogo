package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

// Sin @Transactional en la clase: las transacciones de los casos de uso deben confirmarse o deshacerse de verdad
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SynchronizeCatalogIntegrationTest {

    private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final SyncStateRepository syncStateRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcClient jdbc;

    @MockitoBean
    private CatedraApiClient catedraApiClient;

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("DELETE FROM weekly_schedules").update();
        jdbc.sql("DELETE FROM professionals").update();
        jdbc.sql("DELETE FROM professional_categories").update();
        jdbc.sql("""
                UPDATE sync_state SET local_version = 0, last_sync_at = NULL, last_sync_type = NULL,
                                      last_status = NULL, last_error = NULL""").update();
    }

    private CatalogSnapshot validSnapshot(long version) {
        return snapshot(version, List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10)));
    }

    // Una categoría y un profesional válidos más otro profesional con una categoría que no existe: falla al confirmar
    private CatalogSnapshot snapshotThatFailsHalfway(long version) {
        return snapshot(version, List.of(category(1), category(2)),
                List.of(professional(10, 1), professional(11, 99)), List.of(schedule(100, 10)));
    }

    private Map<String, Object> syncState() {
        return jdbc.sql("SELECT * FROM sync_state").query().singleRow();
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    private String categoryName(long id) {
        return jdbc.sql("SELECT name FROM professional_categories WHERE id = :id").param("id", id)
                .query(String.class).single();
    }

    @Test
    void storesTheCatalogAndTheVersionTogether() {
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(7));

        SyncResult result = synchronizeCatalogUseCase.synchronizeFull();

        assertThat(result).isEqualTo(new SyncResult(0, 7, SyncType.FULL));
        assertThat(count("professional_categories")).isEqualTo(1);
        assertThat(count("professionals")).isEqualTo(1);
        assertThat(count("weekly_schedules")).isEqualTo(1);
        assertThat(syncState()).containsEntry("local_version", 7L).containsEntry("last_sync_type", "FULL")
                .containsEntry("last_status", "OK").containsEntry("last_error", null);
        assertThat(syncState().get("last_sync_at")).isNotNull();
    }

    @Test
    void leavesDataAndVersionUntouchedWhenTheSnapshotFailsHalfway() {
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(7), snapshotThatFailsHalfway(8));
        synchronizeCatalogUseCase.synchronizeFull();

        assertThatThrownBy(() -> synchronizeCatalogUseCase.synchronizeFull()).isInstanceOf(RuntimeException.class);

        // Ni la categoría 2 nueva ni el profesional 11 quedaron; tampoco cambió la versión
        assertThat(count("professional_categories")).isEqualTo(1);
        assertThat(count("professionals")).isEqualTo(1);
        assertThat(syncState()).containsEntry("local_version", 7L);
    }

    @Test
    void recordsTheErrorInItsOwnTransactionWhenTheSnapshotFailsHalfway() {
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(7), snapshotThatFailsHalfway(8));
        synchronizeCatalogUseCase.synchronizeFull();

        assertThatThrownBy(() -> synchronizeCatalogUseCase.synchronizeFull()).isInstanceOf(RuntimeException.class);

        assertThat(syncState()).containsEntry("last_status", "ERROR").containsEntry("last_sync_type", "FULL");
        assertThat((String) syncState().get("last_error")).isNotBlank();
    }

    @Test
    void recordsTheErrorAndKeepsTheDataWhenTheCatedraDoesNotAnswer() {
        CatedraUnavailableException failure = new CatedraUnavailableException("sin conexión", null);
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(7)).thenThrow(failure);
        synchronizeCatalogUseCase.synchronizeFull();

        assertThatThrownBy(() -> synchronizeCatalogUseCase.synchronizeFull()).isSameAs(failure);

        assertThat(count("professional_categories")).isEqualTo(1);
        assertThat(syncState()).containsEntry("local_version", 7L).containsEntry("last_status", "ERROR")
                .containsEntry("last_error", "CatedraUnavailableException: sin conexión");
    }

    @Test
    void aSuccessfulSyncClearsTheErrorOfTheFailedOne() {
        when(catedraApiClient.fetchSnapshot())
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenReturn(validSnapshot(7));
        assertThatThrownBy(() -> synchronizeCatalogUseCase.synchronizeFull())
                .isInstanceOf(CatedraUnavailableException.class);

        synchronizeCatalogUseCase.synchronizeFull();

        assertThat(syncState()).containsEntry("last_status", "OK").containsEntry("last_error", null);
    }

    @Test
    void appliesTheSnapshotAndReportsItWhenItsVersionIsLowerThanTheLocalOne() {
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(9), validSnapshot(4));
        synchronizeCatalogUseCase.synchronizeFull();

        SyncResult result = synchronizeCatalogUseCase.synchronizeFull();

        assertThat(result).isEqualTo(new SyncResult(9, 4, SyncType.FULL));
        assertThat(syncState()).containsEntry("local_version", 4L);
    }

    @Test
    void updatesTheExistingRowsWhenTheCatedraChangesThem() {
        var renamed = category(1);
        renamed.setName("Cardiologia");
        when(catedraApiClient.fetchSnapshot()).thenReturn(validSnapshot(7),
                snapshot(8, List.of(renamed), List.of(professional(10, 1)), List.of(schedule(100, 10))));
        synchronizeCatalogUseCase.synchronizeFull();

        synchronizeCatalogUseCase.synchronizeFull();

        assertThat(categoryName(1)).isEqualTo("Cardiologia");
        assertThat(count("professional_categories")).isEqualTo(1);
    }

    @Test
    void aSecondTransactionWaitsWhileTheSyncStateRowIsLocked() throws Exception {
        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            Future<?> holder = pool.submit(() -> transaction.executeWithoutResult(status -> {
                syncStateRepository.findForUpdate();
                rowLocked.countDown();
                awaitQuietly(release);
            }));
            assertThat(rowLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<SyncResult> waiting = pool.submit(() -> applyCatalogSnapshotUseCase.applySnapshot(validSnapshot(7)));

            assertThatThrownBy(() -> waiting.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(syncState()).containsEntry("local_version", 0L);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertThat(waiting.get(5, TimeUnit.SECONDS)).isEqualTo(new SyncResult(0, 7, SyncType.FULL));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
