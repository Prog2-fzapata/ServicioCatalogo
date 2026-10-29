package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ProfessionalFilterTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void aBlankNameDoesNotFilter(String blank) {
        assertThat(new ProfessionalFilter(null, blank, null, null).name()).isNull();
    }

    @Test
    void trimsTheName() {
        assertThat(new ProfessionalFilter(null, "  ana gom  ", null, null).name()).isEqualTo("ana gom");
    }

    @Test
    void keepsTheOtherCriteriaAsGiven() {
        ProfessionalFilter filter = new ProfessionalFilter(3L, "ana", true, false);

        assertThat(filter.categoryId()).isEqualTo(3L);
        assertThat(filter.enabled()).isTrue();
        assertThat(filter.available()).isFalse();
    }
}
