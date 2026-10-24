package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class Argon2PasswordHasherTest {

    // Los mínimos de OWASP, con los mismos tamaños de sal y de hash que usa la configuración
    private static final String OWASP_PREFIX = "$argon2id$v=19$m=19456,t=2,p=1$";

    private final Argon2PasswordHasher hasher = hasherWith(19456, 2, 1);

    private static Argon2PasswordHasher hasherWith(int memoryKib, int iterations, int parallelism) {
        return new Argon2PasswordHasher(new Argon2PasswordEncoder(16, 32, parallelism, memoryKib, iterations));
    }

    @Test
    void hashHasArgon2idFormatWithTheConfiguredCostAndDoesNotContainThePassword() {
        String hash = hasher.hash("clave123");

        assertThat(hash).startsWith(OWASP_PREFIX).hasSize(97).doesNotContain("clave123");
    }

    @Test
    void samePasswordProducesDifferentHashesBecauseOfTheSalt() {
        assertThat(hasher.hash("clave123")).isNotEqualTo(hasher.hash("clave123"));
    }

    @Test
    void matchesAcceptsTheRightPasswordOnly() {
        String hash = hasher.hash("clave123");

        assertThat(hasher.matches("clave123", hash)).isTrue();
        assertThat(hasher.matches("otra", hash)).isFalse();
    }

    @Test
    void everyCharacterOfALongPasswordCounts() {
        // Sin límite de 72 bytes: ni se rechaza ni se ignora lo que pasa de ahí
        String longPassword = "a".repeat(99) + "x";
        String hash = hasher.hash(longPassword);

        assertThat(hasher.matches(longPassword, hash)).isTrue();
        assertThat(hasher.matches("a".repeat(99) + "y", hash)).isFalse();
    }

    @Test
    void multibytePasswordOf100CharactersIsAccepted() {
        // 100 ñ pesan 200 bytes en UTF-8
        String password = "ñ".repeat(100);
        String hash = hasher.hash(password);

        assertThat(hasher.matches(password, hash)).isTrue();
    }

    // El hash guarda sus parámetros: subir el costo después no deja afuera a los usuarios ya registrados
    @Test
    void verifiesAHashWithTheCostStoredInItEvenIfTheConfigurationChangedLater() {
        String oldHash = hasher.hash("clave123");
        Argon2PasswordHasher laterHasher = hasherWith(32768, 3, 2);

        assertThat(laterHasher.matches("clave123", oldHash)).isTrue();
        assertThat(laterHasher.hash("clave123")).startsWith("$argon2id$v=19$m=32768,t=3,p=2$");
    }

    @Test
    void matchesIsFalseForAHashInAnotherFormat() {
        String bcryptHash = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";

        assertThat(hasher.matches("clave123", bcryptHash)).isFalse();
    }
}
