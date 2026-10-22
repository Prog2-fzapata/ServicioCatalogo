package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.CategoryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.ProfessionalResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.WeeklyScheduleResponse;

class CatedraSnapshotMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-10T13:40:00Z");

    private final CatedraSnapshotMapper mapper = new CatedraSnapshotMapper(new CatedraEntityMapper());

    private CategoryResponse category() {
        return new CategoryResponse(1L, "Clinica medica", null, true, NOW, NOW);
    }

    private ProfessionalResponse professional() {
        return new ProfessionalResponse(15L, 1L, "Ana", "Gomez", true, NOW, NOW);
    }

    private WeeklyScheduleResponse schedule() {
        return new WeeklyScheduleResponse(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30,
                true, NOW, NOW);
    }

    private CatedraSnapshotResponse snapshot(Long version, List<CategoryResponse> categories,
            List<ProfessionalResponse> professionals, List<WeeklyScheduleResponse> schedules) {
        return new CatedraSnapshotResponse(version, NOW, categories, professionals, schedules);
    }

    @Test
    void descriptionIsOptional() {
        mapper.toDomainModel(snapshot(1L, List.of(category()), List.of(professional()), List.of(schedule())));
    }

    @Test
    void missingResponseIsRejected() {
        assertThatThrownBy(() -> mapper.toDomainModel(null)).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void missingVersionIsRejected() {
        assertThatThrownBy(() -> mapper.toDomainModel(snapshot(null, List.of(), List.of(), List.of())))
                .isInstanceOf(CatedraResponseException.class).hasMessageContaining("snapshotVersion");
    }

    @Test
    void missingCollectionIsRejected() {
        assertThatThrownBy(() -> mapper.toDomainModel(snapshot(1L, List.of(), null, List.of())))
                .isInstanceOf(CatedraResponseException.class).hasMessageContaining("professionals");
    }

    @Test
    void categoryWithoutNameIsRejected() {
        CategoryResponse unnamed = new CategoryResponse(1L, null, null, true, NOW, NOW);

        assertThatThrownBy(() -> mapper.toDomainModel(snapshot(1L, List.of(unnamed), List.of(), List.of())))
                .isInstanceOf(CatedraResponseException.class).hasMessageContaining("professionalCategories.name");
    }

    @Test
    void professionalWithoutCategoryIsRejected() {
        ProfessionalResponse orphan = new ProfessionalResponse(15L, null, "Ana", "Gomez", true, NOW, NOW);

        assertThatThrownBy(() -> mapper.toDomainModel(snapshot(1L, List.of(), List.of(orphan), List.of())))
                .isInstanceOf(CatedraResponseException.class).hasMessageContaining("professionals.categoryId");
    }

    @Test
    void scheduleWithoutEndTimeIsRejected() {
        WeeklyScheduleResponse open = new WeeklyScheduleResponse(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(9, 0), null,
                30, true, NOW, NOW);

        assertThatThrownBy(() -> mapper.toDomainModel(snapshot(1L, List.of(), List.of(), List.of(open))))
                .isInstanceOf(CatedraResponseException.class).hasMessageContaining("weeklySchedules.endTime");
    }
}
