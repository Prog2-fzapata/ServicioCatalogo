package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;

// Sin base de datos: el orden que se le pide a JPA. El desempate por id no se ve con unos pocos datos de prueba
class JpaProfessionalCategoryRepositoryAdapterSortTest {

    private final JpaProfessionalCategoryRepositoryAdapter adapter = new JpaProfessionalCategoryRepositoryAdapter(
            null, null);

    @Test
    void addsTheIdAsTheLastCriterionToBreakTies() {
        assertThat(adapter.toSort(CategorySortOrder.byName())).containsExactly(Sort.Order.asc("name"),
                Sort.Order.asc("id"));
    }

    @Test
    void doesNotRepeatTheIdWhenTheRequestAlreadySortsByIt() {
        assertThat(adapter.toSort(List.of(new CategorySortOrder(Field.ID, Direction.DESC))))
                .containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void translatesTheFieldAndTheDirectionToTheEntityAttribute() {
        assertThat(adapter.toSort(List.of(new CategorySortOrder(Field.NAME, Direction.DESC))))
                .containsExactly(Sort.Order.desc("name"), Sort.Order.asc("id"));
    }
}
