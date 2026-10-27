package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import lombok.RequiredArgsConstructor;

// Redis (con un usuario de solo lectura, como el de la cátedra) y PostgreSQL reales; solo el pedido REST del
// snapshot es un doble. Sin @Transactional en la clase: las transacciones de los casos de uso deben confirmarse
@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SynchronizeCatalogIncrementalIntegrationTest {

    private static final String USER = "grp_prueba";
    private static final String PASSWORD = "clave-de-prueba";
    private static final String CREATED = "2026-01-10T12:00:00Z";
    private static final String UPDATED = "2026-02-20T08:30:00Z";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory adminFactory;
    private static StringRedisTemplate admin;

    private final CatalogService catalogService;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final JdbcClient jdbc;

    @MockitoBean
    private CatedraApiClient catedraApiClient;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("CATEDRA_REDIS_HOST", REDIS::getHost);
        registry.add("CATEDRA_REDIS_PORT", () -> REDIS.getMappedPort(6379));
        registry.add("CATEDRA_REDIS_USERNAME", () -> USER);
        registry.add("CATEDRA_REDIS_PASSWORD", () -> PASSWORD);
    }

    @BeforeAll
    static void createReadOnlyUser() throws IOException, InterruptedException {
        REDIS.execInContainer("redis-cli", "ACL", "SETUSER", USER, "on", ">" + PASSWORD, "~catedra:sync:*",
                "+@read", "+@connection");
        adminFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        adminFactory.afterPropertiesSet();
        admin = new StringRedisTemplate(adminFactory);
    }

    @AfterAll
    static void disconnect() {
        adminFactory.destroy();
    }

    @BeforeEach
    void cleanRedisAndDatabase() {
        admin.getConnectionFactory().getConnection().serverCommands().flushAll();
        jdbc.sql("DELETE FROM processed_events").update();
        jdbc.sql("DELETE FROM weekly_schedules").update();
        jdbc.sql("DELETE FROM professionals").update();
        jdbc.sql("DELETE FROM professional_categories").update();
        jdbc.sql("""
                UPDATE sync_state SET local_version = 0, last_sync_at = NULL, last_sync_type = NULL,
                                      last_status = NULL, last_error = NULL""").update();
    }

    private void givenLocalCopyAtVersion(long version) {
        applyCatalogSnapshotUseCase.applySnapshot(snapshot(version, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));
    }

    private void givenRedisVersions(long current, long oldest) {
        admin.opsForHash().putAll("catedra:sync:metadata", Map.of("currentVersion", String.valueOf(current),
                "oldestAvailableVersion", String.valueOf(oldest), "publishedAt", "2026-10-26T12:00:00Z"));
    }

    private void givenVersionAffecting(long version, List<Long> categories, List<Long> professionals,
            List<Long> schedules) {
        admin.opsForValue().set("catedra:sync:changes:" + version, """
                {"version": %d, "changes": {"professionalCategories": %s, "professionals": %s,
                 "weeklySchedules": %s}}""".formatted(version, categories, professionals, schedules));
    }

    private void givenCategoryInRedis(long id, boolean enabled) {
        admin.opsForHash().put("catedra:sync:professional-categories", String.valueOf(id), """
                {"id": %d, "name": "Categoria %d", "description": "Descripcion %d", "enabled": %b,
                 "createdAt": "%s", "updatedAt": "%s"}""".formatted(id, id, id, enabled, CREATED, UPDATED));
    }

    private void givenProfessionalInRedis(long id, long categoryId, boolean enabled) {
        admin.opsForHash().put("catedra:sync:professionals", String.valueOf(id), """
                {"id": %d, "categoryId": %d, "firstName": "Nombre%d", "lastName": "Apellido%d", "enabled": %b,
                 "createdAt": "%s", "updatedAt": "%s"}""".formatted(id, categoryId, id, id, enabled, CREATED, UPDATED));
    }

    private void givenScheduleInRedis(long id, long professionalId) {
        admin.opsForHash().put("catedra:sync:weekly-schedules", String.valueOf(id), """
                {"id": %d, "professionalId": %d, "dayOfWeek": "TUESDAY", "startTime": "09:00:00",
                 "endTime": "12:30:00", "slotDurationMinutes": 30, "enabled": true,
                 "createdAt": "%s", "updatedAt": "%s"}""".formatted(id, professionalId, CREATED, UPDATED));
    }

    private CatalogUpdatedEvent event(String id, long version) {
        return new CatalogUpdatedEvent(id, "CatalogUpdated", version);
    }

    private long localVersion() {
        return jdbc.sql("SELECT local_version FROM sync_state WHERE id = 1").query(Long.class).single();
    }

    private String lastSyncType() {
        return jdbc.sql("SELECT last_sync_type FROM sync_state WHERE id = 1").query(String.class).single();
    }

    private long count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }

    @Test
    void appliesSeveralVersionsInOrderAndRecordsTheEvent() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(6, 2);
        givenVersionAffecting(5, List.of(2L), List.of(), List.of());
        givenCategoryInRedis(2, true);
        givenVersionAffecting(6, List.of(), List.of(11L), List.of(101L));
        givenProfessionalInRedis(11, 2, true);
        givenScheduleInRedis(101, 11);

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-6", 6));

        assertThat(result).contains(new SyncResult(4, 6, SyncType.INCREMENTAL));
        assertThat(localVersion()).isEqualTo(6);
        assertThat(lastSyncType()).isEqualTo("INCREMENTAL");
        assertThat(count("professional_categories")).isEqualTo(2);
        assertThat(count("professionals")).isEqualTo(2);
        assertThat(count("weekly_schedules")).isEqualTo(2);
        assertThat(count("processed_events")).isEqualTo(1);
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    // Una versión puede no traer ninguna entidad: la lectura por ids con una lista vacía tiene que funcionar
    @Test
    void advancesTheVersionWhenAVersionAffectsNoEntity() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(5, 2);
        givenVersionAffecting(5, List.of(), List.of(), List.of());

        assertThat(catalogService.synchronizeForEvent(event("evento-5", 5)))
                .contains(new SyncResult(4, 5, SyncType.INCREMENTAL));

        assertThat(localVersion()).isEqualTo(5);
        assertThat(count("professionals")).isEqualTo(1);
    }

    @Test
    void keepsADisabledEntityAsDisabledInsteadOfDeletingIt() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(5, 2);
        givenVersionAffecting(5, List.of(), List.of(10L), List.of());
        givenProfessionalInRedis(10, 1, false);

        catalogService.synchronizeForEvent(event("evento-5", 5));

        assertThat(jdbc.sql("SELECT enabled FROM professionals WHERE id = 10").query(Boolean.class).single())
                .isFalse();
        assertThat(count("weekly_schedules")).isEqualTo(1);
    }

    @Test
    void anEventAlreadyProcessedDoesNotChangeTheResult() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(5, 2);
        givenVersionAffecting(5, List.of(2L), List.of(), List.of());
        givenCategoryInRedis(2, true);
        catalogService.synchronizeForEvent(event("evento-5", 5));

        Optional<SyncResult> second = catalogService.synchronizeForEvent(event("evento-5", 5));

        assertThat(second).isEmpty();
        assertThat(localVersion()).isEqualTo(5);
        assertThat(count("processed_events")).isEqualTo(1);
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void doesNothingWhenAnOldEventArrivesAfterTheCopyIsAlreadyCurrent() {
        givenLocalCopyAtVersion(7);
        givenRedisVersions(7, 2);

        assertThat(catalogService.synchronizeForEvent(event("evento-viejo", 5))).isEmpty();

        assertThat(localVersion()).isEqualTo(7);
        assertThat(count("processed_events")).isZero();
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void fallsBackToTheSnapshotWhenTheLocalVersionIsOlderThanTheOldestAvailable() {
        givenLocalCopyAtVersion(3);
        givenRedisVersions(7, 4);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(7, List.of(category(1), category(2)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-7", 7));

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).contains("anterior a la más antigua");
        });
        assertThat(localVersion()).isEqualTo(7);
        assertThat(lastSyncType()).isEqualTo("FULL");
        assertThat(count("professional_categories")).isEqualTo(2);
        assertThat(count("processed_events")).isEqualTo(1);
    }

    // La cátedra es la fuente de verdad: si publica una versión menor que la local, se aplica su snapshot
    @Test
    void appliesTheSnapshotWhenTheCatedraWentBackwards() {
        givenLocalCopyAtVersion(9);
        givenRedisVersions(5, 2);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(5, List.of(category(1)),
                List.of(professional(10, 1)), List.of()));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-5", 5));

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).contains("retrocedió");
        });
        assertThat(localVersion()).isEqualTo(5);
        assertThat(count("weekly_schedules")).isZero();
    }

    @Test
    void fallsBackToTheSnapshotWhenTheVersionHistoryIsMissing() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(6, 2);
        givenVersionAffecting(5, List.of(2L), List.of(), List.of());
        givenCategoryInRedis(2, true);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(6, List.of(category(1), category(2)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-6", 6));

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).contains("falló la incremental", "versión 6");
        });
        assertThat(localVersion()).isEqualTo(6);
        assertThat(count("processed_events")).isEqualTo(1);
    }

    @Test
    void fallsBackToTheSnapshotWhenAnEntityOfTheVersionIsMissingInRedis() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(5, 2);
        givenVersionAffecting(5, List.of(), List.of(11L), List.of());
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(5, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-5", 5));

        assertThat(result).hasValueSatisfying(applied -> assertThat(applied.type()).isEqualTo(SyncType.FULL));
        assertThat(localVersion()).isEqualTo(5);
    }

    // Una versión que la base rechaza (profesional de una categoría que no existe) no deja nada a medias
    @Test
    void fallsBackToTheSnapshotWhenTheDatabaseRejectsAVersion() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(5, 2);
        givenVersionAffecting(5, List.of(), List.of(11L), List.of());
        givenProfessionalInRedis(11, 99, true);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(5, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-5", 5));

        assertThat(result).hasValueSatisfying(applied -> assertThat(applied.type()).isEqualTo(SyncType.FULL));
        assertThat(count("professionals")).isEqualTo(1);
        assertThat(localVersion()).isEqualTo(5);
    }

    @Test
    void fallsBackToTheSnapshotWhenRedisHasNoMetadata() {
        givenLocalCopyAtVersion(4);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(6, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronizeForEvent(event("evento-6", 6));

        assertThat(result).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("no se pudo leer Redis"));
        assertThat(localVersion()).isEqualTo(6);
    }

    // Una notificación de Kafka perdida: nadie avisó de las versiones 5 y 6, pero la verificación las encuentra en Redis
    @Test
    void theVersionCheckRecoversALostNotificationAndRecordsNoEvent() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(6, 2);
        givenVersionAffecting(5, List.of(2L), List.of(), List.of());
        givenCategoryInRedis(2, true);
        givenVersionAffecting(6, List.of(), List.of(11L), List.of());
        givenProfessionalInRedis(11, 2, true);

        Optional<SyncResult> result = catalogService.synchronize();

        assertThat(result).contains(new SyncResult(4, 6, SyncType.INCREMENTAL));
        assertThat(localVersion()).isEqualTo(6);
        assertThat(count("professionals")).isEqualTo(2);
        assertThat(count("processed_events")).isZero();
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void theVersionCheckBringsTheSnapshotWhenTheDatabaseIsEmpty() {
        givenRedisVersions(3, 1);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(3, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        Optional<SyncResult> result = catalogService.synchronize();

        assertThat(result).hasValueSatisfying(applied -> {
            assertThat(applied.type()).isEqualTo(SyncType.FULL);
            assertThat(applied.snapshotReason()).isEqualTo("la base está vacía");
        });
        assertThat(localVersion()).isEqualTo(3);
        assertThat(count("professionals")).isEqualTo(1);
        assertThat(count("processed_events")).isZero();
    }

    @Test
    void theVersionCheckDoesNothingWhenTheCopyIsAlreadyCurrent() {
        givenLocalCopyAtVersion(5);
        givenRedisVersions(5, 2);

        assertThat(catalogService.synchronize()).isEmpty();

        assertThat(localVersion()).isEqualTo(5);
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void theVersionCheckFallsBackToTheSnapshotWhenRedisHasNoMetadata() {
        givenLocalCopyAtVersion(4);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(6, List.of(category(1)),
                List.of(professional(10, 1)), List.of(schedule(100, 10))));

        assertThat(catalogService.synchronize()).hasValueSatisfying(applied ->
                assertThat(applied.snapshotReason()).contains("no se pudo leer Redis"));

        assertThat(localVersion()).isEqualTo(6);
    }

    // Una transacción por versión: si la cátedra falla a mitad, lo ya aplicado se conserva y el evento no queda
    // como procesado
    @Test
    void keepsTheVersionsAlreadyAppliedAndDoesNotRecordTheEventWhenEverythingFailsMidway() {
        givenLocalCopyAtVersion(4);
        givenRedisVersions(6, 2);
        givenVersionAffecting(5, List.of(2L), List.of(), List.of());
        givenCategoryInRedis(2, true);
        when(catedraApiClient.fetchSnapshot()).thenThrow(new CatedraUnavailableException("sin conexión", null));
        CatalogUpdatedEvent event = event("evento-6", 6);

        assertThatThrownBy(() -> catalogService.synchronizeForEvent(event))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasSuppressedException(new IllegalStateException(
                        "La cátedra ya no conserva los cambios de la versión 6"));

        assertThat(localVersion()).isEqualTo(5);
        assertThat(count("professional_categories")).isEqualTo(2);
        assertThat(count("processed_events")).isZero();
        assertThat(jdbc.sql("SELECT last_status FROM sync_state WHERE id = 1").query(String.class).single())
                .isEqualTo("ERROR");
    }
}
