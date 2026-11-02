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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.WeeklyScheduleRepository;

@ExtendWith(MockitoExtension.class)
class SearchProfessionalsUseCaseImplTest {

    private static final ProfessionalFilter FILTER = new ProfessionalFilter(null, null, null, null);
    private static final PageQuery PAGE = new PageQuery(0, 20);
    private static final List<ProfessionalSortOrder> SORT = ProfessionalSortOrder.byName();

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private ProfessionalCategoryRepository professionalCategoryRepository;

    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @InjectMocks
    private SearchProfessionalsUseCaseImpl useCase;

    private WeeklySchedule scheduleOn(long id, long professionalId, DayOfWeek day, int startHour) {
        WeeklySchedule schedule = schedule(id, professionalId);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(startHour, 0));
        schedule.setEndTime(LocalTime.of(startHour + 1, 0));
        return schedule;
    }

    private void givenFound(long total, Professional... professionals) {
        when(professionalRepository.search(FILTER, PAGE, SORT))
                .thenReturn(new PagedResult<>(List.of(professionals), total, PAGE));
    }

    @Test
    void completesEachProfessionalWithTheNameOfItsCategoryAndItsSchedules() {
        Professional ana = professional(10, 1);
        Professional luis = professional(11, 2);
        givenFound(2, ana, luis);
        when(professionalCategoryRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(category(1), category(2)));
        when(weeklyScheduleRepository.findEnabledByProfessionalIds(Set.of(10L, 11L)))
                .thenReturn(List.of(scheduleOn(100, 10, DayOfWeek.MONDAY, 9), scheduleOn(101, 11, DayOfWeek.FRIDAY, 14)));

        PagedResult<ProfessionalSummary> result = useCase.searchProfessionals(FILTER, PAGE, SORT);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).professional()).isSameAs(ana);
        assertThat(result.content().get(0).categoryName()).isEqualTo("Categoria 1");
        assertThat(result.content().get(0).weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(100L);
        assertThat(result.content().get(1).categoryName()).isEqualTo("Categoria 2");
        assertThat(result.content().get(1).weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(101L);
    }

    @Test
    void asksForTheCategoriesAndSchedulesOfThePageInASingleQueryEach() {
        givenFound(3, professional(10, 1), professional(11, 1), professional(12, 1));
        when(professionalCategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category(1)));
        when(weeklyScheduleRepository.findEnabledByProfessionalIds(Set.of(10L, 11L, 12L))).thenReturn(List.of());

        useCase.searchProfessionals(FILTER, PAGE, SORT);

        verify(professionalRepository).search(FILTER, PAGE, SORT);
        verify(professionalCategoryRepository).findAllById(Set.of(1L));
        verify(weeklyScheduleRepository).findEnabledByProfessionalIds(Set.of(10L, 11L, 12L));
    }

    @Test
    void sortsTheSchedulesByDayOfTheWeekAndThenByStartTime() {
        givenFound(1, professional(10, 1));
        when(professionalCategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category(1)));
        when(weeklyScheduleRepository.findEnabledByProfessionalIds(Set.of(10L))).thenReturn(List.of(
                scheduleOn(1, 10, DayOfWeek.WEDNESDAY, 15),
                scheduleOn(2, 10, DayOfWeek.MONDAY, 14),
                scheduleOn(3, 10, DayOfWeek.WEDNESDAY, 9),
                scheduleOn(4, 10, DayOfWeek.MONDAY, 8)));

        PagedResult<ProfessionalSummary> result = useCase.searchProfessionals(FILTER, PAGE, SORT);

        assertThat(result.content().get(0).weeklySchedules()).extracting(WeeklySchedule::getId)
                .containsExactly(4L, 2L, 3L, 1L);
    }

    @Test
    void aProfessionalWithoutEnabledSchedulesHasAnEmptyList() {
        givenFound(1, professional(10, 1));
        when(professionalCategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category(1)));
        when(weeklyScheduleRepository.findEnabledByProfessionalIds(Set.of(10L))).thenReturn(List.of());

        assertThat(useCase.searchProfessionals(FILTER, PAGE, SORT).content().get(0).weeklySchedules()).isEmpty();
    }

    @Test
    void keepsTheTotalAndThePageThatTheRepositoryReported() {
        PageQuery thirdPage = new PageQuery(2, 1);
        when(professionalRepository.search(FILTER, thirdPage, SORT))
                .thenReturn(new PagedResult<>(List.of(professional(12, 1)), 25, thirdPage));
        when(professionalCategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category(1)));
        when(weeklyScheduleRepository.findEnabledByProfessionalIds(Set.of(12L))).thenReturn(List.of());

        PagedResult<ProfessionalSummary> result = useCase.searchProfessionals(FILTER, thirdPage, SORT);

        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.pageQuery()).isEqualTo(thirdPage);
    }

    @Test
    void anEmptyPageDoesNotQueryTheCategoriesNorTheSchedules() {
        when(professionalRepository.search(FILTER, PAGE, SORT)).thenReturn(new PagedResult<>(List.of(), 0, PAGE));

        PagedResult<ProfessionalSummary> result = useCase.searchProfessionals(FILTER, PAGE, SORT);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(professionalCategoryRepository, never()).findAllById(any());
        verify(weeklyScheduleRepository, never()).findEnabledByProfessionalIds(any());
    }
}
