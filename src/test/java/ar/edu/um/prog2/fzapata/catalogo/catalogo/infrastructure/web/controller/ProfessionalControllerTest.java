package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.ProfessionalDtoMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.ProfessionalQueryMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.ProblemDetailsSecurityHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.GlobalExceptionHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import lombok.RequiredArgsConstructor;

@WebMvcTest(ProfessionalController.class)
@Import({ProfessionalQueryMapper.class, ProfessionalDtoMapper.class, ProblemDetailsFactory.class,
        GlobalExceptionHandler.class, SecurityConfiguration.class, JwtConfiguration.class,
        ProblemDetailsSecurityHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ProfessionalControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    private void givenResult(PagedResult<ProfessionalSummary> result) {
        when(catalogService.searchProfessionals(any(), any(), any())).thenReturn(result);
    }

    private PagedResult<ProfessionalSummary> emptyResult() {
        return new PagedResult<>(List.of(), 0, new PageQuery(0, 20));
    }

    private WeeklySchedule monday() {
        WeeklySchedule schedule = schedule(100, 15);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setSlotDurationMinutes(30);
        return schedule;
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void anyAuthenticatedUserCanSearch() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professionals").with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void returnsAnArrayOfProfessionalsWithTheirCategoryAndSchedules() throws Exception {
        givenResult(new PagedResult<>(List.of(
                new ProfessionalSummary(professional(15, 1), category(1).getName(), List.of(monday()))),
                1, new PageQuery(0, 20)));

        mockMvc.perform(get("/api/professionals").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Categoria 1"))
                .andExpect(jsonPath("$[0].firstName").value("Nombre15"))
                .andExpect(jsonPath("$[0].lastName").value("Apellido15"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].weeklySchedules", hasSize(1)))
                .andExpect(jsonPath("$[0].weeklySchedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].weeklySchedules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].weeklySchedules[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$[0].weeklySchedules[0].slotDurationMinutes").value(30));
    }

    @Test
    void noResultsIsAnEmptyArrayWithATotalOfZero() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professionals").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void reportsTheTotalAndTheLinksInTheHeaders() throws Exception {
        givenResult(new PagedResult<>(List.of(new ProfessionalSummary(professional(15, 1), "Cat", List.of())), 5,
                new PageQuery(1, 2)));

        mockMvc.perform(get("/api/professionals?name=ana&page=1&size=2").with(jwt()))
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(header().string("Link", containsString("rel=\"next\"")))
                .andExpect(header().string("Link", containsString("rel=\"prev\"")))
                .andExpect(header().string("Link", containsString("rel=\"last\"")))
                .andExpect(header().string("Link", containsString("rel=\"first\"")))
                .andExpect(header().string("Link", containsString("name=ana")))
                .andExpect(header().string("Link", containsString("page=2&size=2")));
    }

    @Test
    void passesTheFiltersThePageAndTheSortToTheService() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professionals").param("categoryId", "3").param("name", "ana g")
                .param("enabled", "true").param("available", "false").param("page", "1").param("size", "5")
                .param("sort", "lastName,desc", "id,asc").with(jwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<ProfessionalFilter> filter = ArgumentCaptor.forClass(ProfessionalFilter.class);
        ArgumentCaptor<PageQuery> page = ArgumentCaptor.forClass(PageQuery.class);
        ArgumentCaptor<List<ProfessionalSortOrder>> sort = ArgumentCaptor.captor();
        verify(catalogService).searchProfessionals(filter.capture(), page.capture(), sort.capture());
        assertThat(filter.getValue()).isEqualTo(new ProfessionalFilter(3L, "ana g", true, false));
        assertThat(page.getValue()).isEqualTo(new PageQuery(1, 5));
        assertThat(sort.getValue()).containsExactly(new ProfessionalSortOrder(Field.LAST_NAME, Direction.DESC),
                new ProfessionalSortOrder(Field.ID, Direction.ASC));
    }

    @Test
    void withoutParametersItSearchesEveryoneInThePageOfTwentySortedByName() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professionals").with(jwt())).andExpect(status().isOk());

        verify(catalogService).searchProfessionals(new ProfessionalFilter(null, null, null, null),
                new PageQuery(0, 20), ProfessionalSortOrder.byName());
    }

    // El tope sale de CATALOGO_PAGE_MAX_SIZE (100 por defecto): un size mayor se reduce, no es un error
    @Test
    void aSizeAboveTheMaximumIsReducedToTheMaximum() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professionals?size=5000").with(jwt())).andExpect(status().isOk());

        verify(catalogService).searchProfessionals(any(), org.mockito.ArgumentMatchers.eq(new PageQuery(0, 100)),
                any());
    }

    @Test
    void aSortFieldThatIsNotAllowedIsABadRequestThatNamesTheField() throws Exception {
        mockMvc.perform(get("/api/professionals?sort=password,asc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("id, firstName, lastName")));

        verifyNoInteractions(catalogService);
    }

    @Test
    void aNameThatIsTooLongIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/professionals?name=" + "a".repeat(101)).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void aParameterWithTheWrongTypeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/professionals?categoryId=abc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/professionals?enabled=quizas").with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalogService);
    }

    @Test
    void onlyAllowsGet() throws Exception {
        mockMvc.perform(post("/api/professionals").with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void theDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/professionals/15"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void theDetailShowsEveryScheduleWithItsIdAndItsEnabledFlag() throws Exception {
        WeeklySchedule disabled = schedule(101, 15);
        disabled.setDayOfWeek(DayOfWeek.TUESDAY);
        disabled.setStartTime(LocalTime.of(14, 0));
        disabled.setEndTime(LocalTime.of(18, 0));
        disabled.setSlotDurationMinutes(20);
        disabled.setEnabled(false);
        when(catalogService.getProfessional(15L)).thenReturn(
                new ProfessionalSummary(professional(15, 1), "Categoria 1", List.of(monday(), disabled)));

        mockMvc.perform(get("/api/professionals/15").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Categoria 1"))
                .andExpect(jsonPath("$.firstName").value("Nombre15"))
                .andExpect(jsonPath("$.lastName").value("Apellido15"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules", hasSize(2)))
                .andExpect(jsonPath("$.weeklySchedules[0].id").value(100))
                .andExpect(jsonPath("$.weeklySchedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.weeklySchedules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.weeklySchedules[0].enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules[1].id").value(101))
                .andExpect(jsonPath("$.weeklySchedules[1].enabled").value(false));
    }

    @Test
    void aDisabledProfessionalIsReturnedWithEnabledFalse() throws Exception {
        var disabled = professional(15, 1);
        disabled.setEnabled(false);
        when(catalogService.getProfessional(15L))
                .thenReturn(new ProfessionalSummary(disabled, "Categoria 1", List.of()));

        mockMvc.perform(get("/api/professionals/15").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.weeklySchedules", hasSize(0)));
    }

    @Test
    void anUnknownProfessionalIsANotFoundWithItsOwnCode() throws Exception {
        when(catalogService.getProfessional(99L)).thenThrow(new ProfessionalNotFoundException(99L));

        mockMvc.perform(get("/api/professionals/99").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No existe el profesional con id 99"))
                .andExpect(jsonPath("$.path").value("/api/professionals/99"));
    }

    @Test
    void anIdThatIsNotANumberIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/professionals/abc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void theDetailOnlyAllowsGet() throws Exception {
        mockMvc.perform(post("/api/professionals/15").with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }
}
