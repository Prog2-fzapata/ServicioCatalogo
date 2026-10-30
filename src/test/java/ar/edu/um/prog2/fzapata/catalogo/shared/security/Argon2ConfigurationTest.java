package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class Argon2ConfigurationTest {

    private static final Map<String, String> OWASP_MINIMUM = Map.of(
            "password.argon2.memory-kib", "19456",
            "password.argon2.iterations", "2",
            "password.argon2.parallelism", "1");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Argon2Configuration.class);

    // Los tres parámetros en el mínimo de OWASP, cambiando uno por otro valor o quitándolo (value = null)
    private String[] minimumWith(String name, String value) {
        List<String> values = new ArrayList<>();
        OWASP_MINIMUM.forEach((key, minimum) -> {
            String used = key.equals(name) ? value : minimum;
            if (used != null) {
                values.add(key + "=" + used);
            }
        });
        return values.toArray(String[]::new);
    }

    @Test
    void theOwaspMinimumIsAccepted() {
        contextRunner.withPropertyValues(minimumWith("", "")).run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void theEncoderUsesTheConfiguredCost() {
        contextRunner.withPropertyValues("password.argon2.memory-kib=20000", "password.argon2.iterations=3",
                "password.argon2.parallelism=2").run(context -> {
                    String hash = context.getBean(Argon2PasswordEncoder.class).encode("clave123");

                    // 97 caracteres: sal de 16 bytes y hash de 32 bytes
                    assertThat(hash).startsWith("$argon2id$v=19$m=20000,t=3,p=2$").hasSize(97);
                });
    }

    @ParameterizedTest
    @CsvSource({
            "password.argon2.memory-kib, 19455, memoryKib",
            "password.argon2.iterations, 1, iterations",
            "password.argon2.parallelism, 0, parallelism"})
    void contextFailsWhenTheCostIsBelowTheMinimum(String property, String value, String field) {
        contextRunner.withPropertyValues(minimumWith(property, value))
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining(field));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password.argon2.memory-kib", "password.argon2.iterations",
            "password.argon2.parallelism"})
    void contextFailsWhenAnyParameterIsMissing(String missing) {
        contextRunner.withPropertyValues(minimumWith(missing, null)).run(context -> assertThat(context).hasFailed());
    }
}
