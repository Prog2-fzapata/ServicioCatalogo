package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PagedResultTest {

    @Test
    void roundsTheTotalOfPagesUp() {
        assertThat(new PagedResult<>(List.of(1, 2), 25, new PageQuery(0, 10)).totalPages()).isEqualTo(3);
    }

    @Test
    void anExactMultipleDoesNotAddAnEmptyPage() {
        assertThat(new PagedResult<>(List.of(1), 20, new PageQuery(0, 10)).totalPages()).isEqualTo(2);
    }

    @Test
    void noElementsMeansNoPages() {
        assertThat(new PagedResult<Integer>(List.of(), 0, new PageQuery(0, 10)).totalPages()).isZero();
    }

    @Test
    void mapsTheContentAndKeepsTheTotalAndThePageQuery() {
        PageQuery query = new PageQuery(1, 2);

        PagedResult<String> mapped = new PagedResult<>(List.of(1, 2), 7, query).map(number -> "n" + number);

        assertThat(mapped.content()).containsExactly("n1", "n2");
        assertThat(mapped.totalElements()).isEqualTo(7);
        assertThat(mapped.pageQuery()).isSameAs(query);
    }
}
