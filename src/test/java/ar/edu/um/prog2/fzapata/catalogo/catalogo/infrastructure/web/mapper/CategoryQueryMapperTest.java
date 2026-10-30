package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception.InvalidSearchParameterException;

class CategoryQueryMapperTest {

    private final CategoryQueryMapper mapper = new CategoryQueryMapper();

    @Test
    void buildsTheFilterFromTheParameter() {
        assertThat(mapper.toFilter(true)).isEqualTo(new CategoryFilter(true));
        assertThat(mapper.toFilter(null)).isEqualTo(new CategoryFilter(null));
    }

    @Test
    void takesThePageAndTheSizeFromThePageable() {
        assertThat(mapper.toPageQuery(PageRequest.of(2, 15))).isEqualTo(new PageQuery(2, 15));
    }

    @Test
    void withoutASortTheOrderIsTheName() {
        assertThat(mapper.toSortOrder(PageRequest.of(0, 20))).isEqualTo(CategorySortOrder.byName());
    }

    @Test
    void translatesEachAllowedFieldAndDirection() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("name"), Sort.Order.asc("id")));

        assertThat(mapper.toSortOrder(pageable)).containsExactly(new CategorySortOrder(Field.NAME, Direction.DESC),
                new CategorySortOrder(Field.ID, Direction.ASC));
    }

    @Test
    void rejectsAFieldThatIsNotAllowedAndNamesTheAllowedOnes() {
        assertThatThrownBy(() -> mapper.toSortOrder(PageRequest.of(0, 20, Sort.by("description"))))
                .isInstanceOfSatisfying(InvalidSearchParameterException.class, ex -> {
                    assertThat(ex.getObjectName()).isEqualTo("categorySearch");
                    assertThat(ex.getField()).isEqualTo("sort");
                    assertThat(ex.getMessage()).contains("'description'").contains("id, name");
                });
    }

    @Test
    void oneInvalidFieldAmongValidOnesRejectsTheWholeSort() {
        assertThatThrownBy(() -> mapper.toSortOrder(PageRequest.of(0, 20, Sort.by("name", "enabled"))))
                .isInstanceOf(InvalidSearchParameterException.class);
    }
}
