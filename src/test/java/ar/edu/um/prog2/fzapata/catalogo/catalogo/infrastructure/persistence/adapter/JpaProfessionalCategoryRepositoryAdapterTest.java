package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

// Contra PostgreSQL real: el filtro, el orden y el total salen de la misma consulta paginada
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaProfessionalCategoryRepositoryAdapter.class, CatalogMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaProfessionalCategoryRepositoryAdapterTest {

    private static final PageQuery ALL = new PageQuery(0, 50);

    private final JpaProfessionalCategoryRepositoryAdapter adapter;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;
    private final EntityManager entityManager;

    private ProfessionalCategory named(long id, String name, boolean enabled) {
        ProfessionalCategory category = category(id);
        category.setName(name);
        category.setEnabled(enabled);
        return category;
    }

    private List<Long> idsOf(CategoryFilter filter, PageQuery page, List<CategorySortOrder> sort) {
        return adapter.search(filter, page, sort).content().stream().map(ProfessionalCategory::getId).toList();
    }

    // Cinco categorías, la 3 y la 5 deshabilitadas. Por nombre: Cardiología (2), Dermatología (4), Neurología (3),
    // Pediatría (1) y Traumatología (5)
    @BeforeEach
    void storeTheCategories() {
        categoryRepository.saveAll(List.of(
                named(1, "Pediatría", true),
                named(2, "Cardiología", true),
                named(3, "Neurología", false),
                named(4, "Dermatología", true),
                named(5, "Traumatología", false)).stream().map(catalogMapper::toEntity).toList());
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void withoutACriterionReturnsEveryCategoryEvenTheDisabledOnes() {
        PagedResult<ProfessionalCategory> result = adapter.search(new CategoryFilter(null), ALL,
                CategorySortOrder.byName());

        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.content()).extracting(ProfessionalCategory::getId).containsExactly(2L, 4L, 3L, 1L, 5L);
    }

    @Test
    void filtersByEnabled() {
        assertThat(idsOf(new CategoryFilter(true), ALL, CategorySortOrder.byName())).containsExactly(2L, 4L, 1L);
        assertThat(idsOf(new CategoryFilter(false), ALL, CategorySortOrder.byName())).containsExactly(3L, 5L);
    }

    @Test
    void sortsInTheRequestedDirectionAndField() {
        assertThat(idsOf(new CategoryFilter(null), ALL, List.of(new CategorySortOrder(Field.NAME, Direction.DESC))))
                .containsExactly(5L, 1L, 3L, 4L, 2L);
        assertThat(idsOf(new CategoryFilter(null), ALL, List.of(new CategorySortOrder(Field.ID, Direction.DESC))))
                .containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Test
    void paginatesWithoutRepeatingOrSkippingAndReportsTheTotal() {
        PagedResult<ProfessionalCategory> first = adapter.search(new CategoryFilter(null), new PageQuery(0, 2),
                CategorySortOrder.byName());
        PagedResult<ProfessionalCategory> last = adapter.search(new CategoryFilter(null), new PageQuery(2, 2),
                CategorySortOrder.byName());

        assertThat(first.content()).extracting(ProfessionalCategory::getId).containsExactly(2L, 4L);
        assertThat(last.content()).extracting(ProfessionalCategory::getId).containsExactly(5L);
        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
    }

    @Test
    void theTotalCountsTheFilteredElementsAndNotTheWholeTable() {
        PagedResult<ProfessionalCategory> result = adapter.search(new CategoryFilter(true), new PageQuery(0, 1),
                CategorySortOrder.byName());

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    void aPageBeyondTheEndIsEmptyButStillReportsTheTotal() {
        PagedResult<ProfessionalCategory> result = adapter.search(new CategoryFilter(null), new PageQuery(9, 2),
                CategorySortOrder.byName());

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(5);
    }

    @Test
    void findsACategoryByIdAndKeepsItsFields() {
        ProfessionalCategory found = adapter.findById(3L).orElseThrow();

        assertThat(found.getName()).isEqualTo("Neurología");
        assertThat(found.getDescription()).isEqualTo("Descripcion 3");
        assertThat(found.isEnabled()).isFalse();
    }

    @Test
    void anUnknownIdIsEmpty() {
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void returnsTheExistingCategoriesAndIgnoresTheMissing() {
        assertThat(adapter.findAllById(List.of(1L, 2L, 99L))).extracting(ProfessionalCategory::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }
}
