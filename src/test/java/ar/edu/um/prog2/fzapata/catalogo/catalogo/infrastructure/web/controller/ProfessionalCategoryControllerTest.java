package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.CategoryDtoMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.CategoryQueryMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.ProblemDetailsSecurityHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.GlobalExceptionHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import lombok.RequiredArgsConstructor;

@WebMvcTest(ProfessionalCategoryController.class)
@Import({CategoryQueryMapper.class, CategoryDtoMapper.class, ProblemDetailsFactory.class,
        GlobalExceptionHandler.class, SecurityConfiguration.class, JwtConfiguration.class,
        ProblemDetailsSecurityHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ProfessionalCategoryControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    private void givenResult(PagedResult<ProfessionalCategory> result) {
        when(catalogService.searchCategories(any(), any(), any())).thenReturn(result);
    }

    private PagedResult<ProfessionalCategory> emptyResult() {
        return new PagedResult<>(List.of(), 0, new PageQuery(0, 20));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/professional-categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void returnsAnArrayOfCategories() throws Exception {
        ProfessionalCategory disabled = category(2);
        disabled.setEnabled(false);
        givenResult(new PagedResult<>(List.of(category(1), disabled), 2, new PageQuery(0, 20)));

        mockMvc.perform(get("/api/professional-categories").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Categoria 1"))
                .andExpect(jsonPath("$[0].description").value("Descripcion 1"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    void noResultsIsAnEmptyArrayWithATotalOfZero() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professional-categories").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void reportsTheTotalAndTheLinksInTheHeaders() throws Exception {
        givenResult(new PagedResult<>(List.of(category(3)), 5, new PageQuery(1, 2)));

        mockMvc.perform(get("/api/professional-categories?enabled=true&page=1&size=2").with(jwt()))
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(header().string("Link", containsString("rel=\"next\"")))
                .andExpect(header().string("Link", containsString("rel=\"prev\"")))
                .andExpect(header().string("Link", containsString("rel=\"last\"")))
                .andExpect(header().string("Link", containsString("rel=\"first\"")))
                .andExpect(header().string("Link", containsString("enabled=true")))
                .andExpect(header().string("Link", containsString("page=2&size=2")));
    }

    @Test
    void passesTheFilterThePageAndTheSortToTheService() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professional-categories").param("enabled", "false").param("page", "1")
                .param("size", "5").param("sort", "name,desc").with(jwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<List<CategorySortOrder>> sort = ArgumentCaptor.captor();
        verify(catalogService).searchCategories(eq(new CategoryFilter(false)), eq(new PageQuery(1, 5)),
                sort.capture());
        assertThat(sort.getValue()).containsExactly(new CategorySortOrder(Field.NAME, Direction.DESC));
    }

    @Test
    void withoutParametersItListsEverythingInThePageOfTwentySortedByName() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professional-categories").with(jwt())).andExpect(status().isOk());

        verify(catalogService).searchCategories(new CategoryFilter(null), new PageQuery(0, 20),
                CategorySortOrder.byName());
    }

    // El tope sale de CATALOGO_PAGE_MAX_SIZE (100 por defecto): un size mayor se reduce, no es un error
    @Test
    void aSizeAboveTheMaximumIsReducedToTheMaximum() throws Exception {
        givenResult(emptyResult());

        mockMvc.perform(get("/api/professional-categories?size=5000").with(jwt())).andExpect(status().isOk());

        verify(catalogService).searchCategories(any(), eq(new PageQuery(0, 100)), any());
    }

    @Test
    void aSortFieldThatIsNotAllowedIsABadRequestThatNamesTheField() throws Exception {
        mockMvc.perform(get("/api/professional-categories?sort=description,asc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].objectName").value("categorySearch"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("id, name")));

        verifyNoInteractions(catalogService);
    }

    @Test
    void anEnabledValueThatIsNotABooleanIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/professional-categories?enabled=quizas").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void onlyAllowsGet() throws Exception {
        mockMvc.perform(post("/api/professional-categories").with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }
}
