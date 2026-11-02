package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;

// Sin base de datos: el orden que se le pide a JPA. El desempate por id no se ve con unos pocos datos de prueba
// (Postgres devuelve los empates en el orden de inserción), por eso se comprueba acá
class JpaProfessionalRepositoryAdapterSortTest {

    private final JpaProfessionalRepositoryAdapter adapter = new JpaProfessionalRepositoryAdapter(null, null);

    @Test
    void addsTheIdAsTheLastCriterionToBreakTies() {
        Sort sort = adapter.toSort(ProfessionalSortOrder.byName());

        assertThat(sort).containsExactly(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"), Sort.Order.asc("id"));
    }

    @Test
    void doesNotRepeatTheIdWhenTheRequestAlreadySortsByIt() {
        Sort sort = adapter.toSort(List.of(new ProfessionalSortOrder(Field.ID, Direction.DESC)));

        assertThat(sort).containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void translatesEachFieldAndEachDirectionToTheEntityAttribute() {
        Sort sort = adapter.toSort(List.of(new ProfessionalSortOrder(Field.FIRST_NAME, Direction.DESC),
                new ProfessionalSortOrder(Field.LAST_NAME, Direction.ASC)));

        assertThat(sort).containsExactly(Sort.Order.desc("firstName"), Sort.Order.asc("lastName"), Sort.Order.asc("id"));
    }
}
