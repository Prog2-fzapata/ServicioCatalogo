package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void acceptsTheFirstPageWithOneElement() {
        PageQuery query = new PageQuery(0, 1);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(1);
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeSmallerThanOne() {
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
