package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;

@ExtendWith(MockitoExtension.class)
class SearchProfessionalCategoriesUseCaseImplTest {

    @Mock
    private ProfessionalCategoryRepository professionalCategoryRepository;

    @InjectMocks
    private SearchProfessionalCategoriesUseCaseImpl useCase;

    @Test
    void returnsWhatTheRepositoryFoundForTheFilterThePageAndTheSort() {
        CategoryFilter filter = new CategoryFilter(true);
        PageQuery page = new PageQuery(1, 5);
        List<CategorySortOrder> sort = CategorySortOrder.byName();
        PagedResult<ProfessionalCategory> expected = new PagedResult<>(List.of(category(1)), 6, page);
        when(professionalCategoryRepository.search(filter, page, sort)).thenReturn(expected);

        assertThat(useCase.searchCategories(filter, page, sort)).isSameAs(expected);
    }
}
