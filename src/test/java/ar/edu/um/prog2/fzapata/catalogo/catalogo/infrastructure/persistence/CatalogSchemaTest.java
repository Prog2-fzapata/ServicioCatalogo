package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import lombok.RequiredArgsConstructor;

// Prueba el esquema de la migración V2 directamente en SQL: todavía no hay clases Java del catálogo
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class CatalogSchemaTest {

    private final JdbcClient jdbc;

    private void insertCategory(long id) {
        jdbc.sql("""
                INSERT INTO professional_categories (id, name, description, enabled, created_at, updated_at)
                VALUES (:id, 'Clinica medica', 'Atencion clinica general', true, now(), now())""")
                .param("id", id).update();
    }

    private void insertProfessional(long id, long categoryId) {
        jdbc.sql("""
                INSERT INTO professionals (id, category_id, first_name, last_name, enabled, created_at, updated_at)
                VALUES (:id, :categoryId, 'Ana', 'Gomez', true, now(), now())""")
                .param("id", id).param("categoryId", categoryId).update();
    }

    private void insertSchedule(long id, long professionalId, String day, String start, String end, int slotMinutes) {
        jdbc.sql("""
                INSERT INTO weekly_schedules (id, professional_id, day_of_week, start_time, end_time,
                                              slot_duration_minutes, enabled, created_at, updated_at)
                VALUES (:id, :professionalId, :day, CAST(:start AS time), CAST(:end AS time), :slot,
                        true, now(), now())""")
                .param("id", id).param("professionalId", professionalId).param("day", day)
                .param("start", start).param("end", end).param("slot", slotMinutes).update();
    }

    private void insertProcessedEvent(String eventId) {
        jdbc.sql("INSERT INTO processed_events (event_id, event_type) VALUES (:id, 'CatalogUpdated')")
                .param("id", eventId).update();
    }

    private void insertParents() {
        insertCategory(1);
        insertProfessional(15, 1);
    }

    @Test
    void validCategoryProfessionalAndScheduleAreAccepted() {
        insertParents();
        insertSchedule(21, 15, "MONDAY", "09:00", "12:00", 30);

        Map<String, Object> schedule = jdbc.sql("SELECT * FROM weekly_schedules WHERE id = 21").query().singleRow();

        assertThat(schedule).containsEntry("professional_id", 15L).containsEntry("day_of_week", "MONDAY")
                .containsEntry("slot_duration_minutes", 30);
    }

    @Test
    void professionalWithUnknownCategoryIsRejected() {
        assertThatThrownBy(() -> insertProfessional(15, 999))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_professionals_category");
    }

    @Test
    void scheduleWithUnknownProfessionalIsRejected() {
        assertThatThrownBy(() -> insertSchedule(21, 999, "MONDAY", "09:00", "12:00", 30))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_weekly_schedules_professional");
    }

    @Test
    void scheduleWithInvalidDayOfWeekIsRejected() {
        insertParents();

        assertThatThrownBy(() -> insertSchedule(21, 15, "FUNDAY", "09:00", "12:00", 30))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_weekly_schedules_day");
    }

    @Test
    void scheduleEndingBeforeItStartsIsRejected() {
        insertParents();

        assertThatThrownBy(() -> insertSchedule(21, 15, "MONDAY", "12:00", "09:00", 30))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_weekly_schedules_time_range");
    }

    @Test
    void scheduleWithZeroLengthIsRejected() {
        insertParents();

        assertThatThrownBy(() -> insertSchedule(21, 15, "MONDAY", "09:00", "09:00", 30))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_weekly_schedules_time_range");
    }

    @Test
    void scheduleWithNonPositiveSlotDurationIsRejected() {
        insertParents();

        assertThatThrownBy(() -> insertSchedule(21, 15, "MONDAY", "09:00", "12:00", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_weekly_schedules_slot_duration");
    }

    @Test
    void syncStateStartsAsASingleRowAtVersionZeroWithoutHistory() {
        Map<String, Object> state = jdbc.sql("SELECT * FROM sync_state").query().singleRow();

        assertThat(state).containsEntry("id", 1).containsEntry("local_version", 0L)
                .containsEntry("last_sync_at", null).containsEntry("last_sync_type", null)
                .containsEntry("last_status", null).containsEntry("last_error", null);
    }

    @Test
    void secondSyncStateRowIsRejected() {
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO sync_state (id) VALUES (2)").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_sync_state_single_row");
    }

    @Test
    void syncStateRejectsAnUnknownSyncType() {
        assertThatThrownBy(() -> jdbc.sql("UPDATE sync_state SET last_sync_type = 'PARTIAL'").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_sync_state_type");
    }

    @Test
    void syncStateRejectsAnUnknownStatus() {
        assertThatThrownBy(() -> jdbc.sql("UPDATE sync_state SET last_status = 'WAIT'").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_sync_state_status");
    }

    @Test
    void syncStateAcceptsTheDocumentedValues() {
        int updated = jdbc.sql("""
                UPDATE sync_state SET local_version = 7, last_sync_at = now(), last_sync_type = 'INCREMENTAL',
                                      last_status = 'ERROR', last_error = 'timeout'""").update();

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void processedEventIsStoredWithItsProcessingTime() {
        insertProcessedEvent("catalog-event-7-4f3b");

        Object processedAt = jdbc.sql("SELECT processed_at FROM processed_events WHERE event_id = 'catalog-event-7-4f3b'")
                .query().singleValue();

        assertThat(processedAt).isNotNull();
    }

    // Base de la deduplicación: Kafka entrega al menos una vez, así que el mismo evento puede llegar dos veces
    @Test
    void duplicatedProcessedEventIsRejected() {
        insertProcessedEvent("catalog-event-7-4f3b");

        assertThatThrownBy(() -> insertProcessedEvent("catalog-event-7-4f3b"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("processed_events_pkey");
    }
}
