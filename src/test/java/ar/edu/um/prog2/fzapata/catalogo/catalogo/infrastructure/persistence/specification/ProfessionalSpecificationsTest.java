package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfessionalSpecificationsTest {

    @Test
    void escapesThePercentTheUnderscoreAndTheBackslash() {
        assertThat(ProfessionalSpecifications.escapeLike("50%")).isEqualTo("50\\%");
        assertThat(ProfessionalSpecifications.escapeLike("a_b")).isEqualTo("a\\_b");
        assertThat(ProfessionalSpecifications.escapeLike("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    void leavesAnOrdinaryTextUntouched() {
        assertThat(ProfessionalSpecifications.escapeLike("ana gomez")).isEqualTo("ana gomez");
    }

    // La barra se escapa primero: si no, la barra que agrega el escape de % se volvería a escapar
    @Test
    void escapesTheBackslashBeforeTheOtherCharacters() {
        assertThat(ProfessionalSpecifications.escapeLike("\\%")).isEqualTo("\\\\\\%");
    }
}
