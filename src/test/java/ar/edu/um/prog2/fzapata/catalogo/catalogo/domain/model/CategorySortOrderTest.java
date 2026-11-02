package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;

class CategorySortOrderTest {

    @Test
    void theDefaultOrderIsTheName() {
        assertThat(CategorySortOrder.byName()).containsExactly(new CategorySortOrder(Field.NAME, Direction.ASC));
    }

    @Test
    void onlyTheseFieldsCanBeSortedBy() {
        assertThat(Field.values()).containsExactly(Field.ID, Field.NAME);
    }
}
