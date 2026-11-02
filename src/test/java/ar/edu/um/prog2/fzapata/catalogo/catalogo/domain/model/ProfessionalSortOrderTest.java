package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;

class ProfessionalSortOrderTest {

    @Test
    void theDefaultOrderIsLastNameAndThenFirstName() {
        assertThat(ProfessionalSortOrder.byName()).containsExactly(
                new ProfessionalSortOrder(Field.LAST_NAME, Direction.ASC),
                new ProfessionalSortOrder(Field.FIRST_NAME, Direction.ASC));
    }

    @Test
    void onlyTheseFieldsCanBeSortedBy() {
        assertThat(Field.values()).containsExactly(Field.ID, Field.FIRST_NAME, Field.LAST_NAME);
    }
}
