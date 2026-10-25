package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import ar.edu.um.prog2.fzapata.catalogo.KafkaTestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;

// Kafka y PostgreSQL reales. Sin @Transactional en la clase: el listener corre en otro hilo y las transacciones
// de los casos de uso deben confirmarse de verdad. Cada test usa eventos y versiones propios
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=true",
        "catedra.kafka.max-retries=2",
        "catedra.kafka.initial-backoff=20ms"})
@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class CatalogEventListenerIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(30);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdmin kafkaAdmin;
    private final Environment environment;
    private final JdbcClient jdbc;

    @MockitoBean
    private CatedraApiClient catedraApiClient;

    @MockitoSpyBean
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("DELETE FROM processed_events").update();
        jdbc.sql("DELETE FROM weekly_schedules").update();
        jdbc.sql("DELETE FROM professionals").update();
        jdbc.sql("DELETE FROM professional_categories").update();
        jdbc.sql("""
                UPDATE sync_state SET local_version = 0, last_sync_at = NULL, last_sync_type = NULL,
                                      last_status = NULL, last_error = NULL""").update();
    }

    private String topic() {
        return environment.getRequiredProperty("catedra.contract.kafka-catalog-topic");
    }

    private void send(String payload) {
        kafkaTemplate.send(topic(), payload);
        kafkaTemplate.flush();
    }

    private String event(String eventId, long version) {
        return """
                {"eventId":"%s","eventType":"CatalogUpdated","newVersion":%d,
                 "occurredAt":"2026-10-25T12:00:00Z","source":"MANUAL","schemaVersion":1}""".formatted(eventId, version);
    }

    private CatalogSnapshot catalogOfVersion(long version) {
        return snapshot(version, List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10)));
    }

    // Mensajes ya consumidos y confirmados del grupo (el topic tiene una sola partición)
    private long committedOffset() throws Exception {
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            String group = environment.getRequiredProperty("spring.kafka.consumer.group-id");
            return admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS)
                    .entrySet().stream()
                    .filter(entry -> entry.getKey().topic().equals(topic()))
                    .map(java.util.Map.Entry::getValue)
                    .mapToLong(OffsetAndMetadata::offset)
                    .sum();
        }
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

    private boolean offsetReached(long offset) {
        try {
            return committedOffset() >= offset;
        } catch (Exception e) {
            return false;
        }
    }

    private long localVersion() {
        return jdbc.sql("SELECT local_version FROM sync_state WHERE id = 1").query(Long.class).single();
    }

    private long processedEvents() {
        return jdbc.sql("SELECT count(*) FROM processed_events").query(Long.class).single();
    }

    @Test
    void appliesTheCatalogAndRecordsTheEventBeforeConfirmingTheOffset() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot()).thenReturn(catalogOfVersion(5));

        send(event("evento-aplica", 5));
        waitUntil("el offset confirmado", () -> offsetReached(before + 1));

        // Si el offset ya está confirmado, la transacción tuvo que confirmarse antes
        assertThat(localVersion()).isEqualTo(5);
        assertThat(processedEvents()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM professionals").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void theOffsetIsNotConfirmedWhileTheEventIsStillBeingProcessed() throws Exception {
        long before = committedOffset();
        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(catedraApiClient.fetchSnapshot()).thenAnswer(invocation -> {
            processing.countDown();
            release.await(30, TimeUnit.SECONDS);
            return catalogOfVersion(6);
        });

        send(event("evento-lento", 6));
        assertThat(processing.await(30, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(1500);
        assertThat(committedOffset()).isEqualTo(before);

        release.countDown();
        waitUntil("el offset confirmado", () -> offsetReached(before + 1));
        assertThat(localVersion()).isEqualTo(6);
    }

    @Test
    void aRepeatedEventDoesNotChangeTheResult() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot()).thenReturn(catalogOfVersion(7));

        send(event("evento-repetido", 7));
        send(event("evento-repetido", 7));
        waitUntil("los dos mensajes confirmados", () -> offsetReached(before + 2));

        verify(catedraApiClient, times(1)).fetchSnapshot();
        assertThat(localVersion()).isEqualTo(7);
        assertThat(processedEvents()).isEqualTo(1);
    }

    // Mismo eventId con una versión mayor: la versión sola no lo descarta, solo lo hace el eventId
    @Test
    void theEventIdAloneIsEnoughToDiscardARepeatedEvent() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot()).thenReturn(catalogOfVersion(7));

        send(event("evento-mismo-id", 7));
        send(event("evento-mismo-id", 8));
        waitUntil("los dos mensajes confirmados", () -> offsetReached(before + 2));

        verify(catedraApiClient, times(1)).fetchSnapshot();
        assertThat(localVersion()).isEqualTo(7);
    }

    // Si no se puede guardar el eventId, los datos y la versión tampoco quedan: es una sola transacción
    @Test
    void ifTheEventIdCannotBeSavedNeitherTheDataNorTheVersionRemain() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot()).thenReturn(catalogOfVersion(12));
        IllegalStateException failure = new IllegalStateException("no se pudo guardar");
        doThrow(failure).when(processedEventRepository).save(any());

        send(event("evento-atomico", 12));
        waitUntil("el mensaje descartado y confirmado", () -> offsetReached(before + 1));

        assertThat(localVersion()).isZero();
        assertThat(processedEvents()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM professionals").query(Long.class).single()).isZero();
    }

    @Test
    void aFailureIsRetriedUntilTheEventIsApplied() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot())
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenReturn(catalogOfVersion(8));

        send(event("evento-reintento", 8));
        waitUntil("el offset confirmado", () -> offsetReached(before + 1));

        verify(catedraApiClient, times(2)).fetchSnapshot();
        assertThat(localVersion()).isEqualTo(8);
        assertThat(processedEvents()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT last_status FROM sync_state WHERE id = 1").query(String.class).single())
                .isEqualTo("OK");
    }

    // Sin cola de eventos muertos (D70): agotados los reintentos se descarta y el siguiente evento se procesa
    @Test
    void anEventThatAlwaysFailsIsSkippedAfterTheRetriesAndTheNextOneIsProcessed() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot())
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenReturn(catalogOfVersion(10));

        send(event("evento-roto", 9));
        send(event("evento-siguiente", 10));
        waitUntil("los dos mensajes confirmados", () -> offsetReached(before + 2));

        // Primer intento más dos reintentos; el cuarto pedido es el del evento siguiente
        verify(catedraApiClient, times(4)).fetchSnapshot();
        assertThat(localVersion()).isEqualTo(10);
        assertThat(processedEvents()).isEqualTo(1);
    }

    @Test
    void anInvalidMessageIsSkippedWithoutRetryingAndTheNextOneIsProcessed() throws Exception {
        long before = committedOffset();
        when(catedraApiClient.fetchSnapshot()).thenReturn(catalogOfVersion(11));

        send("esto no es json");
        send(event("evento-valido", 11));
        waitUntil("los dos mensajes confirmados", () -> offsetReached(before + 2));

        verify(catedraApiClient, times(1)).fetchSnapshot();
        assertThat(localVersion()).isEqualTo(11);
    }
}
