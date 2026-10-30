package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.scheduling;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraSyncSource;
import lombok.RequiredArgsConstructor;

// La verificación corre sola, con la configuración real de Spring y PostgreSQL real; la cátedra es un doble que se
// prepara después de arrancar. Mientras no está preparado, las vueltas fallan y se registran: lo mismo que pasaría
// con la cátedra caída, y por eso cada test también prueba que el servicio sigue verificando
@SpringBootTest(properties = {
        "catedra.sync.check-enabled=true",
        "catedra.sync.check-interval=200ms",
        "catedra.sync.initial-delay=0s"})
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class CatalogVersionCheckSchedulerIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(30);

    private final JdbcClient jdbc;

    @MockitoBean
    private CatedraSyncSource catedraSyncSource;

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

    private void waitUntil(String description, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Se esperaba: " + description);
    }

    private long localVersion() {
        return jdbc.sql("SELECT local_version FROM sync_state WHERE id = 1").query(Long.class).single();
    }

    @Test
    void theFirstRoundsBringTheSnapshotWhenTheDatabaseIsEmpty() throws Exception {
        when(catedraSyncSource.readMetadata()).thenReturn(new SyncMetadata(3, 1, Instant.now()));
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(3, List.of(category(1)),
                List.of(professional(10, 1)), List.of()));

        waitUntil("la versión local en 3", () -> localVersion() == 3);

        assertThat(jdbc.sql("SELECT count(*) FROM professionals").query(Long.class).single()).isEqualTo(1);
    }

    // Kafka nunca avisó de las versiones 4 y 5: la vuelta periódica las encuentra en Redis y las aplica
    @Test
    void aLaterRoundRecoversALostNotificationWithoutAskingForTheSnapshot() throws Exception {
        jdbc.sql("UPDATE sync_state SET local_version = 3 WHERE id = 1").update();
        when(catedraSyncSource.readMetadata()).thenReturn(new SyncMetadata(5, 1, Instant.now()));
        when(catedraSyncSource.readChanges(4)).thenReturn(Optional.of(new CatalogChanges(4, List.of(), List.of(), List.of())));
        when(catedraSyncSource.readChanges(5)).thenReturn(Optional.of(new CatalogChanges(5, List.of(), List.of(), List.of())));

        waitUntil("la versión local en 5", () -> localVersion() == 5);

        verify(catedraApiClient, never()).fetchSnapshot();
    }

    // Con la cátedra caída cada vuelta falla: el servicio sigue verificando y deja el error en sync_state
    @Test
    void aFailedRoundDoesNotStopTheNextOnesAndLeavesTheErrorInTheSyncState() throws Exception {
        when(catedraSyncSource.readMetadata()).thenThrow(new CatedraUnavailableException("sin conexión", null));
        when(catedraApiClient.fetchSnapshot()).thenThrow(new CatedraUnavailableException("sin conexión", null));

        verify(catedraSyncSource, timeout(WAIT.toMillis()).atLeast(3)).readMetadata();

        assertThat(jdbc.sql("SELECT last_status FROM sync_state WHERE id = 1").query(String.class).single())
                .isEqualTo("ERROR");
        assertThat(localVersion()).isZero();
        verify(catedraApiClient, timeout(WAIT.toMillis()).atLeast(1)).fetchSnapshot();
        verify(catedraSyncSource, never()).readCategories(any());
    }
}
