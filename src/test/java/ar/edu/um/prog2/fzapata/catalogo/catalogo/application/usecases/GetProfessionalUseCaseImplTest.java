package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.WeeklyScheduleRepository;

@ExtendWith(MockitoExtension.class)
class GetProfessionalUseCaseImplTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private ProfessionalCategoryRepository professionalCategoryRepository;

    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @InjectMocks
    private GetProfessionalUseCaseImpl useCase;

    private WeeklySchedule scheduleOn(long id, DayOfWeek day, int startHour, boolean enabled) {
        WeeklySchedule schedule = schedule(id, 10);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(startHour, 0));
        schedule.setEnabled(enabled);
        return schedule;
    }

    @Test
    void completesTheProfessionalWithTheNameOfItsCategoryAndItsSchedules() {
        Professional ana = professional(10, 1);
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(ana));
        when(professionalCategoryRepository.findById(1L)).thenReturn(Optional.of(category(1)));
        when(weeklyScheduleRepository.findByProfessionalId(10L))
                .thenReturn(List.of(scheduleOn(100, DayOfWeek.MONDAY, 9, true)));

        ProfessionalSummary summary = useCase.getProfessional(10L).orElseThrow();

        assertThat(summary.professional()).isSameAs(ana);
        assertThat(summary.categoryName()).isEqualTo("Categoria 1");
        assertThat(summary.weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(100L);
    }

    // La agenda vigente la interpreta quien consume: un horario deshabilitado se informa como tal
    @Test
    void includesTheDisabledSchedulesSortedByDayAndStartTime() {
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(professional(10, 1)));
        when(professionalCategoryRepository.findById(1L)).thenReturn(Optional.of(category(1)));
        when(weeklyScheduleRepository.findByProfessionalId(10L)).thenReturn(List.of(
                scheduleOn(1, DayOfWeek.WEDNESDAY, 15, true),
                scheduleOn(2, DayOfWeek.MONDAY, 14, false),
                scheduleOn(3, DayOfWeek.MONDAY, 8, true)));

        ProfessionalSummary summary = useCase.getProfessional(10L).orElseThrow();

        assertThat(summary.weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(3L, 2L, 1L);
        assertThat(summary.weeklySchedules()).extracting(WeeklySchedule::isEnabled).containsExactly(true, false, true);
    }

    @Test
    void aDisabledProfessionalInADisabledCategoryIsReturnedAnyway() {
        Professional disabled = professional(10, 1);
        disabled.setEnabled(false);
        var disabledCategory = category(1);
        disabledCategory.setEnabled(false);
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(disabled));
        when(professionalCategoryRepository.findById(1L)).thenReturn(Optional.of(disabledCategory));
        when(weeklyScheduleRepository.findByProfessionalId(10L)).thenReturn(List.of());

        ProfessionalSummary summary = useCase.getProfessional(10L).orElseThrow();

        assertThat(summary.professional().isEnabled()).isFalse();
        assertThat(summary.categoryName()).isEqualTo("Categoria 1");
        assertThat(summary.weeklySchedules()).isEmpty();
    }

    @Test
    void anUnknownIdIsEmptyAndDoesNotQueryTheCategoryNorTheSchedules() {
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(useCase.getProfessional(99L)).isEmpty();
        verify(professionalCategoryRepository, never()).findById(any());
        verify(weeklyScheduleRepository, never()).findByProfessionalId(any());
    }

    // La clave foránea lo impide; si la copia local quedara alterada, el nombre queda vacío y no se cae la consulta
    @Test
    void aMissingCategoryLeavesTheNameEmpty() {
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(professional(10, 1)));
        when(professionalCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        when(weeklyScheduleRepository.findByProfessionalId(10L)).thenReturn(List.of());

        assertThat(useCase.getProfessional(10L).orElseThrow().categoryName()).isNull();
    }
}
