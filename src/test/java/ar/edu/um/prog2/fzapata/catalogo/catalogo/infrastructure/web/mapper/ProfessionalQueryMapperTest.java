package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception.InvalidSearchParameterException;

class ProfessionalQueryMapperTest {

    private final ProfessionalQueryMapper mapper = new ProfessionalQueryMapper();

    @Test
    void buildsTheFilterFromTheParameters() {
        assertThat(mapper.toFilter(3L, "ana", true, false)).isEqualTo(new ProfessionalFilter(3L, "ana", true, false));
    }

    @Test
    void everyParameterIsOptional() {
        assertThat(mapper.toFilter(null, null, null, null)).isEqualTo(new ProfessionalFilter(null, null, null, null));
    }

    @Test
    void acceptsANameOfExactlyTheMaximumLength() {
        assertThat(mapper.toFilter(null, "a".repeat(ProfessionalQueryMapper.MAX_NAME_LENGTH), null, null).name())
                .hasSize(ProfessionalQueryMapper.MAX_NAME_LENGTH);
    }

    @Test
    void rejectsANameLongerThanTheMaximum() {
        assertThatThrownBy(() -> mapper.toFilter(null, "a".repeat(ProfessionalQueryMapper.MAX_NAME_LENGTH + 1), null,
                null))
                .isInstanceOfSatisfying(InvalidSearchParameterException.class, ex -> {
                    assertThat(ex.getObjectName()).isEqualTo("professionalSearch");
                    assertThat(ex.getField()).isEqualTo("name");
                    assertThat(ex.getMessage()).contains("100");
                });
    }

    @Test
    void takesThePageAndTheSizeFromThePageable() {
        assertThat(mapper.toPageQuery(PageRequest.of(2, 15))).isEqualTo(new PageQuery(2, 15));
    }

    @Test
    void withoutASortTheOrderIsLastNameAndThenFirstName() {
        assertThat(mapper.toSortOrder(PageRequest.of(0, 20))).isEqualTo(ProfessionalSortOrder.byName());
    }

    @Test
    void translatesEachAllowedFieldAndDirection() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("lastName"), Sort.Order.asc("firstName"),
                Sort.Order.desc("id")));

        assertThat(mapper.toSortOrder(pageable)).containsExactly(
                new ProfessionalSortOrder(Field.LAST_NAME, Direction.DESC),
                new ProfessionalSortOrder(Field.FIRST_NAME, Direction.ASC),
                new ProfessionalSortOrder(Field.ID, Direction.DESC));
    }

    @Test
    void rejectsAFieldThatIsNotAllowedAndNamesTheAllowedOnes() {
        assertThatThrownBy(() -> mapper.toSortOrder(PageRequest.of(0, 20, Sort.by("password"))))
                .isInstanceOfSatisfying(InvalidSearchParameterException.class, ex -> {
                    assertThat(ex.getField()).isEqualTo("sort");
                    assertThat(ex.getMessage()).contains("'password'").contains("id, firstName, lastName");
                });
    }

    // El nombre del campo de la base no es un nombre válido: el contrato habla de la respuesta, no de las tablas
    @Test
    void rejectsTheNamesOfTheDatabaseColumns() {
        assertThatThrownBy(() -> mapper.toSortOrder(PageRequest.of(0, 20, Sort.by("last_name"))))
                .isInstanceOf(InvalidSearchParameterException.class);
    }

    @Test
    void oneInvalidFieldAmongValidOnesRejectsTheWholeSort() {
        assertThatThrownBy(() -> mapper.toSortOrder(PageRequest.of(0, 20, Sort.by("lastName", "enabled"))))
                .isInstanceOf(InvalidSearchParameterException.class);
    }
}
