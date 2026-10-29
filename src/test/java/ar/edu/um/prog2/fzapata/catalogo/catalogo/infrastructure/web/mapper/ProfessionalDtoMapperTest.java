package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalSummaryResponse;

class ProfessionalDtoMapperTest {

    private final ProfessionalDtoMapper mapper = new ProfessionalDtoMapper();

    @Test
    void mapsTheProfessionalTheCategoryNameAndTheSchedules() {
        Professional professional = professional(15, 1);
        professional.setEnabled(false);
        var monday = schedule(100, 15);
        monday.setDayOfWeek(DayOfWeek.MONDAY);
        monday.setStartTime(LocalTime.of(9, 0));
        monday.setEndTime(LocalTime.of(12, 30));
        monday.setSlotDurationMinutes(30);

        ProfessionalSummaryResponse response = mapper
                .toResponse(new ProfessionalSummary(professional, "Clinica medica", List.of(monday)));

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Clinica medica");
        assertThat(response.getFirstName()).isEqualTo("Nombre15");
        assertThat(response.getLastName()).isEqualTo("Apellido15");
        assertThat(response.isEnabled()).isFalse();
        assertThat(response.getWeeklySchedules()).singleElement().satisfies(item -> {
            assertThat(item.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(item.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(item.getEndTime()).isEqualTo(LocalTime.of(12, 30));
            assertThat(item.getSlotDurationMinutes()).isEqualTo(30);
        });
    }

    @Test
    void aProfessionalWithoutSchedulesHasAnEmptyList() {
        assertThat(mapper.toResponse(new ProfessionalSummary(professional(15, 1), "Cat", List.of()))
                .getWeeklySchedules()).isEmpty();
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
