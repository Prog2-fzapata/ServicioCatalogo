package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CatedraPropertiesTest {

    // El toString de un record imprime todo: si el JWT apareciera, cualquier log de la configuración lo filtraría
    @Test
    void toStringDoesNotRevealTheJwt() {
        CatedraProperties properties = new CatedraProperties("http://catedra.test", "jwt-super-secreto",
                Duration.ofSeconds(3), Duration.ofSeconds(10));

        assertThat(properties.toString()).doesNotContain("jwt-super-secreto").contains("http://catedra.test");
    }
}
