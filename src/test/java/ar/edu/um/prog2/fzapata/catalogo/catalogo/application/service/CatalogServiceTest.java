package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetProfessionalUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalCategoriesUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SynchronizeCatalogUseCase synchronizeCatalogUseCase;

    @Mock
    private GetSyncStatusUseCase getSyncStatusUseCase;

    @Mock
    private SearchProfessionalsUseCase searchProfessionalsUseCase;

    @Mock
    private GetProfessionalUseCase getProfessionalUseCase;

    @Mock
    private SearchProfessionalCategoriesUseCase searchProfessionalCategoriesUseCase;

    @InjectMocks
    private CatalogService service;

    @Test
    void returnsTheSyncStatus() {
        SyncState state = SyncState.builder().localVersion(5).build();
        when(getSyncStatusUseCase.getSyncStatus()).thenReturn(Optional.of(state));

        assertThat(service.getSyncStatus()).isSameAs(state);
    }

    // Una base sin sincronizar tiene la fila con versión 0; sin fila, la base fue alterada
    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(getSyncStatusUseCase.getSyncStatus()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSyncStatus()).isInstanceOf(SyncStateMissingException.class)
                .hasMessage("La tabla sync_state está vacía, la base fue alterada");
    }

    @Test
    void delegatesTheSearchOfProfessionals() {
        ProfessionalFilter filter = new ProfessionalFilter(1L, "ana", true, null);
        PageQuery page = new PageQuery(0, 20);
        List<ProfessionalSortOrder> sort = ProfessionalSortOrder.byName();
        PagedResult<ProfessionalSummary> expected = new PagedResult<>(List.of(), 0, page);
        when(searchProfessionalsUseCase.searchProfessionals(filter, page, sort)).thenReturn(expected);

        assertThat(service.searchProfessionals(filter, page, sort)).isSameAs(expected);
    }

    @Test
    void returnsTheRequestedProfessional() {
        ProfessionalSummary summary = new ProfessionalSummary(professional(10, 1), "Cat", List.of());
        when(getProfessionalUseCase.getProfessional(10L)).thenReturn(Optional.of(summary));

        assertThat(service.getProfessional(10L)).isSameAs(summary);
    }

    @Test
    void failsWhenTheProfessionalDoesNotExist() {
        when(getProfessionalUseCase.getProfessional(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfessional(99L)).isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessage("No existe el profesional con id 99");
    }

    @Test
    void delegatesTheSearchOfCategories() {
        CategoryFilter filter = new CategoryFilter(true);
        PageQuery page = new PageQuery(0, 20);
        List<CategorySortOrder> sort = CategorySortOrder.byName();
        PagedResult<ProfessionalCategory> expected = new PagedResult<>(List.of(), 0, page);
        when(searchProfessionalCategoriesUseCase.searchCategories(filter, page, sort)).thenReturn(expected);

        assertThat(service.searchCategories(filter, page, sort)).isSameAs(expected);
    }
}
