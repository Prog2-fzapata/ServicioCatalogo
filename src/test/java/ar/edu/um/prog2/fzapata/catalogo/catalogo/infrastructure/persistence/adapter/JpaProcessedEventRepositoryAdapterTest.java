package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.ProcessedEventMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaProcessedEventRepositoryAdapter.class, ProcessedEventMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaProcessedEventRepositoryAdapterTest {

    private static final Instant AT = Instant.parse("2026-10-25T15:00:00Z");

    private final JpaProcessedEventRepositoryAdapter adapter;
    private final EntityManager entityManager;
    private final JdbcClient jdbc;

    private ProcessedEvent event(String id) {
        return ProcessedEvent.builder().eventId(id).eventType("CatalogUpdated").processedAt(AT).build();
    }

    // Escribe en la base y vacía la memoria de Hibernate: lo que se lee después viene de la base
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void doesNotKnowAnEventThatWasNeverSaved() {
        assertThat(adapter.existsById("evento-1")).isFalse();
    }

    @Test
    void savesEveryFieldAndFindsTheEventById() {
        adapter.save(event("evento-1"));
        flushAndClear();

        assertThat(adapter.existsById("evento-1")).isTrue();
        assertThat(jdbc.sql("SELECT event_type FROM processed_events WHERE event_id = 'evento-1'")
                .query(String.class).single()).isEqualTo("CatalogUpdated");
        assertThat(jdbc.sql("SELECT processed_at FROM processed_events WHERE event_id = 'evento-1'")
                .query(java.sql.Timestamp.class).single().toInstant()).isEqualTo(AT);
    }

    // La clave primaria es la defensa final ante dos procesos que guarden el mismo evento a la vez
    @Test
    void theDatabaseRejectsTheSameEventSavedTwiceByTwoTransactions() {
        jdbc.sql("INSERT INTO processed_events (event_id, event_type) VALUES ('evento-2', 'CatalogUpdated')").update();

        assertThatThrownBy(() -> jdbc.sql(
                "INSERT INTO processed_events (event_id, event_type) VALUES ('evento-2', 'CatalogUpdated')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
